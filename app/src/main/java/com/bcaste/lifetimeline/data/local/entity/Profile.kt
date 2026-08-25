package com.bcaste.lifetimeline.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val id: String,
    val name: String,
    val isVisibleInMain: Boolean = false,
    val color: String = "#3D82F5"
) {
    companion object {
        const val MAIN_ID = "main"
        fun createMain() = Profile(MAIN_ID, "Principal", true, "#3D82F5")
    }
}
