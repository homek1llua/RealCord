package com.discordclone.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.discordclone.MainActivity;
import com.discordclone.R;
import com.discordclone.chat.DMChatActivity;
import com.discordclone.calls.CallActivity;
import com.discordclone.models.CallOffer;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

public class FCMService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "discord_clone_messages";
    private static final String CHANNEL_CALLS = "discord_clone_calls";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        String uid = FirebaseUtil.getCurrentUid();
        if (uid != null) {
            FirebaseUtil.currentUserRef().update("fcmToken", token);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        String type = message.getData().get("type");

        if ("call".equals(type)) {
            handleCallNotification(message);
        } else {
            handleMessageNotification(message);
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel msgChannel = new NotificationChannel(
                CHANNEL_ID, "Messages", NotificationManager.IMPORTANCE_HIGH);
            msgChannel.setDescription("New messages");
            getSystemService(NotificationManager.class).createNotificationChannel(msgChannel);

            NotificationChannel callChannel = new NotificationChannel(
                CHANNEL_CALLS, "Calls", NotificationManager.IMPORTANCE_HIGH);
            callChannel.setDescription("Incoming calls");
            getSystemService(NotificationManager.class).createNotificationChannel(callChannel);
        }
    }

    private void handleMessageNotification(RemoteMessage message) {
        createNotificationChannels();
        String title = message.getData().get("title");
        String body = message.getData().get("body");
        String channelId = message.getData().get("channelId");

        Intent intent = new Intent(this, DMChatActivity.class);
        intent.putExtra("channelId", channelId);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build();

        getSystemService(NotificationManager.class).notify((int) System.currentTimeMillis(), notification);
    }

    private void handleCallNotification(RemoteMessage message) {
        createNotificationChannels();
        String callerName = message.getData().get("callerName");
        String callerId = message.getData().get("callerId");
        String callType = message.getData().get("callType");
        String callId = message.getData().get("callId");

        Intent intent = new Intent(this, CallActivity.class);
        intent.putExtra("callId", callId);
        intent.putExtra("callerId", callerId);
        intent.putExtra("callType", callType);
        intent.putExtra("isIncoming", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_CALLS)
            .setContentTitle("Incoming " + callType + " call")
            .setContentText("From " + callerName)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build();

        getSystemService(NotificationManager.class).notify(1000, notification);
    }
}
