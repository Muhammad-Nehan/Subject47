/* ── Sci-fi corner bracket ─────────────────────────────────────────────── */
interface CornerBracketProps {
  pos: 'tl' | 'tr' | 'bl' | 'br';
}

export function CornerBracket({ pos }: CornerBracketProps) {
  const cls = {
    tl: 'top-8 left-8 border-t border-l',
    tr: 'top-8 right-8 border-t border-r',
    bl: 'bottom-8 left-8 border-b border-l',
    br: 'bottom-8 right-8 border-b border-r',
  }[pos];
  return <div className={`absolute w-12 h-12 ${cls} border-cyan-500/40 pointer-events-none z-30`} />;
}
