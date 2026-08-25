package com.bcaste.lifetimeline.ui.screens.timeline

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bcaste.lifetimeline.ui.utils.getIconByName
import com.bcaste.lifetimeline.ui.utils.toColor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineInfographicScreen(
    isFilterVisible: Boolean = false,
    viewModel: TimelineInfographicViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val dayFormatter = remember { SimpleDateFormat("d", Locale.getDefault()) }
    val weekDayFormatter = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AnimatedVisibility(visible = isFilterVisible) {
            InfographicFilterSection(
                availableCategories = state.availableCategories,
                selectedIds = state.selectedCategoryIds,
                onToggleCategory = viewModel::toggleCategorySelection,
                onClear = viewModel::selectAllCategories
            )
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.items.isEmpty() && !state.isPeriodsOnlyMode) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay eventos registrados", color = Color.Gray)
            }
        } else {
            val baseRowHeight = if (state.isPeriodsOnlyMode) 28.dp else 48.dp
            val rowHeight = baseRowHeight * zoomScale

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(0.1f, 10f)
                        }
                    },
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                itemsIndexed(state.items) { index, item ->
                    TimelineRow(
                        item = item,
                        index = index,
                        state = state,
                        rowHeight = rowHeight,
                        dayFormatter = dayFormatter,
                        weekDayFormatter = weekDayFormatter
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineRow(
    item: TimelineItem,
    index: Int,
    state: InfographicUiState,
    rowHeight: androidx.compose.ui.unit.Dp,
    dayFormatter: SimpleDateFormat,
    weekDayFormatter: SimpleDateFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = if (item is TimelineItem.MonthHeader) 40.dp else rowHeight)
    ) {
        // 1. Sidebar for Period Bars
        val sidebarWidth = 80.dp // Increased to avoid overlaps
        Box(
            modifier = Modifier
                .width(sidebarWidth)
                .fillMaxHeight()
        ) {
            val now = state.currentTime
            val density = LocalDensity.current
            val textPaint = remember(density) {
                android.graphics.Paint().apply {
                    textSize = with(density) { 9.sp.toPx() }
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
            }

            state.periodBars.forEach { bar ->
                val start = bar.event.event.timestamp
                val end = bar.event.event.endTimestamp ?: now
                val isActive = isItemInPeriod(item, start, end)

                if (isActive) {
                    // Check if this is the start of the bar IN THE LIST
                    val isTopInList = index == 0 || !isItemInPeriod(state.items[index - 1], start, end)
                    val isBottomInList = index == state.items.size - 1 || !isItemInPeriod(state.items[index + 1], start, end)

                    // Repeat label logic: Top or every 12 rows
                    val shouldShowLabel = isTopInList || (index % 12 == 0 && item is TimelineItem.DayRow)

                    PeriodBarSegment(
                        bar = bar,
                        item = item,
                        isTopInList = isTopInList,
                        isBottomInList = isBottomInList,
                        shouldShowLabel = shouldShowLabel,
                        textPaint = textPaint,
                        now = now
                    )
                }
            }
        }

        // 2. Timeline central line
        Box(
            modifier = Modifier
                .width(30.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (item is TimelineItem.DayRow) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)
                    drawLine(
                        color = Color.DarkGray.copy(alpha = 0.3f),
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, size.height),
                        strokeWidth = 1.5f,
                        pathEffect = pathEffect
                    )
                }
            }

            if (item is TimelineItem.DayRow && item.pointEvents.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .size(10.dp)
                        .border(1.2.dp, Color.Gray.copy(alpha = 0.8f), CircleShape)
                        .background(Color.Black, CircleShape)
                )
            }
        }

        // 3. Content
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 0.dp)
        ) {
            when (item) {
                is TimelineItem.MonthHeader -> {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = Color.Gray.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                is TimelineItem.DayRow -> {
                    if (item.pointEvents.isEmpty()) {
                        Spacer(modifier = Modifier.fillMaxWidth().height(rowHeight))
                    } else {
                        Column {
                            item.pointEvents.forEachIndexed { eventIndex, eventWithDetails ->
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (eventIndex == 0) {
                                            DayBlock(item.timestamp, dayFormatter, weekDayFormatter, Color.White)
                                        } else {
                                            // Keep space for alignment
                                            Box(modifier = Modifier.width(44.dp))
                                        }
                                        
                                        Spacer(modifier = Modifier.width(16.dp))

                                        val category = eventWithDetails.categories.firstOrNull()
                                        val color = category?.color?.toColor() ?: Color.White
                                        
                                        Icon(
                                            imageVector = getIconByName(category?.icon ?: "event"),
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(24.dp)
                                        )

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Text(
                                            text = eventWithDetails.event.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = Color.White, 
                                                fontWeight = FontWeight.SemiBold, 
                                                fontSize = 16.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = Color.DarkGray.copy(alpha = 0.15f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayBlock(
    timestamp: Long, 
    dayFormatter: SimpleDateFormat, 
    weekDayFormatter: SimpleDateFormat, 
    textColor: Color
) {
    val date = Date(timestamp)
    Column(
        modifier = Modifier.width(44.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = dayFormatter.format(date),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold, 
                color = textColor, 
                fontSize = 20.sp,
                letterSpacing = (-0.5).sp
            )
        )
        Text(
            text = weekDayFormatter.format(date).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.Gray, 
                fontSize = 9.sp, 
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun PeriodBarSegment(
    bar: PeriodBarInfo,
    item: TimelineItem,
    isTopInList: Boolean,
    isBottomInList: Boolean,
    shouldShowLabel: Boolean,
    textPaint: android.graphics.Paint,
    now: Long
) {
    val barColor = bar.color.toColor()
    // More spacing between columns: 16dp base + 14dp per column
    val columnOffset = 14.dp + (bar.column * 14).dp

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(start = columnOffset)
            .width(3.5.dp)
            .background(
                color = barColor,
                shape = RoundedCornerShape(
                    topStart = if (isTopInList) 4.dp else 0.dp,
                    topEnd = if (isTopInList) 4.dp else 0.dp,
                    bottomStart = if (isBottomInList) 4.dp else 0.dp,
                    bottomEnd = if (isBottomInList) 4.dp else 0.dp
                )
            )
    )

    if (shouldShowLabel) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val paint = android.graphics.Paint(textPaint).apply {
                color = android.graphics.Color.argb(
                    (barColor.alpha * 255).toInt(),
                    (barColor.red * 255).toInt(),
                    (barColor.green * 255).toInt(),
                    (barColor.blue * 255).toInt()
                )
            }
            
            drawContext.canvas.nativeCanvas.save()
            // Anchor point for rotation: slightly to the left of the bar
            val x = columnOffset.toPx() - 4.dp.toPx()
            // If it's a MonthHeader, the label starts a bit lower to look centered in the gap
            val y = if (item is TimelineItem.MonthHeader) 50.dp.toPx() else 40.dp.toPx()
            
            drawContext.canvas.nativeCanvas.rotate(-90f, x, y)
            drawContext.canvas.nativeCanvas.drawText(
                bar.event.event.title,
                x,
                y,
                paint
            )
            drawContext.canvas.nativeCanvas.restore()
        }
    }
}

private fun isItemInPeriod(item: TimelineItem, periodStart: Long, periodEnd: Long): Boolean {
    val calendar = Calendar.getInstance()
    
    val pStartCal = Calendar.getInstance().apply { 
        timeInMillis = periodStart
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val pEndCal = Calendar.getInstance().apply { 
        timeInMillis = periodEnd
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
    }

    return when(item) {
        is TimelineItem.MonthHeader -> {
            calendar.timeInMillis = item.timestamp
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val monthEnd = calendar.timeInMillis
            monthStart <= pEndCal.timeInMillis && monthEnd >= pStartCal.timeInMillis
        }
        is TimelineItem.DayRow -> {
            calendar.timeInMillis = item.timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 12)
            calendar.timeInMillis in pStartCal.timeInMillis..pEndCal.timeInMillis
        }
    }
}

@Composable
fun InfographicFilterSection(
    availableCategories: List<com.bcaste.lifetimeline.data.local.entity.Category>,
    selectedIds: Set<String>?,
    onToggleCategory: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp).background(Color.Black)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableCategories.forEach { category ->
                val isSelected = selectedIds?.contains(category.id) == true
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleCategory(category.id) },
                    label = { Text(category.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = Color.Gray,
                        selectedLabelColor = Color.White,
                        selectedContainerColor = category.color.toColor().copy(alpha = 0.3f)
                    )
                )
            }
        }
        if (selectedIds != null && selectedIds.isNotEmpty()) {
            TextButton(onClick = onClear) {
                Text("Limpiar filtros", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
