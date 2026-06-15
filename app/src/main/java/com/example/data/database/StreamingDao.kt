package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamingDao {
    // Installed Extensions Queries
    @Query("SELECT * FROM installed_extensions ORDER BY isBuiltIn DESC, name ASC")
    fun getAllExtensions(): Flow<List<InstalledExtension>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtension(extension: InstalledExtension)

    @Delete
    suspend fun deleteExtension(extension: InstalledExtension)

    @Query("SELECT * FROM installed_extensions WHERE id = :id")
    suspend fun getExtensionById(id: String): InstalledExtension?


    // Media Bookmarks Queries
    @Query("SELECT * FROM media_bookmarks ORDER BY lastUpdated DESC")
    fun getAllBookmarks(): Flow<List<MediaBookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: MediaBookmark)

    @Query("DELETE FROM media_bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM media_bookmarks WHERE id = :id)")
    fun isBookmarked(id: String): Flow<Boolean>


    // Watch History Queries
    @Query("SELECT * FROM watch_history ORDER BY lastUpdated DESC")
    fun getAllHistory(): Flow<List<WatchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: WatchHistory)

    @Query("SELECT * FROM watch_history WHERE mediaId = :mediaId")
    suspend fun getHistoryItem(mediaId: String): WatchHistory?

    @Query("DELETE FROM watch_history WHERE mediaId = :mediaId")
    suspend fun deleteHistoryById(mediaId: String)
}
