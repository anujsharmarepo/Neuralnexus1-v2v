package com.example.data.repository

import android.content.Context
import com.example.domain.model.Guardian
import com.example.domain.repository.GuardiansRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GuardiansRepositoryImpl(private val context: Context) : GuardiansRepository {
    private val prefs = context.getSharedPreferences("abhaya_guardians_prefs", Context.MODE_PRIVATE)
    
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val listType = Types.newParameterizedType(List::class.java, Guardian::class.java)
    private val adapter = moshi.adapter<List<Guardian>>(listType)

    private val defaultGuardians = listOf(
        Guardian("1", "Mom", "Mother", "+1-555-0192", isActive = true),
        Guardian("2", "Dad", "Father", "+1-555-0193", isActive = true),
        Guardian("3", "Vikram", "Brother", "+1-555-0194", isActive = true),
        Guardian("4", "Ananya", "Best Friend", "+1-555-0195", isActive = true),
        Guardian("5", "Officer Shinde", "Local Helpline", "100", isActive = true)
    )

    private val _guardians = MutableStateFlow<List<Guardian>>(loadGuardians())
    override val guardians: StateFlow<List<Guardian>> = _guardians.asStateFlow()

    private fun loadGuardians(): List<Guardian> {
        val json = prefs.getString("guardians_json", null)
        if (json.isNullOrBlank()) {
            return defaultGuardians
        }
        return try {
            adapter.fromJson(json) ?: defaultGuardians
        } catch (e: Exception) {
            e.printStackTrace()
            defaultGuardians
        }
    }

    private fun saveGuardians(list: List<Guardian>) {
        try {
            val json = adapter.toJson(list)
            prefs.edit().putString("guardians_json", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun addGuardian(guardian: Guardian): Result<Unit> {
        val currentList = _guardians.value.toMutableList()
        currentList.add(guardian)
        _guardians.value = currentList
        saveGuardians(currentList)
        return Result.success(Unit)
    }

    override suspend fun removeGuardian(id: String): Result<Unit> {
        val currentList = _guardians.value.filter { it.id != id }
        _guardians.value = currentList
        saveGuardians(currentList)
        return Result.success(Unit)
    }

    override suspend fun toggleGuardianActive(id: String): Result<Unit> {
        val currentList = _guardians.value.map {
            if (it.id == id) it.copy(isActive = !it.isActive) else it
        }
        _guardians.value = currentList
        saveGuardians(currentList)
        return Result.success(Unit)
    }
}
