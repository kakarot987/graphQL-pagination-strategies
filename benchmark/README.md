# PostgreSQL benchmark

Create 250,000 rows and indexed seek/offset comparisons at depths 0, 1K, 10K, 100K, 200K. Measured results depend on hardware, cache state, PostgreSQL version and load; no measured speedup is claimed.

```sh
docker compose --profile postgres up -d postgres
docker compose exec -T postgres psql -U pagination -d pagination < benchmark/01_seed.sql
docker compose exec -T postgres psql -U pagination -d pagination < benchmark/02_explain.sql
```

Repeat the EXPLAIN script at least three times and report `Execution Time`, shared buffer hits/reads, query plans and experimental environment. `01_seed.sql` truncates only the dedicated benchmark table.

Optional PostgreSQL application: `docker compose --profile postgres up --build -d postgres graphql-postgres`. GraphQL runs on port 8081. The PostgreSQL profile uses `create-drop` and disposable demo credentials, not production settings.
