package com.momostack.app.feature.screenshotvault.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ScreenshotEntity in ScreenshotVaultDatabase.
 */
@Dao
interface ScreenshotDao {
    @Query("SELECT * FROM screenshots ORDER BY createdAt DESC")
    fun getAllScreenshots(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE id = :id")
    suspend fun getScreenshotById(id: Long): ScreenshotEntity?

    @Query("SELECT * FROM screenshots WHERE extractedText LIKE '%' || :query || '%' OR (userNotes IS NOT NULL AND userNotes LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun searchScreenshots(query: String): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteScreenshots(): Flow<List<ScreenshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreenshot(screenshot: ScreenshotEntity): Long

    @Update
    suspend fun updateScreenshot(screenshot: ScreenshotEntity)

    @Delete
    suspend fun deleteScreenshot(screenshot: ScreenshotEntity)

    @Query("DELETE FROM screenshots WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM screenshots")
    suspend fun getScreenshotCount(): Int

    @Query("SELECT originalUri FROM screenshots")
    suspend fun getAllOriginalUris(): List<String>

    @Query("SELECT * FROM screenshots ORDER BY createdAt DESC")
    suspend fun getAllScreenshotsList(): List<ScreenshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(screenshots: List<ScreenshotEntity>)
}
