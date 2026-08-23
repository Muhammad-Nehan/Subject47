import { useMemo } from 'react';

/* ── Floating particle dot ─────────────────────────────────────────────── */
function Particle({ style }: { style: React.CSSProperties }) {
  return <div className="particle" style={style} />;
}

/* ── Static particle field (memoised to prevent re-randomise) ──────────── */
export function ParticleField() {
  const particles = useMemo(() =>
    Array.from({ length: 60 }, (_, i) => ({
      id: i,
      style: {
        left: `${Math.random() * 100}%`,
        top: `${Math.random() * 100}%`,
        width: `${Math.random() * 3 + 1}px`,
        height: `${Math.random() * 3 + 1}px`,
        opacity: Math.random() * 0.5 + 0.1,
        animationDuration: `${Math.random() * 8 + 4}s`,
        animationDelay: `${Math.random() * 4}s`,
      } as React.CSSProperties,
    })), []);

  return (
    <div className="absolute inset-0 pointer-events-none z-10 overflow-hidden">
      {particles.map(p => <Particle key={p.id} style={p.style} />)}
    </div>
  );
}
