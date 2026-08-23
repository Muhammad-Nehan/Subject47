package subject47.ui;

import java.util.function.Consumer;
import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;

public class UIManager {

    private final SimpleApplication app;
    private final ActionListener keypadListener;
    private final ActionListener modalListener;
    private final Node hudNode = new Node("Hud");
    private final Node modalNode = new Node("Modal");
    private final Node modalChromeNode = new Node("ModalChrome");

    private BitmapText crosshair;
    private BitmapText prompt;
    private BitmapText objective;
    private BitmapText status;
    private BitmapText message;
    private BitmapText editorOverlay;
    private BitmapText keypadTitle;
    private BitmapText keypadInput;
    private BitmapText keypadHint;
    private BitmapText modalEyebrow;
    private BitmapText modalTitle;
    private BitmapText modalBody;
    private BitmapText modalFooter;
    private BitmapText modalSidebar;
    private Geometry modalBackdrop;
    private Geometry modalPanel;
    private Geometry modalSidebarPanel;
    private Geometry modalAccentTop;
    private Geometry modalAccentBottom;
    private Geometry modalFooterPanel;

    private float messageTimer;
    private float modalInputDelay;
    private boolean keypadOpen;
    private boolean infoModalOpen;
    private Consumer<String> keypadSubmit;
    private Runnable infoModalContinue;
    private final StringBuilder keypadBuffer = new StringBuilder();

    public UIManager(SimpleApplication app) {
        this.app = app;
        this.keypadListener = this::handleKeypadInput;
        this.modalListener = this::handleModalInput;
    }

    public void initialize() {
        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        modalBackdrop = createPanel("ModalBackdrop", new ColorRGBA(0.02f, 0.03f, 0.05f, 0.92f));
        modalPanel = createPanel("ModalPanel", new ColorRGBA(0.08f, 0.1f, 0.13f, 0.97f));
        modalSidebarPanel = createPanel("ModalSidebarPanel", new ColorRGBA(0.12f, 0.16f, 0.2f, 0.95f));
        modalAccentTop = createPanel("ModalAccentTop", new ColorRGBA(0.15f, 0.78f, 0.98f, 0.95f));
        modalAccentBottom = createPanel("ModalAccentBottom", new ColorRGBA(0.72f, 0.12f, 0.18f, 0.95f));
        modalFooterPanel = createPanel("ModalFooterPanel", new ColorRGBA(0.12f, 0.14f, 0.18f, 0.94f));
        modalChromeNode.attachChild(modalBackdrop);
        modalChromeNode.attachChild(modalPanel);
        modalChromeNode.attachChild(modalSidebarPanel);
        modalChromeNode.attachChild(modalAccentTop);
        modalChromeNode.attachChild(modalAccentBottom);
        modalChromeNode.attachChild(modalFooterPanel);
        modalNode.attachChild(modalChromeNode);

        crosshair = new BitmapText(font);
        crosshair.setSize(font.getCharSet().getRenderedSize() * 2.5f);
        crosshair.setText("+");
        centerCrosshair();
        hudNode.attachChild(crosshair);

        prompt = new BitmapText(font);
        prompt.setLocalTranslation(520, 320, 0);
        hudNode.attachChild(prompt);

        objective = new BitmapText(font);
        objective.setLocalTranslation(30, 690, 0);
        hudNode.attachChild(objective);

        status = new BitmapText(font);
        status.setLocalTranslation(30, 650, 0);
        hudNode.attachChild(status);

        message = new BitmapText(font);
        message.setLocalTranslation(30, 120, 0);
        hudNode.attachChild(message);

        editorOverlay = new BitmapText(font);
        editorOverlay.setLocalTranslation(30, 220, 0);
        hudNode.attachChild(editorOverlay);

        keypadTitle = new BitmapText(font);
        keypadTitle.setLocalTranslation(440, 420, 0);
        modalNode.attachChild(keypadTitle);

        keypadInput = new BitmapText(font);
        keypadInput.setLocalTranslation(520, 380, 0);
        modalNode.attachChild(keypadInput);

        keypadHint = new BitmapText(font);
        keypadHint.setSize(font.getCharSet().getRenderedSize() * 1.05f);
        keypadHint.setText("0-9 enter code  Backspace delete  Enter confirm  Esc cancel");
        modalNode.attachChild(keypadHint);

        modalEyebrow = new BitmapText(font);
        modalEyebrow.setSize(font.getCharSet().getRenderedSize() * 1.0f);
        modalNode.attachChild(modalEyebrow);

        modalTitle = new BitmapText(font);
        modalTitle.setSize(font.getCharSet().getRenderedSize() * 2.3f);
        modalNode.attachChild(modalTitle);

        modalBody = new BitmapText(font);
        modalBody.setSize(font.getCharSet().getRenderedSize() * 1.15f);
        modalNode.attachChild(modalBody);

        modalFooter = new BitmapText(font);
        modalFooter.setSize(font.getCharSet().getRenderedSize() * 1.0f);
        modalNode.attachChild(modalFooter);

        modalSidebar = new BitmapText(font);
        modalSidebar.setSize(font.getCharSet().getRenderedSize() * 1.0f);
        modalNode.attachChild(modalSidebar);

        app.getGuiNode().attachChild(hudNode);
        app.getGuiNode().attachChild(modalNode);

        setPrompt(null);
        setHudVisible(false);
        closeKeypad();
        registerKeypadInput();
        registerModalInput();
        layoutModal();
    }

