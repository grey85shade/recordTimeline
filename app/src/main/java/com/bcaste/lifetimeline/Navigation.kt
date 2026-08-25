package com.bcaste.lifetimeline

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.bcaste.lifetimeline.ui.screens.calendar.CalendarScreen
import com.bcaste.lifetimeline.ui.screens.calendar.CalendarViewMode
import com.bcaste.lifetimeline.ui.screens.calendar.CalendarViewModel
import com.bcaste.lifetimeline.ui.screens.category.CategoryManagerScreen
import com.bcaste.lifetimeline.ui.screens.event.ModifyEventScreen
import com.bcaste.lifetimeline.ui.screens.event.RegisterEventScreen
import com.bcaste.lifetimeline.ui.screens.image.FullscreenImageViewerScreen
import com.bcaste.lifetimeline.ui.screens.lock.LockScreen
import com.bcaste.lifetimeline.ui.screens.lock.LockViewModel
import com.bcaste.lifetimeline.ui.screens.search.SearchFilterScreen
import com.bcaste.lifetimeline.ui.screens.settings.ProfileManagerScreen
import com.bcaste.lifetimeline.ui.screens.settings.SettingsScreen
import com.bcaste.lifetimeline.ui.screens.timeline.TimelineInfographicScreen
import com.bcaste.lifetimeline.ui.screens.timeline.TimelineInfographicViewModel
import com.bcaste.lifetimeline.ui.screens.timeline.TimelineVerticalScreen
import com.bcaste.lifetimeline.ui.screens.timeline.TimelineViewModel
import com.bcaste.lifetimeline.ui.utils.toColor

