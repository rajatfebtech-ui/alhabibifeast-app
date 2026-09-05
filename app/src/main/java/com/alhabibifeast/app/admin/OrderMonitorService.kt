package com.alhabibifeast.app.admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.alhabibifeast.app.R
import com.alhabibifeast.app.data.api.ApiClient
import kotlinx.coroutines.*

class OrderMonitorService : Service() {

    companion object {
        private const val CHANNEL_MONITOR = "ahf_monitor"
        private const val CHANNEL_ORDERS  = "ahf_new_orders"
        private const val NOTIF_ID_FG     = 1001
        private const val POLL_MS         = 20_000L   // 20 seconds
        private const val PREFS_POLL      = "ahf_poll"
        private const val KEY_SEEN_IDS    = "seen_ids"
        private const val KEY_START_TS    = "service_start_ts"

        fun start(ctx: Context) {
            val i = Intent(ctx, OrderMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }

        fun stop(ctx: Context) = ctx.stopService(Intent(ctx, OrderMonitorService::class.java))
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(i: Intent?): IBinder? = null
    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForeground(NOTIF_ID_FG, buildFgNotif())

        // Record when service started — only alert for orders AFTER this time
        val prefs = getSharedPreferences(PREFS_POLL, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_START_TS)) {
            prefs.edit().putLong(KEY_START_TS, System.currentTimeMillis()).apply()
        }

        scope.launch {
            while (isActive) {
                try { poll() } catch (_: Exception) {}
                delay(POLL_MS)
            }
        }
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    private suspend fun poll() {
        val adminOk = getSharedPreferences("ahf_admin", Context.MODE_PRIVATE)
            .getBoolean("authed", false)
        if (!adminOk) return

        val prefs    = getSharedPreferences(PREFS_POLL, Context.MODE_PRIVATE)
        val startTs  = prefs.getLong(KEY_START_TS, System.currentTimeMillis())
        val seenIds  = prefs.getStringSet(KEY_SEEN_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()

        val resp = ApiClient.api.getAdminOrders(AdminDashboardActivity.ADMIN_TOKEN)
        if (!resp.ok) return

        // Only consider orders placed AFTER service started + not yet notified
        val newOrders = resp.orders.filter { o ->
            val orderTs = try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
                    .parse(o.timestamp)?.time ?: 0L
            } catch (_: Exception) { 0L }
            orderTs >= startTs && o.isPending && !seenIds.contains(o.orderId)
        }

        newOrders.forEach { o ->
            notify(o.orderId, o.customerName, o.displayTotal)
            seenIds.add(o.orderId)
        }

        if (newOrders.isNotEmpty()) {
            prefs.edit().putStringSet(KEY_SEEN_IDS, seenIds).apply()
        }
    }

    private fun notify(orderId: String, name: String, total: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pi = PendingIntent.getActivity(
            this, orderId.hashCode(),
            Intent(this, AdminDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        nm.notify(orderId.hashCode(), NotificationCompat.Builder(this, CHANNEL_ORDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🛒 New Order — #$orderId")
            .setContentText("${name.ifBlank { "Customer" }} · ₹$total · Tap to open")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Customer: ${name.ifBlank{"N/A"}}\nTotal: ₹$total\n\nTap to manage in Admin Dashboard"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setSound(sound)
            .setVibrate(longArrayOf(0, 600, 200, 600))
            .build())
    }

    private fun buildFgNotif() = NotificationCompat.Builder(this, CHANNEL_MONITOR)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("Al Habibi Feast Admin")
        .setContentText("Watching for new orders every 20s...")
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setOngoing(true)
        .setContentIntent(PendingIntent.getActivity(
            this, 0, Intent(this, AdminDashboardActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE))
        .build()

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_MONITOR, "Admin Monitor", NotificationManager.IMPORTANCE_MIN))

        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_ORDERS, "New Orders", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 200, 600)
            setSound(sound, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
        })
    }
}
