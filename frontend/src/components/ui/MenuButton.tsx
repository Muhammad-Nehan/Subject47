import { motion } from 'motion/react';
import { ChevronRight } from 'lucide-react';

export interface MenuBtnProps {
  icon: React.ReactNode;
  label: string;
  sub?: string;
  delay?: number;
  accent?: 'cyan' | 'red' | 'gray';
  onClick?: () => void;
  disabled?: boolean;
}

/* ── Menu button ────────────────────────────────────────────────────────── */
export function MenuButton({ icon, label, sub, delay = 0, accent = 'cyan', onClick, disabled }: MenuBtnProps) {
  const colors = {
    cyan: {
      border: 'border-cyan-500/30 hover:border-cyan-400/70',
      bg:     'hover:bg-cyan-950/40',
      text:   'text-cyan-400',
      glow:   '0 0 24px rgba(0,255,255,0.08)',
      bar:    'bg-cyan-500',
    },
    red: {
      border: 'border-red-500/20 hover:border-red-400/50',
      bg:     'hover:bg-red-950/30',
      text:   'text-red-400',
      glow:   '0 0 24px rgba(255,0,0,0.06)',
      bar:    'bg-red-500',
    },
    gray: {
      border: 'border-white/10 hover:border-white/25',
      bg:     'hover:bg-white/5',
      text:   'text-white/40',
      glow:   'none',
      bar:    'bg-white/30',
    },
  }[accent];

  return (
    <motion.button
      initial={{ opacity: 0, x: -30 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.6, delay }}
      whileHover={{ x: 6 }}
      disabled={disabled}
      onClick={onClick}
      className={`
        group relative w-full flex items-center gap-4 px-6 py-4
        border ${colors.border} ${colors.bg}
        backdrop-blur-sm transition-all duration-300 cursor-pointer
        disabled:opacity-30 disabled:cursor-not-allowed
      `}
      style={{ boxShadow: colors.glow }}
    >
      {/* left accent bar */}
      <div className={`absolute left-0 top-0 bottom-0 w-[2px] ${colors.bar} opacity-0 group-hover:opacity-100 transition-opacity`} />

      {/* icon */}
      <div className={`${colors.text} shrink-0 transition-colors`}>{icon}</div>

      {/* labels */}
      <div className="flex-1 text-left">
        <div className={`font-mono text-sm tracking-[0.2em] uppercase ${colors.text} group-hover:text-white transition-colors`}>
          {label}
        </div>
        {sub && (
          <div className="text-[10px] font-mono text-white/20 tracking-widest mt-0.5">{sub}</div>
        )}
      </div>

      {/* chevron */}
      <ChevronRight
        size={14}
        className="text-white/10 group-hover:text-white/50 group-hover:translate-x-1 transition-all"
      />

      {/* scan sweep on hover */}
      <div className="absolute inset-0 translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-700 bg-gradient-to-r from-transparent via-white/5 to-transparent pointer-events-none" />
    </motion.button>
  );
}
