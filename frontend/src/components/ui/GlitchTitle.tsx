import { useState, useEffect } from 'react';
import { motion } from 'motion/react';

/* ── Glitchy main title ─────────────────────────────────────────────────── */
export function GlitchTitle() {
  const [glitch, setGlitch] = useState(false);

  useEffect(() => {
    const fire = () => {
      setGlitch(true);
      setTimeout(() => setGlitch(false), 180);
      setTimeout(fire, Math.random() * 5000 + 2000);
    };
    const t = setTimeout(fire, 1500);
    return () => clearTimeout(t);
  }, []);

  return (
    <div className="relative select-none">
      {/* sub-label */}
      <motion.div
        initial={{ opacity: 0, letterSpacing: '0.5em' }}
        animate={{ opacity: 1, letterSpacing: '1.3em' }}
        transition={{ duration: 1.2, delay: 0.3 }}
        className="text-[9px] font-mono tracking-[1.3em] text-cyan-400/50 mb-5 uppercase text-center ml-[1.3em]"
      >
        A Psychological Horror Experience
      </motion.div>

      {/* main heading */}
      <div className="relative">
        <motion.h1
          initial={{ opacity: 0, y: 20, filter: 'blur(12px)' }}
          animate={{ opacity: 1, y: 0, filter: 'blur(0px)' }}
          transition={{ duration: 1.4, delay: 0.5, ease: 'easeOut' }}
          className={`text-[5rem] md:text-[8rem] font-black tracking-[-0.02em] text-white text-center leading-none glitch-title ${glitch ? 'is-glitching' : ''}`}
          data-text="SUBJECT 47"
        >
          SUBJECT 47
        </motion.h1>

        {/* side accent lines */}
        <motion.div
          initial={{ scaleX: 0 }}
          animate={{ scaleX: 1 }}
          transition={{ duration: 1, delay: 1.2 }}
          className="h-px bg-gradient-to-r from-transparent via-cyan-500/60 to-transparent mt-4"
        />
      </div>

      {/* classification label */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.8, delay: 1.8 }}
        className="flex items-center justify-center gap-3 mt-3"
      >
        <div className="h-px w-16 bg-red-500/40" />
        <span className="text-[9px] font-mono text-red-400/70 tracking-[0.4em] uppercase">
          Classified — Eyes Only
        </span>
        <div className="h-px w-16 bg-red-500/40" />
      </motion.div>
    </div>
  );
}
