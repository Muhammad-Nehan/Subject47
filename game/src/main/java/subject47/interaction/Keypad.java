package subject47.interaction;

import java.util.function.Consumer;
import subject47.Main;

public class Keypad implements Interactable {

    private final Main app;
    private final String correctCode;
    private final String title;
    private final Consumer<String> onSuccess;

    public Keypad(Main app, String title, String correctCode, Consumer<String> onSuccess) {
        this.app = app;
        this.correctCode = correctCode;
        this.title = title;
        this.onSuccess = onSuccess;
    }

    @Override
    public void interact() {
        app.getUi().openKeypad(title, code -> {
            if (correctCode.equals(code)) {
                app.getUi().showMessage("Access granted.", 2f);
                onSuccess.accept(code);
            } else {
                app.getUi().showMessage("Access denied.", 2f);
            }
        });
    }

    @Override
    public String getPrompt() {
        return "Press E to use keypad";
    }
}
