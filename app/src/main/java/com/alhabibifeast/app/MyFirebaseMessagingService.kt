package com.alhabibifeast.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title   = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Al Habibi Feast"
        val body    = remoteMessage.notification?.body  ?: remoteMessage.data["body"]  ?: ""
        val deepUrl = remoteMessage.data["url"] ?: ""
        sendNotification(title, body, deepUrl)
    }

    override fun onNewToken(token: String) {
        // Send token to your server if needed
        // e.g. POST to https://alhabibifeast.in/api/fcm-token.php
    }

    private fun sendNotification(title: String, body: String, deepUrl: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (deepUrl.isNotEmpty()) putExtra("url", deepUrl)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_ONE_SHOT

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
        val channelId     = "ahf_orders"
        val sound         = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notifBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(sound)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Order Updates", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        manager.notify(System.currentTimeMillis().toInt(), notifBuilder.build())
    }
}
