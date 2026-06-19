package com.discordclone.models;

import com.google.firebase.Timestamp;

public class Channel {
    private String id;
    private String serverId;
    private String name;
    private String type;
    private int position;
    private Timestamp createdAt;

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_VOICE = "voice";

    public Channel() {}

    public Channel(String serverId, String name, String type) {
        this.serverId = serverId;
        this.name = name;
        this.type = type;
        this.createdAt = Timestamp.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
