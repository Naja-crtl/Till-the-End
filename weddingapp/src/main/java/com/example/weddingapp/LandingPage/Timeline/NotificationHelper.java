package com.example.weddingapp.LandingPage.Timeline;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.example.weddingapp.R;

public class NotificationHelper extends ContextWrapper {
    private static final String CHANNEL_ID = "event_channel";
    private static final String CHANNEL_NAME = "Event Notifications";
    private NotificationManager notificationManager;

    public NotificationHelper(Context base) {
        super(base);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for event notifications");
            getManager().createNotificationChannel(channel);
        }
    }

    public NotificationManager getManager() {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }

    public void sendNotification(String title, String content, int notificationId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        getManager().notify(notificationId, builder.build());
    }
}
