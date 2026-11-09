# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 概要

ブラウザだけで動く、Trello 風のカンバン式タスク管理アプリ（固定3列・ドラッグ＆ドロップで移動）。HTML・CSS・JavaScript はすべて [index.html](index.html) にインラインで書かれている。ビルド・パッケージマネージャ・依存ライブラリ・テスト・リンターは存在しない。

要件は [docs/requirements.md](docs/requirements.md) にまとめてある。機能を足すときは、まずこの要件定義書と食い違わないかを確認する。

## 実行・公開

- `open index.html` でブラウザで直接開く（サーバー不要）
- ローカルサーバーが必要な場合: `python3 -m http.server` → http://localhost:8000
- 動作確認は手動でブラウザ上で行う（自動テストなし）
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

## 規約

- UI の文言は日本語、識別子は英語。
- ユーザー入力は `textContent` で挿入する（`innerHTML` は固定文字列のみ）。
