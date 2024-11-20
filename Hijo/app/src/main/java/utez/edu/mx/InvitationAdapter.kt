package utez.edu.mx

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class InvitationAdapter(
    private val invitations: MutableList<Invitation>, // Lista de invitaciones
    private val context: Context, // Contexto de la aplicación
    private val onInvitationAccepted: (Invitation) -> Unit // Acción cuando se acepta una invitación
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
            tvParentId.text = "Invitación de: ${invitation.parentId}"

            btnAccept.setOnClickListener {
                acceptInvitation(invitation)
            }
        }

        private fun acceptInvitation(invitation: Invitation) {
            val db = FirebaseFirestore.getInstance()

            // Actualizar estado de la invitación en Firebase
            db.collection("invitations").document(invitation.id)
                .update("status", "accepted")
                .addOnSuccessListener {
                    Toast.makeText(context, "Invitación aceptada", Toast.LENGTH_SHORT).show()
                    // Ejecutar la acción pasada desde la actividad
                    onInvitationAccepted(invitation)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error al aceptar la invitación: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
