package com.example

import com.example.engine.DownloadProgress
import com.example.engine.DownloadStage
import com.example.storage.MediaStorageManager
import com.example.storage.TargetDestination
import com.example.ui.util.LocalizedStrings
import com.example.util.TikTokUrlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File

class VidMateProTestSuite {

  @Test
  fun testTikTokUrlValidation_validUrls() {
    assertTrue(TikTokUrlValidator.isValidTikTokUrl("https://www.tiktok.com/@creator/video/1234567890123456789"))
    assertTrue(TikTokUrlValidator.isValidTikTokUrl("https://vt.tiktok.com/ZS1234567/"))
    assertTrue(TikTokUrlValidator.isValidTikTokUrl("https://vm.tiktok.com/ZM8765432/"))
    assertTrue(TikTokUrlValidator.isValidTikTokUrl("https://www.tiktokv.com/share/video/7461819411524078879"))
    assertTrue(TikTokUrlValidator.isValidTikTokUrl("http://tiktok.com/@user/video/7123456789012345678?is_from_webapp=1"))
  }

  @Test
  fun testTikTokUrlValidation_invalidUrls() {
    assertFalse(TikTokUrlValidator.isValidTikTokUrl(""))
    assertFalse(TikTokUrlValidator.isValidTikTokUrl("   "))
    assertFalse(TikTokUrlValidator.isValidTikTokUrl("https://youtube.com/watch?v=12345"))
    assertFalse(TikTokUrlValidator.isValidTikTokUrl("https://google.com"))
    assertFalse(TikTokUrlValidator.isValidTikTokUrl("not a valid url at all"))
    assertFalse(TikTokUrlValidator.isValidTikTokUrl("https://fake-tiktok.com.evil.com/video/123"))
  }

  @Test
  fun testIncomingSharedLinksExtraction() {
    val shareText1 = "Check out this funny clip on TikTok! https://vt.tiktok.com/ZSxyz123/ #viral"
    val extracted1 = TikTokUrlValidator.extractTikTokUrl(shareText1)
    assertEquals("https://vt.tiktok.com/ZSxyz123/", extracted1)

    val shareText2 = "Look at this video: https://www.tiktok.com/@user/video/7123456789012345678."
    val extracted2 = TikTokUrlValidator.extractTikTokUrl(shareText2)
    assertEquals("https://www.tiktok.com/@user/video/7123456789012345678", extracted2)

    val shareTextWithQuotes = "\"https://vm.tiktok.com/ZM8765432/\""
    val extracted3 = TikTokUrlValidator.extractTikTokUrl(shareTextWithQuotes)
    assertEquals("https://vm.tiktok.com/ZM8765432/", extracted3)

    val nonTikTokShare = "Hey check this google search https://www.google.com"
    val extractedNonTikTok = TikTokUrlValidator.extractTikTokUrl(nonTikTokShare)
    assertNull(extractedNonTikTok)
  }

  @Test
  fun testDownloadStateTransitions() {
    var state = DownloadProgress(stage = DownloadStage.IDLE)
    assertEquals(DownloadStage.IDLE, state.stage)
    assertEquals(0, state.percent)

    state = state.copy(stage = DownloadStage.EXTRACTING)
    assertEquals(DownloadStage.EXTRACTING, state.stage)

    state = state.copy(
      stage = DownloadStage.DOWNLOADING,
      percent = 50,
      bytesDownloaded = 5000L,
      totalBytes = 10000L,
      speedBytesPerSec = 1024L
    )
    assertEquals(DownloadStage.DOWNLOADING, state.stage)
    assertEquals(50, state.percent)
    assertEquals(5000L, state.bytesDownloaded)

    state = state.copy(stage = DownloadStage.VERIFYING, percent = 99)
    assertEquals(DownloadStage.VERIFYING, state.stage)

    state = state.copy(
      stage = DownloadStage.COMPLETED,
      percent = 100,
      savedUriString = "content://media/external/video/media/42"
    )
    assertEquals(DownloadStage.COMPLETED, state.stage)
    assertEquals(100, state.percent)
    assertNotNull(state.savedUriString)
  }

  @Test
  fun testErrorStateHandling() {
    val failedState = DownloadProgress(
      stage = DownloadStage.FAILED,
      errorMessage = "Stream extraction failed: Video is private or deleted."
    )
    assertEquals(DownloadStage.FAILED, failedState.stage)
    assertTrue(failedState.errorMessage!!.contains("private or deleted"))

    val cancelledState = DownloadProgress(
      stage = DownloadStage.CANCELLED,
      errorMessage = "Cancelled by user"
    )
    assertEquals(DownloadStage.CANCELLED, cancelledState.stage)
  }

  @Test
  fun testLocalizationResolution_EnglishAndSomali() {
    val englishName = LocalizedStrings.get("app_name", "en")
    val somaliName = LocalizedStrings.get("app_name", "so")
    assertEquals("VidMate Pro", englishName)
    assertEquals("VidMate Pro", somaliName)

    val englishPaste = LocalizedStrings.get("paste_button", "en")
    val somaliPaste = LocalizedStrings.get("paste_button", "so")
    assertEquals("Paste", englishPaste)
    assertEquals("Dheji", somaliPaste)

    val englishDownload = LocalizedStrings.get("download_video", "en")
    val somaliDownload = LocalizedStrings.get("download_video", "so")
    assertEquals("Download Video", englishDownload)
    assertEquals("Soo Daji Muuqaalka", somaliDownload)
  }
}
