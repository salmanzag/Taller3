package com.example.taller3

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.taller3.databinding.ActivityUserTrackingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class UserTrackingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserTrackingBinding
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance("https://trabajo-en-clase-57464-default-rtdb.firebaseio.com").reference
    
    private var trackedUserMarker: Marker? = null
    private var myMarker: Marker? = null
    private var distanceLine: Polyline? = null
    private var locationManager: LocationManager? = null
    private var userLocationListener: ValueEventListener? = null
    
    private var myLocation: Location? = null
    private var trackedUserLocation: GeoPoint? = null
    
    private val PERMISSION_REQUEST_LOCATION = 1001
    private lateinit var userId: String
    private lateinit var userName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        userId = intent.getStringExtra("userId") ?: ""
        userName = intent.getStringExtra("userName") ?: "Usuario"

        if (userId.isEmpty()) {
            Toast.makeText(this, "Error: Usuario no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvTrackedUserName.text = "Siguiendo a: $userName"

        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))

        binding.mapTracking.setMultiTouchControls(true)
        binding.mapTracking.controller.setZoom(15.0)

        binding.btnVolver.setOnClickListener {
            finish()
        }

        if (hasLocationPermission()) {
            startMyLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_LOCATION
            )
        }

        listenToUserLocation()
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun startMyLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            locationManager = getSystemService(LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                Toast.makeText(this, "Error: LocationManager no disponible", Toast.LENGTH_SHORT).show()
                return
            }

            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    1f,
                    myLocationListener
                )
            } else if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L,
                    5f,
                    myLocationListener
                )
            }

            val last = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            last?.let { updateMyLocation(it) }
        } catch (ex: Exception) {
            ex.printStackTrace()
            Toast.makeText(this, "Error iniciando ubicación: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val myLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            updateMyLocation(location)
        }
        override fun onProviderDisabled(provider: String) {}
        override fun onProviderEnabled(provider: String) {}
    }

    private fun updateMyLocation(location: Location) {
        myLocation = location
        val geo = GeoPoint(location.latitude, location.longitude)
        
        runOnUiThread {
            if (myMarker == null) {
                myMarker = Marker(binding.mapTracking)
                myMarker?.title = "Mi ubicación"
                myMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                myMarker?.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation)
                binding.mapTracking.overlays.add(myMarker)
            }
            myMarker?.position = geo
            binding.mapTracking.invalidate()
            
            updateDistanceAndLine()
        }
    }

    private fun listenToUserLocation() {
        userLocationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitude").getValue(Double::class.java)
                val lon = snapshot.child("longitude").getValue(Double::class.java)

                android.util.Log.d("UserTrackingActivity", "Usuario rastreado - Lat: $lat, Lon: $lon")

                if (lat != null && lon != null && (lat != 0.0 || lon != 0.0)) {
                    trackedUserLocation = GeoPoint(lat, lon)
                    updateTrackedUserMarker(lat, lon)
                    updateDistanceAndLine()
                    android.util.Log.d("UserTrackingActivity", "✅ Ubicación del usuario actualizada")
                } else {
                    android.util.Log.w("UserTrackingActivity", "⚠️ Usuario sin ubicación GPS válida")
                    Toast.makeText(
                        this@UserTrackingActivity, 
                        "⚠️ $userName no tiene ubicación GPS activa. Pídele que abra el mapa para activar su GPS.", 
                        Toast.LENGTH_LONG
                    ).show()
                    
                    binding.tvTrackedUserLocation.text = "Ubicación: GPS no disponible"
                    binding.tvDistance.text = "Distancia: --"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("UserTrackingActivity", "Error al seguir usuario: ${error.message}")
                Toast.makeText(this@UserTrackingActivity, "Error al seguir usuario: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
        database.child("users").child(userId).addValueEventListener(userLocationListener!!)
    }

    private fun updateTrackedUserMarker(lat: Double, lon: Double) {
        runOnUiThread {
            val geo = GeoPoint(lat, lon)

            if (trackedUserMarker == null) {
                trackedUserMarker = Marker(binding.mapTracking)
                trackedUserMarker?.title = userName
                trackedUserMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                trackedUserMarker?.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_compass)
                binding.mapTracking.overlays.add(trackedUserMarker)
                binding.mapTracking.controller.setCenter(geo)
            }

            trackedUserMarker?.position = geo
            binding.tvTrackedUserLocation.text = "Ubicación: ${"%.5f".format(lat)}, ${"%.5f".format(lon)}"
            binding.mapTracking.invalidate()
        }
    }

    private fun updateDistanceAndLine() {
        if (myLocation != null && trackedUserLocation != null) {
            val distance = calculateDistance(
                myLocation!!.latitude,
                myLocation!!.longitude,
                trackedUserLocation!!.latitude,
                trackedUserLocation!!.longitude
            )
            
            runOnUiThread {
                binding.tvDistance.text = "Distancia: ${"%.2f".format(distance)} km"
                
                if (distanceLine != null) {
                    binding.mapTracking.overlays.remove(distanceLine)
                }
                
                distanceLine = Polyline().apply {
                    addPoint(GeoPoint(myLocation!!.latitude, myLocation!!.longitude))
                    addPoint(trackedUserLocation)
                    outlinePaint.color = android.graphics.Color.BLUE
                    outlinePaint.strokeWidth = 8f
                }
                
                binding.mapTracking.overlays.add(distanceLine)
                binding.mapTracking.invalidate()
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startMyLocationUpdates()
            } else {
                Toast.makeText(this, "Se necesitan permisos de ubicación para esta función", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapTracking.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapTracking.onPause()
        try {
            locationManager?.removeUpdates(myLocationListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationManager?.removeUpdates(myLocationListener)
            locationManager = null
            
            userLocationListener?.let {
                database.child("users").child(userId).removeEventListener(it)
            }
            userLocationListener = null
            
            if (distanceLine != null) {
                binding.mapTracking.overlays.remove(distanceLine)
                distanceLine = null
            }
            
            myMarker = null
            trackedUserMarker = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
