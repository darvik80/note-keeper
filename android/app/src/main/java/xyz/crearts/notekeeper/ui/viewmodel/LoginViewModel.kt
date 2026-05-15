package xyz.crearts.notekeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.crearts.notekeeper.data.local.TokenDataStore
import xyz.crearts.notekeeper.data.model.AuthRequest
import xyz.crearts.notekeeper.data.remote.api.AuthApiService

class LoginViewModel(
    private val authApiService: AuthApiService,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            tokenDataStore.token.collect { token ->
                _isLoggedIn.value = token != null
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = authApiService.login(AuthRequest(email, password))
                if (response.isSuccessful) {
                    response.body()?.token?.let { token ->
                        tokenDataStore.saveToken(token)
                    }
                } else {
                    _error.value = "Invalid credentials"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = authApiService.register(AuthRequest(email, password, name))
                if (response.isSuccessful) {
                    response.body()?.token?.let { token ->
                        tokenDataStore.saveToken(token)
                    }
                } else {
                    _error.value = "Registration failed"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenDataStore.clearToken()
        }
    }

    class Factory(
        private val authApiService: AuthApiService,
        private val tokenDataStore: TokenDataStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(authApiService, tokenDataStore) as T
        }
    }
}
