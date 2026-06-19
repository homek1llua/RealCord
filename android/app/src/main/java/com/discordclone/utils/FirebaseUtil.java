package com.discordclone.utils;

import android.util.Log;

import com.discordclone.models.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FirebaseUtil {
    private static final String TAG = "FirebaseUtil";
    private static final boolean USE_EMULATOR = false;

    private static FirebaseAuth auth;
    private static FirebaseFirestore db;
    private static FirebaseStorage storage;
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        if (USE_EMULATOR) {
            try {
                auth.useEmulator("10.0.2.2", 9099);
                db.useEmulator("10.0.2.2", 8080);
                storage.useEmulator("10.0.2.2", 9199);
                Log.d(TAG, "Firebase emulators configured");
            } catch (Exception e) {
                Log.w(TAG, "Failed to configure emulators", e);
                try {
                    auth.useEmulator("127.0.0.1", 9099);
                    db.useEmulator("127.0.0.1", 8080);
                    storage.useEmulator("127.0.0.1", 9199);
                    Log.d(TAG, "Firebase emulators configured on localhost");
                } catch (Exception e2) {
                    Log.w(TAG, "Failed to configure emulators on localhost", e2);
                }
            }
        }
        initialized = true;
    }

    public static FirebaseAuth getAuth() {
        if (!initialized) init();
        return auth;
    }

    public static FirebaseFirestore getDb() {
        if (!initialized) init();
        return db;
    }

    public static FirebaseStorage getStorage() {
        if (!initialized) init();
        return storage;
    }

    public static String getCurrentUid() {
        FirebaseUser user = getAuth().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public static boolean isLoggedIn() {
        return getAuth().getCurrentUser() != null;
    }

    // Collection references
    public static CollectionReference usersRef() {
        return getDb().collection("users");
    }

    public static DocumentReference currentUserRef() {
        String uid = getCurrentUid();
        return uid != null ? usersRef().document(uid) : null;
    }

    public static CollectionReference friendsRef() {
        return getDb().collection("friends");
    }

    public static DocumentReference updatesRef() {
        return getDb().collection("updates").document("latest");
    }

    public static CollectionReference friendRequestsRef() {
        return getDb().collection("friendRequests");
    }

    public static CollectionReference serversRef() {
        return getDb().collection("servers");
    }

    public static CollectionReference channelsRef() {
        return getDb().collection("channels");
    }

    public static CollectionReference messagesRef() {
        return getDb().collection("messages");
    }

    public static CollectionReference callsRef() {
        return getDb().collection("calls");
    }

    public static StorageReference avatarsRef() {
        return getStorage().getReference().child("avatars");
    }

    public static StorageReference attachmentsRef() {
        return getStorage().getReference().child("attachments");
    }

    // Query helpers
    public static Query friendsForUser(String userId) {
        return friendsRef().whereEqualTo("userId", userId);
    }

    public static Query friendRequestsForUser(String userId) {
        return friendRequestsRef().whereEqualTo("toId", userId).whereEqualTo("status", "pending");
    }

    public static Query sentFriendRequests(String userId) {
        return friendRequestsRef().whereEqualTo("fromId", userId).whereEqualTo("status", "pending");
    }

    public static Query messagesForChannel(String channelId) {
        return messagesRef().whereEqualTo("channelId", channelId);
    }

    public static Query serversForUser(String userId) {
        return serversRef().whereArrayContains("memberIds", userId);
    }

    public static Query channelsForServer(String serverId) {
        return channelsRef().whereEqualTo("serverId", serverId).orderBy("position");
    }

    public static Query usersByUsername(String username) {
        return usersRef().whereEqualTo("username", username);
    }

    // Task helpers
    public static Task<DocumentSnapshot> getUser(String uid) {
        return usersRef().document(uid).get();
    }

    public static void updateUserStatus(String status) {
        DocumentReference ref = currentUserRef();
        if (ref != null) {
            ref.update("status", status);
        }
    }

    public static void updateLastSeen() {
        DocumentReference ref = currentUserRef();
        if (ref != null) {
            ref.update("lastSeen", System.currentTimeMillis());
        }
    }

    public static String getRecipientIdFromDmChannel(String channelId) {
        String currentUid = getCurrentUid();
        if (currentUid == null || channelId == null || !channelId.startsWith("dm_")) return null;
        String ids = channelId.substring(3);
        String[] parts = ids.split("_");
        if (parts.length != 2) return null;
        return parts[0].equals(currentUid) ? parts[1] : parts[0];
    }

    public static void incrementUnreadCount(String recipientId, String channelId) {
        if (recipientId == null) return;
        usersRef().document(recipientId)
            .update("unreadCounts." + channelId, com.google.firebase.firestore.FieldValue.increment(1));
    }

    public static void resetUnreadCount(String channelId) {
        DocumentReference ref = currentUserRef();
        if (ref != null && channelId != null) {
            ref.update("unreadCounts." + channelId, 0);
        }
    }
}
