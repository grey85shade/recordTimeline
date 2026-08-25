package com.bcaste.lifetimeline.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcaste.lifetimeline.data.ProfileManager
import com.bcaste.lifetimeline.data.TimelineRepository
import com.bcaste.lifetimeline.data.local.entity.Category
import com.bcaste.lifetimeline.data.local.entity.EventWithDetails
import com.bcaste.lifetimeline.data.local.entity.TimelineEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

enum class CalendarViewMode {
    MONTH, YEAR
}

data class CalendarUiState(
    val currentMonth: Calendar = Calendar.getInstance(),
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedDate: Calendar = Calendar.getInstance(),
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val events: List<EventWithDetails> = emptyList(),
    val availableCategories: List<Category> = emptyList(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val activeProfileId: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: TimelineRepository,
    private val profileManager: ProfileManager
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) })
    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    private val _viewMode = MutableStateFlow(CalendarViewMode.MONTH)
    private val _selectedCategoryIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<CalendarUiState> = combine(
        _currentMonth,
        _currentYear,
        _selectedDate,
        _viewMode,
        profileManager.activeProfileId.flatMapLatest { repository.getEventsByProfile(it) },
        profileManager.activeProfileId.flatMapLatest { repository.getCategoriesForProfile(it) },
        profileManager.activeProfileId,
        _selectedCategoryIds
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val month = flows[0] as Calendar
        @Suppress("UNCHECKED_CAST")
        val year = flows[1] as Int
        @Suppress("UNCHECKED_CAST")
        val selected = flows[2] as Calendar
        @Suppress("UNCHECKED_CAST")
        val mode = flows[3] as CalendarViewMode
        @Suppress("UNCHECKED_CAST")
        val allEvents = flows[4] as List<EventWithDetails>
        @Suppress("UNCHECKED_CAST")
        val categories = flows[5] as List<Category>
        @Suppress("UNCHECKED_CAST")
        val profileId = flows[6] as String
        @Suppress("UNCHECKED_CAST")
        val selectedIds = flows[7] as Set<String>

        val filteredEvents = if (selectedIds.isEmpty()) {
            allEvents
        } else {
            allEvents.filter { event ->
                event.categories.any { it.id in selectedIds }
            }
        }

        CalendarUiState(
            currentMonth = month,
            currentYear = year,
            selectedDate = selected,
            viewMode = mode,
            events = filteredEvents,
            availableCategories = categories,
            selectedCategoryIds = selectedIds,
            activeProfileId = profileId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    fun selectDate(date: Calendar) {
        _selectedDate.value = date
        _currentMonth.value = (date.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        _currentYear.value = date.get(Calendar.YEAR)
        _viewMode.value = CalendarViewMode.MONTH
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == CalendarViewMode.MONTH) CalendarViewMode.YEAR else CalendarViewMode.MONTH
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

    fun clearFilters() {
        _selectedCategoryIds.value = emptySet()
    }

    fun next() {
        if (_viewMode.value == CalendarViewMode.MONTH) {
            val nextMonth = (_currentMonth.value.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
            }
            _currentMonth.value = nextMonth
            _currentYear.value = nextMonth.get(Calendar.YEAR)
        } else {
            _currentYear.value += 1
        }
    }

    fun previous() {
        if (_viewMode.value == CalendarViewMode.MONTH) {
            val prevMonth = (_currentMonth.value.clone() as Calendar).apply {
                add(Calendar.MONTH, -1)
            }
            _currentMonth.value = prevMonth
            _currentYear.value = prevMonth.get(Calendar.YEAR)
        } else {
            _currentYear.value -= 1
        }
    }

    fun deleteEvent(event: TimelineEvent) {
        viewModelScope.launch {
            repository.deleteEvent(event)
        }
    }
}
