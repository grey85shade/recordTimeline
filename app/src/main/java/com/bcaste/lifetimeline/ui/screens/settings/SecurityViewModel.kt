package com.bcaste.lifetimeline.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.bcaste.lifetimeline.data.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _isPasswordSet = MutableStateFlow(securityManager.isPasswordSet())
    val isPasswordSet: StateFlow<Boolean> = _isPasswordSet.asStateFlow()

    fun updatePassword(password: String?) {
        securityManager.setPassword(password)
        _isPasswordSet.value = securityManager.isPasswordSet()
    }
}
