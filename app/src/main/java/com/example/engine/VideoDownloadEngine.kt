package com.example.engine

import android.content.Context
import java.io.File

interface VideoDownloadEngine {
  val engineName: String
  val isNativeOnDevice: Boolean

  /**
   * Extracts direct video stream metadata directly from user's device connection.
   */
  suspend fun extractMetadata(url: String): Result<VideoMetadata>

  /**
   * Downloads media stream chunk-by-chunk with real-time byte tracking and cancellation support.
   */
  suspend fun downloadMedia(
    context: Context,
    metadata: VideoMetadata,
    isVideo: Boolean,
    onProgress: (DownloadProgress) -> Unit
  ): Result<VerifiedFileResult>
}

data class VerifiedFileResult(
  val uriString: String,
  val filePath: String,
  val fileSizeBytes: Long,
  val mimeType: String,
  val isVideo: Boolean
)
