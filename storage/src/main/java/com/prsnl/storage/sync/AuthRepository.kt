package com.prsnl.storage.sync

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val prefs = context.getSharedPreferences("prsnl_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                prefs.edit()
                    .putString("cached_user_id", user.uid)
                    .putString("cached_user_email", user.email)
                    .apply()
            }
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                prefs.edit()
                    .putString("cached_user_id", user.uid)
                    .putString("cached_user_email", user.email)
                    .apply()
                Result.success(user)
            } else {
                Result.failure(Exception("User was null after Google sign-in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    fun isUserSignedIn(): Boolean = auth.currentUser != null || prefs.getString("cached_user_id", null) != null

    fun getUserId(): String? = auth.currentUser?.uid ?: prefs.getString("cached_user_id", null)

    fun getUserEmail(): String? = auth.currentUser?.email ?: prefs.getString("cached_user_email", null)
}
