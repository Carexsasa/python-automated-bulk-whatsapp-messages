"""Runtime configuration, all overridable via environment variables."""
from __future__ import annotations

import os
from datetime import date


def _env_set(name: str) -> set[str]:
    raw = os.getenv(name, "")
    return {x.strip() for x in raw.split(",") if x.strip()}


class Settings:
    HTTP_USER_AGENT = os.getenv(
        "HTTP_USER_AGENT", "GroceryCompare/0.1 (+contact@example.com)"
    )
    REQUEST_TIMEOUT_S = float(os.getenv("REQUEST_TIMEOUT_S", "15"))
    PER_HOST_RPS = float(os.getenv("PER_HOST_RPS", "1"))  # polite: 1 req/sec/host

    # Marktguru
    MARKTGURU_BASE = os.getenv("MARKTGURU_BASE", "https://api.marktguru.de/api/v1")
    MARKTGURU_WEB = os.getenv("MARKTGURU_WEB", "https://www.marktguru.de")
    MARKTGURU_CLIENT_KEY = os.getenv("MARKTGURU_CLIENT_KEY")  # pin a known-good key
    MARKTGURU_API_KEY = os.getenv("MARKTGURU_API_KEY")

    DEFAULT_PLZ = os.getenv("DEFAULT_PLZ", "80331")  # Munich
    DB_PATH = os.getenv("DB_PATH", "grocery.db")

    DISABLED_CHAINS = _env_set("DISABLED_CHAINS")

    # Data source toggle: prefer the aggregator (covers all chains) unless disabled.
    USE_AGGREGATOR = os.getenv("USE_AGGREGATOR", "1") == "1"


settings = Settings()


def offer_week(d: date | None = None) -> str:
    """ISO-week string used as the cache/version key, e.g. ``2026-W34``.

    German discounter offers run Mon–Sat and refresh for the new week; keying by ISO
    week is the natural cache-busting version.
    """
    d = d or date.today()
    iso = d.isocalendar()
    return f"{iso.year}-W{iso.week:02d}"
