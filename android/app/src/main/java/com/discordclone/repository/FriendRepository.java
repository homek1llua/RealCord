package com.discordclone.repository;

import android.util.Log;

import com.discordclone.models.Friend;
import com.discordclone.models.FriendRequest;
import com.discordclone.utils.FirebaseUtil;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class FriendRepository {
    private static final String TAG = "FriendRepository";

    public Task<Void> sendFriendRequest(String toUserId) {
        String fromId = FirebaseUtil.getCurrentUid();
        if (fromId == null || fromId.equals(toUserId)) {
            throw new IllegalArgumentException("Cannot send request to yourself");
        }
        FriendRequest request = new FriendRequest(fromId, toUserId);
        return FirebaseUtil.friendRequestsRef().document().set(request);
    }

    public Task<Void> acceptFriendRequest(String requestId, String fromId) {
        String myId = FirebaseUtil.getCurrentUid();

        Friend friend1 = new Friend(myId, fromId);
        Friend friend2 = new Friend(fromId, myId);

        return FirebaseUtil.friendRequestsRef().document(requestId)
            .update("status", FriendRequest.STATUS_ACCEPTED)
            .continueWithTask(task -> {
                Task<Void> add1 = FirebaseUtil.friendsRef().document().set(friend1);
                Task<Void> add2 = FirebaseUtil.friendsRef().document().set(friend2);
                return add1.continueWithTask(t -> add2);
            });
    }

    public Task<Void> rejectFriendRequest(String requestId) {
        return FirebaseUtil.friendRequestsRef().document(requestId)
            .update("status", FriendRequest.STATUS_REJECTED);
    }

    public Task<Void> removeFriend(String friendId) {
        String myId = FirebaseUtil.getCurrentUid();
        return FirebaseUtil.friendsRef()
            .whereEqualTo("userId", myId)
            .whereEqualTo("friendId", friendId)
            .get()
            .continueWithTask(task -> {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    return FirebaseUtil.friendsRef()
                        .document(task.getResult().getDocuments().get(0).getId())
                        .delete();
                }
                return null;
            })
            .continueWithTask(task -> {
                if (task != null && task.isSuccessful()) {
                    return FirebaseUtil.friendsRef()
                        .whereEqualTo("userId", friendId)
                        .whereEqualTo("friendId", myId)
                        .get()
                        .continueWithTask(t -> {
                            if (t.isSuccessful() && !t.getResult().isEmpty()) {
                                return FirebaseUtil.friendsRef()
                                    .document(t.getResult().getDocuments().get(0).getId())
                                    .delete();
                            }
                            return null;
                        });
                }
                return null;
            });
    }

    public Task<QuerySnapshot> getFriends(String userId) {
        return FirebaseUtil.friendsRef()
            .whereEqualTo("userId", userId)
            .get();
    }

    public Task<QuerySnapshot> getFriendRequests(String userId) {
        return FirebaseUtil.friendRequestsForUser(userId).get();
    }

    public Task<QuerySnapshot> getSentRequests(String userId) {
        return FirebaseUtil.sentFriendRequests(userId).get();
    }

    public List<Friend> parseFriends(QuerySnapshot snapshot) {
        List<Friend> friends = new ArrayList<>();
        if (snapshot != null) {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Friend friend = doc.toObject(Friend.class);
                if (friend != null) {
                    friend.setId(doc.getId());
                    friends.add(friend);
                }
            }
        }
        return friends;
    }

    public List<FriendRequest> parseRequests(QuerySnapshot snapshot) {
        List<FriendRequest> requests = new ArrayList<>();
        if (snapshot != null) {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                FriendRequest request = doc.toObject(FriendRequest.class);
                if (request != null) {
                    request.setId(doc.getId());
                    requests.add(request);
                }
            }
        }
        return requests;
    }
}
