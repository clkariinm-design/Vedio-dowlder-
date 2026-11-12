# Fikiraya Sports Research Bot + API

Football-first research backend integrated with the existing Telegram bot. This is a read-only sports information and analysis tool. It does not place real-money bets or accept deposits.

## Features

- `/today`: today's football fixtures from API-Football, with fixture IDs.
- `/analyse FIXTURE_ID`: provider prediction, available comparison data, and recorded injuries.
- HTTP API: `GET /health`, `GET /matches?date=YYYY-MM-DD`, and `GET /analysis/{fixture_id}`.
- Existing virtual-coin demo commands remain separate from real sports data.

## Railway configuration

Keep:
- `TELEGRAM_BOT_TOKEN`
- `ADMIN_IDS` (Telegram numeric IDs, comma-separated)

Add:
- `API_FOOTBALL_KEY`: API key from https://www.api-football.com/
- `PORT`: Railway normally injects this automatically.

Start command remains `python bot.py`. The process starts the Telegram polling bot and the HTTP API on Railway's assigned port.

## API routes

- `GET /health`
- `GET /matches?date=2026-11-12`
- `GET /analysis/123456`

The API uses the API-Football v3 endpoints for fixtures, predictions and injuries. Provider coverage varies by competition and season; a missing field is reported as unavailable rather than invented.

## Research quality and limitations

- Provider predictions are third-party outputs, not a model trained or independently validated by this project.
- No outcome is guaranteed. The system is not financial advice.
- Before presenting any model as reliable, collect historical predictions and final results, then evaluate calibration using Brier score and log loss. Avoid look-ahead bias and use time-based train/test splits.
- The first version is football-only. Additional sports require sport-specific providers and separate evaluation.
- Do not connect real-money deposits, payouts, or bookmaker execution to this research service.

## Data provider

API-Football: https://www.api-football.com/documentation-v3
