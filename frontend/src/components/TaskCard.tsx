// ボード画面のカード1枚（docs/screens.md の 2.2）
import type { DragEvent } from "react";
import { PRIORITY_LABEL, type Task } from "../types";

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

  const className = ["card", dragging && "dragging", pending && "pending"].filter(Boolean).join(" ");

  return (
    <li
      className={className}
      data-id={task.id}
      draggable={!pending}
      tabIndex={0}
      onClick={() => !pending && onOpen()}
      onKeyDown={(e) => {
        if (e.key === "Enter" && e.target === e.currentTarget && !pending) onOpen();
      }}
      onDragStart={dragStart}
      onDragEnd={onDragEnd}
    >
      <div className="body">
        <div className="title">{task.title}</div>
        <div className="meta">
          <span>優先度: {PRIORITY_LABEL[task.priority]}</span>
          {task.due && <span>期限: {task.due.replaceAll("-", "/")}</span>}
        </div>
      </div>
      <button
        type="button"
        className="del"
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
