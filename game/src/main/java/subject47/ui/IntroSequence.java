package subject47.ui;

import java.awt.AlphaComposite;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;

import subject47.Main;
import subject47.audio.AudioManager;

public class IntroSequence {

    private enum Phase {
        START_SCREEN,
        CINEMATIC
    }

    private record CameraPoint(float time, Vector3f position, Vector3f lookAt) {
    }

    private record Subtitle(float start, float end, String text) {
    }

    private static final float DURATION = 20f;

    private static final CameraPoint[] CAMERA_PATH = {
            new CameraPoint(0f, new Vector3f(0f, 2f, 5f), new Vector3f(0f, 1.5f, -5f)),
            new CameraPoint(5f, new Vector3f(0f, 2f, -10f), new Vector3f(0f, 1.5f, -20f)),
            new CameraPoint(8f, new Vector3f(-2f, 3f, -15f), new Vector3f(1f, 1.5f, -25f)),
            new CameraPoint(13f, new Vector3f(0f, 2f, -28f), new Vector3f(0f, 1f, -31f)),
            new CameraPoint(16f, new Vector3f(2f, 2.2f, -26f), new Vector3f(2.5f, 2f, -26f)),
            new CameraPoint(20f, new Vector3f(2.3f, 2.1f, -26.3f), new Vector3f(2.5f, 2f, -26f))
    };

    private static final Subtitle[] SUBTITLES = {
            new Subtitle(0.5f, 4f, "LAB 04: CLASSIFIED HUMAN EXPERIMENTATION FACILITY"),
            new Subtitle(4.5f, 8f, "MOST SUBJECTS TERMINATED FOLLOWING ADVERSE COGNITIVE SYNC"),
            new Subtitle(8.5f, 12f, "SUBJECT 47: BIOLOGICAL STABILITY ATTAINED."),
            new Subtitle(12.5f, 16f, "CRITICAL ALERT: FACILITY SHUTDOWN BY UNKNOWN ACTOR"),
            new Subtitle(16.5f, 19.5f, "PROTOCOL 47: MEMORY WIPE COMPLETE. WAKING REPAIR TECHNICIAN...")
    };

    private final Main app;
    private final Runnable onFinished;
    private final AudioManager audioManager;
    private final Node sceneNode = new Node("IntroScene");
    private final Node guiNode = new Node("IntroGui");
    private final Node startNode = new Node("IntroStart");
    private final Node cinematicNode = new Node("IntroCinematic");
    private final ActionListener skipListener = this::handleSkipInput;

    private BitmapText startEyebrow;
    private Geometry startTitle;
    private BitmapText connectionText;
    private BitmapText initializeLabel;
    private BitmapText playIcon;
    private Geometry startBackdrop;
    private Geometry startFrame;
    private Geometry startDivider;
    private Geometry initializeButton;
    private Geometry initializeButtonGlow;
    private BitmapText subtitleText;
    private BitmapText decryptText;
    private BitmapText headerLeft;
    private BitmapText headerRight;
    private BitmapText logoText;
    private Geometry backdrop;
    private Geometry vignetteTop;
    private Geometry vignetteBottom;
    private Geometry progressBar;
    private Geometry logoBackdrop;

    private Phase phase = Phase.START_SCREEN;
    private float elapsed;
    private float startElapsed;
    private boolean active;
    private boolean finished;

    public IntroSequence(Main app, Runnable onFinished) {
        this.app = app;
        this.onFinished = onFinished;
        this.audioManager = app.getAudio();
    }

    public void initialize() {
        createScene();
        createGui();
        registerInput();
        sceneNode.setCullHint(Spatial.CullHint.Always);
        guiNode.setCullHint(Spatial.CullHint.Always);
        app.getRootNode().attachChild(sceneNode);
        app.getGuiNode().attachChild(guiNode);
    }

    public void start() {
        elapsed = 0f;
        startElapsed = 0f;
        finished = false;
        active = true;
        phase = Phase.START_SCREEN;
        sceneNode.setCullHint(Spatial.CullHint.Never);
        guiNode.setCullHint(Spatial.CullHint.Never);
        startNode.setCullHint(Spatial.CullHint.Never);
        cinematicNode.setCullHint(Spatial.CullHint.Always);
        app.getInputManager().setCursorVisible(true);
        updateStartCamera(0f);
        updateGui(0f);
    }

