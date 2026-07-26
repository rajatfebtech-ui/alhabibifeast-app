package com.alhabibifeast.app

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.alhabibifeast.app.data.CartManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        lateinit var instance: MainActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        CartManager.init(applicationContext)

        setContentView(R.layout.activity_main)

        val crashPrefs = getSharedPreferences("ahf_crash", Context.MODE_PRIVATE)
        val crashFile  = File(filesDir, "crash.txt")
        val lastCrash  = crashPrefs.getString("last", null)
            ?: if (crashFile.exists()) crashFile.readText() else null
        if (lastCrash != null) {
            crashPrefs.edit().remove("last").commit()
            crashFile.delete()
            AlertDialog.Builder(this)
                .setTitle("Crash Report (Debug)")
                .setMessage(lastCrash)
                .setPositiveButton("OK", null)
                .show()
        }

        val navHost   = supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        val nav       = navHost.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setupWithNavController(nav)

        updateCartBadge()
    }

    fun updateCartBadge() {
        try {
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav) ?: return
            val count = CartManager.getTotalCount()
            val badge = bottomNav.getOrCreateBadge(R.id.nav_cart)
            badge.isVisible = count > 0
            if (count > 0) badge.number = count
        } catch (_: Exception) {}
    }
}