    private void registerKeypadInput() {
        // KEY_0..KEY_9 are NOT sequential in JME (KEY_0=11, KEY_1=2..KEY_9=10).
        // Must map each digit explicitly.
        int[] digitKeys = {
            KeyInput.KEY_0, KeyInput.KEY_1, KeyInput.KEY_2, KeyInput.KEY_3, KeyInput.KEY_4,
            KeyInput.KEY_5, KeyInput.KEY_6, KeyInput.KEY_7, KeyInput.KEY_8, KeyInput.KEY_9
        };
        int[] numpadKeys = {
            KeyInput.KEY_NUMPAD0, KeyInput.KEY_NUMPAD1, KeyInput.KEY_NUMPAD2,
            KeyInput.KEY_NUMPAD3, KeyInput.KEY_NUMPAD4, KeyInput.KEY_NUMPAD5,
            KeyInput.KEY_NUMPAD6, KeyInput.KEY_NUMPAD7, KeyInput.KEY_NUMPAD8,
            KeyInput.KEY_NUMPAD9
        };
        for (int d = 0; d <= 9; d++) {
            app.getInputManager().addMapping("KPDigit" + d,
                    new KeyTrigger(digitKeys[d]),
                    new KeyTrigger(numpadKeys[d]));
        }
        app.getInputManager().addMapping("KeypadSubmit", new KeyTrigger(KeyInput.KEY_RETURN),
                                                         new KeyTrigger(KeyInput.KEY_NUMPADENTER));
        app.getInputManager().addMapping("KeypadBack",   new KeyTrigger(KeyInput.KEY_BACK),
                                                         new KeyTrigger(KeyInput.KEY_DELETE));
        // ESC closes the keypad (safe — JME's default ESC-quit is removed in Main.simpleInitApp).
        app.getInputManager().addMapping("KeypadCancel", new KeyTrigger(KeyInput.KEY_ESCAPE));

        String[] digitNames = new String[10];
        for (int d = 0; d <= 9; d++) digitNames[d] = "KPDigit" + d;
        app.getInputManager().addListener(keypadListener,
                "KPDigit0","KPDigit1","KPDigit2","KPDigit3","KPDigit4",
                "KPDigit5","KPDigit6","KPDigit7","KPDigit8","KPDigit9",
                "KeypadSubmit", "KeypadBack", "KeypadCancel");
    }

    private void handleKeypadInput(String name, boolean pressed, float tpf) {
        if (!pressed || !keypadOpen) return;

        if (name.startsWith("KPDigit") && keypadBuffer.length() < 4) {
            keypadBuffer.append(name.charAt(name.length() - 1));
            refreshKeypad();
            return;
        }

        switch (name) {
            case "KeypadBack" -> {
                if (keypadBuffer.length() > 0) {
                    keypadBuffer.deleteCharAt(keypadBuffer.length() - 1);
                    refreshKeypad();
                }
            }
            case "KeypadSubmit" -> {
                if (keypadBuffer.length() == 4 && keypadSubmit != null) {
                    String code = keypadBuffer.toString();
                    Consumer<String> submit = keypadSubmit;
                    closeKeypad();
                    submit.accept(code);
                }
            }
            case "KeypadCancel" -> {
                closeKeypad();
                showMessage("Keypad cancelled.", 1.5f);
            }
        }
    }

