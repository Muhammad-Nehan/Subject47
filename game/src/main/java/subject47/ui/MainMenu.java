package subject47.ui;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import subject47.Main;

import java.awt.AlphaComposite;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;

public class MainMenu {

    private static final String ACTION_START    = "start";
    private static final String ACTION_CONTROLS = "controls";
    private static final String ACTION_BACK     = "back";
    private static final String ACTION_EXIT     = "exit";

    // Sci-fi colour palette
    private static final ColorRGBA COL_BG        = new ColorRGBA(0f,    0.01f, 0.04f, 0.93f);
    private static final ColorRGBA COL_PANEL     = new ColorRGBA(0.02f, 0.06f, 0.12f, 0.88f);
    private static final ColorRGBA COL_ACCENT    = new ColorRGBA(0.0f,  0.7f,  1.0f,  0.18f);
    private static final ColorRGBA COL_BORDER    = new ColorRGBA(0.0f,  0.6f,  0.9f,  0.35f);
    private static final ColorRGBA COL_BTN_MAIN  = new ColorRGBA(0.0f,  0.55f, 0.9f,  0.85f);
    private static final ColorRGBA COL_BTN_SEC   = new ColorRGBA(0.04f, 0.18f, 0.32f, 0.88f);
    private static final ColorRGBA COL_BTN_QUIT  = new ColorRGBA(0.28f, 0.05f, 0.05f, 0.88f);
    private static final ColorRGBA COL_SCANLINE  = new ColorRGBA(0.0f,  0.8f,  1.0f,  0.06f);
    private static final ColorRGBA COL_RED_ALERT = new ColorRGBA(0.9f,  0.12f, 0.12f, 0.85f);

    private final Main app;
    private final ActionListener menuListener;
    private final Node menuNode   = new Node("MainMenu");
    private final Node buttonNode = new Node("MenuButtons");

    private BitmapFont font;

    // Background layers
    private Geometry bgFull;
    private Geometry bgPanel;
    private Geometry bgPanelBorder;
    private Geometry topBar;
    private Geometry bottomBar;
    private Geometry scanline;

    // Corner bracket lines (4 corners × 2 lines each)
    private Geometry[] cornerH = new Geometry[4];
    private Geometry[] cornerV = new Geometry[4];

    // Title graphic
    private Geometry titleGraphic;
    private BitmapText eyebrow;
    private BitmapText classifiedLabel;
    private Geometry classifiedLineL;
    private Geometry classifiedLineR;

    // Status strip
    private BitmapText statusLeft;
    private BitmapText statusRight;

    // Buttons
    private Geometry startBtn;
    private Geometry controlsBtn;
    private Geometry backBtn;
    private Geometry exitBtn;
    private BitmapText startLabel;
    private BitmapText startSub;
    private BitmapText controlsLabel;
    private BitmapText controlsSub;
    private BitmapText backLabel;
    private BitmapText backSub;
    private BitmapText exitLabel;
    private BitmapText exitSub;
    private Geometry startGlow;

    // Controls / body text
    private BitmapText body;

    private boolean visible;
    private boolean controlsVisible;

    // Input grace period – blocks menu input for a short time after the menu
    // becomes visible, preventing a simultaneous key press (e.g. ENTER skipping
    // the intro) from also immediately triggering a menu action.
    private float inputGrace = 0f;

    // Scanline animation
    private float scanY = 0f;
    private float scanTimer = 0f;

    public MainMenu(Main app) {
        this.app = app;
        this.menuListener = this::handleMenuInput;
    }

    // ── Initialise 

    public void initialize() {
        font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        buildBackground();
        buildCornerBrackets();
        buildTitle();
        buildStatusStrip();
        buildButtons();
        buildBodyText();

        menuNode.attachChild(buttonNode);
        app.getGuiNode().attachChild(menuNode);
        registerInput();
        showMainScreen();
    }

    // ── Background layers 

