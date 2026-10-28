package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.DownloadedItem
import com.example.data.repository.DownloadRepository
import com.example.engine.DownloadProgress
import com.example.engine.DownloadStage
import com.example.engine.VideoMetadata
import com.example.ui.theme.AppThemeMode
import com.example.util.TikTokUrlValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab {
  DOWNLOADER,
  HISTORY,
  PLAYER,
  SETTINGS
}

enum class HistoryFilter {
  ALL,
  VIDEO,
  AUDIO
}

data class UiState(
  val currentTab: AppTab = AppTab.DOWNLOADER,
  val inputUrl: String = "",
  val isUrlValid: Boolean = false,
  val isExtracting: Boolean = false,
  val currentMetadata: VideoMetadata? = null,
  val downloadProgress: DownloadProgress = DownloadProgress(),
  val activeFilter: HistoryFilter = HistoryFilter.ALL,
  val selectedItemForPlayback: DownloadedItem? = null,
  val themeMode: AppThemeMode = AppThemeMode.DARK,
  val languageCode: String = "en", // "en" or "so"
  val snackbarMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val database = AppDatabase.getDatabase(application)
  private val repository = DownloadRepository(application, database.downloadDao())

  private val _uiState = MutableStateFlow(UiState())
  val uiState: StateFlow<UiState> = _uiState.asStateFlow()

  private var downloadJob: Job? = null

  private val _downloads = repository.allDownloads
  val filteredDownloads: StateFlow<List<DownloadedItem>> = combine(
    _downloads,
    _uiState
  ) { list, state ->
    when (state.activeFilter) {
      HistoryFilter.ALL -> list
      HistoryFilter.VIDEO -> list.filter { it.isVideo }
      HistoryFilter.AUDIO -> list.filter { !it.isVideo }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun onUrlChanged(newUrl: String) {
    val isValid = TikTokUrlValidator.isValidTikTokUrl(newUrl)
    _uiState.update {
      it.copy(
        inputUrl = newUrl,
        isUrlValid = isValid,
        currentMetadata = if (isValid && it.currentMetadata?.originalUrl == newUrl) it.currentMetadata else null
      )
    }
  }

  fun pasteFromClipboard(rawText: String?) {
    if (rawText.isNullOrBlank()) {
      showSnackbar("Clipboard is empty")
      return
    }

    val extracted = TikTokUrlValidator.extractTikTokUrl(rawText) ?: rawText.trim()
    onUrlChanged(extracted)
    showSnackbar("Pasted into input")
  }

  fun clearInput() {
    _uiState.update { it.copy(inputUrl = "", isUrlValid = false, currentMetadata = null) }
  }

  fun useSampleUrl() {
    onUrlChanged(TikTokUrlValidator.SAMPLE_TIKTOK_URL)
    showSnackbar("Sample TikTok link loaded")
  }

  fun handleIncomingSharedText(sharedText: String?) {
    if (sharedText.isNullOrBlank()) return
    val extracted = TikTokUrlValidator.extractTikTokUrl(sharedText)
    if (extracted != null) {
      _uiState.update {
        it.copy(
          currentTab = AppTab.DOWNLOADER,
          inputUrl = extracted,
          isUrlValid = true
        )
      }
      showSnackbar("Received TikTok video link from Sharesheet")
    } else {
      val trimmed = sharedText.trim()
      _uiState.update {
        it.copy(
          currentTab = AppTab.DOWNLOADER,
          inputUrl = trimmed,
          isUrlValid = TikTokUrlValidator.isValidTikTokUrl(trimmed)
        )
      }
    }
  }

  fun startDownload(isVideo: Boolean = true) {
    val url = _uiState.value.inputUrl.trim()
    if (url.isBlank()) {
      showSnackbar("Please enter a TikTok URL")
      return
    }

    if (!TikTokUrlValidator.isValidTikTokUrl(url)) {
      showSnackbar("Invalid TikTok link. Must contain tiktok.com, vt.tiktok.com, or vm.tiktok.com")
      return
    }

    if (downloadJob?.isActive == true) {
      showSnackbar("A download is already in progress")
      return
    }

    downloadJob = viewModelScope.launch {
      _uiState.update {
        it.copy(
          isExtracting = true,
          downloadProgress = DownloadProgress(stage = DownloadStage.EXTRACTING)
        )
      }

      // Step 1: Extract stream metadata on-device
      val metadataResult = repository.extractMetadata(url)
      if (metadataResult.isFailure) {
        val err = metadataResult.exceptionOrNull()?.message ?: "Failed to resolve video stream"
        _uiState.update {
          it.copy(
            isExtracting = false,
            downloadProgress = DownloadProgress(stage = DownloadStage.FAILED, errorMessage = err)
          )
        }
        showSnackbar("Extraction error: $err")
        return@launch
      }

      val metadata = metadataResult.getOrThrow()
      _uiState.update {
        it.copy(
          isExtracting = false,
          currentMetadata = metadata,
          downloadProgress = DownloadProgress(stage = DownloadStage.DOWNLOADING)
        )
      }

      // Step 2: Download stream chunk-by-chunk with verification
      val downloadResult = repository.downloadMedia(metadata, isVideo) { progress ->
        _uiState.update { it.copy(downloadProgress = progress) }
      }

      downloadResult.onSuccess { downloadedItem ->
        showSnackbar("Downloaded and verified successfully!")
        _uiState.update {
          it.copy(
            selectedItemForPlayback = downloadedItem
          )
        }
      }.onFailure { err ->
        val msg = err.message ?: "Download failed"
        showSnackbar("Download failed: $msg")
      }
    }
  }

  fun cancelDownload() {
    if (downloadJob?.isActive == true) {
      downloadJob?.cancel()
      _uiState.update {
        it.copy(
          isExtracting = false,
          downloadProgress = DownloadProgress(stage = DownloadStage.CANCELLED, errorMessage = "Cancelled by user")
        )
      }
      showSnackbar("Download cancelled")
    }
  }

  fun selectItemForPlayback(item: DownloadedItem) {
    _uiState.update {
      it.copy(
        selectedItemForPlayback = item,
        currentTab = AppTab.PLAYER
      )
    }
  }

  fun deleteDownload(item: DownloadedItem) {
    viewModelScope.launch {
      val success = repository.deleteDownload(item)
      if (_uiState.value.selectedItemForPlayback?.id == item.id) {
        _uiState.update { it.copy(selectedItemForPlayback = null) }
      }
      showSnackbar(if (success) "Video deleted from device" else "Removed from history")
    }
  }

  fun setTab(tab: AppTab) {
    _uiState.update { it.copy(currentTab = tab) }
  }

  fun setFilter(filter: HistoryFilter) {
    _uiState.update { it.copy(activeFilter = filter) }
  }

  fun setThemeMode(mode: AppThemeMode) {
    _uiState.update { it.copy(themeMode = mode) }
  }

  fun setLanguage(code: String) {
    _uiState.update { it.copy(languageCode = code) }
  }

  fun clearSnackbarMessage() {
    _uiState.update { it.copy(snackbarMessage = null) }
  }

  private fun showSnackbar(msg: String) {
    _uiState.update { it.copy(snackbarMessage = msg) }
  }

  fun getEngineName(): String = repository.getActiveEngineName()
}
