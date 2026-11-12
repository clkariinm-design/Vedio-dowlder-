"""Read-only football research integration using API-Football.

No wagers, deposits, wallet balances, or guaranteed outcomes are handled here.
"""
from __future__ import annotations

import os
from datetime import datetime, timezone
from typing import Any

import httpx


BASE_URL = "https://v3.football.api-sports.io"


class ResearchError(RuntimeError):
    """Raised when a sports data request cannot be completed."""


class ApiFootballClient:
    def __init__(
        self,
        api_key: str | None = None,
        *,
        transport: httpx.BaseTransport | None = None,
        timeout: float = 15.0,
    ) -> None:
        self.api_key = (api_key if api_key is not None else os.getenv("API_FOOTBALL_KEY", "")).strip()
        self.transport = transport
        self.timeout = timeout

    def _get(self, endpoint: str, params: dict[str, Any]) -> dict[str, Any]:
        if not self.api_key:
            raise ResearchError("API_FOOTBALL_KEY lama dejin. Ku dar Railway Variables.")
        try:
            with httpx.Client(
                base_url=BASE_URL,
                headers={"x-apisports-key": self.api_key},
                timeout=self.timeout,
                transport=self.transport,
            ) as client:
                response = client.get(endpoint, params=params)
                response.raise_for_status()
                payload = response.json()
        except (httpx.HTTPError, ValueError) as exc:
            raise ResearchError(f"Adeegga xogta ciyaaraha lama heli karo: {exc}") from exc

        errors = payload.get("errors")
        if errors:
            raise ResearchError(f"API-Football error: {errors}")
        return payload

    def matches(self, date: str) -> list[dict[str, Any]]:
        payload = self._get("/fixtures", {"date": date, "timezone": "Africa/Mogadishu"})
        matches: list[dict[str, Any]] = []
        for item in payload.get("response", []):
            fixture = item.get("fixture", {})
            teams = item.get("teams", {})
            league = item.get("league", {})
            goals = item.get("goals", {})
            matches.append({
                "fixture_id": fixture.get("id"),
                "date": fixture.get("date"),
                "status": (fixture.get("status") or {}).get("long"),
                "league": league.get("name"),
                "home": (teams.get("home") or {}).get("name"),
                "away": (teams.get("away") or {}).get("name"),
                "home_goals": goals.get("home"),
                "away_goals": goals.get("away"),
            })
        return matches

    def analyze(self, fixture_id: int) -> dict[str, Any]:
        fixture_payload = self._get("/fixtures", {"id": fixture_id})
        fixtures = fixture_payload.get("response", [])
        if not fixtures:
            raise ResearchError("Ciyaartan lagama helin xogta API-Football.")

        fixture = fixtures[0]
        teams = fixture.get("teams", {})
        home = teams.get("home") or {}
        away = teams.get("away") or {}

        predictions_payload = self._get("/predictions", {"fixture": fixture_id})
        prediction_rows = predictions_payload.get("response", [])
        prediction = prediction_rows[0] if prediction_rows else {}
        prediction_data = prediction.get("predictions") or {}

        injuries_payload = self._get("/injuries", {"fixture": fixture_id})
        injuries = []
        for item in injuries_payload.get("response", []):
            player = item.get("player") or {}
            team = item.get("team") or {}
            injuries.append({
                "player": player.get("name"),
                "team": team.get("name"),
                "reason": player.get("reason"),
                "type": player.get("type"),
            })

        return {
            "fixture_id": fixture_id,
            "home": home.get("name"),
            "away": away.get("name"),
            "league": (fixture.get("league") or {}).get("name"),
            "kickoff": (fixture.get("fixture") or {}).get("date"),
            "prediction": {
                "winner": (prediction_data.get("winner") or {}).get("name"),
                "advice": prediction_data.get("advice"),
                "under_over": prediction_data.get("under_over"),
                "percent": prediction_data.get("percent"),
                "win_or_draw": prediction_data.get("win_or_draw"),
            } if prediction_data else None,
            "comparison": prediction.get("comparison"),
            "injuries": injuries,
            "prediction_available": bool(prediction_data),
            "source": "API-Football",
            "retrieved_at": datetime.now(timezone.utc).isoformat(),
            "notice": "Saadaal xog ku salaysan; ma aha dammaanad guul ama talo maaliyadeed.",
        }
