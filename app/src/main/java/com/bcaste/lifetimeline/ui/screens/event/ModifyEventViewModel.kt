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
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ModifyEventViewModel @Inject constructor(
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

    private var eventId: String? = null
    private var eventProfileId: String = Profile.MAIN_ID

    fun loadEvent(id: String) {
        if (eventId == id) return
        eventId = id
        viewModelScope.launch {
            val eventWithDetails = timelineRepository.getEventById(id).filterNotNull().first()
            val event = eventWithDetails.event
            eventProfileId = event.profileId
            _uiState.value = RegisterEventUiState(
                title = event.title,
                description = event.description,
                eventType = event.eventType,
                startTimestamp = event.timestamp,
                endTimestamp = event.endTimestamp,
                selectedCategoryIds = eventWithDetails.categories.map { it.id }.toSet(),
                selectedImageUris = eventWithDetails.images.map { it.imageUri }.toSet()
            )
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateEventType(eventType: EventType) {
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
        val id = eventId ?: return
        if (state.title.isBlank()) return

        viewModelScope.launch {
            val event = TimelineEvent(
                id = id,
                profileId = eventProfileId,
                title = state.title,
                description = state.description,
                timestamp = state.startTimestamp,
                eventType = state.eventType,
                endTimestamp = if (state.eventType == EventType.PERIOD) state.endTimestamp else null
            )
            timelineRepository.updateEvent(
                event = event,
                categoryIds = state.selectedCategoryIds.toList(),
                imageUris = state.selectedImageUris.toList()
            )

            analytics.logEvent("event_modified") {
                param("event_type", event.eventType.name)
                param("profile_id", event.profileId)
                param("images_count", state.selectedImageUris.size.toLong())
                param("categories_count", state.selectedCategoryIds.size.toLong())
            }

            onSuccess()
        }
    }
}
