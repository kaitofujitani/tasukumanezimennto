package com.example.taskboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskboard.dto.BoardResponse;
import com.example.taskboard.service.BoardService;

/** A3 ボードを取得する（docs/api.md）。ログイン（A1）は未実装のため、今は誰でも呼べる。 */
@RestController
@RequestMapping("/api/board")
public class BoardController {

	private final BoardService boardService;

	public BoardController(BoardService boardService) {
		this.boardService = boardService;
	}

	@GetMapping
	public BoardResponse getBoard() {
		return boardService.getBoard();
	}

}
