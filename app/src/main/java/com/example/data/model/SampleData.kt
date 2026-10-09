package com.example.data.model

object SampleData {
    // Verified 100% accessible public video stream URLs
    const val DEFAULT_FALLBACK_STREAM =
        "https://vjs.zencdn.net/v/oceans.mp4"
    private const val STREAM_OCEANS =
        "https://vjs.zencdn.net/v/oceans.mp4"
    private const val STREAM_BIG_BUCK_BUNNY =
        "https://media.w3.org/2010/05/bunny/movie.mp4"
    private const val STREAM_SINTEL =
        "https://media.w3.org/2010/05/sintel/trailer.mp4"
    private const val STREAM_BLUE_MOON =
        "https://cdn.plyr.io/static/demo/View_From_A_Blue_Moon_Trailer-720p.mp4"
    private const val STREAM_ELEPHANTS_DREAM =
        "https://archive.org/download/ElephantsDream/ed_1024_512kb.mp4"
    private const val STREAM_BBB_ARCHIVE =
        "https://archive.org/download/BigBuckBunny_328/BigBuckBunny_512kb.mp4"

    val seriesList = listOf(
        ContentItem(
            id = "streamx-s1",
            title = "CYBERPUNK: PROTOCOL ZERO",
            description = "In the subterranean layers of Neo-Tokyo, a disgraced neural detective uncovers a syndicate synthesizing artificial consciousness, threatening the boundary between human instinct and autonomous machine logic.",
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=1200&auto=format&fit=crop&q=80",
            type = ContentType.SERIES,
            rating = "9.4",
            releaseYear = 2024,
            ageRating = "18+",
            quality = "4K UHD HDR",
            audioFormat = "Dolby Atmos 7.1",
            genres = listOf("Sci-Fi", "Cyberpunk", "Thriller", "Action"),
            isFeatured = true,
            seasons = listOf(
                Season(
                    seasonNumber = 1,
                    title = "Season 1: Awakening",
                    episodes = listOf(
                        Episode(
                            id = "s1-e1",
                            seasonNumber = 1,
                            episodeNumber = 1,
                            title = "The Ghost Circuit",
                            synopsis = "A memory broker's terminal explodes in Shinjuku, leaving behind an encrypted cybernetic core labeled Protocol Zero.",
                            durationMinutes = 12,
                            durationMs = 12 * 60 * 1000L,
                            thumbnailUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=500&auto=format&fit=crop&q=80",
                            videoUrl = STREAM_OCEANS
                        ),
                        Episode(
                            id = "s1-e2",
                            seasonNumber = 1,
                            episodeNumber = 2,
                            title = "Neon Labyrinth",
                            synopsis = "Pursued by corporate strike drones, Jax retreats into the abandoned geothermal tunnels beneath district 9.",
                            durationMinutes = 10,
                            durationMs = 10 * 60 * 1000L,
                            thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500&auto=format&fit=crop&q=80",
                            videoUrl = STREAM_BIG_BUCK_BUNNY
                        ),
                        Episode(
                            id = "s1-e3",
                            seasonNumber = 1,
                            episodeNumber = 3,
                            title = "Neural Bleed",
                            synopsis = "A rogue bio-hacker attempts to decompile the synthetic memory cortex before system thermal overload.",
                            durationMinutes = 15,
                            durationMs = 15 * 60 * 1000L,
                            thumbnailUrl = "https://images.unsplash.com/photo-1515260268569-9271009adfdb?w=500&auto=format&fit=crop&q=80",
                            videoUrl = STREAM_SINTEL
                        ),
                        Episode(
                            id = "s1-e4",
                            seasonNumber = 1,
                            episodeNumber = 4,
                            title = "Override Protocol",
                            synopsis = "The server farm mainframe initiates an automated purge as Jax connects directly to the mainframe gateway.",
                            durationMinutes = 11,
                            durationMs = 11 * 60 * 1000L,
                            thumbnailUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&auto=format&fit=crop&q=80",
                            videoUrl = STREAM_BLUE_MOON
                        )
                    )
                ),
                Season(
                    seasonNumber = 2,
                    title = "Season 2: Convergence",
                    episodes = listOf(
                        Episode(
                            id = "s2-e1",
                            seasonNumber = 2,
                            episodeNumber = 1,
                            title = "Sub-Zero Rebirth",
                            synopsis = "Six months after the purge, residual neural echoes resurface inside orbital communications relays.",
                            durationMinutes = 13,
                            durationMs = 13 * 60 * 1000L,
                            thumbnailUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=500&auto=format&fit=crop&q=80",
                            videoUrl = STREAM_ELEPHANTS_DREAM
                        ),
                        Episode(
                            id = "s2-e2",
                            seasonNumber = 2,
                            episodeNumber = 2,
                            title = "Quantum Echo",
                            synopsis = "Jax and Naomi infiltrate the orbital station server room during an atmospheric solar flare blackout.",
                            durationMinutes = 14,
                            durationMs = 14 * 60 * 1000L,
                            thumbnailUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500&auto=format&fit=crop&q=80",
                            videoUrl = STREAM_BBB_ARCHIVE
                        )
                    )
                )
            )
        ),
        ContentItem(
            id = "streamx-s2",
            title = "SHADOW HORIZON",
            description = "A deep-space reconnaissance frigate investigates an ancient alien megastructure at the precipice of a supermassive black hole event horizon.",
            posterUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=600&auto=format&fit=crop&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1200&auto=format&fit=crop&q=80",
            type = ContentType.SERIES,
            rating = "9.1",
            releaseYear = 2024,
            ageRating = "16+",
            quality = "4K UHD IMAX",
            audioFormat = "Dolby Atmos 5.1",
            genres = listOf("Sci-Fi", "Mystery", "Space", "Drama"),
            isFeatured = true,
            seasons = listOf(
                Season(
                    seasonNumber = 1,
                    title = "Season 1: Deep Gravity",
                    episodes = listOf(
                        Episode(
                            id = "sh-s1-e1",
                            seasonNumber = 1,
                            episodeNumber = 1,
                            title = "The Singularity Gate",
                            synopsis = "Commander Vance receives telemetry from a vessel lost forty years prior inside the gravitational well.",
                            durationMinutes = 14,
                            durationMs = 14 * 60 * 1000L,
                            thumbnailUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=500&auto=format&fit=crop&q=80",
                            videoUrl = STREAM_SINTEL
                        ),
                        Episode(
                            id = "sh-s1-e2",
                            seasonNumber = 1,
                            episodeNumber = 2,
                            title = "Tidal Force",
                            synopsis = "Time dilation forces the crew to make impossible decisions as seconds on the surface equal months aboard the ship.",
                            durationMinutes = 11,
                            durationMs = 11 * 60 * 1000L,
                            thumbnailUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=500&auto=format&fit=crop&q=80",
                            videoUrl = STREAM_OCEANS
                        )
                    )
                )
            )
        ),
        ContentItem(
            id = "streamx-m1",
            title = "APEX CHASE: VELOCITY",
            description = "An undercover street racer must outrun high-tech paramilitary trackers across the neon coast in a prototype kinetic hypercar.",
            posterUrl = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?w=600&auto=format&fit=crop&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=1200&auto=format&fit=crop&q=80",
            type = ContentType.MOVIE,
            rating = "8.7",
            releaseYear = 2024,
            ageRating = "16+",
            quality = "4K HDR",
            audioFormat = "DTS:X Ultra",
            genres = listOf("Action", "Speed", "Crime", "Thriller"),
            directVideoUrl = STREAM_BLUE_MOON,
            isFeatured = true
        ),
        ContentItem(
            id = "streamx-m2",
            title = "THE FORGOTTEN DYNASTY",
            description = "Ancient martial masters defend the sacred jade archive from an emperor who seeks to rewrite the history of mortal warfare.",
            posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1200&auto=format&fit=crop&q=80",
            type = ContentType.MOVIE,
            rating = "8.9",
            releaseYear = 2023,
            ageRating = "13+",
            quality = "4K UHD",
            audioFormat = "Dolby 5.1",
            genres = listOf("Action", "Martial Arts", "Historical", "Epic"),
            directVideoUrl = STREAM_ELEPHANTS_DREAM
        ),
        ContentItem(
            id = "streamx-s3",
            title = "NIGHTFALL CHRONICLES",
            description = "Supernatural detectives confront mythical creatures emerging from the London underground shadows during total eclipses.",
            posterUrl = "https://images.unsplash.com/photo-1514533450685-4493e01d1fdc?w=600&auto=format&fit=crop&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200&auto=format&fit=crop&q=80",
            type = ContentType.SERIES,
            rating = "8.6",
            releaseYear = 2023,
            ageRating = "16+",
            quality = "1080p HD",
            audioFormat = "Stereo 2.0",
            genres = listOf("Horror", "Mystery", "Supernatural"),
            seasons = listOf(
                Season(
                    seasonNumber = 1,
                    title = "Season 1",
                    episodes = listOf(
                        Episode(
                            id = "nc-s1-e1",
                            seasonNumber = 1,
                            episodeNumber = 1,
                            title = "Blood Moon Rising",
                            synopsis = "The British Museum guards vanish as astronomical alignments trigger forgotten subterranean gates.",
                            durationMinutes = 12,
                            durationMs = 12 * 60 * 1000L,
                            thumbnailUrl = "https://images.unsplash.com/photo-1514533450685-4493e01d1fdc?w=500&auto=format&fit=crop&q=80",
                            videoUrl = STREAM_BIG_BUCK_BUNNY
                        )
                    )
                )
            )
        ),
        ContentItem(
            id = "streamx-m3",
            title = "GLACIAL REALM: ARCTIC EXPEDITION",
            description = "A climate exploration team uncovers preserved biological entities trapped beneath miles of Greenland ice sheets.",
            posterUrl = "https://images.unsplash.com/photo-1483921020237-2ff51e8e4b22?w=600&auto=format&fit=crop&q=80",
            backdropUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1200&auto=format&fit=crop&q=80",
            type = ContentType.MOVIE,
            rating = "8.5",
            releaseYear = 2024,
            ageRating = "13+",
            quality = "4K UHD",
            audioFormat = "Dolby Atmos 5.1",
            genres = listOf("Documentary", "Adventure", "Nature", "Thriller"),
            directVideoUrl = STREAM_OCEANS
        )
    )

