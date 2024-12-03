package utez.edu.mx

data class Invitation(
    var id: String = "",
    val childId: String = "",
    val parentId: String = "",
    val status: String = "pending"
)
