import unittest

import httpx

from research import ApiFootballClient, ResearchError


class ApiFootballClientTests(unittest.TestCase):
    def test_matches_normalizes_fixture_data(self):
        def handler(request):
            self.assertEqual(request.url.path, "/fixtures")
            return httpx.Response(200, json={"response": [{
                "fixture": {"id": 77, "date": "2026-11-12T18:00:00+00:00",
                            "status": {"long": "Not Started"}},
                "league": {"name": "Premier League"},
                "teams": {"home": {"name": "Arsenal"}, "away": {"name": "Chelsea"}},
                "goals": {"home": None, "away": None},
            }]})

        client = ApiFootballClient("test-key", transport=httpx.MockTransport(handler))
        result = client.matches("2026-11-12")
        self.assertEqual(result[0]["fixture_id"], 77)
        self.assertEqual(result[0]["home"], "Arsenal")
        self.assertEqual(result[0]["away"], "Chelsea")

    def test_missing_api_key_is_rejected(self):
        client = ApiFootballClient("")
        with self.assertRaisesRegex(ResearchError, "API_FOOTBALL_KEY"):
            client.matches("2026-11-12")

    def test_provider_errors_are_not_silently_ignored(self):
        def handler(request):
            return httpx.Response(200, json={"errors": {"token": "invalid"}})

        client = ApiFootballClient("bad-key", transport=httpx.MockTransport(handler))
        with self.assertRaisesRegex(ResearchError, "invalid"):
            client.matches("2026-11-12")


if __name__ == "__main__":
    unittest.main()
