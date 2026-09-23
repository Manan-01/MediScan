package com.example.mediscan;


import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {

    public static final String CHANNEL_ID="mediscan_expiry_channel";
    public static final String CHANNEL_NAME="Medicine Expiry Alerts";

    public static void createChannel(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);

        channel.setDescription("Alerts for medicines nearing or past expiry");
        NotificationManager manager=context.getSystemService(NotificationManager.class);
        if(manager !=null) manager.createNotificationChannel(channel);
        }
    }

    public  static void showExpiringSoon(Context context,String medicineName,int notificationId){
        show(context, notificationId, "Medicine expiring soon", medicineName + " is expiring this month.");
    }

    public  static void showExpiredToday(Context context,String medicineName,int notificationId){
        show(context, notificationId, "Medicine expired",
                medicineName + " has expired.");
    }

    public static void showPersistingNag(Context context,int count,int notificationId){
        String text=count==1 ? "You have 1 expired medicine still in your list." : "You have " + count + " expired medicines still in your list.";
        NotificationCompat.Builder builder=
                new NotificationCompat.Builder(context,CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setContentTitle("Remove Expired Medicines").setContentText(text).setOngoing(true).setPriority(NotificationCompat.PRIORITY_HIGH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(context).notify(notificationId,builder.build());
    }

    private static void show(Context context, int id, String title, String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(context).notify(id, builder.build());
    }
}
