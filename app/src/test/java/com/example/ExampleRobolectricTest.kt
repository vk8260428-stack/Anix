package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DownloadEntity
import com.example.data.local.StreamXDatabase
import com.example.data.local.WatchHistoryEntity
import com.example.data.local.WatchlistEntity
import com.example.data.model.SampleData
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: StreamXDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, StreamXDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `verify app name string resource`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("StreamX", appName)
    }

    @Test
    fun `verify watch history auto-resume persistence`() = runBlocking {
        val historyDao = db.watchHistoryDao()
        val entry = WatchHistoryEntity(
            contentId = "streamx-s1",
            seasonNumber = 1,
            episodeNumber = 2,
            lastPositionMs = 145000L,
            totalDurationMs = 600000L,
            isCompleted = false
        )
        historyDao.upsertHistory(entry)

        val retrieved = historyDao.getLatestForContent("streamx-s1")
        assertNotNull(retrieved)
        assertEquals(1, retrieved?.seasonNumber)
        assertEquals(2, retrieved?.episodeNumber)
        assertEquals(145000L, retrieved?.lastPositionMs)
    }

    @Test
    fun `verify offline first download management`() = runBlocking {
        val downloadDao = db.downloadDao()
        val download = DownloadEntity(
            downloadId = "streamx-s1_s1_e1",
            contentId = "streamx-s1",
            seasonNumber = 1,
            episodeNumber = 1,
            filePath = "file:///data/user/0/com.aistudio.streamx.ottapp/files/s1e1.mp4",
            fileSizeBytes = 250_000_000L,
            isDownloaded = true
        )
        downloadDao.insert(download)

        val retrieved = downloadDao.getDownload("streamx-s1", 1, 1)
        assertNotNull(retrieved)
        assertTrue(retrieved?.isDownloaded == true)
        assertEquals("file:///data/user/0/com.aistudio.streamx.ottapp/files/s1e1.mp4", retrieved?.filePath)
    }

    @Test
    fun `verify content sample catalog items`() {
        assertTrue(SampleData.seriesList.isNotEmpty())
        val firstSeries = SampleData.seriesList.first()
        assertTrue(firstSeries.seasons.isNotEmpty())
        assertTrue(firstSeries.seasons.first().episodes.isNotEmpty())
        assertNotNull(firstSeries.seasons.first().episodes.first().videoUrl)
    }
}
