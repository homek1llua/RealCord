package com.discordclone.models;

import com.google.firebase.Timestamp;

public class Friend {
    private String id;
    private String userId;
    private String friendId;
    private Timestamp addedAt;

    public Friend() {}

    public Friend(String userId, String friendId) {
        this.userId = userId;
        this.friendId = friendId;
        this.addedAt = Timestamp.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFriendId() { return friendId; }
    public void setFriendId(String friendId) { this.friendId = friendId; }

    public Timestamp getAddedAt() { return addedAt; }
    public void setAddedAt(Timestamp addedAt) { this.addedAt = addedAt; }
}
