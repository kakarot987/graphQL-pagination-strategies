# GraphQL Pagination Strategies

Java 21 / Spring Boot 3.3 / Spring GraphQL / JPA, using H2 by default and optionally PostgreSQL.

## Run and test

```sh
./gradlew test
./gradlew bootRun
# or docker compose up --build
```

Explore queries at http://localhost:8080/graphiql.

## Strategies

- `activityFeed(first: 5, after: "...")` uses a descending-ID cursor, real database-level seek predicate, and `first+1` query limit. The ID represents insertion order, **not timestamp order**. Cursor is Base64(`cursor:activity:ID`), which is encoding rather than encryption/authentication. Malformed or wrong-type cursors return errors.
- `products(page: 0, size: 10)` and `adminUsers` use offset pagination with deterministic ID sort tie-breakers, but can shift when rows are inserted or removed.
- `activityFeedOffset(page: 0, size: 5)` is a demonstration-only comparator on the activity data. It is intentionally susceptible to offset drift after a head insertion.
- Offset `page >= 0`; `size` and cursor `first` are 1–100. Price bounds must be nonnegative, minimum not above maximum, ratings 0–5.
- Product category facet counts and price-range metadata respect **all active filters** (including category); empty results return no facets and zero price range.
- Product prices and user total spending persist using `BigDecimal`, while the GraphQL Float fields remain demo-compatible. Production money APIs should use exact decimal strings or minor units.

## Verify cursor stability

Run `.\\demo.ps1` in PowerShell from the repository root while the server is running. It captures the next pages, inserts a new activity, and asserts that the saved cursor returns the same next page with no duplicates, while the offset page shifts.

## Optional PostgreSQL and performance comparison

```sh
docker compose --profile postgres up --build -d postgres graphql-postgres
```

The PostgreSQL app is at http://localhost:8081/graphql, while the ordinary H2 service is on port 8080. The [benchmark instructions](benchmark/README.md) generate 250,000 rows and compare indexed keyset versus offset using `EXPLAIN (ANALYZE, BUFFERS)`. No benchmark timings are claimed without an actual run.

## Testing

CI: `.github/workflows/tests.yml` runs Gradle tests on pull requests and main pushes.

## Security disclaimer

**Educational demo only.** Admin endpoints and mutation have no authentication or authorization. GraphiQL, H2 console, introspection, demo credentials and permissive development settings are not suitable for production. PostgreSQL demo profile uses `create-drop`. Secure these before any public deployment, including access control, migration management, credentials and rate limits. Cursors are not signed, so clients can construct valid IDs; add tamper protection if needed.
