import { motion, AnimatePresence } from 'motion/react';
import { type Subtitle } from '../../constants/subtitles';

interface SubtitleOverlayProps {
  subtitle: Subtitle | undefined;
}

/* ── Subtitle overlay shown during the cinematic sequence ───────────────── */
export function SubtitleOverlay({ subtitle }: SubtitleOverlayProps) {
  return (
    <div className="flex justify-center flex-grow items-end pb-24 pointer-events-none">
      <AnimatePresence mode="wait">
        {subtitle && (
          <motion.div
            key={subtitle.text}
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 1.05 }}
            transition={{ duration: 0.8 }}
            className="max-w-3xl text-center"
          >
            <div className="space-y-4">
              <p className="italic text-2xl md:text-3xl font-serif text-white/80 tracking-wide leading-relaxed">
                "{subtitle.text}"
              </p>
              <div className="flex gap-4 justify-center items-center">
                <div className="w-1.5 h-1.5 bg-red-600 animate-pulse" />
                <div className="text-[10px] font-mono tracking-[0.3em] uppercase opacity-40">
                  Processing decryption stream...
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
