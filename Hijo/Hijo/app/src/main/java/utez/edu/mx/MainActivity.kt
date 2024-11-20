package utez.edu.mx

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
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

        // Configuración del RecyclerView
        recyclerView = findViewById(R.id.rvInvitations)
        recyclerView.layoutManager = LinearLayoutManager(this)
        invitationList = mutableListOf() // Inicializa la lista de invitaciones

        // Inicializa el adaptador
        invitationAdapter = InvitationAdapter(
            invitations = invitationList, // Lista de invitaciones
            context = this, // Contexto de la actividad
            onInvitationAccepted = { invitation ->
                // Manejo cuando se acepta la invitación
                fetchInvitations() // Recarga la lista
            }
        )
        recyclerView.adapter = invitationAdapter

        // Llama al método para cargar las invitaciones
        fetchInvitations()
    }

    /**
     * Genera un nuevo ID único para el hijo y lo guarda en SharedPreferences.
     */
    private fun generateAndSaveChildId(): String {
        val id = UUID.randomUUID().toString()
        sharedPreferences.edit().putString("CHILD_ID", id).apply()
        return id
    }

    /**
     * Obtiene las invitaciones dirigidas al hijo desde Firestore.
     */
    private fun fetchInvitations() {
        db.collection("invitations")
            .whereEqualTo("childId", childId)
            .get()
            .addOnSuccessListener { result ->
                invitationList.clear()
                for (document in result) {
                    val invitation = document.toObject(Invitation::class.java).apply { id = document.id }
                    invitationList.add(invitation)
                }
                invitationAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar invitaciones: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Acepta la invitación del padre y actualiza el estado en Firestore.
     */
    private fun acceptInvitation(invitation: Invitation) {
        db.collection("invitations").document(invitation.id)
            .update("status", "Aceptado")
            .addOnSuccessListener {
                Toast.makeText(this, "Invitación aceptada", Toast.LENGTH_SHORT).show()
                fetchInvitations() // Actualizar lista de invitaciones
                notifyParent(invitation.parentId) // Notificar al padre que fue aceptado
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al aceptar la invitación: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Notifica al padre que la invitación fue aceptada.
     */
    private fun notifyParent(parentId: String) {
        val notification = hashMapOf(
            "childId" to childId,
            "message" to "Invitación aceptada por el hijo.",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users").document(parentId)
            .collection("notifications")
            .add(notification)
            .addOnSuccessListener {
                Toast.makeText(this, "Notificación enviada al padre", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al notificar al padre: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
