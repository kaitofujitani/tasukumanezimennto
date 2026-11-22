// S1 ボード画面（docs/screens.md の 2.2）。ボードのデータを持ち、操作を API に保存する。
// 操作したら先に画面を書き換え、そのあとサーバーに保存する。保存に失敗したらエラーの帯を出し、ボードを読み込み直す
import { useCallback, useEffect, useRef, useState } from "react";
import { api, ApiError } from "../api";
import { addTask, moveTask, removeTask, replaceTask, tasksIn, updateTask } from "../board";
import type { Column as ColumnData, SortBy, Task, TaskPatch } from "../types";
import { Column, type DropTarget } from "./Column";
import { TaskDetail } from "./TaskDetail";

/** 読み込みがこの時間を超えたら「サーバーを起動しています」と出す（無料のサーバーは休止から起きるのに時間がかかる） */
export const SLOW_NOTICE_MS = 3000;
/** エラーの帯を消すまでの時間 */
const BANNER_MS = 5000;
const SAVE_ERROR = "保存できませんでした。通信状況を確認してください";
/** 読み込み中・読み込み失敗のお知らせ */
const STATUS_CLASS =
  "rounded-[10px] bg-column px-4 py-8 text-center text-muted photo:bg-glass photo:backdrop-blur-[6px]";

type LoadState = "loading" | "slow" | "error" | "ready";

interface Banner {
  message: string;
  /** true のときは「再読み込み」ボタンを出し、自動では消さない */
  retry: boolean;
}

interface Props {
  /** ログインしていない・ログインの期限が切れたとき */
  onUnauthorized: () => void;
  onLoggedOut: () => void;
}

