package subject47.world;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import subject47.Main;
import subject47.interaction.ControlSwitch;
import subject47.interaction.Door;
import subject47.interaction.Interactable;
import subject47.interaction.Keypad;

public class WorldManager {

    private static final String INTERACTABLE_ID = "interactableId";
    private static final String EDITOR_PROP_ID = "editorPropId";
    private static final String LEVEL_PROP_ID  = "levelPropId";
    private static final Path MAP_DIRECTORY = Paths.get("maps");

    private final Main app;
    private final Node worldNode = new Node("World");
    private final Node propNode = new Node("Props");
    private final Node ghostNode = new Node("Ghost");
    private final LabGenerator generator = new LabGenerator();
    private final List<Material> lightMaterials = new ArrayList<>();
    private final Map<String, Interactable> interactables = new HashMap<>();
    private final List<Door> animatedDoors = new ArrayList<>();
    private final Map<String, EditorPropInstance> editorProps = new HashMap<>();
    private final Map<String, Spatial> levelProps = new HashMap<>();

    /** Ghost preview material (cyan, semi-transparent). Created once. */
    private Material ghostMaterial;

    /**
     * PC desk model parsed once from disk.  Volatile so the background preload
     * thread's write is visible to the render thread without synchronisation.
     */
    private volatile Spatial cachedPcModel;
    private Thread pcModelPreloadThread;

    private Chunk currentChunk;
    private int currentLevel;
    private float lightFlickerTime;
    private boolean emergencyLighting;
    private int nextInteractableId;
    private int nextEditorPropId;
    private boolean savedPropsLoaded;

    public WorldManager(Main app) {
        this.app = app;
    }

    public void initialize() {
        app.getRootNode().attachChild(worldNode);
        app.getRootNode().attachChild(propNode);
        app.getRootNode().attachChild(ghostNode);
        // Register the assets/ folder so assetManager.loadModel() can find
        // OBJ/FBX files placed there (by default it only searches the classpath).
        app.getAssetManager().registerLocator("assets/", com.jme3.asset.plugins.FileLocator.class);
        // Parse the heavy OBJ on a background thread while the main menu is
        // visible, so Level 1 loads instantly when the player starts the game.
        pcModelPreloadThread = new Thread(() -> {
            try {
                Spatial model = app.getAssetManager().loadModel("Pc/pc_desk.obj");
                applyUniformMaterial(model,
                        coloredMaterial(new ColorRGBA(0.13f, 0.15f, 0.19f, 1f), false),
                        coloredMaterial(new ColorRGBA(0.22f, 0.25f, 0.30f, 1f), false));
                cachedPcModel = model;
                System.out.println("[WorldManager] PC model preloaded.");
            } catch (Exception e) {
                System.err.println("[WorldManager] PC model preload failed: " + e.getMessage());
            }
        }, "PC-Model-Preload");
        pcModelPreloadThread.setDaemon(true);
        pcModelPreloadThread.start();
    }

    public void loadLevel(int level) {
        currentLevel = level;
        propNode.detachAllChildren();
        worldNode.detachAllChildren();
        ghostNode.detachAllChildren();
        lightMaterials.clear();
        interactables.clear();
        animatedDoors.clear();
        editorProps.clear();
        levelProps.clear();
        nextInteractableId = 0;
        nextEditorPropId = 0;
        emergencyLighting = level >= 3;

        currentChunk = new Chunk();

        if (!loadSavedChunk(level)) {
            switch (level) {
                case 1 -> generator.generateLevel1(currentChunk);
                case 2 -> generator.generateLevel2(currentChunk);
                case 3 -> generator.generateLevel3(currentChunk);
                case 4 -> generator.generateLevel4(currentChunk);
                default -> generator.generateLevel1(currentChunk);
            }
        }

        renderChunk();
        // loadSavedProps returns true when a saved props file was found.
        // In that case, level decoration is skipped so the saved state is authoritative.
        savedPropsLoaded = loadSavedProps(level);
    }

    public void update(float tpf) {
        if (lightFlickerTime > 0f) {
            lightFlickerTime -= tpf;
            float scale = ((int) (lightFlickerTime * 18f) % 2 == 0) ? 0.12f : 1f;
            applyLightState(scale);
            if (lightFlickerTime <= 0f) {
                applyLightState(1f);
            }
        }

        for (Door door : animatedDoors) {
            door.update(tpf);
        }
    }

    public void flickerLights(float duration) {
        lightFlickerTime = Math.max(lightFlickerTime, duration);
    }

    public void setEmergencyLighting(boolean enabled) {
        emergencyLighting = enabled;
        applyLightState(1f);
    }

    public Door createDoor(String name, Vector3f position, boolean locked) {
        Node doorNode = new Node(name);

        Geometry frame = new Geometry(name + "Frame", new Box(0.86f, 1.62f, 0.18f));
        frame.setMaterial(coloredMaterial(new ColorRGBA(0.05f, 0.08f, 0.1f, 1f), false));
        frame.setLocalTranslation(0f, 0f, -0.08f);
        doorNode.attachChild(frame);

        Geometry lintel = new Geometry(name + "Lintel", new Box(0.72f, 0.16f, 0.08f));
        lintel.setMaterial(coloredMaterial(new ColorRGBA(0.7f, 0.08f, 0.08f, 1f), true));
        lintel.setLocalTranslation(0f, 1.32f, 0.06f);
        doorNode.attachChild(lintel);

        Geometry panelLeft = new Geometry(name + "PanelLeft", new Box(0.28f, 1.32f, 0.07f));
        panelLeft.setMaterial(coloredMaterial(new ColorRGBA(0.32f, 0.36f, 0.42f, 1f), false));
        panelLeft.setLocalTranslation(-0.3f, 0f, 0.08f);
        doorNode.attachChild(panelLeft);

        Geometry panelRight = new Geometry(name + "PanelRight", new Box(0.28f, 1.32f, 0.07f));
        panelRight.setMaterial(coloredMaterial(new ColorRGBA(0.32f, 0.36f, 0.42f, 1f), false));
        panelRight.setLocalTranslation(0.3f, 0f, 0.08f);
        doorNode.attachChild(panelRight);

        Geometry seam = new Geometry(name + "Seam", new Box(0.03f, 1.12f, 0.075f));
        seam.setMaterial(coloredMaterial(new ColorRGBA(0.12f, 0.14f, 0.16f, 1f), false));
        seam.setLocalTranslation(0f, -0.05f, 0.09f);
        doorNode.attachChild(seam);

        Door door = new Door(app, doorNode, name, position, new Vector3f(0f, 2.8f, 0f), locked);
        tagSpatial(doorNode, door);
        animatedDoors.add(door);
        propNode.attachChild(doorNode);
        return door;
    }

