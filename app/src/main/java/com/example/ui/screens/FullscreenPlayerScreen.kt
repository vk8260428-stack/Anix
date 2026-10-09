package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.components.StreamXPlayerView
import com.example.ui.theme.ObsidianBlack
import com.example.ui.viewmodel.StreamXViewModel

@Composable
fun FullscreenPlayerScreen(
    viewModel: StreamXViewModel,
    onExitFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onExitFullscreen()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("fullscreen_player_screen")
    ) {
        StreamXPlayerView(
            playerManager = viewModel.playerManager,
            onBackClick = onExitFullscreen,
            isFullscreen = true,
            onToggleFullscreen = onExitFullscreen,
            modifier = Modifier.fillMaxSize()
        )
    }
}
