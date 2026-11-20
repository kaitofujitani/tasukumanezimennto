// ログイン画面とボード画面の切り替えと、背景写真（docs/screens.md の 1章）
import { useCallback, useEffect, useState } from "react";
import { BACKGROUNDS, type Background } from "./backgrounds";
import { Board } from "./components/Board";
import { LoginPage } from "./components/LoginPage";

type Screen = "board" | "login";

export function App() {
  // まずボードを読み込んでみて、ログインしていなければ（401）ログイン画面に切り替える
  const [screen, setScreen] = useState<Screen>("board");
  const [background, setBackground] = useState<Background | null>(null);

  const showLogin = useCallback(() => setScreen("login"), []);

  // ページを開くたびに背景写真をランダムで1枚選ぶ。読み込めたときだけ表示する（読めなければ写真なし）
  useEffect(() => {
    const pick = BACKGROUNDS[Math.floor(Math.random() * BACKGROUNDS.length)];
    const img = new Image();
    img.onload = () => {
      setBackground(pick);
      document.body.classList.add("has-bg");
    };
    img.src = pick.src;
    return () => {
      img.onload = null;
    };
  }, []);

  return (
    <>
      <div className="bg" style={background ? { backgroundImage: `url("${background.src}")` } : undefined} />
      {background && (
        <a className="credit" href={background.url} target="_blank" rel="noopener noreferrer">
          {background.credit}
        </a>
      )}
      {screen === "login" ? (
        <LoginPage onLoggedIn={() => setScreen("board")} />
      ) : (
        <Board onUnauthorized={showLogin} onLoggedOut={showLogin} />
      )}
    </>
  );
}
