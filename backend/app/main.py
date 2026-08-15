"""FastAPI application: the clean normalized API the Android app talks to.

The phone NEVER scrapes. It only calls these endpoints; all fetching/parsing happens on
the server on a weekly schedule.
"""
from __future__ import annotations

from contextlib import asynccontextmanager
from typing import Optional

from fastapi import FastAPI, Query

from .chains import ALL_PARSERS
from .config import offer_week, settings
from .models import (
    Chain,
    ChainHealth,
    HealthResponse,
    SearchResponse,
)
from .scheduler import refresh_region, start_scheduler
from .search import autocomplete, best_basket, search
from .store import db


@asynccontextmanager
async def lifespan(app: FastAPI):
    db.init_db()
    sched = start_scheduler()
    app.state.scheduler = sched
    yield
    sched.shutdown(wait=False)


app = FastAPI(
    title="GroceryCompare API",
    version="0.1.0",
    description="Normalized weekly grocery offers for German discounters.",
    lifespan=lifespan,
)


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    states = db.get_connector_states()
    chains: list[ChainHealth] = []
    for c in Chain:
        # health is reported per chain; the aggregator feeds them all
        src_state = states.get("marktguru")
        counts = db.load_offers(settings.DEFAULT_PLZ, offer_week())
        n = sum(1 for o in counts if o.chain == c)
        status = "disabled" if c.value in settings.DISABLED_CHAINS else (
            "ok" if src_state and src_state["status"] == "ok" else "stale"
        )
        chains.append(
            ChainHealth(
                chain=c,
                status=status,
                last_success=src_state["last_success"] if src_state else None,
                last_error=src_state["last_error"] if src_state else None,
                offer_count=n,
            )
        )
    return HealthResponse(
        status="demo" if settings.DEMO_MODE else "ok",
        offer_week=offer_week(),
        chains=chains,
    )


@app.get("/search", response_model=SearchResponse)
def search_endpoint(
    q: str = Query(..., min_length=1, description="Item, e.g. 'Kartoffeln'"),
    plz: str = Query(settings.DEFAULT_PLZ, description="5-digit German postal code"),
    lat: Optional[float] = None,
    lon: Optional[float] = None,
    max_distance_km: Optional[float] = None,
    limit: int = 50,
) -> SearchResponse:
    return search(
        q, plz, lat=lat, lon=lon, max_distance_km=max_distance_km, limit=limit
    )


@app.get("/autocomplete")
def autocomplete_endpoint(
    q: str = Query(..., min_length=1),
    plz: str = Query(settings.DEFAULT_PLZ),
) -> dict:
    return {"suggestions": autocomplete(q, plz)}


@app.get("/basket")
def basket_endpoint(
    items: list[str] = Query(..., description="Repeat ?items= for each item"),
    plz: str = Query(settings.DEFAULT_PLZ),
    lat: Optional[float] = None,
    lon: Optional[float] = None,
    max_distance_km: Optional[float] = None,
) -> dict:
    return best_basket(items, plz, lat=lat, lon=lon, max_distance_km=max_distance_km)


@app.post("/admin/refresh")
async def admin_refresh(plz: str = Query(settings.DEFAULT_PLZ)) -> dict:
    """Manual refresh trigger (for dev / ops). In prod, protect this."""
    n = await refresh_region(plz)
    return {"plz": plz, "offer_week": offer_week(), "written": n}
