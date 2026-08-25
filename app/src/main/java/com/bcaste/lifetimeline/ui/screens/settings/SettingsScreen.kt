package com.bcaste.lifetimeline.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(
    onNavigateToCategories: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    modifier: Modifier = Modifier,
    securityViewModel: SecurityViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel()
) {
    val isPasswordSet by securityViewModel.isPasswordSet.collectAsState()
    val backupState by backupViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showPasswordDialog by remember { mutableStateOf(false) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            uri?.let {
                val outputStream = context.contentResolver.openOutputStream(it)
                backupViewModel.createBackup(outputStream)
            }
        }
    )

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                backupViewModel.restoreBackup(inputStream)
            }
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Configuración",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ListItem(
            headlineContent = { Text("Gestionar Etiquetas") },
            supportingContent = { Text("Crea y organiza tus etiquetas personalizadas") },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            modifier = Modifier.clickable { onNavigateToCategories() }
        )

        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Gestionar Perfiles") },
            supportingContent = { Text("Organiza tus eventos en diferentes perfiles (Personal, Trabajo...)") },
            leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.clickable { onNavigateToProfiles() }
        )

        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Bloqueo de aplicación") },
            supportingContent = { Text(if (isPasswordSet) "Contraseña activada" else "Sin contraseña (desactivado)") },
            leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
            modifier = Modifier.clickable { showPasswordDialog = true }
        )

        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Copia de Seguridad") },
            supportingContent = { Text("Exportar todos tus datos a un archivo ZIP") },
            leadingContent = { Icon(Icons.Default.Backup, contentDescription = null) },
            modifier = Modifier.clickable { 
                createBackupLauncher.launch("LifeTimeline_Backup.zip")
            }
        )

        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Restaurar Datos") },
            supportingContent = { Text("Importar datos desde un archivo de copia anterior") },
            leadingContent = { Icon(Icons.Default.Upload, contentDescription = null) },
            modifier = Modifier.clickable { 
                restoreBackupLauncher.launch(arrayOf("application/zip"))
            }
        )

        HorizontalDivider()
    }

    if (showPasswordDialog) {
        PasswordManagementDialog(
            isCurrentlySet = isPasswordSet,
            onDismiss = { showPasswordDialog = false },
            onSave = { newPassword ->
                securityViewModel.updatePassword(newPassword)
                showPasswordDialog = false
            }
        )
    }

    // Backup/Restore Feedback Dialogs
    if (backupState is BackupUiState.Loading) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Procesando...") },
            text = { 
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator() 
                }
            },
            confirmButton = {}
        )
    }

    if (backupState is BackupUiState.Success) {
        AlertDialog(
            onDismissRequest = { backupViewModel.resetState() },
            title = { Text("Operación completada") },
            text = { Text("Los datos se han procesado correctamente. Si has restaurado, reinicia la app para aplicar cambios.") },
            confirmButton = {
                TextButton(onClick = { backupViewModel.resetState() }) { Text("Aceptar") }
            }
        )
    }

    if (backupState is BackupUiState.Error) {
        AlertDialog(
            onDismissRequest = { backupViewModel.resetState() },
            title = { Text("Error") },
            text = { Text((backupState as BackupUiState.Error).message) },
            confirmButton = {
                TextButton(onClick = { backupViewModel.resetState() }) { Text("Cerrar") }
            }
        )
    }
}

@Composable
fun PasswordManagementDialog(
    isCurrentlySet: Boolean,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCurrentlySet) "Cambiar Contraseña" else "Establecer Contraseña") },
        text = {
            Column {
                Text(
                    text = "Introduce la nueva contraseña. Déjala vacía para desactivar el bloqueo.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(if (password.isBlank()) null else password) }) {
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
