// 画面の動きのテスト。fetch を偽のサーバーに差し替え、送った API と画面の表示を確かめる
import { createEvent, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";
import type { Board, Task } from "./types";

interface Call {
  method: string;
  path: string;
  body: unknown;
}

function task(id: string, columnId: number, position: number, extra: Partial<Task> = {}): Task {
  return {
    id, columnId, title: id, description: "", priority: "mid", due: null, position,
    createdAt: "2026-11-20T00:00:00Z", ...extra,
  };
}

const COLUMNS = [
  { id: 1, name: "未着手", position: 0 },
  { id: 2, name: "進行中", position: 1 },
  { id: 3, name: "完了", position: 2 },
];

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
const noContent = () => new Response(null, { status: 204 });

/** 偽のサーバー。override で「METHOD /path」ごとに返事を差し替えられる */
function fakeServer(options: { loggedIn?: boolean; tasks?: Task[] } = {}) {
  let loggedIn = options.loggedIn ?? true;
  const board: Board = { columns: COLUMNS, tasks: options.tasks ?? [task("資料を作る", 1, 0), task("本を返す", 1, 1)] };
  const calls: Call[] = [];
  const override: Record<string, (body: unknown) => Response> = {};

  vi.stubGlobal(
    "fetch",
    vi.fn(async (path: string, init?: RequestInit) => {
      const method = init?.method ?? "GET";
      const body = init?.body ? JSON.parse(String(init.body)) : undefined;
      calls.push({ method, path, body });
      const key = `${method} ${path}`;
      if (override[key]) return override[key](body);

      if (key === "POST /api/login") {
        if ((body as { password: string }).password !== "secret") return json(401, { error: "パスワードが違います" });
        loggedIn = true;
        return noContent();
      }
      if (!loggedIn) return json(401, { error: "ログインしてください" });
      if (key === "GET /api/board") return json(200, board);
      if (key === "POST /api/logout") {
        loggedIn = false;
        return noContent();
      }
      if (key === "POST /api/tasks") {
        const b = body as { columnId: number; title: string };
        return json(201, task("new-id", b.columnId, 99, { title: b.title }));
      }
      if (method === "PATCH") {
        const id = decodeURIComponent(path.split("/").pop()!);
        return json(200, { ...board.tasks.find((t) => t.id === id), ...(body as object) });
      }
      return noContent();
    }),
  );
  return { calls, override, board };
}

/** 未着手の列 */
async function todoColumn() {
  const heading = await screen.findByText("未着手", { selector: "h2 span" });
  return heading.closest("section")!;
}

beforeEach(() => {
  vi.spyOn(window, "confirm").mockReturnValue(true);
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("ログイン", () => {
  it("ログインしていなければログイン画面を出し、正しいパスワードでボードを表示する", async () => {
    fakeServer({ loggedIn: false });
    const user = userEvent.setup();
    render(<App />);

    const input = await screen.findByLabelText("パスワード");
    const button = screen.getByRole("button", { name: "ログイン" });
    expect(button).toBeDisabled(); // 空のあいだは押せない

    await user.type(input, "wrong");
    await user.click(button);
    expect(await screen.findByText("パスワードが違います")).toBeInTheDocument();
    expect(input).toHaveValue(""); // 間違えたら入力欄を空にする

    await user.type(input, "secret{Enter}");
    expect(await screen.findByText("資料を作る")).toBeInTheDocument();
    expect(screen.getByText("進行中")).toBeInTheDocument();
  });

  it("ログアウトするとログイン画面に戻る", async () => {
    const server = fakeServer();
    const user = userEvent.setup();
    render(<App />);
    await screen.findByText("資料を作る");

    await user.click(screen.getByRole("button", { name: "ログアウト" }));
    expect(await screen.findByLabelText("パスワード")).toBeInTheDocument();
    expect(server.calls.some((c) => c.method === "POST" && c.path === "/api/logout")).toBe(true);
  });

  it("保存のときにログインが切れていたらログイン画面に戻る", async () => {
    const server = fakeServer();
    server.override["DELETE /api/tasks/%E6%9C%AC%E3%82%92%E8%BF%94%E3%81%99"] = () =>
      json(401, { error: "ログインしてください" });
    const user = userEvent.setup();
    render(<App />);
    await screen.findByText("本を返す");

    await user.click(screen.getByRole("button", { name: "「本を返す」を削除" }));
    expect(await screen.findByLabelText("パスワード")).toBeInTheDocument();
  });
});

describe("ボード画面", () => {
  it("列の見出しに件数を、カードに優先度と期限を表示する", async () => {
    fakeServer({ tasks: [task("資料を作る", 1, 0, { priority: "high", due: "2026-11-25" }), task("買い物", 2, 0)] });
    render(<App />);

    const todo = await todoColumn();
    expect(within(todo).getByText("1")).toBeInTheDocument();
    expect(within(todo).getByText("優先度: 高")).toBeInTheDocument();
    expect(within(todo).getByText("期限: 2026/11/25")).toBeInTheDocument();
  });

  it("入力欄から列の一番下に登録し、入力欄を空にする", async () => {
    const server = fakeServer();
    const user = userEvent.setup();
    render(<App />);
    const todo = await todoColumn();

    const input = within(todo).getByLabelText("未着手にカードを追加");
    await user.type(input, "  新しいタスク  {Enter}");

    await waitFor(() =>
      expect(server.calls).toContainEqual({ method: "POST", path: "/api/tasks", body: { columnId: 1, title: "新しいタスク" } }),
    );
    const titles = within(todo).getAllByRole("listitem").map((li) => within(li).getByTestId("task-title").textContent);
    expect(titles).toEqual(["資料を作る", "本を返す", "新しいタスク"]);
    expect(input).toHaveValue("");
    expect(input).toHaveFocus();
  });

  it("空白だけのタイトルは登録しない", async () => {
    const server = fakeServer();
    const user = userEvent.setup();
    render(<App />);
    const todo = await todoColumn();

    await user.type(within(todo).getByLabelText("未着手にカードを追加"), "   {Enter}");
    expect(server.calls.some((c) => c.path === "/api/tasks")).toBe(false);
  });

  it("×で確認してから削除する", async () => {
    const server = fakeServer();
    const user = userEvent.setup();
    render(<App />);
    await screen.findByText("本を返す");

    await user.click(screen.getByRole("button", { name: "「本を返す」を削除" }));
    expect(window.confirm).toHaveBeenCalledWith("「本を返す」を削除しますか？");
    expect(screen.queryByText("本を返す")).not.toBeInTheDocument();
    expect(server.calls.some((c) => c.method === "DELETE")).toBe(true);
    // 詳細画面は開かない
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("×の確認でキャンセルしたら削除しない", async () => {
    vi.mocked(window.confirm).mockReturnValue(false);
    const server = fakeServer();
    const user = userEvent.setup();
    render(<App />);
    await screen.findByText("本を返す");

    await user.click(screen.getByRole("button", { name: "「本を返す」を削除" }));
    expect(screen.getByText("本を返す")).toBeInTheDocument();
    expect(server.calls.some((c) => c.method === "DELETE")).toBe(false);
  });

  it("保存に失敗したらエラーの帯を出し、ボードを読み込み直す", async () => {
    const server = fakeServer();
    server.override["DELETE /api/tasks/%E6%9C%AC%E3%82%92%E8%BF%94%E3%81%99"] = () => json(500, {});
    const user = userEvent.setup();
    render(<App />);
    await screen.findByText("本を返す");

    await user.click(screen.getByRole("button", { name: "「本を返す」を削除" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("保存できませんでした。通信状況を確認してください");
    // 読み込み直したので、サーバーに残っているタスクが表示に戻る
    expect(await screen.findByText("本を返す")).toBeInTheDocument();
    expect(server.calls.filter((c) => c.path === "/api/board")).toHaveLength(2);
  });

  it("ボードを読み込めなければ、お知らせと再読み込みボタンを出す", async () => {
    const server = fakeServer();
    server.override["GET /api/board"] = () => json(500, {});
    const user = userEvent.setup();
    render(<App />);

    expect(await screen.findByText("読み込めませんでした", { selector: "p" })).toBeInTheDocument();
    delete server.override["GET /api/board"];
    await user.click(screen.getByRole("button", { name: "再読み込み" }));
    expect(await screen.findByText("資料を作る")).toBeInTheDocument();
  });

  it("登録の返事を待っている間は並べ替えを押せない", async () => {
    const server = fakeServer();
    // 登録の返事は、respond を呼ぶまで返さない
    let respond: ((res: Response) => void) | undefined;
    server.override["POST /api/tasks"] = () =>
      new Promise<Response>((resolve) => {
        respond = resolve;
      }) as unknown as Response;
    const user = userEvent.setup();
    render(<App />);
    const todo = await todoColumn();
    const sortButton = screen.getByRole("button", { name: "並べ替え" });
    expect(sortButton).toBeEnabled();

    await user.type(within(todo).getByLabelText("未着手にカードを追加"), "登録中{Enter}");
    expect(sortButton).toBeDisabled();

    respond?.(json(201, task("new-id", 1, 2, { title: "登録中" })));
    await waitFor(() => expect(sortButton).toBeEnabled());
  });

  it("選んだ並べ方で並べ替え、サーバーが返した順に表示する", async () => {
    const server = fakeServer();
    server.override["POST /api/tasks/sort"] = () =>
      json(200, { columns: COLUMNS, tasks: [task("本を返す", 1, 0), task("資料を作る", 1, 1)] });
    const user = userEvent.setup();
    render(<App />);
    const todo = await todoColumn();

    await user.selectOptions(screen.getByLabelText("並べ方"), "due");
    expect(server.calls.some((c) => c.path === "/api/tasks/sort")).toBe(false); // 選んだだけでは並べ替えない
    await user.click(screen.getByRole("button", { name: "並べ替え" }));

    await waitFor(() => {
      const titles = within(todo).getAllByRole("listitem").map((li) => within(li).getByTestId("task-title").textContent);
      expect(titles).toEqual(["本を返す", "資料を作る"]);
    });
    expect(server.calls).toContainEqual({ method: "POST", path: "/api/tasks/sort", body: { by: "due" } });
  });

  it("登録の返事を待っているカードは、落とす位置の目印にしない", async () => {
    const server = fakeServer({ tasks: [task("買い物", 2, 0)] });
    server.override["POST /api/tasks"] = () => new Promise<Response>(() => {}) as unknown as Response; // 返事が来ない
    // jsdom は要素の位置を計算しないので、どのカードも上端 10px・高さ 20px にする
    vi.spyOn(Element.prototype, "getBoundingClientRect").mockReturnValue(new DOMRect(0, 10, 100, 20));
    const user = userEvent.setup();
    render(<App />);
    const todo = await todoColumn();
    await user.type(within(todo).getByLabelText("未着手にカードを追加"), "登録中{Enter}");

    // 「買い物」を、未着手の返事待ちのカードより上（y=0）に落とす
    drag("dragStart", screen.getByText("買い物"));
    drag("dragOver", todo, 0);
    drag("drop", todo, 0);

    await waitFor(() =>
      expect(server.calls).toContainEqual({
        method: "POST", path: "/api/tasks/%E8%B2%B7%E3%81%84%E7%89%A9/move", body: { columnId: 1, beforeId: null },
      }),
    );
  });
});

/** ドラッグのイベントを送る。jsdom にはドラッグのイベントの clientY と dataTransfer がないので足す */
function drag(type: "dragStart" | "dragOver" | "drop", el: Element, clientY = 0) {
  const event = createEvent[type](el, { dataTransfer: { setData: () => {}, effectAllowed: "" } });
  Object.defineProperty(event, "clientY", { value: clientY });
  fireEvent(el, event);
}

describe("タスク詳細画面", () => {
  it.each(["{Enter}", " "])("カードはキーボードで選んで %s でも開ける", async (key) => {
    fakeServer();
    const user = userEvent.setup();
    render(<App />);
    await screen.findByText("資料を作る");

    // 1枚目のカードの「開く」部分まで Tab で進む（並べ方・並べ替え・ログアウトの次）
    for (let i = 0; i < 4; i++) await user.tab();
    expect(document.activeElement).toHaveAttribute("role", "button");
    expect(document.activeElement).toHaveTextContent("資料を作る");
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();

    await user.keyboard(key);
    expect(screen.getByRole("dialog")).toBeInTheDocument();
  });

  it("優先度と期限は変えたときに、タイトルは入力が止まってから保存する", async () => {
    const server = fakeServer();
    const user = userEvent.setup();
    render(<App />);
    await user.click(await screen.findByText("資料を作る"));
    const dialog = screen.getByRole("dialog");

    await user.selectOptions(within(dialog).getByLabelText("優先度"), "high");
    expect(server.calls).toContainEqual({
      method: "PATCH", path: "/api/tasks/%E8%B3%87%E6%96%99%E3%82%92%E4%BD%9C%E3%82%8B", body: { priority: "high" },
    });

    const title = within(dialog).getByLabelText("タイトル");
    await user.clear(title);
    await user.type(title, "見積書を作る");
    // 1文字ずつではなく、まとめて1回だけ保存する
    await waitFor(() =>
      expect(server.calls.filter((c) => c.method === "PATCH" && (c.body as { title?: string }).title)).toEqual([
        { method: "PATCH", path: "/api/tasks/%E8%B3%87%E6%96%99%E3%82%92%E4%BD%9C%E3%82%8B", body: { title: "見積書を作る" } },
      ]),
    );
    expect(screen.getByText("優先度: 高")).toBeInTheDocument();
  });

  it("閉じるときに、まだ保存していない説明を保存する", async () => {
    const server = fakeServer();
    const user = userEvent.setup();
    render(<App />);
    await user.click(await screen.findByText("資料を作る"));
    const dialog = screen.getByRole("dialog");

    await user.type(within(dialog).getByLabelText("説明"), "メモ");
    await user.click(within(dialog).getByRole("button", { name: "閉じる" }));

    expect(server.calls.filter((c) => c.method === "PATCH")).toEqual([
      { method: "PATCH", path: "/api/tasks/%E8%B3%87%E6%96%99%E3%82%92%E4%BD%9C%E3%82%8B", body: { description: "メモ" } },
    ]);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("上へ・下へは端では押せず、上へで1つ上のタスクの直前へ動かす", async () => {
    const server = fakeServer();
    const user = userEvent.setup();
    render(<App />);
    await user.click(await screen.findByText("本を返す"));
    const dialog = screen.getByRole("dialog");

    expect(within(dialog).getByRole("button", { name: "下へ" })).toBeDisabled();
    await user.click(within(dialog).getByRole("button", { name: "上へ" }));

    expect(server.calls).toContainEqual({
      method: "POST", path: "/api/tasks/%E6%9C%AC%E3%82%92%E8%BF%94%E3%81%99/move", body: { columnId: 1, beforeId: "資料を作る" },
    });
    expect(within(dialog).getByRole("button", { name: "上へ" })).toBeDisabled();
  });

  it("ステータスのボタンを押すと、その列の一番下へ動かす", async () => {
    const server = fakeServer();
    const user = userEvent.setup();
    render(<App />);
    await user.click(await screen.findByText("資料を作る"));
    const status = within(screen.getByRole("dialog")).getByRole("group", { name: "ステータス" });
    expect(within(status).getByRole("button", { name: "未着手" })).toHaveAttribute("aria-pressed", "true");

    await user.click(within(status).getByRole("button", { name: "進行中" }));
    expect(server.calls).toContainEqual({
      method: "POST", path: "/api/tasks/%E8%B3%87%E6%96%99%E3%82%92%E4%BD%9C%E3%82%8B/move", body: { columnId: 2, beforeId: null },
    });
    const doing = screen.getByText("進行中", { selector: "h2 span" }).closest("section")!;
    expect(within(doing).getByText("資料を作る")).toBeInTheDocument();
  });

  it("削除は確認してから行い、画面を閉じる", async () => {
    const server = fakeServer();
    const user = userEvent.setup();
    render(<App />);
    await user.click(await screen.findByText("資料を作る"));

    await user.click(within(screen.getByRole("dialog")).getByRole("button", { name: "削除" }));
    expect(window.confirm).toHaveBeenCalledWith("「資料を作る」を削除しますか？");
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(screen.queryByText("資料を作る")).not.toBeInTheDocument();
    expect(server.calls.some((c) => c.method === "DELETE")).toBe(true);
  });
});
