#!/bin/bash
# 開発用のポート（画面 5173・バックエンド 8080・DB 5432）を空ける。/dev-up スキルから使う。
# 使っているプロセス・コンテナを表示してから止める。
#   引数なし : 5432 がこのプロジェクトの DB（backend-db-1）なら、そのまま使うので止めない
#   --stop   : このプロジェクトの DB も含めて、すべて止める（開発用サーバーの停止）
# 空けられなかったポートがあれば exit 1。
# 注意: macOS 標準の bash（3.2）は "$pid（" のように変数の直後に全角文字があると変数名の一部と読むので、${pid} のように囲む。

PORTS="5173 8080 5432"
PROJECT_DB_CONTAINER="backend-db-1"
STOP_ALL=false
[ "${1:-}" = "--stop" ] && STOP_ALL=true

docker_up() { docker info >/dev/null 2>&1; }

# ポートを待ち受けている、Docker 以外のプロセスの PID（Docker Desktop 自体はコンテナ単位で扱うので除く）
listener_pids() {
  for pid in $(lsof -nP -tiTCP:"$1" -sTCP:LISTEN 2>/dev/null); do
    case "$(ps -p "$pid" -o comm= 2>/dev/null)" in
      *com.docker* | *Docker* | *vpnkit*) ;;
      *) echo "$pid" ;;
    esac
  done
}

# ポートを公開しているコンテナの名前
publishing_containers() {
  docker_up && docker ps --filter "publish=$1" --format '{{.Names}}'
}

free_port() {
  local port=$1

  for c in $(publishing_containers "$port"); do
    if [ "$c" = "$PROJECT_DB_CONTAINER" ] && ! $STOP_ALL; then
      echo "[$port] このプロジェクトの DB（${c}）が使用中。そのまま使う"
    else
      echo "[$port] コンテナ $c が使用中 → 止める"
      docker stop "$c" >/dev/null
    fi
  done

  local pids
  pids=$(listener_pids "$port")
  [ -z "$pids" ] && return
  for pid in $pids; do
    echo "[$port] PID ${pid}（$(ps -p "$pid" -o command= | cut -c1-120)）が使用中 → 止める"
    kill "$pid" 2>/dev/null
  done
  # 10 秒待っても終わらなければ強制終了する
  for _ in $(seq 1 20); do
    [ -z "$(listener_pids "$port")" ] && return
    sleep 0.5
  done
  for pid in $(listener_pids "$port"); do
    echo "[$port] PID $pid が終了しないので強制終了する"
    kill -9 "$pid" 2>/dev/null
  done
}

for port in $PORTS; do
  free_port "$port"
done

# 最後に、空いていないポートがないかを確かめる
failed=false
for port in $PORTS; do
  others=$(publishing_containers "$port" | grep -vx "$($STOP_ALL || echo "$PROJECT_DB_CONTAINER")")
  if [ -n "$(listener_pids "$port")" ] || [ -n "$others" ]; then
    echo "[$port] まだ使われている。手動で止めてください" >&2
    failed=true
  fi
done
$failed && exit 1
echo "ポート $PORTS の準備ができた"
