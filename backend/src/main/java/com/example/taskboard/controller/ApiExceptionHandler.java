package com.example.taskboard.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.taskboard.service.NotFoundException;

/** エラーを docs/api.md の 2章の形 { "error": "理由" } で返す */
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Map<String, String> notFound(NotFoundException e) {
		return Map.of("error", e.getMessage());
	}

	/** URL の id や columnId の形が正しくないとき */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> badParameter(MethodArgumentTypeMismatchException e) {
		return Map.of("error", e.getName() + " の形が正しくありません");
	}

}
