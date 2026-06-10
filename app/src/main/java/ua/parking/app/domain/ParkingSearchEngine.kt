package ua.parking.app.domain

import ua.parking.app.data.model.ParkingModel

class ParkingSearchEngine {
    fun search(list: List<ParkingModel>, query: String): List<ParkingModel> {
        if (query.isBlank()) return list
        val q = query.trim().lowercase()
        return list.filter {
            it.name.lowercase().contains(q) || it.address.lowercase().contains(q)
        }
    }
}
