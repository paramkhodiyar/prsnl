package com.prsnl.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.prsnl.storage.sync.AuthRepository
import com.prsnl.storage.sync.FirestoreSyncEngine
import com.prsnl.storage.sync.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncEngine: FirestoreSyncEngine
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUser
    val syncState: StateFlow<SyncState> = syncEngine.syncState
    val lastSyncTimestamp: StateFlow<Long?> = syncEngine.lastSyncTimestamp
    val syncErrorMessage: StateFlow<String?> = syncEngine.syncErrorMessage

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    fun onGoogleSignInToken(idToken: String) {
        viewModelScope.launch {
            _isSigningIn.value = true
            _authError.value = null
            val result = authRepository.signInWithGoogle(idToken)
            _isSigningIn.value = false
            result.onSuccess {
                // Auto trigger sync on successful sign-in
                triggerSync()
            }.onFailure { e ->
                _authError.value = e.localizedMessage ?: "Google Sign-In failed"
            }
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            syncEngine.syncAll()
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
