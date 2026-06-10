package ua.parking.app.ui.favorites

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import ua.parking.app.data.model.ParkingModel
import ua.parking.app.data.repository.ParkingRepositoryImpl

class FavoritesViewModel : ViewModel() {

    private val repository = ParkingRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()

    private val _favorites = MutableLiveData<List<ParkingModel>>()
    val favorites: LiveData<List<ParkingModel>> = _favorites

    private var allParkings: List<ParkingModel> = emptyList()

    init {
        repository.observeParkings { parkings ->
            allParkings = parkings
            loadFavorites()
        }
    }

    fun loadFavorites() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val ids = repository.loadFavorites(userId)
            _favorites.value = allParkings.filter { it.id in ids }
        }
    }
}
