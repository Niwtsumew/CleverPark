package ua.parking.app.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import ua.parking.app.data.repository.ParkingRepositoryImpl

class ProfileViewModel : ViewModel() {

    private val repository = ParkingRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()

    private val _balance = MutableLiveData<Double>()
    val balance: LiveData<Double> = _balance

    fun loadBalance() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _balance.value = repository.getUserBalance(userId)
        }
    }

    fun topUp(amount: Double) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val current = repository.getUserBalance(userId)
            val newBalance = current + amount
            repository.saveUserBalance(userId, newBalance)
            _balance.value = newBalance
        }
    }
}
