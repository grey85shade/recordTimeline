package com.bcaste.lifetimeline.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.bcaste.lifetimeline.data.local.entity.EventCategoryCrossRef
import com.bcaste.lifetimeline.data.local.entity.EventImage
import com.bcaste.lifetimeline.data.local.entity.EventWithDetails
import com.bcaste.lifetimeline.data.local.entity.TimelineEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: TimelineEvent)

    @Update
    suspend fun updateEvent(event: TimelineEvent)

    @Delete
    suspend fun deleteEvent(event: TimelineEvent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventImages(images: List<EventImage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventCategoryCrossRefs(crossRefs: List<EventCategoryCrossRef>)

    @Delete
    suspend fun deleteEventCategoryCrossRefs(crossRefs: List<EventCategoryCrossRef>)

    @Query("DELETE FROM event_category_cross_ref WHERE eventId = :eventId")
    suspend fun deleteCategoriesByEventId(eventId: String)

    @Query("DELETE FROM event_images WHERE eventId = :eventId")
    suspend fun deleteImagesByEventId(eventId: String)

    @Transaction
    suspend fun insertEventWithDetails(
        event: TimelineEvent,
        images: List<EventImage>,
        crossRefs: List<EventCategoryCrossRef>
    ) {
        insertEvent(event)
        if (images.isNotEmpty()) insertEventImages(images)
        if (crossRefs.isNotEmpty()) insertEventCategoryCrossRefs(crossRefs)
    }

    @Transaction
    suspend fun updateEventWithDetails(
        event: TimelineEvent,
        images: List<EventImage>,
        crossRefs: List<EventCategoryCrossRef>
    ) {
        updateEvent(event)
        deleteCategoriesByEventId(event.id)
        deleteImagesByEventId(event.id)
        if (images.isNotEmpty()) insertEventImages(images)
        if (crossRefs.isNotEmpty()) insertEventCategoryCrossRefs(crossRefs)
    }

    @Transaction
    @Query("SELECT * FROM timeline_events ORDER BY timestamp DESC")
    fun getAllEventsWithDetails(): Flow<List<EventWithDetails>>

    @Transaction
    @Query("SELECT * FROM timeline_events WHERE id = :id")
    fun getEventWithDetailsById(id: String): Flow<EventWithDetails?>

    @Transaction
    @Query("""
        SELECT e.* FROM timeline_events e
        INNER JOIN profiles p ON e.profileId = p.id
        WHERE (:profileId = 'main' AND (e.profileId = 'main' OR p.isVisibleInMain = 1))
           OR (:profileId != 'main' AND e.profileId = :profileId)
        ORDER BY e.timestamp DESC
    """)
    fun getEventsByProfile(profileId: String): Flow<List<EventWithDetails>>
}
