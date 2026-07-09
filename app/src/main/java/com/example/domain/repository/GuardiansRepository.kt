package com.example.domain.repository

import com.example.domain.model.Guardian
import kotlinx.coroutines.flow.StateFlow

interface GuardiansRepository {
    val guardians: StateFlow<List<Guardian>>
    
    suspend fun addGuardian(guardian: Guardian): Result<Unit>
    suspend fun removeGuardian(id: String): Result<Unit>
    suspend fun toggleGuardianActive(id: String): Result<Unit>
}
