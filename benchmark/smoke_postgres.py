#!/usr/bin/env python3
"""PostgreSQL-backed GraphQL end-to-end smoke test (stdlib only)."""
from __future__ import annotations

import json
import time
import urllib.error
import urllib.request

URL = "http://127.0.0.1:8081/graphql"
QUERY = """
{
  activityFeed(first: 3) {
    nodes { id username }
    pageInfo { hasNextPage endCursor }
    totalCount
  }
  products(page: 0, size: 3, filter: { category: "Electronics" }) {
    nodes { id name price }
    categoryFacets { name count }
    priceRange { min max average }
    pageInfo { currentPage totalPages }
  }
  adminUsers(page: 0, size: 3) {
    nodes { id username totalSpent }
    pageInfo { currentPage }
  }
}
"""


def check() -> None:
    body = json.dumps({"query": QUERY}).encode("utf-8")
    request = urllib.request.Request(
        URL, data=body, headers={"Content-Type": "application/json"}, method="POST"
    )
    with urllib.request.urlopen(request, timeout=10) as response:
        payload = json.load(response)
    if payload.get("errors"):
        raise AssertionError(f"GraphQL errors: {payload['errors']}")
    data = payload.get("data") or {}
    for connection in ("activityFeed", "products", "adminUsers"):
        part = data.get(connection) or {}
        if len(part.get("nodes") or []) != 3:
            raise AssertionError(f"Expected 3 non-null {connection} nodes: {part}")
        if part.get("pageInfo") is None:
            raise AssertionError(f"Missing {connection} pageInfo")
    if not data["products"]["categoryFacets"]:
        raise AssertionError("Filtered products should have an Electronics facet")
    if data["products"]["priceRange"]["max"] <= 0:
        raise AssertionError("Price aggregation should return nonzero max")
    print("POSTGRES_GRAPHQL_SMOKE_PASS: activityFeed, products, adminUsers, facets and prices", flush=True)


if __name__ == "__main__":
    last_error: Exception | None = None
    for attempt in range(60):
        try:
            check()
            break
        except (urllib.error.URLError, TimeoutError, AssertionError, ValueError) as error:
            last_error = error
            if attempt == 59:
                raise SystemExit(f"POSTGRES_GRAPHQL_SMOKE_FAIL: {error}") from error
            time.sleep(5)
