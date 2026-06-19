package com.discordclone.repository;

import android.net.Uri;
import android.util.Log;

import com.discordclone.models.User;
import com.discordclone.utils.FirebaseUtil;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {
    private static final String TAG = "UserRepository";

    public Task<AuthResult> register(String email, String password, String username) {
        return FirebaseUtil.getAuth().createUserWithEmailAndPassword(email, password)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                FirebaseUser fUser = task.getResult().getUser();
                User newUser = new User(fUser.getUid(), username, email);
                return FirebaseUtil.usersRef().document(fUser.getUid()).set(newUser)
                    .continueWithTask(t -> fUser.updateProfile(
                        new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(username).build()
                    ))
                    .continueWith(t -> task.getResult());
            });
    }

    public Task<AuthResult> login(String email, String password) {
        return FirebaseUtil.getAuth().signInWithEmailAndPassword(email, password);
    }

    public void logout() {
        FirebaseUtil.updateUserStatus(User.STATUS_OFFLINE);
        FirebaseUtil.getAuth().signOut();
    }

    public Task<DocumentSnapshot> getUser(String uid) {
        return FirebaseUtil.usersRef().document(uid).get();
    }

    public Task<QuerySnapshot> searchUsers(String query) {
        String lowerQuery = query.toLowerCase();
        return FirebaseUtil.usersRef()
            .orderBy("username")
            .startAt(lowerQuery)
            .endAt(lowerQuery + "\uf8ff")
            .get();
    }

    public Task<QuerySnapshot> getAllUsers() {
        return FirebaseUtil.usersRef().get();
    }

    public Task<Void> updateUsername(String newUsername) {
        return FirebaseUtil.currentUserRef().update("username", newUsername);
    }

    public Task<Void> updateBio(String bio) {
        return FirebaseUtil.currentUserRef().update("bio", bio);
    }

    public Task<Void> updateStatus(String status) {
        return FirebaseUtil.currentUserRef().update("status", status);
    }

    public UploadTask uploadAvatar(Uri imageUri) {
        String uid = FirebaseUtil.getCurrentUid();
        return FirebaseUtil.avatarsRef().child(uid + ".jpg").putFile(imageUri);
    }

    public void setAvatarUrl(String url) {
        FirebaseUtil.currentUserRef().update("avatarUrl", url);
    }

    public void updateProfile(String bio, String avatarColor, String avatarText, String usernameColor, String usernameFont) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("bio", bio);
        updates.put("avatarColor", avatarColor);
        updates.put("avatarText", avatarText);
        if (usernameColor != null) updates.put("usernameColor", usernameColor);
        if (usernameFont != null) updates.put("usernameFont", usernameFont);
        FirebaseUtil.currentUserRef().update(updates);
    }

    public void updateFullProfile(
            String bio, String avatarColor, String avatarText,
            String usernameColor, String usernameFont,
            String usernameGradientStart, String usernameGradientEnd,
            String chatColor, String chatGradientStart, String chatGradientEnd,
            String profileBackgroundColor, String profileBackgroundEmoji, double profileBackgroundEmojiOpacity,
            String profileBannerUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("bio", bio);
        updates.put("avatarColor", avatarColor);
        updates.put("avatarText", avatarText);
        if (usernameColor != null) updates.put("usernameColor", usernameColor);
        if (usernameFont != null) updates.put("usernameFont", usernameFont);
        updates.put("usernameGradientStart", usernameGradientStart);
        updates.put("usernameGradientEnd", usernameGradientEnd);
        if (chatColor != null) updates.put("chatColor", chatColor);
        else updates.put("chatColor", null);
        updates.put("chatGradientStart", chatGradientStart);
        updates.put("chatGradientEnd", chatGradientEnd);
        if (profileBackgroundColor != null) updates.put("profileBackgroundColor", profileBackgroundColor);
        if (profileBackgroundEmoji != null) updates.put("profileBackgroundEmoji", profileBackgroundEmoji);
        else updates.put("profileBackgroundEmoji", null);
        updates.put("profileBackgroundEmojiOpacity", profileBackgroundEmojiOpacity);
        if (profileBannerUrl != null) updates.put("profileBannerUrl", profileBannerUrl);
        else updates.put("profileBannerUrl", null);
        FirebaseUtil.currentUserRef().update(updates);
    }

    public List<User> parseUsers(QuerySnapshot snapshot) {
        List<User> users = new ArrayList<>();
        if (snapshot != null) {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                User user = doc.toObject(User.class);
                if (user != null) {
                    user.setUid(doc.getId());
                    users.add(user);
                }
            }
        }
        return users;
    }
}