    public Spatial createConsole(String name, Vector3f position, ColorRGBA color, Interactable interactable) {
        Node console = new Node(name);

        Geometry base = new Geometry(name + "Base", new Box(0.6f, 0.8f, 0.4f));
        base.setMaterial(coloredMaterial(new ColorRGBA(0.1f, 0.12f, 0.14f, 1f), false));
        console.attachChild(base);

        Geometry screen = new Geometry(name + "Screen", new Box(0.48f, 0.35f, 0.03f));
        screen.setMaterial(coloredMaterial(color, true));
        screen.setLocalTranslation(0f, 0.35f, 0.43f);
        console.attachChild(screen);

        console.setLocalTranslation(position);
        tagSpatial(console, interactable);
        propNode.attachChild(console);
        return console;
    }

    public Spatial createAudioLogPickup(String name, Vector3f position, ColorRGBA color, Interactable interactable) {
        Geometry pickup = new Geometry(name, new Box(0.24f, 0.12f, 0.24f));
        pickup.setMaterial(coloredMaterial(color, true));
        pickup.setLocalTranslation(position);
        tagSpatial(pickup, interactable);
        propNode.attachChild(pickup);
        return pickup;
    }

    /**
     * Loads the PC desk 3-D model from assets/Pc/ and places it at the given
     * position with the supplied interactable wired up (same contract as
     * {@link #createAudioLogPickup}).  Falls back to a coloured box if the
     * model cannot be loaded so the rest of the level still works.
     */
    public Spatial createPcModel(String name, Vector3f position, ColorRGBA fallbackColor, Interactable interactable) {
        final String MODEL_PATH = "Pc/pc_desk.obj";
        try {
            // If the background preload thread is still running, wait for it.
            // In normal play the thread finishes during the main menu, so this is instant.
            if (cachedPcModel == null && pcModelPreloadThread != null && pcModelPreloadThread.isAlive()) {
                pcModelPreloadThread.join(30_000);
            }
            // Fallback: if preload failed or timed out, load synchronously now.
            if (cachedPcModel == null) {
                cachedPcModel = app.getAssetManager().loadModel(MODEL_PATH);
                applyUniformMaterial(cachedPcModel,
                        coloredMaterial(new ColorRGBA(0.13f, 0.15f, 0.19f, 1f), false),
                        coloredMaterial(new ColorRGBA(0.22f, 0.25f, 0.30f, 1f), false));
            }
            Spatial model = cachedPcModel.clone();
            model.setName(name);
            model.setLocalScale(0.2f);
            model.setLocalTranslation(position.x, position.y - 1.0f, position.z);
            tagSpatial(model, interactable);
            propNode.attachChild(model);
            return model;
        } catch (Exception e) {
            System.err.println("[WorldManager] Could not load PC model (" + MODEL_PATH + "): " + e.getMessage());
            return createAudioLogPickup(name, position, new ColorRGBA(1f, 0.1f, 0.1f, 1f), interactable);
        }
    }

    /**
     * Walks the spatial tree and alternates between two materials so the model
     * has some visual depth without needing real textures.
     */
    private void applyUniformMaterial(Spatial spatial, Material primary, Material accent) {
        applyUniformMaterialRecursive(spatial, primary, accent, new int[]{0});
    }

    private void applyUniformMaterialRecursive(Spatial spatial, Material primary, Material accent, int[] counter) {
        if (spatial instanceof Geometry geo) {
            geo.setMaterial((counter[0]++ % 3 == 0) ? accent : primary);
        } else if (spatial instanceof Node node) {
            for (Spatial child : node.getChildren()) {
                applyUniformMaterialRecursive(child, primary, accent, counter);
            }
        }
    }

    public Spatial createLabBench(String name, Vector3f position, float width, ColorRGBA accentColor) {
        Node bench = new Node(name);

        Geometry top = new Geometry(name + "Top", new Box(width, 0.08f, 0.45f));
        top.setMaterial(coloredMaterial(new ColorRGBA(0.78f, 0.82f, 0.86f, 1f), false));
        top.setLocalTranslation(0f, 0.85f, 0f);
        bench.attachChild(top);

        Geometry supportLeft = new Geometry(name + "SupportLeft", new Box(0.08f, 0.75f, 0.08f));
        supportLeft.setMaterial(coloredMaterial(new ColorRGBA(0.18f, 0.2f, 0.24f, 1f), false));
        supportLeft.setLocalTranslation(-width + 0.08f, 0.05f, -0.3f);
        bench.attachChild(supportLeft);

        Geometry supportRight = new Geometry(name + "SupportRight", new Box(0.08f, 0.75f, 0.08f));
        supportRight.setMaterial(coloredMaterial(new ColorRGBA(0.18f, 0.2f, 0.24f, 1f), false));
        supportRight.setLocalTranslation(width - 0.08f, 0.05f, -0.3f);
        bench.attachChild(supportRight);

        Geometry cabinet = new Geometry(name + "Cabinet", new Box(width * 0.48f, 0.5f, 0.32f));
        cabinet.setMaterial(coloredMaterial(new ColorRGBA(0.12f, 0.14f, 0.17f, 1f), false));
        cabinet.setLocalTranslation(0f, 0.32f, 0.05f);
        bench.attachChild(cabinet);

        Geometry monitor = new Geometry(name + "Monitor", new Box(0.22f, 0.18f, 0.03f));
        monitor.setMaterial(coloredMaterial(accentColor, true));
        monitor.setLocalTranslation(0f, 1.12f, -0.15f);
        bench.attachChild(monitor);

        bench.setLocalTranslation(position);
        propNode.attachChild(bench);
        return bench;
    }

    public Spatial createServerRack(String name, Vector3f position, ColorRGBA accentColor) {
        Node rack = new Node(name);

        Geometry body = new Geometry(name + "Body", new Box(0.45f, 1.2f, 0.45f));
        body.setMaterial(coloredMaterial(new ColorRGBA(0.08f, 0.1f, 0.12f, 1f), false));
        rack.attachChild(body);

        for (int i = 0; i < 4; i++) {
            Geometry panel = new Geometry(name + "Panel" + i, new Box(0.32f, 0.14f, 0.02f));
            panel.setMaterial(coloredMaterial(accentColor.mult(0.65f + (i * 0.08f)), true));
            panel.setLocalTranslation(0f, -0.7f + (i * 0.48f), 0.47f);
            rack.attachChild(panel);
        }

        rack.setLocalTranslation(position);
        propNode.attachChild(rack);
        return rack;
    }

