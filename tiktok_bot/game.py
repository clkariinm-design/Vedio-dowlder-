"""Virtual-coin sports prediction demo. No real-money deposits or payouts."""
import sqlite3
from contextlib import contextmanager

STARTING_COINS = 1000
OUTCOMES = {"home", "draw", "away"}


class GameStore:
    def __init__(self, path="game.db"):
        self.path = path
        with self._db() as db:
            db.executescript("""
                CREATE TABLE IF NOT EXISTS wallets (
                    user_id INTEGER PRIMARY KEY,
                    coins INTEGER NOT NULL
                );
                CREATE TABLE IF NOT EXISTS matches (
                    match_id TEXT PRIMARY KEY,
                    home TEXT NOT NULL,
                    away TEXT NOT NULL,
                    home_odds REAL NOT NULL,
                    draw_odds REAL NOT NULL,
                    away_odds REAL NOT NULL,
                    result TEXT,
                    settled INTEGER NOT NULL DEFAULT 0
                );
                CREATE TABLE IF NOT EXISTS bets (
                    bet_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    match_id TEXT NOT NULL,
                    outcome TEXT NOT NULL,
                    stake INTEGER NOT NULL,
                    odds REAL NOT NULL,
                    potential_win INTEGER NOT NULL,
                    status TEXT NOT NULL DEFAULT 'pending'
                );
            """)

    @contextmanager
    def _db(self):
        db = sqlite3.connect(self.path, timeout=15)
        db.row_factory = sqlite3.Row
        try:
            yield db
            db.commit()
        except Exception:
            db.rollback()
            raise
        finally:
            db.close()

    def add_match(self, match_id, home, away, home_odds, draw_odds, away_odds):
        with self._db() as db:
            db.execute(
                """INSERT OR REPLACE INTO matches
                (match_id, home, away, home_odds, draw_odds, away_odds, result, settled)
                VALUES (?, ?, ?, ?, ?, ?, NULL, 0)""",
                (match_id, home, away, home_odds, draw_odds, away_odds),
            )

    def seed_demo_matches(self):
        with self._db() as db:
            count = db.execute("SELECT COUNT(*) FROM matches").fetchone()[0]
        if count == 0:
            self.add_match("demo1", "Blue FC", "Red United", 2.10, 3.20, 3.40)
            self.add_match("demo2", "City Stars", "Ocean FC", 1.85, 3.50, 4.10)
            self.add_match("demo3", "Lions", "Eagles", 2.40, 3.00, 2.80)

    def balance(self, user_id):
        with self._db() as db:
            db.execute(
                "INSERT OR IGNORE INTO wallets(user_id, coins) VALUES (?, ?)",
                (user_id, STARTING_COINS),
            )
            return db.execute(
                "SELECT coins FROM wallets WHERE user_id=?", (user_id,)
            ).fetchone()["coins"]

    def matches(self):
        with self._db() as db:
            return [dict(row) for row in db.execute(
                "SELECT * FROM matches ORDER BY match_id"
            ).fetchall()]

    def place_bet(self, user_id, match_id, outcome, stake):
        if outcome not in OUTCOMES:
            raise ValueError("Natiijo aan sax ahayn.")
        if not isinstance(stake, int) or stake <= 0:
            raise ValueError("Coins-ku waa inay noqdaan tiro ka weyn eber.")
        with self._db() as db:
            match = db.execute(
                "SELECT * FROM matches WHERE match_id=?", (match_id,)
            ).fetchone()
            if not match or match["settled"]:
                raise ValueError("Ciyaartan lama heli karo ama waa la xiray.")
            db.execute(
                "INSERT OR IGNORE INTO wallets(user_id, coins) VALUES (?, ?)",
                (user_id, STARTING_COINS),
            )
            wallet = db.execute(
                "SELECT coins FROM wallets WHERE user_id=?", (user_id,)
            ).fetchone()
            if wallet["coins"] < stake:
                raise ValueError("Coins kugu filan ma haysid.")
            odds = float(match[{"home": "home_odds", "draw": "draw_odds", "away": "away_odds"}[outcome]])
            potential = int(stake * odds)
            db.execute("UPDATE wallets SET coins=coins-? WHERE user_id=?", (stake, user_id))
            cursor = db.execute(
                """INSERT INTO bets(user_id, match_id, outcome, stake, odds, potential_win)
                VALUES (?, ?, ?, ?, ?, ?)""",
                (user_id, match_id, outcome, stake, odds, potential),
            )
            return {"bet_id": cursor.lastrowid, "odds": odds, "potential_win": potential}

    def settle(self, match_id, result):
        if result not in OUTCOMES:
            raise ValueError("Natiijadu waa inay noqotaa home, draw ama away.")
        with self._db() as db:
            match = db.execute(
                "SELECT settled FROM matches WHERE match_id=?", (match_id,)
            ).fetchone()
            if not match:
                raise ValueError("Match lama helin.")
            if match["settled"]:
                raise ValueError("Match-kan horay ayaa loo settle-gareeyey.")
            db.execute(
                "UPDATE matches SET result=?, settled=1 WHERE match_id=?",
                (result, match_id),
            )
            winners = db.execute(
                "SELECT user_id, SUM(potential_win) AS payout FROM bets "
                "WHERE match_id=? AND outcome=? AND status='pending' GROUP BY user_id",
                (match_id, result),
            ).fetchall()
            for winner in winners:
                db.execute(
                    "UPDATE wallets SET coins=coins+? WHERE user_id=?",
                    (winner["payout"], winner["user_id"]),
                )
            db.execute(
                "UPDATE bets SET status=CASE WHEN outcome=? THEN 'won' ELSE 'lost' END "
                "WHERE match_id=? AND status='pending'",
                (result, match_id),
            )
            return len(winners)

    def leaderboard(self, limit=10):
        with self._db() as db:
            return [dict(row) for row in db.execute(
                "SELECT user_id, coins FROM wallets ORDER BY coins DESC LIMIT ?",
                (limit,),
            ).fetchall()]
