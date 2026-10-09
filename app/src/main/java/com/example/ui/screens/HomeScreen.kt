package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContentType
import com.example.data.model.SampleData
import com.example.ui.components.ContentPosterCard
import com.example.ui.components.ContentRowSectionShimmer
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.HeroBanner
import com.example.ui.components.HomeScreenShimmerContent
import kotlinx.coroutines.delay
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.StreamXViewModel

@Composable
fun HomeScreen(
    viewModel: StreamXViewModel,
    modifier: Modifier = Modifier
) {
    val isLoadingContent by viewModel.isLoadingContent.collectAsState()
    val watchlistIds by viewModel.watchlistIds.collectAsState()
    val watchHistory by viewModel.watchHistory.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }
    var isCategoryLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCategory) {
        if (!isLoadingContent) {
            isCategoryLoading = true
            delay(240)
            isCategoryLoading = false
        }
    }

    val allContent = SampleData.seriesList
    val featuredItem = allContent.firstOrNull { it.isFeatured } ?: allContent.first()

    val filteredContent = when (selectedCategory) {
        "Series" -> allContent.filter { it.type == ContentType.SERIES }
        "Movies" -> allContent.filter { it.type == ContentType.MOVIE }
        "Sci-Fi" -> allContent.filter { it.genres.contains("Sci-Fi") }
        "Action" -> allContent.filter { it.genres.contains("Action") }
        else -> allContent
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("home_screen")
    ) {
        // StreamX Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // StreamX red logo badge
                Surface(
                    color = CrimsonRed,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "X",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "STREAMX",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.refreshContent() },
                    modifier = Modifier.testTag("refresh_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh content",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = { viewModel.selectTab(NavigationTab.SEARCH) }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Category Pills
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            val categories = listOf("All", "Series", "Movies", "Sci-Fi", "Action")
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) CrimsonRed else ObsidianCard,
                    border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)) else null,
                    modifier = Modifier
                        .clickable { selectedCategory = cat }
                        .testTag("category_pill_$cat")
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Shimmer Loading State while fetching from Room Database
        if (isLoadingContent) {
            HomeScreenShimmerContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            // Main Scroll Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Hero Banner
                item {
                    HeroBanner(
                        content = featuredItem,
                        isWatchlisted = watchlistIds.contains(featuredItem.id),
                        onPlayClick = { viewModel.openContentDetail(featuredItem) },
                        onWatchlistClick = { viewModel.toggleWatchlist(featuredItem.id) }
                    )
                }

                // Continue Watching section (if watch history exists)
                if (watchHistory.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(
                                text = "Continue Watching",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(watchHistory, key = { it.id }) { history ->
                                    val matchedContent = allContent.find { it.id == history.contentId }
                                    if (matchedContent != null) {
                                        val progressFraction = if (history.totalDurationMs > 0) {
                                            history.lastPositionMs.toFloat() / history.totalDurationMs.toFloat()
                                        } else {
                                            0.25f
                                        }
                                        ContinueWatchingCard(
                                            content = matchedContent,
                                            progressFraction = progressFraction,
                                            seasonNumber = history.seasonNumber,
                                            episodeNumber = history.episodeNumber,
                                            onClick = {
                                                viewModel.openContentDetail(
                                                    content = matchedContent,
                                                    season = history.seasonNumber,
                                                    episode = history.episodeNumber
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isCategoryLoading) {
                    items(3) {
                        ContentRowSectionShimmer()
                    }
                } else {
                    // Trending Now
                    item {
                        ContentRowSection(
                            title = "Trending Now",
                            items = filteredContent,
                            onItemClick = { viewModel.openContentDetail(it) }
                        )
                    }

                    // Top 10 Series
                    item {
                        ContentRowSection(
                            title = "Top 10 Today",
                            items = filteredContent.filter { it.type == ContentType.SERIES },
                            onItemClick = { viewModel.openContentDetail(it) }
                        )
                    }

                    // Sci-Fi & Cyberpunk Hits
                    item {
                        ContentRowSection(
                            title = "Sci-Fi & Cyberpunk",
                            items = allContent.filter { it.genres.contains("Sci-Fi") },
                            onItemClick = { viewModel.openContentDetail(it) }
                        )
                    }

                    // Action & Thrillers
                    item {
                        ContentRowSection(
                            title = "High Octane Action",
                            items = allContent.filter { it.genres.contains("Action") },
                            onItemClick = { viewModel.openContentDetail(it) }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun ContentRowSection(
    title: String,
    items: List<com.example.data.model.ContentItem>,
    onItemClick: (com.example.data.model.ContentItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { item ->
                ContentPosterCard(
                    content = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}
