package com.example.myapplication

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyFirebaseMessagingService : FirebaseMessagingService() {
    /**
     * Called when message is received.
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom())

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification()!!.getBody())


            // If notification payload is present, display the notification
            val title = remoteMessage.getNotification()!!.getTitle()
            val body = remoteMessage.getNotification()!!.getBody()
            sendNotification(title, body, remoteMessage.getData())
        }

        // Check if message contains a data payload
        if (remoteMessage.getData().size > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData())

            // Process data payload if any
            // For long-running tasks, consider using WorkManager
            if (needsToBeProcessedInBackground()) {
                // Schedule a job using WorkManager for operations > 10 sec
                scheduleBackgroundWork(remoteMessage.getData())
            } else {
                // Handle message immediately for quick processing (< 10 sec)
                handleNow(remoteMessage.getData())
            }
        }
    }

    /**
     * Called if the FCM registration token is updated or generated for the first time.
     * This will be invoked on app install and if token is refreshed.
     * @param token The new token.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: " + token)

        // If you want to send messages to this application instance or
        // manage this app's subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }

    /**
     * Handle time-consuming operations in a separate thread.
     * @param data Message data payload
     */
    private fun scheduleBackgroundWork(data: MutableMap<String?, String?>?) {
        // Use WorkManager for background processing
        // Example:
        /*
        Data inputData = new Data.Builder()
                .putString("key1", data.get("key1"))
                .putString("key2", data.get("key2"))
                .build();

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(MyWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).beginWith(work).enqueue();
        */
    }

    /**
     * Handle message within 10 seconds of receiving it.
     * @param data Message data payload
     */
    private fun handleNow(data: MutableMap<String?, String?>?) {
        // Quick processing here (< 10 sec)
        // Example: parse data, update local database, etc.
    }

    /**
     * Determine if the message should be processed in a background task
     */
    private fun needsToBeProcessedInBackground(): Boolean {
        // Implement your logic to determine if the message needs background processing
        return false // Default: quick processing
    }

    /**
     * Send the FCM token to your app server.
     * @param token FCM token
     */
    private fun sendRegistrationToServer(token: String?) {
        // Implement this method to send token to your app server
        // For testing purposes, you can log the token
        Log.d(TAG, "Device Token: " + token)


        // TODO: Send the token to your server
        // Example using Retrofit or other HTTP library
    }

    /**
     * Create and show a notification with the given data.
     */
    private fun sendNotification(
        title: String?,
        messageBody: String?,
        data: MutableMap<String?, String?>
    ) {
        // Create an intent for when user taps the notification
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)


        // Add any data from the message to the intent
        for (entry in data.entries) {
            intent.putExtra(entry.key, entry.value)
        }


        // Create pending intent
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = "CHANNEL_ID"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)


        // Build the notification
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification_overlay)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // For Android 8.0+ (Oreo) we need to create a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "CHANEL_ID",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Show notification
        notificationManager.notify(0, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
