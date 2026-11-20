package com.example.taskboard.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.example.taskboard.entity.BoardColumn;
import com.example.taskboard.entity.Task;
import com.example.taskboard.repository.BoardColumnRepository;
import com.example.taskboard.repository.TaskRepository;

/** 開発用 DB を使う。@Transactional でテスト後に書き込みは取り消される。 */
@SpringBootTest
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
		// seed.sql を入れた DB でも結果が決まるよう、既存のタスクを消してから入れる（テスト後に取り消される）
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

}
