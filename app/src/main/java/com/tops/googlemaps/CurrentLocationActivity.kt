package com.tops.googlemaps

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.tops.googlemaps.databinding.ActivityCurrentLocationBinding

const val ACTION_LOCATION_UPDATE = "com.tops.googlemaps.LOCATION_UPDATE"

class CurrentLocationActivity : AppCompatActivity(), OnMapReadyCallback {


    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var isMapReady = false


    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityCurrentLocationBinding

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("Broadcast", "Received location broadcast")
            currentLatitude = intent?.getDoubleExtra("latitude", 0.0)
            currentLongitude = intent?.getDoubleExtra("longitude", 0.0)

            if (isMapReady && currentLatitude != null && currentLongitude != null) {
                val currentLatLng = LatLng(currentLatitude!!, currentLongitude!!)
                mMap.clear()
                mMap.addMarker(MarkerOptions().position(currentLatLng).title("Current Location"))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            }}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCurrentLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ask for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(

                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        } else {
            startLocationService()
        }

        // Setup Map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationService()
        } else {
            Log.e("Permission", "Location permission denied")
        }
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        isMapReady = true

//        if (currentLatitude != null && currentLongitude != null) {
//            val currentLatLng = LatLng(currentLatitude!!, currentLongitude!!)
//            mMap.clear()
//            mMap.addMarker(MarkerOptions().position(currentLatLng).title("Current Location"))
//            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
//        }
    }


    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(ACTION_LOCATION_UPDATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(locationReceiver, filter, RECEIVER_EXPORTED)
            val serviceIntent = Intent(this, LocationService::class.java)
            startService(serviceIntent)
        } else {
            ContextCompat.registerReceiver(applicationContext,locationReceiver,filter,
                ContextCompat.RECEIVER_VISIBLE_TO_INSTANT_APPS)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(locationReceiver)
    }
}