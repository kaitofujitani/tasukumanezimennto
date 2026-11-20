package com.example.taskboard.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** A7 POST /api/tasks/:id/move で受け取る内容。beforeId のタスクの直前に入れる。null なら列の一番下 */
public record MoveTaskRequest(
		@NotNull(message = "列を指定してください") Integer columnId,
		UUID beforeId) {
}
