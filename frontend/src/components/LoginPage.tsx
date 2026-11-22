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
    <section className="grid min-h-screen place-items-center p-4">
      <form
        className="grid w-[min(360px,100%)] gap-3 rounded-xl bg-column p-6 photo:bg-glass photo:backdrop-blur-[6px]"
        onSubmit={submit}
      >
        <h1 className="text-2xl font-bold">ボード</h1>
        <label className="grid gap-1 text-[.85rem] text-muted">
          パスワード
          <input
            className="text-fg"
            ref={inputRef}
            type="password"
            autoComplete="current-password"
            autoFocus
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        {/* パスワードが空のあいだはログインボタンを押せない */}
        <button
          className="justify-self-end border-none bg-accent font-semibold text-white"
          disabled={!password || sending}
        >
          ログイン
        </button>
        {error && <p className="text-[.9rem] text-danger">{error}</p>}
      </form>
    </section>
  );
}
