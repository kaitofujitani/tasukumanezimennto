package com.example.taskboard.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

	/** 送られた内容が入力のルール（@NotBlank など）に違反したとき。最初の違反の理由を返す */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> invalid(MethodArgumentNotValidException e) {
		FieldError error = e.getBindingResult().getFieldError();
		return Map.of("error", error != null ? error.getDefaultMessage() : "送られた内容が正しくありません");
	}

	/** 送られた内容が JSON として読めない・型が合わないとき */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> unreadable(HttpMessageNotReadableException e) {
		return Map.of("error", "送られた内容が正しくありません");
	}

	/** URL の id や columnId の形が正しくないとき */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> badParameter(MethodArgumentTypeMismatchException e) {
		return Map.of("error", e.getName() + " の形が正しくありません");
	}

}
