package com.bcaste.lifetimeline.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bcaste.lifetimeline.data.local.entity.Profile
import com.bcaste.lifetimeline.ui.utils.CATEGORY_COLORS
import com.bcaste.lifetimeline.ui.utils.toColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagerScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileManagerViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Perfiles") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Perfil")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(profiles) { profile ->
                ProfileItem(
                    profile = profile,
                    onDelete = { viewModel.deleteProfile(profile) },
                    onUpdate = { viewModel.updateProfile(it) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddProfileDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, isVisible, color ->
                viewModel.addProfile(name, isVisible, color)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ProfileItem(
    profile: Profile,
    onDelete: () -> Unit,
    onUpdate: (Profile) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showEditDialog = true },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F171F))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(profile.color.toColor())
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = profile.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(
                    text = if (profile.isVisibleInMain) "Visible en Principal" else "Perfil separado",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            if (profile.id != Profile.MAIN_ID) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showEditDialog) {
        AddProfileDialog(
            initialProfile = profile,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, isVisible, color ->
                onUpdate(profile.copy(name = name, isVisibleInMain = isVisible, color = color))
                showEditDialog = false
            }
        )
    }
}

@Composable
fun AddProfileDialog(
    initialProfile: Profile? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, String) -> Unit
) {
    var name by remember { mutableStateOf(initialProfile?.name ?: "") }
    var isVisibleInMain by remember { mutableStateOf(initialProfile?.isVisibleInMain ?: false) }
    var selectedColor by remember { mutableStateOf(initialProfile?.color ?: CATEGORY_COLORS[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialProfile == null) "Nuevo Perfil" else "Editar Perfil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del perfil") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isVisibleInMain,
                        onCheckedChange = { isVisibleInMain = it }
                    )
                    Text("Mostrar eventos en Perfil Principal")
                }

                Text("Color", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CATEGORY_COLORS) { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color.toColor())
                                .clickable { selectedColor = color }
                                .padding(4.dp)
                        ) {
                            if (selectedColor == color) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, isVisibleInMain, selectedColor) },
                enabled = name.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
