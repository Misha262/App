import { useEffect, useRef, useState, useCallback } from "react";
import "./Snake.css";

const GRID = 22;
const TILE = 18;
const SPEED = 140;

function randomCell(snake) {
  while (true) {
    const cell = { x: Math.floor(Math.random() * GRID), y: Math.floor(Math.random() * GRID) };
    if (!snake.some((p) => p.x === cell.x && p.y === cell.y)) return cell;
  }
}

export default function Snake({ user }) {
  const canvasRef = useRef(null);
  const loopRef = useRef(null);
  const [snake, setSnake] = useState([
    { x: 5, y: 10 },
    { x: 4, y: 10 },
    { x: 3, y: 10 },
  ]);
  const [dir, setDir] = useState({ x: 1, y: 0 });
  const [food, setFood] = useState({ x: 12, y: 10 });
  const [dead, setDead] = useState(false);
  const [score, setScore] = useState(0);
  const touchStart = useRef(null);

  const turn = useCallback((direction) => {
    setDir((d) => {
      if (direction === "up" && d.y !== 1) return { x: 0, y: -1 };
      if (direction === "down" && d.y !== -1) return { x: 0, y: 1 };
      if (direction === "left" && d.x !== 1) return { x: -1, y: 0 };
      if (direction === "right" && d.x !== -1) return { x: 1, y: 0 };
      return d;
    });
  }, []);

  useEffect(() => {
    const handleKey = (e) => {
      if (e.key === "ArrowUp") turn("up");
      if (e.key === "ArrowDown") turn("down");
      if (e.key === "ArrowLeft") turn("left");
      if (e.key === "ArrowRight") turn("right");
      if (e.key === " " && dead) reset();
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [dead, turn]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const handleStart = (e) => {
      const t = e.touches?.[0];
      if (!t) return;
      touchStart.current = { x: t.clientX, y: t.clientY };
    };
    const handleEnd = (e) => {
      const t = e.changedTouches?.[0];
      if (!t || !touchStart.current) return;
      const dx = t.clientX - touchStart.current.x;
      const dy = t.clientY - touchStart.current.y;
      const ax = Math.abs(dx);
      const ay = Math.abs(dy);
      if (Math.max(ax, ay) < 24) return;
      if (ax > ay) {
        turn(dx > 0 ? "right" : "left");
      } else {
        turn(dy > 0 ? "down" : "up");
      }
      touchStart.current = null;
      e.preventDefault();
    };
    canvas.addEventListener("touchstart", handleStart, { passive: true });
    canvas.addEventListener("touchend", handleEnd, { passive: false });
    return () => {
      canvas.removeEventListener("touchstart", handleStart);
      canvas.removeEventListener("touchend", handleEnd);
    };
  }, [turn]);

  useEffect(() => {
    loopRef.current = setInterval(tick, SPEED);
    return () => clearInterval(loopRef.current);
  });

  useEffect(() => {
    draw();
  }, [snake, food, dead]);

  const reset = () => {
    setSnake([
      { x: 5, y: 10 },
      { x: 4, y: 10 },
      { x: 3, y: 10 },
    ]);
    setDir({ x: 1, y: 0 });
    setFood(randomCell([{ x: 5, y: 10 }, { x: 4, y: 10 }, { x: 3, y: 10 }]));
    setDead(false);
    setScore(0);
  };

  const tick = () => {
    if (dead) return;
    setSnake((prev) => {
      const head = { x: prev[0].x + dir.x, y: prev[0].y + dir.y };
      if (head.x < 0 || head.y < 0 || head.x >= GRID || head.y >= GRID || prev.some((p) => p.x === head.x && p.y === head.y)) {
        setDead(true);
        return prev;
      }
      const next = [head, ...prev];
      if (head.x === food.x && head.y === food.y) {
        setScore((s) => s + 10);
        setFood(randomCell(next));
        return next;
      }
      next.pop();
      return next;
    });
  };

  const draw = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    ctx.fillStyle = "#0a0c10";
    ctx.fillRect(0, 0, GRID * TILE, GRID * TILE);

    // grid dots
    ctx.fillStyle = "#111826";
    for (let x = 0; x < GRID; x++) {
      for (let y = 0; y < GRID; y++) {
        ctx.fillRect(x * TILE + TILE / 2 - 1, y * TILE + TILE / 2 - 1, 2, 2);
      }
    }

    // food
    ctx.fillStyle = "#ef4444";
    ctx.fillRect(food.x * TILE, food.y * TILE, TILE, TILE);

    // snake
    snake.forEach((p, idx) => {
      const g = 120 + idx * 2;
      ctx.fillStyle = `rgb(${g},${g},${g})`;
      ctx.fillRect(p.x * TILE, p.y * TILE, TILE, TILE);
    });
  };

  return (
    <div className="snake-wrap">
      <div className="snake-hero">
        <div className="snake-meta">
          <div className="snake-kicker">Classic mode</div>
          <h1>
            You grew up.<span> Snake didn't.</span>
          </h1>
          <p>Retro grid. No fluff, just you, the tail, and the red pixel.</p>
          <div className="snake-cta">
            <button onClick={reset}>Play now</button>
            <div className="snake-score">Score: {score}</div>
          </div>
          {dead && <div className="snake-dead">Game over - press Space/Play or swipe to restart</div>}
        </div>
        <div className="snake-canvas">
          <canvas ref={canvasRef} width={GRID * TILE} height={GRID * TILE} />
        </div>
      </div>
      <div className="snake-controls">
        <button aria-label="Up" onClick={() => turn("up")}>Up</button>
        <div className="snake-controls-row">
          <button aria-label="Left" onClick={() => turn("left")}>Left</button>
          <button aria-label="Down" onClick={() => turn("down")}>Down</button>
          <button aria-label="Right" onClick={() => turn("right")}>Right</button>
        </div>
      </div>
    </div>
  );
}
