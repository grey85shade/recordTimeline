package com.bcaste.lifetimeline.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcaste.lifetimeline.data.BackupManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

sealed class BackupUiState {
    object Idle : BackupUiState()
    object Loading : BackupUiState()
    object Success : BackupUiState()
    data class Error(val message: String) : BackupUiState()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val analytics: FirebaseAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun createBackup(outputStream: OutputStream?) {
        if (outputStream == null) return
        
        viewModelScope.launch {
            _uiState.value = BackupUiState.Loading
            val result = backupManager.createBackup(outputStream)
            _uiState.value = if (result.isSuccess) {
                analytics.logEvent("backup_created") {
                    param("status", "success")
                }
                BackupUiState.Success
            } else {
                analytics.logEvent("backup_created") {
                    param("status", "error")
                }
                BackupUiState.Error(result.exceptionOrNull()?.message ?: "Error desconocido al crear backup")
            }
        }
    }

    fun restoreBackup(inputStream: InputStream?) {
        if (inputStream == null) return
        
        viewModelScope.launch {
            _uiState.value = BackupUiState.Loading
            val result = backupManager.restoreBackup(inputStream)
            _uiState.value = if (result.isSuccess) {
                analytics.logEvent("backup_restored") {
                    param("status", "success")
                }
                BackupUiState.Success
            } else {
                analytics.logEvent("backup_restored") {
                    param("status", "error")
                }
                BackupUiState.Error(result.exceptionOrNull()?.message ?: "Error desconocido al restaurar backup")
            }
        }
    }

    fun resetState() {
        _uiState.value = BackupUiState.Idle
    }
}
