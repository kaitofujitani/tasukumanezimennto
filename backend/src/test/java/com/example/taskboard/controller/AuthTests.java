package com.example.taskboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.example.taskboard.TestcontainersConfiguration;

import jakarta.servlet.http.Cookie;

/**
 * ログイン・ログアウトと、ログインしていないときの API の動き（docs/api.md の 5章）。
 * ほかのテストと違い、Spring Security の仕組みを通して確かめる。パスワードは src/test/resources/config の test-password
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthTests {

	@Autowired
	WebApplicationContext context;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	private MvcResult login(String password) throws Exception {
		return mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\": \"" + password + "\"}"))
				.andReturn();
	}

	@Test
	void ログインしていないとAPIは401() throws Exception {
		mvc.perform(get("/api/board"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("ログインしてください"));
		mvc.perform(get("/api/tasks"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void パスワードが違うと1秒待って401() throws Exception {
		long start = System.currentTimeMillis();
		MvcResult result = login("wrong");
		long elapsed = System.currentTimeMillis() - start;

		assertThat(result.getResponse().getStatus()).isEqualTo(401);
		assertThat(result.getResponse().getContentAsString()).contains("パスワードが違います");
		assertThat(elapsed).isGreaterThanOrEqualTo(AuthController.FAILURE_DELAY_MILLIS);
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		// 失敗してもログイン済みにはならない
		mvc.perform(get("/api/board").session(session != null ? session : new MockHttpSession()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void パスワードがなければ401() throws Exception {
		mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void ログインするとAPIが使えてXSRF_TOKENのCookieが返る() throws Exception {
		MvcResult result = login("test-password");
		assertThat(result.getResponse().getStatus()).isEqualTo(204);
		Cookie xsrf = result.getResponse().getCookie("XSRF-TOKEN");
		assertThat(xsrf).isNotNull();
		assertThat(xsrf.isHttpOnly()).isFalse();
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

		mvc.perform(get("/api/board").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columns").isArray());
	}

	@Test
	void ログインしていてもCSRFトークンのない書き込みは403() throws Exception {
		MockHttpSession session = (MockHttpSession) login("test-password").getRequest().getSession(false);

		mvc.perform(post("/api/tasks").session(session).contentType(MediaType.APPLICATION_JSON)
				.content("{\"columnId\": 1, \"title\": \"トークンなし\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("CSRF 対策のトークンがないか、正しくありません"));
	}

	@Test
	void Cookieのトークンをヘッダーに付ければ書き込める() throws Exception {
		MvcResult loggedIn = login("test-password");
		MockHttpSession session = (MockHttpSession) loggedIn.getRequest().getSession(false);
		Cookie xsrf = loggedIn.getResponse().getCookie("XSRF-TOKEN");

		// 画面と同じように、Cookie の値をヘッダー X-XSRF-TOKEN に付けて送る
		mvc.perform(post("/api/tasks").session(session).cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"columnId\": 1, \"title\": \"トークンあり\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	void ログアウトすると204で再び401になる() throws Exception {
		MockHttpSession session = (MockHttpSession) login("test-password").getRequest().getSession(false);

		mvc.perform(post("/api/logout").session(session).with(csrf()))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge("SESSION", 0));
		assertThat(session.isInvalid()).isTrue();

		mvc.perform(get("/api/board").session(new MockHttpSession()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void API以外のパスはログインしなくても401にならない() throws Exception {
		int status = mvc.perform(get("/index.html")).andReturn().getResponse().getStatus();
		assertThat(status).isNotEqualTo(401);
	}

}
