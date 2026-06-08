package com.example.stepwars2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepwars2.data.model.User
import com.example.stepwars2.data.repository.AuthRepository
import com.example.stepwars2.data.repository.UserRepository
import com.example.stepwars2.data.repository.UserStateManager
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow<Boolean?>(null) // null = checking
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            val loggedIn = authRepository.isLoggedIn
            _isLoggedIn.value = loggedIn
            if (loggedIn) {
                UserStateManager.startListening()
                // Kartları kontrol et — yoksa başlangıç kartları ver
                val uid = authRepository.currentUser?.uid
                if (uid != null) {
                    userRepository.ensureUserHasCards(uid)
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.loginWithEmail(email, password)
            result.fold(
                onSuccess = { user ->
                    UserStateManager.startListening()
                    // Kartları kontrol et
                    userRepository.ensureUserHasCards(user.uid)
                    _authState.value = AuthState.Success(user)
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(
                        getErrorMessage(exception)
                    )
                }
            )
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.registerWithEmail(email, password)
            result.fold(
                onSuccess = { firebaseUser ->
                    try {
                        // 1. Firebase Auth displayName ayarla
                        val profileUpdates = userProfileChangeRequest {
                            displayName = username
                        }
                        firebaseUser.updateProfile(profileUpdates).await()

                        // 2. RTDB'ye kullanıcıyı doğru isimle yaz (startListening'den ÖNCE)
                        val db = FirebaseDatabase.getInstance("https://stepwars2-f0e9e-default-rtdb.europe-west1.firebasedatabase.app")
                        val user = User(
                            uid = firebaseUser.uid,
                            username = username,
                            email = email
                        )
                        db.reference.child("users").child(firebaseUser.uid)
                            .setValue(user.toMap()).await()

                        // 3. Şimdi listener başlat (RTDB'de kullanıcı zaten var)
                        UserStateManager.startListening()

                        // 4. Başlangıç kartları ver
                        userRepository.createUser(user)
                    } catch (_: Exception) {}

                    _authState.value = AuthState.Success(firebaseUser)
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(
                        getErrorMessage(exception)
                    )
                }
            )
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signInWithGoogleCredential(idToken)
            result.fold(
                onSuccess = { firebaseUser ->
                    // Check if user exists
                    val existsResult = userRepository.userExists(firebaseUser.uid)
                    val exists = existsResult.getOrDefault(false)
                    if (!exists) {
                        // Create new user profile in both RTDB and Firestore
                        val user = User(
                            uid = firebaseUser.uid,
                            username = firebaseUser.displayName ?: "Savaşçı",
                            email = firebaseUser.email ?: "",
                            avatarUrl = firebaseUser.photoUrl?.toString() ?: ""
                        )
                        // RTDB'ye yaz (ana veri kaynağı)
                        val db = FirebaseDatabase.getInstance("https://stepwars2-f0e9e-default-rtdb.europe-west1.firebasedatabase.app")
                        try {
                            db.reference.child("users").child(firebaseUser.uid)
                                .setValue(user.toMap()).await()
                        } catch (_: Exception) {}
                        // Başlangıç kartları ver
                        userRepository.createUser(user)
                    }
                    UserStateManager.startListening()
                    // Kartları kontrol et
                    userRepository.ensureUserHasCards(firebaseUser.uid)
                    _authState.value = AuthState.Success(firebaseUser)
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(
                        getErrorMessage(exception)
                    )
                }
            )
        }
    }

    fun signOut() {
        UserStateManager.stopListening()
        authRepository.signOut()
        _authState.value = AuthState.Idle
        _isLoggedIn.value = false
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    private fun getErrorMessage(exception: Throwable): String {
        return when {
            exception.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ||
            exception.message?.contains("INVALID_EMAIL") == true ->
                "E-posta veya şifre hatalı"
            exception.message?.contains("USER_NOT_FOUND") == true ->
                "Bu e-posta ile kayıtlı kullanıcı bulunamadı"
            exception.message?.contains("EMAIL_EXISTS") == true ||
            exception.message?.contains("email-already-in-use") == true ->
                "Bu e-posta zaten kullanılıyor"
            exception.message?.contains("WEAK_PASSWORD") == true ->
                "Şifre en az 6 karakter olmalı"
            exception.message?.contains("NETWORK") == true ->
                "İnternet bağlantınızı kontrol edin"
            else -> exception.message ?: "Bilinmeyen bir hata oluştu"
        }
    }
}
