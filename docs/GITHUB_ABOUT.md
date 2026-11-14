# GitHub About section — copy-ready text

**Repository description (134 characters):**

Spring GraphQL pagination lab: offset vs cursor/keyset, consistency tests, PostgreSQL benchmarks (250K rows), Docker and CI.

**Suggested topics (choose up to 20 in GitHub):**

`graphql`, `spring-boot`, `java`, `pagination`, `cursor-pagination`, `keyset-pagination`, `offset-pagination`, `postgresql`, `spring-data-jpa`, `performance-benchmark`, `system-design`, `docker`, `github-actions`

**Suggested social preview headline:**

**GraphQL Pagination Strategies — Offset vs Keyset, Tested and Measured**

**Short project pitch:**

A reproducible comparison of pagination choices for real-world UI workloads: numbered catalog pages versus continuously changing activity feeds. Built with Java 21, Spring Boot, GraphQL and JPA; backed by correctness tests, Docker smoke checks and measured PostgreSQL query plans.

**Suggested pinned-repository blurb:**

See cursor stability under insertions, understand deterministic offset sorting, and explore why indexed keyset queries avoid deep-offset costs. Includes full source, Mermaid diagrams, GraphQL examples and an automated 250K-row benchmark.

**Evidence / attribution rules:**

- When quoting a **~372×** query-time difference at depth 200,000, link to the [successful benchmark run](https://github.com/kakarot987/graphQL-pagination-strategies/actions/runs/43594538554) and say *in this dataset/runner*.
- Never claim the benchmark is an end-to-end API speedup or a production SLA.
- Avoid describing the activity feed as ordered by timestamp; it is ordered by descending insertion ID.
- Do not call the demo secure, authenticated or snapshot-consistent.

To apply the description and topics, open the repository's GitHub **About** gear icon and paste the suggested values. This file provides the copy; it does not itself change the GitHub About metadata.
