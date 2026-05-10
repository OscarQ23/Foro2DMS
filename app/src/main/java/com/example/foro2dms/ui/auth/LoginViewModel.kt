package com.example.foro2dms.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foro2dms.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false
)

class LoginViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        if (repository.currentUser != null) {
            _uiState.value = _uiState.value.copy(isAuthenticated = true)
        }
    }

    fun login(email: String, password: String) {
        if (!validateInput(email, password)) return

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            repository.signIn(email, password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = mapErrorToMessage(e)
                    )
                }
        }
    }

    fun register(email: String, password: String) {
        if (!validateInput(email, password)) return

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            repository.signUp(email, password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = mapErrorToMessage(e)
                    )
                }
        }
    }

    fun signOut() {
        repository.signOut()
        _uiState.value = LoginUiState()
    }

    private fun validateInput(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "El correo no puede estar vacío"
                )
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "El correo no tiene un formato válido"
                )
                false
            }
            password.length < 6 -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "La contraseña debe tener al menos 6 caracteres"
                )
                false
            }
            else -> true
        }
    }

    private fun mapErrorToMessage(e: Throwable): String {
        return when (e) {
            is FirebaseAuthInvalidUserException ->
                "No existe una cuenta con ese correo"
            is FirebaseAuthInvalidCredentialsException ->
                "Credenciales incorrectas. Verifica correo y contraseña"
            is FirebaseAuthUserCollisionException ->
                "Ya existe una cuenta con ese correo. Usa Ingresar"
            is FirebaseAuthWeakPasswordException ->
                "La contraseña es muy débil. Usa al menos 6 caracteres"
            else -> e.localizedMessage ?: "Error desconocido. Intenta de nuevo"
        }
    }
}
