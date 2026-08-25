package com.bcaste.lifetimeline.ui.screens.lock

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.bcaste.lifetimeline.R
import com.bcaste.lifetimeline.data.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val securityManager: SecurityManager
) : ViewModel() {
    val isPasswordSet: Boolean get() = securityManager.isPasswordSet()

    fun checkPassword(input: String): Boolean {
        return securityManager.checkPassword(input)
    }
}

@Composable
fun LockScreen(
    onUnlock: () -> Unit,
    viewModel: LockViewModel = hiltViewModel()
) {
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo1),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
                
                Spacer(modifier = Modifier.width(48.dp))
                
                Column(modifier = Modifier.width(300.dp)) {
                    LockContent(
                        password = password,
                        onPasswordChange = { 
                            password = it
                            isError = false
                        },
                        isError = isError,
                        onUnlock = {
                            if (viewModel.checkPassword(password)) {
                                onUnlock()
                            } else {
                                isError = true
                            }
                        }
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo1),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                LockContent(
                    password = password,
                    onPasswordChange = { 
                        password = it
                        isError = false
                    },
                    isError = isError,
                    onUnlock = {
                        if (viewModel.checkPassword(password)) {
                            onUnlock()
                        } else {
                            isError = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LockContent(
    password: String,
    onPasswordChange: (String) -> Unit,
    isError: Boolean,
    onUnlock: () -> Unit
) {
    Column {
        Text(
            text = "Aplicación Bloqueada",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        
        Text(
            text = "Introduce tu contraseña para continuar",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White.copy(alpha = 0.6f)
            ),
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
            )
        )

        if (isError) {
            Text(
                text = "Contraseña incorrecta",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onUnlock,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Desbloquear")
        }
    }
}
