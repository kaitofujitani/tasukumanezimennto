package com.example.taskboard.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskboard.dto.CreateTaskRequest;
import com.example.taskboard.dto.TaskResponse;
import com.example.taskboard.service.TaskService;

import jakarta.validation.Valid;

/** A4・A9・A10 タスクの登録と読み取り（docs/api.md）。ログイン（A1）は未実装のため、今は誰でも呼べる。 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	/** A9 一覧。columnId を付けるとその列だけ */
	@GetMapping
	public List<TaskResponse> list(@RequestParam(required = false) Integer columnId) {
		return columnId == null ? taskService.findAll() : taskService.findByColumn(columnId);
	}

	/** A4 登録。指定した列の一番下に入れ、201 と登録したタスクを返す */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TaskResponse create(@Valid @RequestBody CreateTaskRequest request) {
		return taskService.create(request);
	}

	/** A10 1件 */
	@GetMapping("/{id}")
	public TaskResponse get(@PathVariable UUID id) {
		return taskService.findById(id);
	}

}
