# Sports Prediction Telegram Bot (Demo)

This version replaces the TikTok downloader with a sports prediction game using virtual coins only.

## Railway deployment
1. Keep the existing Railway variable `TELEGRAM_BOT_TOKEN`.
2. Set `ADMIN_IDS` to your Telegram numeric user ID (comma-separated if multiple admins).
3. Railway installs `requirements.txt` and starts `python bot.py`.
4. Open the existing Telegram bot and send `/start`.

## Player commands
- `/start` - create/open your demo wallet (1,000 virtual coins for a new user).
- `/matches` - list sample demo fixtures and odds.
- `/bet demo1 home 100` - place a virtual-coin prediction. Outcomes: `home`, `draw`, `away`.
- `/wallet` - show balance.
- `/leaderboard` - top players.
- `/help` - instructions.

## Admin commands
- `/settle demo1 home` - settle a demo match; outcomes: home/draw/away.
- `/stats` - basic demo statistics.

## Configuration
- `TELEGRAM_BOT_TOKEN`: existing Telegram bot token.
- `ADMIN_IDS`: Telegram numeric user IDs.
- `GAME_DB_PATH`: SQLite database path; defaults to `game.db`.

## Important
This is a test game, not a real-money betting service. Virtual coins have no cash value. No deposits, cash withdrawals, real odds feed, or real match results are connected. The bundled fixtures and odds are sample data for testing only. SQLite data on Railway's ephemeral filesystem can be lost on redeploy/restart; configure a persistent Railway volume or managed database before using this beyond a disposable demo.