    private void buildBackground() {
        bgFull        = quad("MenuBg",          COL_BG,       -10f);
        bgPanel       = quad("MenuPanel",        COL_PANEL,    -9f);
        bgPanelBorder = quad("MenuPanelBorder",  COL_BORDER,   -9.5f);
        topBar        = quad("MenuTopBar",        COL_ACCENT,   -8f);
        bottomBar     = quad("MenuBottomBar",     COL_ACCENT,   -8f);
        scanline      = quad("MenuScanline",      COL_SCANLINE, -7f);

        menuNode.attachChild(bgFull);
        menuNode.attachChild(bgPanel);
        menuNode.attachChild(bgPanelBorder);
        menuNode.attachChild(topBar);
        menuNode.attachChild(bottomBar);
        menuNode.attachChild(scanline);
    }

    // ── Corner brackets (HUD-style)

    private void buildCornerBrackets() {
        ColorRGBA bc = new ColorRGBA(0f, 0.75f, 1f, 0.55f);
        for (int i = 0; i < 4; i++) {
            cornerH[i] = quad("CornerH" + i, bc, -6f);
            cornerV[i] = quad("CornerV" + i, bc, -6f);
            menuNode.attachChild(cornerH[i]);
            menuNode.attachChild(cornerV[i]);
        }
    }

    // ── Title 

    private void buildTitle() {
        titleGraphic = createTitleGraphic();
        menuNode.attachChild(titleGraphic);

        eyebrow = bitmapText("A   P S Y C H O L O G I C A L   H O R R O R   E X P E R I E N C E",
                0.82f, new ColorRGBA(1f, 1f, 1f, 0.22f));
        menuNode.attachChild(eyebrow);

        classifiedLabel = bitmapText("C L A S S I F I E D  —  E Y E S  O N L Y",
                0.78f, new ColorRGBA(0.9f, 0.15f, 0.15f, 0.75f));
        menuNode.attachChild(classifiedLabel);

        classifiedLineL = quad("ClassLineL", new ColorRGBA(0.8f, 0.1f, 0.1f, 0.45f), -6f);
        classifiedLineR = quad("ClassLineR", new ColorRGBA(0.8f, 0.1f, 0.1f, 0.45f), -6f);
        menuNode.attachChild(classifiedLineL);
        menuNode.attachChild(classifiedLineR);
    }

    // ── Status strip 

    private void buildStatusStrip() {
        statusLeft = bitmapText(
                "SECTOR_7  SUBLEVEL_04\nFACILITY_STATUS: CRITICAL_FAILURE\n[REC]  SITE_47_LOG",
                0.75f, new ColorRGBA(0.1f, 0.65f, 0.95f, 0.65f));
        menuNode.attachChild(statusLeft);

        statusRight = bitmapText(
                "AUTH: TECH_UNIT_884\nSIGNAL  ||| .",
                0.75f, new ColorRGBA(0.75f, 0.8f, 0.88f, 0.45f));
        menuNode.attachChild(statusRight);
    }

    // ── Menu Buttons 

    private void buildButtons() {
        startGlow = quad("StartGlow", new ColorRGBA(0f, 0.55f, 0.9f, 0.22f), -7.5f);
        buttonNode.attachChild(startGlow);

        startBtn    = actionQuad("StartButton",    COL_BTN_MAIN, ACTION_START);
        controlsBtn = actionQuad("ControlsButton", COL_BTN_SEC,  ACTION_CONTROLS);
        backBtn     = actionQuad("BackButton",      COL_BTN_QUIT, ACTION_BACK);
        exitBtn     = actionQuad("ExitButton",
                new ColorRGBA(0.22f, 0.04f, 0.04f, 0.92f), ACTION_EXIT);

        startLabel    = bitmapText("INITIALIZE PROTOCOL", 1.35f, ColorRGBA.White);
        startSub      = bitmapText("Begin classified sequence", 0.78f,
                new ColorRGBA(0.7f, 0.9f, 1f, 0.55f));
        controlsLabel = bitmapText("CONTROLS", 1.2f, new ColorRGBA(0.6f, 0.85f, 1f, 0.92f));
        controlsSub   = bitmapText("Key bindings & reference", 0.78f,
                new ColorRGBA(0.5f, 0.75f, 0.95f, 0.5f));
        backLabel     = bitmapText("QUIT", 1.2f, new ColorRGBA(1f, 0.35f, 0.35f, 0.92f));
        backSub       = bitmapText("Exit to desktop", 0.78f,
                new ColorRGBA(0.9f, 0.4f, 0.4f, 0.45f));
        exitLabel     = bitmapText("EXIT GAME", 1.2f, new ColorRGBA(1f, 0.25f, 0.25f, 0.92f));
        exitSub       = bitmapText("Quit to desktop", 0.78f,
                new ColorRGBA(0.9f, 0.3f, 0.3f, 0.45f));

        buttonNode.attachChild(startLabel);
        buttonNode.attachChild(startSub);
        buttonNode.attachChild(controlsLabel);
        buttonNode.attachChild(controlsSub);
        buttonNode.attachChild(backLabel);
        buttonNode.attachChild(backSub);
        buttonNode.attachChild(exitLabel);
        buttonNode.attachChild(exitSub);
    }