    public void update(float tpf) {
        if (!active) {
            return;
        }

        if (phase == Phase.START_SCREEN) {
            startElapsed += tpf;
            animateScene(startElapsed);
            updateStartCamera(startElapsed);
            updateGui(0f);
            return;
        }

        elapsed += tpf;
        animateScene(elapsed);

        updateCamera(elapsed);
        updateGui(elapsed);

        if (elapsed >= DURATION) {
            finish();
        }
    }

    public boolean isActive() {
        return active;
    }

    private void createScene() {
        Random random = new Random(47);

        for (int i = 0; i < 15; i++) {
            float z = -i * 4f;
            createFloorSegment(z, random);
            createWallSegment(-3f, z, true);
            createWallSegment(3f, z, false);
            createVoxel("CableA" + i, new Vector3f(-2.93f, 3f, z), new Vector3f(0.08f, 0.08f, 3.8f), new ColorRGBA(0.01f, 0.01f, 0.012f, 1f));

            if (i % 4 == 0) {
                createVoxel("BlueCeilingLight" + i, new Vector3f(0f, 4.5f, z), new Vector3f(1f, 0.08f, 1f), new ColorRGBA(0.02f, 0.45f, 0.95f, 1f));
            } else if (i % 7 == 0) {
                createVoxel("RedCeilingLight" + i, new Vector3f(0f, 4.5f, z), new Vector3f(1f, 0.08f, 1f), new ColorRGBA(0.85f, 0.04f, 0.03f, 1f));
            }

            if (i % 3 == 0) {
                createVoxel("Debris" + i, new Vector3f(random.nextFloat() * 4f - 2f, 0f, z + random.nextFloat() * 2f - 1f),
                        new Vector3f(0.3f, 0.1f, 0.3f), new ColorRGBA(0.13f, 0.13f, 0.13f, 1f));
            }
        }

        createVoxel("CrateA", new Vector3f(2f, 0.5f, -8f), new Vector3f(1f, 1f, 1f), new ColorRGBA(0.12f, 0.12f, 0.12f, 1f));
        createVoxel("CrateB", new Vector3f(-2f, 1.5f, -20f), new Vector3f(1f, 1f, 1f), new ColorRGBA(0.08f, 0.08f, 0.08f, 1f));
        createVoxel("CrateC", new Vector3f(-2f, 0.5f, -20f), new Vector3f(1f, 1f, 1f), new ColorRGBA(0.15f, 0.15f, 0.15f, 1f));

        createVoxel("LabStation", new Vector3f(0f, 1f, -25f), new Vector3f(2f, 2f, 2f), new ColorRGBA(0.18f, 0.18f, 0.18f, 1f));
        createVoxel("LabScreen", new Vector3f(0f, 1f, -23.85f), new Vector3f(1.5f, 1f, 0.08f), new ColorRGBA(0.02f, 0.8f, 0.9f, 1f));

        createVoxel("TankBase", new Vector3f(0f, 0f, -30f), new Vector3f(4f, 1f, 4f), new ColorRGBA(0.1f, 0.1f, 0.1f, 1f));
        createVoxel("TerminalPost", new Vector3f(2.5f, 1f, -31f), new Vector3f(0.5f, 2f, 0.5f), new ColorRGBA(0.08f, 0.08f, 0.08f, 1f));
        createVoxel("TerminalAlert", new Vector3f(2.5f, 2f, -30.72f), new Vector3f(0.6f, 0.4f, 0.08f), new ColorRGBA(0.9f, 0.02f, 0.02f, 1f));

        for (int i = 0; i < 12; i++) {
            Geometry shard = createVoxel("GlassShard" + i,
                    new Vector3f(random.nextFloat() * 4f - 2f, 0.2f, -30f + random.nextFloat() * 4f - 2f),
                    new Vector3f(0.2f, 0.2f, 0.2f), new ColorRGBA(0.38f, 0.9f, 0.95f, 0.78f));
            shard.rotate(random.nextFloat(), random.nextFloat(), random.nextFloat());
        }
    }

