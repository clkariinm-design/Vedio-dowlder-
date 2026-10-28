package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.engine.DownloadProgress
import com.example.engine.DownloadStage
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.util.LocalizedStrings

@Composable
fun DownloaderScreen(
  viewModel: MainViewModel,
  uiState: UiState,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lang = uiState.languageCode
  val progress = uiState.downloadProgress
  val isDownloading = progress.stage == DownloadStage.DOWNLOADING || progress.stage == DownloadStage.EXTRACTING || progress.stage == DownloadStage.VERIFYING

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header Banner
    item {
      HeaderBanner(lang = lang)
    }

    // Input Card with Paste & Clear
    item {
      InputSection(
        inputUrl = uiState.inputUrl,
        onUrlChange = { viewModel.onUrlChanged(it) },
        onPaste = {
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          val clip = clipboard.primaryClip
          val text = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).text?.toString() else null
          viewModel.pasteFromClipboard(text)
        },
        onClear = { viewModel.clearInput() },
        lang = lang,
        isDownloading = isDownloading
      )
    }

    // Quick Test link helper & Sharesheet tip
    item {
      QuickActionsRow(
        onTrySample = { viewModel.useSampleUrl() },
        lang = lang,
        isDownloading = isDownloading
      )
    }

    // Extracted Preview Card (if metadata found)
    uiState.currentMetadata?.let { meta ->
      item {
        MetadataPreviewCard(
          title = meta.title,
          author = meta.author,
          thumbnailUrl = meta.thumbnailUrl,
          videoId = meta.videoId
        )
      }
    }

    // Download Action Buttons
    item {
      DownloadActionButtons(
        onDownloadVideo = { viewModel.startDownload(isVideo = true) },
        onDownloadAudio = { viewModel.startDownload(isVideo = false) },
        isDownloading = isDownloading,
        isUrlValid = uiState.isUrlValid,
        lang = lang
      )
    }

    // Active Download / Progress Status Card
    if (progress.stage != DownloadStage.IDLE) {
      item {
        DownloadProgressCard(
          progress = progress,
          onCancel = { viewModel.cancelDownload() },
          onPlayNow = {
            uiState.selectedItemForPlayback?.let { viewModel.selectItemForPlayback(it) }
          },
          lang = lang
        )
      }
    }

    // Instruction Card explaining Sharesheet
    item {
      ShareSheetHelpCard(lang = lang)
    }
  }
}

@Composable
private fun HeaderBanner(lang: String) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    shape = RoundedCornerShape(20.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Download,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(28.dp)
        )
      }
      Spacer(modifier = Modifier.width(16.dp))
      Column {
        Text(
          text = LocalizedStrings.get("app_name", lang),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = LocalizedStrings.get("app_tagline", lang),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
      }
    }
  }
}

@Composable
private fun InputSection(
  inputUrl: String,
  onUrlChange: (String) -> Unit,
  onPaste: () -> Unit,
  onClear: () -> Unit,
  lang: String,
  isDownloading: Boolean
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      OutlinedTextField(
        value = inputUrl,
        onValueChange = onUrlChange,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("tiktok_url_input"),
        placeholder = { Text(LocalizedStrings.get("input_hint", lang)) },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Link,
            contentDescription = "Link Icon",
            tint = MaterialTheme.colorScheme.primary
          )
        },
        trailingIcon = {
          if (inputUrl.isNotEmpty() && !isDownloading) {
            IconButton(onClick = onClear) {
              Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = LocalizedStrings.get("clear_button", lang)
              )
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        enabled = !isDownloading,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onPaste,
          modifier = Modifier
            .weight(1f)
            .testTag("paste_button"),
          enabled = !isDownloading,
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(
            imageVector = Icons.Default.ContentPaste,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(LocalizedStrings.get("paste_button", lang))
        }

        if (inputUrl.isNotEmpty()) {
          OutlinedButton(
            onClick = onClear,
            enabled = !isDownloading,
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(LocalizedStrings.get("clear_button", lang))
          }
        }
      }
    }
  }
}

