package com.bcaste.lifetimeline.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcaste.lifetimeline.data.TimelineRepository
import com.bcaste.lifetimeline.data.local.entity.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProfileManagerViewModel @Inject constructor(
    private val repository: TimelineRepository
) : ViewModel() {

    val profiles: StateFlow<List<Profile>> = repository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addProfile(name: String, isVisibleInMain: Boolean, color: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val profile = Profile(
                id = UUID.randomUUID().toString(),
                name = name,
                isVisibleInMain = isVisibleInMain,
                color = color
            )
            repository.insertProfile(profile)
        }
    }

    fun updateProfile(profile: Profile) {
        viewModelScope.launch {
            repository.insertProfile(profile)
        }
    }

    fun deleteProfile(profile: Profile) {
        if (profile.id == Profile.MAIN_ID) return
        viewModelScope.launch {
            repository.deleteProfile(profile)
        }
    }
}
