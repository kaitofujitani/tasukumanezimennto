// S2 タスク詳細画面（docs/screens.md の 2.3）。ボード画面の上に <dialog> で重ねて表示する
import { useEffect, useRef, useState, type FormEvent } from "react";
import type { Column, Priority, Task, TaskPatch } from "../types";

/** タイトルと説明は、入力がこの時間止まったらまとめて保存する */
export const SAVE_DELAY_MS = 500;

/** 項目名（薄い色）と入力欄（*:text-fg で通常の色）を縦に並べる */
const LABEL_CLASS = "grid gap-1 text-[.85rem] text-muted *:text-fg";
/** 2つの項目を横に並べる行 */
const ROW_CLASS = "grid grid-cols-2 items-end gap-3";

interface Props {
  task: Task;
  columns: Column[];
  canMoveUp: boolean;
  canMoveDown: boolean;
  /** 画面のタスクを書き換えてサーバーに保存する */
  onSave: (patch: TaskPatch) => void;
  onMoveToColumn: (columnId: number) => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
  onDelete: () => void;
  onClose: () => void;
}

export function TaskDetail({ task, columns, canMoveUp, canMoveDown, ...props }: Props) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [title, setTitle] = useState(task.title);
  const [description, setDescription] = useState(task.description);

  // まだ保存していないタイトル・説明と、保存までの待ち時間
  const unsaved = useRef<TaskPatch>({});
  const timer = useRef<number | undefined>(undefined);
  const onSaveRef = useRef(props.onSave);
  useEffect(() => {
    onSaveRef.current = props.onSave;
  });

  useEffect(() => {
    const dialog = dialogRef.current;
    if (dialog && !dialog.open) dialog.showModal();
    return () => window.clearTimeout(timer.current);
  }, []);

  /** 保存していない入力があれば、すぐに保存する */
  function flush() {
    window.clearTimeout(timer.current);
    const patch = unsaved.current;
    unsaved.current = {};
    if (Object.keys(patch).length > 0) onSaveRef.current(patch);
  }

  function saveLater(patch: TaskPatch) {
    unsaved.current = { ...unsaved.current, ...patch };
    window.clearTimeout(timer.current);
    timer.current = window.setTimeout(flush, SAVE_DELAY_MS);
  }

  function changeTitle(value: string) {
    setTitle(value);
    const trimmed = value.trim();
    if (trimmed) {
      saveLater({ title: trimmed });
    } else {
      // タイトルは必須。空のあいだは保存しない
      delete unsaved.current.title;
    }
  }

  function close() {
    dialogRef.current?.close(); // close イベントで flush と onClose が呼ばれる
  }

  function remove() {
    if (!confirm(`「${task.title}」を削除しますか？`)) return;
    window.clearTimeout(timer.current);
    unsaved.current = {};
    props.onDelete();
  }

  function submit(e: FormEvent) {
    e.preventDefault();
    close();
  }

  return (
    // 余白は form 側に持たせ、dialog 自体のクリック＝背景のクリックとして扱う。
    // Tailwind は全要素の margin を 0 にするので、m-auto で画面の中央に置く。
    // 背景のクリックで閉じる操作は、キーボードでは Esc（ブラウザ標準）が同じ働きをするので、キー操作は付けない
    // oxlint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-noninteractive-element-interactions
    <dialog
      className="m-auto w-[min(480px,calc(100vw-32px))] rounded-xl border border-line bg-bg p-0 text-fg backdrop:bg-black/40"
      ref={dialogRef}
      aria-labelledby="detail-heading"
      onClose={() => {
        flush();
        props.onClose();
      }}
      // 背景（dialog の外側）をクリックしたら閉じる。Esc キーでも閉じる（ブラウザ標準）
      onClick={(e) => {
        if (e.target === e.currentTarget) close();
      }}
    >
      <form className="grid gap-3 p-5" onSubmit={submit}>
        <h2 id="detail-heading" className="text-[1.1rem] font-bold">
          カードの詳細
        </h2>
        <label className={LABEL_CLASS}>
          タイトル
          <input
            value={title}
            autoComplete="off"
            onChange={(e) => changeTitle(e.target.value)}
            // 空のまま入力欄から離れたら、元のタイトルに戻す
            onBlur={() => {
              if (!title.trim()) setTitle(task.title);
            }}
          />
        </label>
        <label className={LABEL_CLASS}>
          説明
          <textarea
            className="resize-y"
            rows={4}
            value={description}
            onChange={(e) => {
              setDescription(e.target.value);
              saveLater({ description: e.target.value });
            }}
          />
        </label>
        <div className={ROW_CLASS}>
          <label className={LABEL_CLASS}>
            優先度
            <select value={task.priority} onChange={(e) => props.onSave({ priority: e.target.value as Priority })}>
              <option value="high">高</option>
              <option value="mid">中</option>
              <option value="low">低</option>
            </select>
          </label>
          <label className={LABEL_CLASS}>
            期限
            <input
              type="date"
              value={task.due ?? ""}
              onChange={(e) => props.onSave({ due: e.target.value || null })}
            />
          </label>
        </div>
        <div className={ROW_CLASS}>
          {/* ステータス＝列。今の列のボタンを青くし、ほかを押すとその列の一番下へ動かす */}
          <fieldset className="min-w-0 text-[.85rem] text-muted">
            <legend className="mb-1">ステータス</legend>
            <div className="flex gap-1.5">
              {columns.map((c) => {
                const current = c.id === task.columnId;
                return (
                  <button
                    key={c.id}
                    type="button"
                    className={`flex-1 px-1 ${current ? "border-accent bg-accent font-semibold text-white" : "text-fg"}`}
                    aria-pressed={current}
                    onClick={() => !current && props.onMoveToColumn(c.id)}
                  >
                    {c.name}
                  </button>
                );
              })}
            </div>
          </fieldset>
          <div className="flex gap-1.5 *:flex-1">
            <button type="button" disabled={!canMoveUp} onClick={props.onMoveUp}>
              上へ
            </button>
            <button type="button" disabled={!canMoveDown} onClick={props.onMoveDown}>
              下へ
            </button>
          </div>
        </div>
        <div className="mt-1 flex justify-between">
          <button type="button" className="border-danger bg-transparent text-danger" onClick={remove}>
            削除
          </button>
          <button className="border-none bg-accent font-semibold text-white">閉じる</button>
        </div>
      </form>
    </dialog>
  );
}