    public Spatial createContainmentPod(String name, Vector3f position, ColorRGBA glowColor) {
        Node pod = new Node(name);

        Geometry base = new Geometry(name + "Base", new Box(0.7f, 0.18f, 1.1f));
        base.setMaterial(coloredMaterial(new ColorRGBA(0.15f, 0.18f, 0.22f, 1f), false));
        pod.attachChild(base);

        Geometry chamber = new Geometry(name + "Chamber", new Box(0.5f, 0.95f, 0.95f));
        chamber.setMaterial(coloredMaterial(new ColorRGBA(0.45f, 0.7f, 0.9f, 0.22f), true));
        chamber.setQueueBucket(RenderQueue.Bucket.Transparent);
        chamber.setLocalTranslation(0f, 0.95f, 0f);
        pod.attachChild(chamber);

        Geometry core = new Geometry(name + "Core", new Box(0.18f, 0.55f, 0.18f));
        core.setMaterial(coloredMaterial(glowColor, true));
        core.setLocalTranslation(0f, 0.95f, 0f);
        pod.attachChild(core);

        pod.setLocalTranslation(position);
        propNode.attachChild(pod);
        return pod;
    }

    public Spatial createStorageCrate(String name, Vector3f position, ColorRGBA stripeColor) {
        Node crate = new Node(name);

        Geometry body = new Geometry(name + "Body", new Box(0.45f, 0.45f, 0.45f));
        body.setMaterial(coloredMaterial(new ColorRGBA(0.28f, 0.3f, 0.33f, 1f), false));
        crate.attachChild(body);

        Geometry stripe = new Geometry(name + "Stripe", new Box(0.4f, 0.06f, 0.48f));
        stripe.setMaterial(coloredMaterial(stripeColor, true));
        stripe.setLocalTranslation(0f, 0.16f, 0f);
        crate.attachChild(stripe);

        crate.setLocalTranslation(position);
        propNode.attachChild(crate);
        return crate;
    }

    public Spatial createMedicalCart(String name, Vector3f position, ColorRGBA accentColor) {
        Node cart = new Node(name);

        Geometry tray = new Geometry(name + "Tray", new Box(0.42f, 0.08f, 0.28f));
        tray.setMaterial(coloredMaterial(new ColorRGBA(0.75f, 0.79f, 0.83f, 1f), false));
        tray.setLocalTranslation(0f, 0.82f, 0f);
        cart.attachChild(tray);

        Geometry base = new Geometry(name + "Base", new Box(0.3f, 0.42f, 0.22f));
        base.setMaterial(coloredMaterial(new ColorRGBA(0.22f, 0.25f, 0.28f, 1f), false));
        base.setLocalTranslation(0f, 0.34f, 0f);
        cart.attachChild(base);

        Geometry screen = new Geometry(name + "Screen", new Box(0.18f, 0.12f, 0.03f));
        screen.setMaterial(coloredMaterial(accentColor, true));
        screen.setLocalTranslation(0f, 1.02f, -0.1f);
        cart.attachChild(screen);

        cart.setLocalTranslation(position);
        propNode.attachChild(cart);
        return cart;
    }

    public Spatial createWallMonitor(String name, Vector3f position, ColorRGBA accentColor) {
        Node monitor = new Node(name);

        Geometry panel = new Geometry(name + "Panel", new Box(0.44f, 0.28f, 0.04f));
        panel.setMaterial(coloredMaterial(new ColorRGBA(0.08f, 0.1f, 0.12f, 1f), false));
        monitor.attachChild(panel);

        Geometry screen = new Geometry(name + "Screen", new Box(0.36f, 0.2f, 0.02f));
        screen.setMaterial(coloredMaterial(accentColor, true));
        screen.setLocalTranslation(0f, 0f, 0.05f);
        monitor.attachChild(screen);

        monitor.setLocalTranslation(position);
        propNode.attachChild(monitor);
        return monitor;
    }

    public Spatial createLockerBank(String name, Vector3f position, ColorRGBA accentColor) {
        Node lockers = new Node(name);

        for (int i = 0; i < 3; i++) {
            Geometry locker = new Geometry(name + "Locker" + i, new Box(0.24f, 0.9f, 0.2f));
            locker.setMaterial(coloredMaterial(new ColorRGBA(0.26f, 0.29f, 0.33f, 1f), false));
            locker.setLocalTranslation((i - 1) * 0.54f, 0.9f, 0f);
            lockers.attachChild(locker);

            Geometry strip = new Geometry(name + "Strip" + i, new Box(0.18f, 0.04f, 0.21f));
            strip.setMaterial(coloredMaterial(accentColor, true));
            strip.setLocalTranslation((i - 1) * 0.54f, 1.42f, 0f);
            lockers.attachChild(strip);
        }

        lockers.setLocalTranslation(position);
        propNode.attachChild(lockers);
        return lockers;
    }

    public Spatial createDoorButton(String name, Vector3f position, Interactable interactable) {
        Node button = new Node(name);

        Geometry base = new Geometry(name + "Base", new Box(0.12f, 0.18f, 0.08f));
        base.setMaterial(coloredMaterial(new ColorRGBA(0.18f, 0.2f, 0.22f, 1f), false));
        button.attachChild(base);

        Geometry cap = new Geometry(name + "Cap", new Box(0.08f, 0.08f, 0.03f));
        cap.setMaterial(coloredMaterial(new ColorRGBA(0.92f, 0.12f, 0.12f, 1f), true));
        cap.setLocalTranslation(0f, 0f, 0.09f);
        button.attachChild(cap);

        button.setLocalTranslation(position);
        tagSpatial(button, interactable);
        propNode.attachChild(button);
        return button;
    }

    public Spatial createCeilingLight(String name, Vector3f position, ColorRGBA glowColor) {
        Node light = new Node(name);

        Geometry housing = new Geometry(name + "Housing", new Box(0.4f, 0.08f, 0.4f));
        housing.setMaterial(coloredMaterial(new ColorRGBA(0.28f, 0.32f, 0.35f, 1f), false));
        light.attachChild(housing);

        Geometry bulb = new Geometry(name + "Bulb", new Box(0.26f, 0.04f, 0.26f));
        bulb.setMaterial(coloredMaterial(glowColor, true));
        bulb.setLocalTranslation(0f, -0.12f, 0f);
        light.attachChild(bulb);

        light.setLocalTranslation(position);
        propNode.attachChild(light);
        return light;
    }

