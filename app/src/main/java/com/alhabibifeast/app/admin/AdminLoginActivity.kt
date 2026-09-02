package com.alhabibifeast.app.admin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.alhabibifeast.app.R
import com.google.android.material.textfield.TextInputEditText

class AdminLoginActivity : AppCompatActivity() {

    companion object {
        private const val ADMIN_PIN = "2024"
        private const val PREFS     = "ahf_admin"
        private const val KEY_AUTHED= "authed"

        fun isLoggedIn(ctx: Context) =
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_AUTHED, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        if (isLoggedIn(this)) {
            openDashboard()
            return
        }

        setContentView(R.layout.activity_admin_login)

        val etPin   = findViewById<TextInputEditText>(R.id.etAdminPin)
        val btnLogin= findViewById<Button>(R.id.btnAdminLogin)
        val tvError = findViewById<TextView>(R.id.tvAdminLoginError)

        fun tryLogin() {
            val pin = etPin.text?.toString()?.trim() ?: ""
            if (pin == ADMIN_PIN) {
                getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY_AUTHED, true).apply()
                openDashboard()
            } else {
                tvError.text = "❌ Wrong PIN. Try again."
                tvError.visibility = TextView.VISIBLE
                etPin.text?.clear()
            }
        }

        btnLogin.setOnClickListener { tryLogin() }
        etPin.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { tryLogin(); true } else false
        }
    }

    private fun openDashboard() {
        startActivity(Intent(this, AdminDashboardActivity::class.java))
        finish()
    }
}
