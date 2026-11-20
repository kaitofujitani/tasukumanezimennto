package com.example.taskboard.service;

/** 送られた内容が正しくないとき（入力の形は正しいが、データと合わない）。API では 400 を返す */
public class BadRequestException extends RuntimeException {

	public BadRequestException(String message) {
		super(message);
	}

}
