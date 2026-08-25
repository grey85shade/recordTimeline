package com.bcaste.lifetimeline.ui.screens.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.bcaste.lifetimeline.data.local.entity.Category
import com.bcaste.lifetimeline.data.local.entity.EventWithDetails
import com.bcaste.lifetimeline.data.local.entity.TimelineEvent
import com.bcaste.lifetimeline.ui.components.TimelineEventCard
import com.bcaste.lifetimeline.ui.utils.getIconByName
import com.bcaste.lifetimeline.ui.utils.toColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimelineVerticalScreen(
    isSearchVisible: Boolean = false,
    isFilterVisible: Boolean = false,
    onNavigateToRegister: () -> Unit,
    onNavigateToModify: (String) -> Unit,
    onNavigateToImagePager: (List<String>, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val monthYearFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayFormatter = remember { SimpleDateFormat("d", Locale.getDefault()) }
    val monthAbbrFormatter = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val groupedEvents = remember(state.events) {
        state.events.groupBy { eventWithDetails ->
            monthYearFormatter.format(Date(eventWithDetails.event.timestamp))
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }

    var expandedEventId by remember { mutableStateOf<String?>(null) }
    var eventToDelete by remember { mutableStateOf<TimelineEvent?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search and Filter Bar - Controlled externally
        AnimatedVisibility(
            visible = isSearchVisible,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            SearchRow(
                query = state.searchQuery,
                onQueryChange = viewModel::updateSearchQuery
            )
        }

        AnimatedVisibility(
            visible = isFilterVisible,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            FilterSection(
                state = state,
                onToggleCategory = viewModel::toggleCategory,
                onSelectDate = { showDatePicker = true },
                onToggleYear = viewModel::toggleCurrentYearFilter,
                onToggleMonth = viewModel::toggleCurrentMonthFilter,
                onClear = viewModel::clearFilters,
                dateFormatter = dateFormatter
            )
        }

        if (state.events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (state.searchQuery.isNotEmpty() || isFilterVisible) "No se encontraron eventos" else "No hay eventos registrados",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = if (isLandscape) GridCells.Fixed(2) else GridCells.Fixed(1),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp, start = 8.dp, end = 8.dp, top = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedEvents.forEach { (monthYear, monthEvents) ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = monthYear,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    items(monthEvents) { eventWithDetails ->
                        val isExpanded = expandedEventId == eventWithDetails.event.id
                        TimelineEventCard(
                            eventWithDetails = eventWithDetails,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedEventId = if (isExpanded) null else eventWithDetails.event.id
                            },
                            onEdit = { onNavigateToModify(eventWithDetails.event.id) },
                            onDelete = { eventToDelete = eventWithDetails.event },
                            onImageClick = { index ->
                                onNavigateToImagePager(eventWithDetails.images.map { it.imageUri }, index)
                            },
                            dayText = dayFormatter.format(Date(eventWithDetails.event.timestamp)),
                            monthText = monthAbbrFormatter.format(Date(eventWithDetails.event.timestamp)).uppercase()
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateSelectedDate(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (eventToDelete != null) {
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("Eliminar evento") },
            text = { Text("¿Estás seguro de que deseas eliminar este evento?") },
            confirmButton = {
                TextButton(onClick = {
                    eventToDelete?.let { viewModel.deleteEvent(it) }
                    eventToDelete = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun SearchRow(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Buscar eventos...") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F171F),
                unfocusedContainerColor = Color(0xFF0F171F),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )
    }
}

@Composable
fun FilterSection(
    state: TimelineUiState,
    onToggleCategory: (String) -> Unit,
    onSelectDate: () -> Unit,
    onToggleYear: () -> Unit,
    onToggleMonth: () -> Unit,
    onClear: () -> Unit,
    dateFormatter: SimpleDateFormat
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
            item {
                FilterChip(
                    selected = state.isCurrentYearFilterActive,
                    onClick = onToggleYear,
                    label = { Text("Año actual") },
                    leadingIcon = if (state.isCurrentYearFilterActive) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
            item {
                FilterChip(
                    selected = state.isCurrentMonthFilterActive,
                    onClick = onToggleMonth,
                    label = { Text("Mes actual") },
                    leadingIcon = if (state.isCurrentMonthFilterActive) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
            item {
                FilterChip(
                    selected = state.selectedDate != null,
                    onClick = onSelectDate,
                    label = {
                        Text(
                            state.selectedDate?.let { dateFormatter.format(Date(it)) } ?: "Fecha específica"
                        )
                    },
                    trailingIcon = if (state.selectedDate != null) {
                        {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onSelectDate() } // This is a bit recursive, better handle separately
                            )
                        }
                    } else null
                )
            }
            
            items(state.categories) { category ->
                val isSelected = state.selectedCategoryIds.contains(category.id)
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

        TextButton(
            onClick = onClear,
            modifier = Modifier.padding(horizontal = 16.dp).height(32.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Limpiar todos los filtros", style = MaterialTheme.typography.labelSmall)
        }
    }
}

