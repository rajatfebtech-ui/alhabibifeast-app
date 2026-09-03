package com.alhabibifeast.app.admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.alhabibifeast.app.R
import com.alhabibifeast.app.data.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OrderAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "ahf_orders"
        private const val PREFS_POLL = "ahf_poll"
        private const val KEY_KNOWN  = "known_ids"
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        // Only run if admin is logged in
        val adminPrefs = ctx.getSharedPreferences("ahf_admin", Context.MODE_PRIVATE)
        if (!adminPrefs.getBoolean("authed", false)) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs    = ctx.getSharedPreferences(PREFS_POLL, Context.MODE_PRIVATE)
                val knownIds = prefs.getStringSet(KEY_KNOWN, emptySet())?.toMutableSet() ?: mutableSetOf()

                val resp = ApiClient.api.getAdminOrders(AdminDashboardActivity.ADMIN_TOKEN)
                if (!resp.ok) { pending.finish(); return@launch }

                val newOrders = resp.orders.filter { o ->
                    o.isPending && !knownIds.contains(o.orderId)
                }

                newOrders.forEach { o ->
                    showNotification(ctx, o.orderId, o.customerName, o.displayTotal)
                    knownIds.add(o.orderId)
                }

                if (newOrders.isNotEmpty()) {
                    prefs.edit().putStringSet(KEY_KNOWN, knownIds).apply()
                }
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }

    private fun showNotification(ctx: Context, orderId: String, customerName: String, total: Int) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "New Orders", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            nm.createNotificationChannel(ch)
        }

        val pi = PendingIntent.getActivity(
            ctx, orderId.hashCode(),
            Intent(ctx, AdminLoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        nm.notify(orderId.hashCode(), NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🛒 New Order!")
            .setContentText("#$orderId · ${customerName.ifBlank { "Customer" }} · ₹$total")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build())
    }
}
