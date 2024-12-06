package utez.edu.mx

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class InvitationAdapter(
    private val invitations: MutableList<Invitation>,
    private val context: Context,
    private val onInvitationAccepted: (Invitation) -> Unit
) : RecyclerView.Adapter<InvitationAdapter.InvitationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvitationViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_invitation, parent, false)
        return InvitationViewHolder(view)
    }

    override fun onBindViewHolder(holder: InvitationViewHolder, position: Int) {
        val invitation = invitations[position]
        holder.bind(invitation)
    }

    override fun getItemCount(): Int = invitations.size

    inner class InvitationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvParentId: TextView = itemView.findViewById(R.id.tvParentId)
        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)

        fun bind(invitation: Invitation) {
            tvParentId.text = "Invitación de: ${invitation.parentId ?: "Desconocido"}"

            btnAccept.setOnClickListener {
                btnAccept.isEnabled = false
                btnAccept.text = "Procesando..."
                acceptInvitation(invitation)
            }
        }

        private fun acceptInvitation(invitation: Invitation) {
            val db = FirebaseFirestore.getInstance()
            Log.d("InvitationAdapter", "Aceptando invitación: ${invitation.id}")

            val childId = invitation.childId
            val parentId = invitation.parentId

            // Actualizar el estado de la invitación
            db.collection("invitations").document(invitation.id)
                .update("status", "accepted")
                .addOnSuccessListener {
                    Toast.makeText(context, "Invitación aceptada", Toast.LENGTH_SHORT).show()

                    // Crear el documento en la subcolección del hijo con las coordenadas iniciales
                    val childData = hashMapOf(
                        "name" to "Nombre del Hijo", // Ajusta según sea necesario
                        "id" to childId,
                        "parentId" to parentId,
                        "latitude" to 0.0, // Inicializa con valores predeterminados
                        "longitude" to 0.0 // Inicializa con valores predeterminados
                    )

                    db.collection("usuarios").document("hijos")
                        .collection(childId) // Subcolección para el hijo
                        .document("details") // Documento "details"
                        .set(childData)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Detalles del hijo creados correctamente en Firestore.")
                            invitations.remove(invitation)
                            notifyItemRemoved(adapterPosition)
                            onInvitationAccepted(invitation)
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Error al crear los detalles del hijo: ${e.message}")
                            Toast.makeText(context, "Error al crear detalles del hijo.", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error al aceptar la invitación: ${e.message}")
                    Toast.makeText(context, "Error al aceptar la invitación: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnAccept.isEnabled = true
                    btnAccept.text = "Aceptar"
                }
        }

    }

}
