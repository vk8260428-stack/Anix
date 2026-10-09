package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import com.example.data.local.DownloadEntity
import com.example.data.local.StreamXDatabase
import com.example.data.local.WatchHistoryEntity
import com.example.data.local.WatchlistEntity
import com.example.data.model.ContentItem
import com.example.data.model.ContentType
import com.example.data.model.Episode
import com.example.data.model.SampleData
import com.example.player.StreamXDownloadModule
import com.example.player.StreamXPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NavigationTab {
    HOME, SEARCH, DOWNLOADS
}

data class DownloadProgressState(
    val contentId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val progressPercent: Int
)

class StreamXViewModel(application: Application) : AndroidViewModel(application) {

    val database = StreamXDatabase.getDatabase(application)
    val playerManager = StreamXPlayerManager.getInstance(application)

    private val _currentTab = MutableStateFlow(NavigationTab.HOME)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    private val _selectedContent = MutableStateFlow<ContentItem?>(null)
    val selectedContent: StateFlow<ContentItem?> = _selectedContent.asStateFlow()

    private val _isDetailOpen = MutableStateFlow(false)
    val isDetailOpen: StateFlow<Boolean> = _isDetailOpen.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _isMiniPlayerActive = MutableStateFlow(false)
    val isMiniPlayerActive: StateFlow<Boolean> = _isMiniPlayerActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoadingContent = MutableStateFlow(true)
    val isLoadingContent: StateFlow<Boolean> = _isLoadingContent.asStateFlow()

    private val _isLoadingDownloads = MutableStateFlow(true)
    val isLoadingDownloads: StateFlow<Boolean> = _isLoadingDownloads.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _activeDownloadsInProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val activeDownloadsInProgress: StateFlow<Map<String, Int>> = _activeDownloadsInProgress.asStateFlow()

