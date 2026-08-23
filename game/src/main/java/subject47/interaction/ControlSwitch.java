package subject47.interaction;

import subject47.Main;

public class ControlSwitch implements Interactable {

    private final Main app;
    private final String prompt;
    private final String successMessage;
    private final Runnable action;

    public ControlSwitch(Main app, String prompt, String successMessage, Runnable action) {
        this.app = app;
        this.prompt = prompt;
        this.successMessage = successMessage;
        this.action = action;
    }

    @Override
    public void interact() {
        if (action != null) {
            action.run();
        }
        if (successMessage != null && !successMessage.isBlank()) {
            app.getUi().showMessage(successMessage, 1.8f);
        }
    }

    @Override
    public String getPrompt() {
        return prompt;
    }
}
