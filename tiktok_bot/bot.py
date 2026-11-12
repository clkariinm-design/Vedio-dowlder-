import asyncio
import logging
import os
import re
import tempfile
import time
from collections import defaultdict
from pathlib import Path
from urllib.parse import urlparse

import yt_dlp
from telegram import Update
from telegram.constants import ChatAction
from telegram.ext import Application, CommandHandler, MessageHandler, ContextTypes, filters

MAX_FILE_BYTES = 49 * 1024 * 1024
MAX_QUEUE_SIZE = max(1, int(os.getenv("MAX_QUEUE_SIZE", "20")))
MAX_CONCURRENT_DOWNLOADS = max(1, int(os.getenv("MAX_CONCURRENT_DOWNLOADS", "2")))
USER_COOLDOWN_SECONDS = max(0, int(os.getenv("USER_COOLDOWN_SECONDS", "15")))
DOWNLOAD_SLOTS = asyncio.Semaphore(MAX_CONCURRENT_DOWNLOADS)
QUEUE_LOCK = asyncio.Lock()
QUEUED_JOBS = 0
ACTIVE_DOWNLOADS = 0
LAST_REQUEST_AT = {}
STATS = {"requests": 0, "completed": 0, "failed": 0}
KNOWN_USERS = set()

QUALITY_FORMATS = {
    "high": "best[height<=1080][filesize<=49000000]/best[height<=1080]/best",
    "medium": "best[height<=720][filesize<=49000000]/best[height<=720]/best",
    "low": "best[height<=480][filesize<=49000000]/best[height<=480]/worst",
}

logging.basicConfig(
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
    level=os.getenv("LOG_LEVEL", "INFO"),
)
logger = logging.getLogger("tiktok-downloader")


def admin_ids() -> set[int]:
    result = set()
    for value in os.getenv("ADMIN_IDS", "").split(","):
        value = value.strip()
        if value.isdigit():
            result.add(int(value))
    return result


def valid_tiktok_url(text: str) -> bool:
    url = extract_url(text)
    if not url:
        return False
    host = (urlparse(url).hostname or "").lower()
    return host == "tiktok.com" or host.endswith(".tiktok.com")


def extract_url(text: str) -> str | None:
    match = re.search(r"https?://\S+", text or "")
    return match.group(0).rstrip(".,!?)]}") if match else None


def download_video(url: str, folder: str, quality: str = "high", progress_hook=None) -> Path:
    opts = {
        "outtmpl": str(Path(folder) / "%(id)s.%(ext)s"),
        "format": QUALITY_FORMATS.get(quality, QUALITY_FORMATS["high"]),
        "noplaylist": True,
        "quiet": True,
        "no_warnings": True,
        "max_filesize": MAX_FILE_BYTES,
        "socket_timeout": 25,
        "retries": 3,
        "fragment_retries": 3,
    }
    if progress_hook:
        opts["progress_hooks"] = [progress_hook]
    with yt_dlp.YoutubeDL(opts) as ydl:
        info = ydl.extract_info(url, download=True)
        filename = ydl.prepare_filename(info)
    path = Path(filename)
    if not path.exists():
        files = list(Path(folder).glob("*"))
        if not files:
            raise RuntimeError("Video file lama helin.")
        path = files[0]
    if path.stat().st_size > MAX_FILE_BYTES:
        raise ValueError("Video-gu wuu ka weyn yahay 49 MB.")
    return path


async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.effective_message.reply_text(
        "Ku soo dhowow TikTok Video Downloader!\n"
        "Ii soo dir link TikTok ah si aan video-ga kuugu soo diro.\n"
        "Tayada beddel: /quality high, /quality medium, ama /quality low.\n"
        "Caawimaad: /help\n"
        "Isticmaal kaliya video aad leedahay ama fasax u haysato."
    )


async def help_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.effective_message.reply_text(
        "Sida loo isticmaalo:\n"
        "1. Ii soo dir link TikTok oo sax ah.\n"
        "2. Tayada dooro: /quality high, /quality medium, /quality low.\n"
        "3. Sug inta video-ga la soo dejinayo laguna soo dirayo.\n\n"
        "Xadka faylka Telegram: 49 MB. Private ama content xaddidan lama soo dejin karo."
    )


async def quality_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    choice = context.args[0].lower() if context.args else ""
    if choice not in QUALITY_FORMATS:
        await update.effective_message.reply_text(
            "Dooro tayada: /quality high, /quality medium, ama /quality low."
        )
        return
    context.user_data["quality"] = choice
    await update.effective_message.reply_text(f"Tayada download-ka waxaa loo dejiyey: {choice}.")


async def stats_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user = update.effective_user
    if not user or user.id not in admin_ids():
        await update.effective_message.reply_text("Amarkan waxaa isticmaali kara admin-ka oo keliya.")
        return
    async with QUEUE_LOCK:
        queued = QUEUED_JOBS
        active = ACTIVE_DOWNLOADS
    await update.effective_message.reply_text(
        "Tirakoobka bot-ka (tan iyo restart-kii ugu dambeeyey):\n"
        f"Users: {len(KNOWN_USERS)}\n"
        f"Codsiyada: {STATS['requests']}\n"
        f"Guulaystay: {STATS['completed']}\n"
        f fashilmay: {STATS['failed']}\n"
        f"Hadda socda: {active}\n"
        f"Safka ku jira: {queued}"
    )


