package com.example.taskboard.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskboard.dto.CreateTaskRequest;
import com.example.taskboard.dto.MoveTaskRequest;
import com.example.taskboard.dto.TaskResponse;
import com.example.taskboard.dto.UpdateTaskRequest;
import com.example.taskboard.entity.BoardColumn;
import com.example.taskboard.entity.Task;
import com.example.taskboard.repository.BoardColumnRepository;
import com.example.taskboard.repository.TaskRepository;

/** タスクの読み取り・登録・内容の変更・削除・移動（並べ替えは今後ここに足す） */
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

	/**
	 * タスクを columnId の列の、beforeId のタスクの直前へ動かす（beforeId が null なら列の一番下）。
	 * 動かし元と動かし先の列の position を 0 から振り直す。
	 * タスク・列・beforeId のタスクがなければ NotFoundException、beforeId のタスクが動かし先の列になければ BadRequestException
	 */
	@Transactional
	public void move(UUID id, MoveTaskRequest request) {
		Task task = tasks.findById(id)
				.orElseThrow(() -> new NotFoundException("タスクが見つかりません"));
		BoardColumn destColumn = columns.findById(request.columnId())
				.orElseThrow(() -> new NotFoundException("列が見つかりません"));
		UUID beforeId = request.beforeId();
		if (id.equals(beforeId)) {
			// 自分の直前＝今の位置。何も変えない
			return;
		}
		Integer sourceColumnId = task.getColumn().getId();

		// 書き換える前に両方の列の並びを読み、動かすタスクを抜いておく
		List<Task> dest = new ArrayList<>(tasks.findByColumnIdOrderByPositionAsc(destColumn.getId()));
		dest.removeIf(t -> t.getId().equals(id));
		// 同じ列の中で動かすときは dest の振り直しだけでよいので、source は空にしておく
		List<Task> source = sourceColumnId.equals(destColumn.getId())
				? new ArrayList<>()
				: new ArrayList<>(tasks.findByColumnIdOrderByPositionAsc(sourceColumnId));

		int index = dest.size();
		if (beforeId != null) {
			index = indexOf(dest, beforeId);
			if (index < 0) {
				if (!tasks.existsById(beforeId)) {
					throw new NotFoundException("beforeId のタスクが見つかりません");
				}
				throw new BadRequestException("beforeId のタスクが動かし先の列にありません");
			}
		}

		task.setColumn(destColumn);
		dest.add(index, task);
		renumber(dest);
		source.removeIf(t -> t.getId().equals(id));
		renumber(source);
	}

	private static int indexOf(List<Task> list, UUID id) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getId().equals(id)) {
				return i;
			}
		}
		return -1;
	}

	/** 並んでいる順に position を 0, 1, 2… と振り直す */
	private static void renumber(List<Task> list) {
		for (int i = 0; i < list.size(); i++) {
			list.get(i).setPosition(i);
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
