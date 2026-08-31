package com.alhabibifeast.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alhabibifeast.app.R
import com.alhabibifeast.app.data.CartManager
import com.alhabibifeast.app.data.api.ApiClient
import com.alhabibifeast.app.data.model.Product
import com.alhabibifeast.app.ui.menu.ProductAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private val categories = listOf(
        "All" to "all",
        "🍗 Chicken" to "chicken",
        "🥩 Mutton" to "mutton",
        "🐟 Seafood" to "seafood",
        "🛒 Grocery" to "grocery",
        "✨ Brand" to "branded",
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val chipGroup    = view.findViewById<ChipGroup>(R.id.categoryChips)
        val rvFeatured   = view.findViewById<RecyclerView>(R.id.rvFeatured)
        val progress     = view.findViewById<ProgressBar>(R.id.homeProgress)
        val tvError      = view.findViewById<TextView>(R.id.tvHomeError)

        categories.forEach { (label, cat) ->
            val chip = Chip(requireContext()).apply {
                text        = label
                isCheckable = true
                isChecked   = cat == "all"
                setOnClickListener { navigateToMenu(cat) }
            }
            chipGroup.addView(chip)
        }

        rvFeatured.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        loadFeatured(rvFeatured, progress, tvError)
    }

    private fun navigateToMenu(cat: String) {
        val bundle = Bundle().apply { putString("cat", cat) }
        findNavController().navigate(R.id.nav_menu, bundle)
    }

    private fun loadFeatured(rv: RecyclerView, progress: ProgressBar, tvError: TextView) {
        lifecycleScope.launch {
            progress.visibility = View.VISIBLE
            tvError.visibility  = View.GONE
            try {
                val resp = ApiClient.api.getProducts()
                if (resp.ok && resp.products.isNotEmpty()) {
                    val featured = resp.products.filter { it.isBestseller }.take(10)
                        .ifEmpty { resp.products.take(10) }
                    rv.adapter = ProductAdapter(featured) { product, qty ->
                        CartManager.addItem(product, qty)
                        activity?.let { (it as? com.alhabibifeast.app.MainActivity)?.updateCartBadge() }
                    }
                } else {
                    if (isAdded) {
                        tvError.text       = "No products found. Pull to refresh."
                        tvError.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                if (isAdded) {
                    tvError.text       = "Could not load products: ${e.javaClass.simpleName}: ${e.message?.take(80)}"
                    tvError.visibility = View.VISIBLE
                }
            } finally {
                progress.visibility = View.GONE
            }
        }
    }
}
