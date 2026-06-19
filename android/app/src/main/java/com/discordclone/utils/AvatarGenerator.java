package com.discordclone.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.Random;

public class AvatarGenerator {

    private static final String[] DEFAULT_COLORS = {
        "#5865F2", "#ED4245", "#23A559", "#F0B232",
        "#9B59B6", "#1ABC9C", "#E67E22", "#3498DB",
        "#E91E63", "#00BCD4", "#FF5722", "#795548"
    };

    public static String getColorForUser(String uid) {
        int idx = Math.abs(uid.hashCode()) % DEFAULT_COLORS.length;
        return DEFAULT_COLORS[idx];
    }

    public static Bitmap generate(String text, String colorHex, int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        int color;
        try {
            color = Color.parseColor(colorHex);
        } catch (Exception e) {
            color = Color.parseColor(DEFAULT_COLORS[new Random().nextInt(DEFAULT_COLORS.length)]);
        }

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(color);
        canvas.drawOval(new RectF(0, 0, size, size), bgPaint);

        String display = text != null && text.length() > 0 ? text.substring(0, 1).toUpperCase() : "?";

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(size * 0.5f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        float x = size / 2f;
        float y = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(display, x, y, textPaint);

        return bitmap;
    }
}
