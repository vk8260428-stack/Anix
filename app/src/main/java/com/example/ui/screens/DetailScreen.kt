package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbUpAlt
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContentItem
import com.example.data.model.ContentType
import com.example.data.model.SampleData
import com.example.ui.components.ContentPosterCard
import com.example.ui.components.ContentRowSectionShimmer
import com.example.ui.components.EpisodeItemCard
import com.example.ui.components.EpisodeItemCardShimmer
import com.example.ui.components.StreamXPlayerView
import kotlinx.coroutines.delay
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.StreamXViewModel

@Composable
fun DetailScreen(
    viewModel: StreamXViewModel,
    content: ContentItem,
    onBack: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    // BackHandler: minimize to mini player
    BackHandler {
        viewModel.minimizeToMiniPlayer()
    }

    val playerState by viewModel.playerManager.playerState.collectAsState()
    val watchlistIds by viewModel.watchlistIds.collectAsState()
    val isWatchlisted = watchlistIds.contains(content.id)
    val activeDownloads by viewModel.activeDownloadsInProgress.collectAsState()
    val allDownloads by viewModel.allDownloads.collectAsState()

    var selectedSeasonNumber by remember(content.id) {
        mutableIntStateOf(content.seasons.firstOrNull()?.seasonNumber ?: 1)
    }

    var isSeasonLoading by remember { mutableStateOf(false) }
    LaunchedEffect(selectedSeasonNumber) {
        isSeasonLoading = true
        delay(220)
        isSeasonLoading = false
    }

    var selectedDetailTab by remember { mutableIntStateOf(0) } // 0: Episodes, 1: More Like This, 2: Details
    var isLiked by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("detail_screen")
    ) {
        // FIXED 16:9 "HALF VIDEO PLAYER" PINNED AT TOP
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(ObsidianBlack)
                .testTag("sticky_half_video_player")
        ) {
            StreamXPlayerView(
                playerManager = viewModel.playerManager,
                onBackClick = onBack,
                isFullscreen = false,
                onToggleFullscreen = onToggleFullscreen,
                modifier = Modifier.fillMaxSize()
            )
        }

        // SCROLLABLE METADATA, SEASONS, EPISODES, RECOMMENDATIONS
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("detail_content_scroll")
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Title
                    Text(
                        text = content.title,
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.3.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    // Badges row: Year, Age rating, Rating, Quality, Audio, CC
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${content.releaseYear}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Surface(
                            color = ObsidianCard,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = content.ageRating,
                                color = TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            color = CrimsonRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = content.rating,
                                    color = TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            color = ObsidianCard,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = content.quality,
                                color = AccentCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            color = ObsidianCard,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "CC",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Action buttons row: Watchlist, Like, Share, Download Season
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActionIconButton(
                            icon = if (isWatchlisted) Icons.Default.Check else Icons.Outlined.Add,
                            label = if (isWatchlisted) "In List" else "My List",
                            tint = if (isWatchlisted) CrimsonRed else TextPrimary,
                            onClick = { viewModel.toggleWatchlist(content.id) }
                        )

                        ActionIconButton(
                            icon = Icons.Default.ThumbUp,
                            label = if (isLiked) "Liked" else "Rate",
                            tint = if (isLiked) AccentCyan else TextPrimary,
                            onClick = { isLiked = !isLiked }
                        )

                        ActionIconButton(
                            icon = Icons.Default.Share,
                            label = "Share",
                            onClick = { /* Share intent */ }
                        )

                        ActionIconButton(
                            icon = Icons.Default.Download,
                            label = "Download",
                            onClick = {
                                if (content.type == ContentType.SERIES) {
                                    val currentSeasonObj = content.seasons.find { it.seasonNumber == selectedSeasonNumber }
                                    currentSeasonObj?.episodes?.firstOrNull()?.let { ep ->
                                        viewModel.downloadEpisode(content, selectedSeasonNumber, ep.episodeNumber)
                                    }
                                } else {
                                    viewModel.downloadEpisode(content, 1, 1)
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Description synopsis
                    Text(
                        text = content.description,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    // Genres
                    Text(
                        text = "Genres: ${content.genres.joinToString(", ")}",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Audio: ${content.audioFormat}",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Tab Navigation: Episodes / Recommendations / Details
            item {
                TabRow(
                    selectedTabIndex = selectedDetailTab,
                    containerColor = ObsidianSurface,
                    contentColor = TextPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedDetailTab]),
                            color = CrimsonRed,
                            height = 3.dp
                        )
                    }
                ) {
                    if (content.type == ContentType.SERIES) {
                        Tab(
                            selected = selectedDetailTab == 0,
                            onClick = { selectedDetailTab = 0 },
                            text = {
                                Text(
                                    "EPISODES",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedDetailTab == 0) TextPrimary else TextSecondary
                                )
                            }
                        )
                    }
                    Tab(
                        selected = selectedDetailTab == 1,
                        onClick = { selectedDetailTab = 1 },
                        text = {
                            Text(
                                "MORE LIKE THIS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedDetailTab == 1) TextPrimary else TextSecondary
                            )
                        }
                    )
                    Tab(
                        selected = selectedDetailTab == 2,
                        onClick = { selectedDetailTab = 2 },
                        text = {
                            Text(
                                "ABOUT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedDetailTab == 2) TextPrimary else TextSecondary
                            )
                        }
                    )
                }
            }

            // Tab 0: Episodes List
            if (content.type == ContentType.SERIES && selectedDetailTab == 0) {
                // Season Selector
                item {
                    var showSeasonMenu by remember { mutableStateOf(false) }
                    val currentSeasonObj = content.seasons.find { it.seasonNumber == selectedSeasonNumber }
                        ?: content.seasons.firstOrNull()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Surface(
                            color = ObsidianElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showSeasonMenu = true }
                                .testTag("season_selector_dropdown")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentSeasonObj?.title ?: "Season 1",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "▼",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSeasonMenu,
                            onDismissRequest = { showSeasonMenu = false },
                            modifier = Modifier.background(ObsidianCard)
                        ) {
                            content.seasons.forEach { season ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${season.title} (${season.episodes.size} eps)",
                                            color = if (season.seasonNumber == selectedSeasonNumber) CrimsonRed else TextPrimary,
                                            fontWeight = if (season.seasonNumber == selectedSeasonNumber) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedSeasonNumber = season.seasonNumber
                                        showSeasonMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Episode cards
                val activeSeason = content.seasons.find { it.seasonNumber == selectedSeasonNumber }
                    ?: content.seasons.firstOrNull()
                val episodes = activeSeason?.episodes ?: emptyList()

                if (isSeasonLoading) {
                    items(4) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            EpisodeItemCardShimmer()
                        }
                    }
                } else {
                    items(episodes, key = { it.id }) { ep ->
                    val isCurrentlyPlaying = playerState.activeEpisode?.id == ep.id
                    val downloadKey = "${content.id}_s${selectedSeasonNumber}_e${ep.episodeNumber}"
                    val downloadProgress = activeDownloads[downloadKey]
                    val isDownloaded = allDownloads.any {
                        it.contentId == content.id && it.seasonNumber == selectedSeasonNumber && it.episodeNumber == ep.episodeNumber
                    }

                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        EpisodeItemCard(
                            episode = ep,
                            isPlaying = isCurrentlyPlaying,
                            isDownloaded = isDownloaded,
                            downloadProgress = downloadProgress,
                            onPlayClick = {
                                viewModel.playerManager.playContent(
                                    content = content,
                                    requestedSeason = selectedSeasonNumber,
                                    requestedEpisode = ep.episodeNumber
                                )
                            },
                            onDownloadClick = {
                                viewModel.downloadEpisode(content, selectedSeasonNumber, ep.episodeNumber)
                            }
                        )
                    }
                }
            }
        }

            // Tab 1: Recommendations
            if (selectedDetailTab == 1 || (content.type == ContentType.MOVIE && selectedDetailTab == 0)) {
                item {
                    val recommended = SampleData.seriesList.filter { it.id != content.id }
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            text = "Recommended For You",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recommended, key = { it.id }) { item ->
                                ContentPosterCard(
                                    content = item,
                                    onClick = {
                                        viewModel.openContentDetail(item)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Tab 2: About / Technical Specifications
            if (selectedDetailTab == 2) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Technical Specifications", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("• Video: 3840x2160p 4K UHD, HDR10+, Dolby Vision Profile 8", color = TextSecondary, fontSize = 12.sp)
                        Text("• Audio: Dolby Atmos 7.1, DTS-HD Master Audio, Spatial 3D Audio", color = TextSecondary, fontSize = 12.sp)
                        Text("• Subtitles: English [CC], Español, Français, 日本語, Deutsch", color = TextSecondary, fontSize = 12.sp)
                        Text("• Offline Storage: High Efficiency H.265 (HEVC) local container", color = TextSecondary, fontSize = 12.sp)
                        Text("• Studio: StreamX Originals Global Productions", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            item {
                Spacer(Modifier.height(50.dp))
            }
        }
    }
}

@Composable
private fun ActionIconButton(
    icon: ImageVector,
    label: String,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
