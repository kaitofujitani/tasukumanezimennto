package com.example.taskboard.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** A8 POST /api/tasks/sort で受け取る内容。by は priority（優先度順）・due（期限順）・created（登録順） */
public record SortTasksRequest(
		@NotNull(message = "並べ方を指定してください")
		@Pattern(regexp = "priority|due|created", message = "並べ方は priority・due・created のどれかにしてください")
		String by) {
}
