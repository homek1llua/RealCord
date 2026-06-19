package com.discordclone.models;

import com.google.firebase.Timestamp;

public class FriendRequest {
    private String id;
    private String fromId;
    private String toId;
    private String status;
    private Timestamp createdAt;

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_REJECTED = "rejected";

    public FriendRequest() {}

    public FriendRequest(String fromId, String toId) {
        this.fromId = fromId;
        this.toId = toId;
        this.status = STATUS_PENDING;
        this.createdAt = Timestamp.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFromId() { return fromId; }
    public void setFromId(String fromId) { this.fromId = fromId; }

    public String getToId() { return toId; }
    public void setToId(String toId) { this.toId = toId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
