package com.example.taskboard.dto;

import com.example.taskboard.entity.BoardColumn;

/** API で返す列の形 */
public record ColumnResponse(Integer id, String name, int position) {

	public static ColumnResponse from(BoardColumn column) {
		return new ColumnResponse(column.getId(), column.getName(), column.getPosition());
	}

}
