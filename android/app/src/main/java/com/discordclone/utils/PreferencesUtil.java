package com.discordclone.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesUtil {
    private static final String PREF_NAME = "discord_clone_prefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_AVATAR = "avatar_url";
    private static final String KEY_THEME = "theme";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";

    private SharedPreferences prefs;

    public PreferencesUtil(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public void saveEmail(String email) {
        prefs.edit().putString(KEY_EMAIL, email).apply();
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public void saveAvatarUrl(String url) {
        prefs.edit().putString(KEY_AVATAR, url).apply();
    }

    public String getAvatarUrl() {
        return prefs.getString(KEY_AVATAR, "");
    }

    public void setTheme(String theme) {
        prefs.edit().putString(KEY_THEME, theme).apply();
    }

    public String getTheme() {
        return prefs.getString(KEY_THEME, "dark");
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }

    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATIONS, true);
    }

    public void setLastRead(String channelId) {
        prefs.edit().putLong("last_read_" + channelId, System.currentTimeMillis()).apply();
    }

    public long getLastRead(String channelId) {
        return prefs.getLong("last_read_" + channelId, 0);
    }

    public void markChannelRead(String channelId) {
        setLastRead(channelId);
    }

    public boolean hasUnread(String channelId) {
        return false; // This will be checked dynamically per friend
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
