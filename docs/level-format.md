# Level File Format

Subject 47 levels are defined by two files per level, both stored in `maps/`:

- `level-N.map` — the block/voxel layout of the world
- `level-N.props` — interactable object placement (terminals, doors, keypads, switches)

---

## `.map` File

The `.map` file encodes the 3D voxel grid of a level. Each value corresponds to a `BlockType` enum constant defined in `game/src/main/java/subject47/world/BlockType.java`.

### Format

Binary or text-based grid of block type IDs, read by `WorldManager.java` chunk by chunk. Each chunk is 16×16×16 blocks.

### BlockType Values

| ID | Name | Description |
|---|---|---|
| `0` | `VOID` | Empty / air |
| `1` | `FLOOR` | Standard floor tile |
| `2` | `WALL` | Wall block |
| `3` | `CEILING` | Ceiling block |
| `4` | `LIGHT` | Emissive ceiling light |
| `5` | `GLASS` | Transparent/semi-transparent block |

---

## `.props` File

The `.props` file places interactable objects into the level. Each entry is a line defining the prop type and its world-space position.

### Format

```
PROP_TYPE  x  y  z  [optional parameters]
```

### Prop Types

Defined by `EditorPropType.java`:

| Type | Description | Optional Parameters |
|---|---|---|
| `TERMINAL` | Readable log terminal | `narrative_id` — links to a narrative sequence |
| `DOOR` | Animated door | `locked=true/false`, `switch_id` |
| `KEYPAD` | Numeric keypad | `code=XXXX`, `door_id` |
| `SWITCH` | Toggle switch | `door_id` — ID of door to control |

### Example `.props`

```
TERMINAL  12  0  -8   narrative_id=lab_log_01
DOOR      0   0  -16  locked=true  switch_id=sw_01
SWITCH    -5  1  -14  door_id=door_main
KEYPAD    3   1  -20  code=4719  door_id=door_secondary
```

---

## Adding a New Level

1. Create `maps/level-N.map` with your block layout
2. Create `maps/level-N.props` with your interactable placements
3. Register the level in `LevelManager.java` by adding it to the level sequence
4. Add narrative content to `UIManager.java` if new terminal texts are needed

---

## Level Editor

The in-engine level editor can be toggled with **F3** during gameplay. It allows you to place, move, and inspect props visually, with changes saved back to the `.props` file via `EditorManager.java`.
