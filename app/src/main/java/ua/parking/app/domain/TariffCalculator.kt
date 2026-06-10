package ua.parking.app.domain

import kotlin.math.ceil
import kotlin.math.roundToLong

class TariffCalculator {

    fun calculate(elapsedSeconds: Long, pricePerHour: Double?): Double {
        if (pricePerHour == null || pricePerHour <= 0.0) return 0.0
        if (elapsedSeconds <= 0) return 0.0

        val pricePerMinute = pricePerHour / 60.0
        val minutes = ceil(elapsedSeconds / 60.0)
        val raw = minutes * pricePerMinute
        return (raw * 100.0).roundToLong() / 100.0
    }

    fun formatCost(cost: Double): String =
        if (cost <= 0.0) "Безкоштовно" else "%.2f грн".format(cost)
}
