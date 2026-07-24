package com.alhabibifeast.app.rider

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alhabibifeast.app.R
import com.alhabibifeast.app.data.api.ApiClient
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class RiderLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rider_login)

        // Already logged in?
        val prefs = getSharedPreferences("ahf_rider", Context.MODE_PRIVATE)
        if (prefs.getString("rider_id", null) != null) {
            openDashboard()
            return
        }

        val etPhone = findViewById<TextInputEditText>(R.id.riderPhone)
        val etPin   = findViewById<TextInputEditText>(R.id.riderPin)
        val btnLogin = findViewById<Button>(R.id.btnRiderLogin)

        btnLogin.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            val pin   = etPin.text.toString().trim()
            if (phone.length < 10 || pin.length != 4) {
                Toast.makeText(this, "Enter valid phone and 4-digit PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            doLogin(phone, pin, prefs)
        }
    }

    private fun doLogin(phone: String, pin: String, prefs: android.content.SharedPreferences) {
        val btn = findViewById<Button>(R.id.btnRiderLogin)
        btn.isEnabled = false
        btn.text = "Logging in..."

        lifecycleScope.launch {
            try {
                val resp = ApiClient.api.riderLogin(phone, pin)
                if (resp.ok) {
                    prefs.edit()
                        .putString("rider_id", resp.riderId)
                        .putString("rider_name", resp.name)
                        .putString("rider_phone", phone)
                        .apply()
                    openDashboard()
                } else {
                    Toast.makeText(this@RiderLoginActivity, resp.msg.ifEmpty { "Invalid credentials" }, Toast.LENGTH_LONG).show()
                    btn.isEnabled = true
                    btn.text = "Login"
                }
            } catch (e: Exception) {
                Toast.makeText(this@RiderLoginActivity, "Connection error: ${e.message}", Toast.LENGTH_LONG).show()
                btn.isEnabled = true
                btn.text = "Login"
            }
        }
    }

    private fun openDashboard() {
        startActivity(Intent(this, RiderDashboardActivity::class.java))
        finish()
    }
}
