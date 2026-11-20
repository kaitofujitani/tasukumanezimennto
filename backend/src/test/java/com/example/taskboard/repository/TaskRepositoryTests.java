package com.example.taskboard.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskboard.entity.BoardColumn;
import com.example.taskboard.entity.Task;

/** 開発用 DB（compose.yaml）を使う。@Transactional でテスト後に書き込みは取り消される。 */
@SpringBootTest
@Transactional
class TaskRepositoryTests {

	@Autowired
	BoardColumnRepository columns;

	@Autowired
	TaskRepository tasks;

	@Test
	void 初期データの3列が表示順で読める() {
		assertThat(columns.findAllByOrderByPositionAsc())
				.extracting(BoardColumn::getName)
				.containsExactly("未着手", "進行中", "完了");
	}

	@Test
	void タスクを保存して列ごとに読める() {
		// seed.sql を入れた DB でも結果が決まるよう、既存のタスクを消してから入れる（テスト後に取り消される）
		tasks.deleteAll();
		BoardColumn todo = columns.findAllByOrderByPositionAsc().get(0);
		tasks.save(new Task(todo, "2つ目", 1));
		tasks.save(new Task(todo, "1つ目", 0));
		tasks.flush();

		List<Task> found = tasks.findByColumnIdOrderByPositionAsc(todo.getId());

		assertThat(found).extracting(Task::getTitle).containsExactly("1つ目", "2つ目");
		Task first = found.get(0);
		assertThat(first.getId()).isNotNull();
		assertThat(first.getPriority()).isEqualTo("mid");
		assertThat(first.getDescription()).isEmpty();
		assertThat(first.getCreatedAt()).isNotNull();
	}

}
