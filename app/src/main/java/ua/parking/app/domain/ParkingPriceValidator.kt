package ua.parking.app.domain

class ParkingPriceValidator {

    data class ValidationResult(val isValid: Boolean, val errorMessage: String = "")

    companion object {
        const val MAX_PRICE = 1_000.0
        const val MIN_PRICE = 1.0
    }

    fun validate(rawInput: String): Pair<ValidationResult, Double?> {
        if (rawInput.isBlank()) return Pair(ValidationResult(true), null)

        val price = rawInput.trim().replace(",", ".").toDoubleOrNull()
            ?: return Pair(ValidationResult(false, "Введіть коректне число"), null)

        if (price < MIN_PRICE)
            return Pair(ValidationResult(false, "Мінімальна ціна — ${MIN_PRICE.toInt()} грн/год"), null)
        if (price > MAX_PRICE)
            return Pair(ValidationResult(false, "Максимальна ціна — ${MAX_PRICE.toInt()} грн/год"), null)

        return Pair(ValidationResult(true), price)
    }
}