async def handle_link(update: Update, context: ContextTypes.DEFAULT_TYPE):
    global QUEUED_JOBS, ACTIVE_DOWNLOADS
    message = update.effective_message
    user = update.effective_user
    url = extract_url(message.text or "")
    if not url or not valid_tiktok_url(url):
        await message.reply_text("Fadlan soo dir link TikTok oo sax ah.")
        return

    now = time.monotonic()
    user_id = user.id if user else message.chat_id
    previous = LAST_REQUEST_AT.get(user_id, 0)
    remaining = USER_COOLDOWN_SECONDS - (now - previous)
    if remaining > 0:
        await message.reply_text(f"Fadlan sug {int(remaining) + 1} ilbiriqsi ka hor codsi kale.")
        return
    LAST_REQUEST_AT[user_id] = now
    STATS["requests"] += 1
    if user:
        KNOWN_USERS.add(user.id)

    async with QUEUE_LOCK:
        if QUEUED_JOBS >= MAX_QUEUE_SIZE:
            await message.reply_text("Safka download-ku wuu buuxaa. Fadlan mar kale isku day wax yar kadib.")
            return
        QUEUED_JOBS += 1

    quality = context.user_data.get("quality", "high")
    status = await message.reply_text("Codsigaaga safka ayaa la geliyey. Fadlan sug...")
    is_queued = True
    is_active = False
    try:
        async with DOWNLOAD_SLOTS:
            async with QUEUE_LOCK:
                QUEUED_JOBS -= 1
                is_queued = False
                ACTIVE_DOWNLOADS += 1
                is_active = True

            await status.edit_text(f"Video-ga waan soo dejinayaa ({quality}).")
            loop = asyncio.get_running_loop()
            last_bucket = [-1]

            def progress_hook(data):
                if data.get("status") != "downloading":
                    return
                total = data.get("total_bytes") or data.get("total_bytes_estimate")
                downloaded = data.get("downloaded_bytes", 0)
                if not total:
                    return
                percent = int(downloaded * 100 / total)
                bucket = min(90, (percent // 10) * 10)
                if bucket <= last_bucket[0]:
                    return
                last_bucket[0] = bucket

                def schedule_update():
                    async def update_status():
                        try:
                            await status.edit_text(f"Video-ga waa la soo dejinayaa: {percent}%")
                        except Exception:
                            pass
                    asyncio.create_task(update_status())

                loop.call_soon_threadsafe(schedule_update)

            await context.bot.send_chat_action(message.chat_id, ChatAction.TYPING)
            with tempfile.TemporaryDirectory(prefix="tiktok-bot-") as folder:
                path = await asyncio.to_thread(download_video, url, folder, quality, progress_hook)
                await status.edit_text("Download-ku wuu dhammaaday. Video-ga Telegram ayaan kuu soo dirayaa...")
                await context.bot.send_chat_action(message.chat_id, ChatAction.UPLOAD_VIDEO)
                with path.open("rb") as video:
                    await context.bot.send_video(
                        chat_id=message.chat_id,
                        video=video,
                        supports_streaming=True,
                        caption="Waa kan video-gaaga.",
                        read_timeout=120,
                        write_timeout=120,
                        connect_timeout=30,
                    )
            STATS["completed"] += 1
            await status.delete()
    except Exception as exc:
        STATS["failed"] += 1
        logger.exception("Video download/send failed (%s)", type(exc).__name__)
        try:
            await status.edit_text(
                "Download-ku ma suurtagelin. Sababaha suuragalka ah: link khaldan, video private/"
                "aan la taageerin, ama fayl ka weyn 49 MB. Fadlan hubi link-ga oo mar kale isku day."
            )
        except Exception:
            pass
    finally:
        async with QUEUE_LOCK:
            if is_queued:
                QUEUED_JOBS = max(0, QUEUED_JOBS - 1)
            if is_active:
                ACTIVE_DOWNLOADS = max(0, ACTIVE_DOWNLOADS - 1)


def main():
    token = os.getenv("TELEGRAM_BOT_TOKEN", "").strip()
    if not token or token == "GELI_TOKEN_KAAGA_HALKAN":
        raise RuntimeError("Token ma jiro. Geli Railway variable TELEGRAM_BOT_TOKEN.")
    app = Application.builder().token(token).concurrent_updates(16).build()
    app.add_handler(CommandHandler("start", start))
    app.add_handler(CommandHandler("help", help_command))
    app.add_handler(CommandHandler("quality", quality_command))
    app.add_handler(CommandHandler("stats", stats_command))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_link))
    logger.info(
        "TikTok Telegram bot started (max concurrent downloads=%s, queue=%s).",
        MAX_CONCURRENT_DOWNLOADS,
        MAX_QUEUE_SIZE,
    )
    app.run_polling(drop_pending_updates=True)


if __name__ == "__main__":
    main()
