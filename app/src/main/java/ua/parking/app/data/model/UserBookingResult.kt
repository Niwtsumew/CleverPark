package ua.parking.app.data.model

enum class UserBookingResult {
    SUCCESS,
    ALREADY_BOOKED,
    PARKING_FULL,
    FREED,
    NOT_BOOKED,
    ERROR
}
