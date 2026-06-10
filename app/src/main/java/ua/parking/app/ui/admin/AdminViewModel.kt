package ua.parking.app.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ua.parking.app.data.model.ParkingModel
import ua.parking.app.data.repository.ParkingRepositoryImpl

class AdminViewModel : ViewModel() {

    private val repository = ParkingRepositoryImpl()

    private val _parkings = MutableLiveData<List<ParkingModel>>()
    val parkings: LiveData<List<ParkingModel>> = _parkings

    private val _operationResult = MutableLiveData<String?>()
    val operationResult: LiveData<String?> = _operationResult

    init {
        repository.observeParkings { _parkings.value = it }
    }

    fun addParking(parking: ParkingModel) {
        viewModelScope.launch {
            repository.addParking(parking).fold(
                onSuccess = { _operationResult.value = "Парковку додано" },
                onFailure = { _operationResult.value = "Помилка: ${it.message}" }
            )
        }
    }

    fun updateParking(parking: ParkingModel) {
        viewModelScope.launch {
            repository.updateParking(parking).fold(
                onSuccess = { _operationResult.value = "Парковку оновлено" },
                onFailure = { _operationResult.value = "Помилка: ${it.message}" }
            )
        }
    }

    fun deleteParking(parkingId: String) {
        viewModelScope.launch {
            repository.deleteParking(parkingId).fold(
                onSuccess = { _operationResult.value = "Парковку видалено" },
                onFailure = { _operationResult.value = "Помилка: ${it.message}" }
            )
        }
    }

    fun clearResult() { _operationResult.value = null }
}