    public Spatial createAnalyzerStation(String name, Vector3f position, ColorRGBA glowColor) {
        Node station = new Node(name);

        Geometry base = new Geometry(name + "Base", new Box(0.35f, 0.75f, 0.35f));
        base.setMaterial(coloredMaterial(new ColorRGBA(0.2f, 0.22f, 0.26f, 1f), false));
        station.attachChild(base);

        Geometry scanner = new Geometry(name + "Scanner", new Box(0.22f, 0.08f, 0.22f));
        scanner.setMaterial(coloredMaterial(glowColor, true));
        scanner.setLocalTranslation(0f, 0.86f, 0f);
        station.attachChild(scanner);

        Geometry arm = new Geometry(name + "Arm", new Box(0.06f, 0.24f, 0.06f));
        arm.setMaterial(coloredMaterial(new ColorRGBA(0.55f, 0.58f, 0.62f, 1f), false));
        arm.setLocalTranslation(0.18f, 1.05f, 0f);
        station.attachChild(arm);

        station.setLocalTranslation(position);
        propNode.attachChild(station);
        return station;
    }

    public Spatial createTankCluster(String name, Vector3f position, ColorRGBA glowColor) {
        Node tanks = new Node(name);
        for (int i = 0; i < 3; i++) {
            Geometry tank = new Geometry(name + "Tank" + i, new Box(0.18f, 0.82f, 0.18f));
            tank.setMaterial(coloredMaterial(new ColorRGBA(0.16f, 0.19f, 0.22f, 1f), false));
            tank.setLocalTranslation((i - 1) * 0.34f, 0.82f, 0f);
            tanks.attachChild(tank);

            Geometry gauge = new Geometry(name + "Gauge" + i, new Box(0.06f, 0.06f, 0.2f));
            gauge.setMaterial(coloredMaterial(glowColor, true));
            gauge.setLocalTranslation((i - 1) * 0.34f, 1.18f, 0.2f);
            tanks.attachChild(gauge);
        }
        tanks.setLocalTranslation(position);
        propNode.attachChild(tanks);
        return tanks;
    }

    public void removeProp(Spatial spatial) {
        if (spatial != null) {
            spatial.removeFromParent();
        }
    }

    public Vector3f getSpawnPoint(int level) {
        return switch (level) {
            case 1 -> worldToScene(5, 1.8f, 6);
            case 2 -> worldToScene(8, 1.8f, 18);
            case 3 -> worldToScene(28, 1.8f, 7);
            case 4 -> worldToScene(35, 1.8f, 22);
            default -> worldToScene(5, 1.8f, 6);
        };
    }

    public Vector3f worldToScene(float x, float y, float z) {
        return new Vector3f(x * 1.2f, y * 1.2f, z * 1.2f);
    }

    public Interactable getInteractable(Spatial spatial) {
        Spatial current = spatial;
        while (current != null) {
            String interactableId = current.getUserData(INTERACTABLE_ID);
            if (interactableId != null) {
                return interactables.get(interactableId);
            }
            current = current.getParent();
        }
        return null;
    }

    public void placeEditorProp(EditorPropType type, BlockHit hit, Vector3f viewDirection, int rotation) {
        Vector3f pos = computePlacementPosition(type, hit, viewDirection);
        String propId = "editor-prop-" + nextEditorPropId++;
        EditorPropInstance instance = buildEditorProp(propId, type, pos, rotation, viewDirection);
        if (instance != null) {
            editorProps.put(propId, instance);
        }
    }

    // -----------------------------------------------------------------------
    // Ghost preview
    // -----------------------------------------------------------------------

    /**
     * Updates (or creates) a translucent ghost box at the predicted placement
     * position for the given prop type and block hit. Call every frame while
     * the editor is in prop mode.
     */
    public void updateEditorGhost(EditorPropType type, BlockHit hit, Vector3f viewDirection, int rotation) {
        ghostNode.detachAllChildren();
        if (hit == null) return;

        if (ghostMaterial == null) {
            ghostMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            ghostMaterial.setColor("Color", new ColorRGBA(0.2f, 0.85f, 1f, 0.35f));
            ghostMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        }

        Vector3f pos = computePlacementPosition(type, hit, viewDirection);
        float[] hs = getPropHalfExtents(type);

        Geometry ghost = new Geometry("ghost", new Box(hs[0], hs[1], hs[2]));
        ghost.setMaterial(ghostMaterial);
        ghost.setQueueBucket(RenderQueue.Bucket.Transparent);
        ghost.setLocalTranslation(pos);
        if (rotation != 0) {
            ghost.setLocalRotation(new Quaternion().fromAngleAxis(rotation * FastMath.HALF_PI, Vector3f.UNIT_Y));
        }
        ghostNode.attachChild(ghost);
    }

    /** Removes the ghost preview. Call when leaving prop mode or editor. */
    public void clearEditorGhost() {
        ghostNode.detachAllChildren();
    }

    /**
     * Computes the world-space placement position for a prop, respecting the
     * face normal of the hit block so doors snap to the floor Y they're placed on.
     */
    public Vector3f computePlacementPosition(EditorPropType type, BlockHit hit, Vector3f viewDirection) {
        // Step into the adjacent AIR cell via the face normal.
        int tx = hit.x() + Math.round(hit.normal().x);
        int ty = hit.y() + Math.round(hit.normal().y);
        int tz = hit.z() + Math.round(hit.normal().z);

        // Scan DOWN from ty to find the highest non-AIR block in the (tx,tz) column.
        // We use isAnyBlock() (any non-AIR type) so FLOOR, LIGHT, etc. are all detected.
        // floorBlock = Y of the solid surface block the prop will stand on.
        int floorBlock = 0; // default: ground
        for (int scanY = ty; scanY >= 0; scanY--) {
            if (currentChunk.getBlock(tx, scanY, tz) != BlockType.AIR) {
                floorBlock = scanY;  // this is the surface block itself
                break;
            }
        }

        // Y offsets below exactly match the original hardcoded level-dress values.
        // floorBlock is the solid surface block Y (e.g. 0 at ground level).
        return switch (type) {
            case AUTO_DOOR        -> worldToScene(tx, floorBlock + 1.5f,  tz);
            case KEYPAD_DOOR      -> worldToScene(tx, floorBlock + 1.5f,  tz);
            case CONSOLE          -> worldToScene(tx, floorBlock + 1.2f,  tz);
            case LAB_BENCH        -> worldToScene(tx, floorBlock,          tz);
            case SERVER_RACK      -> worldToScene(tx, floorBlock + 1.2f,  tz);
            case STORAGE_CRATE    -> worldToScene(tx, floorBlock + 0.45f, tz);
            case MEDICAL_CART     -> worldToScene(tx, floorBlock + 0.4f,  tz);
            case WALL_MONITOR     -> worldToScene(tx, floorBlock + 1.5f,  tz);
            case LOCKER_BANK      -> worldToScene(tx, floorBlock,          tz);
            case ANALYZER_STATION -> worldToScene(tx, floorBlock,          tz);
            case TANK_CLUSTER     -> worldToScene(tx, floorBlock,          tz);
            case CONTAINMENT_POD  -> worldToScene(tx, floorBlock + 0.2f,  tz);
            case CEILING_LIGHT    -> worldToScene(tx, ty + 0.75f,          tz);
        };
    }