export function Board({ onUnauthorized, onLoggedOut }: Props) {
  const [columns, setColumns] = useState<ColumnData[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [banner, setBanner] = useState<Banner | null>(null);
  const [sortBy, setSortBy] = useState<SortBy>("priority");
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const [dropTarget, setDropTarget] = useState<DropTarget | null>(null);
  const bannerTimer = useRef<number | undefined>(undefined);

  const showBanner = useCallback((message: string, retry = false) => {
    window.clearTimeout(bannerTimer.current);
    setBanner({ message, retry });
    if (!retry) bannerTimer.current = window.setTimeout(() => setBanner(null), BANNER_MS);
  }, []);

  useEffect(() => () => window.clearTimeout(bannerTimer.current), []);

  /** ボードを読み込む。showStatus が false のときは、今の表示のまま裏で読み込み直す */
  const load = useCallback(
    async (showStatus: boolean) => {
      let slowTimer: number | undefined;
      if (showStatus) {
        setLoadState("loading");
        slowTimer = window.setTimeout(
          () => setLoadState((s) => (s === "loading" ? "slow" : s)),
          SLOW_NOTICE_MS,
        );
      }
      try {
        const board = await api.getBoard();
        setColumns([...board.columns].sort((a, b) => a.position - b.position));
        setTasks(board.tasks);
        setLoadState("ready");
      } catch (e) {
        if (e instanceof ApiError && e.status === 401) {
          onUnauthorized();
        } else {
          setLoadState("error");
          showBanner("読み込めませんでした", true);
        }
      } finally {
        window.clearTimeout(slowTimer);
      }
    },
    [onUnauthorized, showBanner],
  );

  useEffect(() => {
    load(true);
  }, [load]);

  /** 保存に失敗したとき。ログインが切れていればログイン画面へ、それ以外は帯を出して読み込み直す */
  function failed(e: unknown) {
    if (e instanceof ApiError && e.status === 401) {
      onUnauthorized();
      return;
    }
    showBanner(SAVE_ERROR);
    load(false);
  }

  function save(request: Promise<unknown>) {
    request.catch(failed);
  }

  // ---- 操作 ----

  function add(columnId: number, title: string) {
    const tempId = `temp-${crypto.randomUUID()}`;
    setTasks((ts) =>
      addTask(ts, {
        id: tempId, columnId, title, description: "", priority: "mid", due: null,
        position: 0, createdAt: new Date().toISOString(), pending: true,
      }),
    );
    api
      .createTask(columnId, title)
      .then((created) => setTasks((ts) => replaceTask(ts, tempId, created)))
      .catch(failed);
  }

  function remove(id: string) {
    setTasks((ts) => removeTask(ts, id));
    if (editingId === id) setEditingId(null);
    save(api.deleteTask(id));
  }

  function move(id: string, columnId: number, beforeId: string | null) {
    const before = tasks.find((t) => t.id === id);
    const next = moveTask(tasks, id, columnId, beforeId);
    const after = next.find((t) => t.id === id);
    // 同じ場所に落としたときは何もしない
    if (!before || !after || (before.columnId === after.columnId && before.position === after.position)) return;
    setTasks(next);
    save(api.moveTask(id, columnId, beforeId));
  }

  function update(id: string, patch: TaskPatch) {
    setTasks((ts) => updateTask(ts, id, patch));
    save(api.updateTask(id, patch));
  }

  async function sort() {
    try {
      const board = await api.sortTasks(sortBy);
      setTasks(board.tasks);
    } catch (e) {
      failed(e);
    }
  }

  async function logout() {
    try {
      await api.logout();
    } catch (e) {
      if (!(e instanceof ApiError && e.status === 401)) {
        showBanner("ログアウトできませんでした。通信状況を確認してください");
        return;
      }
    }
    onLoggedOut();
  }

  // ---- ドラッグ＆ドロップ ----

  function dragOver(target: DropTarget) {
    setDropTarget((prev) =>
      prev && prev.columnId === target.columnId && prev.beforeId === target.beforeId ? prev : target,
    );
  }

  function drop(target: DropTarget) {
    const id = draggingId;
    setDraggingId(null);
    setDropTarget(null);
    if (id) move(id, target.columnId, target.beforeId);
  }

  // ---- 詳細画面 ----

  const editing = editingId ? tasks.find((t) => t.id === editingId) : undefined;
  // 読み込み直しなどで開いていたタスクがなくなったら、詳細画面を閉じる
  useEffect(() => {
    if (editingId && !editing) setEditingId(null);
  }, [editingId, editing]);

  function detail(task: Task) {
    const same = tasksIn(tasks, task.columnId);
    const i = same.findIndex((t) => t.id === task.id);
    return (
      <TaskDetail
        key={task.id}
        task={task}
        columns={columns}
        canMoveUp={i > 0}
        canMoveDown={i < same.length - 1}
        onSave={(patch) => update(task.id, patch)}
        onMoveToColumn={(columnId) => {
          if (columnId !== task.columnId) move(task.id, columnId, null); // 選んだ列の一番下へ
        }}
        // 上へ：1つ上のタスクの直前へ。下へ：2つ下のタスクの直前へ（なければ一番下）
        onMoveUp={() => i > 0 && move(task.id, task.columnId, same[i - 1].id)}
        onMoveDown={() => move(task.id, task.columnId, same[i + 2]?.id ?? null)}
        onDelete={() => remove(task.id)}
        onClose={() => setEditingId(null)}
      />
    );
  }

  return (
    <main className="mx-auto max-w-[1100px] px-4 pt-8 pb-12">
      {banner && (
        // エラーの帯（画面の上部に出す）
        <div
          className="fixed top-3 left-1/2 z-10 flex w-max max-w-[calc(100vw-32px)] -translate-x-1/2 items-center gap-3 rounded-lg bg-danger px-4 py-2.5 text-white shadow-[0_4px_12px_rgb(0_0_0/.25)]"
          role="alert"
        >
          <span>{banner.message}</span>
          {banner.retry && (
            <button
              type="button"
              className="border-none bg-white px-2.5 py-1 font-semibold text-[#b91c1c]"
              onClick={() => {
                setBanner(null);
                load(true);
              }}
            >
              再読み込み
            </button>
          )}
        </div>
      )}
      <header className="mb-5 flex flex-wrap items-center justify-between gap-3 photo:rounded-[10px] photo:bg-glass photo:px-3 photo:py-2.5 photo:backdrop-blur-[6px]">
        <h1 className="text-2xl font-bold">ボード</h1>
        <div className="flex flex-wrap gap-1.5">
          <select aria-label="並べ方" value={sortBy} onChange={(e) => setSortBy(e.target.value as SortBy)}>
            <option value="priority">優先度順</option>
            <option value="due">期限順</option>
            <option value="created">登録順</option>
          </select>
          <button type="button" onClick={sort} disabled={loadState !== "ready"}>
            並べ替え
          </button>
          <button type="button" onClick={logout}>
            ログアウト
          </button>
        </div>
      </header>

      {loadState === "slow" && <p className={STATUS_CLASS}>サーバーを起動しています。1〜2分ほどかかることがあります</p>}
      {loadState === "error" && <p className={STATUS_CLASS}>読み込めませんでした</p>}
      {loadState === "ready" && (
        <div className="grid grid-cols-3 items-start gap-4 max-[720px]:grid-cols-1">
          {columns.map((c) => (
            <Column
              key={c.id}
              column={c}
              tasks={tasksIn(tasks, c.id)}
              draggingId={draggingId}
              dropBeforeId={dropTarget?.columnId === c.id ? dropTarget.beforeId : undefined}
              onAdd={(title) => add(c.id, title)}
              onOpen={setEditingId}
              onDelete={remove}
              onDragStart={setDraggingId}
              onDragEnd={() => {
                setDraggingId(null);
                setDropTarget(null);
              }}
              onDragOver={dragOver}
              onDragLeave={() => setDropTarget(null)}
              onDrop={drop}
            />
          ))}
        </div>
      )}
      {editing && detail(editing)}
    </main>
  );
}
