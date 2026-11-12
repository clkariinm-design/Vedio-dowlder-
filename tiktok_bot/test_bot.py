import unittest

from bot import QUALITY_FORMATS, extract_url, valid_tiktok_url


class BotValidationTests(unittest.TestCase):
    def test_accepts_tiktok_and_short_links(self):
        self.assertTrue(valid_tiktok_url("https://www.tiktok.com/@user/video/123"))
        self.assertTrue(valid_tiktok_url("https://vm.tiktok.com/abc/"))

    def test_rejects_lookalike_domains(self):
        self.assertFalse(valid_tiktok_url("https://tiktok.com.example.org/video/1"))
        self.assertFalse(valid_tiktok_url("https://not-tiktok.com/video/1"))
        self.assertFalse(valid_tiktok_url("not a url"))

    def test_extracts_url_and_removes_sentence_punctuation(self):
        self.assertEqual(
            extract_url("Fadlan eeg https://www.tiktok.com/@u/video/1."),
            "https://www.tiktok.com/@u/video/1",
        )

    def test_quality_profiles_exist(self):
        self.assertEqual(set(QUALITY_FORMATS), {"high", "medium", "low"})
        self.assertIn("1080", QUALITY_FORMATS["high"])
        self.assertIn("720", QUALITY_FORMATS["medium"])
        self.assertIn("480", QUALITY_FORMATS["low"])


if __name__ == "__main__":
    unittest.main()
