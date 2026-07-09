package com.example.data.repository

import android.content.Context
import com.example.domain.model.User
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay

class UserRepositoryImpl(private val context: Context) : UserRepository {
    private val prefs = context.getSharedPreferences("abhaya_user_prefs", Context.MODE_PRIVATE)

    // Default current user to null or previous session from SharedPreferences
    private val _currentUser = MutableStateFlow<User?>(
        if (prefs.getBoolean("is_logged_in", false)) {
            User(
                uid = prefs.getString("uid", "user_123") ?: "user_123",
                name = prefs.getString("name", "Anuj") ?: "Anuj",
                email = prefs.getString("email", "") ?: "",
                isGpsEnabled = prefs.getBoolean("is_gps_enabled", true),
                isInternetEnabled = prefs.getBoolean("is_internet_enabled", true),
                isProtectionActive = prefs.getBoolean("is_protection_active", true)
            )
        } else {
            null
        }
    )
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun login(email: String, password: String): Result<User> {
        delay(1000) // Simulate network/auth lag
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Email and password cannot be empty."))
        }
        if (password.length < 6) {
            return Result.failure(Exception("Password must be at least 6 characters."))
        }
        // Match user's "Anuj" screen requirements:
        val name = if (email.startsWith("anuj", ignoreCase = true)) "Anuj" else email.substringBefore("@")
        val user = User(
            uid = "user_123",
            name = name,
            email = email,
            isGpsEnabled = true,
            isInternetEnabled = true,
            isProtectionActive = true
        )
        _currentUser.value = user
        
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("uid", user.uid)
            putString("name", user.name)
            putString("email", user.email)
            putBoolean("is_gps_enabled", user.isGpsEnabled)
            putBoolean("is_internet_enabled", user.isInternetEnabled)
            putBoolean("is_protection_active", user.isProtectionActive)
            apply()
        }
        
        return Result.success(user)
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<User> {
        delay(1200) // Simulate auth lag
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            return Result.failure(Exception("All fields are required."))
        }
        if (password.length < 6) {
            return Result.failure(Exception("Password must be at least 6 characters."))
        }
        val user = User(
            uid = "user_new",
            name = name,
            email = email,
            isGpsEnabled = true,
            isInternetEnabled = true,
            isProtectionActive = true
        )
        _currentUser.value = user

        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("uid", user.uid)
            putString("name", user.name)
            putString("email", user.email)
            putBoolean("is_gps_enabled", user.isGpsEnabled)
            putBoolean("is_internet_enabled", user.isInternetEnabled)
            putBoolean("is_protection_active", user.isProtectionActive)
            apply()
        }

        return Result.success(user)
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        delay(800)
        if (email.isBlank()) {
            return Result.failure(Exception("Please enter your email address."))
        }
        return Result.success(Unit)
    }

    override suspend fun logout(): Result<Unit> {
        prefs.edit().apply {
            putBoolean("is_logged_in", false)
            remove("uid")
            remove("name")
            remove("email")
            putBoolean("is_gps_enabled", true)
            putBoolean("is_internet_enabled", true)
            putBoolean("is_protection_active", true)
            apply()
        }
        _currentUser.value = null
        return Result.success(Unit)
    }

    override suspend fun updateProtectionStatus(gps: Boolean, internet: Boolean, active: Boolean): Result<Unit> {
        val current = _currentUser.value ?: return Result.failure(Exception("No active user session."))
        val updated = current.copy(
            isGpsEnabled = gps,
            isInternetEnabled = internet,
            isProtectionActive = active
        )
        _currentUser.value = updated

        prefs.edit().apply {
            putBoolean("is_gps_enabled", gps)
            putBoolean("is_internet_enabled", internet)
            putBoolean("is_protection_active", active)
            apply()
        }

        return Result.success(Unit)
    }
}
