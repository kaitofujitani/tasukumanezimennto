package com.example.taskboard.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ログインのパスワード（環境変数 APP_PASSWORD）と、送られたパスワードを比べる。
 * かかる時間で中身を推測されないよう、両方を SHA-256 にしてから MessageDigest.isEqual で比べる
 * （そのまま比べると長さの違いで早く終わるため、長さもそろえる）。
 */
@Component
public class PasswordChecker {

	private final byte[] expectedHash;

	public PasswordChecker(@Value("${app.password}") String password) {
		if (password == null || password.isBlank()) {
			throw new IllegalStateException("環境変数 APP_PASSWORD にログインのパスワードを設定してください");
		}
		this.expectedHash = sha256(password);
	}

	public boolean matches(String input) {
		if (input == null) {
			return false;
		}
		return MessageDigest.isEqual(expectedHash, sha256(input));
	}

	private static byte[] sha256(String value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}
