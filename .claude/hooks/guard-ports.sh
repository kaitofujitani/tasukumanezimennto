#!/bin/bash
# Claude Code の PreToolUse フック。
# 開発用のサーバーを決まったポート以外で起動するコマンドを止める（exit 2 でブロック）。
#   画面（Vite）5173・バックエンド（Spring Boot）8080・DB（PostgreSQL）5432（CLAUDE.md の「開発用サーバーのポート」）
# ポートが使われていたら、別のポートに逃げずに /dev-up スキル（.claude/skills/dev-up/free-ports.sh）で空ける。

cmd=$(jq -r '.tool_input.command // ""')

block() {
  echo "開発用サーバーは決まったポート（画面 5173・バックエンド 8080・DB 5432）でしか起動できません（$1）。" >&2
  echo "ポートが使われているときは、bash .claude/skills/dev-up/free-ports.sh で使っているプロセスを止めてから、決まったポートで起動してください。" >&2
  exit 2
}

# git・gh のコマンド（コミットメッセージや Issue の本文）は調べない
if echo "$cmd" | grep -Eq '^[[:space:]]*(git|gh)[[:space:]]'; then
  exit 0
fi

# サーバーを起動するコマンドでなければ調べない
if ! echo "$cmd" | grep -Eq '(vite|npm|npx|node|gradlew|bootRun|java|docker)'; then
  exit 0
fi

# 画面（Vite）: --port は 5173 だけ
for p in $(echo "$cmd" | grep -Eo -- '--port[= ]+[0-9]+' | grep -Eo '[0-9]+$'); do
  [ "$p" != 5173 ] && block "--port $p"
done

# バックエンド: PORT / SERVER_PORT / server.port は 8080 だけ
for p in $(echo "$cmd" | grep -Eo '(^|[^A-Za-z_])(SERVER_)?PORT=[0-9]+' | grep -Eo '[0-9]+$'); do
  [ "$p" != 8080 ] && block "PORT=$p"
done
for p in $(echo "$cmd" | grep -Eo 'server\.port=[0-9]+' | grep -Eo '[0-9]+$'); do
  [ "$p" != 8080 ] && block "server.port=$p"
done

# Docker: 5432・8080・5173 を別のポート番号で公開しない（-p 5433:5432 など）
for m in $(echo "$cmd" | grep -Eo -- '-p[= ]*[0-9]+:(5432|8080|5173)\b' | grep -Eo '[0-9]+:[0-9]+$'); do
  [ "${m%%:*}" != "${m##*:}" ] && block "-p $m"
done

exit 0
