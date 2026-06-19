package com.discordclone.chat;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.discordclone.R;
import com.discordclone.models.Message;
import com.discordclone.models.User;
import com.discordclone.repository.FriendRepository;
import com.discordclone.repository.MessageRepository;
import com.discordclone.utils.AvatarGenerator;
import com.discordclone.utils.FirebaseUtil;
import com.discordclone.utils.NitroManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatAdapter extends BaseAdapter {
    private List<Message> messages = new ArrayList<>();
    private String currentUid;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private MessageRepository messageRepo = new MessageRepository();
    private Map<String, String> senderStatusCache = new HashMap<>();
    private Map<String, User> senderProfileCache = new HashMap<>();
    private Context dmContext;
    private OnReplyListener replyListener;
    private OnDeleteListener deleteListener;

    public interface OnReplyListener {
        void onReply(String messageId, String senderName);
    }

    public interface OnDeleteListener {
        void onDelete(String messageId);
    }

    public void setOnReplyListener(OnReplyListener listener) { this.replyListener = listener; }
    public void setOnDeleteListener(OnDeleteListener listener) { this.deleteListener = listener; }

    private static final String[] REACTION_EMOJIS = {"\u2764\uFE0F", "\uD83D\uDE06", "\uD83D\uDE2E", "\uD83D\uDE22", "\uD83D\uDE4F", "\uD83D\uDCAF"};

    public ChatAdapter() {
        this.currentUid = FirebaseUtil.getCurrentUid();
    }

    public void setDmContext(Context context) {
        this.dmContext = context;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void addMessage(Message message) {
        messages.add(message);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Message getItem(int position) {
        return messages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = getItem(position);
        if (msg.getSenderId() != null && msg.getSenderId().equals(currentUid)) {
            return 0;
        }
        return 1;
    }

    private int getStatusColor(String status) {
        if (status == null) return 0xFF80848E;
        switch (status) {
            case User.STATUS_ONLINE: return 0xFF23A559;
            case User.STATUS_IDLE: return 0xFFF0B232;
            case User.STATUS_DND: return 0xFFED4245;
            default: return 0xFF80848E;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Message msg = getItem(position);
        int type = getItemViewType(position);

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (type == 0) {
                convertView = inflater.inflate(R.layout.item_message_sent, parent, false);
            } else {
                convertView = inflater.inflate(R.layout.item_message_received, parent, false);
            }
        }

        ViewHolder holder;
        if (convertView.getTag() instanceof ViewHolder) {
            holder = (ViewHolder) convertView.getTag();
        } else {
            holder = new ViewHolder();
            holder.content = convertView.findViewById(R.id.message_content);
            holder.time = convertView.findViewById(R.id.message_time);
            holder.senderName = convertView.findViewById(R.id.message_sender);
            holder.avatar = convertView.findViewById(R.id.message_avatar);
            holder.imageView = convertView.findViewById(R.id.message_image);
            holder.reactions = convertView.findViewById(R.id.message_reactions);
            holder.avatarStatus = convertView.findViewById(R.id.message_avatar_status);
            holder.nitroBadge = convertView.findViewById(R.id.message_nitro_badge);
            holder.messageStatus = convertView.findViewById(R.id.message_status);
            convertView.setTag(holder);
        }

        if (holder.content != null) {
            String rawContent = msg.getContent() != null ? msg.getContent() : "";
            SpannableString spannable = new SpannableString(rawContent);
            Matcher matcher = Pattern.compile("@\\w+").matcher(rawContent);
            while (matcher.find()) {
                spannable.setSpan(new ForegroundColorSpan(0xFF7983F5),
                    matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            holder.content.setText(spannable);

            // Premium chat color (received messages only)
            if (type == 1 && msg.getSenderId() != null) {
                applyChatColor(msg.getSenderId(), holder.content);
            }
        }

        if (holder.time != null) {
            Date date = msg.getCreatedAt() != null ? msg.getCreatedAt().toDate() : new Date();
            holder.time.setText(timeFormat.format(date));
        }

        // Message status for sent messages
        if (holder.messageStatus != null) {
            if (type == 0) {
                if (msg.getReadAt() != null) {
                    holder.messageStatus.setText("\u2713\u2713");
                    holder.messageStatus.setTextColor(0xFF7983F5);
                    holder.messageStatus.setVisibility(View.VISIBLE);
                } else {
                    holder.messageStatus.setText("\u2713");
                    holder.messageStatus.setTextColor(0xFFB0B8C1);
                    holder.messageStatus.setVisibility(View.VISIBLE);
                }
            } else {
                holder.messageStatus.setVisibility(View.GONE);
            }
        }

        // Sender name with nitro color/font — use profile username, not stored message name
        if (holder.senderName != null) {
            loadSenderStyle(msg.getSenderId(), holder.senderName);
        }

        if (holder.imageView != null && msg.getImageUrl() != null) {
            holder.imageView.setVisibility(View.VISIBLE);
            String url = msg.getImageUrl();
            if (url.toLowerCase().endsWith(".gif")) {
                Glide.with(convertView.getContext())
                    .asGif()
                    .load(url)
                    .into(holder.imageView);
            } else {
                Glide.with(convertView.getContext())
                    .load(url)
                    .into(holder.imageView);
            }
        } else if (holder.imageView != null) {
            holder.imageView.setVisibility(View.GONE);
        }

        // Avatar + status dot + nitro badge + click to profile
        if (holder.avatar != null) {
            String uid = msg.getSenderId();
            String name = msg.getSenderName();
            String avatarUrl = msg.getSenderAvatar();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(convertView.getContext())
                    .load(avatarUrl)
                    .circleCrop()
                    .into(holder.avatar);
            } else {
                String color = uid != null ? AvatarGenerator.getColorForUser(uid) : "#5865F2";
                String text = name != null ? name : "?";
                Bitmap bmp = AvatarGenerator.generate(text, color, 64);
                holder.avatar.setImageBitmap(bmp);
            }

            // Status dot on received message avatars
            if (holder.avatarStatus != null && uid != null && !uid.equals(currentUid)) {
                loadSenderStatus(uid, holder.avatarStatus);
            } else if (holder.avatarStatus != null) {
                holder.avatarStatus.setVisibility(View.GONE);
            }

            // Nitro badge
            if (holder.nitroBadge != null && uid != null) {
                loadNitroBadge(uid, holder.nitroBadge);
            }

            // Avatar click -> profile view
            final String senderId = uid;
            final String senderName = name;
            holder.avatar.setOnClickListener(v -> showProfileDialog(v.getContext(), senderId, senderName));
        }

        // Reactions
        if (holder.reactions != null) {
            Map<String, Integer> reactions = msg.getReactions();
            Map<String, String> userReactions = msg.getUserReactions();
            if (reactions != null && !reactions.isEmpty()) {
                SpannableStringBuilder sb = new SpannableStringBuilder();
                for (Map.Entry<String, Integer> entry : reactions.entrySet()) {
                    if (sb.length() > 0) sb.append("  ");
                    String emoji = entry.getKey();
                    int count = entry.getValue();
                    String text = emoji + " " + count;
                    int start = sb.length();
                    sb.append(text);
                    if (userReactions != null && emoji.equals(userReactions.get(currentUid))) {
                        sb.setSpan(new android.text.style.BackgroundColorSpan(0x337983F5),
                            start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                holder.reactions.setText(sb);
                holder.reactions.setVisibility(View.VISIBLE);
            } else {
                holder.reactions.setVisibility(View.GONE);
            }
        }

        // Long-press for actions
        convertView.setOnLongClickListener(v -> {
            String msgId = msg.getId();

            androidx.appcompat.app.AlertDialog.Builder menu = new androidx.appcompat.app.AlertDialog.Builder(parent.getContext());
            String[] options;
            if (msg.getSenderId() != null && msg.getSenderId().equals(currentUid)) {
                options = new String[]{"Reply", "React", "Delete"};
            } else {
                options = new String[]{"Reply", "React"};
            }

            menu.setItems(options, (dialog, which) -> {
                String selected = options[which];
                if ("Reply".equals(selected) && msgId != null) {
                    if (replyListener != null) {
                        String name = msg.getSenderName() != null ? msg.getSenderName() : "User";
                        replyListener.onReply(msgId, name);
                    }
                } else if ("React".equals(selected) && msgId != null) {
                    showReactionPicker(parent.getContext(), msgId);
                } else if ("Delete".equals(selected) && msgId != null) {
                    if (deleteListener != null) deleteListener.onDelete(msgId);
                }
            });
            menu.show();
            return true;
        });

        return convertView;
    }

    private void loadSenderStyle(String uid, TextView nameView) {
        if (uid == null) return;
        if (senderProfileCache.containsKey(uid)) {
            applySenderStyle(senderProfileCache.get(uid), nameView);
            return;
        }
        FirebaseUtil.getUser(uid).addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null) {
                senderProfileCache.put(uid, user);
                applySenderStyle(user, nameView);
            }
        });
    }

    private void applySenderStyle(User user, TextView nameView) {
        String name = user.getUsername() != null ? user.getUsername() : "Unknown";
        int color = 0xFF7983F5;
        int typeface = Typeface.NORMAL;
        if (user.hasNitro() && user.getUsernameColor() != null) {
            try {
                color = android.graphics.Color.parseColor(user.getUsernameColor());
            } catch (Exception ignored) {}
        }
        if (user.hasNitro() && user.getUsernameFont() != null) {
            switch (user.getUsernameFont()) {
                case "bold": typeface = Typeface.BOLD; break;
                case "italic": typeface = Typeface.ITALIC; break;
                case "serif": typeface = Typeface.BOLD_ITALIC; break;
            }
        }
        nameView.setText(name);
        nameView.setTextColor(color);
        nameView.setTypeface(null, typeface);
    }

    private void loadSenderStatus(String uid, View statusDot) {
        if (senderStatusCache.containsKey(uid)) {
            String status = senderStatusCache.get(uid);
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(getStatusColor(status));
            statusDot.setBackground(dot);
            statusDot.setVisibility(View.VISIBLE);
            return;
        }
        FirebaseUtil.getUser(uid).addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null) {
                String status = user.getStatus() != null ? user.getStatus() : User.STATUS_OFFLINE;
                senderStatusCache.put(uid, status);
                GradientDrawable dot = new GradientDrawable();
                dot.setShape(GradientDrawable.OVAL);
                dot.setColor(getStatusColor(status));
                statusDot.setBackground(dot);
                statusDot.setVisibility(View.VISIBLE);
                notifyDataSetChanged();
            }
        });
    }

    private void loadNitroBadge(String uid, TextView badge) {
        if (senderProfileCache.containsKey(uid)) {
            User user = senderProfileCache.get(uid);
            badge.setVisibility(user.hasNitro() ? View.VISIBLE : View.GONE);
            return;
        }
        FirebaseUtil.getUser(uid).addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null) {
                senderProfileCache.put(uid, user);
                badge.setVisibility(user.hasNitro() ? View.VISIBLE : View.GONE);
                notifyDataSetChanged();
            }
        });
    }

    private void applyChatColor(String uid, TextView contentView) {
        if (senderProfileCache.containsKey(uid)) {
            User user = senderProfileCache.get(uid);
            if (user.hasPremium() && user.getChatColor() != null) {
                try {
                    int c = android.graphics.Color.parseColor(user.getChatColor());
                    contentView.setTextColor(c);
                } catch (Exception ignored) {}
            }
            return;
        }
        FirebaseUtil.getUser(uid).addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null) {
                senderProfileCache.put(uid, user);
                if (user.hasPremium() && user.getChatColor() != null) {
                    try {
                        int c = android.graphics.Color.parseColor(user.getChatColor());
                        contentView.setTextColor(c);
                    } catch (Exception ignored) {}
                }
                notifyDataSetChanged();
            }
        });
    }

    private void showProfileDialog(Context context, String uid, String name) {
        if (uid == null) return;

        final String profileUid = uid;
        Context ctx = dmContext != null ? dmContext : context;
        User cached = senderProfileCache.get(uid);

        if (cached != null) {
            showProfileDialogWithUser(ctx, cached, profileUid);
            return;
        }

        FirebaseUtil.getUser(uid).addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null) {
                senderProfileCache.put(uid, user);
                showProfileDialogWithUser(ctx, user, profileUid);
            }
        });
    }

    private void showProfileDialogWithUser(Context context, User user, String uid) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_profile, null);

        ImageView avatarView = view.findViewById(R.id.profile_avatar);
        TextView nameView = view.findViewById(R.id.profile_username);
        TextView statusView = view.findViewById(R.id.profile_status);
        TextView bioView = view.findViewById(R.id.profile_bio);
        TextView nitroBadgeView = view.findViewById(R.id.profile_nitro_badge);
        TextView nitroExpiryView = view.findViewById(R.id.profile_nitro_expiry);
        TextView premiumBadgeView = view.findViewById(R.id.profile_premium_badge);
        View bannerView = view.findViewById(R.id.profile_banner);
        ImageView bannerImageView = view.findViewById(R.id.profile_banner_image);
        TextView bgEmojiView = view.findViewById(R.id.profile_bg_emoji);
        Button unfriendBtn = view.findViewById(R.id.profile_unfriend_btn);

        // Avatar
        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(context).load(avatarUrl).circleCrop().into(avatarView);
        } else {
            String color = user.getAvatarColor() != null ? user.getAvatarColor() : "#5865F2";
            String text = user.getAvatarText() != null ? user.getAvatarText() :
                (user.getUsername() != null ? user.getUsername() : "?");
            Bitmap bmp = AvatarGenerator.generate(text, color, 128);
            avatarView.setImageBitmap(bmp);
        }

        // Name with nitro style
        String username = user.getUsername() != null ? user.getUsername() : "Unknown";
        int nameColor = 0xFF7983F5;
        int typeface = Typeface.NORMAL;
        if (user.hasNitro() && user.getUsernameColor() != null) {
            try {
                nameColor = android.graphics.Color.parseColor(user.getUsernameColor());
            } catch (Exception ignored) {}
        }
        if (user.hasNitro() && user.getUsernameFont() != null) {
            switch (user.getUsernameFont()) {
                case "bold": typeface = Typeface.BOLD; break;
                case "italic": typeface = Typeface.ITALIC; break;
                case "serif": typeface = Typeface.BOLD_ITALIC; break;
            }
        }
        nameView.setText(username);
        nameView.setTextColor(nameColor);
        nameView.setTypeface(null, typeface);

        // Status
        String status = user.getStatus() != null ? user.getStatus() : User.STATUS_OFFLINE;
        String statusText;
        int statusColor;
        switch (status) {
            case User.STATUS_ONLINE: statusText = "Online"; statusColor = 0xFF23A559; break;
            case User.STATUS_IDLE: statusText = "Idle"; statusColor = 0xFFF0B232; break;
            case User.STATUS_DND: statusText = "Do Not Disturb"; statusColor = 0xFFED4245; break;
            default: statusText = "Offline"; statusColor = 0xFF80848E;
        }
        statusView.setText(statusText);
        statusView.setTextColor(statusColor);

        // Bio
        if (user.getBio() != null && !user.getBio().isEmpty()) {
            bioView.setText(user.getBio());
            bioView.setVisibility(View.VISIBLE);
        } else {
            bioView.setVisibility(View.GONE);
        }

        // Premium background
        if (user.hasPremium()) {
            String bannerUrl = user.getProfileBannerUrl();
            if (bannerUrl != null && !bannerUrl.isEmpty()) {
                bannerImageView.setVisibility(View.VISIBLE);
                Glide.with(context).load(bannerUrl).into(bannerImageView);
            } else {
                bannerImageView.setVisibility(View.GONE);
            }
            if (user.getProfileBackgroundColor() != null) {
                try {
                    bannerView.setBackgroundColor(android.graphics.Color.parseColor(user.getProfileBackgroundColor()));
                } catch (Exception ignored) {}
            }
            if (user.getProfileBackgroundEmoji() != null && !user.getProfileBackgroundEmoji().isEmpty()) {
                bgEmojiView.setText(user.getProfileBackgroundEmoji());
                double opacity = user.getProfileBackgroundEmojiOpacity();
                if (opacity <= 0) opacity = 0.12;
                bgEmojiView.setAlpha((float) opacity);
                bgEmojiView.setVisibility(View.VISIBLE);
            }
        }

        // Nitro
        if (user.hasNitro()) {
            nitroBadgeView.setVisibility(View.VISIBLE);
            String expiry = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(user.getNitroExpiresAt().toDate());
            nitroExpiryView.setText("Expires " + expiry);
            nitroExpiryView.setVisibility(View.VISIBLE);

            if (user.hasPremium()) {
                premiumBadgeView.setVisibility(View.VISIBLE);
            } else {
                premiumBadgeView.setVisibility(View.GONE);
            }
        } else {
            nitroBadgeView.setVisibility(View.GONE);
            nitroExpiryView.setVisibility(View.GONE);
            premiumBadgeView.setVisibility(View.GONE);
        }

        // Unfriend button (only for other users)
        if (unfriendBtn != null) {
            if (uid != null && !uid.equals(currentUid)) {
                unfriendBtn.setVisibility(View.VISIBLE);
                unfriendBtn.setOnClickListener(v -> {
                    new androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("Unfriend " + (user.getUsername() != null ? user.getUsername() : "User") + "?")
                        .setPositiveButton("Unfriend", (d, w) -> {
                            new FriendRepository().removeFriend(uid);
                            Toast.makeText(context, "Unfriended", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                });
            } else {
                unfriendBtn.setVisibility(View.GONE);
            }
        }

        new AlertDialog.Builder(context)
            .setView(view)
            .setPositiveButton("Close", null)
            .show();
    }

    private void showReactionPicker(Context context, String messageId) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 8, 16, 8);

        LinearLayout emojiRow = new LinearLayout(context);
        emojiRow.setOrientation(LinearLayout.HORIZONTAL);
        for (String emoji : REACTION_EMOJIS) {
            Button btn = new Button(context);
            btn.setText(emoji);
            btn.setTextSize(20);
            btn.setBackgroundResource(android.R.color.transparent);
            btn.setOnClickListener(v -> {
                messageRepo.addReaction(messageId, emoji, currentUid);
            });
            emojiRow.addView(btn);
        }
        root.addView(emojiRow);

        final EditText customInput = new EditText(context);
        customInput.setHint("Type custom emoji or text...");
        customInput.setTextSize(14);
        customInput.setPadding(8, 8, 8, 8);
        root.addView(customInput);

        Button customBtn = new Button(context);
        customBtn.setText("React");
        customBtn.setOnClickListener(v -> {
            String text = customInput.getText().toString().trim();
            if (!text.isEmpty()) {
                messageRepo.addReaction(messageId, text, currentUid);
            }
        });
        root.addView(customBtn);

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        builder.setTitle("Add Reaction");
        builder.setView(root);
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    static class ViewHolder {
        TextView content;
        TextView time;
        TextView senderName;
        ImageView avatar;
        View avatarStatus;
        TextView nitroBadge;
        ImageView imageView;
        TextView reactions;
        TextView messageStatus;
    }
}