@Composable
private fun QuickActionsRow(
  onTrySample: () -> Unit,
  lang: String,
  isDownloading: Boolean
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    OutlinedButton(
      onClick = onTrySample,
      enabled = !isDownloading,
      shape = RoundedCornerShape(20.dp),
      modifier = Modifier.testTag("sample_link_button")
    ) {
      Text(LocalizedStrings.get("try_sample", lang), style = MaterialTheme.typography.labelMedium)
    }

    Text(
      text = "vt.tiktok.com • vm.tiktok.com",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    )
  }
}

@Composable
private fun MetadataPreviewCard(
  title: String,
  author: String,
  thumbnailUrl: String?,
  videoId: String
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (!thumbnailUrl.isNullOrBlank()) {
        AsyncImage(
          model = thumbnailUrl,
          contentDescription = "TikTok Video Thumbnail",
          modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(10.dp)),
          contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.SemiBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "@$author • ID: $videoId",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1
        )
      }
    }
  }
}

@Composable
private fun DownloadActionButtons(
  onDownloadVideo: () -> Unit,
  onDownloadAudio: () -> Unit,
  isDownloading: Boolean,
  isUrlValid: Boolean,
  lang: String
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Button(
      onClick = onDownloadVideo,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("download_video_button"),
      enabled = !isDownloading && isUrlValid,
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
      )
    ) {
      Icon(imageVector = Icons.Default.Download, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = LocalizedStrings.get("download_video", lang),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    }

    OutlinedButton(
      onClick = onDownloadAudio,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("download_audio_button"),
      enabled = !isDownloading && isUrlValid,
      shape = RoundedCornerShape(14.dp)
    ) {
      Icon(imageVector = Icons.Default.MusicNote, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = LocalizedStrings.get("download_audio", lang),
        style = MaterialTheme.typography.titleSmall
      )
    }
  }
}

@Composable
private fun DownloadProgressCard(
  progress: DownloadProgress,
  onCancel: () -> Unit,
  onPlayNow: () -> Unit,
  lang: String
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = when (progress.stage) {
        DownloadStage.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
        DownloadStage.FAILED -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
      }
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          when (progress.stage) {
            DownloadStage.EXTRACTING -> {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = LocalizedStrings.get("status_extracting", lang),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
              )
            }
            DownloadStage.DOWNLOADING -> {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "${LocalizedStrings.get("status_downloading", lang)} ${progress.percent}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
              )
            }
            DownloadStage.VERIFYING -> {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = LocalizedStrings.get("status_verifying", lang),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
              )
            }
            DownloadStage.COMPLETED -> {
              Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = LocalizedStrings.get("status_success", lang),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
            DownloadStage.FAILED -> {
              Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = LocalizedStrings.get("status_failed", lang),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
              )
            }
            DownloadStage.CANCELLED -> {
              Icon(imageVector = Icons.Default.Close, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = LocalizedStrings.get("status_cancelled", lang),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
              )
            }
            else -> {}
          }
        }

        if (progress.stage == DownloadStage.DOWNLOADING || progress.stage == DownloadStage.EXTRACTING) {
          IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
            Icon(imageVector = Icons.Default.Close, contentDescription = LocalizedStrings.get("cancel", lang))
          }
        }
      }

      if (progress.stage == DownloadStage.DOWNLOADING) {
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
          progress = { progress.percent / 100f },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          val mbDownloaded = progress.bytesDownloaded / (1024f * 1024f)
          val mbTotal = progress.totalBytes / (1024f * 1024f)
          val speedKb = progress.speedBytesPerSec / 1024f
          val speedText = if (speedKb > 1024) String.format("%.1f MB/s", speedKb / 1024f) else String.format("%.0f KB/s", speedKb)

          Text(
            text = String.format("%.1f MB / %.1f MB", mbDownloaded, mbTotal),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = speedText,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      if (progress.stage == DownloadStage.FAILED && !progress.errorMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = progress.errorMessage,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onErrorContainer
        )
      }

      if (progress.stage == DownloadStage.COMPLETED) {
        Spacer(modifier = Modifier.height(10.dp))
        Button(
          onClick = onPlayNow,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text(LocalizedStrings.get("play_now", lang))
        }
      }
    }
  }
}

@Composable
private fun ShareSheetHelpCard(lang: String) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.Share,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = LocalizedStrings.get("sharesheet_tip", lang),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
