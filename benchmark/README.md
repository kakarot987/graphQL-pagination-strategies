# PostgreSQL benchmark

Create 250,000 rows and indexed seek/offset comparisons at depths 0, 1K, 10K, 100K, 200K. Measured results depend on hardware, cache state, PostgreSQL version and load; no measured speedup is claimed.

```sh
docker compose --profile postgres up -d postgres
docker compose exec -T postgres psql -U pagination -d pagination < benchmark/01_seed.sql
docker compose exec -T postgres psql -U pagination -d pagination < benchmark/02_explain.sql
```

Repeat the EXPLAIN script at least three times and report `Execution Time`, shared buffer hits/reads, query plans and experimental environment. `01_seed.sql` truncates only the dedicated benchmark table.

Optional PostgreSQL application: `docker compose --profile postgres up --build -d postgres graphql-postgres`. GraphQL runs on port 8081. The PostgreSQL profile uses `create-drop` and disposable demo credentials, not production settings.


## Automated measurements and app smoke test

The [PostgreSQL benchmark and GraphQL smoke workflow](../.github/workflows/postgres-benchmark.yml)
runs on relevant pull requests and can be triggered manually after merging.
It seeds the 250,000-row table, checks that equivalent offset and keyset queries
return identical IDs, takes five `EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)`
measurements per depth and strategy after warmup, and uploads
`benchmark/results/summary.md` and `samples.csv` as downloadable workflow
artifacts. It also builds the PostgreSQL-backed application through Docker
Compose and checks actual GraphQL responses.

For local reproduction, after running `01_seed.sql`:

```sh
python3 benchmark/run_benchmark.py
```

This measures database execution time on the environment where the workflow
ran, not network request latency or real-world production performance.
Report runner details, PostgreSQL version, dataset and index definitions with
the measurements. Do not extrapolate from a single virtual machine.
