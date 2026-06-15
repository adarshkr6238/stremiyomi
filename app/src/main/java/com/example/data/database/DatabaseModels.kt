package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installed_extensions")
data class InstalledExtension(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // "stremio" or "anime" or "manga"
    val baseUrl: String,
    val iconUrl: String,
    val description: String,
    val isBuiltIn: Boolean = false,
    val isEnabled: Boolean = true
)

@Entity(tableName = "media_bookmarks")
data class MediaBookmark(
    @PrimaryKey val id: String, // E.g. "cinemeta:movie:tt12345" or "anime:gogoanime:naruto"
    val title: String,
    val type: String, // "movie", "series", "anime", "manga"
    val poster: String,
    val banner: String,
    val description: String,
    val rating: String,
    val year: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_history")
data class WatchHistory(
    @PrimaryKey val mediaId: String,
    val title: String,
    val type: String, // "movie", "series", "anime", "manga"
    val poster: String,
    val lastWatchedEpisode: Int = 0,
    val lastWatchedSeason: Int = 0,
    val lastWatchedProgress: Long = 0L, // playback milliseconds
    val lastWatchedTotal: Long = 0L, // total milliseconds
    val lastReadChapter: Int = 0,
    val lastReadPage: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
