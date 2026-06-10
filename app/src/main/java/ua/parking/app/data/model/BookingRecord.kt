package ua.parking.app.data.model

data class BookingRecord(
    val userId: String = "",
    val parkingId: String = "",
    val bookedAt: Long = 0L
)
