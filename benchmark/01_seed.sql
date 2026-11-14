-- Isolated benchmark only; will truncate pagination_benchmark_activity.
CREATE TABLE IF NOT EXISTS pagination_benchmark_activity (
 id BIGINT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 activity_type TEXT NOT NULL,
 created_at TIMESTAMP NOT NULL,
 payload TEXT NOT NULL
);
TRUNCATE TABLE pagination_benchmark_activity;
INSERT INTO pagination_benchmark_activity
SELECT i, (i % 1000) + 1,
 CASE (i % 4) WHEN 0 THEN 'PURCHASE' WHEN 1 THEN 'REVIEW' WHEN 2 THEN 'VIEW' ELSE 'SHARE' END,
 TIMESTAMP '2025-01-01' + i * INTERVAL '1 second', md5(i::text)
FROM generate_series(1,250000) AS i;
CREATE INDEX IF NOT EXISTS idx_bench_user_id ON pagination_benchmark_activity(user_id,id DESC);
CREATE INDEX IF NOT EXISTS idx_bench_type_id ON pagination_benchmark_activity(activity_type,id DESC);
ANALYZE pagination_benchmark_activity;
SELECT count(*) AS row_count FROM pagination_benchmark_activity;
