# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

内容は [AGENTS.md](AGENTS.md)（Codex 向け）と揃えてある。どちらかを変えたら、もう一方も同じように更新する。

## 概要

ブラウザだけで動く、Trello 風のカンバン式タスク管理アプリ（固定3列・ドラッグ＆ドロップで移動）。HTML・CSS・JavaScript はすべて [index.html](index.html) にインラインで書かれている。ビルド・パッケージマネージャ・依存ライブラリ・テスト・リンターは存在しない。

要件は [docs/requirements.md](docs/requirements.md) に概要をまとめ、機能・画面・データベース・API・技術の詳細は docs/ 内の別の資料に分けてある（一覧は [docs/README.md](docs/README.md)）。機能を足すときは、まず要件定義書と関係する資料に食い違わないかを確認し、変更したら該当する資料も更新する。

なお、資料はサーバーと PostgreSQL を使う版を前提に書いている。下の「アーキテクチャ」は、まだ移行前の今の実装（localStorage に保存する版）の説明。

## 実行・公開

- `open index.html` でブラウザで直接開く（サーバー不要）
- ローカルサーバーが必要な場合: `python3 -m http.server` → http://localhost:8000
- 動作確認は手動でブラウザ上で行う（自動テストなし）
- バックエンド（移行中・`backend/`）: `cd backend && docker compose up -d` で開発用 PostgreSQL を起動し、`APP_PASSWORD=<ログインのパスワード> ./gradlew bootRun` → http://localhost:8080 （`APP_PASSWORD` が未設定・空だと起動しない）。起動時に Flyway が `src/main/resources/db/migration/` の SQL を適用する。接続先は環境変数 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` で上書きでき、未設定なら `compose.yaml` の開発用 DB につなぐ
- テストデータ: テーブルができたあと（一度 `bootRun` したあと）に `cd backend && docker compose exec -T db psql -U taskboard -d taskboard < dev/seed.sql` で入れる。本番に入らないよう Flyway のマイグレーションには入れていない。実行するたびに tasks の行をすべて消して入れ直す。API はログインが必要。curl では `curl -c cookies.txt -X POST localhost:8080/api/login -H 'Content-Type: application/json' -d '{"password":"…"}'` でログインし、以後 `-b cookies.txt` を付けて呼ぶ（例: `curl -b cookies.txt localhost:8080/api/board`）。`POST`・`PATCH`・`DELETE` は Cookie `XSRF-TOKEN` の値をヘッダー `X-XSRF-TOKEN` にも付ける
- GitHub Pages で `main` ブランチのルートを公開している: https://kaitofujitani.github.io/tasukumanezimennto/ 。`main` に push すると数分で反映される。

## アーキテクチャ

- **状態**: モジュールスコープの `cards` 配列（全カード）と、ドラッグ中のカード ID を持つ `draggingId`、詳細画面で開いているカードを持つ `editing`。
- **永続化**: `localStorage` のキー `board.v1` に JSON で保存。データ構造を変える場合は互換性のためキーのバージョンを上げるか、`load()` で移行処理を入れること。
- **カードの形**: `{ id, title, description, priority: "high" | "mid" | "low", due: "YYYY-MM-DD" | "", status: "todo" | "doing" | "done", created: number(ms) }`。`description` / `priority` / `due` は後から追加した項目なので、`load()` で欠けていれば既定値（`""` / `"mid"` / `""`）を補っている。
- **更新の流れ**: すべての操作は「`cards` を直接変更 → `save()` → `render()`」の順。`render()` はボードの DOM を毎回作り直す（差分更新はしない）。イベントハンドラは `onclick` などのプロパティ代入で設定している。
- **列**: `COLUMNS` 配列（`id` と表示名 `label`）で固定定義し、この順に表示する。詳細画面の「列」の選択肢もここから作る。
- **並び順**: 列内の順番は `cards` 配列内の順序そのもの（`order` のようなフィールドはない）。`render()` は `status` で絞り込むだけで並べ替えない。
- **自動の並べ替え**: 「並べ替え」ボタンを押したときだけ、`#sort` の値に対応する `COMPARE` の比較関数で `cards` 配列全体を `sort()` して保存する（表示のたびに並べ替えるのではない）。そのため、並べ替えたあとも手動で順番を調整できる。
- **優先度**: `PRIO`（並べ替え用の数値）と `PRIO_LABEL`（表示用）の2つの対応表がある。優先度を追加する場合は両方の対応表と、詳細画面の `<select>` の選択肢を揃えて更新する。
- **ドラッグ＆ドロップ**: HTML5 Drag and Drop API を使用。`dragover` で `cardAfter()` がマウスの Y 座標から挿入位置を求め、`.placeholder` の線を表示する。`drop` でカードを配列から抜き、`status` を書き換えて挿入先カードの直前（なければ末尾）に入れ直す。タッチ操作では動かない。
- **詳細画面**: `<dialog id="detail">` を `showModal()` で開く。入力は `oninput` / `onchange` でその場で `editing` に反映して `save()` → `render()` する（保存ボタンはない）。列の変更は移動先の列の末尾に入れ、「上へ」「下へ」は同じ列の隣のカードと配列内の位置を入れ替える。スマホではこの画面が列の移動・並べ替えの手段になる。
- **テーマ**: 色は `:root` の CSS 変数で定義し、`prefers-color-scheme: dark` で上書きしている。新しい色はこの変数に追加する。
- **背景写真**: 写真は `images/` に置き、`BACKGROUNDS` 配列（`{ src, credit, url }`）に登録する。`credit` は右下の `#credit` リンクの文言、`url` はそのリンク先（出典ページ）。現在の写真はすべて Wikimedia Commons のもので、ライセンスの詳細は [images/CREDITS.md](images/CREDITS.md) にある。写真を追加・削除したら `BACKGROUNDS` と CREDITS.md の両方を更新する。1枚 500KB 以下を目安に縮小・圧縮して置く。`setBackground()` が開くたびにランダムで1枚選び、読み込めたときだけ `body` に `has-bg` を付ける。`has-bg` のときは列とヘッダーを `--glass`（半透明）にする。写真は iPhone の Safari でも固定表示されるよう、`background-attachment: fixed` ではなく固定配置の `.bg` 要素に敷いている。写真は利用が許可されたものだけを使い、必要なら `credit` を表記する。

