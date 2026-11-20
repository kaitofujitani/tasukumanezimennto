package com.example.taskboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** A4 POST /api/tasks で受け取る内容。タイトルは空白だけでも空とみなす（docs/screens.md 3章） */
public record CreateTaskRequest(
		@NotNull(message = "列を指定してください") Integer columnId,
		@NotBlank(message = "タイトルを入力してください") String title) {
}