    /**
     * Returns [halfX, halfY, halfZ] approximate visual extents for the ghost box
     * of the given prop type.
     */
    private float[] getPropHalfExtents(EditorPropType type) {
        return switch (type) {
            case AUTO_DOOR        -> new float[]{0.86f, 1.62f, 0.18f};
            case KEYPAD_DOOR      -> new float[]{0.86f, 1.62f, 0.18f};
            case CONSOLE          -> new float[]{0.62f, 0.95f, 0.45f};
            case LAB_BENCH        -> new float[]{1.1f, 0.9f,  0.5f};
            case SERVER_RACK      -> new float[]{0.52f, 1.3f,  0.35f};
            case STORAGE_CRATE    -> new float[]{0.52f, 0.5f,  0.52f};
            case MEDICAL_CART     -> new float[]{0.48f, 0.9f,  0.32f};
            case WALL_MONITOR     -> new float[]{0.46f, 0.32f, 0.08f};
            case LOCKER_BANK      -> new float[]{0.9f,  1.8f,  0.24f};
            case CEILING_LIGHT    -> new float[]{0.32f, 0.18f, 0.32f};
            case ANALYZER_STATION -> new float[]{0.62f, 0.85f, 0.45f};
            case TANK_CLUSTER     -> new float[]{0.55f, 0.9f,  0.55f};
            case CONTAINMENT_POD  -> new float[]{0.52f, 1.1f,  0.52f};
        };
    }

    /**
     * Finds the nearest prop (editor-placed or level-dressed) that the ray hits.
     * Returns a compound string "editor:propId" or "level:propId" so the caller
     * knows which map to remove from.
     */
    public String findAnyPropHit(Vector3f origin, Vector3f direction, float maxDistance) {
        CollisionResults results = new CollisionResults();
        propNode.collideWith(new Ray(origin, direction), results);
        for (CollisionResult result : results) {
            if (result.getDistance() > maxDistance) {
                break;
            }
            // Check editor props first
            String editorPropId = findTagInHierarchy(result.getGeometry(), EDITOR_PROP_ID);
            if (editorPropId != null) {
                return "editor:" + editorPropId;
            }
            // Fall back to level props
            String levelPropId = findTagInHierarchy(result.getGeometry(), LEVEL_PROP_ID);
            if (levelPropId != null) {
                return "level:" + levelPropId;
            }
        }
        return null;
    }

    /** Legacy alias kept for any existing call-sites. */
    public String findEditorPropHit(Vector3f origin, Vector3f direction, float maxDistance) {
        String hit = findAnyPropHit(origin, direction, maxDistance);
        if (hit == null) return null;
        // Strip the prefix and return the raw id regardless of origin
        return hit.substring(hit.indexOf(':') + 1);
    }

    public boolean removeEditorProp(String propId) {
        EditorPropInstance instance = editorProps.remove(propId);
        if (instance == null) {
            return false;
        }
        for (Spatial spatial : instance.spatials()) {
            spatial.removeFromParent();
        }
        if (instance.door() != null) {
            animatedDoors.remove(instance.door());
        }
        return true;
    }

    /**
     * Removes a level-dressed prop by its id.
     * Returns true if a prop was found and removed.
     */
    public boolean removeLevelProp(String propId) {
        Spatial spatial = levelProps.remove(propId);
        if (spatial == null) {
            return false;
        }
        spatial.removeFromParent();
        // If this spatial is a door node, also unregister the Door object.
        for (Door door : new ArrayList<>(animatedDoors)) {
            if (door.getSpatial() == spatial) {
                animatedDoors.remove(door);
                break;
            }
        }
        return true;
    }

    public boolean hasSavedProps(int level) {
        return Files.exists(getPropPath(level));
    }

    public boolean wasSavedPropsLoaded() {
        return savedPropsLoaded;
    }

    public BlockHit findBlockHit(Vector3f origin, Vector3f direction, float maxDistance) {
        CollisionResults results = new CollisionResults();
        Ray ray = new Ray(origin, direction.normalize());
        worldNode.collideWith(ray, results);

        for (CollisionResult cr : results) {
            if (cr.getDistance() > maxDistance) break;

            // Sample slightly behind the hit surface to identify the block
            Vector3f contact = cr.getContactPoint();
            Vector3f normal  = cr.getContactNormal();
            if (normal == null) continue;

            // Move 1 mm inside the surface so toChunkIndex lands in the solid block
            float eps = 0.001f;
            int x = toChunkIndex(contact.x - normal.x * eps);
            int y = toChunkIndex(contact.y - normal.y * eps);
            int z = toChunkIndex(contact.z - normal.z * eps);

            BlockType type = currentChunk.getBlock(x, y, z);
            if (type == BlockType.AIR) continue;  // glass-back-face or stale geometry

            return new BlockHit(x, y, z, type, quantizeNormal(normal));
        }
        return null;
    }

    /** Snaps an arbitrary surface normal to the nearest cardinal axis direction. */
    private Vector3f quantizeNormal(Vector3f n) {
        float ax = Math.abs(n.x), ay = Math.abs(n.y), az = Math.abs(n.z);
        if (ax >= ay && ax >= az) return new Vector3f(Math.signum(n.x), 0f, 0f);
        if (ay >= ax && ay >= az) return new Vector3f(0f, Math.signum(n.y), 0f);
        return new Vector3f(0f, 0f, Math.signum(n.z));
    }