## Git 運用ルール（厳守）

作業は必ず「Issue → ブランチ → PR → マージ」の順で進める。例外は作らない。

1. **Issue を先に作る**: コードや資料を変える前に `gh issue create` で Issue を登録する（テンプレートは `.github/ISSUE_TEMPLATE/task.md`：背景・やること・完了条件）。既存の Issue があればそれを使う。ラベルは `enhancement`（機能追加）/ `bug`（不具合）/ `documentation`（資料）から選ぶ。
2. **ブランチを切る**: 最新の `main` から `<type>/#<issue番号>-<説明>` の名前で作る。説明は英小文字とハイフン（ケバブケース）で短く。
   - type: `feature`（機能追加）/ `fix`（不具合修正）/ `docs`（資料のみ）/ `refactor`（動作を変えない整理）/ `chore`（設定・依存関係など）
   - 例: `feature/#12-add-login`、`fix/#15-drag-drop-order`、`docs/#20-update-db-design`
   - `#` はシェルでコメント扱いになることがあるので、コマンドではブランチ名を必ず引用符で囲む（例: `git switch -c 'feature/#12-add-login'`）。
   - 1 ブランチ = 1 Issue。関係ない変更を混ぜない。
3. **コミット**: 作業ブランチ上でのみ行う。メッセージは Conventional Commits 形式 `<type>: <説明>` で、type は英語、説明は日本語で変更内容がわかるように書く。
   - type: `feat`（機能追加）/ `fix`（不具合修正）/ `docs`（資料）/ `refactor`（動作を変えない整理）/ `chore`（設定・依存関係など）/ `test`（テスト）/ `style`（見た目だけの整形）
   - 例: `feat: ログイン機能を追加`、`fix: ドラッグ後に並び順が崩れる不具合を修正`、`docs: API 仕様書を更新`
   - 全体設定（`~/.claude/CLAUDE.md`）の「日本語で一行」より、このプロジェクトではこの形式を優先する。
4. **PR を作る**: ブランチを push し `gh pr create` で PR を作る（テンプレートは `.github/pull_request_template.md`）。タイトルもコミットと同じ Conventional Commits 形式にする。本文に `Closes #issue番号` を入れ、マージ時に Issue が自動で閉じるようにする。
5. **マージ**: ユーザーの確認を得てから行う。マージ後は作業ブランチを削除する。

`main` への直接 commit / push は禁止。GitHub の Ruleset（`protect-main`）で PR 必須・強制 push 禁止・ブランチ削除禁止にしてあり、Claude Code でも `.claude/hooks/guard-main.sh` が main 上の `git commit` と main への `git push` を止める。GitHub Pages は `main` から公開しているため、PR がマージされた時点で反映される。

## 規約

- UI の文言は日本語、識別子は英語。
- ユーザー入力は `textContent` で挿入する（`innerHTML` は固定文字列のみ）。
