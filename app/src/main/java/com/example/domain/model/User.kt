package com.example.domain.model

data class User(
    val uid: String,
    val name: String,
    val email: String,
    val isGpsEnabled: Boolean = true,
    val isInternetEnabled: Boolean = true,
    val isProtectionActive: Boolean = true
)
