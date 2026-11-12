import os
import tempfile
import unittest

from game import GameStore


class GameStoreTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.store = GameStore(os.path.join(self.tmp.name, "test.db"))
        self.store.add_match("m1", "Team A", "Team B", 2.0, 3.0, 4.0)

    def tearDown(self):
        self.tmp.cleanup()

    def test_new_user_receives_demo_coins(self):
        self.assertEqual(self.store.balance(101), 1000)

    def test_place_bet_reserves_stake_and_returns_ticket(self):
        ticket = self.store.place_bet(101, "m1", "home", 100)
        self.assertEqual(ticket["potential_win"], 200)
        self.assertEqual(self.store.balance(101), 900)

    def test_rejects_stake_above_balance(self):
        with self.assertRaises(ValueError):
            self.store.place_bet(101, "m1", "home", 1001)

    def test_settlement_pays_winner_once(self):
        self.store.place_bet(101, "m1", "home", 100)
        self.assertEqual(self.store.settle("m1", "home"), 1)
        self.assertEqual(self.store.balance(101), 1100)
        with self.assertRaises(ValueError):
            self.store.settle("m1", "home")

    def test_losing_bet_does_not_pay(self):
        self.store.place_bet(101, "m1", "away", 100)
        self.store.settle("m1", "home")
        self.assertEqual(self.store.balance(101), 900)


if __name__ == "__main__":
    unittest.main()
