package com.bcaste.lifetimeline.ui.screens.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcaste.lifetimeline.data.ProfileManager
import com.bcaste.lifetimeline.data.TimelineRepository
import com.bcaste.lifetimeline.data.local.entity.Category
import com.bcaste.lifetimeline.data.local.entity.EventType
import com.bcaste.lifetimeline.data.local.entity.Profile
import com.bcaste.lifetimeline.data.local.entity.TimelineEvent
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RegisterEventViewModel @Inject constructor(
    private val timelineRepository: TimelineRepository,
    private val profileManager: ProfileManager,
    private val analytics: FirebaseAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterEventUiState())
    val uiState: StateFlow<RegisterEventUiState> = _uiState.asStateFlow()

    val availableCategories: StateFlow<List<Category>> = profileManager.activeProfileId
        .flatMapLatest { profileId ->
            if (profileId == Profile.MAIN_ID) timelineRepository.getAllCategories()
            else timelineRepository.getCategoriesByProfile(profileId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateEventType(eventType: EventType) {
        // When switching to POINT, clear end timestamp
        val newState = if (eventType == EventType.POINT) {
            _uiState.value.copy(eventType = eventType, endTimestamp = null)
        } else {
            _uiState.value.copy(eventType = eventType)
        }
        _uiState.value = newState
    }

    fun updateStartTimestamp(timestamp: Long) {
        _uiState.value = _uiState.value.copy(startTimestamp = timestamp)
    }

    fun updateEndTimestamp(timestamp: Long) {
        _uiState.value = _uiState.value.copy(endTimestamp = timestamp)
    }

    fun toggleCategory(categoryId: String) {
        val current = _uiState.value.selectedCategoryIds.toMutableSet()
        if (current.contains(categoryId)) {
            current.remove(categoryId)
        } else {
            current.add(categoryId)
        }
        _uiState.value = _uiState.value.copy(selectedCategoryIds = current)
    }

    fun addImages(uris: List<String>) {
        val current = _uiState.value.selectedImageUris.toMutableSet()
        current.addAll(uris)
        _uiState.value = _uiState.value.copy(selectedImageUris = current)
    }

    fun removeImage(uri: String) {
        val current = _uiState.value.selectedImageUris.toMutableSet()
        current.remove(uri)
        _uiState.value = _uiState.value.copy(selectedImageUris = current)
    }

    fun saveEvent(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank()) return

        viewModelScope.launch {
            try {
                val event = TimelineEvent(
                    id = UUID.randomUUID().toString(),
                    profileId = profileManager.activeProfileId.value,
                    title = state.title,
                    description = state.description,
                    timestamp = state.startTimestamp,
                    eventType = state.eventType,
                    endTimestamp = if (state.eventType == EventType.PERIOD) state.endTimestamp else null
                )
                timelineRepository.insertEvent(
                    event = event,
                    categoryIds = state.selectedCategoryIds.toList(),
                    imageUris = state.selectedImageUris.toList()
                )
                
                analytics.logEvent("event_created") {
                    param("event_type", event.eventType.name)
                    param("profile_id", event.profileId)
                    param("images_count", state.selectedImageUris.size.toLong())
                    param("categories_count", state.selectedCategoryIds.size.toLong())
                }

                // Reset state after successful save
                _uiState.value = RegisterEventUiState()
                onSuccess()
            } catch (e: Exception) {
                // You could add an error state to uiState to show a message to the user
                e.printStackTrace()
            }
        }
    }
}

data class RegisterEventUiState(
    val title: String = "",
    val description: String = "",
    val eventType: EventType = EventType.POINT,
    val startTimestamp: Long = System.currentTimeMillis(),
    val endTimestamp: Long? = null,
    val selectedCategoryIds: Set<String> = emptySet(),
    val selectedImageUris: Set<String> = emptySet()
)
