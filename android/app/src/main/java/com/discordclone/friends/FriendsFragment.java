package com.discordclone.friends;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.discordclone.R;
import com.discordclone.chat.DMChatActivity;
import com.discordclone.models.Friend;
import com.discordclone.models.FriendRequest;
import com.discordclone.models.User;
import com.discordclone.repository.FriendRepository;
import com.discordclone.repository.UserRepository;
import com.discordclone.utils.AvatarGenerator;
import com.discordclone.utils.FirebaseUtil;
import com.discordclone.utils.NotificationHelper;
import com.discordclone.utils.PreferencesUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendsFragment extends Fragment {
    private ListView friendsList;
    private TextView emptyView, requestBadge;
    private LinearLayout requestsSection;
    private ListView requestsList;
    private Button addFriendBtn;
    private FriendRepository friendRepo;
    private UserRepository userRepo;
    private ListenerRegistration friendsListener, requestsListener, unreadListener;
    private Map<String, Long> prevUnreadCounts = new HashMap<>();
    private List<FriendInfo> friendInfos = new ArrayList<>();
    private List<RequestInfo> requestInfos = new ArrayList<>();
    private PreferencesUtil prefs;
    private String myUid;

    private static class FriendInfo {
        String uid, name, status;
        long unreadCount;
    }

    private static class RequestInfo {
        String requestId, fromId, name;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        friendRepo = new FriendRepository();
        userRepo = new UserRepository();
        prefs = new PreferencesUtil(getActivity());
        myUid = FirebaseUtil.getCurrentUid();

        friendsList = view.findViewById(R.id.friends_list);
        emptyView = view.findViewById(R.id.friends_empty);
        requestsSection = view.findViewById(R.id.friend_requests_section);
        requestsList = view.findViewById(R.id.friend_requests_list);
        requestBadge = view.findViewById(R.id.friend_request_badge);
        addFriendBtn = view.findViewById(R.id.add_friend_btn);

        addFriendBtn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AddFriendActivity.class));
        });

        friendsList.setAdapter(new FriendListAdapter());
        requestsList.setAdapter(new RequestListAdapter());

        loadFriends();
        loadRequests();

        return view;
    }

    private void loadFriends() {
        String uid = FirebaseUtil.getCurrentUid();
        if (uid == null) return;

        friendsListener = FirebaseUtil.friendsForUser(uid)
            .addSnapshotListener((snapshot, e) -> {
                if (e != null || snapshot == null) return;

                List<Friend> friends = friendRepo.parseFriends(snapshot);
                friendInfos.clear();

                if (friends.isEmpty()) {
                    friendsList.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    return;
                }

                friendsList.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);

                watchUnreadCounts();
                for (Friend friend : friends) {
                    loadFriendInfo(friend.getFriendId());
                }
            });
    }

    private String getDmChannelId(String friendUid) {
        if (myUid == null) return null;
        String combined = myUid.compareTo(friendUid) < 0 ?
            myUid + "_" + friendUid : friendUid + "_" + myUid;
        return "dm_" + combined;
    }

    private boolean firstUnreadLoad = true;

    private void watchUnreadCounts() {
        if (unreadListener != null) unreadListener.remove();
        unreadListener = FirebaseUtil.currentUserRef().addSnapshotListener((doc, e) -> {
            if (e != null || doc == null || !doc.exists() || getActivity() == null) return;
            Object uc = doc.get("unreadCounts");
            Map<String, Long> current = new HashMap<>();
            if (uc instanceof Map) {
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) uc).entrySet()) {
                    if (entry.getValue() instanceof Number) {
                        current.put(entry.getKey(), ((Number) entry.getValue()).longValue());
                    }
                }
            }

            if (!firstUnreadLoad) {
                // Detect new unreads for notification
                for (Map.Entry<String, Long> entry : current.entrySet()) {
                    String channelId = entry.getKey();
                    long newCount = entry.getValue();
                    long prevCount = prevUnreadCounts.containsKey(channelId) ? prevUnreadCounts.get(channelId) : 0;
                    if (newCount > prevCount && !channelId.equals(DMChatActivity.currentChannel)) {
                        String recipientId = FirebaseUtil.getRecipientIdFromDmChannel(channelId);
                        if (recipientId != null) {
                            FirebaseUtil.getUser(recipientId).addOnSuccessListener(userDoc -> {
                                if (getActivity() == null) return;
                                com.discordclone.models.User user = userDoc.toObject(com.discordclone.models.User.class);
                                String senderName = user != null && user.getUsername() != null ? user.getUsername() : "Someone";
                                NotificationHelper.showMessageNotification(getActivity(), channelId, senderName, "Sent you a message");
                            });
                        }
                    }
                }
            }
            firstUnreadLoad = false;
            prevUnreadCounts.clear();
            prevUnreadCounts.putAll(current);

            // Update existing friend info badges
            for (FriendInfo info : friendInfos) {
                String ch = getDmChannelId(info.uid);
                info.unreadCount = ch != null && current.containsKey(ch) ? current.get(ch) : 0;
            }
            ((FriendListAdapter) friendsList.getAdapter()).notifyDataSetChanged();
        });
    }

    private void loadFriendInfo(final String friendUid) {
        FirebaseUtil.getUser(friendUid).addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user == null) return;

            FriendInfo info = new FriendInfo();
            info.uid = friendUid;
            info.name = user.getUsername();
            info.status = user.getStatus() != null ? user.getStatus() : User.STATUS_OFFLINE;

            String channelId = getDmChannelId(friendUid);
            info.unreadCount = 0;
            if (channelId != null && prevUnreadCounts.containsKey(channelId)) {
                long count = prevUnreadCounts.get(channelId);
                info.unreadCount = count > 0 ? count : 0;
            }

            friendInfos.add(info);
            ((FriendListAdapter) friendsList.getAdapter()).notifyDataSetChanged();
        });
    }

    private class FriendListAdapter extends BaseAdapter {
        @Override
        public int getCount() { return friendInfos.size(); }

        @Override
        public FriendInfo getItem(int position) { return friendInfos.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                    .inflate(R.layout.item_friend, parent, false);
            }

            FriendInfo info = getItem(position);
            String name = info.name != null ? info.name : "Unknown";
            String firstLetter = name.substring(0, 1).toUpperCase();

            TextView avatarView = convertView.findViewById(R.id.friend_avatar);
            avatarView.setText(firstLetter);
            String color = AvatarGenerator.getColorForUser(info.uid);
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(android.graphics.Color.parseColor(color));
            avatarView.setBackground(drawable);

            ((TextView) convertView.findViewById(R.id.friend_name_text)).setText(name);

            View statusDot = convertView.findViewById(R.id.friend_status_dot);
            GradientDrawable dotDrawable = new GradientDrawable();
            dotDrawable.setShape(GradientDrawable.OVAL);
            int dotColor;
            String statusText;
            switch (info.status) {
                case User.STATUS_ONLINE:
                    dotColor = 0xFF23A559;
                    statusText = "Online";
                    break;
                case User.STATUS_IDLE:
                    dotColor = 0xFFF0B232;
                    statusText = "Idle";
                    break;
                case User.STATUS_DND:
                    dotColor = 0xFFED4245;
                    statusText = "Do Not Disturb";
                    break;
                default:
                    dotColor = 0xFF80848E;
                    statusText = "Offline";
            }
            dotDrawable.setColor(dotColor);
            statusDot.setBackground(dotDrawable);

            ((TextView) convertView.findViewById(R.id.friend_status_text)).setText(statusText);

            // Unread badge
            TextView badgeView = convertView.findViewById(R.id.friend_unread_badge);
            if (info.unreadCount > 0) {
                badgeView.setText(String.valueOf(Math.min(info.unreadCount, 99)));
                badgeView.setVisibility(View.VISIBLE);
            } else {
                badgeView.setVisibility(View.GONE);
            }

            convertView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), DMChatActivity.class);
                intent.putExtra("friendId", info.uid);
                intent.putExtra("friendName", info.name);
                startActivity(intent);
            });

            return convertView;
        }
    }

    private class RequestListAdapter extends BaseAdapter {
        @Override
        public int getCount() { return requestInfos.size(); }

        @Override
        public RequestInfo getItem(int position) { return requestInfos.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                    .inflate(R.layout.item_friend, parent, false);
            }

            RequestInfo info = getItem(position);
            String name = info.name != null ? info.name : "Unknown";
            String firstLetter = name.substring(0, 1).toUpperCase();

            TextView avatarView = convertView.findViewById(R.id.friend_avatar);
            avatarView.setText(firstLetter);
            String color = AvatarGenerator.getColorForUser(info.fromId);
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(android.graphics.Color.parseColor(color));
            avatarView.setBackground(drawable);

            ((TextView) convertView.findViewById(R.id.friend_name_text)).setText(name);

            TextView statusText = convertView.findViewById(R.id.friend_status_text);
            statusText.setText("Wants to be friends");

            View statusDot = convertView.findViewById(R.id.friend_status_dot);
            GradientDrawable dotDrawable = new GradientDrawable();
            dotDrawable.setShape(GradientDrawable.OVAL);
            dotDrawable.setColor(0xFFF0B232);
            statusDot.setBackground(dotDrawable);

            // No unread badge for requests
            TextView badgeView = convertView.findViewById(R.id.friend_unread_badge);
            badgeView.setVisibility(View.GONE);

            convertView.setOnClickListener(v -> {
                androidx.appcompat.app.AlertDialog.Builder builder =
                    new androidx.appcompat.app.AlertDialog.Builder(getActivity());
                builder.setTitle("Friend Request")
                    .setMessage("Accept friend request from " + name + "?")
                    .setPositiveButton("Accept", (dialog, which) -> {
                        friendRepo.acceptFriendRequest(info.requestId, info.fromId)
                            .addOnSuccessListener(v2 -> Toast.makeText(getActivity(),
                                "Friend added!", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Reject", (dialog, which) -> {
                        friendRepo.rejectFriendRequest(info.requestId);
                    })
                    .show();
            });

            return convertView;
        }
    }

    private void loadRequests() {
        String uid = FirebaseUtil.getCurrentUid();
        if (uid == null) return;

        requestsListener = FirebaseUtil.friendRequestsForUser(uid)
            .addSnapshotListener((snapshot, e) -> {
                if (e != null || snapshot == null) return;

                List<FriendRequest> requests = friendRepo.parseRequests(snapshot);
                requestInfos.clear();

                if (requests.isEmpty()) {
                    requestsSection.setVisibility(View.GONE);
                    return;
                }

                requestsSection.setVisibility(View.VISIBLE);
                requestBadge.setText(String.valueOf(requests.size()));
                requestBadge.setVisibility(View.VISIBLE);

                for (FriendRequest req : requests) {
                    loadRequestInfo(req);
                }
            });
    }

    private void loadRequestInfo(final FriendRequest request) {
        FirebaseUtil.getUser(request.getFromId()).addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user == null) return;

            RequestInfo info = new RequestInfo();
            info.requestId = request.getId();
            info.fromId = request.getFromId();
            info.name = user.getUsername();
            requestInfos.add(info);
            ((RequestListAdapter) requestsList.getAdapter()).notifyDataSetChanged();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (friendsListener != null) friendsListener.remove();
        if (requestsListener != null) requestsListener.remove();
        if (unreadListener != null) unreadListener.remove();
    }
}