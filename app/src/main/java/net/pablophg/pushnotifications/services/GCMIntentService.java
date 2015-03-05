package net.pablophg.pushnotifications.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import net.pablophg.pushnotifications.MainActivity;
import net.pablophg.pushnotifications.broadcastreceivers.MyReceiver;
import net.pablophg.pushnotifications.R;

public class GCMIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    public static final String TAG = "GCMIntentService";

    public GCMIntentService() {
        super("GCMIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) { // If it's a regular GCM message, do some work.
                // You could do some work here
                sendNotification("Received: " + extras.get("message"));
            }
        }
        Log.i(TAG, "Received: " + extras.toString());
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        MyReceiver.completeWakefulIntent(intent);
    }

    private void sendNotification(String msg) {
        // Build intent for notification content
        Intent viewIntent = new Intent(this, MainActivity.class);

        PendingIntent viewPendingIntent = PendingIntent.getActivity(this, 0, viewIntent, 0);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("GCM Notification")
                        .setContentText(msg)
                        .setContentIntent(viewPendingIntent)
                        .setDefaults(Notification.DEFAULT_ALL);

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Build the notification and display it.
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }
}