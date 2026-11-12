import logging
import os

from telegram import Update
from telegram.ext import Application, CommandHandler, ContextTypes

from game import GameStore

logging.basicConfig(
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
    level=os.getenv("LOG_LEVEL", "INFO"),
)
logger = logging.getLogger("sports-prediction-demo")
STORE = GameStore(os.getenv("GAME_DB_PATH", "game.db"))
STORE.seed_demo_matches()


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
        "Ku soo dhowow Sports Prediction Demo!\n\n"
        "Waxaad ku ciyaaraysaa Virtual Coins oo tijaabo ah; lacag dhab ah lama dhigayo "
        "lamana bixinayo.\n"
        f"Balance-kaaga: {coins:,} coins\n\n"
        "Amarrada: /matches, /bet, /wallet, /leaderboard, /help"
    )


async def help_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.effective_message.reply_text(
        "Sida loo ciyaaro (DEMO):\n"
        "/matches - eeg ciyaaraha iyo odds-ka tijaabada\n"
        "/bet demo1 home 100 - dhig 100 virtual coins; dooro home, draw ama away\n"
        "/wallet - eeg coins-kaaga\n"
        "/leaderboard - eeg 10-ka ciyaaryahan ee ugu sarreeya\n\n"
        "Admin: /settle demo1 home (natiijada: home, draw ama away).\n"
        "Dhammaan coins-ku waa kuwo tijaabo ah, qiime lacageedna ma laha."
    )


async def matches_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    matches = STORE.matches()
    lines = ["Ciyaaraha demo iyo odds-ka:"]
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
    await update.effective_message.reply_text(f"Wallet-kaaga: {coins:,} Virtual Coins.")


async def bet_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if len(context.args) != 3:
        await update.effective_message.reply_text(
            "Qaabka: /bet demo1 home 100\nNatiijooyinka: home, draw, away."
        )
        return
    match_id, outcome = context.args[0], context.args[1].lower()
    try:
        stake = int(context.args[2])
        ticket = STORE.place_bet(update.effective_user.id, match_id, outcome, stake)
        balance = STORE.balance(update.effective_user.id)
        await update.effective_message.reply_text(
            f"Bet Slip #{ticket['bet_id']} waa la aqbalay.\n"
            f"Stake: {stake:,} coins\nOdds: {ticket['odds']:.2f}\n"
            f"Guusha suuragalka ah: {ticket['potential_win']:,} coins\n"
            f"Balance haray: {balance:,} coins."
        )
    except (ValueError, TypeError) as exc:
        await update.effective_message.reply_text(f"Bet lama aqbalin: {exc}")


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
            f"Ciyaarta {context.args[0]} waa la settle-gareeyey. "
            f"Users guuleystay: {winners}."
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


def main():
    token = os.getenv("TELEGRAM_BOT_TOKEN", "").strip()
    if not token or token == "GELI_TOKEN_KAAGA_HALKAN":
        raise RuntimeError("Token ma jiro. Geli Railway variable TELEGRAM_BOT_TOKEN.")
    app = Application.builder().token(token).build()
    app.add_handler(CommandHandler("start", start))
    app.add_handler(CommandHandler("help", help_command))
    app.add_handler(CommandHandler("matches", matches_command))
    app.add_handler(CommandHandler("bet", bet_command))
    app.add_handler(CommandHandler("wallet", wallet_command))
    app.add_handler(CommandHandler("leaderboard", leaderboard_command))
    app.add_handler(CommandHandler("settle", settle_command))
    app.add_handler(CommandHandler("stats", stats_command))
    logger.info("Sports Prediction Demo bot started; virtual coins only.")
    app.run_polling(drop_pending_updates=True)


if __name__ == "__main__":
    main()
