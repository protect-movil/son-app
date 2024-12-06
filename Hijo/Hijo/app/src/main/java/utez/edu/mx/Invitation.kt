package utez.edu.mx

data class Invitation(
    var id: String = "",
    val childId: String = "",
    val parentId: String = "",
    val status: String = "pending",
    val latitude: Double = 0.0, // Valor predeterminado
    val longitude: Double = 0.0 // Valor predeterminado
)
