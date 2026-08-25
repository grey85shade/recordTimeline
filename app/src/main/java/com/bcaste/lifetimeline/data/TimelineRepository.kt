package com.bcaste.lifetimeline.data

import com.bcaste.lifetimeline.data.local.dao.CategoryDao
import com.bcaste.lifetimeline.data.local.dao.ProfileDao
import com.bcaste.lifetimeline.data.local.dao.TimelineDao
import com.bcaste.lifetimeline.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

interface TimelineRepository {
    suspend fun insertEvent(
        event: TimelineEvent,
        categoryIds: List<String> = emptyList(),
        imageUris: List<String> = emptyList()
    )
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesByProfile(profileId: String): Flow<List<Category>>
    fun getCategoriesForProfile(profileId: String): Flow<List<Category>>
    fun getAllEvents(): Flow<List<EventWithDetails>>
    fun getEventsByProfile(profileId: String): Flow<List<EventWithDetails>>
    fun getEventById(id: String): Flow<EventWithDetails?>
    suspend fun deleteEvent(event: TimelineEvent)
    suspend fun insertCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun updateEvent(
        event: TimelineEvent,
        categoryIds: List<String> = emptyList(),
        imageUris: List<String> = emptyList()
    )
    
    // Profiles
    fun getAllProfiles(): Flow<List<Profile>>
    suspend fun insertProfile(profile: Profile)
    suspend fun deleteProfile(profile: Profile)
    suspend fun getProfileById(id: String): Profile?
}

class TimelineRepositoryImpl @Inject constructor(
    private val timelineDao: TimelineDao,
    private val categoryDao: CategoryDao,
    private val profileDao: ProfileDao,
    private val imageStorageManager: ImageStorageManager
) : TimelineRepository {

    init {
        // Initialize default profiles and categories in a background scope
        CoroutineScope(Dispatchers.IO).launch {
            // 1. Ensure Main Profile exists
            val mainProfile = profileDao.getProfileById(Profile.MAIN_ID)
            if (mainProfile == null) {
                profileDao.insertProfile(Profile.createMain())
            }

            // 2. Ensure default categories exist
            val currentCategories = categoryDao.getAllCategories().first()
            if (currentCategories.isEmpty()) {
                val defaults = listOf(
                    Category("cat_study", Profile.MAIN_ID, "Study", "#9C27B0", "school"),
                    Category("cat_work", Profile.MAIN_ID, "Work", "#2196F3", "work"),
                    Category("cat_travel", Profile.MAIN_ID, "Travel", "#00BCD4", "flight"),
                    Category("cat_hike", Profile.MAIN_ID, "Hike", "#4CAF50", "terrain"),
                    Category("cat_friends", Profile.MAIN_ID, "Friends", "#FF9800", "person"),
                    Category("cat_gim", Profile.MAIN_ID, "Gim", "#F44336", "fitness_center"),
                    Category("cat_car", Profile.MAIN_ID, "Car", "#607D8B", "directions_car"),
                    Category("cat_love", Profile.MAIN_ID, "Love", "#E91E63", "favorite"),
                    Category("cat_pet", Profile.MAIN_ID, "Pet", "#795548", "pets")
                )
                defaults.forEach { categoryDao.insertCategory(it) }
            }
        }
    }

    override suspend fun insertEvent(
        event: TimelineEvent,
        categoryIds: List<String>,
        imageUris: List<String>
    ) {
        val savedImagePaths = imageUris.map { imageStorageManager.saveImage(it) }

        val crossRefs = categoryIds.map { catId ->
            EventCategoryCrossRef(eventId = event.id, categoryId = catId)
        }

        val images = savedImagePaths.map { path ->
            EventImage(
                id = UUID.randomUUID().toString(),
                eventId = event.id,
                imageUri = path
            )
        }

        timelineDao.insertEventWithDetails(event, images, crossRefs)
    }

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
    }

    override fun getCategoriesByProfile(profileId: String): Flow<List<Category>> {
        return categoryDao.getCategoriesByProfile(profileId)
    }

    override fun getCategoriesForProfile(profileId: String): Flow<List<Category>> {
        return categoryDao.getCategoriesForProfile(profileId)
    }

    override fun getAllEvents(): Flow<List<EventWithDetails>> {
        return timelineDao.getAllEventsWithDetails().map { list ->
            list.map { item ->
                item.copy(images = item.images.map { img ->
                    img.copy(imageUri = imageStorageManager.resolveUri(img.imageUri))
                })
            }
        }
    }

    override fun getEventsByProfile(profileId: String): Flow<List<EventWithDetails>> {
        return timelineDao.getEventsByProfile(profileId).map { list ->
            list.map { item ->
                item.copy(images = item.images.map { img ->
                    img.copy(imageUri = imageStorageManager.resolveUri(img.imageUri))
                })
            }
        }
    }

    override fun getEventById(id: String): Flow<EventWithDetails?> {
        return timelineDao.getEventWithDetailsById(id).map { item ->
            item?.copy(images = item.images.map { img ->
                img.copy(imageUri = imageStorageManager.resolveUri(img.imageUri))
            })
        }
    }

    override suspend fun deleteEvent(event: TimelineEvent) {
        val eventWithDetails = timelineDao.getEventWithDetailsById(event.id).first()
        eventWithDetails?.images?.forEach { image ->
            imageStorageManager.deleteImage(image.imageUri)
        }
        timelineDao.deleteEvent(event)
    }

    override suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    override suspend fun updateEvent(
        event: TimelineEvent,
        categoryIds: List<String>,
        imageUris: List<String>
    ) {
        val savedImagePaths = imageUris.map { imageStorageManager.saveImage(it) }

        val currentEvent = timelineDao.getEventWithDetailsById(event.id).first()
        currentEvent?.images?.forEach { oldImage ->
            if (!savedImagePaths.contains(oldImage.imageUri)) {
                imageStorageManager.deleteImage(oldImage.imageUri)
            }
        }

        val crossRefs = categoryIds.map { catId ->
            EventCategoryCrossRef(eventId = event.id, categoryId = catId)
        }

        val images = savedImagePaths.map { path ->
            EventImage(
                id = UUID.randomUUID().toString(),
                eventId = event.id,
                imageUri = path
            )
        }

        timelineDao.updateEventWithDetails(event, images, crossRefs)
    }

    override fun getAllProfiles(): Flow<List<Profile>> {
        return profileDao.getAllProfiles()
    }

    override suspend fun insertProfile(profile: Profile) {
        profileDao.insertProfile(profile)
    }

    override suspend fun deleteProfile(profile: Profile) {
        profileDao.deleteProfile(profile)
    }

    override suspend fun getProfileById(id: String): Profile? {
        return profileDao.getProfileById(id)
    }
}
