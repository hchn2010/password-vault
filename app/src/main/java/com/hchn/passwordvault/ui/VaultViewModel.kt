package com.hchn.passwordvault.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.hchn.passwordvault.data.VaultRepository
import com.hchn.passwordvault.model.VaultEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.SecureRandom

data class VaultUiState(
    val configured: Boolean = false,
    val unlocked: Boolean = false,
    val entries: List<VaultEntry> = emptyList(),
    val query: String = "",
    val error: String? = null
) {
    val filteredEntries: List<VaultEntry>
        get() {
            val keyword = query.trim()
            return if (keyword.isBlank()) entries else entries.filter {
                it.title.contains(keyword, true) ||
                    it.username.contains(keyword, true) ||
                    it.website.contains(keyword, true)
            }
        }
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VaultRepository(application)
    private val _uiState = MutableStateFlow(VaultUiState(configured = repository.isConfigured))
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    fun createVault(password: String, confirmation: String) {
        if (password != confirmation) return showError("两次输入的主密码不一致")
        runCatching { repository.create(password.toCharArray()) }
            .onSuccess { _uiState.value = VaultUiState(configured = true, unlocked = true) }
            .onFailure { showError(it.message ?: "创建失败") }
    }

    fun unlock(password: String) {
        runCatching { repository.unlock(password.toCharArray()) }
            .onSuccess { entries ->
                _uiState.update { it.copy(unlocked = true, entries = entries, error = null) }
            }
            .onFailure { showError(it.message ?: "解锁失败") }
    }

    fun lock() {
        repository.lock()
        _uiState.update { it.copy(unlocked = false, entries = emptyList(), query = "", error = null) }
    }

    fun setQuery(value: String) = _uiState.update { it.copy(query = value) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun saveEntry(entry: VaultEntry) {
        val current = _uiState.value.entries
        val updated = if (current.any { it.id == entry.id }) {
            current.map { if (it.id == entry.id) entry.copy(updatedAt = System.currentTimeMillis()) else it }
        } else {
            listOf(entry) + current
        }
        persist(updated)
    }

    fun deleteEntry(entry: VaultEntry) = persist(_uiState.value.entries.filterNot { it.id == entry.id })

    private fun persist(entries: List<VaultEntry>) {
        runCatching { repository.save(entries) }
            .onSuccess { _uiState.update { state -> state.copy(entries = entries, error = null) } }
            .onFailure { showError(it.message ?: "保存失败") }
    }

    private fun showError(message: String) = _uiState.update { it.copy(error = message) }

    companion object {
        private val random = SecureRandom()
        private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#%&*+-_="

        fun generatePassword(length: Int = 20): String = buildString(length) {
            repeat(length) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
        }
    }
}

