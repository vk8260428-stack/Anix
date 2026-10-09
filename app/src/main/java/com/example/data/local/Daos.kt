package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(entity: WatchHistoryEntity)

    @Query("SELECT * FROM watch_history WHERE contentId = :contentId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestForContent(contentId: String): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history WHERE contentId = :contentId AND seasonNumber = :season AND episodeNumber = :episode LIMIT 1")
    suspend fun getForEpisode(contentId: String, season: Int, episode: Int): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history ORDER BY updatedAt DESC")
    fun getAllRecentHistory(): Flow<List<WatchHistoryEntity>>

    @Query("DELETE FROM watch_history WHERE contentId = :contentId")
    suspend fun deleteForContent(contentId: String)
}

@Dao
interface WatchlistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE contentId = :contentId")
    suspend fun remove(contentId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE contentId = :contentId)")
    fun isWatchlisted(contentId: String): Flow<Boolean>

    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getAll(): Flow<List<WatchlistEntity>>
}

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadEntity)

    @Query("DELETE FROM downloads WHERE downloadId = :downloadId")
    suspend fun delete(downloadId: String)

    @Query("SELECT * FROM downloads WHERE contentId = :contentId AND seasonNumber = :season AND episodeNumber = :episode LIMIT 1")
    suspend fun getDownload(contentId: String, season: Int, episode: Int): DownloadEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE contentId = :contentId AND seasonNumber = :season AND episodeNumber = :episode AND isDownloaded = 1)")
    fun isDownloadedFlow(contentId: String, season: Int, episode: Int): Flow<Boolean>

    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>
}
