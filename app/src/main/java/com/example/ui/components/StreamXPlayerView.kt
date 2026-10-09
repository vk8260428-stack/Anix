package com.example.ui.components

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlin.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.SampleData
import com.example.player.StreamXPlayerManager
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PlayerScrimBottom
import com.example.ui.theme.PlayerScrimTop
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StreamXPlayerView(
    playerManager: StreamXPlayerManager,
    onBackClick: () -> Unit,
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val playerState by playerManager.playerState.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showLockedHint by remember { mutableStateOf(false) }

    // Scrubber drag local state
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }

    // Double tap ripple animations
    val leftRippleAlpha by animateFloatAsState(
        targetValue = if (playerState.doubleTapSeekSide == "left") 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "leftRipple"
    )
    val rightRippleAlpha by animateFloatAsState(
        targetValue = if (playerState.doubleTapSeekSide == "right") 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "rightRipple"
    )

    // Pinch-to-zoom gesture state
    var zoomScale by remember { mutableFloatStateOf(1f) }
    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        zoomScale *= zoomChange
        if (zoomScale > 1.25f && playerState.resizeMode != AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
            playerManager.toggleResizeMode()
            zoomScale = 1f
        } else if (zoomScale < 0.85f && playerState.resizeMode != AspectRatioFrameLayout.RESIZE_MODE_FIT) {
            playerManager.toggleResizeMode()
            zoomScale = 1f
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .background(ObsidianBlack)
            .testTag("streamx_player_container")
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight

        // 1. AndroidView PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    resizeMode = playerState.resizeMode
                    player = playerManager.exoPlayer
                }
            },
            update = { view ->
                view.resizeMode = playerState.resizeMode
            },
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformableState)
        )

        // 2. Gesture Handling Surface Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(playerState.isLocked) {
                    if (playerState.isLocked) {
                        detectTapGestures(
                            onTap = {
                                showLockedHint = true
                                coroutineScope.launch {
                                    delay(2500)
                                    showLockedHint = false
                                }
                            }
                        )
                    } else {
                        var dragType = "" // "vertical_brightness", "vertical_volume", "horizontal_seek"
                        var dragStartX = 0f
                        var dragInitialSeek = 0L

                        detectDragGestures(
                            onDragStart = { offset ->
                                dragStartX = offset.x
                                dragInitialSeek = playerState.currentPositionMs
                                val widthPx = size.width
                                val isLeftHalf = offset.x < (widthPx / 2f)

                                dragType = if (isLeftHalf) "vertical_brightness" else "vertical_volume"
                            },
                            onDragEnd = {
                                if (dragType == "horizontal_seek") {
                                    playerState.seekPreviewMs?.let { playerManager.seekTo(it) }
                                    playerManager.updateSeekPreview(null)
                                }
                                dragType = ""
                            },
                            onDragCancel = {
                                playerManager.updateSeekPreview(null)
                                dragType = ""
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val absX = abs(dragAmount.x)
                                val absY = abs(dragAmount.y)

                                // Decide horizontal scrub if drag is strongly horizontal
                                if (dragType != "horizontal_seek" && absX > absY * 2.5f && absX > 15f) {
                                    dragType = "horizontal_seek"
                                }

                                when (dragType) {
                                    "vertical_brightness" -> {
                                        val deltaPct = (-dragAmount.y / 8f).toInt()
                                        if (activity != null && deltaPct != 0) {
                                            playerManager.adjustBrightnessByDelta(activity, deltaPct)
                                        }
                                    }
                                    "vertical_volume" -> {
                                        val deltaPct = (-dragAmount.y / 8f).toInt()
                                        if (deltaPct != 0) {
                                            playerManager.adjustVolumeByDelta(deltaPct)
                                        }
                                    }
                                    "horizontal_seek" -> {
                                        val totalMs = playerState.totalDurationMs.coerceAtLeast(1L)
                                        val scrubDeltaMs = (dragAmount.x * 200).toLong()
                                        val preview = (dragInitialSeek + (change.position.x - dragStartX) * 150)
                                            .toLong()
                                            .coerceIn(0L, totalMs)
                                        playerManager.updateSeekPreview(preview)
                                    }
                                }
                            }
                        )
                    }
                }
                .pointerInput(playerState.isLocked) {
                    if (!playerState.isLocked) {
                        detectTapGestures(
                            onTap = {
                                playerManager.toggleControlsVisibility()
                            },
                            onDoubleTap = { offset ->
                                val widthPx = size.width
                                val isRightHalf = offset.x > (widthPx / 2f)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isRightHalf) {
                                    playerManager.seekRelative(+10_000L)
                                } else {
                                    playerManager.seekRelative(-10_000L)
                                }
                            }
                        )
                    }
                }
        )

        // 3. Double-Tap Animated Seek Ripples
        // Left -10s
        if (leftRippleAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            listOf(CrimsonRed.copy(alpha = 0.35f * leftRippleAlpha), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 10s",
                        tint = TextPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "-10 sec",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Right +10s
        if (rightRippleAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(Alignment.CenterEnd)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, CrimsonRed.copy(alpha = 0.35f * rightRippleAlpha))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Fast forward 10s",
                        tint = TextPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "+10 sec",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. Volume HUD Slider (Right side vertical)
        AnimatedVisibility(
            visible = playerState.showVolumeHud,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp)
        ) {
            Surface(
                color = ObsidianSurface.copy(alpha = 0.88f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val volIcon = when {
                        playerState.volumePercent == 0 -> Icons.AutoMirrored.Filled.VolumeMute
                        playerState.volumePercent < 50 -> Icons.AutoMirrored.Filled.VolumeDown
                        else -> Icons.AutoMirrored.Filled.VolumeUp
                    }
                    Icon(
                        imageVector = volIcon,
                        contentDescription = "Volume",
                        tint = AccentCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(playerState.volumePercent / 100f)
                                .align(Alignment.BottomCenter)
                                .background(AccentCyan)
                        )
                    }
                    Text(
                        text = "${playerState.volumePercent}%",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 5. Brightness HUD Slider (Left side vertical)
        AnimatedVisibility(
            visible = playerState.showBrightnessHud,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
        ) {
            Surface(
                color = ObsidianSurface.copy(alpha = 0.88f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BrightnessHigh,
                        contentDescription = "Brightness",
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(24.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(playerState.brightnessPercent / 100f)
                                .align(Alignment.BottomCenter)
                                .background(Color(0xFFFFB800))
                        )
                    }
                    Text(
                        text = "${playerState.brightnessPercent}%",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 6. Horizontal Seek Time Preview Chip
        if (playerState.seekPreviewMs != null) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = ObsidianSurface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRed)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatTime(playerState.seekPreviewMs ?: 0L),
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "of ${formatTime(playerState.totalDurationMs)}",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 7. Locked Hint Icon
        AnimatedVisibility(
            visible = showLockedHint,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = ObsidianSurface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .clickable {
                        playerManager.setLockMode(false)
                        showLockedHint = false
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock",
                        tint = CrimsonRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Screen Locked • Tap to Unlock",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 8. Auto-Play Next Episode Countdown Card (Floating bottom-right card)
        AnimatedVisibility(
            visible = playerState.showNextCountdown && playerState.nextEpisode != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = if (playerState.areControlsVisible) 80.dp else 20.dp, end = 16.dp)
        ) {
            val nextEp = playerState.nextEpisode
            Surface(
                color = ObsidianCard.copy(alpha = 0.95f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Next Episode in ${playerState.countdownRemainingSeconds}s",
                        color = AccentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "E${nextEp?.episodeNumber}: ${nextEp?.title}",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { playerManager.playNextEpisode() },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Play Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { playerManager.cancelNextEpisodeCountdown() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Cancel", fontSize = 12.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }

        // 9. OVERLAY CONTROLS (Top Bar, Center Controls, Bottom Bar)
        AnimatedVisibility(
            visible = playerState.areControlsVisible && !playerState.isLocked,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(PlayerScrimTop, Color.Transparent)
                            )
                        )
                )

                // Bottom gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, PlayerScrimBottom)
                            )
                        )
                )

                // TOP BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = playerState.activeContent?.title ?: "StreamX Player",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val epSubtitle = if (playerState.activeEpisode != null) {
                            "S${playerState.activeSeasonNumber} : E${playerState.activeEpisodeNumber} • ${playerState.activeEpisode?.title}"
                        } else {
                            playerState.activeContent?.quality ?: "4K UHD"
                        }
                        Text(
                            text = epSubtitle,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Aspect ratio switcher button (Fit vs Zoom)
                    IconButton(
                        onClick = { playerManager.toggleResizeMode() },
                        modifier = Modifier.testTag("aspect_ratio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Aspect ratio",
                            tint = if (playerState.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) AccentCyan else TextPrimary
                        )
                    }

                    // Audio & Subtitles
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("subtitles_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = "Audio and Subtitles",
                            tint = TextPrimary
                        )
                    }

                    // Lock button
                    IconButton(
                        onClick = { playerManager.setLockMode(true) },
                        modifier = Modifier.testTag("lock_player_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Lock controls",
                            tint = TextPrimary
                        )
                    }

                    // Settings gear icon
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("player_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Player settings",
                            tint = TextPrimary
                        )
                    }
                }

                // CENTER PLAYBACK CONTROLS
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Previous Episode (if series)
                    IconButton(
                        onClick = { playerManager.playPreviousEpisode() },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("prev_episode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous episode",
                            tint = TextPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Rewind 10s
                    IconButton(
                        onClick = { playerManager.seekRelative(-10_000L) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("rewind_10s_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = TextPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Main Center Play / Pause button
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(CrimsonRed.copy(alpha = 0.9f))
                            .clickable { playerManager.togglePlayPause() }
                            .testTag("player_center_play_pause"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (playerState.isBuffering) {
                            CircularProgressIndicator(
                                color = TextPrimary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                tint = TextPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // Forward 10s
                    IconButton(
                        onClick = { playerManager.seekRelative(+10_000L) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("forward_10s_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = TextPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Next Episode (if series)
                    IconButton(
                        onClick = { playerManager.playNextEpisode() },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("next_episode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next episode",
                            tint = TextPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // BOTTOM CONTROLS & TIMELINE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    val currentPos = if (isScrubbing) scrubPositionMs else playerState.currentPositionMs
                    val totalDuration = playerState.totalDurationMs.coerceAtLeast(1L)
                    val bufferedPos = playerState.bufferedPositionMs

                    // Draggable Scrubber with buffered progress layer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Buffered progress bar
                        LinearProgressIndicator(
                            progress = { (bufferedPos.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White.copy(alpha = 0.35f),
                            trackColor = Color.White.copy(alpha = 0.15f),
                        )

                        // Main active scrubber
                        Slider(
                            value = (currentPos.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f),
                            onValueChange = { frac ->
                                isScrubbing = true
                                scrubPositionMs = (frac * totalDuration).toLong()
                            },
                            onValueChangeFinished = {
                                isScrubbing = false
                                playerManager.seekTo(scrubPositionMs)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = CrimsonRed,
                                activeTrackColor = CrimsonRed,
                                inactiveTrackColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("player_scrubber_slider")
                        )
                    }

                    // Time display, Quality badge, Fullscreen button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatTime(currentPos)} / ${formatTime(totalDuration)}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(Modifier.weight(1f))

                        // Quality selector badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ObsidianCard,
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .clickable { showSettingsSheet = true }
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = playerState.selectedQuality.uppercase(),
                                color = AccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Fullscreen toggle button
                        IconButton(
                            onClick = onToggleFullscreen,
                            modifier = Modifier.testTag("fullscreen_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (isFullscreen) "Exit fullscreen" else "Fullscreen",
                                tint = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    // 10. Player Settings Modal BottomSheet
    if (showSettingsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState,
            containerColor = ObsidianSurface,
            contentColor = TextPrimary
        ) {
            PlayerSettingsSheetContent(
                playerManager = playerManager,
                playerState = playerState,
                onClose = {
                    coroutineScope.launch {
                        sheetState.hide()
                        showSettingsSheet = false
                    }
                }
            )
        }
    }
}

@Composable
private fun PlayerSettingsSheetContent(
    playerManager: StreamXPlayerManager,
    playerState: com.example.player.PlayerState,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Speed") } // "Speed", "Quality", "Audio", "Subtitles"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Playback Settings",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Speed", "Quality", "Audio", "Subtitles").forEach { tab ->
                val isSelected = selectedTab == tab
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) CrimsonRed else ObsidianCard,
                    modifier = Modifier.clickable { selectedTab = tab }
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        when (selectedTab) {
            "Speed" -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SampleData.playbackSpeeds.forEach { speed ->
                        val isCurrent = playerState.playbackSpeed == speed
                        SettingOptionRow(
                            title = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                            isSelected = isCurrent,
                            onClick = {
                                playerManager.setPlaybackSpeed(speed)
                                onClose()
                            }
                        )
                    }
                }
            }
            "Quality" -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SampleData.qualities.forEach { quality ->
                        val isCurrent = playerState.selectedQuality == quality.id
                        SettingOptionRow(
                            title = "${quality.label} (${quality.resolution})",
                            isSelected = isCurrent,
                            onClick = {
                                playerManager.setQuality(quality.id)
                                onClose()
                            }
                        )
                    }
                }
            }
            "Audio" -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SampleData.audioTracks.forEach { audio ->
                        val isCurrent = playerState.selectedAudioTrack == audio.id
                        SettingOptionRow(
                            title = audio.name,
                            isSelected = isCurrent,
                            onClick = {
                                playerManager.setAudioTrack(audio.id)
                                onClose()
                            }
                        )
                    }
                }
            }
            "Subtitles" -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SampleData.subtitleTracks.forEach { sub ->
                        val isCurrent = playerState.selectedSubtitle == sub.id
                        SettingOptionRow(
                            title = sub.name,
                            isSelected = isCurrent,
                            onClick = {
                                playerManager.setSubtitle(sub.id)
                                onClose()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingOptionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) CrimsonRed.copy(alpha = 0.2f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(CrimsonRed)
                )
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
