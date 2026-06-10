package ua.parking.app.domain

class AuthValidator {

    data class ValidationResult(val isValid: Boolean, val errorMessage: String = "")

    fun validateEmail(email: String): ValidationResult {
        if (email.isBlank()) return ValidationResult(false, "Поле не може бути порожнім")
        val regex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return if (regex.matches(email.trim())) ValidationResult(true)
        else ValidationResult(false, "Некоректний формат email")
    }

    fun validatePassword(password: String): ValidationResult {
        if (password.length < 6) return ValidationResult(false, "Пароль занадто короткий")
        if (!password.any { it.isDigit() }) return ValidationResult(false, "Пароль повинен містити хоча б одну цифру")
        return ValidationResult(true)
    }
}
