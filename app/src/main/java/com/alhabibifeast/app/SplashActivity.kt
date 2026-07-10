package com.alhabibifeast.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo    = findViewById<ImageView>(R.id.splashLogo)
        val name    = findViewById<TextView>(R.id.splashName)
        val tagline = findViewById<TextView>(R.id.splashTagline)

        val fade = AlphaAnimation(0f, 1f).apply { duration = 700; fillAfter = true }
        logo.startAnimation(fade)
        name.startAnimation(AlphaAnimation(0f, 1f).apply { duration = 700; startOffset = 200; fillAfter = true })
        tagline.startAnimation(AlphaAnimation(0f, 1f).apply { duration = 700; startOffset = 400; fillAfter = true })

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1800)
    }
}
