package subject47.interaction;

import com.jme3.scene.Node;
import com.jme3.math.Vector3f;
import subject47.Main;

public class Door implements Interactable {

    private final Main app;
    private final Node spatial;
    private final Vector3f closedPosition;
    private final Vector3f openPosition;
    private final String name;

    private boolean locked;
    private boolean open;
    private float animationSpeed = 4.6f;
    private Runnable onLockedCallback;

    public Door(Main app, Node spatial, String name, Vector3f closedPosition, Vector3f openOffset, boolean locked) {
        this.app = app;
        this.spatial = spatial;
        this.name = name;
        this.closedPosition = closedPosition.clone();
        this.openPosition = closedPosition.add(openOffset);
        this.locked = locked;
        this.spatial.setLocalTranslation(closedPosition);
    }

    @Override
    public void interact() {
        if (locked) {
            if (onLockedCallback != null) {
                onLockedCallback.run();
            } else {
                app.getUi().showMessage("A red door control is mounted nearby for " + name + ".", 2f);
            }
        } else {
            toggleFromButton(null);
        }
    }

    public void setOnLockedCallback(Runnable callback) {
        this.onLockedCallback = callback;
    }

    @Override
    public String getPrompt() {
        return locked ? "Press E to inspect " + name : "Press E to use " + name;
    }

    public void unlock(String message) {
        locked = false;
        app.getUi().showMessage(message, 3f);
    }

    public void lock() {
        locked = true;
        setOpen(false);
    }

    public void openFromEvent(String message) {
        locked = false;
        setOpen(true);
        app.getUi().showMessage(message, 3f);
    }

    public void toggleFromButton(String message) {
        if (locked) {
            app.getUi().showMessage(name + " is still locked.", 2f);
            return;
        }
        setOpen(!open);
        if (message != null && !message.isBlank()) {
            app.getUi().showMessage(message, 2f);
        }
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public void update(float tpf) {
        Vector3f target = open ? openPosition : closedPosition;
        Vector3f current = spatial.getLocalTranslation();
        if (current.distanceSquared(target) < 0.0004f) {
            spatial.setLocalTranslation(target);
            return;
        }
        spatial.setLocalTranslation(current.interpolateLocal(target, Math.min(1f, tpf * animationSpeed)));
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean blocksPosition(Vector3f point, float radius) {
        Vector3f current = spatial.getLocalTranslation();
        float halfWidth = 0.78f + radius;
        float halfDepth = 0.24f + radius;
        float bottom = current.y - 1.55f;
        float top = current.y + 1.55f;

        if (point.y < bottom || point.y > top) {
            return false;
        }

        return Math.abs(point.x - current.x) <= halfWidth
                && Math.abs(point.z - current.z) <= halfDepth;
    }

    public Node getSpatial() {
        return spatial;
    }
}
