package subject47.player;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import subject47.Main;
import subject47.interaction.Interactable;

public class PlayerController {

    private static final float PLAYER_RADIUS = 0.28f;
    private static final float EYE_HEIGHT = 1.55f;

    private final Main app;
    private final ActionListener actionListener;
    private final AnalogListener lookListener;
    private final Vector3f moveDirection = new Vector3f();
    private final Vector2f lookDelta = new Vector2f();

    private boolean enabled;
    private boolean lookHeld;
    private boolean forward;
    private boolean backward;
    private boolean left;
    private boolean right;
    private boolean flyUp;
    private boolean flyDown;
    private boolean flyMode;

    private float moveSpeed = 6f;
    private float lookSensitivity = 2.4f;

    public PlayerController(Main app) {
        this.app = app;
        this.actionListener = this::handleAction;
        this.lookListener = this::handleLook;
    }

    public void initialize() {
        app.getFlyByCamera().setEnabled(false);

        app.getInputManager().addMapping("Forward",  new KeyTrigger(KeyInput.KEY_W));
        app.getInputManager().addMapping("Backward", new KeyTrigger(KeyInput.KEY_S));
        app.getInputManager().addMapping("Left",     new KeyTrigger(KeyInput.KEY_A));
        app.getInputManager().addMapping("Right",    new KeyTrigger(KeyInput.KEY_D));
        app.getInputManager().addMapping("Interact", new KeyTrigger(KeyInput.KEY_E));
        app.getInputManager().addMapping("FlyToggle",new KeyTrigger(KeyInput.KEY_F));
        app.getInputManager().addMapping("FlyUp",    new KeyTrigger(KeyInput.KEY_SPACE));
        app.getInputManager().addMapping("FlyDown",  new KeyTrigger(KeyInput.KEY_LSHIFT));
        app.getInputManager().addMapping("LookHold", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addMapping("LookLeft",  new MouseAxisTrigger(MouseInput.AXIS_X, true));
        app.getInputManager().addMapping("LookRight", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        app.getInputManager().addMapping("LookUp",   new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        app.getInputManager().addMapping("LookDown",  new MouseAxisTrigger(MouseInput.AXIS_Y, true));

        app.getInputManager().addListener(actionListener,
                "Forward", "Backward", "Left", "Right", "Interact", "LookHold",
                "FlyToggle", "FlyUp", "FlyDown");
        app.getInputManager().addListener(lookListener,
                "LookLeft", "LookRight", "LookUp", "LookDown");
    }

    private void handleAction(String name, boolean isPressed, float tpf) {
        if ("Forward".equals(name)) {
            forward = isPressed;
        } else if ("Backward".equals(name)) {
            backward = isPressed;
        } else if ("Left".equals(name)) {
            left = isPressed;
        } else if ("Right".equals(name)) {
            right = isPressed;
        } else if ("FlyUp".equals(name)) {
            flyUp = isPressed;
        } else if ("FlyDown".equals(name)) {
            flyDown = isPressed;
        } else if ("FlyToggle".equals(name) && isPressed) {
            if (app.getEditor().isActive()) {
                flyMode = !flyMode;
                app.getUi().showMessage(flyMode ? "Fly mode ON  (F toggle, Space up, Shift down)" : "Fly mode OFF", 2f);
            }
        } else if ("LookHold".equals(name)) {
            if (!enabled || app.getUi().isOverlayBlockingInput()) {
                lookHeld = false;
                app.getInputManager().setCursorVisible(true);
            } else {
                lookHeld = isPressed;
                app.getInputManager().setCursorVisible(!isPressed);
            }
        } else if ("Interact".equals(name)) {
            if (isPressed) {
                interact();
            }
        }
    }

    private void handleLook(String name, float value, float tpf) {
        if (!enabled || !lookHeld || app.getUi().isOverlayBlockingInput()) {
            return;
        }

        if ("LookLeft".equals(name)) {
            lookDelta.x += value;
        } else if ("LookRight".equals(name)) {
            lookDelta.x -= value;
        } else if ("LookUp".equals(name)) {
            lookDelta.y += value;
        } else if ("LookDown".equals(name)) {
            lookDelta.y -= value;
        }
    }

    public void update(float tpf) {
        if (!enabled) {
            app.getUi().setPrompt(null);
            return;
        }

        updateLook();

        if (!app.getUi().isOverlayBlockingInput()) {
            updateMovement(tpf);
            updatePrompt();
        } else {
            app.getUi().setPrompt(null);
        }
    }

    private void updateLook() {
        if (lookDelta.lengthSquared() == 0f) {
            return;
        }

        float yaw = lookDelta.x * lookSensitivity;
        float pitch = lookDelta.y * lookSensitivity;
        Vector3f direction = app.getCamera().getDirection().clone();
        Vector3f leftAxis = app.getCamera().getLeft().clone().normalizeLocal();

        direction = rotate(direction, Vector3f.UNIT_Y, yaw);
        Vector3f rotated = rotate(direction, leftAxis, pitch).normalizeLocal();

        if (Math.abs(rotated.y) < 0.98f) {
            direction = rotated;
        }

        app.getCamera().lookAtDirection(direction.normalizeLocal(), Vector3f.UNIT_Y);
        lookDelta.set(0f, 0f);
    }

    private void updateMovement(float tpf) {
        moveDirection.set(0f, 0f, 0f);

        if (flyMode) {
            // --- Fly movement: follow full 3D camera direction ---
            float speed = moveSpeed * 1.8f;
            Vector3f camDir  = app.getCamera().getDirection().normalize();
            Vector3f camLeft = app.getCamera().getLeft().normalize();

            if (forward)  moveDirection.addLocal(camDir);
            if (backward) moveDirection.subtractLocal(camDir);
            if (left)     moveDirection.addLocal(camLeft);
            if (right)    moveDirection.subtractLocal(camLeft);
            if (flyUp)    moveDirection.addLocal(0f, 1f, 0f);
            if (flyDown)  moveDirection.addLocal(0f, -1f, 0f);

            if (moveDirection.lengthSquared() > 0f) {
                moveDirection.normalizeLocal().multLocal(speed * tpf);
            }
            // No collision — just move freely
            app.getCamera().setLocation(app.getCamera().getLocation().add(moveDirection));
            return;
        }

        // --- Normal grounded movement ---
        Vector3f forwardDirection = app.getCamera().getDirection().clone();
        forwardDirection.y = 0f;
        if (forwardDirection.lengthSquared() == 0f) {
            forwardDirection.set(0f, 0f, -1f);
        } else {
            forwardDirection.normalizeLocal();
        }

        Vector3f leftDirection = app.getCamera().getLeft().clone();
        leftDirection.y = 0f;
        if (leftDirection.lengthSquared() == 0f) {
            leftDirection.set(-1f, 0f, 0f);
        } else {
            leftDirection.normalizeLocal();
        }

        if (forward) {
            moveDirection.addLocal(forwardDirection);
        }
        if (backward) {
            moveDirection.subtractLocal(forwardDirection);
        }
        if (left) {
            moveDirection.addLocal(leftDirection);
        }
        if (right) {
            moveDirection.subtractLocal(leftDirection);
        }

        if (moveDirection.lengthSquared() > 0f) {
            moveDirection.normalizeLocal().multLocal(moveSpeed * tpf);
            Vector3f currentLocation = app.getCamera().getLocation().clone();
            Vector3f nextLocation = currentLocation.clone();

            if (moveDirection.x != 0f) {
                Vector3f xMove = nextLocation.add(moveDirection.x, 0f, 0f);
                if (app.getWorld().isWalkable(xMove, PLAYER_RADIUS, EYE_HEIGHT)) {
                    nextLocation.x = xMove.x;
                }
            }

            if (moveDirection.z != 0f) {
                Vector3f zMove = nextLocation.add(0f, 0f, moveDirection.z);
                if (app.getWorld().isWalkable(zMove, PLAYER_RADIUS, EYE_HEIGHT)) {
                    nextLocation.z = zMove.z;
                }
            }

            app.getCamera().setLocation(nextLocation);
        }
    }

    private void updatePrompt() {
        CollisionResults results = new CollisionResults();
        Ray ray = new Ray(app.getCamera().getLocation(), app.getCamera().getDirection());
        app.getRootNode().collideWith(ray, results);

        for (CollisionResult result : results) {
            if (result.getDistance() > 4f) {
                break;
            }

            Interactable interactable = app.getWorld().getInteractable(result.getGeometry());
            if (interactable != null) {
                app.getUi().setPrompt(interactable.getPrompt());
                return;
            }
        }

        app.getUi().setPrompt(null);
    }

    private void interact() {
        if (!enabled || app.getUi().isOverlayBlockingInput()) {
            return;
        }

        CollisionResults results = new CollisionResults();
        Ray ray = new Ray(app.getCamera().getLocation(), app.getCamera().getDirection());
        app.getRootNode().collideWith(ray, results);

        for (CollisionResult result : results) {
            if (result.getDistance() > 4f) {
                break;
            }

            Interactable interactable = app.getWorld().getInteractable(result.getGeometry());
            if (interactable != null) {
                interactable.interact();
                return;
            }
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.lookHeld = false;
        this.forward = false;
        this.backward = false;
        this.left = false;
        this.right = false;
        this.flyUp = false;
        this.flyDown = false;
        app.getInputManager().setCursorVisible(true);
    }

    public boolean isFlyMode() {
        return flyMode;
    }

    public void setFlyMode(boolean fly) {
        this.flyMode = fly;
    }

    public void placeAt(Vector3f location, Vector3f lookAt) {
        app.getCamera().setLocation(location.clone());
        app.getCamera().lookAt(lookAt, Vector3f.UNIT_Y);
    }

    private Vector3f rotate(Vector3f vector, Vector3f axis, float angle) {
        return new Quaternion().fromAngleAxis(angle, axis).mult(vector);
    }
}
