package ua.parking.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import ua.parking.app.data.model.BookingRecord
import ua.parking.app.data.model.ParkingModel
import ua.parking.app.data.model.UserBookingResult

class ParkingRepositoryImpl : ParkingRepository {
    private val db = FirebaseFirestore.getInstance()
    private val parkingsRef = db.collection("parkings")
    private val bookingsRef = db.collection("bookings")

    override fun observeParkings(onUpdate: (List<ParkingModel>) -> Unit) {
        parkingsRef.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull {
                it.toObject(ParkingModel::class.java)
            } ?: emptyList()
            onUpdate(list)
        }
    }

    override suspend fun bookSpot(parkingId: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val ref = parkingsRef.document(parkingId)
                val snapshot = transaction.get(ref)
                val current = snapshot.getLong("occupiedSpots")?.toInt() ?: 0
                val total = snapshot.getLong("totalSpots")?.toInt() ?: 0
                if (current < total) {
                    transaction.update(ref, "occupiedSpots", current + 1)
                } else {
                    throw Exception("Parking is full")
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun bookSpotForUser(userId: String, parkingId: String): UserBookingResult {
        return try {
            var result = UserBookingResult.SUCCESS
            db.runTransaction { transaction ->
                val bookingRef = bookingsRef.document("${userId}_${parkingId}")
                val parkingRef = parkingsRef.document(parkingId)
                if (transaction.get(bookingRef).exists()) {
                    result = UserBookingResult.ALREADY_BOOKED
                    return@runTransaction
                }
                val parkingSnap = transaction.get(parkingRef)
                val occupied = parkingSnap.getLong("occupiedSpots")?.toInt() ?: 0
                val total    = parkingSnap.getLong("totalSpots")?.toInt() ?: 0
                if (occupied >= total) {
                    result = UserBookingResult.PARKING_FULL
                    return@runTransaction
                }
                transaction.update(parkingRef, "occupiedSpots", occupied + 1)
                transaction.set(
                    bookingRef,
                    mapOf(
                        "userId"    to userId,
                        "parkingId" to parkingId,
                        "bookedAt"  to System.currentTimeMillis()
                    )
                )
            }.await()
            result
        } catch (e: Exception) {
            UserBookingResult.ERROR
        }
    }

    override suspend fun getUserBooking(userId: String, parkingId: String): BookingRecord? {
        return try {
            val doc = bookingsRef.document("${userId}_${parkingId}").get().await()
            if (doc.exists()) BookingRecord(
                userId    = doc.getString("userId") ?: "",
                parkingId = doc.getString("parkingId") ?: "",
                bookedAt  = doc.getLong("bookedAt") ?: 0L
            ) else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun freeSpot(userId: String, parkingId: String): UserBookingResult {
        return try {
            db.runTransaction { transaction ->
                val bookingRef = bookingsRef.document("${userId}_${parkingId}")
                val parkingRef = parkingsRef.document(parkingId)
                if (!transaction.get(bookingRef).exists()) return@runTransaction
                val occupied = transaction.get(parkingRef).getLong("occupiedSpots")?.toInt() ?: 0
                transaction.update(parkingRef, "occupiedSpots", maxOf(0, occupied - 1))
                transaction.delete(bookingRef)
            }.await()
            UserBookingResult.FREED
        } catch (e: Exception) {
            UserBookingResult.ERROR
        }
    }

    override suspend fun addParking(parking: ParkingModel): Result<Unit> {
        return try {
            val doc = parkingsRef.document()
            parkingsRef.document(doc.id).set(parking.copy(id = doc.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateParking(parking: ParkingModel): Result<Unit> {
        return try {
            parkingsRef.document(parking.id).set(parking).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteParking(parkingId: String): Result<Unit> {
        return try {
            parkingsRef.document(parkingId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveFavorites(userId: String, parkingIds: List<String>) {
        db.collection("favorites").document(userId)
            .set(mapOf("parkingIds" to parkingIds)).await()
    }

    override suspend fun loadFavorites(userId: String): List<String> {
        return try {
            val doc = db.collection("favorites").document(userId).get().await()
            @Suppress("UNCHECKED_CAST")
            doc.get("parkingIds") as? List<String> ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getUserBalance(userId: String): Double {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.getDouble("balance") ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    override suspend fun saveUserBalance(userId: String, balance: Double) {
        try {
            db.collection("users").document(userId)
                .set(mapOf("balance" to balance)).await()
        } catch (e: Exception) {
            // ignore
        }
    }
}
