package com.discordclone.repository;

import android.net.Uri;
import android.util.Log;

import com.discordclone.models.Message;
import com.discordclone.models.User;
import com.discordclone.utils.FirebaseUtil;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageRepository {
    private static final String TAG = "MessageRepository";

    private String getSenderName() {
        FirebaseUser user = FirebaseUtil.getAuth().getCurrentUser();
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            return user.getDisplayName();
        }
        return null;
    }

    public Task<String> sendMessage(String channelId, String content) {
        return sendMessage(channelId, content, null, null);
    }

    public Task<String> sendMessage(String channelId, String content, String replyToId) {
        return sendMessage(channelId, content, null, replyToId);
    }

    public Task<String> sendMessage(String channelId, String content, List<String> mentions) {
        return sendMessage(channelId, content, mentions, null);
    }

    public Task<String> sendMessage(String channelId, String content, List<String> mentions, String replyToId) {
        final String senderId = FirebaseUtil.getCurrentUid();
        String sName = getSenderName();
        if (sName != null) {
            return doSendMessage(channelId, content, mentions, replyToId, senderId, sName);
        }
        return FirebaseUtil.getUser(senderId).continueWithTask(fetchTask -> {
            String name = "Unknown";
            if (fetchTask.isSuccessful() && fetchTask.getResult() != null) {
                User u = fetchTask.getResult().toObject(User.class);
                if (u != null && u.getUsername() != null) name = u.getUsername();
            }
            return doSendMessage(channelId, content, mentions, replyToId, senderId, name);
        });
    }

    private Task<String> doSendMessage(String channelId, String content, List<String> mentions, String replyToId,
                                        String senderId, String senderName) {
        Message message = new Message(channelId, senderId, senderName, content);
        if (mentions != null && !mentions.isEmpty()) {
            message.setMentions(mentions);
        }
        if (replyToId != null) {
            message.setReplyToId(replyToId);
        }
        final com.google.firebase.firestore.DocumentReference docRef = FirebaseUtil.messagesRef().document();
        final String messageId = docRef.getId();
        message.setId(messageId);

        Task<String> sendTask = docRef.set(message)
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    return messageId;
                }
                throw task.getException();
            });

        // Track unread for DM channels
        if (channelId != null && channelId.startsWith("dm_")) {
            String recipientId = FirebaseUtil.getRecipientIdFromDmChannel(channelId);
            if (recipientId != null) {
                FirebaseUtil.incrementUnreadCount(recipientId, channelId);
            }
        }

        return sendTask;
    }

    public Task<String> sendGif(String channelId, String gifUrl) {
        return sendGif(channelId, gifUrl, null);
    }

    public Task<String> sendGif(String channelId, String gifUrl, String replyToId) {
        final String senderId = FirebaseUtil.getCurrentUid();
        String sName = getSenderName();
        if (sName != null) {
            return doSendGif(channelId, gifUrl, replyToId, senderId, sName);
        }
        return FirebaseUtil.getUser(senderId).continueWithTask(fetchTask -> {
            String name = "Unknown";
            if (fetchTask.isSuccessful() && fetchTask.getResult() != null) {
                User u = fetchTask.getResult().toObject(User.class);
                if (u != null && u.getUsername() != null) name = u.getUsername();
            }
            return doSendGif(channelId, gifUrl, replyToId, senderId, name);
        });
    }

    private Task<String> doSendGif(String channelId, String gifUrl, String replyToId,
                                    String senderId, String senderName) {
        Message message = new Message(channelId, senderId, senderName, "");
        message.setImageUrl(gifUrl);
        if (replyToId != null) {
            message.setReplyToId(replyToId);
        }
        final com.google.firebase.firestore.DocumentReference docRef = FirebaseUtil.messagesRef().document();
        final String messageId = docRef.getId();
        message.setId(messageId);

        Task<String> sendTask = docRef.set(message)
            .continueWith(task -> {
                if (task.isSuccessful()) return messageId;
                throw task.getException();
            });

        if (channelId != null && channelId.startsWith("dm_")) {
            String recipientId = FirebaseUtil.getRecipientIdFromDmChannel(channelId);
            if (recipientId != null) {
                FirebaseUtil.incrementUnreadCount(recipientId, channelId);
            }
        }

        return sendTask;
    }

    public Task<Void> sendImage(String channelId, Uri imageUri) {
        final String senderId = FirebaseUtil.getCurrentUid();
        String sName = getSenderName();
        if (sName != null) {
            return doSendImage(channelId, imageUri, senderId, sName);
        }
        return FirebaseUtil.getUser(senderId).continueWithTask(fetchTask -> {
            String name = "Unknown";
            if (fetchTask.isSuccessful() && fetchTask.getResult() != null) {
                User u = fetchTask.getResult().toObject(User.class);
                if (u != null && u.getUsername() != null) name = u.getUsername();
            }
            return doSendImage(channelId, imageUri, senderId, name);
        });
    }

    private Task<Void> doSendImage(String channelId, Uri imageUri, String senderId, String senderName) {
        final String fileName = "img_" + System.currentTimeMillis() + ".jpg";
        return FirebaseUtil.attachmentsRef().child(fileName).putFile(imageUri)
            .continueWithTask(task -> {
                if (task.isSuccessful()) {
                    return FirebaseUtil.attachmentsRef().child(fileName).getDownloadUrl();
                }
                if (task.getException() != null) throw task.getException();
                return null;
            })
            .continueWithTask(task -> {
                if (task.isSuccessful()) {
                    String imageUrl = task.getResult().toString();
                    Message msg = new Message(channelId, senderId, senderName, "");
                    msg.setImageUrl(imageUrl);
                    return FirebaseUtil.messagesRef().document().set(msg);
                }
                if (task.getException() != null) throw task.getException();
                return null;
            });
    }

    public boolean isStorageAvailable() {
        return true;
    }

    public Task<QuerySnapshot> getMessages(String channelId) {
        return FirebaseUtil.messagesForChannel(channelId).get();
    }

    public Task<Void> deleteMessage(String messageId) {
        return FirebaseUtil.messagesRef().document(messageId).delete();
    }

    public Task<Void> editMessage(String messageId, String newContent) {
        return FirebaseUtil.messagesRef().document(messageId)
            .update("content", newContent, "editedAt", com.google.firebase.Timestamp.now());
    }

    public Task<Void> addReaction(String messageId, String emoji, String userId) {
        com.google.firebase.firestore.DocumentReference ref = FirebaseUtil.messagesRef().document(messageId);
        return ref.get().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                throw task.getException() != null ? task.getException() : new Exception("Message not found");
            }
            Message msg = task.getResult().toObject(Message.class);
            Map<String, String> userReactions = msg.getUserReactions();
            if (userReactions == null) userReactions = new HashMap<>();

            String existingEmoji = userReactions.get(userId);
            Map<String, Object> updates = new HashMap<>();

            if (existingEmoji != null) {
                if (existingEmoji.equals(emoji)) {
                    updates.put("reactions." + emoji, com.google.firebase.firestore.FieldValue.increment(-1));
                    updates.put("userReactions." + userId, com.google.firebase.firestore.FieldValue.delete());
                } else {
                    updates.put("reactions." + existingEmoji, com.google.firebase.firestore.FieldValue.increment(-1));
                    updates.put("reactions." + emoji, com.google.firebase.firestore.FieldValue.increment(1));
                    updates.put("userReactions." + userId, emoji);
                }
            } else {
                updates.put("reactions." + emoji, com.google.firebase.firestore.FieldValue.increment(1));
                updates.put("userReactions." + userId, emoji);
            }
            return ref.update(updates);
        });
    }

    public List<Message> parseMessages(QuerySnapshot snapshot) {
        List<Message> messages = new ArrayList<>();
        if (snapshot != null) {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Message msg = doc.toObject(Message.class);
                if (msg != null) {
                    msg.setId(doc.getId());
                    messages.add(msg);
                }
            }
        }
        Collections.sort(messages, Comparator.comparing(m -> m.getCreatedAt() != null ? m.getCreatedAt() : new com.google.firebase.Timestamp(0, 0)));
        return messages;
    }
}
