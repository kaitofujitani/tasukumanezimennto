// ボード画面のカード1枚（docs/screens.md の 2.2）
import type { DragEvent } from "react";
import { PRIORITY_BG, PRIORITY_LABEL, type Task } from "../types";

interface Props {
  task: Task;
  dragging: boolean;
  onOpen: () => void;
  onDelete: () => void;
  onDragStart: () => void;
  onDragEnd: () => void;
}

export function TaskCard({ task, dragging, onOpen, onDelete, onDragStart, onDragEnd }: Props) {
  // 登録してサーバーの返事を待っている間は、開く・動かす・消すをできなくする（まだ仮の id のため）
  const pending = !!task.pending;

  function dragStart(e: DragEvent) {
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/plain", task.id); // Firefox はデータがないとドラッグが始まらない
    onDragStart();
  }

  const className = [
    "mb-2 flex items-center gap-2 rounded-lg border border-line bg-card px-3 py-2.5",
    pending ? "cursor-default opacity-60" : "cursor-grab",
    dragging && "opacity-40",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <li
      className={className}
      data-id={task.id}
      // ドラッグ中のカードの印。落とす位置を求めるとき（Column の beforeIdAt）に除く
      data-dragging={dragging || undefined}
      draggable={!pending}
      tabIndex={0}
      onClick={() => !pending && onOpen()}
      onKeyDown={(e) => {
        if (e.key === "Enter" && e.target === e.currentTarget && !pending) onOpen();
      }}
      onDragStart={dragStart}
      onDragEnd={onDragEnd}
    >
      <div className="min-w-0 flex-1">
        <div className="wrap-break-word" data-testid="task-title">
          {task.title}
        </div>
        <div className="mt-1 flex items-center gap-2.5 text-[.8rem] text-muted">
          <span
            className={`rounded px-1.5 py-px text-[.75rem] font-semibold text-white ${PRIORITY_BG[task.priority]}`}
            aria-label={`優先度: ${PRIORITY_LABEL[task.priority]}`}
          >
            {PRIORITY_LABEL[task.priority]}
          </span>
          {task.due && <span>期限: {task.due.replaceAll("-", "/")}</span>}
        </div>
      </div>
      <button
        type="button"
        className="cursor-pointer border-none bg-transparent px-1.5 py-0.5 text-[1.1rem] text-muted hover:text-danger"
        aria-label={`「${task.title}」を削除`}
        disabled={pending}
        onClick={(e) => {
          e.stopPropagation(); // カードのクリック（詳細画面を開く）まで伝わらないようにする
          onDelete();
        }}
      >
        ×
      </button>
    </li>
  );
}
