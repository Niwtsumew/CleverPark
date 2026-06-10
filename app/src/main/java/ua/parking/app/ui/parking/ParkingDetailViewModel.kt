package ua.parking.app.ui.parking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ua.parking.app.data.model.BookingRecord
import ua.parking.app.data.model.ParkingModel
import ua.parking.app.data.model.UserBookingResult
import ua.parking.app.data.repository.ParkingRepositoryImpl
import ua.parking.app.domain.BookingTimer
import ua.parking.app.domain.TariffCalculator

class ParkingDetailViewModel : ViewModel() {

    private val repository = ParkingRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()
    private val bookingTimer = BookingTimer()
    private val tariffCalculator = TariffCalculator()

    private val _parking = MutableLiveData<ParkingModel>()
    val parking: LiveData<ParkingModel> = _parking

    private val _bookingResult = MutableLiveData<BookingUiResult?>()
    val bookingResult: LiveData<BookingUiResult?> = _bookingResult

    private val _isFavorite = MutableLiveData<Boolean>()
    val isFavorite: LiveData<Boolean> = _isFavorite

    private val _activeBooking = MutableLiveData<BookingRecord?>()
    val activeBooking: LiveData<BookingRecord?> = _activeBooking

    private val _timerText = MutableLiveData<String>()
    val timerText: LiveData<String> = _timerText

    private val _costPreview = MutableLiveData<String>()
    val costPreview: LiveData<String> = _costPreview

    private val _balance = MutableLiveData<Double>()
    val balance: LiveData<Double> = _balance

    private var favoriteIds = mutableListOf<String>()
    private var timerJob: Job? = null

    fun loadParking(parkingId: String) {
        repository.observeParkings { list ->
            list.find { it.id == parkingId }?.let { _parking.value = it }
        }
        loadFavoriteState(parkingId)
        loadBookingState(parkingId)
        loadBalance()
    }

    private fun loadBalance() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _balance.value = repository.getUserBalance(userId)
        }
    }

    private fun loadFavoriteState(parkingId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            favoriteIds = repository.loadFavorites(userId).toMutableList()
            _isFavorite.value = favoriteIds.contains(parkingId)
        }
    }

    private fun loadBookingState(parkingId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val booking = repository.getUserBooking(userId, parkingId)
            _activeBooking.value = booking
            if (booking != null) startTimer(booking.bookedAt)
        }
    }

    private fun startTimer(bookedAt: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = bookingTimer.getElapsed(bookedAt)
                _timerText.value = elapsed?.format() ?: "0 с"
                val cost = tariffCalculator.calculate(
                    elapsed?.totalSeconds ?: 0L,
                    _parking.value?.pricePerHour
                )
                _costPreview.value = tariffCalculator.formatCost(cost)
                delay(1000)
            }
        }
    }

    fun bookSpot(parkingId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            when (repository.bookSpotForUser(userId, parkingId)) {
                UserBookingResult.SUCCESS -> {
                    val booking = repository.getUserBooking(userId, parkingId)
                    _activeBooking.value = booking
                    if (booking != null) startTimer(booking.bookedAt)
                    _bookingResult.value = BookingUiResult.Success
                }
                UserBookingResult.ALREADY_BOOKED -> _bookingResult.value = BookingUiResult.AlreadyBooked
                UserBookingResult.PARKING_FULL   -> _bookingResult.value = BookingUiResult.Full
                else                             -> _bookingResult.value = BookingUiResult.Full
            }
        }
    }

    fun freeSpot(parkingId: String) {
        val userId = auth.currentUser?.uid ?: return
        val bookedAt = _activeBooking.value?.bookedAt ?: return
        val pricePerHour = _parking.value?.pricePerHour

        val elapsedSeconds = bookingTimer.getElapsed(bookedAt)?.totalSeconds ?: 0L
        val cost = tariffCalculator.calculate(elapsedSeconds, pricePerHour)
        val currentBalance = _balance.value ?: 0.0

        if (cost > 0.0 && currentBalance < cost) {
            _bookingResult.value = BookingUiResult.InsufficientBalance(cost, currentBalance)
            return
        }

        viewModelScope.launch {
            val result = repository.freeSpot(userId, parkingId)
            if (result == UserBookingResult.FREED) {
                if (cost > 0.0) {
                    val newBalance = currentBalance - cost
                    repository.saveUserBalance(userId, newBalance)
                    _balance.value = newBalance
                }
                timerJob?.cancel()
                _activeBooking.value = null
                _timerText.value = ""
                _costPreview.value = ""
                _bookingResult.value = if (cost > 0.0)
                    BookingUiResult.Freed(tariffCalculator.formatCost(cost))
                else
                    BookingUiResult.Freed(null)
            }
        }
    }

    fun toggleFavorite(parkingId: String) {
        val userId = auth.currentUser?.uid ?: return
        val isFav = _isFavorite.value ?: false
        if (isFav) favoriteIds.remove(parkingId) else favoriteIds.add(parkingId)
        _isFavorite.value = !isFav
        viewModelScope.launch { repository.saveFavorites(userId, favoriteIds) }
    }

    fun clearBookingResult() { _bookingResult.value = null }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

sealed class BookingUiResult {
    object Success : BookingUiResult()
    object Full : BookingUiResult()
    object AlreadyBooked : BookingUiResult()
    data class Freed(val costFormatted: String?) : BookingUiResult()
    data class InsufficientBalance(val cost: Double, val balance: Double) : BookingUiResult()
}
