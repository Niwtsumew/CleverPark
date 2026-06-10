package ua.parking.app.domain

import ua.parking.app.data.model.ParkingModel

class ParkingFilter {
    fun filter(list: List<ParkingModel>, type: FilterType): List<ParkingModel> = when (type) {
        FilterType.ALL       -> list
        FilterType.FREE_ONLY -> list.filter { it.isFree }
        FilterType.PAID_ONLY -> list.filter { it.isPaid }
    }
}
