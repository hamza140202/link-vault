package com.momostack.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.momostack.app.data.entity.LinkItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkItemDao {

    @Query("SELECT * FROM link_items WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllActive(): Flow<List<LinkItemEntity>>

    @Query("SELECT * FROM link_items WHERE isFavorite = 1 AND isArchived = 0 ORDER BY createdAt DESC")
    fun getFavorites(): Flow<List<LinkItemEntity>>

    @Query("SELECT * FROM link_items WHERE isArchived = 1 ORDER BY createdAt DESC")
    fun getArchived(): Flow<List<LinkItemEntity>>

    @Query("SELECT * FROM link_items WHERE category = :category AND isArchived = 0 ORDER BY createdAt DESC")
    fun getByCategory(category: String): Flow<List<LinkItemEntity>>

    @Query("SELECT * FROM link_items WHERE notes IS NOT NULL AND notes != '' AND isArchived = 0 ORDER BY createdAt DESC")
    fun getNotes(): Flow<List<LinkItemEntity>>

    @Query("SELECT * FROM link_items WHERE id = :id")
    suspend fun getById(id: String): LinkItemEntity?

    @Query("SELECT * FROM link_items WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): LinkItemEntity?

    @Query("""
        SELECT * FROM link_items 
        WHERE (title LIKE '%' || :query || '%' 
           OR domain LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%' 
           OR notes LIKE '%' || :query || '%' 
           OR url LIKE '%' || :query || '%')
          AND isArchived = 0
        ORDER BY createdAt DESC
    """)
    fun search(query: String): Flow<List<LinkItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LinkItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LinkItemEntity>)

    @Update
    suspend fun update(item: LinkItemEntity)

    @Delete
    suspend fun delete(item: LinkItemEntity)

    @Query("DELETE FROM link_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM link_items")
    suspend fun countTotal(): Int

    @Query("SELECT COUNT(*) FROM link_items WHERE isFavorite = 1")
    suspend fun countFavorites(): Int

    @Query("SELECT COUNT(*) FROM link_items WHERE notes IS NOT NULL AND notes != ''")
    suspend fun countNotes(): Int

    @Query("SELECT * FROM link_items")
    suspend fun getAllList(): List<LinkItemEntity>

    @Query("DELETE FROM link_items")
    suspend fun clearAll()
}
