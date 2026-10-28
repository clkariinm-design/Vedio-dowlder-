package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.screens.DownloaderScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.VidMateTheme
import com.example.ui.util.LocalizedStrings

class MainActivity : ComponentActivity() {

  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    handleIntent(intent)

    setContent {
      val uiState by viewModel.uiState.collectAsState()

      VidMateTheme(themeMode = uiState.themeMode) {
        VidMateApp(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    if (intent == null) return
    val action = intent.action
    val type = intent.type

    if (Intent.ACTION_SEND == action && "text/plain" == type) {
      val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
      viewModel.handleIncomingSharedText(sharedText)
    } else if (Intent.ACTION_VIEW == action) {
      val dataString = intent.dataString
      viewModel.handleIncomingSharedText(dataString)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VidMateApp(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val lang = uiState.languageCode

  // Back handling: pop to DOWNLOADER tab if on a sub-screen
  BackHandler(enabled = uiState.currentTab != AppTab.DOWNLOADER) {
    viewModel.setTab(AppTab.DOWNLOADER)
  }

  // Snackbar listener
  LaunchedEffect(uiState.snackbarMessage) {
    uiState.snackbarMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearSnackbarMessage()
    }
  }

  Scaffold(
    modifier = modifier,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = LocalizedStrings.get("app_name", lang),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
          )
        },
        actions = {
          // Quick language switcher in TopBar
          IconButton(
            onClick = {
              val newLang = if (lang == "en") "so" else "en"
              viewModel.setLanguage(newLang)
            },
            modifier = Modifier.testTag("quick_lang_toggle")
          ) {
            Icon(
              imageVector = Icons.Default.Language,
              contentDescription = "Switch Language (${if (lang == "en") "Somali" else "English"})"
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.onSurface
        )
      )
    },
    bottomBar = {
      NavigationBar(
        tonalElevation = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface
      ) {
        NavigationBarItem(
          selected = uiState.currentTab == AppTab.DOWNLOADER,
          onClick = { viewModel.setTab(AppTab.DOWNLOADER) },
          icon = { Icon(Icons.Default.Download, contentDescription = null) },
          label = { Text(LocalizedStrings.get("tab_downloader", lang)) },
          modifier = Modifier.testTag("nav_downloader")
        )
        NavigationBarItem(
          selected = uiState.currentTab == AppTab.HISTORY,
          onClick = { viewModel.setTab(AppTab.HISTORY) },
          icon = { Icon(Icons.Default.Folder, contentDescription = null) },
          label = { Text(LocalizedStrings.get("tab_history", lang)) },
          modifier = Modifier.testTag("nav_history")
        )
        NavigationBarItem(
          selected = uiState.currentTab == AppTab.PLAYER,
          onClick = { viewModel.setTab(AppTab.PLAYER) },
          icon = { Icon(Icons.Default.PlayCircle, contentDescription = null) },
          label = { Text(LocalizedStrings.get("tab_player", lang)) },
          modifier = Modifier.testTag("nav_player")
        )
        NavigationBarItem(
          selected = uiState.currentTab == AppTab.SETTINGS,
          onClick = { viewModel.setTab(AppTab.SETTINGS) },
          icon = { Icon(Icons.Default.Settings, contentDescription = null) },
          label = { Text(LocalizedStrings.get("tab_settings", lang)) },
          modifier = Modifier.testTag("nav_settings")
        )
      }
    }
  ) { innerPadding ->
    when (uiState.currentTab) {
      AppTab.DOWNLOADER -> DownloaderScreen(
        viewModel = viewModel,
        uiState = uiState,
        modifier = Modifier.padding(innerPadding)
      )
      AppTab.HISTORY -> HistoryScreen(
        viewModel = viewModel,
        uiState = uiState,
        modifier = Modifier.padding(innerPadding)
      )
      AppTab.PLAYER -> PlayerScreen(
        viewModel = viewModel,
        uiState = uiState,
        modifier = Modifier.padding(innerPadding)
      )
      AppTab.SETTINGS -> SettingsScreen(
        viewModel = viewModel,
        uiState = uiState,
        modifier = Modifier.padding(innerPadding)
      )
    }
  }
}
