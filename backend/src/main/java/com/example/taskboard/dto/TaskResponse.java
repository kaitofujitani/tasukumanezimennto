package com.example.taskboard.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.example.taskboard.entity.Task;

/** API で返すタスクの形。期限なしは due が null。createdAt は UTC（末尾 Z）で返す。 */
public record TaskResponse(
		UUID id,
		Integer columnId,
		String title,
		String description,
		String priority,
		LocalDate due,
		int position,
		Instant createdAt) {

	public static TaskResponse from(Task task) {
		return new TaskResponse(
				task.getId(),
				task.getColumn().getId(),
				task.getTitle(),
				task.getDescription(),
				task.getPriority(),
				task.getDue(),
				task.getPosition(),
				task.getCreatedAt().toInstant());
	}

}
