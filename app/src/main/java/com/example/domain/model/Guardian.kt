package com.example.domain.model

data class Guardian(
    val id: String,
    val name: String,
    val relation: String,
    val phone: String,
    val isActive: Boolean = true
)
