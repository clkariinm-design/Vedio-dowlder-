import unittest

from game import GameStore


class BotSmokeTests(unittest.TestCase):
    def test_demo_matches_can_be_seeded(self):
        store = GameStore(":memory:")
        store.seed_demo_matches()
        self.assertEqual(len(store.matches()), 3)


if __name__ == "__main__":
    unittest.main()