    public boolean isWalkable(Vector3f position, float radius, float eyeHeight) {
        float lowerSample = position.y - eyeHeight + 0.15f;
        float midSample = position.y - (eyeHeight * 0.45f);
        float upperSample = position.y - 0.15f;
        float[] xOffsets = {0f, -radius, radius, 0f, 0f, -radius, radius, -radius, radius};
        float[] zOffsets = {0f, 0f, 0f, -radius, radius, -radius, -radius, radius, radius};
        float[] ySamples = {lowerSample, midSample, upperSample};

        for (int i = 0; i < xOffsets.length; i++) {
            float sampleX = position.x + xOffsets[i];
            float sampleZ = position.z + zOffsets[i];
            for (float sampleY : ySamples) {
                if (isSolidWorldPoint(sampleX, sampleY, sampleZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void setBlock(int x, int y, int z, BlockType type) {
        currentChunk.setBlock(x, y, z, type);
        renderChunk();
    }

    public BlockType getBlock(int x, int y, int z) {
        return currentChunk.getBlock(x, y, z);
    }

    public int toChunkIndex(float coordinate) {
        return (int) Math.floor((coordinate + 0.6f) / 1.2f);
    }

    public void saveCurrentChunk(int level) {
        try {
            Files.createDirectories(MAP_DIRECTORY);
            List<String> lines = new ArrayList<>();
            lines.add("SUBJECT47_MAP 1");
            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int y = 0; y < Chunk.SIZE; y++) {
                    for (int z = 0; z < Chunk.SIZE; z++) {
                        BlockType type = currentChunk.getBlock(x, y, z);
                        if (type != BlockType.AIR) {
                            lines.add(x + "," + y + "," + z + "," + type.name());
                        }
                    }
                }
            }
            Files.write(getMapPath(level), lines);
        } catch (IOException exception) {
            app.getUi().showMessage("Failed to save map: " + exception.getMessage(), 3f);
        }
    }

    public void saveEditorProps(int level) {
        try {
            Files.createDirectories(MAP_DIRECTORY);
            List<String> lines = new ArrayList<>();
            lines.add("SUBJECT47_PROPS 3");
            for (Map.Entry<String, Spatial> entry : levelProps.entrySet()) {
                Object record = entry.getValue().getUserData("levelPropRecord");
                if (record instanceof String recordStr) {
                    lines.add("LEVEL:" + recordStr + ",0");
                }
            }
            // Save actual scene position (floats) so reload is pixel-perfect.
            for (EditorPropInstance inst : editorProps.values()) {
                lines.add(inst.type().name() + ","
                        + inst.pos().x + "," + inst.pos().y + "," + inst.pos().z
                        + "," + inst.rotation());
            }
            Files.write(getPropPath(level), lines);
        } catch (IOException exception) {
            app.getUi().showMessage("Failed to save props: " + exception.getMessage(), 3f);
        }
    }

    private void renderChunk() {
        worldNode.detachAllChildren();
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    BlockType type = currentChunk.getBlock(x, y, z);
                    if (type == BlockType.AIR) {
                        continue;
                    }

                    Geometry block = new Geometry("block", new Box(0.6f, 0.6f, 0.6f));
                    Material material = createBlockMaterial(type);
                    block.setMaterial(material);
                    block.setLocalTranslation(x * 1.2f, y * 1.2f, z * 1.2f);

                    if (type == BlockType.GLASS) {
                        block.setQueueBucket(RenderQueue.Bucket.Transparent);
                    }

                    worldNode.attachChild(block);
                }
            }
        }
    }

    private Material createBlockMaterial(BlockType type) {
        return switch (type) {
            case FLOOR -> coloredMaterial(new ColorRGBA(0.72f, 0.76f, 0.79f, 1f), false);
            case WALL -> coloredMaterial(new ColorRGBA(0.2f, 0.23f, 0.27f, 1f), false);
            case CEILING -> coloredMaterial(new ColorRGBA(0.64f, 0.68f, 0.72f, 1f), false);
            case TRIM -> coloredMaterial(new ColorRGBA(0.82f, 0.86f, 0.9f, 1f), false);
            case PANEL -> coloredMaterial(new ColorRGBA(0.3f, 0.34f, 0.39f, 1f), false);
            case GLASS -> coloredMaterial(new ColorRGBA(0.35f, 0.55f, 0.72f, 0.35f), true);
            case LIGHT -> createLightMaterial();
            default -> coloredMaterial(ColorRGBA.Black, false);
        };
    }

