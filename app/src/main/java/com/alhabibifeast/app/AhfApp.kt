package com.alhabibifeast.app

import android.app.Application
import android.content.Context

class AhfApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val def = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val msg = "${e.javaClass.simpleName}: ${e.message}\n" +
                e.stackTrace.take(4).joinToString("\n") { "  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
            getSharedPreferences("ahf_crash", Context.MODE_PRIVATE)
                .edit().putString("last", msg).apply()
            def?.uncaughtException(Thread.currentThread(), e)
        }
    }
}
