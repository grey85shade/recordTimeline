package com.bcaste.lifetimeline.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val CATEGORY_COLORS = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
    "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
    "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800",
    "#FF5722", "#795548", "#9E9E9E", "#607D8B"
)

val CATEGORY_ICONS = listOf(
    "label" to Icons.AutoMirrored.Filled.Label,
    "work" to Icons.Default.Work,
    "person" to Icons.Default.Person,
    "star" to Icons.Default.Star,
    "favorite" to Icons.Default.Favorite,
    "home" to Icons.Default.Home,
    "event" to Icons.Default.Event,
    "shopping_cart" to Icons.Default.ShoppingCart,
    "fitness_center" to Icons.Default.FitnessCenter,
    "school" to Icons.Default.School,
    "restaurant" to Icons.Default.Restaurant,
    "directions_car" to Icons.Default.DirectionsCar,
    "terrain" to Icons.Default.Terrain,
    "landscape" to Icons.Default.Landscape,
    "flight" to Icons.Default.Flight,
    "pets" to Icons.Default.Pets
)

fun String.toColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color.Gray
    }
}

fun getIconByName(name: String): ImageVector {
    return CATEGORY_ICONS.firstOrNull { it.first == name }?.second ?: Icons.AutoMirrored.Filled.Label
}
