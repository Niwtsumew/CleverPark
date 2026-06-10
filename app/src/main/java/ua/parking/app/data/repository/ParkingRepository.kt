package ua.parking.app.data.repository

import ua.parking.app.data.model.BookingRecord
import ua.parking.app.data.model.ParkingModel
import ua.parking.app.data.model.UserBookingResult

interface ParkingRepository {
    fun observeParkings(onUpdate: (List<ParkingModel>) -> Unit)
    suspend fun bookSpot(parkingId: String): Result<Unit>
    suspend fun bookSpotForUser(userId: String, parkingId: String): UserBookingResult
    suspend fun getUserBooking(userId: String, parkingId: String): BookingRecord?
    suspend fun freeSpot(userId: String, parkingId: String): UserBookingResult
    suspend fun addParking(parking: ParkingModel): Result<Unit>
    suspend fun updateParking(parking: ParkingModel): Result<Unit>
    suspend fun deleteParking(parkingId: String): Result<Unit>
    suspend fun saveFavorites(userId: String, parkingIds: List<String>)
    suspend fun loadFavorites(userId: String): List<String>
    suspend fun getUserBalance(userId: String): Double
    suspend fun saveUserBalance(userId: String, balance: Double)
}
