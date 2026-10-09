package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DefaultDownloaderFactory
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@OptIn(UnstableApi::class)
object StreamXDownloadModule {

    private const val DOWNLOAD_DIR_NAME = "streamx_downloads"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 StreamX/1.0"

    @Volatile
    private var databaseProvider: DatabaseProvider? = null

    @Volatile
    private var downloadCache: SimpleCache? = null

    @Volatile
    private var httpDataSourceFactory: DefaultHttpDataSource.Factory? = null

    @Volatile
    private var upstreamDataSourceFactory: DataSource.Factory? = null

    @Volatile
    private var cacheDataSourceFactory: CacheDataSource.Factory? = null

    @Volatile
    private var downloadManager: DownloadManager? = null

    private val downloadExecutor: Executor = Executors.newFixedThreadPool(4)

    @Synchronized
    fun getDatabaseProvider(context: Context): DatabaseProvider {
        return databaseProvider ?: StandaloneDatabaseProvider(context.applicationContext).also {
            databaseProvider = it
        }
    }

    @Synchronized
    fun getDownloadCache(context: Context): SimpleCache {
        return downloadCache ?: run {
            val downloadDir = File(
                context.applicationContext.getExternalFilesDir(null) ?: context.applicationContext.filesDir,
                DOWNLOAD_DIR_NAME
            )
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            val cache = SimpleCache(
                downloadDir,
                NoOpCacheEvictor(),
                getDatabaseProvider(context)
            )
            downloadCache = cache
            cache
        }
    }

    @Synchronized
    fun getHttpDataSourceFactory(): DefaultHttpDataSource.Factory {
        return httpDataSourceFactory ?: DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .also { httpDataSourceFactory = it }
    }

    @Synchronized
    fun getUpstreamDataSourceFactory(context: Context): DataSource.Factory {
        return upstreamDataSourceFactory ?: DefaultDataSource.Factory(
            context.applicationContext,
            getHttpDataSourceFactory()
        ).also { upstreamDataSourceFactory = it }
    }

    @Synchronized
    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        return cacheDataSourceFactory ?: run {
            val factory = CacheDataSource.Factory()
                .setCache(getDownloadCache(context))
                .setUpstreamDataSourceFactory(getUpstreamDataSourceFactory(context))
                .setCacheWriteDataSinkFactory(null)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            cacheDataSourceFactory = factory
            factory
        }
    }

    @Synchronized
    fun getDownloadManager(context: Context): DownloadManager {
        return downloadManager ?: run {
            val downloaderFactory = DefaultDownloaderFactory(
                getCacheDataSourceFactory(context),
                downloadExecutor
            )
            val manager = DownloadManager(
                context.applicationContext,
                getDatabaseProvider(context),
                getDownloadCache(context),
                getUpstreamDataSourceFactory(context),
                downloadExecutor
            ).apply {
                maxParallelDownloads = 3
                resumeDownloads()
            }
            downloadManager = manager
            manager
        }
    }

    fun buildDownloadRequest(
        downloadId: String,
        uri: Uri,
        contentId: String
    ): DownloadRequest {
        val uriStr = uri.toString()
        val mimeType = if (uriStr.contains(".m3u8", ignoreCase = true)) {
            MimeTypes.APPLICATION_M3U8
        } else {
            MimeTypes.VIDEO_MP4
        }
        return DownloadRequest.Builder(downloadId, uri)
            .setMimeType(mimeType)
            .setData(contentId.toByteArray())
            .build()
    }
}
