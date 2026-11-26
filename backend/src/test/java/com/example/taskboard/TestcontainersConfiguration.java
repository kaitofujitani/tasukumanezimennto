package com.example.taskboard;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * テスト用の PostgreSQL。テストのたびに Docker で使い捨てのコンテナを起動し、@ServiceConnection で接続先を差し替える。
 * 開発用 DB（compose.yaml）は使わないので、起動していなくてもよい（Docker は必要）。
 * テーブルはアプリと同じく Flyway が作る。テストクラスには @Import(TestcontainersConfiguration.class) を付ける。
 * 同じ設定のテストは Spring のコンテキストを使い回すので、コンテナも1つで済む
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	/** 本番（Neon）・開発用（compose.yaml）と同じ版にそろえる */
	@Bean
	@ServiceConnection
	PostgreSQLContainer postgres() {
		return new PostgreSQLContainer("postgres:18");
	}

}
