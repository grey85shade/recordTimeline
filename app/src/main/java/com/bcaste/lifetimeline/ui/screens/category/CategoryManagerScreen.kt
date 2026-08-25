package com.bcaste.lifetimeline.ui.screens.category

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
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bcaste.lifetimeline.data.local.entity.Category
import com.bcaste.lifetimeline.ui.utils.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    var newCategoryName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(CATEGORY_COLORS[0]) }
    var selectedIconName by remember { mutableStateOf(CATEGORY_ICONS[0].first) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Etiquetas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it },
                label = { Text("Nombre de la etiqueta") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Color", style = MaterialTheme.typography.labelLarge)
            CategoryColorPicker(
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Icono", style = MaterialTheme.typography.labelLarge)
            CategoryIconPicker(
                selectedIconName = selectedIconName,
                onIconSelected = { selectedIconName = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.addCategory(newCategoryName, selectedColor, selectedIconName)
                    newCategoryName = ""
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = newCategoryName.isNotBlank()
            ) {
                Text("Crear Etiqueta")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Etiquetas actuales",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    CategoryItem(
                        category = category,
                        onDelete = { viewModel.deleteCategory(category) },
                        onUpdate = { viewModel.updateCategory(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(CATEGORY_COLORS) { colorHex ->
            val isSelected = selectedColor == colorHex
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colorHex.toColor())
                    .clickable { onColorSelected(colorHex) }
                    .padding(4.dp)
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryIconPicker(
    selectedIconName: String,
    onIconSelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(CATEGORY_ICONS) { (name, icon) ->
            val isSelected = selectedIconName == name
            IconButton(
                onClick = { onIconSelected(name) },
                modifier = if (isSelected) Modifier.background(
                    MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                ) else Modifier
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CategoryItem(
    category: Category,
    onDelete: () -> Unit,
    onUpdate: (Category) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showEditDialog = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(category.color.toColor()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(category.icon),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = category.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showEditDialog) {
        EditCategoryDialog(
            category = category,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedCategory ->
                onUpdate(updatedCategory)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun EditCategoryDialog(
    category: Category,
    onDismiss: () -> Unit,
    onConfirm: (Category) -> Unit
) {
    var name by remember { mutableStateOf(category.name) }
    var color by remember { mutableStateOf(category.color) }
    var iconName by remember { mutableStateOf(category.icon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Etiqueta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text("Color", style = MaterialTheme.typography.labelMedium)
                    CategoryColorPicker(
                        selectedColor = color,
                        onColorSelected = { color = it }
                    )
                }

                Column {
                    Text("Icono", style = MaterialTheme.typography.labelMedium)
                    CategoryIconPicker(
                        selectedIconName = iconName,
                        onIconSelected = { iconName = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(category.copy(name = name, color = color, icon = iconName))
                },
                enabled = name.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
