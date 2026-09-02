package com.alhabibifeast.app.admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alhabibifeast.app.R
import com.alhabibifeast.app.data.api.ApiClient

class OrderPollingWorker(
    private val ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {

    companion object {
        private const val CHANNEL_ID  = "ahf_orders"
        private const val PREFS_POLL  = "ahf_poll"
        private const val KEY_LAST_TS = "last_order_ts"
        private const val KEY_KNOWN   = "known_ids"
    }

    override suspend fun doWork(): Result {
        // Only run if admin is logged in
        val adminPrefs = ctx.getSharedPreferences("ahf_admin", Context.MODE_PRIVATE)
        if (!adminPrefs.getBoolean("authed", false)) return Result.success()

        return try {
            val prefs    = ctx.getSharedPreferences(PREFS_POLL, Context.MODE_PRIVATE)
            val knownIds = prefs.getStringSet(KEY_KNOWN, emptySet())?.toMutableSet() ?: mutableSetOf()

            val resp = ApiClient.api.getAdminOrders(AdminDashboardActivity.ADMIN_TOKEN)
            if (!resp.ok) return Result.success()

            val newOrders = resp.orders.filter { o ->
                o.isPending && !knownIds.contains(o.orderId)
            }

            if (newOrders.isNotEmpty()) {
                newOrders.forEach { o ->
                    showNotification(o.orderId, o.customerName, o.displayTotal)
                    knownIds.add(o.orderId)
                }
                prefs.edit().putStringSet(KEY_KNOWN, knownIds).apply()
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(orderId: String, customerName: String, total: Int) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "New Orders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when a new order is placed"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        val intent = Intent(ctx, AdminLoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            ctx, orderId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🛒 New Order Received!")
            .setContentText("#$orderId | ${customerName.ifBlank { "Customer" }} | ₹$total")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        nm.notify(orderId.hashCode(), notification)
    }
}