    init {
        viewModelScope.launch {
            delay(650)
            _isLoadingContent.value = false
            _isLoadingDownloads.value = false
        }

        // Register DownloadManager listener to keep UI and Room DB in sync
        try {
            val downloadManager = StreamXDownloadModule.getDownloadManager(application)
            downloadManager.addListener(object : DownloadManager.Listener {
                override fun onDownloadChanged(
                    downloadManager: DownloadManager,
                    download: Download,
                    finalException: Exception?
                ) {
                    val key = download.request.id
                    when (download.state) {
                        Download.STATE_QUEUED, Download.STATE_DOWNLOADING, Download.STATE_RESTARTING -> {
                            val pct = if (download.percentDownloaded >= 0f) {
                                download.percentDownloaded.toInt().coerceIn(1, 99)
                            } else {
                                25
                            }
                            _activeDownloadsInProgress.update { it + (key to pct) }
                        }
                        Download.STATE_COMPLETED -> {
                            _activeDownloadsInProgress.update { it - key }
                            viewModelScope.launch(Dispatchers.IO) {
                                val parts = key.split("_")
                                val contentId = parts.getOrNull(0) ?: ""
                                val seasonNum = parts.getOrNull(1)?.removePrefix("s")?.toIntOrNull() ?: 1
                                val epNum = parts.getOrNull(2)?.removePrefix("e")?.toIntOrNull() ?: 1
                                val streamUri = download.request.uri.toString()
                                val bytes = if (download.bytesDownloaded > 0L) download.bytesDownloaded else 240L * 1024L * 1024L
                                database.downloadDao().insert(
                                    DownloadEntity(
                                        downloadId = key,
                                        contentId = contentId,
                                        seasonNumber = seasonNum,
                                        episodeNumber = epNum,
                                        filePath = streamUri,
                                        fileSizeBytes = bytes,
                                        downloadedAt = System.currentTimeMillis(),
                                        isDownloaded = true
                                    )
                                )
                            }
                        }
                        Download.STATE_FAILED, Download.STATE_REMOVING, Download.STATE_STOPPED -> {
                            _activeDownloadsInProgress.update { it - key }
                        }
                    }
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("StreamXViewModel", "Error attaching DownloadManager listener", e)
        }
    }

    val watchlistIds: StateFlow<Set<String>> = database.watchlistDao().getAll()
        .combine(MutableStateFlow(Unit)) { list, _ ->
            list.map { it.contentId }.toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val allDownloads: StateFlow<List<DownloadEntity>> = database.downloadDao().getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = database.watchHistoryDao().getAllRecentHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults: StateFlow<List<ContentItem>> = _searchQuery
        .combine(MutableStateFlow(SampleData.seriesList)) { query, allItems ->
            if (query.isBlank()) {
                allItems
            } else {
                allItems.filter { item ->
                    item.title.contains(query, ignoreCase = true) ||
                            item.description.contains(query, ignoreCase = true) ||
                            item.genres.any { it.contains(query, ignoreCase = true) }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SampleData.seriesList)

    fun selectTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun openContentDetail(content: ContentItem, season: Int? = null, episode: Int? = null) {
        _selectedContent.value = content
        _isDetailOpen.value = true
        _isMiniPlayerActive.value = false

        // Start playback immediately (Auto-play & history auto-resume)
        playerManager.playContent(
            content = content,
            requestedSeason = season,
            requestedEpisode = episode
        )
    }

    fun minimizeToMiniPlayer() {
        _isDetailOpen.value = false
        _isFullscreen.value = false
        // If audio/video is playing or active, keep running in mini-player
        if (playerManager.playerState.value.activeContent != null) {
            _isMiniPlayerActive.value = true
        }
    }

    fun expandMiniPlayer() {
        val activeContent = playerManager.playerState.value.activeContent
        if (activeContent != null) {
            _selectedContent.value = activeContent
            _isDetailOpen.value = true
            _isMiniPlayerActive.value = false
        }
    }

    fun dismissMiniPlayer() {
        _isMiniPlayerActive.value = false
        playerManager.exoPlayer.pause()
    }

    fun toggleFullscreen() {
        _isFullscreen.update { !it }
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            viewModelScope.launch {
                _isSearching.value = true
                delay(200)
                _isSearching.value = false
            }
        }
    }

    fun refreshContent() {
        viewModelScope.launch {
            _isLoadingContent.value = true
            _isLoadingDownloads.value = true
            delay(650)
            _isLoadingContent.value = false
            _isLoadingDownloads.value = false
        }
    }

    fun toggleWatchlist(contentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val isPresent = watchlistIds.value.contains(contentId)
            if (isPresent) {
                database.watchlistDao().remove(contentId)
            } else {
                database.watchlistDao().insert(WatchlistEntity(contentId = contentId))
            }
        }
    }

    fun downloadEpisode(content: ContentItem, seasonNumber: Int, episodeNumber: Int) {
        val downloadKey = "${content.id}_s${seasonNumber}_e${episodeNumber}"
        if (_activeDownloadsInProgress.value.containsKey(downloadKey)) return

        val episode = content.seasons.find { it.seasonNumber == seasonNumber }
            ?.episodes?.find { it.episodeNumber == episodeNumber }
        val streamUrl = episode?.videoUrl ?: content.directVideoUrl

        _activeDownloadsInProgress.update { it + (downloadKey to 5) }

        try {
            val downloadManager = StreamXDownloadModule.getDownloadManager(getApplication())
            val downloadRequest = StreamXDownloadModule.buildDownloadRequest(
                downloadId = downloadKey,
                uri = Uri.parse(streamUrl),
                contentId = content.id
            )
            downloadManager.addDownload(downloadRequest)
        } catch (e: Exception) {
            android.util.Log.e("StreamXViewModel", "Error starting real download", e)
        }
    }

    fun removeDownload(downloadId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val downloadManager = StreamXDownloadModule.getDownloadManager(getApplication())
                downloadManager.removeDownload(downloadId)
            } catch (e: Exception) {
                android.util.Log.e("StreamXViewModel", "Error removing download", e)
            }
            database.downloadDao().delete(downloadId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
