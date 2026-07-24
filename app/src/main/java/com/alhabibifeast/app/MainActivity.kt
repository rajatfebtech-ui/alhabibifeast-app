package com.alhabibifeast.app

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.alhabibifeast.app.data.CartManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    companion object {
        lateinit var instance: MainActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        CartManager.init(applicationContext)

        // Show last crash info if any (for debugging)
        val crashPrefs = getSharedPreferences("ahf_crash", Context.MODE_PRIVATE)
        val lastCrash = crashPrefs.getString("last", null)
        if (lastCrash != null) {
            crashPrefs.edit().remove("last").apply()
            Toast.makeText(this, "Debug: $lastCrash", Toast.LENGTH_LONG).show()
        }

        setContentView(R.layout.activity_main)

        val navHost = supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        val nav     = navHost.navController
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
