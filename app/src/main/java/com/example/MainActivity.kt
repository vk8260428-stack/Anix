package com.example

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.StreamXPlayerView
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.FullscreenPlayerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.StreamXViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: StreamXViewModel by viewModels()
    private var isInPipMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        unlockMaxRefreshRate()
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainContent(
                    viewModel = viewModel,
                    isInPip = isInPipMode,
                    onEnterPip = { enterPipIfSupported() }
                )
            }
        }
    }

    /**
     * Iterates through display.supportedModes to find the highest refresh rate available
     * on the hardware and applies it to the current Window using preferredDisplayModeId
     * to ensure the UI runs at 90Hz/120Hz for a fluid cinematic experience.
     */
    private fun unlockMaxRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            } ?: return

            val modes = currentDisplay.supportedModes ?: return
            if (modes.isEmpty()) return

            var highestRateMode: Display.Mode? = null
            var maxRefreshRate = 0f

            for (mode in modes) {
                if (mode.refreshRate > maxRefreshRate) {
                    maxRefreshRate = mode.refreshRate
                    highestRateMode = mode
                }
            }

            highestRateMode?.let { mode ->
                val layoutParams = window.attributes
                layoutParams.preferredDisplayModeId = mode.modeId
                window.attributes = layoutParams
            }
        }
    }

    private fun enterPipIfSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setAutoEnterEnabled(true)
                    }
                }
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (viewModel.playerManager.exoPlayer.isPlaying) {
            enterPipIfSupported()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }
}

@Composable
fun MainContent(
    viewModel: StreamXViewModel,
    isInPip: Boolean,
    onEnterPip: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isDetailOpen by viewModel.isDetailOpen.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val selectedContent by viewModel.selectedContent.collectAsState()
    val isMiniPlayerActive by viewModel.isMiniPlayerActive.collectAsState()

    // When in PiP mode, show ONLY the video player surface!
    if (isInPip) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            StreamXPlayerView(
                playerManager = viewModel.playerManager,
                onBackClick = {},
                isFullscreen = true,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    // Fullscreen video player mode
    if (isFullscreen) {
        FullscreenPlayerScreen(
            viewModel = viewModel,
            onExitFullscreen = { viewModel.setFullscreen(false) }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        containerColor = ObsidianBlack,
        bottomBar = {
            // Only show bottom navigation when detail screen is not full
            if (!isDetailOpen) {
                Column {
                    // In-app Floating Mini-Player docked above bottom bar
                    AnimatedVisibility(
                        visible = isMiniPlayerActive,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        MiniPlayerBar(
                            playerManager = viewModel.playerManager,
                            onExpand = { viewModel.expandMiniPlayer() },
                            onDismiss = { viewModel.dismissMiniPlayer() }
                        )
                    }

                    // Bottom Navigation Bar
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        contentColor = TextPrimary,
                        tonalElevation = 0.dp,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        NavigationBarItem(
                            selected = currentTab == NavigationTab.HOME,
                            onClick = { viewModel.selectTab(NavigationTab.HOME) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home", fontSize = 11.sp) },
                            colors = navigationBarColors(),
                            modifier = Modifier.testTag("nav_item_home")
                        )
                        NavigationBarItem(
                            selected = currentTab == NavigationTab.SEARCH,
                            onClick = { viewModel.selectTab(NavigationTab.SEARCH) },
                            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            label = { Text("Search", fontSize = 11.sp) },
                            colors = navigationBarColors(),
                            modifier = Modifier.testTag("nav_item_search")
                        )
                        NavigationBarItem(
                            selected = currentTab == NavigationTab.DOWNLOADS,
                            onClick = { viewModel.selectTab(NavigationTab.DOWNLOADS) },
                            icon = { Icon(Icons.Default.Download, contentDescription = "Downloads") },
                            label = { Text("Downloads", fontSize = 11.sp) },
                            colors = navigationBarColors(),
                            modifier = Modifier.testTag("nav_item_downloads")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isDetailOpen && selectedContent != null) {
                DetailScreen(
                    viewModel = viewModel,
                    content = selectedContent!!,
                    onBack = { viewModel.minimizeToMiniPlayer() },
                    onToggleFullscreen = { viewModel.toggleFullscreen() }
                )
            } else {
                when (currentTab) {
                    NavigationTab.HOME -> HomeScreen(viewModel = viewModel)
                    NavigationTab.SEARCH -> SearchScreen(viewModel = viewModel)
                    NavigationTab.DOWNLOADS -> DownloadsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun navigationBarColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = CrimsonRed,
    selectedTextColor = CrimsonRed,
    unselectedIconColor = TextMuted,
    unselectedTextColor = TextMuted,
    indicatorColor = Color.Transparent
)
