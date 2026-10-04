// Live input level as a dotted halftone waveform (prototype reference image), drawn on canvas.
// One level sample (every 50 ms) = one dot column; columns scroll left continuously by elapsed time.
// Always 60 fps, also with prefers-reduced-motion: it is a live input meter (feedback), not decoration.
import { useEffect, useRef } from "react";
import type { Levels } from "./useDictation";
import s from "./Dictation.module.css";

type Props = { getLevels: () => Levels | null; compact?: boolean };

const COL = 6; // px between dot columns
const SAMPLE_MS = 50; // matches the sampler in useDictation

export function Waveform({ getLevels, compact }: Props) {
  const ref = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = ref.current;
    const ctx = canvas?.getContext("2d");
    if (!canvas || !ctx) return;
    const css = getComputedStyle(canvas);
    const from = css.getPropertyValue("--red-600").trim() || "red";
    const to = css.getPropertyValue("--yellow-500").trim() || "orange";
    let raf = 0;
    let w = 0;
    let h = 0;
    let grad: CanvasGradient | null = null;

    // Size the backing store only when the box changes (integer px: fractional dpr never compares equal).
    const resize = () => {
      const dpr = devicePixelRatio || 1;
      w = canvas.clientWidth;
      h = canvas.clientHeight;
      canvas.width = Math.round(w * dpr);
      canvas.height = Math.round(h * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      grad = ctx.createLinearGradient(0, 0, w, 0);
      grad.addColorStop(0, from);
      grad.addColorStop(1, to);
    };
    resize();
    const ro = new ResizeObserver(resize);
    ro.observe(canvas);

    const draw = () => {
      ctx.clearRect(0, 0, w, h);
      const lv = getLevels();
      if (lv && grad) {
        ctx.fillStyle = grad;
        const mid = h / 2;
        const rows = Math.max(1, Math.floor(mid / COL));
        const now = performance.now();
        let smooth = 0;
        const { samples } = lv;
        // oldest visible sample first, so the attack/decay smoothing runs forward in time
        const visible = Math.ceil(w / COL) + 2;
        for (
          let i = Math.max(0, samples.length - visible);
          i < samples.length;
          i++
        ) {
          const [t, v] = samples[i];
          smooth =
            v > smooth
              ? smooth + (v - smooth) * 0.7
              : smooth + (v - smooth) * 0.35;
          const x = w - COL / 2 - ((now - t) / SAMPLE_MS) * COL;
          if (x < -COL) continue;
          ctx.globalAlpha = 0.35 + 0.65 * Math.max(0, x / w);
          ctx.beginPath();
          ctx.arc(x, mid, 1.2, 0, Math.PI * 2); // baseline dot
          for (let r = 1, lit = Math.round(smooth * rows); r <= lit; r++) {
            const rad = 1.9 * (1 - r / (rows + 1)) + 0.5; // halftone: dots shrink away from the centre
            ctx.moveTo(x + rad, mid - r * COL);
            ctx.arc(x, mid - r * COL, rad, 0, Math.PI * 2);
            ctx.moveTo(x + rad, mid + r * COL);
            ctx.arc(x, mid + r * COL, rad, 0, Math.PI * 2);
          }
          ctx.fill(); // one fill per column instead of one per dot
        }
        ctx.globalAlpha = 1;
      }
      raf = requestAnimationFrame(draw);
    };
    draw();
    return () => {
      cancelAnimationFrame(raf);
      ro.disconnect();
    };
  }, [getLevels]);

  return (
    <canvas
      ref={ref}
      className={compact ? s.waveSmall : s.wave}
      aria-hidden="true"
    />
  );
}
