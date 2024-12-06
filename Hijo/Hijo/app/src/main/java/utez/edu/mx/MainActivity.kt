package utez.edu.mx

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.gms.maps.SupportMapFragment
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var invitationAdapter: InvitationAdapter
    private lateinit var invitationList: MutableList<Invitation>
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var childId: String
    private lateinit var mapHandler: MapHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar Firestore
        db = FirebaseFirestore.getInstance()

        // Inicializar SharedPreferences y obtener o generar el ID del hijo
        sharedPreferences = getSharedPreferences("ChildAppPrefs", Context.MODE_PRIVATE)
        childId = sharedPreferences.getString("CHILD_ID", null) ?: generateAndSaveChildId()

        // Mostrar el ID del hijo en la interfaz
        val tvChildId = findViewById<TextView>(R.id.tvChildId)
        tvChildId.text = "ID del Hijo: $childId"

        // Configuración del RecyclerView
        recyclerView = findViewById(R.id.rvInvitations)
        recyclerView.layoutManager = LinearLayoutManager(this)
        invitationList = mutableListOf()

        // Inicializa el adaptador
        invitationAdapter = InvitationAdapter(
            invitations = invitationList,
            context = this,
            onInvitationAccepted = { invitation ->
                handleAcceptedInvitation(invitation)
            }
        )
        recyclerView.adapter = invitationAdapter

        // Llama al método para cargar las invitaciones
        fetchInvitations()

        // Configurar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Inicializar el mapa usando MapHandler
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapHandler = MapHandler(this, mapFragment,db,childId)

        // Verificar GPS y obtener ubicación
        verifyGPSAndStartLocation()
    }

    private fun handleAcceptedInvitation(invitation: Invitation) {
        val db = FirebaseFirestore.getInstance()
        Log.d("MainActivity", "Aceptando invitación desde Activity: ${invitation.id}")

        db.collection("invitations").document(invitation.id)
            .update("status", "accepted")
            .addOnSuccessListener {
                Toast.makeText(this, "Invitación aceptada", Toast.LENGTH_SHORT).show()
                invitationList.remove(invitation)
                invitationAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al aceptar la invitación: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun generateAndSaveChildId(): String {
        val id = UUID.randomUUID().toString()
        sharedPreferences.edit().putString("CHILD_ID", id).apply()
        return id
    }

    private fun verifyGPSAndStartLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            showGPSDisabledAlert()
            return
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }

        startLocationUpdates()
    }

    private fun showGPSDisabledAlert() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("GPS Desactivado")
            .setMessage("El GPS está desactivado. ¿Quieres activarlo?")
            .setCancelable(false)
            .setPositiveButton("Sí") { _, _ ->
                val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
                Toast.makeText(this, "El GPS es necesario para el correcto funcionamiento de la app.", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L
        )
            .setMinUpdateIntervalMillis(5000L)
            .setWaitForAccurateLocation(true)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (location.latitude != 0.0 && location.longitude != 0.0) {
                        updateLocationInFirestore(location)
                        mapHandler.updateMarker(location.latitude, location.longitude)
                        break
                    } else {
                        Log.w("LocationCallback", "Ubicación inválida: lat=${location.latitude}, lng=${location.longitude}. Reintentando...")
                    }
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    private fun updateLocationInFirestore(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude

        if (latitude == 0.0 && longitude == 0.0) {
            Log.w("Firestore", "Ubicación inválida no enviada a Firestore: lat=$latitude, lng=$longitude")
            return
        }

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

    private fun fetchInvitations() {
        db.collection("invitations")
            .whereEqualTo("childId", childId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Error al cargar invitaciones: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                invitationList.clear()
                snapshots?.forEach { document ->
                    val invitation = document.toObject(Invitation::class.java).apply {
                        id = document.id
                    }
                    invitationList.add(invitation)
                }
                invitationAdapter.notifyDataSetChanged()
            }
    }
}
