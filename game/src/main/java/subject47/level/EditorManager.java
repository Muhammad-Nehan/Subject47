package subject47.level;

import java.util.ArrayDeque;
import java.util.Deque;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import subject47.Main;
import subject47.world.BlockType;
import subject47.world.EditorPropType;
import subject47.world.WorldManager;

public class EditorManager {

    private static final BlockType[] EDITABLE_TYPES = {
            BlockType.FLOOR,
            BlockType.WALL,
            BlockType.GLASS,
            BlockType.LIGHT,
            BlockType.CEILING,
            BlockType.TRIM,
            BlockType.PANEL,
            BlockType.AIR
    };
    private static final EditorPropType[] EDITABLE_PROPS = {
            EditorPropType.AUTO_DOOR,
            EditorPropType.KEYPAD_DOOR,
            EditorPropType.CONSOLE,
            EditorPropType.LAB_BENCH,
            EditorPropType.SERVER_RACK,
            EditorPropType.STORAGE_CRATE,
            EditorPropType.MEDICAL_CART,
            EditorPropType.WALL_MONITOR,
            EditorPropType.LOCKER_BANK,
            EditorPropType.CEILING_LIGHT,
            EditorPropType.ANALYZER_STATION,
            EditorPropType.TANK_CLUSTER,
            EditorPropType.CONTAINMENT_POD
    };

    private final Main app;
    private final ActionListener actionListener;
    private final Deque<EditAction> undoStack = new ArrayDeque<>();

    private boolean active;
    private int selectedIndex;
    private int selectedPropIndex;
    private boolean propMode;
    private int propRotation; // 0-3, each step = 90° around Y

    public EditorManager(Main app) {
        this.app = app;
        this.actionListener = this::handleAction;
    }

    public void initialize() {
        app.getInputManager().addMapping("EditorToggle",  new KeyTrigger(KeyInput.KEY_F1));
        app.getInputManager().addMapping("EditorPrev",    new KeyTrigger(KeyInput.KEY_Z));
        app.getInputManager().addMapping("EditorNext",    new KeyTrigger(KeyInput.KEY_X));
        app.getInputManager().addMapping("EditorPlace",   new KeyTrigger(KeyInput.KEY_Q));
        app.getInputManager().addMapping("EditorDelete",  new KeyTrigger(KeyInput.KEY_R));
        app.getInputManager().addMapping("EditorUndo",    new KeyTrigger(KeyInput.KEY_U));
        app.getInputManager().addMapping("EditorMode",    new KeyTrigger(KeyInput.KEY_V));
        app.getInputManager().addMapping("EditorSave",    new KeyTrigger(KeyInput.KEY_F5));
        app.getInputManager().addMapping("EditorRotate",  new KeyTrigger(KeyInput.KEY_T));
        app.getInputManager().addListener(
                actionListener,
                "EditorToggle", "EditorPrev", "EditorNext", "EditorPlace",
                "EditorDelete", "EditorUndo", "EditorMode", "EditorSave", "EditorRotate"
        );
    }

    public void update(float tpf) {
        if (!active) {
            app.getUi().setEditorOverlay(null);
            app.getWorld().clearEditorGhost();
            return;
        }

        WorldManager.BlockHit hit = app.getWorld().findBlockHit(
                app.getCamera().getLocation(), app.getCamera().getDirection(), 8f);
        int px = 0, py = 0, pz = 0;
        if (hit != null) {
            px = hit.x() + Math.round(hit.normal().x);
            py = hit.y() + Math.round(hit.normal().y);
            pz = hit.z() + Math.round(hit.normal().z);
        }
        String target = hit == null ? "none" : px + "," + py + "," + pz;
        String rotLabel = propMode ? "  rot=" + (propRotation * 90) + "°" : "";
        app.getUi().setEditorOverlay(
                "EDITOR  F1 exit  F fly  V mode  Z/X cycle  T rotate  Q place  R delete  U undo  F5 save\n" +
                (propMode ? "Prop " + getSelectedPropType() : "Block " + getSelectedType()) +
                rotLabel + "  Place:" + target + "  Undo:" + undoStack.size() +
                (app.getPlayer().isFlyMode() ? "  [FLY]" : "")
        );

        if (propMode) {
            app.getWorld().updateEditorGhost(
                    getSelectedPropType(), hit, app.getCamera().getDirection(), propRotation);
        } else {
            app.getWorld().clearEditorGhost();
        }
    }

    public boolean isActive() {
        return active;
    }

