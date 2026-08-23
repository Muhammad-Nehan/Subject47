import { useState, useEffect } from 'react';
import { motion } from 'motion/react';

/* ── Status readout strip ───────────────────────────────────────────────── */
export function StatusReadout() {
  const [tick, setTick] = useState(0);
  useEffect(() => {
    const id = setInterval(() => setTick(t => t + 1), 1200);
    return () => clearInterval(id);
  }, []);

  const statuses = ['NOMINAL', 'STANDBY', 'NOMINAL'];
  const labels   = ['NEURAL-SYNC', 'BIO-MONITOR', 'MEMORY-CORE'];
  const idx = tick % 3;

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ delay: 2.4 }}
      className="flex items-center gap-6 font-mono text-[9px] tracking-widest uppercase"
    >
      {labels.map((l, i) => (
        <div key={l} className="flex items-center gap-2">
          <div className={`w-1.5 h-1.5 rounded-full ${i === idx ? 'bg-cyan-400 animate-pulse' : 'bg-white/15'}`} />
          <span className="text-white/25">{l}:</span>
          <span className={i === idx ? 'text-cyan-400/80' : 'text-white/15'}>{statuses[i]}</span>
        </div>
      ))}
    </motion.div>
  );
}
