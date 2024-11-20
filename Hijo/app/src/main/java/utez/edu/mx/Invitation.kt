package utez.edu.mx

data class Invitation(
    val id: String = "",
    val childId: String = "",
    val parentId: String = "",
    val status: String = "pending"
)
