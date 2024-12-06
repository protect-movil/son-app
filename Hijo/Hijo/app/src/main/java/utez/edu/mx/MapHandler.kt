package utez.edu.mx

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class MapHandler(
    private val context: Context,
    private val supportMapFragment: SupportMapFragment,
    private val db: FirebaseFirestore,
    private val childId: String
) : OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var currentMarker: Marker? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    init {
        supportMapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true

        // Configurar la cámara en una posición inicial
        val initialPosition = LatLng(0.0, 0.0)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 15f))
    }

    fun startLocationTracking() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // Cada 5 segundos
        )
            .setMinUpdateIntervalMillis(2000L) // Intervalo mínimo de 2 segundos
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    // Actualizar marcador en el mapa
                    updateMarker(latitude, longitude)

                    // Actualizar ubicación en Firestore
                    updateLocationInFirestore(latitude, longitude)
                }
            }
        }
    }

    fun stopLocationTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("MapHandler", "Actualizaciones de ubicación detenidas")
    }

    fun updateMarker(latitude: Double, longitude: Double) {
        val latLng = LatLng(latitude, longitude)

        // Eliminar marcador anterior si existe
        currentMarker?.remove()

        // Crear un nuevo marcador en la posición actual
        currentMarker = googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Ubicación actual")
        )

        // Mover la cámara al marcador actualizado
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    private fun updateLocationInFirestore(latitude: Double, longitude: Double) {
        db.collection("usuarios").document("hijos")
            .collection(childId)
            .document("details")
            .update(
                mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude
                )
            )
            .addOnSuccessListener {
                Log.d("Firestore", "Ubicación actualizada: lat=$latitude, lng=$longitude")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al actualizar la ubicación: ${e.message}")
            }
    }
}
