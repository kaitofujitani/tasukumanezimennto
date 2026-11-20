package com.example.taskboard.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import jakarta.servlet.http.HttpServletResponse;

/**
 * ログインの設定（docs/api.md の 5章）。
 * - /api/login・/api/logout 以外の /api/ はログインしていないと 401
 * - 画面のファイルなど /api/ 以外は誰でも読める
 * - CSRF 対策：トークンを Cookie XSRF-TOKEN（画面から読める）で渡し、POST・PATCH・DELETE ではヘッダー X-XSRF-TOKEN を確かめる。ログインだけは除く
 * - ログイン状態は HttpSession に保存し、Spring Session JDBC がデータベースに入れる
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	CookieCsrfTokenRepository csrfTokenRepository() {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieCustomizer(cookie -> cookie.sameSite("Lax"));
		return repository;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository,
			CookieCsrfTokenRepository csrfTokenRepository) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/login", "/api/logout").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().permitAll())
				.securityContext(context -> context.securityContextRepository(securityContextRepository))
				// spa()：画面（React）から使う前提の設定。毎回 XSRF-TOKEN の Cookie を用意し、ヘッダーの値をそのまま比べる
				.csrf(csrf -> csrf
						.spa()
						.csrfTokenRepository(csrfTokenRepository)
						.ignoringRequestMatchers("/api/login"))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, e) -> writeError(response, HttpStatus.UNAUTHORIZED,
								"ログインしてください"))
						.accessDeniedHandler((request, response, e) -> writeError(response, HttpStatus.FORBIDDEN,
								"CSRF 対策のトークンがないか、正しくありません")))
				.logout(logout -> logout
						.logoutUrl("/api/logout")
						.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
						.deleteCookies("SESSION"))
				.formLogin(form -> form.disable())
				.httpBasic(basic -> basic.disable())
				.requestCache(cache -> cache.disable());
		return http.build();
	}

	/**
	 * ユーザー名とパスワードの組は使わない（パスワードは PasswordChecker で比べる）。
	 * 空のものを置き、Spring Boot が仮のユーザーとパスワードを自動で作ってログに出すのを止める
	 */
	@Bean
	UserDetailsService noUsers() {
		return new InMemoryUserDetailsManager();
	}

	/** エラーを docs/api.md の 2章の形 { "error": "理由" } で返す（理由は固定の文字列だけを渡す） */
	private static void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("{\"error\":\"" + message + "\"}");
	}

}
