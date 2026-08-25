package com.bcaste.lifetimeline.ui.screens.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcaste.lifetimeline.data.ProfileManager
import com.bcaste.lifetimeline.data.TimelineRepository
import com.bcaste.lifetimeline.data.local.entity.Category
import com.bcaste.lifetimeline.data.local.entity.EventWithDetails
import com.bcaste.lifetimeline.data.local.entity.EventType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class InfographicUiState(
    val items: List<TimelineItem> = emptyList(),
    val periodBars: List<PeriodBarInfo> = emptyList(),
    val availableCategories: List<Category> = emptyList(),
    val selectedCategoryIds: Set<String>? = null,
    val isPeriodsOnlyMode: Boolean = false,
    val isLoading: Boolean = true,
    val currentTime: Long = System.currentTimeMillis()
)

sealed class TimelineItem {
    abstract val timestamp: Long
    data class MonthHeader(val label: String, override val timestamp: Long) : TimelineItem()
    data class DayRow(
        override val timestamp: Long,
        val pointEvents: List<EventWithDetails> = emptyList()
    ) : TimelineItem()
}

data class PeriodBarInfo(
    val event: EventWithDetails,
    val color: String,
    val column: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TimelineInfographicViewModel @Inject constructor(
    private val repository: TimelineRepository,
    private val profileManager: ProfileManager
) : ViewModel() {

    private val selectedCategoryIds = MutableStateFlow<Set<String>?>(null)
    private val isPeriodsOnlyMode = MutableStateFlow(false)
    private val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    val uiState: StateFlow<InfographicUiState> = combine(
        profileManager.activeProfileId.flatMapLatest { repository.getCategoriesForProfile(it) },
        profileManager.activeProfileId.flatMapLatest { repository.getEventsByProfile(it) },
        selectedCategoryIds,
        isPeriodsOnlyMode
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val categories = flows[0] as List<Category>
        @Suppress("UNCHECKED_CAST")
        val allEvents = flows[1] as List<EventWithDetails>
        val selIds = flows[2] as Set<String>?
        val periodsOnly = flows[3] as Boolean

        val filteredEvents = allEvents.filter { event ->
            selIds.isNullOrEmpty() || 
                event.categories.any { it.id in selIds } || 
                (event.categories.isEmpty() && selIds.contains("uncategorized"))
        }

        if (filteredEvents.isEmpty()) {
            return@combine InfographicUiState(
                availableCategories = categories,
                selectedCategoryIds = selIds,
                isPeriodsOnlyMode = periodsOnly,
                isLoading = false
            )
        }

        val pointEvents = filteredEvents.filter { it.event.eventType == EventType.POINT }
        val periodEvents = filteredEvents.filter { it.event.eventType == EventType.PERIOD }

        val now = System.currentTimeMillis()

        // 1. Generate items
        val items = mutableListOf<TimelineItem>()
        
        val minTime = filteredEvents.minOf { it.event.timestamp }
        val maxTime = filteredEvents.maxOf { 
            if (it.event.eventType == EventType.PERIOD) it.event.endTimestamp ?: now else it.event.timestamp 
        }

        val iterCal = Calendar.getInstance()
        iterCal.timeInMillis = maxTime
        iterCal.set(Calendar.HOUR_OF_DAY, 23); iterCal.set(Calendar.MINUTE, 59)
        
        val startBound = Calendar.getInstance()
        startBound.timeInMillis = minTime
        startBound.set(Calendar.DAY_OF_MONTH, 1); startBound.set(Calendar.HOUR_OF_DAY, 0)

        if (periodsOnly) {
            // In Periods Only mode, we just want headers for years/months that have periods
            // or maybe just a very compact view.
            // Let's generate headers and only rows for point events if they exist, 
            // but the user said "without seeing all days". 
            // So we'll only generate MonthHeaders for now to provide vertical space.
            while (iterCal.timeInMillis >= startBound.timeInMillis) {
                items.add(TimelineItem.MonthHeader(monthFormatter.format(iterCal.time).uppercase(), iterCal.timeInMillis))
                iterCal.add(Calendar.MONTH, -1)
            }
        } else {
            var lastMonth = -1
            var lastYear = -1
            while (iterCal.timeInMillis >= startBound.timeInMillis) {
                val currentMonth = iterCal.get(Calendar.MONTH)
                val currentYear = iterCal.get(Calendar.YEAR)

                if (currentMonth != lastMonth || currentYear != lastYear) {
                    val monthStartCal = iterCal.clone() as Calendar
                    monthStartCal.set(Calendar.DAY_OF_MONTH, 1)
                    items.add(TimelineItem.MonthHeader(monthFormatter.format(iterCal.time).uppercase(), monthStartCal.timeInMillis))
                    lastMonth = currentMonth
                    lastYear = currentYear
                }

                val dayStart = truncateToDay(iterCal.timeInMillis)
                val dayEnd = dayStart + (24 * 60 * 60 * 1000) - 1
                
                val eventsOnDay = pointEvents.filter { it.event.timestamp in dayStart..dayEnd }
                val hasActivePeriod = periodEvents.any { p ->
                    val pStart = truncateToDay(p.event.timestamp)
                    val pEnd = truncateToDay(p.event.endTimestamp ?: now)
                    dayStart in pStart..pEnd
                }

                if (eventsOnDay.isNotEmpty() || hasActivePeriod) {
                    items.add(TimelineItem.DayRow(dayStart, eventsOnDay))
                }
                
                iterCal.add(Calendar.DAY_OF_YEAR, -1)
            }
        }

        val periodBars = assignColumnsByRows(periodEvents, items, now)

        InfographicUiState(
            items = items,
            periodBars = periodBars,
            availableCategories = categories,
            selectedCategoryIds = selIds,
            isPeriodsOnlyMode = periodsOnly,
            isLoading = false,
            currentTime = now
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InfographicUiState()
    )

    private fun truncateToDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun assignColumnsByRows(
        periods: List<EventWithDetails>,
        items: List<TimelineItem>,
        now: Long
    ): List<PeriodBarInfo> {
        val result = mutableListOf<PeriodBarInfo>()
        val sortedPeriods = periods.sortedByDescending { 
            (it.event.endTimestamp ?: now) - it.event.timestamp 
        }

        val periodRowIndices = sortedPeriods.map { period ->
            val start = period.event.timestamp
            val end = period.event.endTimestamp ?: now
            val indices = items.mapIndexedNotNull { index, item ->
                if (isItemInPeriod(item, start, end)) index else null
            }.toSet()
            period to indices
        }

        periodRowIndices.forEach { (period, indices) ->
            var col = 0
            while (true) {
                val hasRowOverlap = result.filter { it.column == col }.any { other ->
                    val otherIndices = periodRowIndices.find { it.first.event.id == other.event.event.id }?.second ?: emptySet()
                    indices.intersect(otherIndices).isNotEmpty()
                }
                
                if (!hasRowOverlap) {
                    result.add(PeriodBarInfo(
                        event = period,
                        color = period.categories.firstOrNull()?.color ?: "#3D82F5",
                        column = col
                    ))
                    break
                }
                col++
            }
        }
        return result
    }

    private fun isItemInPeriod(item: TimelineItem, periodStart: Long, periodEnd: Long): Boolean {
        val pStartDay = truncateToDay(periodStart)
        val pEndDay = truncateToDay(periodEnd)

        return when(item) {
            is TimelineItem.MonthHeader -> {
                val monthStart = truncateToDay(item.timestamp)
                val calendar = Calendar.getInstance().apply { 
                    timeInMillis = monthStart
                    add(Calendar.MONTH, 1)
                    add(Calendar.MILLISECOND, -1)
                }
                val monthEnd = calendar.timeInMillis
                monthStart <= periodEnd && monthEnd >= periodStart
            }
            is TimelineItem.DayRow -> {
                val itemDay = truncateToDay(item.timestamp)
                itemDay in pStartDay..pEndDay
            }
        }
    }

    fun toggleCategorySelection(categoryId: String) {
        val current = selectedCategoryIds.value?.toMutableSet() ?: mutableSetOf()
        if (current.contains(categoryId)) {
            current.remove(categoryId)
        } else {
            current.add(categoryId)
        }
        selectedCategoryIds.value = current
    }

    fun selectAllCategories() {
        selectedCategoryIds.value = null
    }

    fun togglePeriodsOnlyMode() {
        isPeriodsOnlyMode.value = !isPeriodsOnlyMode.value
    }
}
