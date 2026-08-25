package com.bcaste.lifetimeline.ui.screens.event

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.bcaste.lifetimeline.data.local.entity.EventType
import com.bcaste.lifetimeline.ui.components.*
import com.bcaste.lifetimeline.ui.utils.getIconByName
import com.bcaste.lifetimeline.ui.utils.toColor
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegisterEventScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: RegisterEventViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.startTimestamp)
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.endTimestamp ?: System.currentTimeMillis()
    )

    val dateFormat = remember { SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale.getDefault()) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Ignore if the provider does not support persistable permissions
                }
            }
            viewModel.addImages(uris.map { it.toString() })
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Evento") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Column: Main Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    EventTypeSelector(
                        selectedType = uiState.eventType,
                        onTypeSelected = viewModel::updateEventType
                    )

                    FormCard {
                        FormTextFieldRow(
                            label = "Título",
                            value = uiState.title,
                            onValueChange = viewModel::updateTitle,
                            placeholder = "Nombre del evento",
                            singleLine = true
                        )

                        val startLabel = if (uiState.eventType == EventType.PERIOD) "Fecha de inicio" else "Fecha"
                        FormRow(
                            label = startLabel,
                            value = dateFormat.format(Date(uiState.startTimestamp)),
                            onClick = { showStartDatePicker = true },
                            icon = Icons.Default.CalendarMonth
                        )

                        if (uiState.eventType == EventType.PERIOD) {
                            FormRow(
                                label = "Fecha de fin",
                                value = uiState.endTimestamp?.let { dateFormat.format(Date(it)) } ?: "Sin seleccionar",
                                onClick = { showEndDatePicker = true },
                                icon = Icons.Default.CalendarMonth,
                                isPlaceholder = uiState.endTimestamp == null
                            )
                        }

                        FormTextFieldRow(
                            label = "Descripción",
                            value = uiState.description,
                            onValueChange = viewModel::updateDescription,
                            placeholder = "Detalles adicionales...",
                            showDivider = false
                        )
                    }
                    
                    Button(
                        onClick = { viewModel.saveEvent(onSuccess = onNavigateBack) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = uiState.title.isNotBlank(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Guardar Evento", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Right Column: Categories and Images
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (availableCategories.isNotEmpty()) {
                        Text("Seleccionar Etiquetas", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            availableCategories.forEach { category ->
                                val isSelected = uiState.selectedCategoryIds.contains(category.id)
                                val categoryColor = category.color.toColor()
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.toggleCategory(category.id) },
                                    label = { Text(category.name) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = getIconByName(category.icon),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isSelected) Color.White else categoryColor
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = categoryColor,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = categoryColor,
                                        selectedBorderColor = categoryColor
                                    )
                                )
                            }
                        }
                    }

                    Text("Imágenes", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    
                    // Simple grid for images in landscape instead of LazyRow
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedCard(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.size(80.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = "Añadir imagen", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        
                        uiState.selectedImageUris.forEach { uri ->
                            Box(modifier = Modifier.size(80.dp)) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { viewModel.removeImage(uri) },
                                    modifier = Modifier.align(Alignment.TopEnd).size(20.dp)
                                ) {
                                    Surface(color = Color.Black.copy(alpha = 0.6f), shape = CircleShape) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        } else {
            // Portrait
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ── Tipo de evento ──────────────────────────────────────────
                EventTypeSelector(
                    selectedType = uiState.eventType,
                    onTypeSelected = viewModel::updateEventType
                )

                // ── Formulario Principal ────────────────────────────────────
                FormCard {
                    FormTextFieldRow(
                        label = "Título",
                        value = uiState.title,
                        onValueChange = viewModel::updateTitle,
                        placeholder = "Nombre del evento",
                        singleLine = true
                    )

                    val startLabel = if (uiState.eventType == EventType.PERIOD) "Fecha de inicio" else "Fecha"
                    FormRow(
                        label = startLabel,
                        value = dateFormat.format(Date(uiState.startTimestamp)),
                        onClick = { showStartDatePicker = true },
                        icon = Icons.Default.CalendarMonth
                    )

                    if (uiState.eventType == EventType.PERIOD) {
                        FormRow(
                            label = "Fecha de fin",
                            value = uiState.endTimestamp?.let { dateFormat.format(Date(it)) } ?: "Sin seleccionar",
                            onClick = { showEndDatePicker = true },
                            icon = Icons.Default.CalendarMonth,
                            isPlaceholder = uiState.endTimestamp == null
                        )
                    }

                    FormTextFieldRow(
                        label = "Descripción",
                        value = uiState.description,
                        onValueChange = viewModel::updateDescription,
                        placeholder = "Detalles adicionales...",
                        showDivider = false
                    )
                }

                // ── Categorías (Selector rápido para que sea funcional) ──────
                if (availableCategories.isNotEmpty()) {
                    Text("Seleccionar Etiquetas", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        availableCategories.forEach { category ->
                            val isSelected = uiState.selectedCategoryIds.contains(category.id)
                            val categoryColor = category.color.toColor()
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.toggleCategory(category.id) },
                                label = { Text(category.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getIconByName(category.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isSelected) Color.White else categoryColor
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = categoryColor,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = categoryColor,
                                    selectedBorderColor = categoryColor
                                )
                            )
                        }
                    }
                }

                // ── Imágenes ────────────────────────────────────────────────
                Text("Imágenes", style = MaterialTheme.typography.labelLarge, color = Color.White)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        OutlinedCard(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.size(100.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = "Añadir imagen", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    items(uiState.selectedImageUris.toList()) { uri ->
                        Box(modifier = Modifier.size(100.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.removeImage(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .padding(4.dp)
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar imagen",
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Botón guardar ───────────────────────────────────────────
                Button(
                    onClick = { viewModel.saveEvent(onSuccess = onNavigateBack) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = uiState.title.isNotBlank(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Guardar Evento", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ── Date Pickers ─────────────────────────────────────────────────
        if (showStartDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        startDatePickerState.selectedDateMillis?.let {
                            viewModel.updateStartTimestamp(it)
                        }
                        showStartDatePicker = false
                    }) { Text("Aceptar") }
                },
                dismissButton = {
                    TextButton(onClick = { showStartDatePicker = false }) { Text("Cancelar") }
                }
            ) {
                DatePicker(state = startDatePickerState)
            }
        }

        if (showEndDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showEndDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        endDatePickerState.selectedDateMillis?.let {
                            viewModel.updateEndTimestamp(it)
                        }
                        showEndDatePicker = false
                    }) { Text("Aceptar") }
                },
                dismissButton = {
                    TextButton(onClick = { showEndDatePicker = false }) { Text("Cancelar") }
                }
            ) {
                DatePicker(state = endDatePickerState)
            }
        }
    }
}
