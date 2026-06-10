package ua.parking.app.data.model

data class ParkingModel(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val photoUrl: String = "",
    val schedule: String = "",
    val totalSpots: Int = 0,
    val occupiedSpots: Int = 0,
    val pricePerHour: Double? = null
) {
    val freeSpots: Int get() = maxOf(0, totalSpots - occupiedSpots)
    val isFull: Boolean get() = freeSpots == 0
    val isPaid: Boolean get() = pricePerHour != null && pricePerHour > 0.0
    val isFree: Boolean get() = !isPaid
}
