package com.bcaste.lifetimeline.ui.screens.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcaste.lifetimeline.data.ProfileManager
import com.bcaste.lifetimeline.data.TimelineRepository
import com.bcaste.lifetimeline.data.local.entity.Category
import com.bcaste.lifetimeline.data.local.entity.EventWithDetails
import com.bcaste.lifetimeline.data.local.entity.Profile
import com.bcaste.lifetimeline.data.local.entity.TimelineEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class TimelineUiState(
    val events: List<EventWithDetails> = emptyList(),
    val categories: List<Category> = emptyList(),
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: String = Profile.MAIN_ID,
    val searchQuery: String = "",
    val selectedCategoryIds: Set<String> = emptySet(),
    val selectedDate: Long? = null,
    val isCurrentYearFilterActive: Boolean = false,
    val isCurrentMonthFilterActive: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repository: TimelineRepository,
    private val profileManager: ProfileManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryIds = MutableStateFlow<Set<String>>(emptySet())
    private val _selectedDate = MutableStateFlow<Long?>(null)
    private val _isCurrentYearFilterActive = MutableStateFlow(false)
    private val _isCurrentMonthFilterActive = MutableStateFlow(false)

    val uiState: StateFlow<TimelineUiState> = combine(
        profileManager.activeProfileId.flatMapLatest { repository.getEventsByProfile(it) },
        profileManager.activeProfileId.flatMapLatest { repository.getCategoriesForProfile(it) },
        repository.getAllProfiles(),
        profileManager.activeProfileId,
        _searchQuery,
        _selectedCategoryIds,
        _selectedDate,
        _isCurrentYearFilterActive,
        _isCurrentMonthFilterActive
    ) { flows ->
        val allEvents = flows[0] as List<EventWithDetails>
        val categories = flows[1] as List<Category>
        val profiles = flows[2] as List<Profile>
        val activeId = flows[3] as String
        val query = flows[4] as String
        val categoryIds = flows[5] as Set<String>
        val date = flows[6] as Long?
        val currentYear = flows[7] as Boolean
        val currentMonth = flows[8] as Boolean

        val filteredEvents = allEvents.filter { eventWithDetails ->
            val event = eventWithDetails.event
            
            // Search Filter
            val matchesQuery = query.isBlank() || 
                event.title.contains(query, ignoreCase = true) || 
                event.description.contains(query, ignoreCase = true)
            
            // Category Filter
            val matchesCategory = categoryIds.isEmpty() || 
                eventWithDetails.categories.any { it.id in categoryIds }
            
            // Date Filter
            val cal = Calendar.getInstance()
            val eventCal = Calendar.getInstance().apply { timeInMillis = event.timestamp }
            
            val matchesDate = date == null || isSameDay(event.timestamp, date)
            
            // Current Year Filter
            val matchesYear = !currentYear || eventCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
            
            // Current Month Filter
            val matchesMonth = !currentMonth || (
                eventCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                eventCal.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
            )

            matchesQuery && matchesCategory && matchesDate && matchesYear && matchesMonth
        }

        TimelineUiState(
            events = filteredEvents,
            categories = categories,
            profiles = profiles,
            activeProfileId = activeId,
            searchQuery = query,
            selectedCategoryIds = categoryIds,
            selectedDate = date,
            isCurrentYearFilterActive = currentYear,
            isCurrentMonthFilterActive = currentMonth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimelineUiState()
    )

    fun setActiveProfile(profileId: String) {
        profileManager.setActiveProfile(profileId)
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleCategory(categoryId: String) {
        val current = _selectedCategoryIds.value.toMutableSet()
        if (current.contains(categoryId)) {
            current.remove(categoryId)
        } else {
            current.add(categoryId)
        }
        _selectedCategoryIds.value = current
    }

    fun updateSelectedDate(date: Long?) {
        _selectedDate.value = date
    }

    fun toggleCurrentYearFilter() {
        _isCurrentYearFilterActive.value = !_isCurrentYearFilterActive.value
        if (_isCurrentYearFilterActive.value) _isCurrentMonthFilterActive.value = false
    }

    fun toggleCurrentMonthFilter() {
        _isCurrentMonthFilterActive.value = !_isCurrentMonthFilterActive.value
        if (_isCurrentMonthFilterActive.value) _isCurrentYearFilterActive.value = false
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategoryIds.value = emptySet()
        _selectedDate.value = null
        _isCurrentYearFilterActive.value = false
        _isCurrentMonthFilterActive.value = false
    }

    fun deleteEvent(event: TimelineEvent) {
        viewModelScope.launch {
            repository.deleteEvent(event)
        }
    }
}
