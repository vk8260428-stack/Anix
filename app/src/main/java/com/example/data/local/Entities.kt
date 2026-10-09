package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watch_history",
    indices = [Index(value = ["contentId", "seasonNumber", "episodeNumber"], unique = true)]
)
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contentId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val lastPositionMs: Long,
    val totalDurationMs: Long,
    val isCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey
    val contentId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val downloadId: String,
    val contentId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val filePath: String,
    val fileSizeBytes: Long,
    val downloadedAt: Long = System.currentTimeMillis(),
    val isDownloaded: Boolean = true
)
