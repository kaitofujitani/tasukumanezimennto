// S0 ログイン画面（docs/screens.md の 2.1）
import { useRef, useState, type FormEvent } from "react";
import { api, ApiError } from "../api";

interface Props {
  onLoggedIn: () => void;
}

export function LoginPage({ onLoggedIn }: Props) {
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [sending, setSending] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!password || sending) return;
    setSending(true);
    try {
      await api.login(password);
      onLoggedIn();
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 401
          ? "パスワードが違います"
          : "ログインできませんでした。通信状況を確認してください",
      );
      setPassword("");
      setSending(false);
      inputRef.current?.focus();
    }
  }

  return (
    <section className="login-screen">
      <form className="login-card" onSubmit={submit}>
        <h1>ボード</h1>
        <label>
          パスワード
          <input
            ref={inputRef}
            type="password"
            autoComplete="current-password"
            autoFocus
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        {/* パスワードが空のあいだはログインボタンを押せない */}
        <button className="primary" disabled={!password || sending}>
          ログイン
        </button>
        {error && <p className="login-error">{error}</p>}
      </form>
    </section>
  );
}