    private void registerModalInput() {
        app.getInputManager().addMapping("ModalContinueEnter", new KeyTrigger(KeyInput.KEY_RETURN));
        app.getInputManager().addMapping("ModalContinueUse", new KeyTrigger(KeyInput.KEY_E));
        app.getInputManager().addMapping("ModalContinueSpace", new KeyTrigger(KeyInput.KEY_SPACE));
        app.getInputManager().addListener(modalListener,
                "ModalContinueEnter", "ModalContinueUse", "ModalContinueSpace");
    }

    private void handleModalInput(String name, boolean pressed, float tpf) {
        if (!pressed || !infoModalOpen || keypadOpen || modalInputDelay > 0f) {
            return;
        }
        Runnable continueAction = infoModalContinue;
        closeInfoModal();
        if (continueAction != null) {
            continueAction.run();
        }
    }

    public void update(float tpf) {
        if (messageTimer > 0f) {
            messageTimer -= tpf;
            if (messageTimer <= 0f) {
                message.setText("");
            }
        }
        if (modalInputDelay > 0f) {
            modalInputDelay -= tpf;
        }
        centerCrosshair();
        // Hide the crosshair whenever a modal (objectives, keypad, narrative) is open.
        crosshair.setCullHint(isOverlayBlockingInput()
                ? Spatial.CullHint.Always
                : Spatial.CullHint.Inherit);
        layoutModal();
    }

    /**
     * Keeps the crosshair pixel-perfect at the screen centre.
     * BitmapText y = top-left corner in GuiNode (Y up from bottom).
     * Text renders downward, so: centreY = y - height/2  →  y = centreY + height/2.
     */
    private void centerCrosshair() {
        float sw = app.getCamera().getWidth();
        float sh = app.getCamera().getHeight();
        float cw = crosshair.getLineWidth();
        float ch = crosshair.getHeight();
        crosshair.setLocalTranslation(sw / 2f - cw / 2f, sh / 2f + ch / 2f, 0f);
    }

    public void setHudVisible(boolean visible) {
        hudNode.setCullHint(visible ? Spatial.CullHint.Never : Spatial.CullHint.Always);
        if (!visible) {
            setPrompt(null);
        }
    }

    public void setPrompt(String text) {
        prompt.setText(text == null || keypadOpen ? "" : text);
    }

    public void setObjective(String text) {
        objective.setText(text == null ? "" : "Objective: " + text);
    }

    public void setStatus(String text) {
        status.setText(text == null ? "" : text);
    }

    public void showMessage(String text, float seconds) {
        message.setText(text);
        messageTimer = seconds;
    }

    public void showNarration(String text, float seconds) {
        message.setText(text);
        messageTimer = seconds;
    }

    public void setEditorOverlay(String text) {
        editorOverlay.setText(text == null ? "" : text);
    }

    public void openKeypad(String title, Consumer<String> submit) {
        keypadOpen = true;
        keypadSubmit = submit;
        keypadBuffer.setLength(0);
        keypadTitle.setText(title);
        refreshKeypad();
        keypadHint.setText("0-9 enter code  Backspace delete  Enter confirm  Esc cancel");
        modalEyebrow.setText("SECURE ACCESS");
        modalTitle.setText("");
        modalBody.setText("");
        modalFooter.setText("");
        modalSidebar.setText("AUTH\n\nAwaiting valid\n4-digit code");
        modalNode.setCullHint(Spatial.CullHint.Never);
        setPrompt(null);
        hideInfoModalText();
    }

    public void closeKeypad() {
        keypadOpen = false;
        keypadSubmit = null;
        keypadBuffer.setLength(0);
        keypadTitle.setText("");
        keypadInput.setText("");
        if (!infoModalOpen) {
            modalNode.setCullHint(Spatial.CullHint.Always);
        }
    }

    public boolean isKeypadOpen() {
        return keypadOpen;
    }

