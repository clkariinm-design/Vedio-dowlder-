package com.example.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object TikTokExtractor {

  private val client: OkHttpClient = OkHttpClient.Builder()
    .followRedirects(true)
    .followSslRedirects(true)
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build()

  private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

  private const val MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

  /**
   * Resolves shortened links (vt.tiktok.com/vm.tiktok.com) to canonical URL.
   */
  suspend fun resolveCanonicalUrl(url: String): String = withContext(Dispatchers.IO) {
    try {
      val request = Request.Builder()
        .url(url)
        .header("User-Agent", MOBILE_USER_AGENT)
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .build()

      client.newCall(request).execute().use { response ->
        val resolved = response.request.url.toString()
        if (resolved.isNotBlank()) resolved else url
      }
    } catch (_: Exception) {
      url
    }
  }

  /**
   * Extracts metadata and direct video/audio stream URLs on-device using a multi-strategy pipeline.
   */
  suspend fun extract(url: String): Result<VideoMetadata> = withContext(Dispatchers.IO) {
    try {
      val canonicalUrl = resolveCanonicalUrl(url)
      val videoId = extractVideoId(canonicalUrl) ?: extractVideoId(url) ?: "tiktok_${System.currentTimeMillis()}"

      // Strategy 1: High-reliability direct client resolution (TikWM public endpoint)
      try {
        val encodedUrl = URLEncoder.encode(canonicalUrl, "UTF-8")
        val tikWmRequest = Request.Builder()
          .url("https://tikwm.com/api/?url=$encodedUrl")
          .header("User-Agent", DESKTOP_USER_AGENT)
          .header("Accept", "application/json")
          .build()

        client.newCall(tikWmRequest).execute().use { response ->
          if (response.isSuccessful) {
            val body = response.body?.string()
            if (!body.isNullOrBlank()) {
              val json = JSONObject(body)
              val code = json.optInt("code", -1)
              if (code == 0 && json.has("data")) {
                val data = json.getJSONObject("data")
                var playUrl = data.optString("play", "")
                var musicUrl = data.optString("music", "")
                val title = data.optString("title", "TikTok Video").ifBlank { "TikTok Video" }
                val cover = data.optString("cover", null)
                val authorObj = data.optJSONObject("author")
                val author = authorObj?.optString("nickname", "TikTok Creator") ?: "TikTok Creator"
                val duration = data.optLong("duration", 0L)
                val size = data.optLong("size", 0L)

                if (playUrl.startsWith("/")) {
                  playUrl = "https://www.tikwm.com$playUrl"
                }
                if (musicUrl.startsWith("/")) {
                  musicUrl = "https://www.tikwm.com$musicUrl"
                }

                if (playUrl.isNotBlank()) {
                  return@withContext Result.success(
                    VideoMetadata(
                      videoId = videoId,
                      originalUrl = url,
                      canonicalUrl = canonicalUrl,
                      title = title,
                      author = author,
                      videoStreamUrl = playUrl,
                      audioStreamUrl = if (musicUrl.isNotBlank()) musicUrl else null,
                      thumbnailUrl = cover,
                      durationSeconds = duration,
                      estimatedSizeBytes = size
                    )
                  )
                }
              }
            }
          }
        }
      } catch (_: Exception) {
        // Fallback to direct HTML extraction
      }

      // Strategy 2: Direct on-device TikTok HTML scraping & oEmbed extraction
      var title = "TikTok Video"
      var author = "TikTok Creator"
      var thumbnailUrl: String? = null

      try {
        val oembedUrl = "https://www.tiktok.com/oembed?url=${URLEncoder.encode(canonicalUrl, "UTF-8")}"
        val oembedRequest = Request.Builder()
          .url(oembedUrl)
          .header("User-Agent", DESKTOP_USER_AGENT)
          .build()

        client.newCall(oembedRequest).execute().use { response ->
          if (response.isSuccessful) {
            val body = response.body?.string()
            if (!body.isNullOrBlank()) {
              val json = JSONObject(body)
              title = json.optString("title", title).ifBlank { title }
              author = json.optString("author_name", author).ifBlank { author }
              thumbnailUrl = json.optString("thumbnail_url", null)
            }
          }
        }
      } catch (_: Exception) {}

      val webRequest = Request.Builder()
        .url(canonicalUrl)
        .header("User-Agent", DESKTOP_USER_AGENT)
        .header("Referer", "https://www.tiktok.com/")
        .header("Cookie", "tt_webid_v2=$videoId; ttwid=1%7Cb88; s_v_web_id=verify_123")
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
        .header("Accept-Language", "en-US,en;q=0.9")
        .build()

      var directVideoStreamUrl: String? = null

      client.newCall(webRequest).execute().use { response ->
        val html = response.body?.string() ?: ""

        if (html.contains("\"statusCode\":10204") || html.contains("item doesn't exist")) {
          return@withContext Result.failure(
            IOException("This TikTok video does not exist or has been deleted by its creator.")
          )
        }

        directVideoStreamUrl = extractStreamFromHtml(html)
        if (directVideoStreamUrl == null) {
          directVideoStreamUrl = extractFromMetaTags(html)
        }
        if (thumbnailUrl == null) {
          thumbnailUrl = extractThumbnailFromHtml(html)
        }
      }

      if (directVideoStreamUrl.isNullOrBlank()) {
        return@withContext Result.failure(
          IOException("Could not extract a downloadable video stream. The video may be deleted, private, or region-restricted.")
        )
      }

      val metadata = VideoMetadata(
        videoId = videoId,
        originalUrl = url,
        canonicalUrl = canonicalUrl,
        title = title,
        author = author,
        videoStreamUrl = directVideoStreamUrl!!,
        audioStreamUrl = null,
        thumbnailUrl = thumbnailUrl,
        durationSeconds = 0,
        estimatedSizeBytes = 0
      )

      Result.success(metadata)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun extractStreamFromHtml(html: String): String? {
    val playAddrRegex = Pattern.compile("\"(?:playAddr|downloadAddr)\":\"(https?:[^\"]+)\"")
    val matcher = playAddrRegex.matcher(html)
    if (matcher.find()) {
      val rawUrl = matcher.group(1) ?: return null
      return unescapeJsonUrl(rawUrl)
    }

    val urlListRegex = Pattern.compile("\"url_list\":\\[\"(https?:[^\"]+)\"")
    val urlMatcher = urlListRegex.matcher(html)
    if (urlMatcher.find()) {
      val rawUrl = urlMatcher.group(1) ?: return null
      return unescapeJsonUrl(rawUrl)
    }

    return null
  }

  private fun extractFromMetaTags(html: String): String? {
    val ogVideoRegex = Pattern.compile("<meta[^>]+property=[\"']og:video(?::secure_url)?[\"'][^>]+content=[\"']([^\"']+)[\"']")
    val matcher = ogVideoRegex.matcher(html)
    if (matcher.find()) {
      return unescapeJsonUrl(matcher.group(1) ?: "")
    }

    val twitterPlayer = Pattern.compile("<meta[^>]+name=[\"']twitter:player:stream[\"'][^>]+content=[\"']([^\"']+)[\"']")
    val twitterMatcher = twitterPlayer.matcher(html)
    if (twitterMatcher.find()) {
      return unescapeJsonUrl(twitterMatcher.group(1) ?: "")
    }

    return null
  }

  private fun extractThumbnailFromHtml(html: String): String? {
    val ogImage = Pattern.compile("<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']")
    val matcher = ogImage.matcher(html)
    if (matcher.find()) {
      return unescapeJsonUrl(matcher.group(1) ?: "")
    }
    return null
  }

  fun unescapeJsonUrl(url: String): String {
    return url.replace("\\u002F", "/")
      .replace("\\u0026", "&")
      .replace("\\/", "/")
      .trim()
  }

  fun extractVideoId(url: String): String? {
    val videoIdRegex = Pattern.compile("(?:video|v)/([0-9]{15,22})")
    val matcher = videoIdRegex.matcher(url)
    return if (matcher.find()) matcher.group(1) else null
  }
}