import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val lockViewModel: LockViewModel = hiltViewModel()
    val timelineViewModel: TimelineViewModel = hiltViewModel()
    val calendarViewModel: CalendarViewModel = hiltViewModel()
    var isUnlocked by rememberSaveable { mutableStateOf(!lockViewModel.isPasswordSet) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (!isUnlocked) {
        LockScreen(onUnlock = { isUnlocked = true })
        return
    }

    val backStack = rememberNavBackStack(TimelineVertical)
    val uiState by timelineViewModel.uiState.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    var isFilterActive by remember { mutableStateOf(false) }
    var isCalendarFilterActive by remember { mutableStateOf(false) }
    var showProfileSwitcher by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            val currentEntry = backStack.lastOrNull() ?: TimelineVertical
            val showTopBar = !isLandscape && (currentEntry == TimelineVertical || currentEntry == Calendar || currentEntry == TimelineInfographic)
            if (showTopBar) {
                val activeProfileName = uiState.profiles.find { it.id == uiState.activeProfileId }?.name ?: "Principal"
                TopAppBar(
                    title = { 
                        Column(modifier = Modifier.clickable { showProfileSwitcher = true }) {
                            Text(when (currentEntry) {
                                TimelineVertical -> "Eventos"
                                TimelineInfographic -> "Timeline"
                                Calendar -> "Calendario"
                                else -> ""
                            })
                            Text(
                                text = activeProfileName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    windowInsets = TopAppBarDefaults.windowInsets,
                    navigationIcon = {
                        IconButton(onClick = { showProfileSwitcher = true }) {
                            Image(
                                painter = painterResource(id = R.drawable.logo1),
                                contentDescription = "Logo",
                                modifier = Modifier.size(32.dp).clip(CircleShape)
                            )
                        }
                    },
                    actions = {
                        if (currentEntry == TimelineVertical) {
                            IconButton(onClick = { isSearchActive = !isSearchActive }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = if (isSearchActive) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }
                            IconButton(onClick = { isFilterActive = !isFilterActive }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filtro",
                                    tint = if (isFilterActive) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }
                        } else if (currentEntry == TimelineInfographic) {
                            val infographicViewModel: TimelineInfographicViewModel = hiltViewModel()
                            val infoState by infographicViewModel.uiState.collectAsState()
                            
                            IconButton(onClick = { infographicViewModel.togglePeriodsOnlyMode() }) {
                                Icon(
                                    imageVector = if (infoState.isPeriodsOnlyMode) Icons.Default.ViewStream else Icons.Default.ViewAgenda,
                                    contentDescription = "Vista periodos",
                                    tint = if (infoState.isPeriodsOnlyMode) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }
                            IconButton(onClick = { isFilterActive = !isFilterActive }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filtro",
                                    tint = if (isFilterActive) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }
                        } else if (currentEntry == Calendar) {
                            val calendarState by calendarViewModel.uiState.collectAsState()
                            IconButton(onClick = { calendarViewModel.toggleViewMode() }) {
                                Icon(
                                    imageVector = if (calendarState.viewMode == CalendarViewMode.MONTH) Icons.Default.GridView else Icons.Default.CalendarViewMonth,
                                    contentDescription = "Cambiar vista",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { isCalendarFilterActive = !isCalendarFilterActive }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filtro",
                                    tint = if (isCalendarFilterActive) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }
                        } else {
                            IconButton(onClick = { backStack.add(SearchFilter) }) {
                                Icon(Icons.Default.Search, contentDescription = "Buscar")
                            }
                            IconButton(onClick = { /* Filter */ }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filtro")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        bottomBar = {
            val currentEntry = backStack.lastOrNull() ?: TimelineVertical
            val isMainScreen = currentEntry == TimelineVertical || currentEntry == SearchFilter || currentEntry == Settings || currentEntry == TimelineInfographic || currentEntry == ProfileManager || currentEntry == Calendar
            if (isMainScreen && !isLandscape) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    actions = {
                        IconButton(
                            onClick = { backStack.add(TimelineInfographic) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = "Timeline",
                                tint = if (currentEntry == TimelineInfographic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { 
                                backStack.add(TimelineVertical)
                                isSearchActive = false
                                isFilterActive = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Dashboard,
                                contentDescription = "Eventos",
                                tint = if (currentEntry == TimelineVertical) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Botón de añadir integrado (Centro)
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            FloatingActionButton(
                                onClick = { backStack.add(RegisterEvent) },
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Añadir")
                            }
                        }

                        IconButton(
                            onClick = { backStack.add(Calendar) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Calendario",
                                tint = if (currentEntry == Calendar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { backStack.add(Settings) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.MoreHoriz,
                                contentDescription = "Más",
                                tint = if (currentEntry == Settings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize()) {
            val currentEntry = backStack.lastOrNull() ?: TimelineVertical
            val isMainScreen = currentEntry == TimelineVertical || currentEntry == SearchFilter || currentEntry == Settings || currentEntry == TimelineInfographic || currentEntry == ProfileManager || currentEntry == Calendar
            
            if (isLandscape && isMainScreen) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(64.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    NavigationRailItem(
                        selected = currentEntry == TimelineInfographic,
                        onClick = { backStack.add(TimelineInfographic) },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Timeline") },
                        label = { Text("Time", fontSize = 10.sp) }
                    )
                    
                    NavigationRailItem(
                        selected = currentEntry == TimelineVertical,
                        onClick = { 
                            backStack.add(TimelineVertical)
                            isSearchActive = false
                            isFilterActive = false
                        },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Eventos") },
                        label = { Text("Eventos", fontSize = 10.sp) }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    FloatingActionButton(
                        onClick = { backStack.add(RegisterEvent) },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    NavigationRailItem(
                        selected = currentEntry == Calendar,
                        onClick = { backStack.add(Calendar) },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendario") },
                        label = { Text("Cal", fontSize = 10.sp) }
                    )

                    NavigationRailItem(
                        selected = currentEntry == Settings,
                        onClick = { backStack.add(Settings) },
                        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "Más") },
                        label = { Text("Ajustes", fontSize = 10.sp) }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.padding(if (isLandscape) PaddingValues(0.dp) else paddingValues),
                    entryProvider = entryProvider {
                        entry<TimelineVertical> {
                            TimelineVerticalScreen(
                                isSearchVisible = isSearchActive,
                                isFilterVisible = isFilterActive,
                                onNavigateToRegister = { backStack.add(RegisterEvent) },
                                onNavigateToModify = { eventId -> backStack.add(ModifyEvent(eventId)) },
                                onNavigateToImagePager = { uris, index -> backStack.add(ImagePager(uris, index)) }
                            )
                        }
                        entry<TimelineInfographic> {
                            TimelineInfographicScreen(isFilterVisible = isFilterActive)
                        }
                        entry<Calendar> {
                            CalendarScreen(
                                isFilterVisible = isCalendarFilterActive,
                                onNavigateToModify = { eventId -> backStack.add(ModifyEvent(eventId)) },
                                onNavigateToImagePager = { uris, index -> backStack.add(ImagePager(uris, index)) },
                                viewModel = calendarViewModel
                            )
                        }
                        entry<RegisterEvent> {
                            RegisterEventScreen(onNavigateBack = { backStack.removeLastOrNull() })
                        }
                        entry<ModifyEvent> { modifyEvent ->
                            ModifyEventScreen(
                                eventId = modifyEvent.eventId,
                                onNavigateBack = { backStack.removeLastOrNull() }
                            )
                        }
                        entry<CategoryManager> {
                            CategoryManagerScreen(onNavigateBack = { backStack.removeLastOrNull() })
                        }
                        entry<ProfileManager> {
                            ProfileManagerScreen(onNavigateBack = { backStack.removeLastOrNull() })
                        }
                        entry<SearchFilter> {
                            SearchFilterScreen()
                        }
                        entry<Settings> {
                            SettingsScreen(
                                onNavigateToCategories = { backStack.add(CategoryManager) },
                                onNavigateToProfiles = { backStack.add(ProfileManager) }
                            )
                        }
                        entry<ImagePager> { imagePager ->
                            FullscreenImageViewerScreen(
                                imageUris = imagePager.imageUris,
                                initialIndex = imagePager.initialIndex,
                                onClose = { backStack.removeLastOrNull() }
                            )
                        }
                    }
                )
            }
        }

        if (showProfileSwitcher) {
            ProfileSwitcherDialog(
                profiles = uiState.profiles,
                activeProfileId = uiState.activeProfileId,
                onProfileSelected = {
                    timelineViewModel.setActiveProfile(it)
                    showProfileSwitcher = false
                },
                onDismiss = { showProfileSwitcher = false }
            )
        }
    }
}

@Composable
fun ProfileSwitcherDialog(
    profiles: List<com.bcaste.lifetimeline.data.local.entity.Profile>,
    activeProfileId: String,
    onProfileSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Perfil") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                profiles.forEach { profile ->
                    val isSelected = profile.id == activeProfileId
                    ListItem(
                        headlineContent = { Text(profile.name) },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(profile.color.toColor())
                            )
                        },
                        trailingContent = {
                            if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.clickable { onProfileSelected(profile.id) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
