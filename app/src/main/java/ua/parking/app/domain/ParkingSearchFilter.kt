package ua.parking.app.domain

import ua.parking.app.data.model.ParkingModel

class ParkingSearchFilter {
    fun filter(list: List<ParkingModel>, query: String): List<ParkingModel> {
        if (query.isBlank()) return list
        return list.filter { it.name.lowercase().contains(query.trim().lowercase()) }
    }
}
