package com.alhabibifeast.app.ui.menu

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.alhabibifeast.app.MainActivity
import com.alhabibifeast.app.R
import com.alhabibifeast.app.data.CartManager
import com.alhabibifeast.app.data.api.ApiClient
import com.alhabibifeast.app.data.model.Product
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class MenuFragment : Fragment() {

    private var allProducts = listOf<Product>()
    private var adapter: ProductAdapter? = null
    private var activeCat = "all"
    private var searchQuery = ""

    private val categories = listOf(
        "All" to "all",
        "Chicken" to "chicken",
        "Mutton" to "mutton",
        "Seafood" to "seafood",
        "Grocery" to "grocery",
        "Brand" to "branded",
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_menu, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val swipe     = view.findViewById<SwipeRefreshLayout>(R.id.swipeMenu)
        val rv        = view.findViewById<RecyclerView>(R.id.rvProducts)
        val chipGroup = view.findViewById<ChipGroup>(R.id.menuChips)
        val search    = view.findViewById<EditText>(R.id.etSearch)
        val progress  = view.findViewById<ProgressBar>(R.id.menuProgress)
        val tvEmpty   = view.findViewById<TextView>(R.id.tvEmpty)

        // Pre-select category from nav args
        activeCat = arguments?.getString("cat") ?: "all"

        // Category chips
        categories.forEach { (label, cat) ->
            val chip = Chip(requireContext()).apply {
                text = label; isCheckable = true; isChecked = cat == activeCat
                setOnClickListener { activeCat = cat; filterAndDraw(rv, tvEmpty) }
            }
            chipGroup.addView(chip)
        }

        // RecyclerView
        adapter = ProductAdapter(emptyList()) { product, qty ->
            CartManager.addItem(product, qty)
            (activity as? MainActivity)?.updateCartBadge()
        }
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // Search
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString() ?: ""
                filterAndDraw(rv, tvEmpty)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        // Load
        fun load() {
            lifecycleScope.launch {
                progress.visibility = View.VISIBLE
                try {
                    val resp = ApiClient.api.getProducts()
                    if (resp.ok) {
                        allProducts = resp.products
                        filterAndDraw(rv, tvEmpty)
                    }
                } catch (_: Exception) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "Could not load products. Check your connection."
                } finally {
                    progress.visibility = View.GONE
                    swipe.isRefreshing  = false
                }
            }
        }

        swipe.setColorSchemeResources(R.color.terra)
        swipe.setOnRefreshListener { load() }
        load()
    }

    private fun filterAndDraw(rv: RecyclerView, tvEmpty: TextView) {
        var list = allProducts
        if (activeCat != "all") list = list.filter { it.cat == activeCat }
        if (searchQuery.isNotBlank()) list = list.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.catLabel.contains(searchQuery, ignoreCase = true)
        }
        adapter?.updateProducts(list)
        tvEmpty.visibility = if (list.isEmpty() && allProducts.isNotEmpty()) View.VISIBLE else View.GONE
        tvEmpty.text = "No products found"
    }
}
