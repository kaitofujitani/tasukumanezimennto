// 画面の中のタスクの並びを書き換える処理（サーバーに保存する前に、先に画面を書き換えるため）。
// どれも元の配列は変えず、新しい配列を返す。列の中の順番は position で表し、書き換えたら 0 から振り直す
import type { Task, TaskPatch } from "./types";

/** 指定した列のタスクを上から順に返す */
export function tasksIn(tasks: Task[], columnId: number): Task[] {
  return tasks.filter((t) => t.columnId === columnId).sort((a, b) => a.position - b.position);
}

function renumber(list: Task[]): Task[] {
  return list.map((t, i) => (t.position === i ? t : { ...t, position: i }));
}

/** changed に入っているタスクを差し替えた配列を返す */
function merge(tasks: Task[], changed: Task[]): Task[] {
  const byId = new Map(changed.map((t) => [t.id, t]));
  return [...tasks.filter((t) => !byId.has(t.id)), ...byId.values()];
}

/** 列の一番下に入れる */
export function addTask(tasks: Task[], task: Task): Task[] {
  return [...tasks, { ...task, position: tasksIn(tasks, task.columnId).length }];
}

/** 消して、同じ列の残りを詰める */
export function removeTask(tasks: Task[], id: string): Task[] {
  const task = tasks.find((t) => t.id === id);
  if (!task) return tasks;
  const rest = tasks.filter((t) => t.id !== id);
  return merge(rest, renumber(tasksIn(rest, task.columnId)));
}

/** columnId の列の beforeId の直前へ動かす（beforeId が null なら一番下）。API の A7 と同じ動き */
export function moveTask(tasks: Task[], id: string, columnId: number, beforeId: string | null): Task[] {
  const task = tasks.find((t) => t.id === id);
  if (!task || beforeId === id) return tasks;
  const rest = tasks.filter((t) => t.id !== id);
  const dest = tasksIn(rest, columnId);
  let index = beforeId === null ? -1 : dest.findIndex((t) => t.id === beforeId);
  if (index < 0) index = dest.length;
  dest.splice(index, 0, { ...task, columnId });
  const changed = renumber(dest);
  if (task.columnId !== columnId) changed.push(...renumber(tasksIn(rest, task.columnId)));
  return merge(rest, changed);
}

export function updateTask(tasks: Task[], id: string, patch: TaskPatch): Task[] {
  return tasks.map((t) => (t.id === id ? { ...t, ...patch } : t));
}

/** 仮の id で入れたタスクを、サーバーが返したタスクに差し替える（並び順は画面のものを保つ） */
export function replaceTask(tasks: Task[], oldId: string, task: Task): Task[] {
  return tasks.map((t) => (t.id === oldId ? { ...task, position: t.position } : t));
}
