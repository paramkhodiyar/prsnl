package com.prsnl.core.user

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserProfileManager(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _userName = MutableStateFlow(prefs.getString(KEY_USER_NAME, "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _isWelcomeCompleted = MutableStateFlow(prefs.getBoolean(KEY_WELCOME_COMPLETED, false))
    val isWelcomeCompleted: StateFlow<Boolean> = _isWelcomeCompleted.asStateFlow()

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""

    fun isWelcomeCompleted(): Boolean = prefs.getBoolean(KEY_WELCOME_COMPLETED, false)

    fun saveUserProfile(name: String, completedWelcome: Boolean = true) {
        val trimmed = name.trim()
        prefs.edit()
            .putString(KEY_USER_NAME, trimmed)
            .putBoolean(KEY_WELCOME_COMPLETED, completedWelcome)
            .apply()
        _userName.value = trimmed
        _isWelcomeCompleted.value = completedWelcome
    }

    fun setWelcomeCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_WELCOME_COMPLETED, completed).apply()
        _isWelcomeCompleted.value = completed
    }

    companion object {
        private const val PREFS_NAME = "prsnl_user_profile"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_WELCOME_COMPLETED = "key_welcome_completed"

        @Volatile
        private var INSTANCE: UserProfileManager? = null

        fun getInstance(context: Context): UserProfileManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserProfileManager(context).also { INSTANCE = it }
            }
        }
    }
}
