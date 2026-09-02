package com.alhabibifeast.app.ui.account

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alhabibifeast.app.R
import com.alhabibifeast.app.admin.AdminLoginActivity
import com.alhabibifeast.app.rider.RiderLoginActivity
import com.google.android.material.textfield.TextInputEditText

class AccountFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_account, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        try {
            setupAccountView(view)
        } catch (t: Throwable) {
            saveCrash(t)
        }
    }

    private fun setupAccountView(view: View) {
        val prefs = requireContext().getSharedPreferences("ahf_account", Context.MODE_PRIVATE)

        val etName    = view.findViewById<TextInputEditText>(R.id.acName)
        val etPhone   = view.findViewById<TextInputEditText>(R.id.acPhone)
        val etAddress = view.findViewById<TextInputEditText>(R.id.acAddress)
        val etCity    = view.findViewById<TextInputEditText>(R.id.acCity)
        val etPin     = view.findViewById<TextInputEditText>(R.id.acPin)

        etName?.setText(prefs.getString("name", ""))
        etPhone?.setText(prefs.getString("phone", ""))
        etAddress?.setText(prefs.getString("address", ""))
        etCity?.setText(prefs.getString("city", ""))
        etPin?.setText(prefs.getString("pin", ""))

        view.findViewById<Button>(R.id.btnSaveAccount)?.setOnClickListener {
            prefs.edit()
                .putString("name",    etName?.text.toString())
                .putString("phone",   etPhone?.text.toString())
                .putString("address", etAddress?.text.toString())
                .putString("city",    etCity?.text.toString())
                .putString("pin",     etPin?.text.toString())
                .apply()
            Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnFranchise)?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://alhabibifeast.in/#/franchise")))
        }

        view.findViewById<Button>(R.id.btnWhatsAppSupport)?.setOnClickListener {
            val url = "https://wa.me/917500000000?text=${Uri.encode("Hi! I need help with my Al Habibi Feast order.")}"
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            catch (_: Exception) { Toast.makeText(requireContext(), "WhatsApp not installed", Toast.LENGTH_SHORT).show() }
        }

        view.findViewById<Button>(R.id.btnOrders)?.setOnClickListener {
            findNavController().navigate(R.id.nav_orders)
        }

        view.findViewById<Button>(R.id.btnRiderLogin)?.setOnClickListener {
            startActivity(Intent(requireContext(), RiderLoginActivity::class.java))
        }

        view.findViewById<Button>(R.id.btnAdminPanel)?.setOnClickListener {
            startActivity(Intent(requireContext(), AdminLoginActivity::class.java))
        }
    }

    private fun saveCrash(t: Throwable) {
        try {
            val msg = "AccountFragment: ${t.javaClass.name}: ${t.message}\n" +
                t.stackTrace.take(6).joinToString("\n") { "  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
            requireContext().getSharedPreferences("ahf_crash", android.content.Context.MODE_PRIVATE)
                .edit().putString("last", msg).commit()
        } catch (_: Throwable) {}
    }
}
