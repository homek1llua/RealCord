package com.discordclone.models;

import com.google.firebase.Timestamp;

public class User {
    private String uid;
    private String username;
    private String email;
    private String avatarUrl;
    private String status;
    private String bio;
    private Timestamp createdAt;
    private long lastSeen;
    private String avatarColor;
    private String avatarText;
    private Timestamp nitroExpiresAt;
    private String usernameColor;
    private String usernameFont;
    private String nitroTier;
    private String usernameGradientStart;
    private String usernameGradientEnd;
    private String chatColor;
    private String chatGradientStart;
    private String chatGradientEnd;
    private String profileBackgroundColor;
    private String profileBackgroundEmoji;
    private double profileBackgroundEmojiOpacity;
    private String profileBannerUrl;

    public static final String STATUS_ONLINE = "online";
    public static final String STATUS_IDLE = "idle";
    public static final String STATUS_DND = "dnd";
    public static final String STATUS_OFFLINE = "offline";

    public static final String NITRO_NORMAL = "normal";
    public static final String NITRO_PREMIUM = "premium";

    public User() {}

    public User(String uid, String username, String email) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.status = STATUS_ONLINE;
        this.createdAt = Timestamp.now();
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    public String getAvatarColor() { return avatarColor; }
    public void setAvatarColor(String avatarColor) { this.avatarColor = avatarColor; }

    public String getAvatarText() { return avatarText; }
    public void setAvatarText(String avatarText) { this.avatarText = avatarText; }

    public Timestamp getNitroExpiresAt() { return nitroExpiresAt; }
    public void setNitroExpiresAt(Timestamp nitroExpiresAt) { this.nitroExpiresAt = nitroExpiresAt; }

    public String getUsernameColor() { return usernameColor; }
    public void setUsernameColor(String usernameColor) { this.usernameColor = usernameColor; }

    public String getUsernameFont() { return usernameFont; }
    public void setUsernameFont(String usernameFont) { this.usernameFont = usernameFont; }

    public String getNitroTier() { return nitroTier; }
    public void setNitroTier(String nitroTier) { this.nitroTier = nitroTier; }

    public String getUsernameGradientStart() { return usernameGradientStart; }
    public void setUsernameGradientStart(String v) { this.usernameGradientStart = v; }

    public String getUsernameGradientEnd() { return usernameGradientEnd; }
    public void setUsernameGradientEnd(String v) { this.usernameGradientEnd = v; }

    public String getChatColor() { return chatColor; }
    public void setChatColor(String v) { this.chatColor = v; }

    public String getChatGradientStart() { return chatGradientStart; }
    public void setChatGradientStart(String v) { this.chatGradientStart = v; }

    public String getChatGradientEnd() { return chatGradientEnd; }
    public void setChatGradientEnd(String v) { this.chatGradientEnd = v; }

    public String getProfileBackgroundColor() { return profileBackgroundColor; }
    public void setProfileBackgroundColor(String v) { this.profileBackgroundColor = v; }

    public String getProfileBackgroundEmoji() { return profileBackgroundEmoji; }
    public void setProfileBackgroundEmoji(String v) { this.profileBackgroundEmoji = v; }

    public double getProfileBackgroundEmojiOpacity() { return profileBackgroundEmojiOpacity; }
    public void setProfileBackgroundEmojiOpacity(double v) { this.profileBackgroundEmojiOpacity = v; }

    public String getProfileBannerUrl() { return profileBannerUrl; }
    public void setProfileBannerUrl(String v) { this.profileBannerUrl = v; }

    public boolean hasNitro() {
        return nitroExpiresAt != null && nitroExpiresAt.toDate().after(new java.util.Date());
    }

    public boolean hasPremium() {
        return hasNitro() && NITRO_PREMIUM.equals(nitroTier);
    }
}