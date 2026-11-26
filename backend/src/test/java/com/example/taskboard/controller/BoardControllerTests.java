package com.example.taskboard.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
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
class BoardControllerTests {

	@Autowired
	WebApplicationContext context;

	@Autowired
	BoardColumnRepository columns;

	@Autowired
	TaskRepository tasks;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).build();
		// ほかのテストの書き込みに左右されないよう、既存のタスクを消してから入れる（テスト後に取り消される）
		tasks.deleteAll();
	}

	@Test
	void 列とタスクを表示順で返す() throws Exception {
		List<BoardColumn> cols = columns.findAllByOrderByPositionAsc();
		BoardColumn todo = cols.get(0);
		BoardColumn doing = cols.get(1);
		// わざと表示順とは違う順に保存する
		tasks.save(new Task(doing, "進行中の1つ目", 0));
		tasks.save(new Task(todo, "未着手の2つ目", 1));
		Task withDue = new Task(todo, "未着手の1つ目", 0);
		withDue.setPriority("high");
		withDue.setDue(LocalDate.of(2026, 11, 20));
		tasks.save(withDue);
		tasks.flush();

		mvc.perform(get("/api/board"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns[*].name").value(contains("未着手", "進行中", "完了")))
				.andExpect(jsonPath("$.tasks[*].title").value(contains("未着手の1つ目", "未着手の2つ目", "進行中の1つ目")))
				.andExpect(jsonPath("$.tasks[0].columnId").value(todo.getId()))
				.andExpect(jsonPath("$.tasks[0].priority").value("high"))
				.andExpect(jsonPath("$.tasks[0].due").value("2026-11-20"))
				.andExpect(jsonPath("$.tasks[0].description").value(""))
				.andExpect(jsonPath("$.tasks[0].id").isString())
				.andExpect(jsonPath("$.tasks[0].createdAt").isString())
				.andExpect(jsonPath("$.tasks[1].due").value(nullValue()))
				.andExpect(jsonPath("$.tasks[2].columnId").value(doing.getId()));
	}

}
