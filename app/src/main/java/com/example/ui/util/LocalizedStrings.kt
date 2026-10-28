package com.example.ui.util

object LocalizedStrings {

  fun get(key: String, languageCode: String): String {
    val isSomali = languageCode == "so"
    return when (key) {
      "app_name" -> "VidMate Pro"
      "app_tagline" -> if (isSomali) "Soo Dajiye TikTok oo Toos ah" else "Fast On-Device TikTok Downloader"
      "tab_downloader" -> if (isSomali) "Soo Dajiye" else "Downloader"
      "tab_history" -> if (isSomali) "Dajinta" else "Downloads"
      "tab_player" -> if (isSomali) "Daare" else "Player"
      "tab_settings" -> if (isSomali) "Dejinta" else "Settings"

      "input_hint" -> if (isSomali) "Ku dheji lifaaqa muuqaalka TikTok halkan…" else "Paste TikTok video link here…"
      "paste_button" -> if (isSomali) "Dheji" else "Paste"
      "clear_button" -> if (isSomali) "Tirtir" else "Clear"
      "download_video" -> if (isSomali) "Soo Daji Muuqaalka" else "Download Video"
      "download_audio" -> if (isSomali) "Soo Daji Codka (MP3)" else "Download Audio (MP3)"
      "try_sample" -> if (isSomali) "Tijaabi Lifaaq" else "Try Sample Link"
      "sharesheet_tip" -> if (isSomali) "Talo: Ku dhex dhufo Share TikTok ka dibna dooro VidMate Pro" else "Tip: Tap Share in TikTok then choose VidMate Pro"

      "status_idle" -> if (isSomali) "Diyaar u ah soo dajin" else "Ready to download"
      "status_extracting" -> if (isSomali) "Muuqaalka ayaa laga soo saarayaa qalabkaaga dhexdiisa…" else "Extracting video stream on-device…"
      "status_downloading" -> if (isSomali) "Muuqaalka ayaa la soo dajinayaa…" else "Downloading video…"
      "status_verifying" -> if (isSomali) "Faylka ayaa la xaqiijinayaa…" else "Verifying file integrity…"
      "status_success" -> if (isSomali) "Soo dajintu way dhammaatay waana la xaqiijiyay!" else "Download complete and verified!"
      "status_cancelled" -> if (isSomali) "Soo dajintii waa la joojiyay" else "Download cancelled"
      "status_failed" -> if (isSomali) "Soo dajintu way fashilantay" else "Download error"
      "cancel" -> if (isSomali) "Jooji" else "Cancel"
      "play_now" -> if (isSomali) "Daaro Hadda" else "Play Now"

      "history_title" -> if (isSomali) "Muuqaallada La Soo Dajiyay" else "Downloaded Videos"
      "history_empty" -> if (isSomali) "Wali Waxba Lama Soo Dajin" else "No Downloads Yet"
      "history_empty_desc" -> if (isSomali) "Muuqaallada TikTok ee aad soo dajisay waxay ka muuqan doonaan halkan iyo album-ka taleefankaaga." else "Downloaded TikTok videos will appear here and in your device gallery."
      "filter_all" -> if (isSomali) "Dhammaan" else "All"
      "filter_video" -> if (isSomali) "Muuqaallo" else "Videos"
      "filter_audio" -> if (isSomali) "Codad" else "Audio"
      "play" -> if (isSomali) "Daaro" else "Play"
      "share" -> if (isSomali) "La wadaag" else "Share"
      "delete" -> if (isSomali) "Tirtir" else "Delete"
      "delete_confirm" -> if (isSomali) "Ma hubtaa inaad tirtirto muuqaalkan?" else "Are you sure you want to delete this download?"
      "confirm" -> if (isSomali) "Haa, Tirtir" else "Yes, Delete"

      "player_title" -> if (isSomali) "Daaraha Muuqaalka" else "Video Player"
      "player_no_video" -> if (isSomali) "Ka dooro muuqaal liiska dajinta si aad u daawato" else "Select a video from your Downloads list to start playback"
      "player_loop" -> if (isSomali) "Ku Celceli" else "Loop"
      "player_open_external" -> if (isSomali) "Ku fur Barnaamij Kale" else "Open in External App"
      "back_to_downloads" -> if (isSomali) "Ku noqo Dajinta" else "Back to Downloads"

      "settings_title" -> if (isSomali) "Dejinta & Matoorka" else "Settings & Engine"
      "language" -> if (isSomali) "Luqadda" else "Language"
      "theme" -> if (isSomali) "Muuqaalka" else "Theme"
      "theme_system" -> if (isSomali) "Sida Taleefanka" else "System Default"
      "theme_light" -> if (isSomali) "Iftiin" else "Light Theme"
      "theme_dark" -> if (isSomali) "Madow" else "Dark Theme"

      "engine_section" -> if (isSomali) "Xogta Matoorka & yt-dlp" else "Download Engine & yt-dlp Status"
      "active_engine" -> if (isSomali) "Matoorka Shaqeynaya" else "Active Engine"
      "active_engine_desc" -> if (isSomali) "Matoor Toos ah oo Qalabka ku dhex shaqeeya (Pure Kotlin). Zero Server, 100% taleefankan." else "Native On-Device Extractor (Pure Kotlin). Zero external server, runs 100% on this device."
      "ytdlp_review_title" -> if (isSomali) "Qaab-dhismeedka yt-dlp" else "yt-dlp Architecture Review"
      "ytdlp_review_desc" -> if (isSomali) "yt-dlp wuxuu u baahan yahay Python buuxa (~80MB per ABI) ama Termux. VidMate Pro wuxuu isticmaalaa matoor toos ah oo taleefanka dhexdiisa ka soo saara qulqulka muuqaalka bilaa server." else "yt-dlp requires bundling a full CPython 3 runtime (~80MB per ABI) or Termux. VidMate Pro implements an extensible VideoDownloadEngine architecture. The current native extractor performs direct client-side stream resolution without Python overhead or external servers."
      "storage_location" -> if (isSomali) "Goobta Kaydinta" else "Storage Location"
      "storage_location_path" -> "Movies/VidMatePro (Android MediaStore)"
      "watermark_title" -> if (isSomali) "Hufnaanta Calaamadda (Watermark)" else "Watermark Transparency"
      "watermark_desc" -> if (isSomali) "Muuqaallada waxaa si toos ah looga soo dajiyaa shabakadda TikTok. Helitaanka muuqaal aan calaamad lahayn waxay ku xiran tahay sida TikTok u bixiyo feylka gobolkaaga." else "Videos are downloaded directly from TikTok's available web delivery endpoints. Availability of watermark-free feeds depends on TikTok's dynamic regional CDN delivery."

      else -> key
    }
  }
}