    val qualities = listOf(
        VideoQualityOption("auto", "Auto (Dynamic)", "1080p/4K", isAuto = true),
        VideoQualityOption("4k", "4K Ultra HD", "2160p"),
        VideoQualityOption("1080p", "Full HD", "1080p"),
        VideoQualityOption("720p", "High Definition", "720p"),
        VideoQualityOption("480p", "Standard Definition", "480p"),
        VideoQualityOption("data_saver", "Data Saver", "360p")
    )

    val audioTracks = listOf(
        AudioTrackOption("en_atmos", "English [Original] (Dolby Atmos 5.1)", "en"),
        AudioTrackOption("en_desc", "English - Audio Description", "en"),
        AudioTrackOption("es_dub", "Español (Latinoamérica) (5.1)", "es"),
        AudioTrackOption("ja_dub", "Japanese (Original Cast)", "ja"),
        AudioTrackOption("fr_dub", "Français (5.1)", "fr"),
        AudioTrackOption("de_dub", "Deutsch (5.1)", "de")
    )

    val subtitleTracks = listOf(
        SubtitleOption("off", "Off", ""),
        SubtitleOption("en", "English [CC]", "en"),
        SubtitleOption("es", "Español", "es"),
        SubtitleOption("fr", "Français", "fr"),
        SubtitleOption("ja", "日本語", "ja"),
        SubtitleOption("de", "Deutsch", "de")
    )

    val playbackSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
}
