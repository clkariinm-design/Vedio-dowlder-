# Telegram Video & Audio Downloader

Telegram bot built with Python, python-telegram-bot, yt-dlp, and FFmpeg.

## Features
- Accepts a supported video/audio URL in Telegram.
- Lets the user choose Video or Audio/MP3.
- Sends the downloaded media back in Telegram.
- Uses temporary storage; downloaded files are removed after sending.
- Uses a 49 MB upload guard and a configurable duration limit.

## Deploy on Render
1. Connect this repository to Render as a Blueprint.
2. Render reads `render.yaml` and proposes a Background Worker.
3. Set the required `BOT_TOKEN` environment variable using the token from Telegram @BotFather.
4. Review the Blueprint plan and its price before approving the first sync.
5. After approval, Render builds the Docker image and runs the worker.

## Environment
- `BOT_TOKEN`: required Telegram bot token. Never commit it to Git.
- `MAX_DURATION_SECONDS`: optional; defaults to 3600 (60 minutes).

## Local run
Install FFmpeg, then run:

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
export BOT_TOKEN="YOUR_TELEGRAM_BOT_TOKEN"
python bot.py
```

## Limitations
- Some sites block automated downloads or require login; those URLs may not work.
- Only download media you own or are authorized to download. Respect copyright and each site's terms.
- Telegram's standard Bot API upload limit is treated as 49 MB by this application.
- Run only one polling instance for a given bot token.
