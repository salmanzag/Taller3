package com.example.taller3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.example.taller3.databinding.ActivityMapBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.BufferedReader
import java.io.InputStreamReader

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private val PERMISSION_REQUEST_LOCATION = 1001
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().reference

    private var userMarker: Marker? = null
    private var locationManager: LocationManager? = null
    private var currentStatus: String = "connected"

    data class LocationPoint(
        val latitude: Double,
        val longitude: Double,
        val name: String
    )

    data class LocationsWrapper(
        @SerializedName("locationsArray")
        val locationsArray: List<LocationPoint>?,
        @SerializedName("locations")
        val locations: Map<String, LocationPoint>?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        
        startService(Intent(this, UserStatusService::class.java))

        binding.btnCerrarSesion.setOnClickListener {
            cerrarSesion()
        }
        
        binding.btnEstado.setOnClickListener {
            android.util.Log.d("MapActivity", "Botón de estado presionado")
            toggleUserStatus()
        }
        
        binding.btnVerUsuarios.setOnClickListener {
            startActivity(Intent(this, UsersListActivity::class.java))
        }

        Configuration.getInstance()
            .load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))

        val map: MapView = binding.map
        map.setMultiTouchControls(true)
        val controller = map.controller
        controller.setZoom(13.5)

        val locations = readLocationsFromAssets("locations.json")
        if (locations.isNotEmpty()) {
            val first = locations[0]
            controller.setCenter(GeoPoint(first.latitude, first.longitude))

            for (poi in locations) {
                val marker = Marker(map)
                marker.position = GeoPoint(poi.latitude, poi.longitude)
                marker.title = poi.name
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                map.overlays.add(marker)
            }
            binding.tvStatus.text = "Puntos cargados: ${locations.size}"
        } else {
            binding.tvStatus.text = "No se encontraron lugares en assets/locations.json"
        }

        if (hasLocationPermission()) {
            startLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_LOCATION
            )
        }
        
        loadCurrentUserStatus()
    }
    
    private fun loadCurrentUserStatus() {
        val currentUserId = auth.currentUser?.uid ?: return
        database.child("users").child(currentUserId).child("status").get()
            .addOnSuccessListener { snapshot ->
                val status = snapshot.getValue(String::class.java) ?: "connected"
                currentStatus = status
                updateStatusButton(status)
                if (snapshot.value == null) {
                    updateUserStatus("connected")
                }
            }
            .addOnFailureListener {
                currentStatus = "connected"
                updateStatusButton("connected")
                updateUserStatus("connected")
            }
    }

    private fun cerrarSesion() {
        try {
            locationManager?.removeUpdates(locationListener)
            
            stopService(Intent(this, UserStatusService::class.java))
            
            auth.signOut()
            
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readLocationsFromAssets(filename: String): List<LocationPoint> {
        try {
            val input = assets.open(filename)
            input.use {
                val reader = BufferedReader(InputStreamReader(it))
                val json = reader.readText()
                
                if (json.isBlank()) {
                    binding.tvStatus.text = "Error: archivo JSON vacío"
                    return emptyList()
                }
                
                val gson = Gson()
                val wrapper = gson.fromJson(json, LocationsWrapper::class.java)

                wrapper.locationsArray?.let { return it }
                wrapper.locations?.let { map ->
                    return map.entries
                        .sortedBy { it.key.toIntOrNull() ?: 0 }
                        .map { it.value }
                }
            }
        } catch (e: java.io.FileNotFoundException) {
            e.printStackTrace()
            binding.tvStatus.text = "Error: No se encontró el archivo $filename"
        } catch (e: com.google.gson.JsonSyntaxException) {
            e.printStackTrace()
            binding.tvStatus.text = "Error: Formato JSON inválido"
        } catch (e: Exception) {
            e.printStackTrace()
            binding.tvStatus.text = "Error leyendo ubicaciones: ${e.message}"
        }
        return emptyList()
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationUpdates() {
        binding.tvStatus.text = "Buscando ubicación..."
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        try {
            locationManager = getSystemService(LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                binding.tvStatus.text = "Error: LocationManager no disponible"
                return
            }
            
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    1f,
                    locationListener
                )
            } else if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L,
                    5f,
                    locationListener
                )
            } else {
                binding.tvStatus.text = "No hay proveedores de ubicación disponibles"
                return
            }

            val last = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            last?.let { updateUserLocationOnMap(it) }
        } catch (ex: SecurityException) {
            ex.printStackTrace()
            binding.tvStatus.text = "Error de permisos: ${ex.message}"
        } catch (ex: Exception) {
            ex.printStackTrace()
            binding.tvStatus.text = "Error iniciando ubicación: ${ex.message}"
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            updateUserLocationOnMap(location)
        }
        override fun onProviderDisabled(provider: String) {}
        override fun onProviderEnabled(provider: String) {}
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    private fun updateUserLocationOnMap(location: Location) {
        runOnUiThread {
            val geo = GeoPoint(location.latitude, location.longitude)
            if (userMarker == null) {
                userMarker = Marker(binding.map)
                userMarker?.title = "Tu ubicación"
                userMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                val blueIcon = resources.getDrawable(android.R.drawable.ic_menu_mylocation, null)
                blueIcon?.setBounds(0, 0, 100, 100)
                userMarker?.icon = blueIcon
                binding.map.overlays.add(userMarker)
                binding.map.controller.setCenter(geo)
                binding.map.controller.setZoom(15.0)
            }
            userMarker?.position = geo
            binding.tvStatus.text = "Ubicación actual: ${"%.5f".format(location.latitude)}, ${"%.5f".format(location.longitude)}"
            binding.map.invalidate()
            
            updateLocationInFirebase(location.latitude, location.longitude)
        }
    }
    
    private fun updateLocationInFirebase(latitude: Double, longitude: Double) {
        val currentUserId = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "latitude" to latitude,
            "longitude" to longitude
        )
        database.child("users").child(currentUserId).updateChildren(updates)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.map_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_users_list -> {
                startActivity(Intent(this, UsersListActivity::class.java))
                true
            }
            R.id.menu_status_available -> {
                updateUserStatus("connected")
                true
            }
            R.id.menu_status_disconnected -> {
                updateUserStatus("disconnected")
                true
            }
            R.id.menu_logout -> {
                cerrarSesion()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun toggleUserStatus() {
        binding.btnEstado.isEnabled = false
        
        val newStatus = if (currentStatus == "connected") "disconnected" else "connected"
        android.util.Log.d("MapActivity", "Alternando estado de '$currentStatus' a '$newStatus'")
        
        currentStatus = newStatus
        
        updateStatusButton(newStatus)
        
        updateUserStatus(newStatus, updateUI = false)
    }
    
    private fun updateUserStatus(status: String, updateUI: Boolean = true) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            binding.btnEstado.isEnabled = true
            return
        }
        
        android.util.Log.d("MapActivity", "Actualizando estado a: $status (currentStatus antes=$currentStatus)")
        database.child("users").child(currentUserId).child("status").setValue(status)
            .addOnSuccessListener {
                currentStatus = status
                val message = if (status == "connected") "Estado: Disponible" else "Estado: Desconectado"
                android.util.Log.d("MapActivity", "Estado actualizado exitosamente a: $status (currentStatus ahora=$currentStatus)")
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                
                if (updateUI) {
                    updateStatusButton(status)
                } else {
                    binding.btnEstado.isEnabled = true
                    android.util.Log.d("MapActivity", "Botón habilitado después de actualizar Firebase")
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MapActivity", "Error al actualizar estado", e)
                Toast.makeText(this, "Error al actualizar estado: ${e.message}", Toast.LENGTH_SHORT).show()
                
                val previousStatus = if (status == "connected") "disconnected" else "connected"
                currentStatus = previousStatus
                updateStatusButton(previousStatus)
            }
    }
    
    private fun updateStatusButton(status: String) {
        android.util.Log.d("MapActivity", "Actualizando botón con estado: $status (currentStatus=$currentStatus)")
        if (status == "connected") {
            binding.btnEstado.text = "● Disponible"
            binding.btnEstado.setBackgroundColor(0xFF4CAF50.toInt())
        } else {
            binding.btnEstado.text = "● Desconectado"
            binding.btnEstado.setBackgroundColor(0xFFF44336.toInt())
        }
        binding.btnEstado.isEnabled = true
        android.util.Log.d("MapActivity", "Botón actualizado y habilitado. Texto: ${binding.btnEstado.text}")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                binding.tvStatus.text = "Permiso de ubicación denegado. Actívalo para ver tu posición."
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
        try {
            locationManager?.removeUpdates(locationListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            locationManager?.removeUpdates(locationListener)
            locationManager = null
            userMarker = null
            
            val currentUserId = auth.currentUser?.uid
            if (currentUserId != null) {
                database.child("users").child(currentUserId).child("status").setValue("disconnected")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
