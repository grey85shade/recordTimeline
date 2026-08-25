package com.bcaste.lifetimeline.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EventType {
    POINT,   // Evento puntual
    PERIOD   // Período con fecha inicio y fin
}

@Entity(tableName = "timeline_events")
data class TimelineEvent(
    @PrimaryKey val id: String,
    val profileId: String = Profile.MAIN_ID,
    val title: String,
    val description: String,
    val timestamp: Long,
    val eventType: EventType = EventType.POINT,
    val endTimestamp: Long? = null,  // Solo para PERIOD
    val isSynced: Boolean = false,
    val lastSyncTime: Long = 0L
)
