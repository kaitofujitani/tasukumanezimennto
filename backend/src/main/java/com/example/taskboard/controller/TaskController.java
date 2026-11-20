package com.example.taskboard.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskboard.dto.TaskResponse;
import com.example.taskboard.service.TaskService;

/** A9・A10 タスクの読み取り（docs/api.md）。ログイン（A1）は未実装のため、今は誰でも呼べる。 */
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

	/** A10 1件 */
	@GetMapping("/{id}")
	public TaskResponse get(@PathVariable UUID id) {
		return taskService.findById(id);
	}

}
