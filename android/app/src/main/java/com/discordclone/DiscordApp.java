package com.discordclone;

import android.app.Application;

import androidx.multidex.MultiDex;

import com.discordclone.utils.FirebaseUtil;

public class DiscordApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MultiDex.install(this);
        FirebaseUtil.init();
    }
}
