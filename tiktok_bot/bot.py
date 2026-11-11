import asyncio
import logging
import os
import re
import tempfile
from pathlib import Path
from urllib.parse import urlparse

import yt_dlp
from telegram import Update
from telegram.constants import ChatAction
from telegram.ext import Application, CommandHandler, MessageHandler, ContextTypes, filters

# Bot token-ka waxaa laga akhriyaa Railway Variables oo keliya.
BOT_TOKEN = ""

MAX_FILE_BYTES = 49 * 1024 * 1024
logging.basicConfig(
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
    level=os.getenv("LOG_LEVEL", "INFO"),
)
logger = logging.getLogger("tiktok-downloader")

def valid_tiktok_url(text: str) -> bool:
    match = re.search(r"https?://\S+", text or "")
    if not match:
        return False
    host = (urlparse(match.group(0).rstrip(".,!?)]}")).hostname or "").lower()
    return host == "tiktok.com" or host.endswith(".tiktok.com")

def extract_url(text: str) -> str | None:
    match = re.search(r"https?://\S+", text or "")
    return match.group(0).rstrip(".,!?)]}") if match else None

def download_video(url: str, folder: str) -> Path:
    opts = {
        "outtmpl": str(Path(folder) / "%(id)s.%(ext)s"),
        "format": "best[filesize<=49000000]/best",
        "noplaylist": True,
        "quiet": True,
        "no_warnings": True,
        "max_filesize": MAX_FILE_BYTES,
        "socket_timeout": 25,
        "retries": 2,
    }
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
        "Isticmaal kaliya video aad leedahay ama fasax u haysato."
    )

async def handle_link(update: Update, context: ContextTypes.DEFAULT_TYPE):
    message = update.effective_message
    url = extract_url(message.text or "")
    if not url or not valid_tiktok_url(url):
        await message.reply_text("Fadlan soo dir link TikTok oo sax ah.")
        return
    status = await message.reply_text("Video-ga waan soo dejinayaa, sug fadlan...")
    try:
        await context.bot.send_chat_action(message.chat_id, ChatAction.UPLOAD_VIDEO)
        with tempfile.TemporaryDirectory(prefix="tiktok-bot-") as folder:
            path = await asyncio.to_thread(download_video, url, folder)
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
        await status.delete()
    except Exception:
        logger.exception("Video download/send failed")
        await status.edit_text(
            "Download-ku ma suurtagelin. Hubi link-ga, ama video-gu private/aan la taageerin/"
            "ka weyn 49 MB ayuu noqon karaa."
        )

def main():
    token = os.getenv("TELEGRAM_BOT_TOKEN", "").strip()
    if not token or token == "GELI_TOKEN_KAAGA_HALKAN":
        raise RuntimeError(
            "Token ma jiro. Geli Railway variable TELEGRAM_BOT_TOKEN."
        )
    app = Application.builder().token(token).build()
    app.add_handler(CommandHandler("start", start))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_link))
    logger.info("TikTok Telegram bot started; waiting for messages.")
    app.run_polling(drop_pending_updates=True)

if __name__ == "__main__":
    main()
