import { motion } from 'motion/react';
import { Play, RotateCcw, Settings, Info } from 'lucide-react';
import { CoreRing } from './CoreRing';
import { GlitchTitle } from './GlitchTitle';
import { MenuButton } from './MenuButton';
import { StatusReadout } from './StatusReadout';

interface MainMenuProps {
  onStart: () => void;
}

/* ── Main menu panel ────────────────────────────────────────────────────── */
export function MainMenu({ onStart }: MainMenuProps) {
  return (
    <div
      className="absolute inset-0 z-50 flex items-center justify-center pointer-events-auto"
      style={{
        background: 'radial-gradient(ellipse at center, rgba(0,20,40,0.85) 0%, rgba(0,0,0,0.95) 70%)',
        backdropFilter: 'blur(4px)',
      }}
    >
      {/* Pulsing rings behind everything */}
      <CoreRing />

      {/* Content container */}
      <div className="relative z-10 flex flex-col items-center gap-12 w-full max-w-lg px-6">

        {/* ── Title ─────────────────────────────────────────── */}
        <GlitchTitle />

        {/* ── Menu Buttons ──────────────────────────────────── */}
        <div className="w-full flex flex-col gap-2">
          <MenuButton
            icon={<Play size={18} fill="currentColor" />}
            label="Initialize Protocol"
            sub="Begin classified sequence"
            delay={1.4}
            accent="cyan"
            onClick={onStart}
          />
          <MenuButton
            icon={<RotateCcw size={16} />}
            label="Continue Memory Restore"
            sub="Resume last checkpoint"
            delay={1.6}
            accent="cyan"
            disabled
          />
          <MenuButton
            icon={<Settings size={16} />}
            label="System Configuration"
            sub="Audio · Display · Controls"
            delay={1.8}
            accent="gray"
            disabled
          />
          <MenuButton
            icon={<Info size={16} />}
            label="Declassified Files"
            sub="Lore · Credits · Build 0.1.0"
            delay={2.0}
            accent="red"
            disabled
          />
        </div>

        {/* ── Status readout strip ──────────────────────────── */}
        <StatusReadout />
      </div>

      {/* Bottom build info */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 2.6 }}
        className="absolute bottom-10 left-1/2 -translate-x-1/2 font-mono text-[8px] tracking-[0.3em] text-white/15 uppercase"
      >
        BUILD 0.1.0-ALPHA · CLEARANCE LEVEL: OMEGA
      </motion.div>
    </div>
  );
}