    private void createFloorSegment(float z, Random random) {
        for (int x = 0; x < 5; x++) {
            for (int dz = 0; dz < 5; dz++) {
                ColorRGBA color = random.nextFloat() > 0.9f
                        ? new ColorRGBA(0.07f, 0.07f, 0.07f, 1f)
                        : new ColorRGBA(0.025f, 0.025f, 0.025f, 1f);
                createVoxel("Floor" + x + "_" + dz + "_" + z, new Vector3f(x - 2f, -0.5f, z + dz - 2f), Vector3f.UNIT_XYZ, color);
            }
        }
    }

    private void createWallSegment(float x, float z, boolean left) {
        for (int dz = 0; dz < 5; dz++) {
            for (int y = 0; y < 5; y++) {
                createVoxel("Wall" + x + "_" + dz + "_" + y + "_" + z,
                        new Vector3f(x, y, z + dz - 2f),
                        Vector3f.UNIT_XYZ,
                        new ColorRGBA(0.035f, 0.035f, 0.04f, 1f));
            }
        }
        createVoxel("WallStripe" + x + "_" + z,
                new Vector3f(x + (left ? 0.08f : -0.08f), 2.5f, z),
                new Vector3f(0.08f, 0.08f, 4.8f),
                new ColorRGBA(0.03f, 0.09f, 0.18f, 1f));
    }

    private Geometry createVoxel(String name, Vector3f position, Vector3f scale, ColorRGBA color) {
        Geometry geometry = new Geometry(name, new Box(0.49f, 0.49f, 0.49f));
        geometry.setMaterial(createMaterial(color));
        geometry.setLocalTranslation(position);
        geometry.setLocalScale(scale);
        geometry.setShadowMode(RenderQueue.ShadowMode.Off);
        sceneNode.attachChild(geometry);
        return geometry;
    }

    private void createGui() {
        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        backdrop = createPanel("IntroBackdrop", new ColorRGBA(0f, 0f, 0f, 0.35f));
        vignetteTop = createPanel("IntroVignetteTop", new ColorRGBA(0f, 0f, 0f, 0.72f));
        vignetteBottom = createPanel("IntroVignetteBottom", new ColorRGBA(0f, 0f, 0f, 0.82f));
        progressBar = createPanel("IntroProgress", new ColorRGBA(0.12f, 0.62f, 0.95f, 0.85f));
        logoBackdrop = createPanel("IntroLogoBackdrop", new ColorRGBA(0f, 0f, 0f, 1f));

        startBackdrop = createPanel("IntroStartBackdrop", new ColorRGBA(0f, 0f, 0f, 0.78f));
        startFrame = createPanel("IntroStartFrame", new ColorRGBA(1f, 1f, 1f, 0.08f));
        startDivider = createPanel("IntroStartDivider", new ColorRGBA(1f, 1f, 1f, 0.08f));
        initializeButtonGlow = createPanel("IntroInitializeGlow", new ColorRGBA(0.02f, 0.16f, 0.42f, 0.45f));
        initializeButton = createPanel("IntroInitializeButton", new ColorRGBA(0.02f, 0.1f, 0.26f, 0.96f));
        initializeButton.setUserData("introAction", "initialize");

        startNode.attachChild(startBackdrop);
        startNode.attachChild(startFrame);
        startNode.attachChild(startDivider);
        startNode.attachChild(initializeButtonGlow);
        startNode.attachChild(initializeButton);

        cinematicNode.attachChild(backdrop);
        cinematicNode.attachChild(vignetteTop);
        cinematicNode.attachChild(vignetteBottom);
        cinematicNode.attachChild(progressBar);
        cinematicNode.attachChild(logoBackdrop);

        startEyebrow = new BitmapText(font);
        startEyebrow.setSize(font.getCharSet().getRenderedSize() * 0.92f);
        startEyebrow.setColor(new ColorRGBA(1f, 1f, 1f, 0.25f));
        startEyebrow.setText("A   P S Y C H O L O G I C A L   H O R R O R   E X P E R I E N C E");
        startNode.attachChild(startEyebrow);

        startTitle = createTitleGraphic();
        startNode.attachChild(startTitle);

        connectionText = new BitmapText(font);
        connectionText.setSize(font.getCharSet().getRenderedSize() * 0.9f);
        connectionText.setColor(new ColorRGBA(0.1f, 0.45f, 1f, 0.95f));
        connectionText.setText("E S T A B L I S H I N G   S E C U R E   C O N N E C T I O N . . .");
        startNode.attachChild(connectionText);

        playIcon = new BitmapText(font);
        playIcon.setSize(font.getCharSet().getRenderedSize() * 1.7f);
        playIcon.setColor(ColorRGBA.White);
        playIcon.setText(">");
        startNode.attachChild(playIcon);

        initializeLabel = new BitmapText(font);
        initializeLabel.setSize(font.getCharSet().getRenderedSize() * 1.55f);
        initializeLabel.setColor(ColorRGBA.White);
        initializeLabel.setText("I N I T I A L I Z E");
        startNode.attachChild(initializeLabel);

        headerLeft = new BitmapText(font);
        headerLeft.setSize(font.getCharSet().getRenderedSize() * 0.8f);
        headerLeft.setColor(new ColorRGBA(0.25f, 0.62f, 0.9f, 0.72f));
        headerLeft.setText("SECTOR_7_SUBLEVEL_04\nFACILITY_STATUS: CRITICAL_FAILURE\nRECORDING: SITE_47_LOG");
        cinematicNode.attachChild(headerLeft);

        headerRight = new BitmapText(font);
        headerRight.setSize(font.getCharSet().getRenderedSize() * 0.8f);
        headerRight.setColor(new ColorRGBA(0.8f, 0.82f, 0.86f, 0.55f));
        headerRight.setText("AUTH: TECH_UNIT_884\nSIGNAL  |||.");
        cinematicNode.attachChild(headerRight);

        subtitleText = new BitmapText(font);
        subtitleText.setSize(font.getCharSet().getRenderedSize() * 1.6f);
        subtitleText.setColor(new ColorRGBA(0.88f, 0.9f, 0.95f, 0.92f));
        cinematicNode.attachChild(subtitleText);

        decryptText = new BitmapText(font);
        decryptText.setSize(font.getCharSet().getRenderedSize() * 0.8f);
        decryptText.setColor(new ColorRGBA(0.72f, 0.14f, 0.18f, 0.78f));
        decryptText.setText("PROCESSING DECRYPTION STREAM...");
        cinematicNode.attachChild(decryptText);

        logoText = new BitmapText(font);
        logoText.setSize(font.getCharSet().getRenderedSize() * 4.2f);
        logoText.setColor(ColorRGBA.White);
        logoText.setText("SUBJECT 47");
        cinematicNode.attachChild(logoText);

        guiNode.attachChild(startNode);
        guiNode.attachChild(cinematicNode);
    }

