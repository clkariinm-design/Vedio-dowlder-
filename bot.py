import asyncio
import logging
import os
import re
import tempfile
from pathlib import Path
from urllib.parse import urlparse

import yt_dlp
from telegram import InlineKeyboardButton, InlineKeyboardMarkup, Update
from telegram.constants import ChatAction
from telegram.ext import Application, CallbackQueryHandler, CommandHandler, ContextTypes, MessageHandler, filters

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("telegram-downloader")

TOKEN = os.environ["BOT_TOKEN"]
MAX_BYTES = 49 * 1024 * 1024
MAX_DURATION = int(os.getenv("MAX_DURATION_SECONDS", "3600"))
URL_RE = re.compile(r"^https?://", re.I)


def valid_url(value: str) -> bool:
    if not URL_RE.match(value.strip()):
        return False
    try:
        host = urlparse(value.strip()).hostname
        return bool(host and "." in host and " " not in host)
    except ValueError:
        return False


def download_media(url: str, mode: str, folder: str) -> Path:
    output = str(Path(folder) / "%(title).80s-%(id)s.%(ext)s")
    options = {
        "outtmpl": output,
        "noplaylist": True,
        "quiet": True,
        "no_warnings": True,
        "restrictfilenames": True,
        "max_filesize": MAX_BYTES,
        "socket_timeout": 30,
        "retries": 2,
        "extractor_retries": 2,
        "match_filter": yt_dlp.utils.match_filter_func(f"duration > {MAX_DURATION}"),
    }
    if mode == "audio":
        options.update({
            "format": "bestaudio/best",
            "postprocessors": [{
                "key": "FFmpegExtractAudio",
                "preferredcodec": "mp3",
                "preferredquality": "192",
            }],
        })
    else:
        options.update({
            "format": "best[ext=mp4][height<=720]/best[height<=720]/best",
            "merge_output_format": "mp4",
        })

    with yt_dlp.YoutubeDL(options) as ydl:
        info = ydl.extract_info(url, download=True)
        path = Path(ydl.prepare_filename(info))
        if mode == "audio":
            path = path.with_suffix(".mp3")
        if not path.exists():
            candidates = [p for p in Path(folder).iterdir() if p.is_file()]
            if not candidates:
                raise RuntimeError("Downloaded output file not found")
            path = candidates[0]
        if path.stat().st_size > MAX_BYTES:
            raise RuntimeError("File is larger than the Telegram upload limit")
        return path


async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text(
        "Salaan! Ii soo dir link video ama audio ah.\n"
        "Kadib dooro Video ama Audio/MP3.\n"
        "Isticmaal oo keliya content aad xaq u leedahay inaad soo dejiso."
    )


async def help_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text(
        f"Dir URL ka bilaabma https:// ama http://, kadib dooro Video ama Audio. "
        f"Xadka file-ka waa 49 MB; muddada ugu badan waa {MAX_DURATION // 60} daqiiqo."
    )


async def receive_url(update: Update, context: ContextTypes.DEFAULT_TYPE):
    url = (update.message.text or "").strip()
    if not valid_url(url):
        await update.message.reply_text("Fadlan soo dir link sax ah oo http:// ama https:// ku bilaabma.")
        return
    context.user_data["pending_url"] = url
    keyboard = [[
        InlineKeyboardButton("Video", callback_data="download:video"),
        InlineKeyboardButton("Audio / MP3", callback_data="download:audio"),
    ]]
    await update.message.reply_text("Dooro nooca aad rabto:", reply_markup=InlineKeyboardMarkup(keyboard))


async def choose_format(update: Update, context: ContextTypes.DEFAULT_TYPE):
    query = update.callback_query
    await query.answer()
    url = context.user_data.get("pending_url")
    if not url:
        await query.edit_message_text("Link-gu wuu dhacay. Mar kale ii soo dir.")
        return

    mode = query.data.split(":", 1)[1]
    await query.edit_message_text("Soo dejintu way bilaabatay. Fadlan sug...")
    chat_id = query.message.chat_id
    try:
        with tempfile.TemporaryDirectory(prefix="telegram-media-") as tempdir:
            path = await asyncio.to_thread(download_media, url, mode, tempdir)
            if mode == "audio":
                await context.bot.send_chat_action(chat_id, ChatAction.UPLOAD_AUDIO)
                with path.open("rb") as stream:
                    await context.bot.send_audio(chat_id, audio=stream, filename=path.name, caption="Waa kan audio-gaaga.")
            else:
                await context.bot.send_chat_action(chat_id, ChatAction.UPLOAD_VIDEO)
                with path.open("rb") as stream:
                    await context.bot.send_video(chat_id, video=stream, filename=path.name,
                                                 supports_streaming=True, caption="Waa kan video-gaaga.")
        await query.message.reply_text("Waa dhammaatay. Link kale waad soo diri kartaa.")
    except Exception:
        log.exception("Media download failed")
        await query.message.reply_text(
            "Soo dejintu way fashilantay. Hubi link-ga, in website-ku oggol yahay, "
            "iyo in file-ku ka yar yahay 49 MB."
        )
    finally:
        context.user_data.pop("pending_url", None)


def main():
    app = Application.builder().token(TOKEN).build()
    app.add_handler(CommandHandler("start", start))
    app.add_handler(CommandHandler("help", help_command))
    app.add_handler(CallbackQueryHandler(choose_format, pattern=r"^download:(video|audio)$"))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, receive_url))
    log.info("Telegram downloader bot is starting")
    app.run_polling(drop_pending_updates=True)


if __name__ == "__main__":
    main()
