// S2 タスク詳細画面（docs/screens.md の 2.3）。ボード画面の上に <dialog> で重ねて表示する
import { useEffect, useRef, useState, type FormEvent } from "react";
import type { Column, Priority, Task, TaskPatch } from "../types";

/** タイトルと説明は、入力がこの時間止まったらまとめて保存する */
export const SAVE_DELAY_MS = 500;

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
    <dialog
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
      <form className="detail-form" onSubmit={submit}>
        <h2 id="detail-heading">カードの詳細</h2>
        <label>
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
        <label>
          説明
          <textarea
            rows={4}
            value={description}
            onChange={(e) => {
              setDescription(e.target.value);
              saveLater({ description: e.target.value });
            }}
          />
        </label>
        <div className="row">
          <label>
            優先度
            <select value={task.priority} onChange={(e) => props.onSave({ priority: e.target.value as Priority })}>
              <option value="high">高</option>
              <option value="mid">中</option>
              <option value="low">低</option>
            </select>
          </label>
          <label>
            期限
            <input
              type="date"
              value={task.due ?? ""}
              onChange={(e) => props.onSave({ due: e.target.value || null })}
            />
          </label>
        </div>
        <div className="row">
          <label>
            列
            <select value={task.columnId} onChange={(e) => props.onMoveToColumn(Number(e.target.value))}>
              {columns.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </label>
          <div className="order">
            <button type="button" disabled={!canMoveUp} onClick={props.onMoveUp}>
              上へ
            </button>
            <button type="button" disabled={!canMoveDown} onClick={props.onMoveDown}>
              下へ
            </button>
          </div>
        </div>
        <div className="actions">
          <button type="button" className="danger" onClick={remove}>
            削除
          </button>
          <button className="primary">閉じる</button>
        </div>
      </form>
    </dialog>
  );
}
