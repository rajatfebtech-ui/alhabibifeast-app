package com.alhabibifeast.app.admin

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object OrderAlarmScheduler {

    private const val REQUEST_CODE = 7777
    private const val INTERVAL_MS  = 2 * 60 * 1000L  // 2 minutes

    fun start(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = getPendingIntent(ctx)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // Fallback to inexact repeating on Android 12+ if exact not permitted
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + INTERVAL_MS, INTERVAL_MS, pi)
        } else {
            am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + INTERVAL_MS, INTERVAL_MS, pi)
        }
    }

    fun stop(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(getPendingIntent(ctx))
    }

    private fun getPendingIntent(ctx: Context) = PendingIntent.getBroadcast(
        ctx, REQUEST_CODE,
        Intent(ctx, OrderAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
