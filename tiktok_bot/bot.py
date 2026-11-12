import asyncio
import logging
import os
import threading
from datetime import datetime
from zoneinfo import ZoneInfo

import uvicorn
from telegram import Update
from telegram.ext import Application, CommandHandler, ContextTypes

from game import GameStore
from research import ApiFootballClient, ResearchError
from web_api import app as research_api

logging.basicConfig(
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
    level=os.getenv("LOG_LEVEL", "INFO"),
)
logger = logging.getLogger("fikiraya-sports-research")
STORE = GameStore(os.getenv("GAME_DB_PATH", "game.db"))
STORE.seed_demo_matches()
RESEARCH = ApiFootballClient(os.getenv("API_FOOTBALL_KEY", ""))


def admin_ids():
    return {
        int(value.strip())
        for value in os.getenv("ADMIN_IDS", "").split(",")
        if value.strip().isdigit()
    }


async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user = update.effective_user
    coins = STORE.balance(user.id)
    await update.effective_message.reply_text(
        "Ku soo dhowow Fikiraya Sports Research!\n\n"
        "Waxaan falanqeeyaa xogta kubadda cagta, kulamada iyo saadaasha xog-ku-salaysan.\n"
        "Saadaaluhu ma dammaanad qaadaan guul, mana aha talo maaliyadeed.\n\n"
        f"Demo balance: {coins:,} virtual coins\n"
        "Amarrada: /today, /analyse, /matches, /help"
    )


async def help_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.effective_message.reply_text(
        "Fikiraya Sports Research\n\n"
        "/today - ciyaaraha maanta iyo fixture ID\n"
        "/analyse 123456 - falanqaynta ciyaar adigoo isticmaalaya fixture ID\n"
        "/matches - ciyaaraha demo ee virtual coins\n"
        "/bet demo1 home 100 - demo keliya, lacag dhab ah ma aha\n"
        "/wallet - eeg virtual coins-ka demo\n"
        "/leaderboard - kala sarreynta demo\n\n"
        "Saadaashu waa xog iyo qiyaas, mana aha dammaanad guul."
    )


async def today_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    try:
        today = datetime.now(ZoneInfo("Africa/Mogadishu")).date().isoformat()
        matches = await asyncio.to_thread(RESEARCH.matches, today)
        if not matches:
            await update.effective_message.reply_text(
                f"Ma helin ciyaaro xogtooda la heli karo maanta ({today})."
            )
            return
        lines = [f"Ciyaaraha maanta ({today}) — xogta API-Football:"]
        for item in matches[:25]:
            lines.append(
                f"#{item['fixture_id']} | {item['home']} vs {item['away']}\n"
                f"{item['league']} | {item['date']} | {item['status']}"
            )
        await update.effective_message.reply_text("\n\n".join(lines)[:4000])
    except ResearchError as exc:
        await update.effective_message.reply_text(f"Xogta lama helin: {exc}")


async def analyse_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if len(context.args) != 1 or not context.args[0].isdigit():
        await update.effective_message.reply_text(
            "Qaabka: /analyse 123456\nIsticmaal fixture ID-ga ka muuqda /today."
        )
        return
    fixture_id = int(context.args[0])
    try:
        result = await asyncio.to_thread(RESEARCH.analyze, fixture_id)
        prediction = result.get("prediction")
        lines = [
            f"Falanqaynta: {result.get('home')} vs {result.get('away')}",
            f"Tartanka: {result.get('league') or 'Xog lama hayo'}",
            f"Bilowga: {result.get('kickoff') or 'Xog lama hayo'}",
        ]
        if prediction:
            lines.extend([
                f"Saadaasha provider-ka: {prediction.get('winner') or 'Aan la cayimin'}",
                f"Talo xogeed: {prediction.get('advice') or 'Ma jiro'}",
                f"Under/Over: {prediction.get('under_over') or 'Ma jiro'}",
                f"Boqolleyda provider-ka: {prediction.get('percent') or 'Lama hayo'}",
            ])
        else:
            lines.append("Saadaal provider oo diyaar ah lama helin ciyaartan.")
        lines.append(f"Dhaawacyo la diiwaangeliyey: {len(result.get('injuries', []))}")
        lines.append(f"Isha xogta: {result.get('source')}")
        lines.append("Ogow: saadaal ma aha dammaanad guul; hubi xogta ka hor go'aan.")
        await update.effective_message.reply_text("\n".join(lines)[:4000])
    except ResearchError as exc:
        await update.effective_message.reply_text(f"Falanqaynta lama helin: {exc}")


