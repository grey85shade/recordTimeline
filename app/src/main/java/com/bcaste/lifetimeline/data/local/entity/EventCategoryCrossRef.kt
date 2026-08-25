package com.bcaste.lifetimeline.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "event_category_cross_ref",
    primaryKeys = ["eventId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = TimelineEvent::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class EventCategoryCrossRef(
    val eventId: String,
    val categoryId: String
)
