package ua.parking.app.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import ua.parking.app.data.repository.AuthRepository
import ua.parking.app.data.repository.AuthRepositoryImpl
import ua.parking.app.domain.AuthValidator

class AuthViewModel : ViewModel() {

    private val repository: AuthRepository = AuthRepositoryImpl()
    private val validator = AuthValidator()

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    fun signIn(email: String, password: String) {
        val emailResult = validator.validateEmail(email)
        if (!emailResult.isValid) { _authState.value = AuthState.Error(emailResult.errorMessage); return }
        val passResult = validator.validatePassword(password)
        if (!passResult.isValid) { _authState.value = AuthState.Error(passResult.errorMessage); return }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            repository.signIn(email, password).fold(
                onSuccess = { _authState.value = AuthState.Success(it) },
                onFailure = { _authState.value = AuthState.Error(it.message ?: "Помилка входу") }
            )
        }
    }

    fun signUp(email: String, password: String, confirmPassword: String) {
        val emailResult = validator.validateEmail(email)
        if (!emailResult.isValid) { _authState.value = AuthState.Error(emailResult.errorMessage); return }
        val passResult = validator.validatePassword(password)
        if (!passResult.isValid) { _authState.value = AuthState.Error(passResult.errorMessage); return }
        if (password != confirmPassword) { _authState.value = AuthState.Error("Паролі не співпадають"); return }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            repository.signUp(email, password).fold(
                onSuccess = { _authState.value = AuthState.Success(it) },
                onFailure = { _authState.value = AuthState.Error(it.message ?: "Помилка реєстрації") }
            )
        }
    }

    fun signInWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            repository.signInWithGoogle(idToken).fold(
                onSuccess = { _authState.value = AuthState.Success(it) },
                onFailure = { _authState.value = AuthState.Error(it.message ?: "Помилка Google входу") }
            )
        }
    }

    fun signOut() = repository.signOut()
    fun getCurrentUser(): FirebaseUser? = repository.getCurrentUser()
}

sealed class AuthState {
    object Loading : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}
