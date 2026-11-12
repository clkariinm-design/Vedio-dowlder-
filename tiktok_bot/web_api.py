"""Small HTTP API exposing read-only football fixtures and research."""
from __future__ import annotations

import os

from fastapi import FastAPI, HTTPException, Query

from research import ApiFootballClient, ResearchError

app = FastAPI(
    title="Fikiraya Sports Research API",
    version="0.1.0",
    description="Read-only football fixtures and pre-match research.",
)
client = ApiFootballClient(os.getenv("API_FOOTBALL_KEY", ""))


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "sports-research"}


@app.get("/matches")
def matches(date: str = Query(..., pattern=r"^\d{4}-\d{2}-\d{2}$")):
    try:
        return {"date": date, "matches": client.matches(date), "source": "API-Football"}
    except ResearchError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc


@app.get("/analysis/{fixture_id}")
def analysis(fixture_id: int):
    if fixture_id <= 0:
        raise HTTPException(status_code=400, detail="fixture_id waa inuu ka weyn yahay eber.")
    try:
        return client.analyze(fixture_id)
    except ResearchError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
