package com.example.taskboard.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskboard.config.PasswordChecker;
import com.example.taskboard.dto.LoginRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** A1 ログイン（docs/api.md）。A2 ログアウトは SecurityConfig の logout 設定で行う */
@RestController
@RequestMapping("/api")
public class AuthController {

	/** パスワードを何度も試されるのを防ぐため、失敗したら待つ時間 */
	static final long FAILURE_DELAY_MILLIS = 1000;

	/** ログインした人を表す名前（ユーザーは1人だけ） */
	private static final String USER_NAME = "owner";

	private final PasswordChecker passwordChecker;
	private final SecurityContextRepository securityContextRepository;
	private final CookieCsrfTokenRepository csrfTokenRepository;

	public AuthController(PasswordChecker passwordChecker, SecurityContextRepository securityContextRepository,
			CookieCsrfTokenRepository csrfTokenRepository) {
		this.passwordChecker = passwordChecker;
		this.securityContextRepository = securityContextRepository;
		this.csrfTokenRepository = csrfTokenRepository;
	}

	/** 正しければ 204 と Cookie（SESSION・XSRF-TOKEN）。違えば 1 秒待って 401 */
	@PostMapping("/login")
	public ResponseEntity<Map<String, String>> login(@RequestBody(required = false) LoginRequest body,
			HttpServletRequest request, HttpServletResponse response) throws InterruptedException {
		if (!passwordChecker.matches(body == null ? null : body.password())) {
			Thread.sleep(FAILURE_DELAY_MILLIS);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "パスワードが違います"));
		}

		// ログイン前のセッション ID を使い回させない（セッション固定攻撃の対策）
		if (request.getSession(false) != null) {
			request.changeSessionId();
		}
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
				USER_NAME, null, AuthorityUtils.createAuthorityList("ROLE_USER")));
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);

		// CSRF 対策のトークンもログインのたびに作り直す
		csrfTokenRepository.saveToken(csrfTokenRepository.generateToken(request), request, response);
		return ResponseEntity.noContent().build();
	}

}
