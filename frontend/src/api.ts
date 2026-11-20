// API を呼ぶ処理（docs/api.md）。通信はブラウザ標準の fetch で行う
import type { Board, SortBy, Task, TaskPatch } from "./types";

/** API がエラーを返したとき。status が 0 のときは通信そのものができなかった */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

/** CSRF 対策のトークン。サーバーが Cookie XSRF-TOKEN で渡してくる（docs/api.md の 5章） */
function xsrfToken(): string | undefined {
  const pair = document.cookie.split("; ").find((c) => c.startsWith("XSRF-TOKEN="));
  return pair ? decodeURIComponent(pair.slice("XSRF-TOKEN=".length)) : undefined;
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (method !== "GET") {
    const token = xsrfToken();
    if (token) headers["X-XSRF-TOKEN"] = token;
  }

  let res: Response;
  try {
    res = await fetch(path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      credentials: "same-origin",
    });
  } catch {
    throw new ApiError(0, "通信できませんでした");
  }

  if (!res.ok) {
    let message = "";
    try {
      message = (await res.json()).error ?? "";
    } catch {
      // 本文が JSON でなければ理由なし
    }
    throw new ApiError(res.status, message);
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

const taskPath = (id: string) => `/api/tasks/${encodeURIComponent(id)}`;

export const api = {
  login: (password: string) => request<void>("POST", "/api/login", { password }),
  logout: () => request<void>("POST", "/api/logout"),
  getBoard: () => request<Board>("GET", "/api/board"),
  createTask: (columnId: number, title: string) => request<Task>("POST", "/api/tasks", { columnId, title }),
  updateTask: (id: string, patch: TaskPatch) => request<Task>("PATCH", taskPath(id), patch),
  deleteTask: (id: string) => request<void>("DELETE", taskPath(id)),
  /** columnId の列の beforeId の直前へ動かす。beforeId が null なら列の一番下 */
  moveTask: (id: string, columnId: number, beforeId: string | null) =>
    request<void>("POST", `${taskPath(id)}/move`, { columnId, beforeId }),
  sortTasks: (by: SortBy) => request<Board>("POST", "/api/tasks/sort", { by }),
};