    public void openInfoModal(String title, String body, String footer, Runnable onContinue) {
        infoModalOpen = true;
        modalInputDelay = 0.2f;
        infoModalContinue = onContinue;
        modalEyebrow.setText(resolveEyebrow(title));
        modalTitle.setText(title == null ? "" : title);
        modalBody.setText(body == null ? "" : body);
        modalFooter.setText(footer == null ? "Press Enter to continue" : footer);
        modalSidebar.setText(resolveSidebar(title, body));
        hideKeypadText();
        modalNode.setCullHint(Spatial.CullHint.Never);
        setPrompt(null);
    }

    /**
     * Opens a multi-page narrative sequence. Each page advances with Enter/E/Space.
     * Sidebar, eyebrow, title, and body can all vary per page.
     * After the last page, {@code onComplete} is invoked.
     */
    public void openNarrativeSequence(
            String[] titles,
            String[] bodies,
            String[] eyebrows,
            String[] sidebars,
            Runnable onComplete) {
        openNarrativePage(titles, bodies, eyebrows, sidebars, onComplete, 0);
    }

    private void openNarrativePage(
            String[] titles, String[] bodies,
            String[] eyebrows, String[] sidebars,
            Runnable onComplete, int index) {
        boolean isLast = index >= titles.length - 1;
        int total = titles.length;
        String pageFooter = isLast
                ? "Press Enter or E to continue"
                : "Press Enter or E to continue  [" + (index + 1) + " / " + total + "]";

        infoModalOpen = true;
        modalInputDelay = 0.35f;
        modalEyebrow.setText(eyebrows != null && index < eyebrows.length ? eyebrows[index] : "");
        modalTitle.setText(titles[index] == null ? "" : titles[index]);
        modalBody.setText(bodies[index] == null ? "" : bodies[index]);
        modalFooter.setText(pageFooter);
        modalSidebar.setText(sidebars != null && index < sidebars.length ? sidebars[index] : "");
        hideKeypadText();
        modalNode.setCullHint(Spatial.CullHint.Never);
        setPrompt(null);

        final int next = index + 1;
        infoModalContinue = () -> {
            if (isLast) {
                closeInfoModal();
                if (onComplete != null) onComplete.run();
            } else {
                openNarrativePage(titles, bodies, eyebrows, sidebars, onComplete, next);
            }
        };
    }

    public void closeInfoModal() {
        infoModalOpen = false;
        infoModalContinue = null;
        hideInfoModalText();
        if (!keypadOpen) {
            modalNode.setCullHint(Spatial.CullHint.Always);
        }
    }

    /**
     * Overrides the modal accent bar and sidebar colours for the next modal.
     * Call {@link #resetModalTheme()} in the onComplete callback to restore defaults.
     */
    public void setModalTheme(ColorRGBA topAccent, ColorRGBA bottomAccent, ColorRGBA sidebar) {
        modalAccentTop.getMaterial().setColor("Color", topAccent);
        modalAccentBottom.getMaterial().setColor("Color", bottomAccent);
        modalSidebarPanel.getMaterial().setColor("Color", sidebar);
    }

    /** Restores the default cyan / red accent colours. */
    public void resetModalTheme() {
        modalAccentTop.getMaterial().setColor("Color",    new ColorRGBA(0.15f, 0.78f, 0.98f, 0.95f));
        modalAccentBottom.getMaterial().setColor("Color", new ColorRGBA(0.72f, 0.12f, 0.18f, 0.95f));
        modalSidebarPanel.getMaterial().setColor("Color", new ColorRGBA(0.12f, 0.16f, 0.20f, 0.95f));
    }

    public boolean isInfoModalOpen() {
        return infoModalOpen;
    }

    public boolean isOverlayBlockingInput() {
        return keypadOpen || infoModalOpen;
    }

