package com.bcaste.lifetimeline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.bcaste.lifetimeline.theme.LifeTimelineTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    val splashScreen = installSplashScreen()
    super.onCreate(savedInstanceState)

    // Si no está cargando nada pesado, forzamos que dure menos (~800ms)
    // El sistema por defecto suele mostrarla 1.5s - 2s
    var keepSplashScreen = true
    splashScreen.setKeepOnScreenCondition { keepSplashScreen }
    
    lifecycleScope.launch {
        delay(800)
        keepSplashScreen = false
    }

    enableEdgeToEdge()
    setContent {
      LifeTimelineTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