    private Geometry createPanel(String name, ColorRGBA color) {
        Geometry geometry = new Geometry(name, new Quad(1f, 1f));
        geometry.setMaterial(createMaterial(color));
        return geometry;
    }

    private Material createMaterial(ColorRGBA color) {
        Material material = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        if (color.a < 1f) {
            material.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
        }
        return material;
    }

    private Geometry createTitleGraphic() {
        int width = 1600;
        int height = 260;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(0, 0, width, height);
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(loadTitleFont(176f));
        java.awt.FontMetrics fm = graphics.getFontMetrics();
        int textW = fm.stringWidth("SUBJECT 47");
        int textX = (width - textW) / 2;   // horizontally centre in the 1600 px canvas
        graphics.setColor(java.awt.Color.WHITE);
        graphics.drawString("SUBJECT 47", textX, 196);
        graphics.dispose();

        AWTLoader loader = new AWTLoader();
        Texture2D texture = new Texture2D(loader.load(image, true));
        Material material = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", texture);
        material.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);

        Geometry title = new Geometry("IntroTitleGraphic", new Quad(1f, 1f));
        title.setMaterial(material);
        return title;
    }

    private Font loadTitleFont(float size) {
        File fontFile = new File("C:\\Windows\\Fonts\\ariblk.ttf");
        if (fontFile.isFile()) {
            try {
                return Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(size);
            } catch (FontFormatException | IOException ignored) {
                // Fall back to a system face if Arial Black cannot be loaded.
            }
        }
        return new Font("Arial Black", Font.BOLD, Math.round(size));
    }

    private void registerInput() {
        app.getInputManager().addMapping("IntroSkipEnter", new KeyTrigger(KeyInput.KEY_RETURN));
        app.getInputManager().addMapping("IntroSkipSpace", new KeyTrigger(KeyInput.KEY_SPACE));
        app.getInputManager().addMapping("IntroSkipClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(skipListener, "IntroSkipEnter", "IntroSkipSpace", "IntroSkipClick");
    }

    private void handleSkipInput(String name, boolean pressed, float tpf) {
        if (!pressed || !active) {
            return;
        }

        if (phase == Phase.START_SCREEN) {
            if ("IntroSkipClick".equals(name)) {
                activateStartButtonAtCursor();
            } else {
                beginCinematic();
            }
            return;
        }

        if (elapsed > 0.8f) {
            finish();
        }
    }

    private void activateStartButtonAtCursor() {
        CollisionResults results = new CollisionResults();
        Vector2f cursor = app.getInputManager().getCursorPosition();
        Ray ray = new Ray(new Vector3f(cursor.x, cursor.y, 10f), new Vector3f(0f, 0f, -1f));
        initializeButton.collideWith(ray, results);

        for (CollisionResult result : results) {
            String action = result.getGeometry().getUserData("introAction");
            if ("initialize".equals(action)) {
                beginCinematic();
                return;
            }
        }
    }

    private void beginCinematic() {
        phase = Phase.CINEMATIC;
        elapsed = 0f;
        sceneNode.setCullHint(Spatial.CullHint.Never);
        startNode.setCullHint(Spatial.CullHint.Always);
        cinematicNode.setCullHint(Spatial.CullHint.Never);
        app.getInputManager().setCursorVisible(false);
        audioManager.playIntroMusic();
        updateCamera(0f);
        updateGui(0f);
    }

    private void animateScene(float time) {
        float flicker = FastMath.sin(time * 10f) > 0.8f ? 1f : 0.35f + 0.25f * FastMath.sin(time * 45f);
        sceneNode.setLocalScale(1f, 1f + flicker * 0.002f, 1f);
    }

    private void updateStartCamera(float time) {
        float drift = FastMath.sin(time * 0.22f) * 0.45f;
        float push = FastMath.sin(time * 0.16f) * 1.2f;
        app.getCamera().setLocation(new Vector3f(drift, 2.05f + FastMath.sin(time * 0.31f) * 0.05f, 4f + push));
        app.getCamera().lookAt(new Vector3f(0f, 1.55f, -10f), Vector3f.UNIT_Y);
    }

    private void updateCamera(float time) {
        CameraPoint start = CAMERA_PATH[CAMERA_PATH.length - 1];
        CameraPoint end = start;
        float t = 0f;

        for (int i = 0; i < CAMERA_PATH.length - 1; i++) {
            CameraPoint a = CAMERA_PATH[i];
            CameraPoint b = CAMERA_PATH[i + 1];
            if (time >= a.time() && time <= b.time()) {
                start = a;
                end = b;
                t = (time - a.time()) / Math.max(0.001f, b.time() - a.time());
                break;
            }
        }

        float eased = easeInOut(t);
        Vector3f position = interpolate(start.position(), end.position(), eased);
        Vector3f lookAt = interpolate(start.lookAt(), end.lookAt(), eased);
        app.getCamera().setLocation(position);
        app.getCamera().lookAt(lookAt, Vector3f.UNIT_Y);
    }

    private void updateGui(float time) {
        float sw = app.getCamera().getWidth();
        float sh = app.getCamera().getHeight();
        float progress = Math.min(time / DURATION, 1f);

        layoutStartScreen(sw, sh);

        backdrop.setLocalScale(sw, sh, 1f);
        backdrop.setLocalTranslation(0f, 0f, -8f);
        vignetteTop.setLocalScale(sw, sh * 0.16f, 1f);
        vignetteTop.setLocalTranslation(0f, sh * 0.84f, -7f);
        vignetteBottom.setLocalScale(sw, sh * 0.24f, 1f);
        vignetteBottom.setLocalTranslation(0f, 0f, -7f);
        progressBar.setLocalScale(sw * progress, 3f, 1f);
        progressBar.setLocalTranslation(0f, 0f, -5f);

        headerLeft.setLocalTranslation(34f, sh - 34f, 0f);
        headerRight.setLocalTranslation(sw - 240f, sh - 34f, 0f);

        Subtitle subtitle = currentSubtitle(time);
        subtitleText.setText(subtitle == null ? "" : "\"" + subtitle.text() + "\"");
        float subtitleWidth = subtitleText.getLineWidth();
        subtitleText.setLocalTranslation(sw / 2f - subtitleWidth / 2f, sh * 0.27f, 0f);
        decryptText.setLocalTranslation(sw / 2f - 150f, sh * 0.2f, 0f);
        decryptText.setCullHint(subtitle == null ? Spatial.CullHint.Always : Spatial.CullHint.Never);

        boolean showLogo = progress >= 0.9f;
        logoBackdrop.setCullHint(showLogo ? Spatial.CullHint.Never : Spatial.CullHint.Always);
        logoText.setCullHint(showLogo ? Spatial.CullHint.Never : Spatial.CullHint.Always);
        if (showLogo) {
            float alpha = Math.min((progress - 0.9f) / 0.1f, 1f);
            logoBackdrop.setLocalScale(sw, sh, 1f);
            logoBackdrop.setLocalTranslation(0f, 0f, -1f);
            logoText.setColor(new ColorRGBA(1f, 1f, 1f, alpha));
            logoText.setLocalTranslation(sw / 2f - logoText.getLineWidth() / 2f, sh / 2f + logoText.getHeight() / 2f, 1f);
        }
    }

    private void layoutStartScreen(float sw, float sh) {
        float frameW = Math.min(sw * 0.74f, 952f);
        float frameH = Math.max(145f, sh * 0.28f);
        float frameX = (sw - frameW) / 2f;
        float frameY = sh * 0.52f;
        float buttonW = Math.min(400f, sw * 0.42f);
        float buttonH = 92f;
        float buttonX = (sw - buttonW) / 2f;
        float buttonY = sh * 0.17f;

        startBackdrop.setLocalScale(sw, sh, 1f);
        startBackdrop.setLocalTranslation(0f, 0f, -8f);
        startFrame.setLocalScale(frameW, frameH, 1f);
        startFrame.setLocalTranslation(frameX, frameY, -7f);
        startDivider.setLocalScale(frameW * 0.5f, 1f, 1f);
        startDivider.setLocalTranslation((sw - frameW * 0.5f) / 2f, frameY - 32f, -7f);
        initializeButtonGlow.setLocalScale(buttonW + 34f, buttonH + 22f, 1f);
        initializeButtonGlow.setLocalTranslation(buttonX - 17f, buttonY - 11f, -7f);
        initializeButton.setLocalScale(buttonW, buttonH, 1f);
        initializeButton.setLocalTranslation(buttonX, buttonY, -6f);

        startEyebrow.setLocalTranslation(sw / 2f - startEyebrow.getLineWidth() / 2f, frameY + frameH + 18f, 0f);
        float titleW = Math.min(sw * 0.78f, 1030f);
        float titleH = titleW * 260f / 1600f;
        startTitle.setLocalScale(titleW, titleH, 1f);
        startTitle.setLocalTranslation(sw / 2f - titleW / 2f, frameY + frameH * 0.22f, 0f);
        connectionText.setLocalTranslation(sw / 2f - connectionText.getLineWidth() / 2f, buttonY + buttonH + 50f, 0f);
        playIcon.setLocalTranslation(buttonX + buttonW * 0.26f, buttonY + buttonH * 0.65f, 0f);
        initializeLabel.setLocalTranslation(buttonX + buttonW * 0.34f, buttonY + buttonH * 0.65f, 0f);
    }

    private Subtitle currentSubtitle(float time) {
        for (Subtitle subtitle : SUBTITLES) {
            if (time >= subtitle.start() && time <= subtitle.end()) {
                return subtitle;
            }
        }
        return null;
    }

    private Vector3f interpolate(Vector3f start, Vector3f end, float t) {
        return new Vector3f(
                FastMath.interpolateLinear(t, start.x, end.x),
                FastMath.interpolateLinear(t, start.y, end.y),
                FastMath.interpolateLinear(t, start.z, end.z));
    }

    private float easeInOut(float t) {
        return t < 0.5f ? 2f * t * t : 1f - FastMath.pow(-2f * t + 2f, 2f) / 2f;
    }

    private void finish() {
        if (finished) {
            return;
        }
        finished = true;
        active = false;
        audioManager.stopIntroMusic();
        sceneNode.setCullHint(Spatial.CullHint.Always);
        guiNode.setCullHint(Spatial.CullHint.Always);
        onFinished.run();
    }
}
