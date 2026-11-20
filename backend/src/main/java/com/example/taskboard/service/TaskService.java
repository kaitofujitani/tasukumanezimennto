package com.example.taskboard.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskboard.dto.TaskResponse;
import com.example.taskboard.repository.BoardColumnRepository;
import com.example.taskboard.repository.TaskRepository;

/** タスクの読み取り（登録・移動・並べ替えは今後ここに足す） */
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

	/** タスクを1件返す。なければ NotFoundException */
	@Transactional(readOnly = true)
	public TaskResponse findById(UUID id) {
		return tasks.findById(id)
				.map(TaskResponse::from)
				.orElseThrow(() -> new NotFoundException("タスクが見つかりません"));
	}

}
