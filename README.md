<div align="center">


 SUBJECT 47

**A Psychological Horror Experience**

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![jMonkeyEngine](https://img.shields.io/badge/jMonkeyEngine-3.6.1-blue?style=flat-square)](https://jmonkeyengine.org/)
[![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react)](https://react.dev/)
[![Three.js](https://img.shields.io/badge/Three.js-r184-black?style=flat-square&logo=three.js)](https://threejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.8-3178C6?style=flat-square&logo=typescript)](https://www.typescriptlang.org/)
[![Build](https://img.shields.io/badge/build-passing-brightgreen?style=flat-square)]()
[![License](https://img.shields.io/badge/license-MIT-green?style=flat-square)](LICENSE)

*LAB 04: CLASSIFIED. MOST SUBJECTS TERMINATED. SUBJECT 47: BIOLOGICAL STABILITY ATTAINED.*

</div>

---

##  Overview

**Subject 47** is a first-person psychological horror game set in an abandoned underground research facility. You wake up with no memory as a "repair technician" — unaware that you are the only surviving test subject of a classified human experimentation program. Navigate dark corridors, decrypt terminals, solve puzzles, and piece together the horrifying truth behind Protocol 47.

The project consists of two independent layers:

| Layer | Technology | Purpose |
|---|---|---|
| **3D Game Engine** | Java + jMonkeyEngine 3 | Core gameplay, level logic, player controller, AI, audio |
| **Cinematic Frontend** | React + Three.js + Vite | Animated intro cutscene & main menu shell |

---

##  Features

-  **Atmospheric voxel-art environments** — dark, brutalist corridors built from modular chunks
-  **Cinematic intro sequence** — a 20-second scripted camera flythrough with glitch subtitles and post-processing
-  **Procedural audio** — ambient drone synthesized via Web Audio API + tuned to the "Subject 47 frequency" (47 Hz)
-  **Interactive terminals & keypads** — in-world interactables that trigger narrative story beats
-  **Door & switch puzzles** — control switches, keycodes, and event-driven door logic across 4 levels
-  **Multi-page narrative modals** — cinematic story pages triggered by terminal interactions
-  **Glitch UI effects** — scanlines, chromatic aberration, film grain, hex-grid overlays, pulsing rings
-  **4 hand-crafted levels** — each with unique layout, props, and narrative events
-  **Built-in level editor** — `EditorManager` for placing and inspecting world props in-engine

---

##  Project Structure

```
Subject47/
├── src/
│   ├── App.tsx                     # React root — main menu & cinematic controller
│   ├── main.tsx                    # Vite entry point
│   ├── index.css                   # Global styles, animations, glitch effects
│   ├── components/
│   │   ├── VoxelScene.tsx          # Three.js voxel lab scene (React Three Fiber)
│   │   └── CinematicCamera.tsx     # Scripted camera path interpolation
│   ├── lib/
│   │   └── AudioEngine.ts          # Web Audio API — ambient drone + MP3 intro
│   └── main/
│       ├── java/subject47/
│       │   ├── Main.java           # jMonkeyEngine application entry point
│       │   ├── audio/
│       │   │   └── AudioManager.java     # In-world OGG/MP3 audio manager
│       │   ├── events/
│       │   │   └── EventManager.java     # Game event bus
│       │   ├── interaction/
│       │   │   ├── Interactable.java     # Base interactable interface
│       │   │   ├── Terminal.java         # Readable terminal props
│       │   │   ├── Keypad.java           # Code-entry keypads
│       │   │   ├── ControlSwitch.java    # Toggle switches
│       │   │   └── Door.java             # Animated doors
│       │   ├── level/
│       │   │   ├── LevelManager.java     # Level loading & transition logic
│       │   │   ├── EditorManager.java    # In-engine level editor
│       │   │   └── CameraSystem.java     # First-person camera rig
│       │   ├── player/
│       │   │   └── PlayerController.java # Movement, collision, input
│       │   ├── ui/
│       │   │   ├── UIManager.java        # HUD & narrative overlay controller
│       │   │   ├── MainMenu.java         # jME main menu screen
│       │   │   └── IntroSequence.java    # In-engine intro cutscene
│       │   └── world/
│       │       ├── WorldManager.java     # Chunk/block world rendering
│       │       ├── LabGenerator.java     # Procedural lab corridor generator
│       │       ├── Chunk.java            # Voxel chunk data structure
│       │       ├── BlockType.java        # Block type enum
│       │       └── EditorPropType.java   # Editor prop enum
│       └── resources/
├── assets/
│   ├── Audio/                      # Game music & SFX (MP3/OGG)
│   ├── Interface/                  # UI textures and icons
│   ├── Materials/                  # jME material definitions
│   ├── MatDefs/                    # Custom shader material definitions
│   ├── Models/                     # 3D models (J3O/OBJ)
│   ├── Scenes/                     # jME scene files (J3O)
│   ├── Shaders/                    # GLSL vertex/fragment shaders
│   ├── Sounds/                     # Ambient and event sounds
│   └── Textures/                   # Diffuse, normal, emissive maps
├── maps/
│   ├── level-1.map / level-1.props # Level 1 layout & props
│   ├── level-2.map / level-2.props # Level 2 layout & props
│   ├── level-3.map / level-3.props # Level 3 layout & props
│   └── level-4.map / level-4.props # Level 4 layout & props
├── build.gradle                    # Gradle build for Java/jME layer
├── settings.gradle
├── package.json                    # npm config for React/Vite layer
├── vite.config.ts
└── tsconfig.json
```

---

##  Getting Started

### Prerequisites

| Tool | Version | Purpose |
|---|---|---|
| **Node.js** | ≥ 18 | React/Vite frontend |
| **JDK** | 21 | Java game engine |
| **Gradle** | via wrapper (`./gradlew`) | Java build |

---

###  Running the Cinematic Frontend (React + Three.js)

This layer provides the animated main menu and intro cutscene in a browser.

```bash
# 1. Install dependencies
npm install

# 2. Copy the environment file and add your API key (if using Gemini features)
cp .env.example .env.local
# Edit .env.local and set GEMINI_API_KEY=your_key_here

# 3. Start the dev server
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

> **Note:** The `GEMINI_API_KEY` is optional for the base game. It is only required if AI-driven features are enabled.

---

###  Running the Java Game Engine (jMonkeyEngine)

This layer runs the full 3D game with gameplay, levels, and narrative.

```bash
# Build and run via Gradle wrapper
./gradlew run          # Linux / macOS
gradlew.bat run        # Windows
```

Or open the project as a **Gradle project** in IntelliJ IDEA or VS Code (Java Extension Pack) and run `Main.java`.

---

###  Building for Production

```bash
# Frontend (React)
npm run build          # Outputs to /dist

# Java game (fat JAR)
./gradlew build        # Outputs to /build/libs/
```

---

##  Controls

| Key | Action |
|---|---|
| `W A S D` | Move |
| `Mouse` | Look around |
| `E` | Interact (terminals, switches, keypads, doors) |
| `Esc` | Pause / Main Menu |
| `Tab` | Toggle HUD |

---

##  Levels

| Level | Theme | Key Mechanic |
|---|---|---|
| **Level 1** | Entry Corridor | Orientation, first terminal |
| **Level 2** | Research Wing | Keypad puzzles, data logs |
| **Level 3** | Containment Zone | Terminal narrative sequences |
| **Level 4** | Core Sector | Multi-stage story reveal, final escape |

---

##  Tech Stack

### Java Game Layer
- **[jMonkeyEngine 3.6.1](https://jmonkeyengine.org/)** — 3D game engine
- **LWJGL 3** — native OpenGL/audio bindings
- **JLayer (jlayer 1.0.1)** — pure-Java MP3 playback
- **SLF4J** — logging

### React Frontend Layer
- **[React 19](https://react.dev/)** + **[TypeScript 5.8](https://www.typescriptlang.org/)**
- **[Vite 6](https://vitejs.dev/)** — build tooling
- **[Three.js r184](https://threejs.org/)** + **[@react-three/fiber](https://docs.pmnd.rs/react-three-fiber)**
- **[@react-three/drei](https://drei.pmnd.rs/)** — Three.js helpers
- **[@react-three/postprocessing](https://github.com/pmndrs/react-postprocessing)** — Bloom, Vignette, Chromatic Aberration, Scanline
- **[Motion (Framer Motion)](https://motion.dev/)** — UI animations
- **[Lucide React](https://lucide.dev/)** — icons
- **[Tailwind CSS 4](https://tailwindcss.com/)** — utility styling

---

##  Configuration

Environment variables (copy `.env.example` → `.env.local`):

| Variable | Description |
|---|---|
| `GEMINI_API_KEY` | Google Gemini API key (optional — for AI features) |
| `APP_URL` | Deployed app URL (for OAuth callbacks, etc.) |

---

##  Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you'd like to change.

1. Fork the repository
2. Create your feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m 'feat: add my feature'`
4. Push to the branch: `git push origin feature/my-feature`
5. Open a Pull Request

---

##  License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

*BUILD 0.1.0-ALPHA · CLEARANCE LEVEL: OMEGA*

</div>
