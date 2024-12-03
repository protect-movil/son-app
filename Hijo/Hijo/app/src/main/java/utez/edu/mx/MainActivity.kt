package utez.edu.mx

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var invitationAdapter: InvitationAdapter
    private lateinit var invitationList: MutableList<Invitation>
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var childId: String

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
                acceptInvitation(invitation)
            }
        )
        recyclerView.adapter = invitationAdapter

        // Llama al método para cargar las invitaciones
        fetchInvitations()

    }

    private fun generateAndSaveChildId(): String {
        val id = UUID.randomUUID().toString()
        sharedPreferences.edit().putString("CHILD_ID", id).apply()
        return id
    }

    private fun fetchInvitations() {
        db.collection("invitations")
            .whereEqualTo("childId", childId) // Usamos el ID del hijo correctamente
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Error al cargar invitaciones: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                // Actualiza la lista de invitaciones con los cambios en tiempo real
                invitationList.clear()
                for (document in snapshots!!) {
                    val invitation = document.toObject(Invitation::class.java).apply {
                        id = document.id
                    }
                    invitationList.add(invitation)
                }
                invitationAdapter.notifyDataSetChanged()
            }
    }



    private fun acceptInvitation(invitation: Invitation) {
        val childId = sharedPreferences.getString("CHILD_ID", null) ?: return

        val childData = hashMapOf(
            "name" to "Nombre del Hijo", // Cambia dinámicamente según los datos del hijo
            "id" to childId,
            "parentId" to invitation.parentId
        )

        db.collection("usuarios").document("hijos")
            .collection(childId) // Aquí almacenamos el hijo
            .document("details")
            .set(childData)
            .addOnSuccessListener {
                Log.d("Firestore", "Hijo agregado correctamente en la colección general de hijos")
                Toast.makeText(this, "Hijo registrado correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al registrar hijo: ${e.message}")
                Toast.makeText(this, "Error al registrar hijo", Toast.LENGTH_SHORT).show()
            }
    }


}

