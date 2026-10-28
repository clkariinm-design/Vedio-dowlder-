package com.example.engine

import android.content.Context
import com.example.storage.MediaStorageManager
import com.example.storage.TargetDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class OnDeviceDownloadEngine(
  private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .followRedirects(true)
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .build()
) : VideoDownloadEngine {

  override val engineName: String = "On-Device Native Extractor (Pure Kotlin)"
  override val isNativeOnDevice: Boolean = true

  override suspend fun extractMetadata(url: String): Result<VideoMetadata> {
    return TikTokExtractor.extract(url)
  }

  override suspend fun downloadMedia(
    context: Context,
    metadata: VideoMetadata,
    isVideo: Boolean,
    onProgress: (DownloadProgress) -> Unit
  ): Result<VerifiedFileResult> = withContext(Dispatchers.IO) {
    var destination: TargetDestination? = null

    try {
      val streamUrl = if (!isVideo && !metadata.audioStreamUrl.isNullOrBlank()) {
        metadata.audioStreamUrl
      } else {
        metadata.videoStreamUrl
      }

      onProgress(
        DownloadProgress(
          stage = DownloadStage.DOWNLOADING,
          percent = 0,
          bytesDownloaded = 0L,
          totalBytes = 0L
        )
      )

      val request = Request.Builder()
        .url(streamUrl)
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
        .header("Referer", "https://www.tiktok.com/")
        .header("Range", "bytes=0-")
        .build()

      val response = okHttpClient.newCall(request).execute()
      if (!response.isSuccessful) {
        throw IOException("HTTP ${response.code}: Failed to connect to media stream server")
      }

      val responseBody = response.body ?: throw IOException("Empty response body from media server")
      val contentType = responseBody.contentType()?.toString()?.lowercase() ?: ""
      if (contentType.contains("text/html")) {
        throw IOException("Media server returned an HTML error or verification wall instead of video stream.")
      }

      val totalBytes = responseBody.contentLength().coerceAtLeast(0L)

      destination = MediaStorageManager.createMediaTarget(
        context = context,
        title = metadata.title,
        isVideo = isVideo
      )

      val outputStream = destination.outputStream
      val inputStream = responseBody.byteStream()
      val buffer = ByteArray(16 * 1024)
      var bytesDownloaded = 0L
      var lastReportTime = System.currentTimeMillis()
      var bytesAtLastReport = 0L

      inputStream.use { input ->
        outputStream.use { output ->
          var read: Int
          while (input.read(buffer).also { read = it } != -1) {
            coroutineContext.ensureActive()
            output.write(buffer, 0, read)
            bytesDownloaded += read

            val now = System.currentTimeMillis()
            val timeDiff = now - lastReportTime
            if (timeDiff >= 200 || bytesDownloaded == totalBytes) {
              val speedBytesPerSec = if (timeDiff > 0) {
                ((bytesDownloaded - bytesAtLastReport) * 1000) / timeDiff
              } else {
                0L
              }

              val percent = if (totalBytes > 0) {
                ((bytesDownloaded * 100) / totalBytes).toInt().coerceIn(0, 99)
              } else {
                ((bytesDownloaded / (1024 * 100)) % 100).toInt()
              }

              val remainingBytes = (totalBytes - bytesDownloaded).coerceAtLeast(0L)
              val etaSec = if (speedBytesPerSec > 0) remainingBytes / speedBytesPerSec else 0L

              onProgress(
                DownloadProgress(
                  stage = DownloadStage.DOWNLOADING,
                  percent = percent,
                  bytesDownloaded = bytesDownloaded,
                  totalBytes = totalBytes,
                  speedBytesPerSec = speedBytesPerSec,
                  etaSeconds = etaSec
                )
              )

              lastReportTime = now
              bytesAtLastReport = bytesDownloaded
            }
          }
          output.flush()
        }
      }

      // Step 3: Verifying file integrity on disk
      onProgress(
        DownloadProgress(
          stage = DownloadStage.VERIFYING,
          percent = 99,
          bytesDownloaded = bytesDownloaded,
          totalBytes = totalBytes
        )
      )

      val verification = MediaStorageManager.verifyIntegrity(context, destination)
      if (!verification.isValid) {
        MediaStorageManager.finalizeMediaTarget(context, destination, false)
        val errorMsg = "Verification failed: ${verification.reason}"
        onProgress(
          DownloadProgress(
            stage = DownloadStage.FAILED,
            errorMessage = errorMsg
          )
        )
        return@withContext Result.failure(IOException(errorMsg))
      }

      // Finalize and mark MediaStore entry visible
      MediaStorageManager.finalizeMediaTarget(context, destination, true)

      onProgress(
        DownloadProgress(
          stage = DownloadStage.COMPLETED,
          percent = 100,
          bytesDownloaded = bytesDownloaded,
          totalBytes = totalBytes,
          savedUriString = destination.uri.toString(),
          savedFilePath = destination.filePath
        )
      )

      Result.success(
        VerifiedFileResult(
          uriString = destination.uri.toString(),
          filePath = destination.filePath,
          fileSizeBytes = verification.sizeBytes,
          mimeType = destination.mimeType,
          isVideo = isVideo
        )
      )
    } catch (e: Exception) {
      destination?.let {
        MediaStorageManager.finalizeMediaTarget(context, it, false)
      }
      val isCancelled = e is kotlinx.coroutines.CancellationException
      val stage = if (isCancelled) DownloadStage.CANCELLED else DownloadStage.FAILED
      val errorMsg = if (isCancelled) "Cancelled by user" else (e.message ?: "Download failed")

      onProgress(
        DownloadProgress(
          stage = stage,
          errorMessage = errorMsg
        )
      )

      Result.failure(e)
    }
  }
}
