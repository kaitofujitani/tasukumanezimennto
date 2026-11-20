// API でやりとりするデータの形（docs/api.md の 4章）

export type Priority = "high" | "mid" | "low";

export type SortBy = "priority" | "due" | "created";

export interface Column {
  id: number;
  name: string;
  position: number;
}

export interface Task {
  id: string;
  columnId: number;
  title: string;
  description: string;
  priority: Priority;
  /** "YYYY-MM-DD"。期限なしは null */
  due: string | null;
  /** 列の中の表示順（0から、小さいほど上） */
  position: number;
  createdAt: string;
  /** 画面だけで使う印。登録してサーバーの返事を待っている間は true（まだ id が仮のもの） */
  pending?: boolean;
}

export interface Board {
  columns: Column[];
  tasks: Task[];
}

/** A5 で変えられる項目 */
export type TaskPatch = Partial<Pick<Task, "title" | "description" | "priority" | "due">>;

export const PRIORITY_LABEL: Record<Priority, string> = { high: "高", mid: "中", low: "低" };
