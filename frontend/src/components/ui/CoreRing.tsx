/* ── Pulsing ring behind the title ─────────────────────────────────────── */
export function CoreRing() {
  return (
    <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
      {[1, 2, 3].map(i => (
        <div
          key={i}
          className="absolute rounded-full border border-cyan-500/10"
          style={{
            width: `${220 + i * 120}px`,
            height: `${220 + i * 120}px`,
            animation: `ring-pulse ${2 + i * 0.8}s ease-in-out infinite`,
            animationDelay: `${i * 0.4}s`,
          }}
        />
      ))}
    </div>
  );
}