async def matches_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    matches = STORE.matches()
    lines = ["Ciyaaraha demo iyo odds-ka virtual:"]
    for match in matches:
        state = " (la xiray)" if match["settled"] else ""
        lines.append(
            f'{match["match_id"]}: {match["home"]} vs {match["away"]}{state}\n'
            f'  1: {match["home_odds"]:.2f} | X: {match["draw_odds"]:.2f} | '
            f'2: {match["away_odds"]:.2f}'
        )
    await update.effective_message.reply_text("\n".join(lines))


async def wallet_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    coins = STORE.balance(update.effective_user.id)
    await update.effective_message.reply_text(f"Wallet-ka demo: {coins:,} Virtual Coins.")


async def bet_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if len(context.args) != 3:
        await update.effective_message.reply_text(
            "Qaabka: /bet demo1 home 100\nDemo virtual coins oo keliya."
        )
        return
    match_id, outcome = context.args[0], context.args[1].lower()
    try:
        stake = int(context.args[2])
        ticket = STORE.place_bet(update.effective_user.id, match_id, outcome, stake)
        balance = STORE.balance(update.effective_user.id)
        await update.effective_message.reply_text(
            f"Demo Bet Slip #{ticket['bet_id']} waa la aqbalay.\n"
            f"Stake: {stake:,} virtual coins\nOdds: {ticket['odds']:.2f}\n"
            f"Guusha suuragalka ah: {ticket['potential_win']:,} coins\n"
            f"Balance haray: {balance:,} coins."
        )
    except (ValueError, TypeError) as exc:
        await update.effective_message.reply_text(f"Demo bet lama aqbalin: {exc}")


async def leaderboard_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    rows = STORE.leaderboard()
    if not rows:
        await update.effective_message.reply_text("Leaderboard-ku weli waa madhan yahay.")
        return
    lines = ["Leaderboard (Virtual Coins):"]
    for index, row in enumerate(rows, 1):
        lines.append(f"{index}. User {row['user_id']}: {row['coins']:,} coins")
    await update.effective_message.reply_text("\n".join(lines))


async def settle_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user = update.effective_user
    if not user or user.id not in admin_ids():
        await update.effective_message.reply_text("Amarkan waxaa isticmaali kara admin-ka oo keliya.")
        return
    if len(context.args) != 2:
        await update.effective_message.reply_text("Qaabka: /settle demo1 home")
        return
    try:
        winners = STORE.settle(context.args[0], context.args[1].lower())
        await update.effective_message.reply_text(
            f"Ciyaarta demo {context.args[0]} waa la settle-gareeyey. Users guuleystay: {winners}."
        )
    except ValueError as exc:
        await update.effective_message.reply_text(f"Settlement ma suurtagelin: {exc}")


async def stats_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user = update.effective_user
    if not user or user.id not in admin_ids():
        await update.effective_message.reply_text("Amarkan waxaa isticmaali kara admin-ka oo keliya.")
        return
    matches = STORE.matches()
    await update.effective_message.reply_text(
        f"Demo matches: {len(matches)}\n"
        f"Settled: {sum(1 for item in matches if item['settled'])}\n"
        f"Wallets: {len(STORE.leaderboard(limit=100000))}"
    )


def start_http_api():
    port = int(os.getenv("PORT", "8000"))
    server = threading.Thread(
        target=uvicorn.run,
        args=(research_api,),
        kwargs={"host": "0.0.0.0", "port": port, "log_level": "info"},
        daemon=True,
        name="sports-research-api",
    )
    server.start()
    logger.info("Sports Research HTTP API starting on port %s", port)


def main():
    token = os.getenv("TELEGRAM_BOT_TOKEN", "").strip()
    if not token or token == "GELI_TOKEN_KAAGA_HALKAN":
        raise RuntimeError("Token ma jiro. Geli Railway variable TELEGRAM_BOT_TOKEN.")
    if os.getenv("PORT"):
        start_http_api()

    app = Application.builder().token(token).build()
    app.add_handler(CommandHandler("start", start))
    app.add_handler(CommandHandler("help", help_command))
    app.add_handler(CommandHandler("today", today_command))
    app.add_handler(CommandHandler("analyse", analyse_command))
    app.add_handler(CommandHandler("matches", matches_command))
    app.add_handler(CommandHandler("bet", bet_command))
    app.add_handler(CommandHandler("wallet", wallet_command))
    app.add_handler(CommandHandler("leaderboard", leaderboard_command))
    app.add_handler(CommandHandler("settle", settle_command))
    app.add_handler(CommandHandler("stats", stats_command))
    logger.info("Fikiraya Sports Research bot started; demo coins only.")
    app.run_polling(drop_pending_updates=True)


if __name__ == "__main__":
    main()
