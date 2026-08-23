# Contributing to Subject 47

Thank you for your interest in contributing! Subject 47 is a dual-layer project — a Java/jMonkeyEngine 3D game engine and a React/Three.js cinematic frontend. Please read this guide before submitting a pull request.

---

## Project Structure

```
Subject47/
├── game/       ← Java / jMonkeyEngine layer
├── frontend/   ← React / Vite / Three.js layer
├── assets/     ← Shared game assets
├── maps/       ← Level map & props files
└── docs/       ← Documentation
```

See [docs/architecture.md](docs/architecture.md) for a detailed breakdown.

---

## Setting Up Locally

### Frontend (React + Three.js)

**Prerequisites:** Node.js ≥ 18

```bash
cd frontend
npm install
cp ../.env.example ../.env.local   # add your GEMINI_API_KEY if needed
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

### Game Engine (Java + jMonkeyEngine)

**Prerequisites:** JDK 21

```bash
cd game
./gradlew run       # Linux/macOS
gradlew.bat run     # Windows
```

Or open `game/` as a Gradle project in IntelliJ IDEA.

---

## Code Style

### Java (game layer)
- Follow standard Java naming conventions (camelCase methods, PascalCase classes)
- Keep classes focused — one responsibility per class
- Add Javadoc to public methods in manager classes
- Encoding: UTF-8 (enforced via `build.gradle`)

### TypeScript / React (frontend layer)
- Functional components only — no class components
- Keep components in `src/components/ui/` if they're purely presentational
- No inline `style` props where a CSS class exists
- Run `npm run lint` before committing

---

## Workflow

1. **Fork** the repository
2. **Create a branch** from `main`:
   ```bash
   git checkout -b feature/my-feature
   # or
   git checkout -b fix/bug-description
   ```
3. **Make your changes** — keep commits small and descriptive
4. **Run checks:**
   ```bash
   # Frontend
   cd frontend && npm run lint

   # Java (build only, no JVM needed for CI)
   cd game && ./gradlew build
   ```
5. **Open a Pull Request** against `main` with a clear description of what changed and why

---

## Reporting Bugs

Open a GitHub Issue and include:
- Your OS and JDK/Node version
- Steps to reproduce
- Expected vs. actual behaviour
- Any relevant console output or logs

---

## Level Design Contributions

See [docs/level-format.md](docs/level-format.md) for the `.map` / `.props` file format specification.

---

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).
