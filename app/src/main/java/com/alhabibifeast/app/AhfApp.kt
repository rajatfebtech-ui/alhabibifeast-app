package com.alhabibifeast.app

import android.app.Application
import android.content.Context
import java.io.File

class AhfApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val def = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            try {
                val msg = "THREAD: ${thread.name}\n" +
                    "${e.javaClass.name}: ${e.message}\n" +
                    e.stackTrace.take(8).joinToString("\n") { "  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" } +
                    (e.cause?.let { "\nCaused by: ${it.javaClass.name}: ${it.message}" } ?: "")
                // .commit() is synchronous — safe under crash conditions
                getSharedPreferences("ahf_crash", Context.MODE_PRIVATE)
                    .edit().putString("last", msg).commit()
                // Also write to file as backup
                File(filesDir, "crash.txt").writeText(msg)
            } catch (_: Throwable) {}
            def?.uncaughtException(thread, e)
        }
    }
}
