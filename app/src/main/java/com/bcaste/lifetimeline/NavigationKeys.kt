package com.bcaste.lifetimeline

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object TimelineVertical : NavKey
@Serializable data object TimelineInfographic : NavKey
@Serializable data object RegisterEvent : NavKey
@Serializable data class ModifyEvent(val eventId: String) : NavKey
@Serializable data object CategoryManager : NavKey
@Serializable data object ProfileManager : NavKey
@Serializable data object Calendar : NavKey
@Serializable data object SearchFilter : NavKey
@Serializable data object Settings : NavKey
@Serializable data class ImagePager(val imageUris: List<String>, val initialIndex: Int) : NavKey
