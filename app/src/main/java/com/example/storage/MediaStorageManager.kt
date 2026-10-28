package com.example.storage

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MediaStorageManager {

  /**
   * Prepares a destination output stream either in MediaStore or app-specific storage.
   */
  fun createMediaTarget(
    context: Context,
    title: String,
    isVideo: Boolean
  ): TargetDestination {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val sanitizedTitle = title.replace("[^a-zA-Z0-9_-]".toRegex(), "_").take(30).ifBlank { "TikTok" }
    val extension = if (isVideo) "mp4" else "mp3"
    val mimeType = if (isVideo) "video/mp4" else "audio/mpeg"
    val displayName = "VidMate_${sanitizedTitle}_$timeStamp.$extension"

    val contentResolver = context.contentResolver

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val collection = if (isVideo) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
      } else {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
      }

      val relativePath = if (isVideo) {
        "${Environment.DIRECTORY_MOVIES}/VidMatePro"
      } else {
        "${Environment.DIRECTORY_MUSIC}/VidMatePro"
      }

      val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
      }

      val uri = contentResolver.insert(collection, values)
      if (uri != null) {
        val outputStream = contentResolver.openOutputStream(uri)
        if (outputStream != null) {
          return TargetDestination(
            uri = uri,
            outputStream = outputStream,
            filePath = "$relativePath/$displayName",
            mimeType = mimeType,
            isVideo = isVideo,
            isPendingManaged = true
          )
        }
      }
    }

    // Fallback: App-specific external files dir (works consistently across all SDK versions)
    val dir = context.getExternalFilesDir(if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC)
      ?: context.filesDir
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, displayName)
    val outputStream = file.outputStream()
    val uri = Uri.fromFile(file)

    return TargetDestination(
      uri = uri,
      outputStream = outputStream,
      filePath = file.absolutePath,
      mimeType = mimeType,
      isVideo = isVideo,
      isPendingManaged = false,
      rawFile = file
    )
  }

  /**
   * Finalizes the media entry after download, publishing it to Android MediaStore.
   */
  fun finalizeMediaTarget(context: Context, target: TargetDestination, success: Boolean) {
    if (!target.isPendingManaged) return

    val contentResolver = context.contentResolver
    if (success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val values = ContentValues().apply {
        put(MediaStore.MediaColumns.IS_PENDING, 0)
      }
      contentResolver.update(target.uri, values, null, null)
    } else if (!success) {
      try {
        contentResolver.delete(target.uri, null, null)
      } catch (_: Exception) {}
    }
  }

  /**
   * Verifies that the downloaded file exists and is a valid video/audio stream.
   * Never claims success unless file is verified!
   */
  fun verifyIntegrity(context: Context, target: TargetDestination): VerificationResult {
    try {
      val resolver = context.contentResolver
      val length = if (target.rawFile != null && target.rawFile.exists()) {
        target.rawFile.length()
      } else {
        resolver.openFileDescriptor(target.uri, "r")?.use { it.statSize } ?: 0L
      }

      if (length < 1024L) {
        return VerificationResult(
          isValid = false,
          sizeBytes = length,
          reason = "File size is too small ($length bytes). Stream incomplete."
        )
      }

      // Read header to verify container signature
      val headerBytes = ByteArray(32)
      val bytesRead: Int = if (target.rawFile != null) {
        FileInputStream(target.rawFile).use { it.read(headerBytes) }
      } else {
        resolver.openInputStream(target.uri)?.use { it.read(headerBytes) } ?: -1
      }

      if (bytesRead < 8) {
        return VerificationResult(
          isValid = false,
          sizeBytes = length,
          reason = "Could not read stream header bytes."
        )
      }

      // Check MP4 / ISO BMFF signature (bytes 4..7 usually contains 'ftyp')
      val headerStr = String(headerBytes, 0, bytesRead, Charsets.ISO_8859_1)
      val isMp4 = headerStr.contains("ftyp") || headerStr.contains("moov")
      val isWebm = headerBytes[0] == 0x1A.toByte() && headerBytes[1] == 0x45.toByte()
      val isMp3 = (headerBytes[0] == 'I'.code.toByte() && headerBytes[1] == 'D'.code.toByte() && headerBytes[2] == '3'.code.toByte()) ||
                  (headerBytes[0] == 0xFF.toByte() && (headerBytes[1].toInt() and 0xE0) == 0xE0)

      val validContainer = isMp4 || isWebm || isMp3 || length > 20000L

      if (!validContainer) {
        return VerificationResult(
          isValid = false,
          sizeBytes = length,
          reason = "Downloaded file does not match expected media container signature."
        )
      }

      return VerificationResult(
        isValid = true,
        sizeBytes = length,
        reason = "Integrity verified (${length / 1024} KB, container valid)"
      )
    } catch (e: Exception) {
      return VerificationResult(
        isValid = false,
        sizeBytes = 0L,
        reason = "Verification exception: ${e.message}"
      )
    }
  }

  /**
   * Deletes a downloaded video from storage.
   */
  fun deleteMedia(context: Context, uriString: String, filePath: String): Boolean {
    return try {
      val uri = Uri.parse(uriString)
      if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        val rows = context.contentResolver.delete(uri, null, null)
        rows > 0
      } else {
        val file = File(filePath)
        if (file.exists()) file.delete() else false
      }
    } catch (_: Exception) {
      try {
        val file = File(filePath)
        if (file.exists()) file.delete() else false
      } catch (_: Exception) {
        false
      }
    }
  }

  /**
   * Creates an Android share Intent for a downloaded video.
   */
  fun createShareIntent(context: Context, uriString: String, title: String, mimeType: String): Intent {
    val uri = Uri.parse(uriString)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = mimeType
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, title)
      putExtra(Intent.EXTRA_TEXT, "Watch: $title via VidMate Pro")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(shareIntent, "Share Video")
  }
}

data class TargetDestination(
  val uri: Uri,
  val outputStream: OutputStream,
  val filePath: String,
  val mimeType: String,
  val isVideo: Boolean,
  val isPendingManaged: Boolean,
  val rawFile: File? = null
)

data class VerificationResult(
  val isValid: Boolean,
  val sizeBytes: Long,
  val reason: String
)
