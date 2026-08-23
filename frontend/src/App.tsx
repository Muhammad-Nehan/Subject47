import { useState, useEffect, useMemo, useCallback } from 'react';
import { Canvas } from '@react-three/fiber';
import { motion, AnimatePresence } from 'motion/react';
import { RotateCcw, Volume2, VolumeX } from 'lucide-react';
import { LabScene } from './components/VoxelScene';
import { CinematicCamera } from './components/CinematicCamera';
import { HexGrid } from './components/ui/HexGrid';
import { ParticleField } from './components/ui/ParticleField';
import { CornerBracket } from './components/ui/CornerBracket';
import { MainMenu } from './components/ui/MainMenu';
import { SubtitleOverlay } from './components/ui/SubtitleOverlay';
import { SUBTITLES } from './constants/subtitles';
import { audioEngine } from './lib/AudioEngine';

export default function App() {
  const [isPlaying, setIsPlaying]     = useState(false);
  const [progress,  setProgress]      = useState(0);
  const [showLogo,  setShowLogo]      = useState(false);
  const [muted,     setMuted]         = useState(false);
  const [menuVisible, setMenuVisible] = useState(true);

  /* ── Cinematic progress ticker ─────────────────────────────────────── */
  useEffect(() => {
    let animationFrame: number;
    let startTime: number;

    const animate = (time: number) => {
      if (!startTime) startTime = time;
      const elapsed = (time - startTime) / 1000;
      const newProgress = Math.min(elapsed / 20, 1);

      setProgress(newProgress);
      if (newProgress >= 0.9) setShowLogo(true);

      if (newProgress < 1) {
        animationFrame = requestAnimationFrame(animate);
      } else {
        setIsPlaying(false);
        setMenuVisible(true);
        audioEngine.stopIntro();
        audioEngine.start();   // fade in ambient drone on menu return
      }
    };

    if (isPlaying) {
      animationFrame = requestAnimationFrame(animate);
    }
    return () => cancelAnimationFrame(animationFrame);
  }, [isPlaying]);

  /* ── Current subtitle ──────────────────────────────────────────────── */
  const currentSubtitle = useMemo(() => {
    const time = progress * 20;
    return SUBTITLES.find(s => time >= s.start && time <= s.end);
  }, [progress]);

  /* ── Start cinematic ───────────────────────────────────────────────── */
  const handleStart = useCallback(() => {
    setProgress(0);
    setShowLogo(false);
    setMenuVisible(false);
    setIsPlaying(true);
    audioEngine.stop();
    audioEngine.playIntro(muted);
  }, [muted]);

  /* ── Mute toggle ───────────────────────────────────────────────────── */
  const toggleMute = useCallback(() => {
    const next = !muted;
    setMuted(next);
    if (isPlaying) {
      audioEngine.setIntroMuted(next);
    } else {
      if (next) audioEngine.stop();
      else      audioEngine.start();
    }
  }, [muted, isPlaying]);

  /* ── Ambient drone auto-start when menu is visible ─────────────────── */
  useEffect(() => {
    if (menuVisible && !muted) audioEngine.start();
  }, [menuVisible, muted]);

  return (
    <div className="relative w-full h-screen bg-black overflow-hidden font-sans text-white">

      {/* ── 3D Scene ─────────────────────────────────────────────────── */}
      <div className="absolute inset-0 z-0">
        <Canvas gl={{ antialias: false, powerPreference: 'high-performance' }} dpr={[1, 2]}>
          <LabScene />
          <CinematicCamera progress={progress} />
        </Canvas>
      </div>

      {/* ── Hex-grid overlay ─────────────────────────────────────────── */}
      <HexGrid />

      {/* ── Particle field ───────────────────────────────────────────── */}
      <ParticleField />

      {/* ── Visual post-overlays ─────────────────────────────────────── */}
      <div className="absolute inset-0 z-40 pointer-events-none">
        <div className="w-full h-full scanline-overlay opacity-15" />
        <div className="scanner-line" />
        <div className="absolute top-0 left-1/3 w-96 h-96 bg-blue-900/10 blur-[120px] rounded-full animate-flicker" />
        <div className="absolute bottom-20 right-1/4 w-[500px] h-[500px] bg-red-900/5 blur-[150px] rounded-full animate-flicker" />
      </div>

      {/* ── Corner brackets ──────────────────────────────────────────── */}
      <CornerBracket pos="tl" />
      <CornerBracket pos="tr" />
      <CornerBracket pos="bl" />
      <CornerBracket pos="br" />

      {/* ── HUD Top Bar ──────────────────────────────────────────────── */}
      <div className="absolute inset-0 z-30 pointer-events-none flex flex-col justify-between p-12">
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1,  y: 0 }}
          transition={{ duration: 0.8, delay: 0.5 }}
          className="flex justify-between items-start"
        >
          {/* Left HUD */}
          <div className="font-mono text-[9px] tracking-widest text-cyan-400/50 flex flex-col gap-1.5 uppercase">
            <div className="flex items-center gap-2">
              <div className="w-1.5 h-1.5 bg-cyan-500 rounded-full animate-pulse" />
              <span>SECTOR_7 · SUBLEVEL_04</span>
            </div>
            <div>
              FACILITY_STATUS: <span className="text-red-400">CRITICAL_FAILURE</span>
            </div>
            <div className="text-white/20">REC ● SITE_47_LOG</div>
          </div>

          {/* Right HUD */}
          <div className="text-right font-mono text-[9px] tracking-widest text-white/30 uppercase flex flex-col items-end gap-2 pointer-events-auto">
            <div className="flex gap-4 items-center">
              <button
                onClick={e => { e.stopPropagation(); toggleMute(); }}
                className="text-white/30 hover:text-cyan-400 transition-colors"
              >
                {muted ? <VolumeX size={14} /> : <Volume2 size={14} />}
              </button>
              <div>AUTH: TECH_UNIT_884</div>
            </div>
            <div className="flex gap-2 justify-end items-center mt-1">
              <span>SIGNAL</span>
              <div className="flex gap-1">
                {Array.from({ length: 4 }).map((_, i) => (
                  <div key={i} className={`h-1 w-2 ${i < 3 ? 'bg-cyan-500' : 'bg-gray-700'}`} />
                ))}
              </div>
            </div>
          </div>
        </motion.div>

        {/* Subtitle area (during cinematic) */}
        <SubtitleOverlay subtitle={currentSubtitle} />
      </div>

      {/* ── Main Menu ────────────────────────────────────────────────── */}
      <AnimatePresence>
        {menuVisible && (
          <motion.div
            key="main-menu"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.6 }}
          >
            <MainMenu onStart={handleStart} />
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── Replay Button (after intro finishes) ─────────────────────── */}
      <AnimatePresence>
        {!isPlaying && !menuVisible && progress > 0 && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-md pointer-events-auto"
          >
            <button
              onClick={handleStart}
              className="flex items-center gap-3 text-white/30 hover:text-cyan-400 transition-colors uppercase text-[10px] tracking-[0.3em] font-mono border border-white/10 hover:border-cyan-500/40 px-8 py-4 bg-white/5 backdrop-blur hover:bg-cyan-950/30"
            >
              <RotateCcw size={14} /> REPLAY_SEQUENCE
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── Ending Logo ──────────────────────────────────────────────── */}
      <AnimatePresence>
        {showLogo && progress > 0.9 && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 z-[100] flex items-center justify-center bg-black pointer-events-none"
          >
            <motion.div
              initial={{ scale: 0.8, letterSpacing: '0.1em', filter: 'blur(20px)' }}
              animate={{ scale: 1,   letterSpacing: '1.2em', filter: 'blur(0px)' }}
              transition={{ duration: 2.5, ease: 'easeOut' }}
              className="text-white text-6xl md:text-8xl font-black italic select-none overflow-hidden whitespace-nowrap glow-blue tracking-[1.2em] opacity-95 text-center ml-[1.2em]"
            >
              SUBJECT 47
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── Film grain ───────────────────────────────────────────────── */}
      <div className="absolute inset-0 pointer-events-none z-[110] opacity-25 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] mix-blend-overlay" />
    </div>
  );
}
