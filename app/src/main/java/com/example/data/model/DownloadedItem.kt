package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_items")
data class DownloadedItem(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val originalUrl: String,
  val canonicalUrl: String,
  val title: String,
  val author: String,
  val localUri: String,
  val filePath: String,
  val fileSize: Long,
  val durationSeconds: Long = 0,
  val isVideo: Boolean = true,
  val mimeType: String = "video/mp4",
  val thumbnailUrl: String? = null,
  val downloadTimestamp: Long = System.currentTimeMillis()
)
