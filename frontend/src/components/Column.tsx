// ボード画面の列1つ：見出し・カード・入力欄と、ドラッグ＆ドロップの受け口（docs/screens.md の 2.2）
import { Fragment, useRef, useState, type DragEvent, type FormEvent } from "react";
import type { Column as ColumnData, Task } from "../types";
import { TaskCard } from "./TaskCard";

/** ドラッグ中に、どこへ落とすか。beforeId のカードの直前（null なら一番下） */
export interface DropTarget {
  columnId: number;
  beforeId: string | null;
}

interface Props {
  column: ColumnData;
  /** この列のタスク（上から順） */
  tasks: Task[];
  draggingId: string | null;
  /** この列が落とす先のときだけ渡される */
  dropBeforeId: string | null | undefined;
  onAdd: (title: string) => void;
  onOpen: (id: string) => void;
  onDelete: (id: string) => void;
  onDragStart: (id: string) => void;
  onDragEnd: () => void;
  onDragOver: (target: DropTarget) => void;
  onDragLeave: () => void;
  onDrop: (target: DropTarget) => void;
}

export function Column(props: Props) {
  const { column, tasks, draggingId, dropBeforeId } = props;
  const [title, setTitle] = useState("");
  const listRef = useRef<HTMLUListElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  function submit(e: FormEvent) {
    e.preventDefault();
    const trimmed = title.trim();
    if (!trimmed) return; // 空のタイトルは何も登録しない（メッセージも出さない）
    props.onAdd(trimmed);
    setTitle("");
    inputRef.current?.focus(); // 続けて入力できるよう、カーソルを残す
  }

  /**
   * マウスの位置より下にある最初のカード（＝落とす位置の直後のカード）の id。なければ null（一番下）。
   * ドラッグ中のカードと、登録の返事を待っているカード（仮の id はサーバーが知らない）は除く
   */
  function beforeIdAt(y: number): string | null {
    const cards =
      listRef.current?.querySelectorAll<HTMLElement>("[data-id]:not([data-dragging]):not([data-pending])") ?? [];
    for (const el of cards) {
      const r = el.getBoundingClientRect();
      if (y < r.top + r.height / 2) return el.dataset.id ?? null;
    }
    return null;
  }

  function dragOver(e: DragEvent) {
    if (!draggingId) return;
    e.preventDefault(); // これがないと drop が起きない
    props.onDragOver({ columnId: column.id, beforeId: beforeIdAt(e.clientY) });
  }

  function dragLeave(e: DragEvent) {
    if (!e.currentTarget.contains(e.relatedTarget as Node | null)) props.onDragLeave();
  }

  function drop(e: DragEvent) {
    if (!draggingId) return;
    e.preventDefault();
    props.onDrop({ columnId: column.id, beforeId: beforeIdAt(e.clientY) });
  }

  const isTarget = dropBeforeId !== undefined;
  const placeholder = <li className="mb-2 h-1 rounded-[2px] bg-accent" aria-hidden="true" />;

  return (
    <section
      className={`rounded-[10px] border-2 bg-column p-3 photo:bg-glass photo:backdrop-blur-[6px] ${
        isTarget ? "border-accent" : "border-transparent"
      }`}
      onDragOver={dragOver}
      onDragLeave={dragLeave}
      onDrop={drop}
    >
      <h2 className="mb-2.5 flex justify-between text-base font-bold">
        <span>{column.name}</span>
        <span className="font-normal text-muted">{tasks.length}</span>
      </h2>
      <ul className="min-h-2" ref={listRef}>
        {tasks.map((t) => (
          <Fragment key={t.id}>
            {isTarget && dropBeforeId === t.id && placeholder}
            <TaskCard
              task={t}
              dragging={t.id === draggingId}
              onOpen={() => props.onOpen(t.id)}
              onDelete={() => props.onDelete(t.id)}
              onDragStart={() => props.onDragStart(t.id)}
              onDragEnd={props.onDragEnd}
            />
          </Fragment>
        ))}
        {isTarget && dropBeforeId === null && placeholder}
      </ul>
      <form className="mt-1 flex gap-1.5" onSubmit={submit}>
        <input
          className="min-w-0 flex-1"
          ref={inputRef}
          placeholder="カードを追加…"
          autoComplete="off"
          aria-label={`${column.name}にカードを追加`}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
        <button className="border-none bg-accent font-semibold text-white">追加</button>
      </form>
    </section>
  );
}
