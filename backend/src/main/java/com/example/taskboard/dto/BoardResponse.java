package com.example.taskboard.dto;

import java.util.List;

/** A3 GET /api/board で返す内容（docs/api.md の 4章）。列もタスクも position の順に並べる。 */
public record BoardResponse(List<ColumnResponse> columns, List<TaskResponse> tasks) {
}
