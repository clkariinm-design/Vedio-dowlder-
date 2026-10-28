package com.example.engine

data class VideoMetadata(
  val videoId: String,
  val originalUrl: String,
  val canonicalUrl: String,
  val title: String,
  val author: String,
  val videoStreamUrl: String,
  val audioStreamUrl: String? = null,
  val thumbnailUrl: String? = null,
  val durationSeconds: Long = 0,
  val estimatedSizeBytes: Long = 0
)

enum class DownloadStage {
  IDLE,
  EXTRACTING,
  DOWNLOADING,
  VERIFYING,
  COMPLETED,
  CANCELLED,
  FAILED
}

data class DownloadProgress(
  val stage: DownloadStage = DownloadStage.IDLE,
  val percent: Int = 0,
  val bytesDownloaded: Long = 0L,
  val totalBytes: Long = 0L,
  val speedBytesPerSec: Long = 0L,
  val etaSeconds: Long = 0L,
  val errorMessage: String? = null,
  val savedUriString: String? = null,
  val savedFilePath: String? = null
)
