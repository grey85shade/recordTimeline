package com.bcaste.lifetimeline.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "event_images",
    foreignKeys = [
        ForeignKey(
            entity = TimelineEvent::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventId")]
)
data class EventImage(
    @PrimaryKey val id: String,
    val eventId: String,
    val imageUri: String,
    val isSynced: Boolean = false,
    val lastSyncTime: Long = 0L
)
