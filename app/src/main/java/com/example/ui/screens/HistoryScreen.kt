package com.example.ui.screens

import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.model.DownloadedItem
import com.example.storage.MediaStorageManager
import com.example.ui.HistoryFilter
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.util.LocalizedStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
  viewModel: MainViewModel,
  uiState: UiState,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lang = uiState.languageCode
  val downloads by viewModel.filteredDownloads.collectAsState()
  var itemToDelete by remember { mutableStateOf<DownloadedItem?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(16.dp))

    // Title & Filter Chips Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = LocalizedStrings.get("history_title", lang),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "${downloads.size} items",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Filter Chips
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FilterChip(
        selected = uiState.activeFilter == HistoryFilter.ALL,
        onClick = { viewModel.setFilter(HistoryFilter.ALL) },
        label = { Text(LocalizedStrings.get("filter_all", lang)) },
        colors = FilterChipDefaults.filterChipColors()
      )
      FilterChip(
        selected = uiState.activeFilter == HistoryFilter.VIDEO,
        onClick = { viewModel.setFilter(HistoryFilter.VIDEO) },
        label = { Text(LocalizedStrings.get("filter_video", lang)) }
      )
      FilterChip(
        selected = uiState.activeFilter == HistoryFilter.AUDIO,
        onClick = { viewModel.setFilter(HistoryFilter.AUDIO) },
        label = { Text(LocalizedStrings.get("filter_audio", lang)) }
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (downloads.isEmpty()) {
      EmptyHistoryView(lang = lang, onGoToDownload = { viewModel.setTab(com.example.ui.AppTab.DOWNLOADER) })
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(downloads, key = { it.id }) { item ->
          DownloadItemCard(
            item = item,
            context = context,
            lang = lang,
            onPlay = { viewModel.selectItemForPlayback(item) },
            onShare = {
              val shareIntent = MediaStorageManager.createShareIntent(
                context = context,
                uriString = item.localUri,
                title = item.title,
                mimeType = item.mimeType
              )
              context.startActivity(shareIntent)
            },
            onDelete = { itemToDelete = item }
          )
        }
      }
    }
  }

  // Delete Confirmation Dialog
  itemToDelete?.let { item ->
    AlertDialog(
      onDismissRequest = { itemToDelete = null },
      title = { Text(LocalizedStrings.get("delete", lang)) },
      text = { Text(LocalizedStrings.get("delete_confirm", lang)) },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.deleteDownload(item)
            itemToDelete = null
          }
        ) {
          Text(LocalizedStrings.get("confirm", lang), color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { itemToDelete = null }) {
          Text(LocalizedStrings.get("cancel", lang))
        }
      }
    )
  }
}

@Composable
private fun DownloadItemCard(
  item: DownloadedItem,
  context: Context,
  lang: String,
  onPlay: () -> Unit,
  onShare: () -> Unit,
  onDelete: () -> Unit
) {
  val formattedSize = remember(item.fileSize) {
    Formatter.formatFileSize(context, item.fileSize)
  }
  val formattedDate = remember(item.downloadTimestamp) {
    SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(item.downloadTimestamp))
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("download_item_${item.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Thumbnail or Media Icon
        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onPlay() },
          contentAlignment = Alignment.Center
        ) {
          if (!item.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
              model = item.thumbnailUrl,
              contentDescription = "Thumbnail",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop
            )
          } else {
            Icon(
              imageVector = if (item.isVideo) Icons.Default.Videocam else Icons.Default.MusicNote,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp)
            )
          }

          // Small play icon overlay
          Box(
            modifier = Modifier
              .size(26.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = "Play",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and details
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "@${item.author}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = formattedSize,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "•",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = if (item.isVideo) "MP4" else "MP3",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.secondary
            )
            Text(
              text = "•",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = formattedDate,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Action row: Play, Share, Delete
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextButton(onClick = onPlay) {
          Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(LocalizedStrings.get("play", lang))
        }

        IconButton(onClick = onShare) {
          Icon(imageVector = Icons.Default.Share, contentDescription = LocalizedStrings.get("share", lang))
        }

        IconButton(onClick = onDelete) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = LocalizedStrings.get("delete", lang),
            tint = MaterialTheme.colorScheme.error
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyHistoryView(
  lang: String,
  onGoToDownload: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(80.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.FolderOpen,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(40.dp)
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = LocalizedStrings.get("history_empty", lang),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = LocalizedStrings.get("history_empty_desc", lang),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )
      Spacer(modifier = Modifier.height(20.dp))
      OutlinedButton(
        onClick = onGoToDownload,
        shape = RoundedCornerShape(12.dp)
      ) {
        Text(LocalizedStrings.get("tab_downloader", lang))
      }
    }
  }
}