    // ── Controls / body text 

    private void buildBodyText() {
        body = bitmapText("", 1.0f, new ColorRGBA(0.78f, 0.88f, 1f, 0.82f));
        menuNode.attachChild(body);
    }

    // ── Layout (called every frame while visible)

    private void layout() {
        float sw = app.getCamera().getWidth();
        float sh = app.getCamera().getHeight();

        // Full-screen bg
        place(bgFull, 0, 0, sw, sh);

        // Central panel (narrow column)
        float panelW = Math.min(sw * 0.62f, 820f);
        float panelX = (sw - panelW) / 2f;
        float panelY = sh * 0.07f;
        float panelH = sh * 0.86f;
        // 1-px border trick: slightly larger quad behind
        place(bgPanelBorder, panelX - 1, panelY - 1, panelW + 2, panelH + 2);
        place(bgPanel, panelX, panelY, panelW, panelH);

        // Top / bottom accent bars
        place(topBar,    panelX, panelY + panelH - 3f, panelW, 3f);
        place(bottomBar, panelX, panelY,               panelW, 3f);

        // Scanline sweep
        scanY = (sh + 6f) - (scanTimer % 3f) / 3f * (sh + 6f);
        place(scanline, 0, scanY, sw, 6f);

        // ── Corner brackets – pass exact corner point + orientation flags ──
        float bm = 24f;  // inset from panel edge
        float bl = 28f;  // arm length
        float bt = 2f;   // arm thickness
        float cxL = panelX + bm;           // left corner x
        float cxR = panelX + panelW - bm;  // right corner x
        float cyT = panelY + panelH - bm;  // top corner y
        float cyB = panelY + bm;           // bottom corner y
        placeCorner(0, cxL, cyT, bl, bt, true,  true);   // top-left
        placeCorner(1, cxR, cyT, bl, bt, true,  false);  // top-right
        placeCorner(2, cxL, cyB, bl, bt, false, true);   // bottom-left
        placeCorner(3, cxR, cyB, bl, bt, false, false);  // bottom-right

        // ── Title (upper-centre of panel)
        float titleW = Math.min(panelW * 0.88f, 710f);
        float titleH = titleW * 260f / 1600f;
        // Bottom of title sits at 58 % up the panel height
        float titleY = panelY + panelH * 0.58f;
        // Perfectly centred on screen
        float titleX = sw / 2f - titleW / 2f;
        titleGraphic.setLocalScale(titleW, titleH, 1f);
        titleGraphic.setLocalTranslation(titleX, titleY, 0f);

        // Eyebrow -- centred above the title
        eyebrow.setLocalTranslation(
                sw / 2f - eyebrow.getLineWidth() / 2f,
                titleY + titleH + 12f, 0f);

        // "Classified" label -- centred below the title
        float clY = titleY - 28f;
        classifiedLabel.setLocalTranslation(
                sw / 2f - classifiedLabel.getLineWidth() / 2f,
                clY, 0f);
        float lineW = 72f;
        place(classifiedLineL,
                sw / 2f - classifiedLabel.getLineWidth() / 2f - lineW - 10f,
                clY - 7f, lineW, 1.5f);
        place(classifiedLineR,
                sw / 2f + classifiedLabel.getLineWidth() / 2f + 10f,
                clY - 7f, lineW, 1.5f);

        // Status strip – statusLeft shifted right of the corner bracket (bracket ends at panelX+52)
        statusLeft.setLocalTranslation(panelX + 60f, panelY + panelH - 16f, 0f);
        statusRight.setLocalTranslation(
                panelX + panelW - statusRight.getLineWidth() - 14f,
                panelY + panelH - 16f, 0f);

        if (!controlsVisible) {
            layoutMainButtons(sw, sh, panelX, panelY, panelW, panelH);
            body.setCullHint(Spatial.CullHint.Always);
        } else {
            layoutControlsBody(sw, sh, panelX, panelY, panelW, panelH);
        }
    }

