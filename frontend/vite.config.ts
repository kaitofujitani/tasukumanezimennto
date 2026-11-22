/// <reference types="vitest/config" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

// 開発中（npm run dev）は /api を Spring Boot（8080）へ転送する。画面と API の URL が本番と同じになる
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // ポートは 5173 に固定。使われていたら別のポートに逃げず、エラーで止める（CLAUDE.md の「開発用サーバーのポート」）
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test/setup.ts"],
  },
});
