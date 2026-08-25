package com.bcaste.lifetimeline.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String,
    val profileId: String = Profile.MAIN_ID,
    val name: String,
    val color: String,
    val icon: String,
    val isSynced: Boolean = false,
    val lastSyncTime: Long = 0L
)