    private void layoutMainButtons(float sw, float sh,
                                   float panelX, float panelY, float panelW, float panelH) {
        float btnW = Math.min(panelW * 0.72f, 520f);
        float btnX = (sw - btnW) / 2f;
        boolean paused = app.hasStartedGame();

        // Use slightly smaller buttons when 4 are showing (pause state)
        float btnH = paused ? 60f : 66f;
        float gap  = paused ? 10f : 12f;

        float y3 = panelY + panelH * 0.10f;  // lowest (exit / quit)
        float y2 = y3 + btnH + gap;           // back / return to lab
        float y1 = y2 + btnH + gap;           // controls
        float y0 = y1 + btnH + gap;           // top: initialize

        // If not paused: collapse y3 into y2 (only 3 buttons)
        if (!paused) {
            y2 = panelY + panelH * 0.12f;
            y1 = y2 + btnH + gap;
            y0 = y1 + btnH + gap;
        }

        place(startGlow, btnX - 10f, y0 - 8f, btnW + 20f, btnH + 16f);
        startGlow.setCullHint(Spatial.CullHint.Never);

        placeBtn(startBtn,    btnX, y0, btnW, btnH);
        placeBtn(controlsBtn, btnX, y1, btnW, btnH);
        placeBtn(backBtn,     btnX, y2, btnW, btnH);

        float lp = btnX + 24f;
        startLabel.setLocalTranslation(lp, y0 + btnH * 0.68f, 1f);
        startSub.setLocalTranslation(  lp, y0 + btnH * 0.32f, 1f);
        controlsLabel.setLocalTranslation(lp, y1 + btnH * 0.68f, 1f);
        controlsSub.setLocalTranslation(  lp, y1 + btnH * 0.32f, 1f);
        backLabel.setLocalTranslation(    lp, y2 + btnH * 0.68f, 1f);
        backSub.setLocalTranslation(      lp, y2 + btnH * 0.32f, 1f);

        if (paused) {
            placeBtn(exitBtn, btnX, y3, btnW, btnH);
            exitLabel.setLocalTranslation(lp, y3 + btnH * 0.68f, 1f);
            exitSub.setLocalTranslation(  lp, y3 + btnH * 0.32f, 1f);
            exitBtn.setCullHint(Spatial.CullHint.Never);
            exitLabel.setCullHint(Spatial.CullHint.Never);
            exitSub.setCullHint(Spatial.CullHint.Never);
        } else {
            exitBtn.setCullHint(Spatial.CullHint.Always);
            exitLabel.setCullHint(Spatial.CullHint.Always);
            exitSub.setCullHint(Spatial.CullHint.Always);
        }

        for (Spatial s : new Spatial[]{ startBtn, controlsBtn, backBtn,
                startLabel, startSub, controlsLabel, controlsSub, backLabel, backSub }) {
            s.setCullHint(Spatial.CullHint.Never);
        }
    }

    private void layoutControlsBody(float sw, float sh,
                                    float panelX, float panelY,
                                    float panelW, float panelH) {
        for (Spatial s : new Spatial[]{ startGlow, startBtn, controlsBtn, backBtn,
                startLabel, startSub, controlsLabel, controlsSub, backLabel, backSub,
                exitBtn, exitLabel, exitSub }) {
            s.setCullHint(Spatial.CullHint.Always);
        }
        body.setCullHint(Spatial.CullHint.Never);
        body.setLocalTranslation(panelX + 48f, panelY + panelH * 0.64f, 0f);
    }

