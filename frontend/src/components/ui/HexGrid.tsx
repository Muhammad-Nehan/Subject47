/* ── Hex-grid background ────────────────────────────────────────────────── */
export function HexGrid() {
  return (
    <div
      className="absolute inset-0 pointer-events-none z-[1] opacity-[0.04]"
      style={{
        backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='56' height='100'%3E%3Cpath d='M28 66L0 50V16L28 0l28 16v34L28 66zM0 50L28 34l28 16M28 0v34M0 16l28 16 28-16' fill='none' stroke='%2300FFFF' stroke-width='1'/%3E%3C/svg%3E")`,
        backgroundSize: '56px 100px',
      }}
    />
  );
}
