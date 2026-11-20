package com.example.taskboard.service;

/** 指定したタスクや列がないとき。API では 404 を返す */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}

}
