# 公開用のイメージ（docs/api.md の 1.4、docs/deploy.md）。Render がこのファイルでビルドして起動する
# ① 画面をビルド ② できたファイルを Spring Boot の static に入れる ③ サーバーをビルド ④ 1つの jar で起動

# ① 画面（React）をビルドする。Node.js はここでだけ使い、公開するイメージには入れない
FROM node:24-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ②③ サーバー（Spring Boot）をビルドする。画面のファイルは static に入れ、jar に一緒に入れて配信する
FROM eclipse-temurin:25-jdk AS backend
WORKDIR /app/backend
# ライブラリのダウンロードを先に済ませ、ソースだけ変えたときはこの段の結果を使い回す
COPY backend/gradlew backend/settings.gradle backend/build.gradle ./
COPY backend/gradle ./gradle
RUN ./gradlew --no-daemon dependencies > /dev/null
COPY backend/src ./src
COPY --from=frontend /app/frontend/dist ./src/main/resources/static
# テストは Testcontainers で Docker を使うので、イメージのビルドの中では実行しない（開発中に ./gradlew test で確かめる）
RUN ./gradlew --no-daemon bootJar

# ④ 起動する。Java の実行環境（JRE）だけの小さいイメージに jar を入れ、管理者ではないユーザーで動かす
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=backend /app/backend/build/libs/taskboard-0.0.1-SNAPSHOT.jar app.jar
USER app
# 待ち受けるポートは環境変数 PORT（Render が設定する。未設定なら 8080）
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
