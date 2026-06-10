package ua.parking.app.domain

import ua.parking.app.data.model.BookingStatus

class BookingManager(private var freeSpots: Int) {
    fun bookSpot(): BookingStatus {
        return if (freeSpots > 0) { freeSpots--; BookingStatus.SUCCESS }
        else BookingStatus.PARKING_FULL
    }
    fun getFreeSpots(): Int = freeSpots
    fun getBookingStatus(): BookingStatus =
        if (freeSpots == 0) BookingStatus.PARKING_FULL else BookingStatus.SUCCESS
}
