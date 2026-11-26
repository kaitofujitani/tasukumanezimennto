package com.example.taskboard.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.example.taskboard.TestcontainersConfiguration;
import com.example.taskboard.entity.BoardColumn;
import com.example.taskboard.entity.Task;
import com.example.taskboard.repository.BoardColumnRepository;
import com.example.taskboard.repository.TaskRepository;

/** テスト用の PostgreSQL（TestcontainersConfiguration）を使う。@Transactional でテスト後に書き込みは取り消される。 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class TaskControllerTests {

	@Autowired
	WebApplicationContext context;

	@Autowired
	BoardColumnRepository columns;

	@Autowired
	TaskRepository tasks;

	MockMvc mvc;

	BoardColumn todo;
	BoardColumn doing;
	Task first;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).build();
		// ほかのテストの書き込みに左右されないよう、既存のタスクを消してから入れる（テスト後に取り消される）
		tasks.deleteAll();
		List<BoardColumn> cols = columns.findAllByOrderByPositionAsc();
		todo = cols.get(0);
		doing = cols.get(1);
		tasks.save(new Task(doing, "進行中の1つ目", 0));
		tasks.save(new Task(todo, "未着手の2つ目", 1));
		first = tasks.save(new Task(todo, "未着手の1つ目", 0));
		tasks.flush();
	}

	@Test
	void 一覧は列の順と列の中の順で返す() throws Exception {
		mvc.perform(get("/api/tasks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].title").value(contains("未着手の1つ目", "未着手の2つ目", "進行中の1つ目")));
	}

	@Test
	void 列で絞り込める() throws Exception {
		mvc.perform(get("/api/tasks").param("columnId", doing.getId().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].title").value(contains("進行中の1つ目")));
	}

	@Test
	void ない列は404() throws Exception {
		mvc.perform(get("/api/tasks").param("columnId", "9999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("列が見つかりません"));
	}

	@Test
	void 数字でない列は400() throws Exception {
		mvc.perform(get("/api/tasks").param("columnId", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").isString());
	}

	@Test
	void 一件取得できる() throws Exception {
		mvc.perform(get("/api/tasks/{id}", first.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("未着手の1つ目"))
				.andExpect(jsonPath("$.columnId").value(todo.getId()));
	}

	@Test
	void ないタスクは404() throws Exception {
		mvc.perform(get("/api/tasks/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("タスクが見つかりません"));
	}

	@Test
	void UUIDでないidは400() throws Exception {
		mvc.perform(get("/api/tasks/not-a-uuid"))
				.andExpect(status().isBadRequest());
	}

	private ResultActions postTask(String json) throws Exception {
		return mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	@Test
	void 登録すると列の一番下に入り201と登録したタスクを返す() throws Exception {
		postTask("{\"columnId\": " + todo.getId() + ", \"title\": \"  新しいタスク  \"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isString())
				.andExpect(jsonPath("$.columnId").value(todo.getId()))
				.andExpect(jsonPath("$.title").value("新しいタスク"))
				.andExpect(jsonPath("$.description").value(""))
				.andExpect(jsonPath("$.priority").value("mid"))
				.andExpect(jsonPath("$.due").value(nullValue()))
				.andExpect(jsonPath("$.position").value(2))
				.andExpect(jsonPath("$.createdAt").isString());

		mvc.perform(get("/api/tasks").param("columnId", todo.getId().toString()))
				.andExpect(jsonPath("$[*].title").value(contains("未着手の1つ目", "未着手の2つ目", "新しいタスク")));
	}

	@Test
	void 空の列に登録するとpositionは0() throws Exception {
		BoardColumn done = columns.findAllByOrderByPositionAsc().get(2);
		postTask("{\"columnId\": " + done.getId() + ", \"title\": \"完了したこと\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.position").value(0));
	}

	@Test
	void 空白だけのタイトルは400() throws Exception {
		postTask("{\"columnId\": " + todo.getId() + ", \"title\": \"   \"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("タイトルを入力してください"));
	}

	@Test
	void 列の指定がなければ400() throws Exception {
		postTask("{\"title\": \"列なし\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("列を指定してください"));
	}

	@Test
	void ない列への登録は404() throws Exception {
		postTask("{\"columnId\": 9999, \"title\": \"どこにも入らない\"}")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("列が見つかりません"));
	}

	private ResultActions patchTask(UUID id, String json) throws Exception {
		return mvc.perform(patch("/api/tasks/{id}", id).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	@Test
	void 送った項目だけ変わる() throws Exception {
		patchTask(first.getId(), "{\"priority\": \"high\", \"due\": \"2026-12-01\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(first.getId().toString()))
				.andExpect(jsonPath("$.title").value("未着手の1つ目"))
				.andExpect(jsonPath("$.description").value(""))
				.andExpect(jsonPath("$.priority").value("high"))
				.andExpect(jsonPath("$.due").value("2026-12-01"))
				.andExpect(jsonPath("$.columnId").value(todo.getId()))
				.andExpect(jsonPath("$.position").value(0));

		patchTask(first.getId(), "{\"title\": \"  新しい名前  \", \"description\": \" 説明\\n2行目 \"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("新しい名前"))
				.andExpect(jsonPath("$.description").value(" 説明\n2行目 "))
				.andExpect(jsonPath("$.priority").value("high"))
				.andExpect(jsonPath("$.due").value("2026-12-01"));

		mvc.perform(get("/api/tasks/{id}", first.getId()))
				.andExpect(jsonPath("$.title").value("新しい名前"))
				.andExpect(jsonPath("$.priority").value("high"));
	}

	@Test
	void dueにnullを送ると期限なしになる() throws Exception {
		patchTask(first.getId(), "{\"due\": \"2026-12-01\"}").andExpect(status().isOk());
		patchTask(first.getId(), "{\"due\": null}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.due").value(nullValue()));
	}

	@Test
	void 空白だけのタイトルへの変更は400で変わらない() throws Exception {
		patchTask(first.getId(), "{\"title\": \"  \"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("タイトルを入力してください"));
		mvc.perform(get("/api/tasks/{id}", first.getId()))
				.andExpect(jsonPath("$.title").value("未着手の1つ目"));
	}

	@Test
	void 決まっていない優先度は400() throws Exception {
		patchTask(first.getId(), "{\"priority\": \"urgent\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("優先度は high・mid・low のどれかにしてください"));
	}

	@Test
	void 日付の形でない期限は400() throws Exception {
		patchTask(first.getId(), "{\"due\": \"12月1日\"}")
				.andExpect(status().isBadRequest());
	}

	@Test
	void ないタスクの変更は404() throws Exception {
		patchTask(UUID.randomUUID(), "{\"title\": \"だれもいない\"}")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("タスクが見つかりません"));
	}

	@Test
	void 削除すると204で残りのpositionが詰められる() throws Exception {
		Task third = tasks.save(new Task(todo, "未着手の3つ目", 2));
		tasks.flush();

		mvc.perform(delete("/api/tasks/{id}", first.getId()))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/tasks/{id}", first.getId()))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/tasks").param("columnId", todo.getId().toString()))
				.andExpect(jsonPath("$[*].title").value(contains("未着手の2つ目", "未着手の3つ目")))
				.andExpect(jsonPath("$[*].position").value(contains(0, 1)));
		// ほかの列は変わらない
		mvc.perform(get("/api/tasks").param("columnId", doing.getId().toString()))
				.andExpect(jsonPath("$[*].title").value(contains("進行中の1つ目")))
				.andExpect(jsonPath("$[*].position").value(contains(0)));
		mvc.perform(get("/api/tasks/{id}", third.getId()))
				.andExpect(jsonPath("$.position").value(1));
	}

	@Test
	void ないタスクの削除は404() throws Exception {
		mvc.perform(delete("/api/tasks/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("タスクが見つかりません"));
	}

	private Task byTitle(String title) {
		return tasks.findAll().stream().filter(t -> t.getTitle().equals(title)).findFirst().orElseThrow();
	}

	private ResultActions moveTask(UUID id, String json) throws Exception {
		return mvc.perform(post("/api/tasks/{id}/move", id).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private static String moveJson(BoardColumn column, Task before) {
		return "{\"columnId\": " + column.getId() + ", \"beforeId\": "
				+ (before == null ? "null" : "\"" + before.getId() + "\"") + "}";
	}

	/** 列のタスクが、この順・position 0, 1, 2… で並んでいることを確かめる */
	private void expectColumn(BoardColumn column, String... titles) throws Exception {
		Integer[] positions = new Integer[titles.length];
		for (int i = 0; i < titles.length; i++) {
			positions[i] = i;
		}
		ResultActions result = mvc.perform(get("/api/tasks").param("columnId", column.getId().toString()));
		if (titles.length == 0) {
			result.andExpect(jsonPath("$").isEmpty());
		} else {
			result.andExpect(jsonPath("$[*].title").value(contains(titles)))
					.andExpect(jsonPath("$[*].position").value(contains(positions)));
		}
	}

	@Test
	void 同じ列の一番下へ動かせる() throws Exception {
		moveTask(first.getId(), moveJson(todo, null)).andExpect(status().isNoContent());
		expectColumn(todo, "未着手の2つ目", "未着手の1つ目");
	}

	@Test
	void 同じ列で1つ上へ動かせる() throws Exception {
		moveTask(byTitle("未着手の2つ目").getId(), moveJson(todo, first)).andExpect(status().isNoContent());
		expectColumn(todo, "未着手の2つ目", "未着手の1つ目");
	}

	@Test
	void 別の列のタスクの直前へ動かせる() throws Exception {
		moveTask(first.getId(), moveJson(doing, byTitle("進行中の1つ目"))).andExpect(status().isNoContent());
		expectColumn(todo, "未着手の2つ目");
		expectColumn(doing, "未着手の1つ目", "進行中の1つ目");
		mvc.perform(get("/api/tasks/{id}", first.getId()))
				.andExpect(jsonPath("$.columnId").value(doing.getId()));
	}

	@Test
	void 空の列へ動かせる() throws Exception {
		BoardColumn done = columns.findAllByOrderByPositionAsc().get(2);
		moveTask(byTitle("進行中の1つ目").getId(), moveJson(done, null)).andExpect(status().isNoContent());
		expectColumn(doing);
		expectColumn(done, "進行中の1つ目");
	}

	@Test
	void 自分自身の直前を指定しても変わらない() throws Exception {
		moveTask(first.getId(), moveJson(todo, first)).andExpect(status().isNoContent());
		expectColumn(todo, "未着手の1つ目", "未着手の2つ目");
	}

	@Test
	void 移動のエラー() throws Exception {
		moveTask(UUID.randomUUID(), moveJson(todo, null))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("タスクが見つかりません"));
		moveTask(first.getId(), "{\"columnId\": 9999, \"beforeId\": null}")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("列が見つかりません"));
		moveTask(first.getId(), "{\"columnId\": " + todo.getId() + ", \"beforeId\": \"" + UUID.randomUUID() + "\"}")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("beforeId のタスクが見つかりません"));
		moveTask(first.getId(), moveJson(todo, byTitle("進行中の1つ目")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("beforeId のタスクが動かし先の列にありません"));
		moveTask(first.getId(), "{\"beforeId\": null}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("列を指定してください"));
		// エラーのときは何も変わらない
		expectColumn(todo, "未着手の1つ目", "未着手の2つ目");
		expectColumn(doing, "進行中の1つ目");
	}

	/** 「完了」列に並べ替え用のタスクを A, B, C, D の順に登録する（登録日時がずれるよう 1 件ずつ保存） */
	private BoardColumn prepareSortData() throws Exception {
		BoardColumn done = columns.findAllByOrderByPositionAsc().get(2);
		Object[][] rows = {
				{ "A", "low", null },
				{ "B", "high", LocalDate.of(2026, 12, 10) },
				{ "C", "mid", LocalDate.of(2026, 12, 1) },
				{ "D", "high", null },
		};
		for (int i = 0; i < rows.length; i++) {
			Task t = new Task(done, (String) rows[i][0], i);
			t.setPriority((String) rows[i][1]);
			t.setDue((LocalDate) rows[i][2]);
			tasks.saveAndFlush(t);
			Thread.sleep(5);
		}
		return done;
	}

	private ResultActions sortTasks(String json) throws Exception {
		return mvc.perform(post("/api/tasks/sort").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	@Test
	void 優先度順に並べ替えてボード全体を返す() throws Exception {
		BoardColumn done = prepareSortData();
		sortTasks("{\"by\": \"priority\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[*].name").value(contains("未着手", "進行中", "完了")))
				.andExpect(jsonPath("$.tasks[*].title").value(contains(
						"未着手の1つ目", "未着手の2つ目", "進行中の1つ目", "B", "D", "C", "A")));
		// 高（期限が近い順・期限なしは後ろ）→ 中 → 低
		expectColumn(done, "B", "D", "C", "A");
		// 条件が同じタスクは今の並びのまま
		expectColumn(todo, "未着手の1つ目", "未着手の2つ目");
	}

	@Test
	void 期限順に並べ替える() throws Exception {
		BoardColumn done = prepareSortData();
		sortTasks("{\"by\": \"due\"}").andExpect(status().isOk());
		// 期限が近い順 → 期限なしは最後（その中は優先度が高い順）
		expectColumn(done, "C", "B", "D", "A");
	}

	@Test
	void 登録順に並べ替える() throws Exception {
		BoardColumn done = prepareSortData();
		sortTasks("{\"by\": \"priority\"}").andExpect(status().isOk());
		sortTasks("{\"by\": \"created\"}").andExpect(status().isOk());
		expectColumn(done, "A", "B", "C", "D");
	}

	@Test
	void 並べ方の指定が正しくなければ400() throws Exception {
		sortTasks("{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("並べ方を指定してください"));
		sortTasks("{\"by\": \"title\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("並べ方は priority・due・created のどれかにしてください"));
	}

	@Test
	void JSONとして読めなければ400() throws Exception {
		postTask("{壊れたJSON")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("送られた内容が正しくありません"));
	}

}
