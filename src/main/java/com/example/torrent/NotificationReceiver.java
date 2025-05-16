package com.example.torrent;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "torrent_notification_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String torrentName = intent.getStringExtra("torrentName");
        String torrentId = intent.getStringExtra("torrentId");

        if (torrentName != null && torrentId != null) {

            Intent notificationIntent = new Intent(context, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                notificationIntent, 
                PendingIntent.FLAG_IMMUTABLE
            );


            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_torrent_logo)
                .setContentTitle("Torrent letöltés kész")
                .setContentText("A(z) " + torrentName + " torrent letöltése befejeződött (100%)")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);


            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            
            try {
                notificationManager.notify(torrentId.hashCode(), builder.build());
            } catch (SecurityException e) {
                Log.e("NotificationReceiver", "Missing notification permission", e);
            }
        }
    }
} 