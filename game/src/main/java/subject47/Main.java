package subject47;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import subject47.audio.AudioManager;
import subject47.events.EventManager;
import subject47.level.EditorManager;
import subject47.level.LevelManager;
import subject47.player.PlayerController;
import subject47.ui.IntroSequence;
import subject47.ui.MainMenu;
import subject47.ui.UIManager;
import subject47.world.WorldManager;

public class Main extends SimpleApplication {

    private PlayerController player;
    private WorldManager world;
    private UIManager ui;
    private AudioManager audio;
    private EventManager events;
    private LevelManager levelManager;
    private EditorManager editor;
    private MainMenu mainMenu;
    private IntroSequence introSequence;

    private boolean startedGame;
    private boolean gameplayActive;

    public static void main(String[] args) {
        Main app = new Main();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Subject 47");
        settings.setResolution(1280, 720);
        settings.setResizable(true);
        settings.setVSync(true);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
        cam.setFrustumPerspective(60f, (float) cam.getWidth() / Math.max(1f, cam.getHeight()), 0.03f, 250f);
        // Remove JME's built-in ESC-quits mapping so ESC can be used for in-game UI (keypad, menus).
        inputManager.deleteMapping(INPUT_MAPPING_EXIT);

        world = new WorldManager(this);
        world.initialize();

        ui = new UIManager(this);
        ui.initialize();

        audio = new AudioManager(this);
        audio.initialize();

        player = new PlayerController(this);
        player.initialize();

        levelManager = new LevelManager(this);

        editor = new EditorManager(this);
        editor.initialize();

        events = new EventManager(this);
        events.initialize();

        mainMenu = new MainMenu(this);
        mainMenu.initialize();
        mainMenu.hide();

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.22f));
        rootNode.addLight(ambient);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.6f, -1.2f, -0.4f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(0.45f));
        rootNode.addLight(sun);

        introSequence = new IntroSequence(this, this::pauseToMenu);
        introSequence.initialize();
        introSequence.start();
    }

    @Override
    public void simpleUpdate(float tpf) {
        ui.update(tpf);
        world.update(tpf);
        mainMenu.update(tpf);
        if (introSequence != null && introSequence.isActive()) {
            introSequence.update(tpf);
            return;
        }

        if (!gameplayActive) {
            return;
        }

        player.update(tpf);
        editor.update(tpf);

        if (!editor.isActive()) {
            levelManager.update(tpf);
            events.update(tpf);
        }
    }

    public void startGame() {
        startedGame = true;
        gameplayActive = true;
        audio.stopMenuMusic();       // ensure menu music stops before gameplay
        mainMenu.hide();
        inputManager.setCursorVisible(true);
        ui.setHudVisible(true);
        ui.closeKeypad();
        levelManager.startNewGame();
        if (gameplayActive && !ui.isOverlayBlockingInput()) {
            player.setEnabled(true);
        }
    }

    public void pauseToMenu() {
        gameplayActive = false;
        player.setEnabled(false);
        ui.closeKeypad();
        ui.closeInfoModal();
        ui.setHudVisible(false);
        inputManager.setCursorVisible(true);
        mainMenu.showMainScreen();
    }

    public void resumeGame() {
        if (!startedGame) {
            return;
        }
        audio.stopMenuMusic();       // ensure menu music doesn't bleed into gameplay
        gameplayActive = true;
        inputManager.setCursorVisible(true);
        ui.setHudVisible(true);
        player.setEnabled(true);
    }

    public void freezeGameplay() {
        gameplayActive = false;
        inputManager.setCursorVisible(true);
        player.setEnabled(false);
    }

    public boolean hasStartedGame() {
        return startedGame;
    }

    public boolean isGameplayActive() {
        return gameplayActive;
    }

    public PlayerController getPlayer() {
        return player;
    }

    public WorldManager getWorld() {
        return world;
    }

    public UIManager getUi() {
        return ui;
    }

    public AudioManager getAudio() {
        return audio;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public EditorManager getEditor() {
        return editor;
    }
}
