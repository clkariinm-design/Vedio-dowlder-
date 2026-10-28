package com.example.util

import java.net.URI
import java.util.regex.Pattern

object TikTokUrlValidator {
  private val URL_PATTERN: Pattern = Pattern.compile(
    "https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(?:/[^\\s]*)?",
    Pattern.CASE_INSENSITIVE
  )

  private val TIKTOK_HOST_PATTERN: Pattern = Pattern.compile(
    "^(?:.*\\.)?(?:tiktok|tiktokv)\\.com$",
    Pattern.CASE_INSENSITIVE
  )

  /**
   * Extracts the first TikTok URL found in raw text (such as from the Android Sharesheet).
   */
  fun extractTikTokUrl(text: String?): String? {
    if (text.isNullOrBlank()) return null
    val matcher = URL_PATTERN.matcher(text)
    while (matcher.find()) {
      val candidate = matcher.group()?.trim() ?: continue
      val cleanCandidate = cleanUrl(candidate)
      if (isValidTikTokUrl(cleanCandidate)) {
        return cleanCandidate
      }
    }
    return null
  }

  /**
   * Verifies if a given string is a valid TikTok URL.
   */
  fun isValidTikTokUrl(rawUrl: String?): Boolean {
    if (rawUrl.isNullOrBlank()) return false
    val clean = cleanUrl(rawUrl)
    return try {
      val uri = URI(clean)
      val host = uri.host?.lowercase() ?: return false
      TIKTOK_HOST_PATTERN.matcher(host).matches()
    } catch (_: Exception) {
      false
    }
  }

  /**
   * Cleans trailing punctuation often attached when users copy-paste sentences.
   */
  fun cleanUrl(url: String): String {
    var trimmed = url.trim()
    while (trimmed.isNotEmpty() && (trimmed.endsWith(".") || trimmed.endsWith(",") || trimmed.endsWith(")") || trimmed.endsWith(">") || trimmed.endsWith("\"") || trimmed.endsWith("'"))) {
      trimmed = trimmed.substring(0, trimmed.length - 1)
    }
    return trimmed
  }

  /**
   * Verified active TikTok video URL provided for testing.
   */
  const val SAMPLE_TIKTOK_URL = "https://www.tiktokv.com/share/video/7461819411524078879"
}
