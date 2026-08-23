/**
 * Subject 47 – Audio Engine
 * Plays the MP3 intro track during the cinematic sequence and generates
 * a low-frequency ambient drone for the main-menu atmosphere.
 */

import introMusicUrl from '../../../assets/Audio/Game intro music.mp3';

class AudioEngine {
  /* ── Ambient drone (main menu) ─────────────────────────────────────── */
  private ctx: AudioContext | null = null;
  private masterBus: GainNode | null = null;
  private droneOsc: OscillatorNode | null = null;
  private glitchTimer: ReturnType<typeof setTimeout> | null = null;

  /* ── Intro MP3 ─────────────────────────────────────────────────────── */
  private introAudio: HTMLAudioElement | null = null;

  // ── Ambtient drone (called when user sits on the main menu) ──────────
  start() {
    if (this.ctx) return;

    this.ctx = new (window.AudioContext || (window as any).webkitAudioContext)();
    this.masterBus = this.ctx.createGain();
    this.masterBus.gain.setValueAtTime(0, this.ctx.currentTime);
    this.masterBus.gain.linearRampToValueAtTime(0.35, this.ctx.currentTime + 2);
    this.masterBus.connect(this.ctx.destination);

    // Deep Base Drone – tuned to Subject-47 frequency
    const osc = this.ctx.createOscillator();
    const lfo = this.ctx.createOscillator();
    const lfoGain = this.ctx.createGain();
    const filter = this.ctx.createBiquadFilter();

    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(47, this.ctx.currentTime);

    lfo.type = 'sine';
    lfo.frequency.setValueAtTime(0.5, this.ctx.currentTime);
    lfoGain.gain.setValueAtTime(2, this.ctx.currentTime);

    filter.type = 'lowpass';
    filter.frequency.setValueAtTime(200, this.ctx.currentTime);
    filter.Q.setValueAtTime(10, this.ctx.currentTime);

    lfo.connect(lfoGain);
    lfoGain.connect(osc.frequency);
    osc.connect(filter);
    filter.connect(this.masterBus);

    osc.start();
    lfo.start();
    this.droneOsc = osc;

    this.scheduleGlitch();
  }

  private scheduleGlitch() {
    if (!this.ctx || !this.masterBus) return;

    const delay = Math.random() * 5 + 3;
    const time = this.ctx.currentTime + delay;

    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();

    osc.type = 'square';
    osc.frequency.setValueAtTime(Math.random() * 100 + 40, time);
    osc.frequency.exponentialRampToValueAtTime(1, time + 0.5);

    gain.gain.setValueAtTime(0, time);
    gain.gain.linearRampToValueAtTime(0.05, time + 0.01);
    gain.gain.exponentialRampToValueAtTime(0.001, time + 0.5);

    osc.connect(gain);
    gain.connect(this.masterBus);

    osc.start(time);
    osc.stop(time + 0.5);

    this.glitchTimer = setTimeout(() => this.scheduleGlitch(), delay * 1000 + 100);
  }

  stop() {
    if (this.glitchTimer) clearTimeout(this.glitchTimer);
    if (this.masterBus && this.ctx) {
      this.masterBus.gain.linearRampToValueAtTime(0, this.ctx.currentTime + 1);
      setTimeout(() => {
        this.ctx?.close();
        this.ctx = null;
        this.masterBus = null;
        this.droneOsc = null;
      }, 1100);
    }
  }

  // ── Intro MP3 ───────────────────────────────────────────────────────
  playIntro(muted = false) {
    if (this.introAudio) {
      this.introAudio.currentTime = 0;
    } else {
      this.introAudio = new Audio(introMusicUrl);
      this.introAudio.loop = false;
    }
    this.introAudio.muted = muted;
    this.introAudio.volume = 0.9;
    this.introAudio.play().catch(() => {/* autoplay blocked – silently ignore */ });
  }

  stopIntro() {
    if (!this.introAudio) return;
    // Fade out gently
    const step = () => {
      if (!this.introAudio) return;
      if (this.introAudio.volume > 0.05) {
        this.introAudio.volume = Math.max(0, this.introAudio.volume - 0.05);
        setTimeout(step, 80);
      } else {
        this.introAudio!.pause();
        this.introAudio!.currentTime = 0;
      }
    };
    step();
  }

  setIntroMuted(muted: boolean) {
    if (this.introAudio) this.introAudio.muted = muted;
  }
}

export const audioEngine = new AudioEngine();
