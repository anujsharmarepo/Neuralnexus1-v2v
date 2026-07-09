package com.example.data.repository

import com.example.domain.model.Guardian
import com.example.domain.repository.GuardiansRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GuardiansRepositoryImpl : GuardiansRepository {
    private val defaultGuardians = listOf(
        Guardian("1", "Mom", "Mother", "+1-555-0192", isActive = true),
        Guardian("2", "Dad", "Father", "+1-555-0193", isActive = true),
        Guardian("3", "Vikram", "Brother", "+1-555-0194", isActive = true),
        Guardian("4", "Ananya", "Best Friend", "+1-555-0195", isActive = true),
        Guardian("5", "Officer Shinde", "Local Helpline", "100", isActive = true)
    )

    private val _guardians = MutableStateFlow<List<Guardian>>(defaultGuardians)
    override val guardians: StateFlow<List<Guardian>> = _guardians.asStateFlow()

    override suspend fun addGuardian(guardian: Guardian): Result<Unit> {
        val currentList = _guardians.value.toMutableList()
        currentList.add(guardian)
        _guardians.value = currentList
        return Result.success(Unit)
    }

    override suspend fun removeGuardian(id: String): Result<Unit> {
        val currentList = _guardians.value.filter { it.id != id }
        _guardians.value = currentList
        return Result.success(Unit)
    }

    override suspend fun toggleGuardianActive(id: String): Result<Unit> {
        val currentList = _guardians.value.map {
            if (it.id == id) it.copy(isActive = !it.isActive) else it
        }
        _guardians.value = currentList
        return Result.success(Unit)
    }
}
