package com.alhabibifeast.app.ui.orders

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.alhabibifeast.app.R
import com.alhabibifeast.app.data.api.ApiClient
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.*

class TrackingFragment : Fragment(), OnMapReadyCallback {

    private var orderId: String = ""
    private var googleMap: GoogleMap? = null
    private var riderMarker: Marker? = null
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Al Habibi Feast shop — Kashipur, Uttarakhand
    private val shopLatLng    = LatLng(29.20952, 78.95836)
    private val customerLatLng = LatLng(29.21872, 78.96941) // updated from checkout

    private var riderProgress = 0.12f

    companion object {
        fun newInstance(orderId: String) = TrackingFragment().apply {
            arguments = Bundle().apply { putString("order_id", orderId) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderId = arguments?.getString("order_id") ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_tracking, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tvOrderId)?.text = "Order #$orderId"

        val mapFrag = childFragmentManager.findFragmentById(R.id.trackingMap) as? SupportMapFragment
        mapFrag?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isMyLocationButtonEnabled = false
            isMapToolbarEnabled = false
        }
        map.mapType = GoogleMap.MAP_TYPE_NORMAL

        // Shop marker
        map.addMarker(MarkerOptions().position(shopLatLng).title("Al Habibi Feast")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))

        // Customer marker
        map.addMarker(MarkerOptions().position(customerLatLng).title("Your Location")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))

        // Route line
        map.addPolyline(PolylineOptions()
            .add(shopLatLng, customerLatLng)
            .color(Color.parseColor("#C4796A"))
            .width(6f)
            .pattern(listOf(Dash(20f), Gap(12f))))

        // Rider marker
        riderMarker = map.addMarker(MarkerOptions()
            .position(shopLatLng)
            .title("Delivery Rider")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))

        // Fit map to show both points
        val bounds = LatLngBounds.Builder()
            .include(shopLatLng).include(customerLatLng).build()
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))

        startPolling()
    }

    private fun startPolling() {
        pollOnce()
        handler.postDelayed(object : Runnable {
            override fun run() {
                pollOnce()
                handler.postDelayed(this, 5000)
            }
        }, 5000)
    }

    private fun pollOnce() {
        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.api.getDeliveryLocation(orderId)
                }
                if (response.ok && !response.isDefault) {
                    moveRider(LatLng(response.lat, response.lng))
                } else {
                    animateRider()
                }
            } catch (e: Exception) {
                animateRider()
            }
        }
    }

    private fun animateRider() {
        riderProgress = (riderProgress + 0.01f).coerceAtMost(0.96f)
        val lat = shopLatLng.latitude + (customerLatLng.latitude - shopLatLng.latitude) * riderProgress
        val lng = shopLatLng.longitude + (customerLatLng.longitude - shopLatLng.longitude) * riderProgress
        moveRider(LatLng(lat, lng))
    }

    private fun moveRider(pos: LatLng) {
        riderMarker?.position = pos
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        super.onDestroyView()
    }
}
