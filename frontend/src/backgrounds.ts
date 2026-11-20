// 背景写真（docs/features.md の F11）。写真は public/images/ に置き、ライセンスは images/CREDITS.md にまとめてある。
// credit は右下に出す撮影者名・ライセンス、url はその出典ページ。写真を足したら CREDITS.md も更新する
export interface Background {
  src: string;
  credit: string;
  url: string;
}

export const BACKGROUNDS: Background[] = [
  {
    src: "/images/eiffel-tower.jpg",
    credit: "写真: Jorge Royan / CC BY-SA 3.0 / Wikimedia Commons",
    url: "https://commons.wikimedia.org/wiki/File:Paris_-_The_Eiffel_Tower_in_spring_-_2307.jpg",
  },
  {
    src: "/images/colosseum.jpg",
    credit: "写真: Diliff / CC BY-SA 2.5 / Wikimedia Commons",
    url: "https://commons.wikimedia.org/wiki/File:Colosseum_in_Rome-April_2007-1-_copie_2B.jpg",
  },
  {
    src: "/images/machu-picchu.jpg",
    credit: "写真: Diego Delso / CC BY-SA 4.0 / Wikimedia Commons",
    url: "https://commons.wikimedia.org/wiki/File:Machu_Picchu,_Per%C3%BA,_2015-07-30,_DD_60.JPG",
  },
  {
    src: "/images/neuschwanstein.jpg",
    credit: "写真: Wilfredor / CC0 / Wikimedia Commons",
    url: "https://commons.wikimedia.org/wiki/File:Neuschwanstein_Castle_2024.jpg",
  },
  {
    src: "/images/golden-gate-bridge.jpg",
    credit: "写真: Frank Schulenburg / CC BY-SA 4.0 / Wikimedia Commons",
    url: "https://commons.wikimedia.org/wiki/File:Golden_Gate_Bridge_as_seen_from_Marshall%E2%80%99s_Beach,_March_2018.jpg",
  },
  {
    src: "/images/santorini-oia.jpg",
    credit: "写真: Giles Laurent / CC BY-SA 4.0 / Wikimedia Commons",
    url: "https://commons.wikimedia.org/wiki/File:1000_Three_domes_of_Oia_in_Santorini_Photo_by_Giles_Laurent.jpg",
  },
];
