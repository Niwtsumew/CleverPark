package ua.parking.app.ui.parking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ua.parking.app.data.model.ParkingModel
import ua.parking.app.data.repository.ParkingRepositoryImpl
import ua.parking.app.domain.FilterType
import ua.parking.app.domain.ParkingFilter
import ua.parking.app.domain.ParkingSearchEngine

class ParkingListViewModel : ViewModel() {

    private val repository = ParkingRepositoryImpl()
    private val searchEngine = ParkingSearchEngine()
    private val parkingFilter = ParkingFilter()

    private val _allParkings = MutableLiveData<List<ParkingModel>>()
    private val _filteredParkings = MutableLiveData<List<ParkingModel>>()
    val filteredParkings: LiveData<List<ParkingModel>> = _filteredParkings

    private var currentQuery = ""
    private var currentFilter = FilterType.ALL

    init {
        repository.observeParkings { parkings ->
            _allParkings.value = parkings
            applyFilter()
        }
    }

    fun search(query: String) {
        currentQuery = query
        applyFilter()
    }

    fun setFilter(type: FilterType) {
        currentFilter = type
        applyFilter()
    }

    private fun applyFilter() {
        val all = _allParkings.value ?: emptyList()
        val searched = searchEngine.search(all, currentQuery)
        _filteredParkings.value = parkingFilter.filter(searched, currentFilter)
    }
}
