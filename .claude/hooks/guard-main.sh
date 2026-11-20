#!/bin/bash
# Claude Code の PreToolUse フック。
# main ブランチ上での git commit と、main への git push を止める（exit 2 でブロック）。
# 作業は Issue に紐づくブランチで行い、PR 経由で main にマージする（CLAUDE.md の「Git 運用ルール」）。

cmd=$(jq -r '.tool_input.command // ""')
branch=$(git -C "${CLAUDE_PROJECT_DIR:-.}" branch --show-current 2>/dev/null)

if echo "$cmd" | grep -Eq '(^|[;&|[:space:]])git[[:space:]]+commit'; then
  if [ "$branch" = "main" ]; then
    echo "main ブランチ上では commit できません。Issue を作り、'<type>/#<issue番号>-<説明>' のブランチを切ってから作業してください。" >&2
    exit 2
  fi
fi

if echo "$cmd" | grep -Eq '(^|[;&|[:space:]])git[[:space:]]+push'; then
  # git push の引数部分（; & | 改行の手前まで）だけを見て、push 先に main が含まれるかを判定する
  push_args=$(echo "$cmd" | grep -Eo 'git[[:space:]]+push[^;&|]*')
  if [ "$branch" = "main" ] || echo "$push_args" | grep -Eq '[[:space:]:]main([[:space:]]|$)'; then
    echo "main への直接 push は禁止です。作業ブランチを push し、PR を作成してください。" >&2
    exit 2
  fi
fi

exit 0
