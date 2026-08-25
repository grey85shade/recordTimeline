package com.bcaste.lifetimeline.ui.screens.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bcaste.lifetimeline.data.local.entity.EventWithDetails
import com.bcaste.lifetimeline.data.local.entity.EventType
import com.bcaste.lifetimeline.ui.components.TimelineEventCard
import com.bcaste.lifetimeline.ui.utils.toColor
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

@Composable
fun CalendarScreen(
    isFilterVisible: Boolean = false,
    onNavigateToModify: (String) -> Unit,
    onNavigateToImagePager: (List<String>, Int) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val monthYearFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayFormatter = remember { SimpleDateFormat("d", Locale.getDefault()) }
    val monthAbbrFormatter = remember { SimpleDateFormat("MMM", Locale.getDefault()) }

    val selectedDayEvents = remember(state.selectedDate, state.events) {
        state.events.filter { isEventOnDay(it, state.selectedDate) }
    }

    var expandedEventId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Filter Section
        AnimatedVisibility(
            visible = isFilterVisible,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            CalendarFilterSection(
                availableCategories = state.availableCategories,
                selectedCategoryIds = state.selectedCategoryIds,
                onToggleCategory = viewModel::toggleCategory,
                onClear = viewModel::clearFilters
            )
        }

        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Side: Calendar
                Column(modifier = Modifier.weight(1.2f)) {
                    CalendarHeader(state, monthYearFormatter, viewModel)
                    
                    if (state.viewMode == CalendarViewMode.MONTH) {
                        MonthView(state = state, onDateSelected = { viewModel.selectDate(it) })
                    } else {
                        YearView(
                            state = state,
                            onMonthSelected = { month ->
                                viewModel.selectDate((state.currentMonth.clone() as Calendar).apply {
                                    set(Calendar.YEAR, state.currentYear)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, 1)
                                })
                                viewModel.toggleViewMode()
                            }
                        )
                    }
                }

                // Right Side: Events
                Column(modifier = Modifier.weight(1f)) {
                    EventListSection(
                        events = selectedDayEvents,
                        expandedEventId = expandedEventId,
                        onToggleExpand = { id -> expandedEventId = if (expandedEventId == id) null else id },
                        onEdit = onNavigateToModify,
                        onDelete = { viewModel.deleteEvent(it) },
                        onImageClick = onNavigateToImagePager,
                        dayFormatter = dayFormatter,
                        monthAbbrFormatter = monthAbbrFormatter
                    )
                }
            }
        } else {
            // Portrait Layout
            CalendarHeader(state, monthYearFormatter, viewModel)

            if (state.viewMode == CalendarViewMode.MONTH) {
                MonthView(state = state, onDateSelected = { viewModel.selectDate(it) })
            } else {
                YearView(
                    state = state,
                    onMonthSelected = { month ->
                        viewModel.selectDate((state.currentMonth.clone() as Calendar).apply {
                            set(Calendar.YEAR, state.currentYear)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, 1)
                        })
                        viewModel.toggleViewMode()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f)) {
                EventListSection(
                    events = selectedDayEvents,
                    expandedEventId = expandedEventId,
                    onToggleExpand = { id -> expandedEventId = if (expandedEventId == id) null else id },
                    onEdit = onNavigateToModify,
                    onDelete = { viewModel.deleteEvent(it) },
                    onImageClick = onNavigateToImagePager,
                    dayFormatter = dayFormatter,
                    monthAbbrFormatter = monthAbbrFormatter
                )
            }
        }
    }
}

@Composable
fun CalendarHeader(
    state: CalendarUiState,
    monthYearFormatter: SimpleDateFormat,
    viewModel: CalendarViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { viewModel.previous() }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Anterior", tint = Color.White)
        }
        
        Text(
            text = if (state.viewMode == CalendarViewMode.MONTH) {
                monthYearFormatter.format(state.currentMonth.time).replaceFirstChar { it.uppercase() }
            } else {
                state.currentYear.toString()
            },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )
        
        IconButton(onClick = { viewModel.next() }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Siguiente", tint = Color.White)
        }
    }
}

@Composable
fun EventListSection(
    events: List<EventWithDetails>,
    expandedEventId: String?,
    onToggleExpand: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (com.bcaste.lifetimeline.data.local.entity.TimelineEvent) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    dayFormatter: SimpleDateFormat,
    monthAbbrFormatter: SimpleDateFormat
) {
    if (events.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No hay eventos para este día",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(events) { eventWithDetails ->
                val isExpanded = expandedEventId == eventWithDetails.event.id
                TimelineEventCard(
                    eventWithDetails = eventWithDetails,
                    isExpanded = isExpanded,
                    onToggleExpand = { onToggleExpand(eventWithDetails.event.id) },
                    onEdit = { onEdit(eventWithDetails.event.id) },
                    onDelete = { onDelete(eventWithDetails.event) },
                    onImageClick = { index ->
                        onImageClick(eventWithDetails.images.map { it.imageUri }, index)
                    },
                    dayText = dayFormatter.format(Date(eventWithDetails.event.timestamp)),
                    monthText = monthAbbrFormatter.format(Date(eventWithDetails.event.timestamp)).uppercase()
                )
            }
        }
    }
}