    // ── Builder helpers 

    private Geometry quad(String name, ColorRGBA color, float z) {
        Geometry g = new Geometry(name, new Quad(1f, 1f));
        g.setMaterial(mat(color));
        g.setLocalTranslation(0f, 0f, z);
        menuNode.attachChild(g);
        return g;
    }

    private Geometry actionQuad(String name, ColorRGBA color, String action) {
        Geometry g = new Geometry(name, new Quad(1f, 1f));
        g.setMaterial(mat(color));
        g.setUserData("menuAction", action);
        buttonNode.attachChild(g);
        return g;
    }

    private BitmapText bitmapText(String text, float sizeMult, ColorRGBA color) {
        BitmapText t = new BitmapText(font);
        t.setSize(font.getCharSet().getRenderedSize() * sizeMult);
        t.setColor(color);
        t.setText(text);
        return t;
    }

    private Material mat(ColorRGBA color) {
        Material m = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", color);
        if (color.a < 1f) {
            m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        }
        return m;
    }

    private void place(Geometry g, float x, float y, float w, float h) {
        g.setLocalScale(w, h, 1f);
        float z = g.getLocalTranslation().z;
        g.setLocalTranslation(x, y, z);
    }

    private void placeBtn(Geometry g, float x, float y, float w, float h) {
        g.setLocalScale(w, h, 1f);
        g.setLocalTranslation(x, y, -6f);
    }

    /**
     * Draw one L-shaped corner bracket.
     * cx,cy = exact corner point (where the two arms meet).
     * top   = true → arms point downward  (top corners)
     * top   = false → arms point upward   (bottom corners)
     * left  = true → arms point rightward (left corners)
     * left  = false → arms point leftward (right corners)
     */
    private void placeCorner(int idx, float cx, float cy,
                             float len, float thick,
                             boolean top, boolean left) {
        // Horizontal arm
        float hx = left ? cx : cx - len;           // left corners start at cx; right corners end at cx
        float hy = top  ? cy - thick : cy;          // top corners hang below cy; bottom corners sit above cy
        cornerH[idx].setLocalScale(len, thick, 1f);
        cornerH[idx].setLocalTranslation(hx, hy, -5f);

        // Vertical arm
        float vx = left ? cx : cx - thick;          // align with horizontal arm's left or right edge
        float vy = top  ? cy - len : cy;            // top corners hang down from cy; bottom corners rise from cy
        cornerV[idx].setLocalScale(thick, len, 1f);
        cornerV[idx].setLocalTranslation(vx, vy, -5f);
    }

    // ── Title graphic (rendered via Java2D → Texture2D) 

    private Geometry createTitleGraphic() {
        int w = 1600, h = 260;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, w, h);
        g.setComposite(AlphaComposite.SrcOver);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(loadTitleFont(172f));
        java.awt.FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth("SUBJECT 47");
        int textX = (w - textW) / 2;   // horizontally centre in the 1600px canvas
        // Cyan glow layer (two offset passes)
        g.setColor(new java.awt.Color(0, 180, 255, 55));
        g.drawString("SUBJECT 47", textX - 6, 198);
        g.drawString("SUBJECT 47", textX + 6, 198);
        // White main text
        g.setColor(java.awt.Color.WHITE);
        g.drawString("SUBJECT 47", textX, 196);
        g.dispose();

