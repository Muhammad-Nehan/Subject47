package subject47.interaction;

import subject47.Main;

public class Terminal implements Interactable {

    private final Main app;
    private final String prompt;
    private final String message;
    private final Runnable action;
    private final boolean singleUse;

    private boolean used;

    public Terminal(Main app, String prompt, String message, boolean singleUse, Runnable action) {
        this.app = app;
        this.prompt = prompt;
        this.message = message;
        this.singleUse = singleUse;
        this.action = action;
    }

    @Override
    public void interact() {
        if (singleUse && used) {
            app.getUi().showMessage("Nothing new appears on the terminal.", 2f);
            return;
        }

        used = true;
        if (message != null && !message.isBlank()) {
            app.getUi().showNarration(message, 6f);
        }

        if (action != null) {
            action.run();
        }
    }

    @Override
    public String getPrompt() {
        return prompt;
    }
}
