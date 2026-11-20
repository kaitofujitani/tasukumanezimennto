package com.example.taskboard.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskboard.dto.BoardResponse;
import com.example.taskboard.dto.ColumnResponse;
import com.example.taskboard.dto.TaskResponse;
import com.example.taskboard.repository.BoardColumnRepository;
import com.example.taskboard.repository.TaskRepository;

/** ボード全体（列とタスク）の読み取り */
@Service
public class BoardService {

	private final BoardColumnRepository columns;
	private final TaskRepository tasks;

	public BoardService(BoardColumnRepository columns, TaskRepository tasks) {
		this.columns = columns;
		this.tasks = tasks;
	}

	/** 列は左から、タスクは列の順→列の中の上から、の順で返す */
	@Transactional(readOnly = true)
	public BoardResponse getBoard() {
		List<ColumnResponse> columnList = columns.findAllByOrderByPositionAsc().stream()
				.map(ColumnResponse::from)
				.toList();
		List<TaskResponse> taskList = tasks.findAllByOrderByColumnPositionAscPositionAsc().stream()
				.map(TaskResponse::from)
				.toList();
		return new BoardResponse(columnList, taskList);
	}

}
