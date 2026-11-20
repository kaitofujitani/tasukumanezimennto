import { describe, expect, it } from "vitest";
import { addTask, moveTask, removeTask, replaceTask, tasksIn } from "./board";
import type { Task } from "./types";

function task(id: string, columnId: number, position: number): Task {
  return { id, columnId, title: id, description: "", priority: "mid", due: null, position, createdAt: "" };
}

/** 列ごとの「id:position」の並び */
function layout(tasks: Task[]) {
  return [1, 2, 3].map((c) => tasksIn(tasks, c).map((t) => `${t.id}:${t.position}`));
}

const initial = [task("a", 1, 0), task("b", 1, 1), task("c", 1, 2), task("x", 2, 0)];

describe("moveTask", () => {
  it("同じ列の一番下へ動かす", () => {
    expect(layout(moveTask(initial, "a", 1, null))).toEqual([["b:0", "c:1", "a:2"], ["x:0"], []]);
  });

  it("同じ列で1つ上へ動かす", () => {
    expect(layout(moveTask(initial, "c", 1, "b"))).toEqual([["a:0", "c:1", "b:2"], ["x:0"], []]);
  });

  it("別の列のタスクの直前へ動かし、両方の列を詰め直す", () => {
    expect(layout(moveTask(initial, "b", 2, "x"))).toEqual([["a:0", "c:1"], ["b:0", "x:1"], []]);
  });

  it("空の列へ動かす", () => {
    expect(layout(moveTask(initial, "x", 3, null))).toEqual([["a:0", "b:1", "c:2"], [], ["x:0"]]);
  });

  it("自分自身の直前なら変えない", () => {
    expect(moveTask(initial, "b", 1, "b")).toBe(initial);
  });

  it("元の配列は変えない", () => {
    moveTask(initial, "a", 2, null);
    expect(layout(initial)).toEqual([["a:0", "b:1", "c:2"], ["x:0"], []]);
  });
});

describe("removeTask", () => {
  it("消して同じ列を詰める", () => {
    expect(layout(removeTask(initial, "a"))).toEqual([["b:0", "c:1"], ["x:0"], []]);
  });
});

describe("addTask と replaceTask", () => {
  it("列の一番下に入れ、仮の id をサーバーの返事に差し替える", () => {
    const added = addTask(initial, { ...task("temp", 2, 0), pending: true });
    expect(layout(added)).toEqual([["a:0", "b:1", "c:2"], ["x:0", "temp:1"], []]);

    const replaced = replaceTask(added, "temp", task("real", 2, 99));
    expect(layout(replaced)).toEqual([["a:0", "b:1", "c:2"], ["x:0", "real:1"], []]);
    expect(replaced.find((t) => t.id === "real")?.pending).toBeUndefined();
  });
});
