package com.alhabibifeast.app.admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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
        private const val CHANNEL_ORDERS  = "ahf_orders"
        private const val NOTIF_ID_FG     = 1001
        private const val PREFS_POLL      = "ahf_poll"
        private const val KEY_KNOWN       = "known_ids"
        private const val POLL_INTERVAL   = 30_000L  // 30 seconds

        fun start(ctx: Context) {
            val intent = Intent(ctx, OrderMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, OrderMonitorService::class.java))
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForeground(NOTIF_ID_FG, buildForegroundNotification())
        startPolling()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun startPolling() {
        scope.launch {
            while (isActive) {
                try {
                    checkNewOrders()
                } catch (_: Exception) {}
                delay(POLL_INTERVAL)
            }
        }
    }

    private suspend fun checkNewOrders() {
        val adminPrefs = getSharedPreferences("ahf_admin", Context.MODE_PRIVATE)
        if (!adminPrefs.getBoolean("authed", false)) return

        val prefs    = getSharedPreferences(PREFS_POLL, Context.MODE_PRIVATE)
        val knownIds = prefs.getStringSet(KEY_KNOWN, emptySet())?.toMutableSet() ?: mutableSetOf()

        val resp = ApiClient.api.getAdminOrders(AdminDashboardActivity.ADMIN_TOKEN)
        if (!resp.ok) return

        val newOrders = resp.orders.filter { it.isPending && !knownIds.contains(it.orderId) }

        newOrders.forEach { o ->
            showOrderNotification(o.orderId, o.customerName, o.displayTotal)
            knownIds.add(o.orderId)
        }

        if (newOrders.isNotEmpty()) {
            prefs.edit().putStringSet(KEY_KNOWN, knownIds).apply()
        }
    }

    private fun showOrderNotification(orderId: String, customerName: String, total: Int) {
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
            .setContentTitle("🛒 New Order Received!")
            .setContentText("#$orderId · ${customerName.ifBlank { "Customer" }} · ₹$total")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Order #$orderId\nCustomer: ${customerName.ifBlank { "N/A" }}\nAmount: ₹$total\n\nTap to open Admin Dashboard"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setSound(sound)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setLights(0xFFFF6600.toInt(), 300, 300)
            .build())
    }

    private fun buildForegroundNotification() = NotificationCompat.Builder(this, CHANNEL_MONITOR)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("Al Habibi Feast Admin")
        .setContentText("Monitoring for new orders...")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0,
                Intent(this, AdminDashboardActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_MONITOR, "Admin Monitor", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Background order monitoring" })

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_ORDERS, "New Orders", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "New order alerts"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            enableLights(true)
            lightColor = 0xFFFF6600.toInt()
        })
    }
}
