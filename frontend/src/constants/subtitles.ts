/** Narrative subtitle entries for the 20-second cinematic sequence */
export interface Subtitle {
  start: number;
  end: number;
  text: string;
}

export const SUBTITLES: Subtitle[] = [
  { start: 0.5,  end: 4,    text: "LAB 04: CLASSIFIED HUMAN EXPERIMENTATION FACILITY" },
  { start: 4.5,  end: 8,    text: "MOST SUBJECTS TERMINATED FOLLOWING ADVERSE COGNITIVE SYNC" },
  { start: 8.5,  end: 12,   text: "SUBJECT 47: BIOLOGICAL STABILITY ATTAINED." },
  { start: 12.5, end: 16,   text: "CRITICAL ALERT: FACILITY SHUTDOWN BY UNKNOWN ACTOR" },
  { start: 16.5, end: 19.5, text: "PROTOCOL 47: MEMORY WIPE COMPLETE. WAKING REPAIR TECHNICIAN..." },
];
