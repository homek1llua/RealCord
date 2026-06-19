package com.discordclone.utils;

import android.util.Log;

import com.discordclone.models.User;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NitroManager {
    private static final String TAG = "NitroManager";
    private static final String GOD_USERNAME = "it";

    public static boolean canGrantNitro(String currentUsername) {
        return currentUsername != null && currentUsername.equalsIgnoreCase(GOD_USERNAME);
    }

    public static class NitroCommand {
        public String targetUsername;
        public long durationMs;
        public String error;
        public boolean isSelf;
        public String tier = User.NITRO_NORMAL;
    }

    public static NitroCommand parseCommand(String content) {
        NitroCommand cmd = new NitroCommand();
        String trimmed = content.trim();

        if (trimmed.startsWith("/nitros ")) {
            cmd.isSelf = true;
            String[] parts = trimmed.split("\\s+", 3);
            if (parts.length < 2) {
                cmd.error = "Usage: /nitros <duration> [normal/premium] (e.g. /nitros 30d premium)";
                return cmd;
            }
            long dur = parseDuration(parts[1]);
            if (dur <= 0) {
                cmd.error = "Invalid duration. Use e.g. 30d, 7d, 24h";
                return cmd;
            }
            cmd.durationMs = dur;
            if (parts.length >= 3) {
                String tier = parts[2].toLowerCase();
                if (!tier.equals(User.NITRO_NORMAL) && !tier.equals(User.NITRO_PREMIUM)) {
                    cmd.error = "Tier must be 'normal' or 'premium'";
                    return cmd;
                }
                cmd.tier = tier;
            }
            return cmd;
        }

        if (trimmed.startsWith("/nitro ")) {
            cmd.isSelf = false;
            String[] parts = trimmed.split("\\s+", 4);
            if (parts.length < 3) {
                cmd.error = "Usage: /nitro @username <duration> [normal/premium] (e.g. /nitro @john 30d premium)";
                return cmd;
            }
            cmd.targetUsername = parts[1].replace("@", "");
            long dur = parseDuration(parts[2]);
            if (dur <= 0) {
                cmd.error = "Invalid duration. Use e.g. 30d, 7d, 24h";
                return cmd;
            }
            cmd.durationMs = dur;
            if (parts.length >= 4) {
                String tierStr = parts[3].toLowerCase();
                if (!tierStr.equals(User.NITRO_NORMAL) && !tierStr.equals(User.NITRO_PREMIUM)) {
                    cmd.error = "Tier must be 'normal' or 'premium'";
                    return cmd;
                }
                cmd.tier = tierStr;
            }
            return cmd;
        }

        cmd.error = "Not a nitro command";
        return cmd;
    }

    private static long parseDuration(String input) {
        if (input == null || input.isEmpty()) return -1;
        input = input.trim().toLowerCase();
        try {
            if (input.endsWith("d")) {
                return Long.parseLong(input.substring(0, input.length() - 1)) * 24L * 60 * 60 * 1000;
            } else if (input.endsWith("h")) {
                return Long.parseLong(input.substring(0, input.length() - 1)) * 60L * 60 * 1000;
            } else if (input.endsWith("m")) {
                return Long.parseLong(input.substring(0, input.length() - 1)) * 60L * 1000;
            }
            return Long.parseLong(input) * 24L * 60 * 60 * 1000;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static void grantNitro(String targetUid, long durationMs, String tier) {
        Date expiresAt = new Date(System.currentTimeMillis() + durationMs);
        Timestamp ts = new Timestamp(expiresAt);

        String[] colors = {"#FF6B6B", "#FFA94D", "#FFD43B", "#69DB7C", "#4DABF7", "#9775FA", "#F783AC", "#20C997"};
        String randomColor = colors[(int)(System.currentTimeMillis() % colors.length)];
        String[] fonts = {"bold", "italic", "serif", "monospace"};
        String randomFont = fonts[(int)((System.currentTimeMillis() / 1000) % fonts.length)];

        String[] gradients = {"#FF6B6B:#FFD43B", "#4DABF7:#9775FA", "#69DB7C:#20C997", "#F783AC:#FFD43B"};
        String randomGrad = gradients[(int)((System.currentTimeMillis() / 10000) % gradients.length)];

        Map<String, Object> updates = new HashMap<>();
        updates.put("nitroExpiresAt", ts);
        updates.put("nitroTier", tier);
        updates.put("usernameColor", randomColor);
        updates.put("usernameFont", randomFont);

        if (User.NITRO_PREMIUM.equals(tier)) {
            String[] gradParts = randomGrad.split(":");
            updates.put("usernameGradientStart", gradParts[0]);
            updates.put("usernameGradientEnd", gradParts[1]);
            updates.put("chatColor", null);
            updates.put("chatGradientStart", null);
            updates.put("chatGradientEnd", null);
            updates.put("profileBackgroundColor", "#1E1E2E");
            updates.put("profileBackgroundEmoji", "⭐");
            updates.put("profileBackgroundEmojiOpacity", 0.12);
            updates.put("profileBannerUrl", null);
        } else {
            updates.put("usernameGradientStart", null);
            updates.put("usernameGradientEnd", null);
            updates.put("chatColor", null);
            updates.put("chatGradientStart", null);
            updates.put("chatGradientEnd", null);
            updates.put("profileBackgroundColor", null);
            updates.put("profileBackgroundEmoji", null);
        }
        FirebaseUtil.usersRef().document(targetUid).update(updates);
    }

    public static void revokeNitro(String uid) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("nitroExpiresAt", null);
        updates.put("nitroTier", null);
        updates.put("usernameColor", null);
        updates.put("usernameFont", null);
        updates.put("usernameGradientStart", null);
        updates.put("usernameGradientEnd", null);
        updates.put("chatColor", null);
        updates.put("chatGradientStart", null);
        updates.put("chatGradientEnd", null);
        updates.put("profileBackgroundColor", null);
        updates.put("profileBackgroundEmoji", null);
        updates.put("profileBackgroundEmojiOpacity", null);
        FirebaseUtil.usersRef().document(uid).update(updates);
    }

    public static String getFontStyleTag(String font) {
        if (font == null) return null;
        switch (font) {
            case "bold": return "b";
            case "italic": return "i";
            case "serif": return "s";
            case "monospace": return "tt";
            default: return null;
        }
    }
}