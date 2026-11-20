package com.example.taskboard.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskboard.dto.CreateTaskRequest;
import com.example.taskboard.dto.TaskResponse;
import com.example.taskboard.entity.BoardColumn;
import com.example.taskboard.entity.Task;
import com.example.taskboard.repository.BoardColumnRepository;
import com.example.taskboard.repository.TaskRepository;

/** タスクの読み取りと登録（変更・削除・移動・並べ替えは今後ここに足す） */
@Service
public class TaskService {

	private final BoardColumnRepository columns;
	private final TaskRepository tasks;

	public TaskService(BoardColumnRepository columns, TaskRepository tasks) {
		this.columns = columns;
		this.tasks = tasks;
	}

	/** すべてのタスクを、列の順 → 列の中の上から、の順で返す */
	@Transactional(readOnly = true)
	public List<TaskResponse> findAll() {
		return tasks.findAllByOrderByColumnPositionAscPositionAsc().stream()
				.map(TaskResponse::from)
				.toList();
	}

	/** 指定した列のタスクを上から順に返す。列がなければ NotFoundException */
	@Transactional(readOnly = true)
	public List<TaskResponse> findByColumn(Integer columnId) {
		if (!columns.existsById(columnId)) {
			throw new NotFoundException("列が見つかりません");
		}
		return tasks.findByColumnIdOrderByPositionAsc(columnId).stream()
				.map(TaskResponse::from)
				.toList();
	}

	/** 指定した列の一番下にタスクを登録する。タイトルは前後の空白を取り除く。列がなければ NotFoundException */
	@Transactional
	public TaskResponse create(CreateTaskRequest request) {
		BoardColumn column = columns.findById(request.columnId())
				.orElseThrow(() -> new NotFoundException("列が見つかりません"));
		int position = tasks.findMaxPositionByColumnId(column.getId()) + 1;
		// saveAndFlush で INSERT し、created_at などを確定させてから返す
		Task saved = tasks.saveAndFlush(new Task(column, request.title().strip(), position));
		return TaskResponse.from(saved);
	}

	/** タスクを1件返す。なければ NotFoundException */
	@Transactional(readOnly = true)
	public TaskResponse findById(UUID id) {
		return tasks.findById(id)
				.map(TaskResponse::from)
				.orElseThrow(() -> new NotFoundException("タスクが見つかりません"));
	}

}