    private boolean isSolidWorldPoint(float x, float y, float z) {
        BlockType type = currentChunk.getBlock(toChunkIndex(x), toChunkIndex(y), toChunkIndex(z));
        if (isSolidBlock(type)) {
            return true;
        }

        Vector3f point = new Vector3f(x, y, z);
        for (Door door : animatedDoors) {
            if (!door.isOpen() && door.blocksPosition(point, 0f)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSolidBlock(BlockType type) {
        return switch (type) {
            case WALL, GLASS, TRIM, PANEL, CEILING -> true;
            default -> false;
        };
    }

    private boolean loadSavedChunk(int level) {
        Path path = getMapPath(level);
        if (!Files.exists(path)) {
            return false;
        }

        try {
            List<String> lines = Files.readAllLines(path);
            currentChunk.fillAir();
            for (String line : lines) {
                if (line == null || line.isBlank() || line.startsWith("SUBJECT47_MAP")) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length != 4) {
                    continue;
                }
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int z = Integer.parseInt(parts[2]);
                BlockType type = BlockType.valueOf(parts[3]);
                currentChunk.setBlock(x, y, z, type);
            }
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            app.getUi().showMessage("Failed to load saved map for level " + level + ".", 3f);
            return false;
        }
    }

    private Path getMapPath(int level) {
        return MAP_DIRECTORY.resolve("level-" + level + ".map");
    }

    private Path getPropPath(int level) {
        return MAP_DIRECTORY.resolve("level-" + level + ".props");
    }

    /**
     * Loads saved props. Returns true when a saved file was found (caller should
     * skip level decoration in that case so the file is the sole source of truth).
     */
    private boolean loadSavedProps(int level) {
        Path path = getPropPath(level);
        if (!Files.exists(path)) return false;

        try {
            List<String> lines = Files.readAllLines(path);
            String header = lines.isEmpty() ? "" : lines.get(0);
            boolean v2 = header.startsWith("SUBJECT47_PROPS 2");
            boolean v3 = header.startsWith("SUBJECT47_PROPS 3");

            for (String line : lines) {
                if (line == null || line.isBlank() || line.startsWith("SUBJECT47_PROPS")) continue;

                if ((v2 || v3) && line.startsWith("LEVEL:")) {
                    String payload = line.substring(6);
                    String[] parts = payload.split(",");
                    if (parts.length < 4) continue;
                    EditorPropType type = EditorPropType.valueOf(parts[0]);
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int z = Integer.parseInt(parts[3]);
                    String propId = "level-prop-" + nextEditorPropId++;
                    Spatial spatial = buildLevelProp(propId, type, x, y, z);
                    if (spatial != null) {
                        spatial.setUserData("levelPropRecord", type.name() + "," + x + "," + y + "," + z);
                        levelProps.put(propId, spatial);
                    }
                } else {
                    String[] parts = line.split(",");
                    if (parts.length < 4) continue;
                    EditorPropType type = EditorPropType.valueOf(parts[0]);
                    int rot = 0;
                    Vector3f pos;
                    if (v3 && parts.length >= 5) {
                        // v3: TYPE,sceneX,sceneY,sceneZ,rotation
                        float sx = Float.parseFloat(parts[1]);
                        float sy = Float.parseFloat(parts[2]);
                        float sz = Float.parseFloat(parts[3]);
                        rot = Integer.parseInt(parts[4]);
                        pos = new Vector3f(sx, sy, sz);
                    } else {
                        // v2: TYPE,blockX,blockY,blockZ  (legacy, approximate)
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        int z = Integer.parseInt(parts[3]);
                        pos = worldToScene(x, y, z);
                    }
                    String propId = "editor-prop-" + nextEditorPropId++;
                    EditorPropInstance inst = buildEditorProp(propId, type, pos, rot,
                            new Vector3f(0f, 0f, -1f));
                    if (inst != null) editorProps.put(propId, inst);
                }
            }
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            app.getUi().showMessage("Failed to load saved props for level " + level + ".", 3f);
            return false;
        }
    }

    private EditorPropInstance buildEditorProp(String propId, EditorPropType type,
                                               Vector3f pos, int rotation, Vector3f viewDirection) {
        List<Spatial> spatials = new ArrayList<>();
        Door linkedDoor = null;

        Quaternion rotQ = (rotation != 0)
                ? new Quaternion().fromAngleAxis(rotation * FastMath.HALF_PI, Vector3f.UNIT_Y)
                : null;

        Vector3f rawOff = computeButtonOffset(viewDirection);
        Vector3f buttonOff = (rotQ != null) ? rotQ.mult(rawOff) : rawOff;

        switch (type) {
            case AUTO_DOOR -> {
                Door door = createDoor("EditorDoor" + propId, pos, false);
                linkedDoor = door;
                if (rotQ != null) door.getSpatial().setLocalRotation(rotQ);
                Vector3f btnPos = pos.add(buttonOff.x * 1.2f, -0.1f, buttonOff.z * 1.2f);
                Spatial button = createDoorButton("EditorDoorButton" + propId, btnPos,
                        new ControlSwitch(app, "Press E to cycle the door", null,
                                () -> door.toggleFromButton("Door cycles open.")));
                tagEditorProp(button, propId);
                tagEditorProp(door.getSpatial(), propId);
                spatials.add(door.getSpatial());
                spatials.add(button);
            }
            case KEYPAD_DOOR -> {
                // The door itself opens the keypad when locked, then toggles when unlocked.
                Door door = createDoor("EditorKDoor" + propId, pos, true);
                linkedDoor = door;
                if (rotQ != null) door.getSpatial().setLocalRotation(rotQ);
                door.setOnLockedCallback(() ->
                    app.getUi().openKeypad("DOOR ACCESS CODE", code -> {
                        if ("1234".equals(code)) {
                            door.unlock("Access granted.");
                        } else {
                            app.getUi().showMessage("Access denied.", 2f);
                        }
                    })
                );
                tagEditorProp(door.getSpatial(), propId);
                spatials.add(door.getSpatial());
            }
            case CONSOLE -> {
                Spatial console = createConsole("EditorConsole" + propId, pos,
                        new ColorRGBA(0.18f, 0.78f, 0.95f, 1f),
                        new ControlSwitch(app, "Press E to inspect console", "Console is idle.", null));
                if (rotQ != null) console.setLocalRotation(rotQ);
                tagEditorProp(console, propId);
                spatials.add(console);
            }
            case LAB_BENCH -> {
                Spatial bench = createLabBench("EditorBench" + propId, pos, 1f, new ColorRGBA(0.18f, 0.82f, 0.95f, 1f));
                if (rotQ != null) bench.setLocalRotation(rotQ);
                tagEditorProp(bench, propId);
                spatials.add(bench);
            }
            case SERVER_RACK -> {
                Spatial rack = createServerRack("EditorRack" + propId, pos, new ColorRGBA(0.22f, 0.86f, 0.95f, 1f));
                if (rotQ != null) rack.setLocalRotation(rotQ);
                tagEditorProp(rack, propId);
                spatials.add(rack);
            }
            case STORAGE_CRATE -> {
                Spatial crate = createStorageCrate("EditorCrate" + propId, pos, new ColorRGBA(0.95f, 0.72f, 0.22f, 1f));
                if (rotQ != null) crate.setLocalRotation(rotQ);
                tagEditorProp(crate, propId);
                spatials.add(crate);
            }
            case MEDICAL_CART -> {
                Spatial cart = createMedicalCart("EditorCart" + propId, pos, new ColorRGBA(0.18f, 0.86f, 0.95f, 1f));
                if (rotQ != null) cart.setLocalRotation(rotQ);
                tagEditorProp(cart, propId);
                spatials.add(cart);
            }
            case WALL_MONITOR -> {
                Spatial monitor = createWallMonitor("EditorMonitor" + propId, pos, new ColorRGBA(0.18f, 0.86f, 0.95f, 1f));
                if (rotQ != null) monitor.setLocalRotation(rotQ);
                tagEditorProp(monitor, propId);
                spatials.add(monitor);
            }
            case LOCKER_BANK -> {
                Spatial lockers = createLockerBank("EditorLockers" + propId, pos, new ColorRGBA(0.82f, 0.14f, 0.14f, 1f));
                if (rotQ != null) lockers.setLocalRotation(rotQ);
                tagEditorProp(lockers, propId);
                spatials.add(lockers);
            }
            case CEILING_LIGHT -> {
                Spatial light = createCeilingLight("EditorLight" + propId, pos, new ColorRGBA(0.74f, 0.9f, 1f, 1f));
                tagEditorProp(light, propId);
                spatials.add(light);
            }
            case ANALYZER_STATION -> {
                Spatial station = createAnalyzerStation("EditorAnalyzer" + propId, pos, new ColorRGBA(0.18f, 0.88f, 0.95f, 1f));
                if (rotQ != null) station.setLocalRotation(rotQ);
                tagEditorProp(station, propId);
                spatials.add(station);
            }
            case TANK_CLUSTER -> {
                Spatial tanks = createTankCluster("EditorTanks" + propId, pos, new ColorRGBA(0.18f, 0.76f, 0.95f, 1f));
                if (rotQ != null) tanks.setLocalRotation(rotQ);
                tagEditorProp(tanks, propId);
                spatials.add(tanks);
            }
            case CONTAINMENT_POD -> {
                Spatial pod = createContainmentPod("EditorPod" + propId, pos, new ColorRGBA(0.25f, 0.9f, 0.42f, 1f));
                if (rotQ != null) pod.setLocalRotation(rotQ);
                tagEditorProp(pod, propId);
                spatials.add(pod);
            }
            default -> { return null; }
        }

        return new EditorPropInstance(propId, type, pos, rotation, linkedDoor, spatials);
    }

    private Vector3f computeButtonOffset(Vector3f direction) {
        if (Math.abs(direction.x) > Math.abs(direction.z)) {
            return new Vector3f(0f, 0f, direction.x >= 0f ? 1f : -1f);
        }
        return new Vector3f(direction.z >= 0f ? -1f : 1f, 0f, 0f);
    }

    private int clampToNormal(int delta) {
        if (delta > 0) {
            return 1;
        }
        if (delta < 0) {
            return -1;
        }
        return 0;
    }

    private Material createLightMaterial() {
        Material material = coloredMaterial(new ColorRGBA(0.35f, 0.7f, 0.95f, 1f), true);
        lightMaterials.add(material);
        return material;
    }

    private Material coloredMaterial(ColorRGBA color, boolean emissive) {
        Material material = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Diffuse", color);
        material.setColor("Ambient", color.mult(0.45f));
        if (emissive) {
            material.setColor("GlowColor", color);
        }
        if (color.a < 1f) {
            material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        }
        return material;
    }

    private void applyLightState(float brightness) {
        ColorRGBA base = emergencyLighting
                ? new ColorRGBA(0.85f, 0.18f, 0.18f, 1f)
                : new ColorRGBA(0.35f, 0.7f, 0.95f, 1f);

        ColorRGBA lit = base.mult(brightness);
        ColorRGBA ambient = lit.mult(0.45f);

        for (Material material : lightMaterials) {
            material.setColor("Diffuse", lit);
            material.setColor("Ambient", ambient);
            material.setColor("GlowColor", lit);
        }
    }

    private void tagSpatial(Spatial spatial, Interactable interactable) {
        String interactableId = "interactable-" + nextInteractableId++;
        tagSpatial(spatial, interactable, interactableId);
        interactables.put(interactableId, interactable);
    }

    private void tagSpatial(Spatial spatial, Interactable interactable, String interactableId) {
        spatial.setUserData(INTERACTABLE_ID, interactableId);
        if (spatial instanceof Node node) {
            for (Spatial child : node.getChildren()) {
                tagSpatial(child, interactable, interactableId);
            }
        }
    }

    private void tagEditorProp(Spatial spatial, String propId) {
        if (spatial == null) {
            return;
        }
        spatial.setUserData(EDITOR_PROP_ID, propId);
        if (spatial instanceof Node node) {
            for (Spatial child : node.getChildren()) {
                tagEditorProp(child, propId);
            }
        }
    }

    /** Tags all geometry in a spatial with the level-prop id (for ray-pick lookup). */
    public void tagLevelProp(Spatial spatial, String propId) {
        if (spatial == null) {
            return;
        }
        spatial.setUserData(LEVEL_PROP_ID, propId);
        if (spatial instanceof Node node) {
            for (Spatial child : node.getChildren()) {
                tagLevelProp(child, propId);
            }
        }
    }

    /**
     * Registers a spatial as a level prop so the editor can remove it.
     * The record string is persisted on save so the prop can be reconstructed.
     */
    public void registerLevelProp(String propId, Spatial spatial, EditorPropType type, int x, int y, int z) {
        String record = type.name() + "," + x + "," + y + "," + z;
        spatial.setUserData("levelPropRecord", record);
        tagLevelProp(spatial, propId);
        levelProps.put(propId, spatial);
    }

    /** Walks up the scene-graph hierarchy to find a user-data tag. */
    private String findTagInHierarchy(Spatial spatial, String key) {
        Spatial current = spatial;
        while (current != null) {
            String val = current.getUserData(key);
            if (val != null) {
                return val;
            }
            current = current.getParent();
            // Stop at propNode boundary
            if (current == propNode) {
                break;
            }
        }
        return null;
    }

    /**
     * Builds a single-spatial level prop (no Door/ControlSwitch wiring).
     * Used when restoring level props from a v2 save file.
     */
    private Spatial buildLevelProp(String propId, EditorPropType type, int x, int y, int z) {
        return switch (type) {
            case CONSOLE -> {
                Spatial s = createConsole("LvlConsole" + propId, worldToScene(x, 1.2f, z),
                        new ColorRGBA(0.18f, 0.78f, 0.95f, 1f),
                        new subject47.interaction.ControlSwitch(app, "Press E to inspect console", "Console is idle.", null));
                yield s;
            }
            case LAB_BENCH -> createLabBench("LvlBench" + propId, worldToScene(x, 0f, z), 1f, new ColorRGBA(0.18f, 0.82f, 0.95f, 1f));
            case SERVER_RACK -> createServerRack("LvlRack" + propId, worldToScene(x, 1.2f, z), new ColorRGBA(0.22f, 0.86f, 0.95f, 1f));
            case STORAGE_CRATE -> createStorageCrate("LvlCrate" + propId, worldToScene(x, 0.45f, z), new ColorRGBA(0.95f, 0.72f, 0.22f, 1f));
            case MEDICAL_CART -> createMedicalCart("LvlCart" + propId, worldToScene(x, 0.4f, z), new ColorRGBA(0.18f, 0.86f, 0.95f, 1f));
            case WALL_MONITOR -> createWallMonitor("LvlMonitor" + propId, worldToScene(x, 1.5f, z), new ColorRGBA(0.18f, 0.86f, 0.95f, 1f));
            case LOCKER_BANK -> createLockerBank("LvlLockers" + propId, worldToScene(x, 0f, z), new ColorRGBA(0.82f, 0.14f, 0.14f, 1f));
            case CEILING_LIGHT -> createCeilingLight("LvlLight" + propId, worldToScene(x, y + 0.75f, z), new ColorRGBA(0.74f, 0.9f, 1f, 1f));
            case ANALYZER_STATION -> createAnalyzerStation("LvlAnalyzer" + propId, worldToScene(x, 0f, z), new ColorRGBA(0.18f, 0.88f, 0.95f, 1f));
            case TANK_CLUSTER -> createTankCluster("LvlTanks" + propId, worldToScene(x, 0f, z), new ColorRGBA(0.18f, 0.76f, 0.95f, 1f));
            case CONTAINMENT_POD -> createContainmentPod("LvlPod" + propId, worldToScene(x, 0.2f, z), new ColorRGBA(0.25f, 0.9f, 0.42f, 1f));
            default -> null; // AUTO_DOOR needs wiring — skip on restore
        };
    }

    public record BlockHit(int x, int y, int z, BlockType type, Vector3f normal) {
    }

    private record EditorPropInstance(
            String id,
            EditorPropType type,
            Vector3f pos,
            int rotation,
            Door door,
            List<Spatial> spatials
    ) {
    }
}
