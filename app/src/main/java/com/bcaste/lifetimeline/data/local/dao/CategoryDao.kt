package com.bcaste.lifetimeline.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.bcaste.lifetimeline.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE profileId = :profileId")
    fun getCategoriesByProfile(profileId: String): Flow<List<Category>>

    @Query("""
        SELECT c.* FROM categories c
        INNER JOIN profiles p ON c.profileId = p.id
        WHERE (:profileId = 'main' AND (c.profileId = 'main' OR p.isVisibleInMain = 1))
           OR (:profileId != 'main' AND c.profileId = :profileId)
    """)
    fun getCategoriesForProfile(profileId: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): Category?
}