    private void refreshKeypad() {
        StringBuilder visible = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            visible.append(i < keypadBuffer.length() ? keypadBuffer.charAt(i) : '_');
            visible.append(' ');
        }
        keypadInput.setText(visible.toString());
    }

    private void hideKeypadText() {
        keypadTitle.setText("");
        keypadInput.setText("");
        keypadHint.setText("");
    }

    private void hideInfoModalText() {
        modalEyebrow.setText("");
        modalTitle.setText("");
        modalBody.setText("");
        modalFooter.setText("");
        modalSidebar.setText("");
    }

    private Geometry createPanel(String name, ColorRGBA color) {
        Geometry geometry = new Geometry(name, new Quad(1f, 1f));
        Material material = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        geometry.setMaterial(material);
        return geometry;
    }

    private void layoutModal() {
        float sw = app.getCamera().getWidth();
        float sh = app.getCamera().getHeight();

        modalBackdrop.setLocalScale(sw, sh, 1f);
        modalBackdrop.setLocalTranslation(0f, 0f, -5f);

        float panelX = sw * 0.09f;
        float panelY = sh * 0.11f;
        float panelW = sw * 0.82f;
        float panelH = sh * 0.78f;
        modalPanel.setLocalScale(panelW, panelH, 1f);
        modalPanel.setLocalTranslation(panelX, panelY, -4f);

        float sidebarW = panelW * 0.22f;
        modalSidebarPanel.setLocalScale(sidebarW, panelH, 1f);
        modalSidebarPanel.setLocalTranslation(panelX, panelY, -3f);

        modalAccentTop.setLocalScale(panelW, 10f, 1f);
        modalAccentTop.setLocalTranslation(panelX, panelY + panelH - 12f, -2f);

        modalAccentBottom.setLocalScale(panelW * 0.28f, 8f, 1f);
        modalAccentBottom.setLocalTranslation(panelX, panelY, -2f);

        modalFooterPanel.setLocalScale(panelW * 0.62f, 52f, 1f);
        modalFooterPanel.setLocalTranslation(panelX + panelW * 0.31f, panelY + 22f, -2f);

        modalEyebrow.setLocalTranslation(panelX + sidebarW + 42f, panelY + panelH - 44f, 2f);
        modalTitle.setLocalTranslation(panelX + sidebarW + 40f, panelY + panelH - 88f, 2f);
        modalBody.setLocalTranslation(panelX + sidebarW + 40f, panelY + panelH - 162f, 2f);
        modalFooter.setLocalTranslation(panelX + sidebarW + 58f, panelY + 58f, 2f);
        modalSidebar.setLocalTranslation(panelX + 28f, panelY + panelH - 58f, 2f);

        keypadTitle.setLocalTranslation(panelX + sidebarW + 58f, panelY + panelH - 110f, 2f);
        keypadInput.setLocalTranslation(panelX + sidebarW + 110f, panelY + panelH - 184f, 2f);
        keypadHint.setLocalTranslation(panelX + sidebarW + 58f, panelY + 120f, 2f);
    }

    private String resolveEyebrow(String title) {
        if (title == null) return "";
        if (title.startsWith("LEVEL 1"))            return "ENTRY PROTOCOL";
        if (title.startsWith("LEVEL 2"))            return "OBSERVATION SYSTEM";
        if (title.startsWith("LEVEL 3"))            return "CONTAINMENT ARCHIVE";
        if (title.startsWith("LEVEL 4"))            return "REVELATION CORE";
        if (title.startsWith("AUDIO LOG"))          return "DIGITAL FORENSICS";
        if (title.startsWith("LEVEL 1 COMPLETE"))  return "MISSION SUCCESS";
        if (title.startsWith("LEVEL 2 COMPLETE"))  return "MISSION SUCCESS";
        if (title.startsWith("LEVEL 3 COMPLETE"))  return "MISSION SUCCESS";
        if ("ENDGAME".equals(title))               return "FINAL REPORT";
        return "LAB SYSTEM";
    }

    private String resolveSidebar(String title, String body) {
        if (title == null) return "";
        if (title.startsWith("AUDIO LOG")) {
            return "ARCHIVE\n\nRecovered log\nindexed into\ncontrol-room\nforensics.";
        }
        if ("ENDGAME".equals(title)) {
            return "STATUS\n\nMemory loop\nresolved.\nFacility silent.";
        }
        if (title.startsWith("LEVEL")) {
            return "BRIEFING\n\nUnderground\nresearch wing\nactive.\n\nReview story\nand mission\nbefore entry.";
        }
        return "SYSTEM\n\n" + (body == null ? "" : body);
    }
}
