# Architecture Overview

Subject 47 uses a **dual-layer monorepo architecture**. The two layers are independent runtimes that share assets but communicate through no runtime interface — they are separate executables.

## Layer 1 — Java Game Engine (`game/`)

| Technology | Version | Role |
|---|---|---|
| jMonkeyEngine 3 | 3.6.1-stable | 3D rendering, scene graph, input |
| LWJGL 3 | (bundled with jME) | Native OpenGL / OpenAL |
| JLayer | 1.0.1 | Pure-Java MP3 playback |
| SLF4J | 2.0.9 | Logging |
| JDK | 21 | Runtime |

### Package Structure

```
game/src/main/java/subject47/
├── Main.java              # Application entry, lifecycle orchestration
├── audio/
│   └── AudioManager.java  # OGG/MP3 audio, ambient music, SFX
├── events/
│   └── EventManager.java  # Pub/sub event bus
├── interaction/
│   ├── Interactable.java  # Interface: any object the player can interact with
│   ├── Terminal.java      # Readable log terminals → triggers narrative modals
│   ├── Keypad.java        # Numeric code-entry panels
│   ├── ControlSwitch.java # Toggle switches
│   └── Door.java          # Animated sliding/rotating doors
├── level/
│   ├── LevelManager.java  # Level loading, transitions, checkpoint saves
│   ├── EditorManager.java # In-engine prop editor (F3 to toggle)
│   └── CameraSystem.java  # First-person camera rig
├── player/
│   └── PlayerController.java # WASD movement, mouse look, collision
├── ui/
│   ├── UIManager.java     # HUD, narrative modals, keypad overlay
│   ├── MainMenu.java      # jME Nifty GUI main menu
│   └── IntroSequence.java # Scripted cinematic intro sequence
└── world/
    ├── WorldManager.java  # Chunk/block world: loading, rendering, culling
    ├── LabGenerator.java  # Procedural corridor generation
    ├── Chunk.java         # 16x16x16 block chunk
    ├── BlockType.java     # Enum: VOID, FLOOR, WALL, CEILING, LIGHT, GLASS…
    └── EditorPropType.java # Enum: TERMINAL, DOOR, KEYPAD, SWITCH…
```

### Game Loop

```
Main.simpleUpdate(tpf)
 ├─ UIManager.update()         (always runs — HUD, modal timers)
 ├─ WorldManager.update()      (always runs — chunk streaming)
 ├─ MainMenu.update()          (always runs — menu animation)
 ├─ IntroSequence.update()     (blocks gameplay while active)
 └─ [if gameplayActive]
     ├─ PlayerController.update()
     ├─ EditorManager.update()
     └─ [if editor not active]
         ├─ LevelManager.update()
         └─ EventManager.update()
```

---

## Layer 2 — React Frontend (`frontend/`)

| Technology | Version | Role |
|---|---|---|
| React | 19 | UI framework |
| TypeScript | 5.8 | Type safety |
| Vite | 6 | Build + HMR dev server |
| Three.js | r184 | 3D WebGL rendering |
| @react-three/fiber | 9 | React renderer for Three.js |
| @react-three/postprocessing | 3 | Post-process effects |
| Motion (Framer Motion) | 12 | UI animations |
| Tailwind CSS | 4 | Utility styling |
| Web Audio API | native | Procedural ambient drone |

### Component Tree

```
App
├── Canvas (R3F)
│   ├── LabScene           — Voxel corridor scene
│   └── CinematicCamera    — Scripted camera path
├── HexGrid                — Background hex pattern overlay
├── ParticleField          — Floating particle dots
├── [post-overlays]        — Scanlines, scanner line, bloom glows
├── CornerBracket ×4       — Sci-fi HUD corners
├── [HUD Top Bar]          — Facility status, auth, signal strength, mute
├── SubtitleOverlay        — Animated subtitle during cinematic
├── MainMenu               — Animated main menu panel
│   ├── GlitchTitle        — "SUBJECT 47" with random glitch
│   ├── MenuButton ×4      — Initialize / Continue / Settings / Files
│   ├── StatusReadout      — Neural-sync / Bio-monitor / Memory-core
│   └── CoreRing           — Pulsing rings behind title
├── [Replay Button]        — Appears after cinematic ends
└── [Ending Logo]          — "SUBJECT 47" reveal at cinematic end
```

---

## Shared Resources

```
assets/          — Game assets (audio, textures, models, shaders)
maps/            — Level layout files (.map) and prop files (.props)
```

The Java engine reads these at runtime. The React frontend reads only `assets/Audio/` (for the intro music MP3) via Vite's asset import pipeline.

---

## Why Two Layers?

The React frontend was originally a standalone cinematic/menu prototype built in AI Studio. The Java engine is the core game. Rather than rewrite the polished cinematic intro in Java (Nifty GUI is far less expressive for this kind of animation), both layers are kept and the Java engine has its own native intro sequence for the in-engine version. They are intentionally decoupled.
