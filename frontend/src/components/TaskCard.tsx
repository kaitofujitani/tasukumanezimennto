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

  const className = [
    "group mb-2 flex items-center gap-2 rounded-lg border border-line bg-card pr-1.5",
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
      // 登録の返事を待っているカードの印。まだ仮の id なので、落とす位置の目印（beforeId）にしない
      data-pending={pending || undefined}
    >
      {/* 詳細画面を開く・ドラッグでつかむ部分。li は一覧の項目のままにし、押せる部分をこの要素に分ける。
          ×ボタンはボタンの中に入れられないので、この要素の隣に置く。
          <button> にしないのは、Firefox では <button> をドラッグできないため */}
      <div
        className="min-w-0 flex-1 rounded-lg px-3 py-2.5"
        // oxlint-disable-next-line jsx-a11y/prefer-tag-over-role
        role="button"
        tabIndex={pending ? -1 : 0}
        aria-disabled={pending || undefined}
        onClick={() => !pending && onOpen()}
        onKeyDown={(e) => {
          if (pending || (e.key !== "Enter" && e.key !== " ")) return;
          e.preventDefault(); // Space で画面がスクロールしないようにする
          onOpen();
        }}
        draggable={!pending}
        onDragStart={dragStart}
        onDragEnd={onDragEnd}
      >
        <div className="wrap-break-word" data-testid="task-title">
          {task.title}
        </div>
        <div className="mt-1 flex gap-2.5 text-[.8rem] text-muted">
          <span>優先度: {PRIORITY_LABEL[task.priority]}</span>
          {task.due && <span>期限: {task.due.replaceAll("-", "/")}</span>}
        </div>
      </div>
      {/* マウスのある端末では、カードにマウスを乗せたとき・キーボードで選んだときだけ見せる。
          スマホ（hover: none）にはマウスを乗せる操作がないので、いつも見せる */}
      <button
        type="button"
        className="cursor-pointer border-none bg-transparent px-1.5 py-0.5 text-[1.1rem] text-muted group-focus-within:opacity-100 group-hover:opacity-100 hover:text-danger [@media(hover:hover)]:opacity-0"
        aria-label={`「${task.title}」を削除`}
        disabled={pending}
        onClick={() => {
          // 元に戻せないので、詳細画面の「削除」と同じく確認してから消す
          if (confirm(`「${task.title}」を削除しますか？`)) onDelete();
        }}
      >
        ×
      </button>
    </li>
  );
}
