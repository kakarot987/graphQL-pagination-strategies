-- Identical page contents at each depth, with and without seek.
\\timing on

\\echo 'Offset depth 0'
EXPLAIN (ANALYZE, BUFFERS) SELECT id FROM pagination_benchmark_activity ORDER BY id DESC LIMIT 20 OFFSET 0;
\\echo 'Seek depth 0'
EXPLAIN (ANALYZE, BUFFERS) SELECT id FROM pagination_benchmark_activity WHERE id < 250001 ORDER BY id DESC LIMIT 20;

\\echo 'Offset depth 1000'
EXPLAIN (ANALYZE, BUFFERS) SELECT id FROM pagination_benchmark_activity ORDER BY id DESC LIMIT 20 OFFSET 1000;
\\echo 'Seek depth 1000'
EXPLAIN (ANALYZE, BUFFERS) SELECT id FROM pagination_benchmark_activity WHERE id < 249001 ORDER BY id DESC LIMIT 20;

\\echo 'Offset depth 10000'
EXPLAIN (ANALYZE, BUFFERS) SELECT id FROM pagination_benchmark_activity ORDER BY id DESC LIMIT 20 OFFSET 10000;
\\echo 'Seek depth 10000'
EXPLAIN (ANALYZE, BUFFERS) SELECT id FROM pagination_benchmark_activity WHERE id < 240001 ORDER BY id DESC LIMIT 20;

\\echo 'Offset depth 100000'
EXPLAIN (ANALYZE, BUFFERS) SELECT id FROM pagination_benchmark_activity ORDER BY id DESC LIMIT 20 OFFSET 100000;
\\echo 'Seek depth 100000'
EXPLAIN (ANALYZE, BUFFERS) SELECT id FROM pagination_benchmark_activity WHERE id < 150001 ORDER BY id DESC LIMIT 20;

\\echo 'Offset depth 200000'
EXPLAIN (ANALYZE, BUFFERS) SELECT id FROM pagination_benchmark_activity ORDER BY id DESC LIMIT 20 OFFSET 200000;
\\echo 'Seek depth 200000'
EXPLAIN (ANALYZE, BUFFERS) SELECT id FROM pagination_benchmark_activity WHERE id < 50001 ORDER BY id DESC LIMIT 20;
