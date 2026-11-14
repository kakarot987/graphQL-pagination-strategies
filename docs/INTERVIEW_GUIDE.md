# Interview guide — GraphQL Pagination Strategies

**Project:** [GitHub repository](https://github.com/kakarot987/graphQL-pagination-strategies) · [System design deep dive](SYSTEM_DESIGN.md) · [Measured benchmark run](https://github.com/kakarot987/graphQL-pagination-strategies/actions/runs/43594538554)

This guide helps explain what the project **actually implements**, what the benchmark proves, and what a production-grade next iteration would require.

## 30-second pitch

> I built a Spring Boot and GraphQL pagination demo comparing offset pagination for products and admin dashboards with ID-based cursor/keyset pagination for an activity feed. It includes filter validation, deterministic sorting, GraphQL integration tests and a live insert-between-pages demonstration. I also automated a 250,000-row PostgreSQL benchmark: at a depth of 200,000 rows, the recorded median SQL execution times were 26.793 ms for offset and 0.072 ms for keyset in that CI environment. I chose the strategy based on the user experience and consistency requirements rather than assuming one approach is always best.

## 90-second walkthrough

1. **Problem:** A catalog needs jump-to-page and total counts; a live activity timeline needs stable continuation after a new entry is inserted.
2. **API:** Spring GraphQL exposes separate connection fields, with typed cursors, metadata and filter inputs.
3. **Correctness:** A saved activity cursor is decoded to an ID and used in `WHERE id < :cursor_id` for descending results. Fetching `first+1` determines `hasNextPage`. In contrast, `OFFSET` operates on positions that can shift across requests.
4. **Database:** JPA specifications/criteria construct filtered queries. Product facets use one grouped aggregation rather than N category-count queries; money uses `BigDecimal` in persistence.
5. **Proof:** Integration tests validate cursor stability after insertion, invalid cursors, page size limits and filtered metadata. A CI workflow compares identical offset and keyset result pages and records PostgreSQL EXPLAIN statistics.
6. **Trade-off:** This is an insertion-ID feed, not true event-time sorting or a database snapshot. Cursor Base64 is not encryption; production endpoints still need authorization, migrations and operational controls.

## Deep-dive questions and model answers

**Q: Why not use cursor pagination everywhere?**

**A:** Offset is useful when a product UI needs page 13, total page counts or arbitrary jumping. Cursor pagination needs a known boundary and a consistent ordering key; it's better suited to "load more" in a changing feed. Cursor-based deep jumps generally require extra indexing or a separate navigation mechanism.

**Q: What exactly breaks with offset pagination during writes?**

**A:** If page 1 returns IDs `[9,8,7]` and ID 10 is inserted before page 2, `LIMIT 3 OFFSET 3` returns `[7,6,5]`. ID 7 repeats. An index and deterministic sort do not prevent that logical boundary shift.

**Q: How does the activity cursor prevent the duplicate?**

**A:** Its payload identifies the last seen ID, for example `cursor:activity:7`. The next page uses `id < 7`, sorted descending. A new ID 10 does not affect which records follow ID 7.

**Q: Is the cursor opaque, secure or tamper-proof?**

**A:** The API treats it as opaque, but the payload is just Base64; clients can decode or construct it. The decoder validates syntax and expected connection type, not authenticity. Secure applications would authorize records independently and consider signing/versioning cursors.

**Q: What if activity timestamps aren't ordered like IDs?**

**A:** This implementation sorts by insertion ID, so it guarantees neither event-time chronology nor timestamp-order keyset traversal. I'd use a composite cursor `(timestamp,id)` and matching index if time ordering were a requirement, while defining how late events are handled.

**Q: Can keyset pagination guarantee a consistent snapshot?**

**A:** No. It stabilizes continuation relative to the saved ordering key for certain changes. Deletes, updates, changing filters and late inserts can still change results between requests. A snapshot token, versioned dataset or consistent read transaction would be needed for strict snapshot semantics.

**Q: Why fetch one extra row?**

**A:** A query capped at `first+1` indicates `hasNextPage` without needing a count for that flag. The API still calculates `totalCount` separately because it exposes this metadata; keyset isn't automatically free of count costs.

**Q: How did you handle duplicate sort values in offset pagination?**

**A:** Product/user sorts add ascending `id` as a unique tie-breaker. For a fixed dataset, that makes page boundaries deterministic even if multiple products share price or rating. It does not prevent drift when data changes.

**Q: What was the query optimization?**

**A:** Product facets previously required one count per distinct category. I replaced that with a grouped count query and applied the active filters; price aggregates also use the filters. The activity seek now uses a bounded Criteria query instead of an implicit pageable count for its data fetch.

**Q: How fair is the PostgreSQL benchmark?**

**A:** It uses 250,000 generated rows, matching 20-row result pages for each offset/keyset pair, an index on the ID seek column, a warmup, five measurement iterations and `EXPLAIN (ANALYZE, BUFFERS)`. The results are from one CI machine and represent SQL execution—not end-to-end GraphQL p95 latency. Production filter distributions and indexes would need separate benchmarks.

**Q: Why did you choose BigDecimal?**

**A:** Binary floating-point is unsuitable for exact money storage. The JPA entities use decimal precision/scale and Java `BigDecimal`, but the existing GraphQL `Float` API isn't fully money-safe. I'd use a custom decimal scalar or minor-unit integer/string type for a real payment integration.

**Q: How would you take this into production?**

**A:** I'd add authentication/authorization for admin operations and mutations, rate limits and complexity guards, migrations, signed cursors where useful, composite indexes tailored to sort/filter patterns, structured logs and tracing, safe error masking, and real API load testing.

## Questions to ask the interviewer

- Does the UI need direct page jumps, or primarily "load more"?
- How frequently can rows be inserted, deleted or reordered?
- Is consistency a stable boundary, monotonic navigation or a full snapshot?
- Which sorts and filters are supported, and what indexes can sustain them?
- Are exact counts mandatory, cached or optional on large datasets?

## Resume bullet options

Use only claims that match the implementation and evidence:

- **Designed and tested** GraphQL cursor/keyset and offset pagination in Java 21/Spring Boot, including typed cursor validation, filter-aware metadata and repeatable integration tests.
- **Optimized** category facet counts with a grouped database query and replaced pageable activity data retrieval with bounded seek queries.
- **Built** an automated PostgreSQL benchmark over 250,000 rows; observed median SQL execution times of 26.793 ms (offset) versus 0.072 ms (keyset) at a depth of 200,000 rows on a GitHub Actions runner.
- **Automated** Java integration testing and Dockerized PostgreSQL GraphQL smoke tests with GitHub Actions.

## The honest limitations worth volunteering

The application is an educational demo with no production authz, signed cursors, or immutable snapshots. Its activity feed seeks by ID rather than timestamp, and the GraphQL monetary scalar is still `Float`. Explain these constraints proactively: they show that you understand the difference between a controlled demonstration and a production system.