    private void handleAction(String name, boolean pressed, float tpf) {
        if (!pressed) {
            return;
        }

        switch (name) {
            case "EditorToggle" -> toggle();
            case "EditorPrev" -> {
                if (active) {
                    if (propMode) {
                        selectedPropIndex = (selectedPropIndex + EDITABLE_PROPS.length - 1) % EDITABLE_PROPS.length;
                    } else {
                        selectedIndex = (selectedIndex + EDITABLE_TYPES.length - 1) % EDITABLE_TYPES.length;
                    }
                }
            }
            case "EditorNext" -> {
                if (active) {
                    if (propMode) {
                        selectedPropIndex = (selectedPropIndex + 1) % EDITABLE_PROPS.length;
                    } else {
                        selectedIndex = (selectedIndex + 1) % EDITABLE_TYPES.length;
                    }
                }
            }
            case "EditorMode" -> {
                if (active) {
                    propMode = !propMode;
                    propRotation = 0;
                    app.getWorld().clearEditorGhost();
                }
            }
            case "EditorRotate" -> {
                if (active && propMode) {
                    propRotation = (propRotation + 1) % 4;
                }
            }
            case "EditorPlace" -> {
                if (active) {
                    if (propMode) {
                        placeProp();
                    } else {
                        placeBlock();
                    }
                }
            }
            case "EditorDelete" -> {
                if (active) {
                    if (propMode) {
                        deleteProp();
                    } else {
                        deleteBlock();
                    }
                }
            }
            case "EditorUndo" -> {
                if (active) {
                    undo();
                }
            }
            case "EditorSave" -> {
                if (active) {
                    app.getWorld().saveCurrentChunk(app.getLevelManager().getCurrentLevel());
                    app.getWorld().saveEditorProps(app.getLevelManager().getCurrentLevel());
                    app.getUi().showMessage("Editor map saved for level " + app.getLevelManager().getCurrentLevel() + ".", 2f);
                }
            }
            default -> {
            }
        }
    }

    private void toggle() {
        active = !active;
        app.getUi().showMessage(active
                ? "Editor enabled. F to fly, V switches block/prop mode."
                : "Editor disabled.", 2f);
        if (!active) {
            app.getUi().setEditorOverlay(null);
            app.getWorld().clearEditorGhost();
            app.getPlayer().setFlyMode(false);  // land when leaving editor
        }
    }

    private void placeBlock() {
        WorldManager.BlockHit hit = app.getWorld().findBlockHit(
                app.getCamera().getLocation(), app.getCamera().getDirection(), 8f);
        BlockType type = getSelectedType();

        int x, y, z;

        if (hit == null) {
            // No surface hit — place 3 m in front at floor level
            Vector3f point = app.getCamera().getLocation().add(
                    app.getCamera().getDirection().mult(3f));
            x = app.getWorld().toChunkIndex(point.x);
            y = app.getWorld().toChunkIndex(Math.max(0.6f, point.y - 1.2f));
            z = app.getWorld().toChunkIndex(point.z);
        } else {
            // JME collision normals point OUTWARD (toward viewer).
            // Adding the normal moves us INTO the adjacent air cell — correct placement.
            x = hit.x() + Math.round(hit.normal().x);
            y = hit.y() + Math.round(hit.normal().y);
            z = hit.z() + Math.round(hit.normal().z);
        }

        BlockType previous = app.getWorld().getBlock(x, y, z);
        if (previous == type) {
            return;
        }
        app.getWorld().setBlock(x, y, z, type);
        pushUndo(x, y, z, previous, type);
    }

    private void deleteBlock() {
        WorldManager.BlockHit hit = app.getWorld().findBlockHit(app.getCamera().getLocation(), app.getCamera().getDirection(), 8f);
        if (hit == null) {
            return;
        }
        BlockType previous = app.getWorld().getBlock(hit.x(), hit.y(), hit.z());
        if (previous == BlockType.AIR) {
            return;
        }
        app.getWorld().setBlock(hit.x(), hit.y(), hit.z(), BlockType.AIR);
        pushUndo(hit.x(), hit.y(), hit.z(), previous, BlockType.AIR);
    }

    private void placeProp() {
        WorldManager.BlockHit hit = app.getWorld().findBlockHit(
                app.getCamera().getLocation(), app.getCamera().getDirection(), 8f);
        if (hit == null) {
            app.getUi().showMessage("Aim at a surface to place a prop.", 1.5f);
            return;
        }
        app.getWorld().placeEditorProp(getSelectedPropType(), hit, app.getCamera().getDirection(), propRotation);
    }

    private void deleteProp() {
        String hit = app.getWorld().findAnyPropHit(app.getCamera().getLocation(), app.getCamera().getDirection(), 8f);
        if (hit == null) {
            app.getUi().showMessage("No prop targeted.", 1.5f);
            return;
        }
        // hit is "editor:<id>" or "level:<id>"
        int colon = hit.indexOf(':');
        String kind = hit.substring(0, colon);
        String propId = hit.substring(colon + 1);
        boolean removed;
        if ("level".equals(kind)) {
            removed = app.getWorld().removeLevelProp(propId);
        } else {
            removed = app.getWorld().removeEditorProp(propId);
        }
        if (removed) {
            app.getUi().showMessage("Prop removed.", 1.2f);
        } else {
            app.getUi().showMessage("Failed to remove prop.", 1.5f);
        }
    }

    private BlockType getSelectedType() {
        return EDITABLE_TYPES[selectedIndex];
    }

    private EditorPropType getSelectedPropType() {
        return EDITABLE_PROPS[selectedPropIndex];
    }

    private void undo() {
        EditAction action = undoStack.pollLast();
        if (action == null) {
            app.getUi().showMessage("Nothing to undo.", 1.5f);
            return;
        }
        app.getWorld().setBlock(action.x(), action.y(), action.z(), action.before());
        app.getUi().showMessage("Undo applied.", 1.2f);
    }

    private void pushUndo(int x, int y, int z, BlockType before, BlockType after) {
        undoStack.addLast(new EditAction(x, y, z, before, after));
        while (undoStack.size() > 200) {
            undoStack.pollFirst();
        }
    }

    private record EditAction(int x, int y, int z, BlockType before, BlockType after) {
    }
}
