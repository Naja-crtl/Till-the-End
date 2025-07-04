package com.example.weddingapp.LandingPage.Timeline;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Extract event details from the intent
        String eventDescription = intent.getStringExtra("eventDescription");
        String eventDate = intent.getStringExtra("eventDate");
        String eventTime = intent.getStringExtra("eventTime");

        // Send the notification
        NotificationHelper notificationHelper = new NotificationHelper(context);
        notificationHelper.sendNotification(
                eventDescription, // Notification title
                "Event on " + eventDate + " at " + eventTime, // Notification content
                (int) System.currentTimeMillis() // Unique notification ID
        );
    }
}
