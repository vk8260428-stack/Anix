package com.example.data.model

data class ContentItem(
    val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val backdropUrl: String,
    val type: ContentType,
    val rating: String = "8.8",
    val releaseYear: Int = 2024,
    val ageRating: String = "16+",
    val quality: String = "4K UHD",
    val audioFormat: String = "Dolby Atmos 5.1",
    val genres: List<String> = listOf("Action", "Sci-Fi", "Drama"),
    val seasons: List<Season> = emptyList(),
    val directVideoUrl: String = "", // Used if type == MOVIE
    val isFeatured: Boolean = false
)

enum class ContentType {
    SERIES, MOVIE
}

data class Season(
    val seasonNumber: Int,
    val title: String,
    val episodes: List<Episode>
)

data class Episode(
    val id: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val synopsis: String,
    val durationMinutes: Int,
    val durationMs: Long,
    val thumbnailUrl: String,
    val videoUrl: String
)

data class VideoQualityOption(
    val id: String,
    val label: String,
    val resolution: String,
    val isAuto: Boolean = false
)

data class AudioTrackOption(
    val id: String,
    val name: String,
    val language: String
)

data class SubtitleOption(
    val id: String,
    val name: String,
    val language: String
)
