package com.example.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import kotlin.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.data.local.StreamXDatabase
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.ContentItem
import com.example.data.model.ContentType
import com.example.data.model.Episode
import com.example.data.model.SampleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlayerState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    val activeContent: ContentItem? = null,
    val activeEpisode: Episode? = null,
    val activeSeasonNumber: Int = 1,
    val activeEpisodeNumber: Int = 1,
    val isLocked: Boolean = false,
    val selectedQuality: String = "auto",
    val selectedAudioTrack: String = "en_atmos",
    val selectedSubtitle: String = "off",
    // Gesture & HUD states
    val brightnessPercent: Int = 75,
    val showBrightnessHud: Boolean = false,
    val volumePercent: Int = 70,
    val showVolumeHud: Boolean = false,
    val seekPreviewMs: Long? = null,
    val doubleTapSeekSide: String? = null, // "left" or "right"
    val doubleTapSeekDelta: Int = 0,
    // Auto-play Next countdown
    val showNextCountdown: Boolean = false,
    val countdownRemainingSeconds: Int = 10,
    val nextEpisode: Episode? = null,
    // Controls visibility
    val areControlsVisible: Boolean = true
)

@OptIn(UnstableApi::class)
class StreamXPlayerManager private constructor(private val context: Context) {

    private val applicationScope = CoroutineScope(Dispatchers.Main + Job())
    private val database = StreamXDatabase.getDatabase(context)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val cacheDataSourceFactory = StreamXDownloadModule.getCacheDataSourceFactory(context)

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(context)
                .setDataSourceFactory(cacheDataSourceFactory)
        )
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true // handleAudioFocus = true
        )
        .build()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var progressTrackerJob: Job? = null
    private var hideControlsJob: Job? = null
    private var hideBrightnessJob: Job? = null
    private var hideVolumeJob: Job? = null
    private var hideDoubleTapJob: Job? = null
    private var isHandlingErrorFallback = false

    init {
        // Read current volume
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val volPct = (curVol * 100) / maxVol
        _playerState.update { it.copy(volumePercent = volPct) }

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("StreamXPlayer", "Playback error encountered: ${error.errorCodeName} - ${error.message}", error)
                if (!isHandlingErrorFallback) {
                    isHandlingErrorFallback = true
                    applicationScope.launch {
                        delay(500)
                        val fallbackItem = MediaItem.Builder()
                            .setUri(Uri.parse(SampleData.DEFAULT_FALLBACK_STREAM))
                            .build()
                        exoPlayer.setMediaItem(fallbackItem)
                        exoPlayer.prepare()
                        exoPlayer.play()
                        delay(2000)
                        isHandlingErrorFallback = false
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                    scheduleHideControls()
                } else {
                    saveCurrentPositionToDb()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _playerState.update { it.copy(isBuffering = true) }
                    }
                    Player.STATE_READY -> {
                        val duration = exoPlayer.duration.coerceAtLeast(0L)
                        _playerState.update {
                            it.copy(
                                isBuffering = false,
                                isEnded = false,
                                totalDurationMs = duration,
                                bufferedPositionMs = exoPlayer.bufferedPosition
                            )
                        }
                    }
                    Player.STATE_ENDED -> {
                        _playerState.update { it.copy(isEnded = true, isPlaying = false) }
                        saveCurrentPositionToDb(isCompleted = true)
                        playNextEpisode(autoPlay = true)
                    }
                    Player.STATE_IDLE -> {
                        _playerState.update { it.copy(isBuffering = false) }
                    }
                }
            }
        })
    }

    fun playContent(
        content: ContentItem,
        requestedSeason: Int? = null,
        requestedEpisode: Int? = null,
        forceResumeTimestampMs: Long? = null
    ) {
        applicationScope.launch {
            // Determine episode to play
            val episodeToPlay: Episode?
            val seasonNum: Int
            val epNum: Int
            val streamUrl: String

            if (content.type == ContentType.SERIES && content.seasons.isNotEmpty()) {
                val targetSeason = if (requestedSeason != null) {
                    content.seasons.find { it.seasonNumber == requestedSeason } ?: content.seasons.first()
                } else {
                    content.seasons.first()
                }

                val targetEp = if (requestedEpisode != null) {
                    targetSeason.episodes.find { it.episodeNumber == requestedEpisode } ?: targetSeason.episodes.firstOrNull()
                } else {
                    targetSeason.episodes.firstOrNull()
                }

                episodeToPlay = targetEp
                seasonNum = targetSeason.seasonNumber
                epNum = targetEp?.episodeNumber ?: 1
                streamUrl = targetEp?.videoUrl ?: SampleData.seriesList.first().seasons.first().episodes.first().videoUrl
            } else {
                // Movie
                episodeToPlay = null
                seasonNum = 1
                epNum = 1
                streamUrl = if (content.directVideoUrl.isNotBlank()) content.directVideoUrl else SampleData.seriesList.first().seasons.first().episodes.first().videoUrl
            }

            // Check downloads first (Offline First)
            val downloaded = withContext(Dispatchers.IO) {
                database.downloadDao().getDownload(content.id, seasonNum, epNum)
            }
            val playbackUrl = if (downloaded?.isDownloaded == true && downloaded.filePath.isNotBlank()) {
                downloaded.filePath
            } else {
                streamUrl
            }

            // Check history auto-resume timestamp
            val resumeMs: Long = if (forceResumeTimestampMs != null) {
                forceResumeTimestampMs
            } else {
                val history = withContext(Dispatchers.IO) {
                    if (content.type == ContentType.SERIES) {
                        database.watchHistoryDao().getForEpisode(content.id, seasonNum, epNum)
                            ?: database.watchHistoryDao().getLatestForContent(content.id)
                    } else {
                        database.watchHistoryDao().getLatestForContent(content.id)
                    }
                }
                if (history != null && !history.isCompleted && history.lastPositionMs > 2000L) {
                    history.lastPositionMs
                } else {
                    0L
                }
            }

            // Prepare next episode reference for countdown
            val nextEp = findNextEpisode(content, seasonNum, epNum)

            _playerState.update {
                it.copy(
                    activeContent = content,
                    activeEpisode = episodeToPlay,
                    activeSeasonNumber = seasonNum,
                    activeEpisodeNumber = epNum,
                    nextEpisode = nextEp,
                    showNextCountdown = false,
                    isEnded = false
                )
            }

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(playbackUrl))
                .setMediaId("${content.id}_s${seasonNum}_e${epNum}")
                .build()

            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            if (resumeMs > 0) {
                exoPlayer.seekTo(resumeMs)
            }
            exoPlayer.play()
        }
    }

    private fun findNextEpisode(content: ContentItem, currentSeason: Int, currentEp: Int): Episode? {
        if (content.type != ContentType.SERIES) return null
        val currentSeasonObj = content.seasons.find { it.seasonNumber == currentSeason } ?: return null
        val nextInSeason = currentSeasonObj.episodes.find { it.episodeNumber == currentEp + 1 }
        if (nextInSeason != null) return nextInSeason
        // Check next season Ep 1
        val nextSeasonObj = content.seasons.find { it.seasonNumber == currentSeason + 1 }
        return nextSeasonObj?.episodes?.firstOrNull()
    }

    fun playNextEpisode(autoPlay: Boolean = true) {
        val currentContent = _playerState.value.activeContent ?: return
        val nextEp = _playerState.value.nextEpisode
        if (nextEp != null) {
            playContent(
                content = currentContent,
                requestedSeason = nextEp.seasonNumber,
                requestedEpisode = nextEp.episodeNumber
            )
        }
    }

    fun playPreviousEpisode() {
        val currentContent = _playerState.value.activeContent ?: return
        if (currentContent.type != ContentType.SERIES) return
        val currentSeason = _playerState.value.activeSeasonNumber
        val currentEp = _playerState.value.activeEpisodeNumber

        val seasonObj = currentContent.seasons.find { it.seasonNumber == currentSeason } ?: return
        val prevInSeason = seasonObj.episodes.find { it.episodeNumber == currentEp - 1 }
        if (prevInSeason != null) {
            playContent(currentContent, currentSeason, prevInSeason.episodeNumber)
        } else if (currentSeason > 1) {
            val prevSeasonObj = currentContent.seasons.find { it.seasonNumber == currentSeason - 1 }
            val lastEpInPrevSeason = prevSeasonObj?.episodes?.lastOrNull()
            if (lastEpInPrevSeason != null) {
                playContent(currentContent, prevSeasonObj.seasonNumber, lastEpInPrevSeason.episodeNumber)
            }
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            showControlsPermanently()
        } else {
            exoPlayer.play()
            scheduleHideControls()
        }
    }

    fun seekTo(positionMs: Long) {
        val total = exoPlayer.duration.coerceAtLeast(0L)
        val clamped = positionMs.coerceIn(0L, total)
        exoPlayer.seekTo(clamped)
        _playerState.update { it.copy(currentPositionMs = clamped) }
        scheduleHideControls()
    }

    fun seekRelative(deltaMs: Long) {
        val newPos = (exoPlayer.currentPosition + deltaMs).coerceAtLeast(0L)
        seekTo(newPos)
        // Trigger double tap visual feedback bubble
        val side = if (deltaMs > 0) "right" else "left"
        val seconds = (deltaMs / 1000).toInt()
        _playerState.update {
            it.copy(
                doubleTapSeekSide = side,
                doubleTapSeekDelta = seconds
            )
        }
        hideDoubleTapJob?.cancel()
        hideDoubleTapJob = applicationScope.launch {
            delay(900)
            _playerState.update { it.copy(doubleTapSeekSide = null, doubleTapSeekDelta = 0) }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.playbackParameters = PlaybackParameters(speed)
        _playerState.update { it.copy(playbackSpeed = speed) }
    }

    fun toggleResizeMode() {
        val newMode = if (_playerState.value.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        } else {
            AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        _playerState.update { it.copy(resizeMode = newMode) }
    }

    fun setLockMode(locked: Boolean) {
        _playerState.update { it.copy(isLocked = locked) }
    }

    fun setQuality(qualityId: String) {
        _playerState.update { it.copy(selectedQuality = qualityId) }
    }

    fun setAudioTrack(trackId: String) {
        _playerState.update { it.copy(selectedAudioTrack = trackId) }
    }

    fun setSubtitle(subtitleId: String) {
        _playerState.update { it.copy(selectedSubtitle = subtitleId) }
    }

    fun toggleControlsVisibility() {
        if (_playerState.value.isLocked) return
        val current = _playerState.value.areControlsVisible
        if (current) {
            _playerState.update { it.copy(areControlsVisible = false) }
        } else {
            _playerState.update { it.copy(areControlsVisible = true) }
            scheduleHideControls()
        }
    }

    fun scheduleHideControls() {
        hideControlsJob?.cancel()
        hideControlsJob = applicationScope.launch {
            delay(3500)
            if (exoPlayer.isPlaying && !_playerState.value.isLocked) {
                _playerState.update { it.copy(areControlsVisible = false) }
            }
        }
    }

    private fun showControlsPermanently() {
        hideControlsJob?.cancel()
        _playerState.update { it.copy(areControlsVisible = true) }
    }

    // Touch Gestures: Volume control
    fun adjustVolumeByDelta(deltaPct: Int) {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val currentPct = (curVol * 100) / maxVol
        val targetPct = (currentPct + deltaPct).coerceIn(0, 100)
        val targetStreamVol = (targetPct * maxVol) / 100

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetStreamVol, 0)
        _playerState.update { it.copy(volumePercent = targetPct, showVolumeHud = true) }

        hideVolumeJob?.cancel()
        hideVolumeJob = applicationScope.launch {
            delay(1500)
            _playerState.update { it.copy(showVolumeHud = false) }
        }
    }

    // Touch Gestures: Brightness control
    fun adjustBrightnessByDelta(activity: Activity, deltaPct: Int) {
        val window = activity.window
        val lp = window.attributes
        var current = lp.screenBrightness
        if (current < 0f) current = 0.5f // Default system brightness
        val currentPct = (current * 100).toInt()
        val targetPct = (currentPct + deltaPct).coerceIn(5, 100)
        lp.screenBrightness = targetPct / 100f
        window.attributes = lp

        _playerState.update { it.copy(brightnessPercent = targetPct, showBrightnessHud = true) }

        hideBrightnessJob?.cancel()
        hideBrightnessJob = applicationScope.launch {
            delay(1500)
            _playerState.update { it.copy(showBrightnessHud = false) }
        }
    }

    // Touch Gestures: Horizontal Seek Preview
    fun updateSeekPreview(previewMs: Long?) {
        _playerState.update { it.copy(seekPreviewMs = previewMs) }
    }

    fun cancelNextEpisodeCountdown() {
        _playerState.update { it.copy(showNextCountdown = false) }
    }

    private fun startProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = applicationScope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition
                    val duration = exoPlayer.duration.coerceAtLeast(0L)
                    val buffered = exoPlayer.bufferedPosition

                    // Check countdown in last 10 seconds
                    val hasNext = _playerState.value.nextEpisode != null
                    val remainingMs = duration - pos
                    val isCountdownRange = hasNext && duration > 20000L && remainingMs in 1000L..10000L

                    val countdownSec = (remainingMs / 1000).toInt().coerceAtLeast(1)

                    _playerState.update {
                        it.copy(
                            currentPositionMs = pos,
                            totalDurationMs = duration,
                            bufferedPositionMs = buffered,
                            showNextCountdown = if (it.showNextCountdown && !hasNext) false else (isCountdownRange || (it.showNextCountdown && remainingMs > 0)),
                            countdownRemainingSeconds = countdownSec
                        )
                    }

                    // Periodically persist watch progress every 5 seconds
                    if (pos % 5000 < 600) {
                        saveCurrentPositionToDb()
                    }
                }
                delay(500)
            }
        }
    }

    private fun saveCurrentPositionToDb(isCompleted: Boolean = false) {
        val state = _playerState.value
        val content = state.activeContent ?: return
        val pos = exoPlayer.currentPosition
        val duration = exoPlayer.duration.coerceAtLeast(0L)

        applicationScope.launch(Dispatchers.IO) {
            database.watchHistoryDao().upsertHistory(
                WatchHistoryEntity(
                    contentId = content.id,
                    seasonNumber = state.activeSeasonNumber,
                    episodeNumber = state.activeEpisodeNumber,
                    lastPositionMs = pos,
                    totalDurationMs = duration,
                    isCompleted = isCompleted || (duration > 0 && pos >= duration - 3000L),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun release() {
        saveCurrentPositionToDb()
        progressTrackerJob?.cancel()
        hideControlsJob?.cancel()
        exoPlayer.release()
    }

    companion object {
        @Volatile
        private var INSTANCE: StreamXPlayerManager? = null

        fun getInstance(context: Context): StreamXPlayerManager {
            return INSTANCE ?: synchronized(this) {
                val instance = StreamXPlayerManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
