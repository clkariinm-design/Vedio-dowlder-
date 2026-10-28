package com.example.ui.screens

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.util.LocalizedStrings
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun PlayerScreen(
  viewModel: MainViewModel,
  uiState: UiState,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lang = uiState.languageCode
  val item = uiState.selectedItemForPlayback

  if (item == null) {
    NoVideoSelectedView(
      lang = lang,
      onGoToHistory = { viewModel.setTab(AppTab.HISTORY) }
    )
    return
  }

  var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
  var isPlaying by remember { mutableStateOf(true) }
  var isLooping by remember { mutableStateOf(true) }
  var currentPositionMs by remember { mutableIntStateOf(0) }
  var durationMs by remember { mutableIntStateOf(0) }
  var isDraggingSlider by remember { mutableStateOf(false) }

  // Periodic polling for progress updates
  LaunchedEffect(isPlaying, isDraggingSlider) {
    while (isPlaying && !isDraggingSlider) {
      videoViewRef?.let { vv ->
        if (vv.isPlaying) {
          currentPositionMs = vv.currentPosition
          if (durationMs == 0 && vv.duration > 0) {
            durationMs = vv.duration
          }
        }
      }
      delay(300)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp)
  ) {
    // Top Bar with Back and Open External
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = { viewModel.setTab(AppTab.HISTORY) }) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = LocalizedStrings.get("back_to_downloads", lang)
        )
      }
      Text(
        text = LocalizedStrings.get("player_title", lang),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      IconButton(
        onClick = {
          val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(item.localUri), item.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          try {
            context.startActivity(intent)
          } catch (_: Exception) {}
        }
      ) {
        Icon(
          imageVector = Icons.Default.OpenInNew,
          contentDescription = LocalizedStrings.get("player_open_external", lang)
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Video Container Box
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .clip(RoundedCornerShape(20.dp))
        .background(Color.Black),
      contentAlignment = Alignment.Center
    ) {
      AndroidView(
        factory = { ctx ->
          VideoView(ctx).apply {
            setVideoURI(Uri.parse(item.localUri))
            setOnPreparedListener { mp ->
              durationMs = mp.duration
              mp.isLooping = isLooping
              start()
              isPlaying = true
            }
            setOnCompletionListener {
              if (!isLooping) {
                isPlaying = false
              }
            }
            setOnErrorListener { _, _, _ ->
              isPlaying = false
              true
            }
            videoViewRef = this
          }
        },
        update = { vv ->
          videoViewRef = vv
        },
        modifier = Modifier.fillMaxSize()
      )

      // Center Play/Pause tap toggle overlay if paused
      if (!isPlaying) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f)),
          contentAlignment = Alignment.Center
        ) {
          IconButton(
            onClick = {
              videoViewRef?.start()
              isPlaying = true
            },
            modifier = Modifier.size(64.dp)
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = "Play",
              tint = Color.White,
              modifier = Modifier.size(40.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Video Details Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        Text(
          text = item.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "@${item.author}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Slider
        Slider(
          value = if (durationMs > 0) currentPositionMs.toFloat() else 0f,
          onValueChange = { newValue ->
            isDraggingSlider = true
            currentPositionMs = newValue.toInt()
          },
          onValueChangeFinished = {
            videoViewRef?.seekTo(currentPositionMs)
            isDraggingSlider = false
          },
          valueRange = 0f..(durationMs.toFloat().coerceAtLeast(1f)),
          colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("player_seekbar")
        )

        // Time indicators
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = formatTime(currentPositionMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = formatTime(durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Playback Controls Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Rewind 5s
          IconButton(onClick = {
            val target = (currentPositionMs - 5000).coerceAtLeast(0)
            videoViewRef?.seekTo(target)
            currentPositionMs = target
          }) {
            Icon(imageVector = Icons.Default.FastRewind, contentDescription = "Rewind 5s")
          }

          // Main Play / Pause Button
          Box(
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
          ) {
            IconButton(
              onClick = {
                videoViewRef?.let { vv ->
                  if (vv.isPlaying) {
                    vv.pause()
                    isPlaying = false
                  } else {
                    vv.start()
                    isPlaying = true
                  }
                }
              },
              modifier = Modifier.testTag("play_pause_button")
            ) {
              Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
              )
            }
          }

          // Forward 5s
          IconButton(onClick = {
            val target = (currentPositionMs + 5000).coerceAtMost(durationMs)
            videoViewRef?.seekTo(target)
            currentPositionMs = target
          }) {
            Icon(imageVector = Icons.Default.FastForward, contentDescription = "Forward 5s")
          }

          // Loop toggle
          IconButton(onClick = {
            isLooping = !isLooping
            videoViewRef?.let { vv ->
              // MediaPlayer loop toggle
            }
          }) {
            Icon(
              imageVector = Icons.Default.Loop,
              contentDescription = LocalizedStrings.get("player_loop", lang),
              tint = if (isLooping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      videoViewRef?.stopPlayback()
    }
  }
}

@Composable
private fun NoVideoSelectedView(
  lang: String,
  onGoToHistory: () -> Unit
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
          imageVector = Icons.Default.VideoLibrary,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(40.dp)
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = LocalizedStrings.get("player_title", lang),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = LocalizedStrings.get("player_no_video", lang),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )
      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = onGoToHistory,
        shape = RoundedCornerShape(12.dp)
      ) {
        Text(LocalizedStrings.get("tab_history", lang))
      }
    }
  }
}

private fun formatTime(ms: Int): String {
  val totalSec = (ms / 1000).coerceAtLeast(0)
  val minutes = totalSec / 60
  val seconds = totalSec % 60
  return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
