package subject47.events;

import java.util.Random;
import subject47.Main;

public class EventManager {

    private final Main app;
    private final Random random = new Random();

    private float timer;
    private float nextEventDelay = 8f;

    public EventManager(Main app) {
        this.app = app;
    }

    public void initialize() {
    }

    public void update(float tpf) {
        timer += tpf;

        if (timer >= nextEventDelay) {
            timer = 0f;
            nextEventDelay = 7f + random.nextFloat() * 6f;
            triggerAmbientEvent();
        }
    }

    private void triggerAmbientEvent() {
        switch (app.getLevelManager().getCurrentLevel()) {
            case 1 -> levelOneEvents();
            case 2 -> levelTwoEvents();
            case 3 -> levelThreeEvents();
            case 4 -> levelFourEvents();
            default -> {
            }
        }
    }

    private void levelOneEvents() {
        if (random.nextBoolean()) {
            app.getWorld().flickerLights(0.7f);
            app.getUi().showMessage("The corridor lights sputter overhead.", 2f);
        } else {
            app.getUi().showMessage("A metallic bang echoes from deeper in the lab.", 2f);
        }
    }

    private void levelTwoEvents() {
        if (random.nextBoolean()) {
            app.getWorld().flickerLights(1f);
            app.getUi().showMessage("Footsteps circle behind you, then stop.", 2.2f);
        } else {
            app.getUi().showMessage("A camera feed jitters: movement detected.", 2.2f);
        }
    }

    private void levelThreeEvents() {
        if (random.nextBoolean()) {
            app.getWorld().setEmergencyLighting(true);
            app.getUi().showMessage("Terminal text appears on its own: WHY ARE YOU HERE?", 2.5f);
        } else {
            app.getUi().showMessage("The corridor layout feels wrong when you look back.", 2.5f);
        }
    }

    private void levelFourEvents() {
        if (random.nextBoolean()) {
            app.getWorld().flickerLights(1.2f);
            app.getUi().showMessage("Memory fragments flash at the edge of your vision.", 2.5f);
        } else {
            app.getUi().showMessage("A system voice repeats: SUBJECT ACTIVE.", 2.2f);
        }
    }
}
