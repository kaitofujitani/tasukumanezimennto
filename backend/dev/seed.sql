-- 開発用 DB（compose.yaml）に入れるテストデータ。本番に入らないよう Flyway のマイグレーションには入れていない
-- 入れ方（backend フォルダで、サーバーを一度起動してテーブルを作ったあとに実行）:
--   docker compose exec -T db psql -U taskboard -d taskboard < dev/seed.sql
-- 何度実行しても同じ状態になるよう、最初に tasks の行をすべて消す（開発用 DB のタスクは消えるので注意）

BEGIN;

DELETE FROM tasks;

INSERT INTO tasks (column_id, title, description, priority, due, position)
SELECT c.id, t.title, t.description, t.priority, t.due::date, t.position
FROM (VALUES
  ('未着手', '要件定義書を読み直す', '',                         'high', '2026-11-25', 0),
  ('未着手', '画面のデザイン案を作る', 'ボード画面と詳細画面の2つ', 'mid',  NULL,         1),
  ('未着手', '背景写真を選ぶ',        '',                         'low',  NULL,         2),
  ('進行中', 'ボード取得 API を作る', 'GET /api/board',           'high', '2026-11-21', 0),
  ('進行中', 'テストデータを用意する', '',                        'mid',  '2026-11-22', 1),
  ('完了',   '開発用 DB を用意する',  'docker compose で PostgreSQL を起動', 'mid', NULL, 0)
) AS t(column_name, title, description, priority, due, position)
JOIN columns c ON c.name = t.column_name;

COMMIT;