        AWTLoader loader = new AWTLoader();
        Texture2D tex = new Texture2D(loader.load(img, true));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", tex);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        Geometry geo = new Geometry("MenuTitleGraphic", new Quad(1f, 1f));
        geo.setMaterial(mat);
        return geo;
    }

    private Font loadTitleFont(float size) {
        File f = new File("C:\\Windows\\Fonts\\ariblk.ttf");
        if (f.isFile()) {
            try { return Font.createFont(Font.TRUETYPE_FONT, f).deriveFont(size); }
            catch (FontFormatException | IOException ignored) {}
        }
        return new Font("Arial Black", Font.BOLD, Math.round(size));
    }

    // ── Input 

    private void registerInput() {
        app.getInputManager().addMapping("MenuAccept",   new KeyTrigger(KeyInput.KEY_RETURN));
        app.getInputManager().addMapping("MenuControls", new KeyTrigger(KeyInput.KEY_C));
        app.getInputManager().addMapping("MenuBack",     new KeyTrigger(KeyInput.KEY_ESCAPE));
        app.getInputManager().addMapping("MenuClick",    new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(menuListener, "MenuAccept", "MenuControls", "MenuBack", "MenuClick");
    }

    private void handleMenuInput(String name, boolean pressed, float tpf) {
        if (!pressed) return;
        if (inputGrace > 0f) return;
        if (!visible && app.getUi().isKeypadOpen()) return;
        switch (name) {
            case "MenuClick"    -> { if (visible) activateButtonAtCursor(); }
            case "MenuBack"     -> performAction(ACTION_BACK);
            case "MenuControls" -> performAction(ACTION_CONTROLS);
            case "MenuAccept"   -> { if (visible && !controlsVisible) performAction(ACTION_START); }
        }
    }

    private void activateButtonAtCursor() {
        CollisionResults results = new CollisionResults();
        Vector2f cursor = app.getInputManager().getCursorPosition();
        Ray ray = new Ray(new Vector3f(cursor.x, cursor.y, 10f), new Vector3f(0f, 0f, -1f));
        buttonNode.collideWith(ray, results);
        for (CollisionResult r : results) {
            String action = r.getGeometry().getUserData("menuAction");
            if (action != null) { performAction(action); return; }
        }
    }

    private void performAction(String action) {
        if (ACTION_CONTROLS.equals(action)) {
            if (!visible) return;
            if (controlsVisible) showMainScreen(); else showControlsScreen();
            return;
        }
        if (ACTION_EXIT.equals(action)) {
            app.stop();
            return;
        }
        if (ACTION_BACK.equals(action)) {
            if (visible && controlsVisible)        { showMainScreen(); return; }
            if (visible && !app.hasStartedGame())  { app.stop(); return; }
            if (visible)                           { hide(); app.resumeGame(); return; }
            app.pauseToMenu();
            return;
        }
        if (!visible || controlsVisible) return;
        if (ACTION_START.equals(action)) app.startGame();
    }

    // ── Public screen states 

    public void showMainScreen() {
        controlsVisible = false;
        visible = true;
        inputGrace = 0.2f;   // block input for 200 ms so the key that triggered this doesn't also activate a button
        startLabel.setText("INITIALIZE PROTOCOL");
        startSub.setText(app.hasStartedGame() ? "Restart the experiment" : "Begin classified sequence");
        backLabel.setText(app.hasStartedGame() ? "RETURN TO LAB" : "QUIT");
        backSub.setText(app.hasStartedGame()   ? "Resume mission"  : "Exit to desktop");
        exitLabel.setText("EXIT GAME");
        exitSub.setText("Quit to desktop");
        body.setText("");
        menuNode.setCullHint(Spatial.CullHint.Never);
        app.getInputManager().setCursorVisible(true);
        app.getAudio().playMenuMusic();
        layout();
    }

    public void showControlsScreen() {
        controlsVisible = true;
        visible = true;
        body.setText(
                "\n\n\n\n\n\n\n\n\n"+
                "W  A  S  D        Move\n" +
                "Hold LMB        Look around\n" +
                "E                       Interact\n" +
                "0 – 9                   Enter keypad digits\n" +
                "ENTER              Use default keypad button\n" +
                "BACKSPACE    Delete keypad digit\n" +
                "ESC                  Back / pause menu\n" +
                "[ C ]                  Toggle controls screen"
        );
        menuNode.setCullHint(Spatial.CullHint.Never);
        app.getInputManager().setCursorVisible(true);
        layout();
    }

    public void hide() {
        visible = false;
        controlsVisible = false;
        app.getAudio().stopMenuMusic();   // ensure music stops when leaving the menu
        menuNode.setCullHint(Spatial.CullHint.Always);
    }

    public boolean isVisible() { return visible; }

    // ── Per-frame update (scanline animation)

    public void update(float tpf) {
        if (!visible) return;
        if (inputGrace > 0f) inputGrace -= tpf;
        scanTimer += tpf;
        layout();
    }
}