@Composable
fun CalendarFilterSection(
    availableCategories: List<com.bcaste.lifetimeline.data.local.entity.Category>,
    selectedCategoryIds: Set<String>,
    onToggleCategory: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableCategories) { category ->
                val isSelected = selectedCategoryIds.contains(category.id)
                val color = category.color.toColor()
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleCategory(category.id) },
                    label = { Text(category.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = 0.2f),
                        selectedLabelColor = color
                    )
                )
            }
        }

        if (selectedCategoryIds.isNotEmpty()) {
            TextButton(
                onClick = onClear,
                modifier = Modifier.padding(horizontal = 16.dp).height(32.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Limpiar filtros", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun MonthView(
    state: CalendarUiState,
    onDateSelected: (Calendar) -> Unit
) {
    CalendarGrid(
        currentMonth = state.currentMonth,
        selectedDate = state.selectedDate,
        events = state.events,
        onDateSelected = onDateSelected
    )
}

@Composable
fun YearView(
    state: CalendarUiState,
    onMonthSelected: (Int) -> Unit
) {
    val months = remember { (0..11).toList() }
    val monthNameFormatter = remember { SimpleDateFormat("MMMM", Locale.getDefault()) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().height(350.dp).padding(horizontal = 8.dp),
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(months) { monthIndex ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, state.currentYear)
                set(Calendar.MONTH, monthIndex)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            
            val hasEvents = state.events.any { isEventInMonth(it, state.currentYear, monthIndex) }

            Card(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onMonthSelected(monthIndex) },
                colors = CardDefaults.cardColors(
                    containerColor = if (state.currentMonth.get(Calendar.MONTH) == monthIndex) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                        else Color(0xFF0F171F)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = monthNameFormatter.format(cal.time).take(3).uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (hasEvents) MaterialTheme.colorScheme.primary else Color.White
                            )
                        )
                        if (hasEvents) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: Calendar,
    selectedDate: Calendar,
    events: List<EventWithDetails>,
    onDateSelected: (Calendar) -> Unit
) {
    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (currentMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK)
    
    val offset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

    val days = (1..daysInMonth).toList()
    val totalCells = ((days.size + offset + 6) / 7) * 7

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        // Week days header
        Row(modifier = Modifier.fillMaxWidth()) {
            val weekDays = listOf("L", "M", "X", "J", "V", "S", "D")
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))

        for (row in 0 until totalCells / 7) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayValue = cellIndex - offset + 1
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(if (androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 1.5f else 1f)
                            .padding(1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayValue in 1..daysInMonth) {
                            val date = (currentMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, dayValue) }
                            val isSelected = date.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                                           date.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
                            val isToday = Calendar.getInstance().let { today ->
                                date.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                date.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                            }
                            
                            val dayEvents = events.filter { isEventOnDay(it, date) }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable { onDateSelected(date) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayValue.toString(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else if (isToday) MaterialTheme.colorScheme.secondary else Color.White,
                                        fontSize = if (androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 12.sp else 14.sp
                                    )
                                )
                                
                                Row(
                                    modifier = Modifier.height(8.dp).padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    dayEvents.take(3).forEach { e ->
                                        val color = e.categories.firstOrNull()?.color?.toColor() ?: MaterialTheme.colorScheme.primary
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isEventOnDay(event: EventWithDetails, day: Calendar): Boolean {
    val startOfDay = (day.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    
    val endOfDay = (day.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    val e = event.event
    val eventStart = e.timestamp
    val eventEnd = if (e.eventType == EventType.PERIOD) {
        e.endTimestamp ?: System.currentTimeMillis()
    } else {
        e.timestamp
    }

    return eventStart <= endOfDay && eventEnd >= startOfDay
}

private fun isEventInMonth(event: EventWithDetails, year: Int, month: Int): Boolean {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, month)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfMonth = calendar.timeInMillis

    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    val endOfMonth = calendar.timeInMillis

    val e = event.event
    val eventStart = e.timestamp
    val eventEnd = if (e.eventType == EventType.PERIOD) {
        e.endTimestamp ?: System.currentTimeMillis()
    } else {
        e.timestamp
    }

    return eventStart <= endOfMonth && eventEnd >= startOfMonth
}
