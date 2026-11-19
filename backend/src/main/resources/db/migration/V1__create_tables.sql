-- テーブルの作成と初期データ（docs/database.md の 2章）
-- 適用済みのこのファイルは書き換えず、変更は V2__…sql を足して行う

CREATE TABLE columns (
  id       integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name     text    NOT NULL,
  position integer NOT NULL UNIQUE
);

CREATE TABLE tasks (
  id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
  column_id   integer     NOT NULL REFERENCES columns (id),
  title       text        NOT NULL CHECK (title <> ''),
  description text        NOT NULL DEFAULT '',
  priority    text        NOT NULL DEFAULT 'mid' CHECK (priority IN ('high', 'mid', 'low')),
  due         date,
  position    integer     NOT NULL,
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX tasks_column_id_position_idx ON tasks (column_id, position);

INSERT INTO columns (name, position) VALUES
  ('未着手', 0),
  ('進行中', 1),
  ('完了', 2);
