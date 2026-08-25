package com.bcaste.lifetimeline.ui.screens.image

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.abs

@Composable
fun FullscreenImageViewerScreen(
    imageUris: List<String>,
    initialIndex: Int,
    onClose: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) {
        imageUris.size
    }
    var isPagerScrollEnabled by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 16.dp,
            contentPadding = PaddingValues(0.dp),
            userScrollEnabled = isPagerScrollEnabled
        ) { pageIndex ->
            ZoomableImage(
                uri = imageUris[pageIndex],
                onZoomChanged = { zoomed ->
                    isPagerScrollEnabled = !zoomed
                }
            )
        }

        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Page indicator
        if (imageUris.size > 1) {
            Text(
                text = "${pagerState.currentPage + 1} / ${imageUris.size}",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ZoomableImage(uri: String, onZoomChanged: (Boolean) -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) {
                        scale = 1f
                        offset = Offset.Zero
                        onZoomChanged(false)
                    } else {
                        scale = 3f
                        onZoomChanged(true)
                    }
                })
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()

                        if (scale > 1f || zoomChange != 1f) {
                            // Consume events if zoomed or zooming
                            scale = (scale * zoomChange).coerceIn(1f, 5f)
                            
                            if (scale > 1f) {
                                offset += panChange
                            } else {
                                offset = Offset.Zero
                            }
                            
                            onZoomChanged(scale > 1f)
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
    }
}
