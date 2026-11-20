package com.example.taskboard.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskboard.dto.CreateTaskRequest;
import com.example.taskboard.dto.TaskResponse;
import com.example.taskboard.dto.UpdateTaskRequest;
import com.example.taskboard.entity.BoardColumn;
import com.example.taskboard.entity.Task;
import com.example.taskboard.repository.BoardColumnRepository;
import com.example.taskboard.repository.TaskRepository;

/** タスクの読み取り・登録・内容の変更・削除（移動・並べ替えは今後ここに足す） */
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

	/** 送られた項目だけを変える。タスクがなければ NotFoundException。updated_at は変更があれば自動で更新される */
	@Transactional
	public TaskResponse update(UUID id, UpdateTaskRequest request) {
		Task task = tasks.findById(id)
				.orElseThrow(() -> new NotFoundException("タスクが見つかりません"));
		if (request.getTitle() != null) {
			task.setTitle(request.getTitle().strip());
		}
		if (request.getDescription() != null) {
			task.setDescription(request.getDescription());
		}
		if (request.getPriority() != null) {
			task.setPriority(request.getPriority());
		}
		if (request.isDueSpecified()) {
			task.setDue(request.getDue());
		}
		tasks.flush();
		return TaskResponse.from(task);
	}

	/** タスクを削除し、同じ列の残りのタスクの position を 0 から振り直す。タスクがなければ NotFoundException */
	@Transactional
	public void delete(UUID id) {
		Task task = tasks.findById(id)
				.orElseThrow(() -> new NotFoundException("タスクが見つかりません"));
		Integer columnId = task.getColumn().getId();
		tasks.delete(task);
		tasks.flush();
		List<Task> remaining = tasks.findByColumnIdOrderByPositionAsc(columnId);
		for (int i = 0; i < remaining.size(); i++) {
			remaining.get(i).setPosition(i);
		}
	}

	/** タスクを1件返す。なければ NotFoundException */
	@Transactional(readOnly = true)
	public TaskResponse findById(UUID id) {
		return tasks.findById(id)
				.map(TaskResponse::from)
				.orElseThrow(() -> new NotFoundException("タスクが見つかりません"));
	}

}
