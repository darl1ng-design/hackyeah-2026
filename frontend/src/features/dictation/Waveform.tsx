// Live input level as a dotted halftone waveform (prototype reference image), drawn on canvas.
import { useEffect, useRef } from 'react';
import type { Levels } from './useDictation';
import s from './Dictation.module.css';

type Props = { getLevels: () => Levels | null; compact?: boolean };

const WINDOW_MS = 6000; // visible history
const COL = 6; // px between dot columns

export function Waveform({ getLevels, compact }: Props) {
  const ref = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = ref.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const css = getComputedStyle(canvas);
    const from = css.getPropertyValue('--red-600').trim() || 'red';
    const to = css.getPropertyValue('--yellow-500').trim() || 'orange';
    const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
    let raf = 0;

    const draw = () => {
      const dpr = devicePixelRatio || 1;
      const w = canvas.clientWidth;
      const h = canvas.clientHeight;
      if (canvas.width !== w * dpr) {
        canvas.width = w * dpr;
        canvas.height = h * dpr;
      }
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      ctx.clearRect(0, 0, w, h);
      const lv = getLevels();
      if (lv) {
        const grad = ctx.createLinearGradient(0, 0, w, 0);
        grad.addColorStop(0, from);
        grad.addColorStop(1, to);
        ctx.fillStyle = grad;
        const cols = Math.floor(w / COL);
        const mid = h / 2;
        const rows = Math.floor(mid / COL);
        for (let c = 0; c < cols; c++) {
          // newest sample at the right edge
          const t = lv.now - ((cols - 1 - c) / cols) * WINDOW_MS;
          let v = 0;
          for (let i = lv.samples.length - 1; i >= 0; i--) {
            if (lv.samples[i][0] <= t) {
              v = t - lv.samples[i][0] < 120 ? lv.samples[i][1] : 0;
              break;
            }
          }
          const x = c * COL + COL / 2;
          ctx.globalAlpha = 0.35 + 0.65 * (c / cols);
          ctx.beginPath();
          ctx.arc(x, mid, 1.2, 0, Math.PI * 2); // baseline dot
          ctx.fill();
          const lit = Math.round(v * rows);
          for (let r = 1; r <= lit; r++) {
            const rad = 1.9 * (1 - r / (rows + 1)) + 0.5; // halftone: dots shrink away from the centre
            for (const y of [mid - r * COL, mid + r * COL]) {
              ctx.beginPath();
              ctx.arc(x, y, rad, 0, Math.PI * 2);
              ctx.fill();
            }
          }
        }
        ctx.globalAlpha = 1;
      }
      if (!reduce) raf = requestAnimationFrame(draw);
    };
    draw();
    const slow = reduce ? window.setInterval(draw, 500) : 0;
    return () => {
      cancelAnimationFrame(raf);
      clearInterval(slow);
    };
  }, [getLevels]);

  return <canvas ref={ref} className={compact ? s.waveSmall : s.wave} aria-hidden="true" />;
}
