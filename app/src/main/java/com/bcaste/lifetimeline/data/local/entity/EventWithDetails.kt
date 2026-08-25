package com.bcaste.lifetimeline.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class EventWithDetails(
    @Embedded val event: TimelineEvent,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = EventCategoryCrossRef::class,
            parentColumn = "eventId",
            entityColumn = "categoryId"
        )
    )
    val categories: List<Category>,

    @Relation(
        parentColumn = "id",
        entityColumn = "eventId"
    )
    val images: List<EventImage>
)
