package com.example.data.repository

import android.content.Context
import com.example.data.database.DownloadDao
import com.example.data.model.DownloadedItem
import com.example.engine.DownloadProgress
import com.example.engine.DownloadStage
import com.example.engine.OnDeviceDownloadEngine
import com.example.engine.VideoDownloadEngine
import com.example.engine.VideoMetadata
import com.example.storage.MediaStorageManager
import kotlinx.coroutines.flow.Flow

class DownloadRepository(
  private val context: Context,
  private val downloadDao: DownloadDao,
  private val downloadEngine: VideoDownloadEngine = OnDeviceDownloadEngine()
) {

  val allDownloads: Flow<List<DownloadedItem>> = downloadDao.getAllDownloads()

  fun getDownloadsByType(isVideo: Boolean): Flow<List<DownloadedItem>> =
    downloadDao.getDownloadsByType(isVideo)

  suspend fun getDownloadById(id: Long): DownloadedItem? =
    downloadDao.getDownloadById(id)

  suspend fun extractMetadata(url: String): Result<VideoMetadata> {
    return downloadEngine.extractMetadata(url)
  }

  suspend fun downloadMedia(
    metadata: VideoMetadata,
    isVideo: Boolean,
    onProgress: (DownloadProgress) -> Unit
  ): Result<DownloadedItem> {
    val result = downloadEngine.downloadMedia(
      context = context,
      metadata = metadata,
      isVideo = isVideo,
      onProgress = onProgress
    )

    return result.mapCatching { verified ->
      val item = DownloadedItem(
        originalUrl = metadata.originalUrl,
        canonicalUrl = metadata.canonicalUrl,
        title = metadata.title,
        author = metadata.author,
        localUri = verified.uriString,
        filePath = verified.filePath,
        fileSize = verified.fileSizeBytes,
        durationSeconds = metadata.durationSeconds,
        isVideo = verified.isVideo,
        mimeType = verified.mimeType,
        thumbnailUrl = metadata.thumbnailUrl,
        downloadTimestamp = System.currentTimeMillis()
      )
      val id = downloadDao.insertDownload(item)
      item.copy(id = id)
    }
  }

  suspend fun deleteDownload(item: DownloadedItem): Boolean {
    val fileDeleted = MediaStorageManager.deleteMedia(context, item.localUri, item.filePath)
    downloadDao.deleteDownload(item)
    return fileDeleted
  }

  fun getActiveEngineName(): String = downloadEngine.engineName
}
