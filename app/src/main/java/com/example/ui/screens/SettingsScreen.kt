package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.YtDlpArchitectureReport
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.theme.AppThemeMode
import com.example.ui.util.LocalizedStrings

@Composable
fun SettingsScreen(
  viewModel: MainViewModel,
  uiState: UiState,
  modifier: Modifier = Modifier
) {
  val lang = uiState.languageCode

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Text(
        text = LocalizedStrings.get("settings_title", lang),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    }

    // Language Selector Card
    item {
      SettingsSectionCard(
        title = LocalizedStrings.get("language", lang),
        icon = Icons.Default.Language
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.setLanguage("en") }
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RadioButton(
            selected = uiState.languageCode == "en",
            onClick = { viewModel.setLanguage("en") },
            modifier = Modifier.testTag("lang_english_radio")
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "English",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (uiState.languageCode == "en") FontWeight.Bold else FontWeight.Normal
          )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = DividerDefaults.color.copy(alpha = 0.3f))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.setLanguage("so") }
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RadioButton(
            selected = uiState.languageCode == "so",
            onClick = { viewModel.setLanguage("so") },
            modifier = Modifier.testTag("lang_somali_radio")
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Af-Soomaali (Somali)",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = if (uiState.languageCode == "so") FontWeight.Bold else FontWeight.Normal
            )
            Text(
              text = "Soo Dajiye Muuqaallada TikTok oo Af-Soomaali ah",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // Theme Mode Card
    item {
      SettingsSectionCard(
        title = LocalizedStrings.get("theme", lang),
        icon = Icons.Default.ColorLens
      ) {
        listOf(
          AppThemeMode.SYSTEM to LocalizedStrings.get("theme_system", lang),
          AppThemeMode.LIGHT to LocalizedStrings.get("theme_light", lang),
          AppThemeMode.DARK to LocalizedStrings.get("theme_dark", lang)
        ).forEachIndexed { index, (mode, label) ->
          if (index > 0) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = DividerDefaults.color.copy(alpha = 0.3f))
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { viewModel.setThemeMode(mode) }
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = uiState.themeMode == mode,
              onClick = { viewModel.setThemeMode(mode) },
              modifier = Modifier.testTag("theme_${mode.name.lowercase()}_radio")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = label,
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = if (uiState.themeMode == mode) FontWeight.Bold else FontWeight.Normal
            )
          }
        }
      }
    }

    // Download Engine & yt-dlp Status Report
    item {
      SettingsSectionCard(
        title = LocalizedStrings.get("engine_section", lang),
        icon = Icons.Default.Code
      ) {
        Text(
          text = LocalizedStrings.get("active_engine", lang),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = LocalizedStrings.get("active_engine_desc", lang),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = LocalizedStrings.get("ytdlp_review_title", lang),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = LocalizedStrings.get("ytdlp_review_desc", lang),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // Storage Location
    item {
      SettingsSectionCard(
        title = LocalizedStrings.get("storage_location", lang),
        icon = Icons.Default.Storage
      ) {
        Text(
          text = LocalizedStrings.get("storage_location_path", lang),
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Standard Scoped Storage: Downloaded videos are directly accessible from your Photos/Gallery app without requiring dangerous full-storage permissions.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // Watermark Transparency & Ethics Card
    item {
      SettingsSectionCard(
        title = LocalizedStrings.get("watermark_title", lang),
        icon = Icons.Default.Security
      ) {
        Text(
          text = LocalizedStrings.get("watermark_desc", lang),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // About & Version
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "VidMate Pro for Android",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Version 1.0.0 • On-Device Architecture",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun SettingsSectionCard(
  title: String,
  icon: ImageVector,
  content: @Composable () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      content()
    }
  }
}
