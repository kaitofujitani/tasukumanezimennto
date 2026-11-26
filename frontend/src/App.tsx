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
    const onLoad = () => {
      setBackground(pick);
      document.body.classList.add("has-bg");
    };
    img.addEventListener("load", onLoad);
    img.src = pick.src;
    return () => {
      img.removeEventListener("load", onLoad);
      document.body.classList.remove("has-bg");
    };
  }, []);

  return (
    <>
      {/* iPhone の Safari でも固定表示されるよう、background-attachment: fixed ではなく固定配置の要素に敷く */}
      <div
        className="fixed inset-0 -z-1 bg-cover bg-center bg-no-repeat opacity-0 transition-opacity duration-600 photo:opacity-100"
        style={background ? { backgroundImage: `url("${background.src}")` } : undefined}
      />
      {background && (
        <a
          className="fixed right-2 bottom-1.5 text-[.7rem] text-white no-underline [text-shadow:0_1px_2px_rgb(0_0_0/.8)] hover:underline"
          href={background.url}
          target="_blank"
          rel="noopener noreferrer"
        >
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
