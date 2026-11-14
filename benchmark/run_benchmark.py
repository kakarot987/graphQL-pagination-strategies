#!/usr/bin/env python3
"""Reproducible PostgreSQL offset-vs-keyset benchmark.

Requires the optional Compose PostgreSQL service and benchmark/01_seed.sql.
Uses only Python's standard library. Writes per-depth measurements and a
Markdown report for upload as a GitHub Actions artifact.
"""
from __future__ import annotations

import csv
import json
import statistics
import subprocess
from datetime import datetime, timezone
from pathlib import Path

ROWS = 250_000
PAGE_SIZE = 20
DEPTHS = (0, 1_000, 10_000, 100_000, 200_000)
REPEATS = 5
RESULTS = Path(__file__).resolve().parent / "results"


def psql(sql: str) -> str:
    cmd = [
        "docker", "compose", "--profile", "postgres", "exec", "-T",
        "postgres", "psql", "-X", "-A", "-t", "-v", "ON_ERROR_STOP=1",
        "-U", "pagination", "-d", "pagination", "-c", sql,
    ]
    completed = subprocess.run(cmd, text=True, capture_output=True, check=True)
    return completed.stdout.strip()


def sql_for(strategy: str, depth: int) -> str:
    if strategy == "offset":
        return (
            "SELECT id FROM pagination_benchmark_activity "
            f"ORDER BY id DESC LIMIT {PAGE_SIZE} OFFSET {depth}"
        )
    if strategy == "keyset":
        # Generated IDs run contiguously from 1 to ROWS.
        return (
            "SELECT id FROM pagination_benchmark_activity "
            f"WHERE id < {ROWS + 1 - depth} "
            f"ORDER BY id DESC LIMIT {PAGE_SIZE}"
        )
    raise ValueError(strategy)


def ids(sql: str) -> list[int]:
    output = psql(sql)
    return [int(value) for value in output.splitlines()] if output else []


def measure(sql: str) -> dict[str, float | int]:
    output = psql(f"EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) {sql}")
    try:
        result = json.loads(output)[0]
    except (ValueError, IndexError) as exc:
        raise RuntimeError(f"Invalid PostgreSQL EXPLAIN JSON: {output[:500]}") from exc
    plan = result["Plan"]
    return {
        "execution_ms": float(result["Execution Time"]),
        "shared_hit_blocks": int(plan.get("Shared Hit Blocks", 0)),
        "shared_read_blocks": int(plan.get("Shared Read Blocks", 0)),
        "actual_rows": int(plan.get("Actual Rows", 0)),
    }


def main() -> None:
    RESULTS.mkdir(parents=True, exist_ok=True)
    actual_row_count = int(psql("SELECT COUNT(*) FROM pagination_benchmark_activity"))
    if actual_row_count != ROWS:
        raise RuntimeError(f"Benchmark needs {ROWS} rows; found {actual_row_count}")

    timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
    metadata = psql("SELECT version()").splitlines()[0]
    samples: list[dict[str, object]] = []
    summary: list[dict[str, object]] = []

    for depth in DEPTHS:
        queries = {strategy: sql_for(strategy, depth) for strategy in ("offset", "keyset")}
        expected = ids(queries["offset"])
        actual = ids(queries["keyset"])
        if expected != actual or len(expected) != PAGE_SIZE:
            raise AssertionError(
                f"Rows differ at depth {depth}: offset={expected}, keyset={actual}"
            )

        # Warm both paths to reduce cold-cache bias; alternate evaluation order.
        for strategy in ("offset", "keyset"):
            measure(queries[strategy])

        gathered: dict[str, list[dict[str, float | int]]] = {"offset": [], "keyset": []}
        for iteration in range(REPEATS):
            order = ("offset", "keyset") if iteration % 2 == 0 else ("keyset", "offset")
            for strategy in order:
                values = measure(queries[strategy])
                if values["actual_rows"] != PAGE_SIZE:
                    raise AssertionError(f"EXPLAIN returned unexpected row count: {values}")
                gathered[strategy].append(values)
                samples.append({"depth": depth, "strategy": strategy, "iteration": iteration + 1, **values})

        for strategy in ("offset", "keyset"):
            measures = gathered[strategy]
            summary.append({
                "depth": depth,
                "strategy": strategy,
                "median_execution_ms": round(statistics.median(m["execution_ms"] for m in measures), 3),
                "min_execution_ms": round(min(m["execution_ms"] for m in measures), 3),
                "max_execution_ms": round(max(m["execution_ms"] for m in measures), 3),
                "median_shared_hit_blocks": statistics.median(m["shared_hit_blocks"] for m in measures),
                "median_shared_read_blocks": statistics.median(m["shared_read_blocks"] for m in measures),
            })

    sample_csv = RESULTS / "samples.csv"
    with sample_csv.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=[
            "depth", "strategy", "iteration", "execution_ms",
            "shared_hit_blocks", "shared_read_blocks", "actual_rows"
        ])
        writer.writeheader()
        writer.writerows(samples)

    lines = [
        "# PostgreSQL pagination benchmark measurements",
        "",
        f"- Timestamp: {timestamp}",
        f"- Server: {metadata}",
        f"- Rows: {ROWS:,}; page size: {PAGE_SIZE}; repetitions per pair: {REPEATS}",
        "- Environment: GitHub Actions ubuntu-latest runner and PostgreSQL 16 Alpine Docker image",
        "- Method: EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON), one warmup per strategy and depth, alternate order",
        "- Each offset/keyset pair was verified to return exactly the same 20 IDs.",
        "- Measured values represent SQL execution in this runner, **not** app request latency or a general performance guarantee.",
        "",
        "| Depth | Strategy | Median execution (ms) | Min–max (ms) | Median shared hits | Median shared reads |",
        "| ---: | --- | ---: | ---: | ---: | ---: |",
    ]
    for row in summary:
        lines.append(
            f"| {row['depth']:,} | {row['strategy']} | {row['median_execution_ms']:.3f} | "
            f"{row['min_execution_ms']:.3f}–{row['max_execution_ms']:.3f} | "
            f"{row['median_shared_hit_blocks']:.0f} | {row['median_shared_read_blocks']:.0f} |"
        )
    lines += [
        "",
        "See `samples.csv` for individual EXPLAIN measurement values.",
        "Repeat benchmarks on the target workload, including realistic filters, insertions, indexes and concurrency.",
    ]

    report = "\n".join(lines) + "\n"
    (RESULTS / "summary.md").write_text(report, encoding="utf-8")
    print(report, flush=True)
    for row in summary:
        print("BENCHMARK_RESULT " + json.dumps(row, sort_keys=True), flush=True)


if __name__ == "__main__":
    main()
