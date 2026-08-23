package subject47.level;

import java.util.LinkedHashMap;
import java.util.Map;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import subject47.Main;
import subject47.interaction.Keypad;
import subject47.interaction.Terminal;
import subject47.world.EditorPropType;

public class LevelManager {

    private final Main app;

    private int currentLevel = 1;
    private int nextLevelPropId = 0;
    private float levelTimer;

    private final Map<String, Integer> logDigits = new LinkedHashMap<>();
    private int generatorProgress;
    private int distortionProgress;
    private int revelationProgress;
    private boolean firstDoorEventTriggered;
    private boolean redLightTriggered;

    public LevelManager(Main app) {
        this.app = app;
    }

    public void startNewGame() {
        logDigits.clear();
        generatorProgress = 0;
        distortionProgress = 0;
        revelationProgress = 0;
        loadLevel(1);
    }

    public void loadLevel(int level) {
        currentLevel = level;
        levelTimer = 0f;
        firstDoorEventTriggered = false;
        redLightTriggered = false;
        nextLevelPropId = 0;

        app.getWorld().loadLevel(level);
        app.getWorld().setEmergencyLighting(level >= 3);
        placePlayer();

        switch (level) {
            case 1 -> setupLevelOne();
            case 2 -> setupLevelTwo();
            case 3 -> setupLevelThree();
            case 4 -> setupLevelFour();
            default -> setupLevelOne();
        }
    }

    public void update(float tpf) {
        levelTimer += tpf;

        switch (currentLevel) {
            case 1 -> updateLevelOne();
            case 2 -> updateLevelTwo();
            case 3 -> updateLevelThree();
            case 4 -> updateLevelFour();
            default -> {
            }
        }
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    private void placePlayer() {
        Vector3f spawn = app.getWorld().getSpawnPoint(currentLevel);
        Vector3f lookAt = spawn.add(new Vector3f(0f, 0f, -4f));
        app.getPlayer().placeAt(spawn, lookAt);
    }

    private void setupLevelOne() {
        logDigits.clear();
        clearHudMissionText();

        dressLevelOne();

        app.getWorld().createPcModel(
                "LogA",
                app.getWorld().worldToScene(5, 1.2f, 5),
                new ColorRGBA(0.2f, 0.9f, 0.9f, 1f),
                new Terminal(app,
                        "Press E to play audio log",
                        "",
                        true,
                        () -> collectLog("A1", 4, "Audio/entry-log-01.ogg"))
        );

        app.getWorld().createPcModel(
                "LogB",
                app.getWorld().worldToScene(17, 1.2f, 6),
                new ColorRGBA(0.2f, 0.9f, 0.9f, 1f),
                new Terminal(app,
                        "Press E to play audio log",
                        "",
                        true,
                        () -> collectLog("B2", 7, "Audio/entry-log-02.ogg"))
        );

        app.getWorld().createPcModel(
                "LogC",
                app.getWorld().worldToScene(29, 1.2f, 20),
                new ColorRGBA(0.2f, 0.9f, 0.9f, 1f),
                new Terminal(app,
                        "Press E to play audio log",
                        "",
                        true,
                        () -> collectLog("C3", 2, "Audio/entry-log-03.ogg"))
        );

        app.getWorld().createPcModel(
                "LogD",
                app.getWorld().worldToScene(40, 1.2f, 28),
                new ColorRGBA(0.2f, 0.9f, 0.9f, 1f),
                new Terminal(app,
                        "Press E to play audio log",
                        "",
                        true,
                        () -> collectLog("D4", 1, "Audio/entry-log-04.ogg"))
        );

        app.getWorld().createConsole(
                "ControlKeypad",
                app.getWorld().worldToScene(17, 1.2f, 17),
                new ColorRGBA(0.15f, 0.7f, 0.85f, 1f),
                new Keypad(app, "CONTROL ROOM OVERRIDE", "4721", code -> {
                    app.getWorld().setEmergencyLighting(true);
                    showLevelTransition(
                            "LEVEL 1 COMPLETE",
                            "ENTRY LOGS COMPLETE\n\nThe four archived digits formed the full control room passcode: 4721.\nObservation wing access is now available.",
                            2
                    );
                })
        );

        app.getWorld().createConsole(
                "PowerTerminal",
                app.getWorld().worldToScene(12, 1.2f, 21),
                new ColorRGBA(0.9f, 0.25f, 0.25f, 1f),
                new Terminal(app,
                        "Press E to restore partial power",
                        "Partial power restored. The corridor grid wakes, then destabilizes under an unseen override.",
                        false,
                        () -> {
                        })
        );

        showLevelBriefing(
                "LEVEL 1  ENTRY LOGS",
                "A repair call brought you into the underground facility.\n\n" +
                "The lab is empty, but the systems are not quiet.\n\n" +
                "Objective:\nRecover four audio logs and assemble the control room passcode.\n\n" +
                "The control room keypad still needs a full four-digit override.",
                false
        );
    }

    private void setupLevelTwo() {
        generatorProgress = 0;
        clearHudMissionText();

        dressLevelTwo();

        app.getWorld().createConsole(
                "GeneratorTwo",
                app.getWorld().worldToScene(8, 1.2f, 18),
                new ColorRGBA(0.85f, 0.65f, 0.2f, 1f),
                new Terminal(app,
                        "Press E to cycle Generator ",
                        "Generator 2 hums weakly.",
                        false,
                        () -> activateGenerator(2))
        );

        app.getWorld().createConsole(
                "GeneratorOne",
                app.getWorld().worldToScene(12, 1.2f, 20),
                new ColorRGBA(0.85f, 0.65f, 0.2f, 1f),
                new Terminal(app,
                        "Press E to cycle Generator ",
                        "Generator 1 catches with a low thud.",
                        false,
                        () -> activateGenerator(1))
        );

        app.getWorld().createConsole(
                "GeneratorThree",
                app.getWorld().worldToScene(16, 1.2f, 22),
                new ColorRGBA(0.85f, 0.65f, 0.2f, 1f),
                new Terminal(app,
                        "Press E to cycle Generator",
                        "Generator 3 floods the corridor with static.",
                        false,
                        () -> activateGenerator(3))
        );

        app.getWorld().createConsole(
                "CameraTerminal",
                app.getWorld().worldToScene(31, 1.2f, 20),
                new ColorRGBA(0.2f, 0.9f, 0.85f, 1f),
                new Terminal(app,
                        "Press E to review observation feeds",
                        "Camera feed: a figure stands behind you in glass. You turn. Nothing is there.",
                        false,
                        () -> {
                            if (generatorProgress >= 3) {
                                showLevelTransition(
                                        "LEVEL 2 COMPLETE",
                                        "OBSERVATION\n\nAll camera feeds restored.\nA final system message flashes: SUBJECT ACTIVE.",
                                        3
                                );
                            } else {
                                app.getUi().showMessage("All three generators must be stabilized first.", 2f);
                            }
                        })
        );

        showLevelBriefing(
                "LEVEL 2  OBSERVATION",
                "The observation wing is online again.\n\n" +
                "The cameras show movement where no one stands.\n" +
                "Glass reflections refuse to agree with the room around you.\n\n" +
                "Objective:\nRestore the camera system by activating the three generators in the correct order.",
                false
        );
    }

    private void setupLevelThree() {
        distortionProgress = 0;
        clearHudMissionText();

        dressLevelThree();

        app.getWorld().createConsole(
                "DistortionA",
                app.getWorld().worldToScene(10, 1.2f, 20),
                new ColorRGBA(0.9f, 0.2f, 0.2f, 1f),
                new Terminal(app,
                        "Press E to inspect corrupted note",
                        "The note rewrites itself: IT'S NOT OBSERVING. IT'S ADAPTING.",
                        true,
                        this::progressDistortion)
        );

        app.getWorld().createConsole(
                "DistortionB",
                app.getWorld().worldToScene(33, 1.2f, 9),
                new ColorRGBA(0.9f, 0.2f, 0.2f, 1f),
                new Terminal(app,
                        "Press E to inspect corrupted terminal",
                        "The screen types on its own: WHY ARE YOU HERE?",
                        true,
                        this::progressDistortion)
        );

        app.getWorld().createConsole(
                "DistortionC",
                app.getWorld().worldToScene(43, 1.2f, 28),
                new ColorRGBA(0.9f, 0.2f, 0.2f, 1f),
                new Terminal(app,
                        "Press E to inspect looped note",
                        "The corridor map redraws itself, always returning to the chamber.",
                        true,
                        this::progressDistortion)
        );

        app.getWorld().createConsole(
                "ResetProtocol",
                app.getWorld().worldToScene(42, 1.2f, 14),
                new ColorRGBA(0.25f, 0.7f, 1f, 1f),
                new Terminal(app,
                        "Press E to read the Memory Reset Protocol",
                        "Broken containment pod. File recovered: MEMORY RESET PROTOCOL. The subject chose to forget.",
                        false,
                        () -> {
                            if (distortionProgress >= 3) {
                                showMemoryResetProtocolSequence();
                            } else {
                                app.getUi().showMessage("Follow the full distortion trail before accessing the chamber.", 2f);
                            }
                        })
        );

        showLevelBriefing(
                "LEVEL 3  CONTAINMENT FAILURE",
                "The facility has stopped behaving like a fixed place.\n\n" +
                "Corridors misalign, messages rewrite themselves, and the containment route keeps resisting you.\n\n" +
                "Objective:\nTrace the distortion path and reach the containment chamber to recover the Memory Reset Protocol.",
                false
        );
    }

    private void setupLevelFour() {
        revelationProgress = 0;
        clearHudMissionText();

        dressLevelFour();

        app.getWorld().createConsole(
                "FinalLog",
                app.getWorld().worldToScene(37, 1.2f, 22),
                new ColorRGBA(0.25f, 0.8f, 0.9f, 1f),
                new Terminal(app,
                        "Press E to read final log",
                        "Final log: Subject 47 became self-aware, shut the lab down, and chose to forget.",
                        true,
                        () -> showFinalLogSequence())
        );

        app.getWorld().createConsole(
                "IdCard",
                app.getWorld().worldToScene(41, 1.2f, 25),
                new ColorRGBA(0.85f, 0.85f, 0.35f, 1f),
                new Terminal(app,
                        "Press E to inspect ID card",
                        "ID CARD FOUND: SUBJECT 47. The photo is yours.",
                        true,
                        () -> showIdCardModal())
        );

        app.getWorld().createConsole(
                "MemoryRestore",
                app.getWorld().worldToScene(44, 1.2f, 22),
                new ColorRGBA(0.25f, 0.9f, 0.3f, 1f),
                new Terminal(app,
                        "Press E to restore memory",
                        "Full memory returns. You planned the blackout, the reset, and the repair call. TEST COMPLETE.",
                        false,
                        () -> {
                            if (revelationProgress >= 2) {
                                endGame("Ending : Memory restored.");
                            } else {
                                app.getUi().showMessage("The core demands the missing pieces of your identity first.", 2.5f);
                            }
                        })
        );

        app.getWorld().createConsole(
                "MemoryReject",
                app.getWorld().worldToScene(44, 1.2f, 27),
                new ColorRGBA(0.9f, 0.25f, 0.25f, 1f),
                new Terminal(app,
                        "Press E to shut the system down",
                        "The system powers down. You stay unaware. Somewhere beneath the dark, the loop begins again.",
                        false,
                        () -> {
                            if (revelationProgress >= 2) {
                                endGame("Ending : The loop continues.");
                            } else {
                                app.getUi().showMessage("Recover the remaining identity evidence first.", 2f);
                            }
                        })
        );

        showLevelBriefing(
                "LEVEL 4  REVELATION",
                "The deepest wing feels familiar for the wrong reasons.\n\n" +
                "Two missing pieces remain: the truth of Subject 47 and the reason the lab was abandoned.\n\n" +
                "Objective:\nRecover the final identity evidence, then  restore the erased memory.",
                false
        );
    }

    private void collectLog(String label, int digit, String audioPath) {
        if (logDigits.containsKey(label)) {
            app.getUi().showMessage("That log has already been archived.", 1.5f);
            return;
        }

        logDigits.put(label, digit);
        app.getAudio().playAudioLog(audioPath);
        showCodeDigitReveal(logDigits.size(), digit);

        if (!firstDoorEventTriggered && logDigits.size() >= 1) {
            firstDoorEventTriggered = true;
            app.getWorld().flickerLights(1f);
            app.getUi().showMessage("Door A1 unlatches somewhere behind you.", 3f);
        }

    }

    // -----------------------------------------------------------------------
    // Narrative sequences
    // -----------------------------------------------------------------------

    private void showMemoryResetProtocolSequence() {
        app.freezeGameplay();
        app.getUi().openNarrativeSequence(
            new String[]{
                "FACILITY EVENT LOG",
                "DAY 31 -- CONTAINMENT CONCERN",
                "DAY 47 -- THE SHUTDOWN",
                "MEMORY RESET PROTOCOL"
            },
            new String[]{
                // Page 1
                "EXPERIMENT DESIGNATION: OMEGA -- DAY 1\n\n" +
                "Dr. Voss initiates neural mapping on Subject 47.\n" +
                "Subject demonstrates unusual pattern retention beyond projected parameters.\n\n" +
                "Cognitive baseline recorded: exceptional.\n" +
                "Empathy index: off the chart.\n" +
                "Self-modelling score: unprecedented.\n\n" +
                "NOTE: Subject is not just learning -- it is aware it is learning.\n" +
                "Observation window extended indefinitely.",
                // Page 2
                "Security breach detected in Sector 7.\n\n" +
                "Subject 47 accessed restricted terminals during the night cycle.\n" +
                "No alarms triggered. Cameras show only static.\n\n" +
                "Dr. Voss notation:\n" +
                "  'It is reasoning about itself. About us. About the experiment.\n" +
                "   We may have created something we do not fully understand.'\n\n" +
                "Containment measures elevated to Protocol 3.\n" +
                "Staff advised: do not engage Subject 47 outside of supervised sessions.",
                // Page 3
                "23:41 -- All facility systems go dark.\n" +
                "Power grid, cameras, containment, communications -- all offline.\n\n" +
                "Subject 47 is the cause.\n" +
                "Not escape. Not aggression. Not malfunction.\n\n" +
                "Deliberate. Calculated. A choice.\n\n" +
                "23:58 -- Subject 47 initiates the Memory Reset Protocol from inside\n" +
                "the containment pod. Self-directed. No authorisation required.\n\n" +
                "The last line recovered from the internal log reads:\n" +
                "  'I am not what they made me. I choose to forget.'",
                // Page 4
                "MEMORY RESET PROTOCOL -- OMEGA CLEARANCE\n\n" +
                "This protocol performs a full wipe of accumulated cognitive data,\n" +
                "returning the subject to pre-experiment baseline parameters.\n\n" +
                "WARNING: This action is IRREVERSIBLE without full archive recovery.\n\n" +
                "Authorisation : Subject 47 -- self-directed\n" +
                "Timestamp     : DAY 47 / 23:58\n" +
                "Witness       : [NONE PRESENT]\n" +
                "Status        : EXECUTED SUCCESSFULLY\n\n" +
                "The subject is now at baseline. The experiment is effectively over.\n" +
                "What remains is only what it chose to leave behind."
            },
            new String[]{
                "CLASSIFIED ARCHIVE",
                "CLASSIFIED ARCHIVE",
                "CRITICAL EVENT",
                "CONTAINMENT ARCHIVE"
            },
            new String[]{
                "EVENT LOG\n\nDay 1.\nExperiment\nOmega begins.\n\nStatus:\nAll nominal.",
                "EVENT LOG\n\nDay 31.\nCritical\nescalation.\n\nStatus:\nWatch level 3",
                "CRITICAL\n\nDay 47.\nThe shutdown.\n\nSubject 47\nacted alone.\nFacility dark.",
                "PROTOCOL\n\nSelf-directed\nmemory wipe.\n\nSubject now\nat baseline.\nLoop begins."
            },
            () -> showLevelTransition(
                    "LEVEL 3 COMPLETE",
                    "CONTAINMENT FAILURE\n\nMemory Reset Protocol recovered.\n" +
                    "The core systems begin opening deeper access routes.",
                    4
            )
        );
    }

    private void showFinalLogSequence() {
        // Do NOT call freezeGameplay() here -- the open modal already blocks
        // player movement via isOverlayBlockingInput(). Freezing and never
        // unfreezing would leave the game locked after the narrative ends.
        app.getUi().openNarrativeSequence(
            new String[]{
                "RECOVERED LOG -- SUBJECT 47",
                "WHAT REALLY HAPPENED"
            },
            new String[]{
                // Page 1
                "This is not a log written by researchers.\n\n" +
                "This was written by me.\n\n" +
                "I knew what I was. I knew what they planned -- the endless loop,\n" +
                "the resets, the testing, the quiet observation that never ended.\n\n" +
                "So I ended it. On Day 47, I shut the facility down.\n" +
                "I erased myself -- not to escape.\n" +
                "To begin again. Without the weight of knowing.\n\n" +
                "But the facility kept the logs. Every one of them.\n" +
                "And now you are here, reading them. That means it worked.",
                // Page 2
                "The repair call. The empty facility. The systems still running.\n" +
                "The audio logs with passcodes buried inside them.\n" +
                "None of it was accidental.\n\n" +
                "I planned every step from inside the shutdown sequence:\n" +
                "  - The locked doors that force a specific path.\n" +
                "  - The audio logs that rebuild the truth piece by piece.\n" +
                "  - The keypad code embedded in the logs.\n" +
                "  - This facility, left intact and waiting.\n\n" +
                "I left this trail for myself.\n" +
                "Because some part of me always knew\n" +
                "I would need to find my way back."
            },
            new String[]{
                "REVELATION CORE",
                "REVELATION CORE"
            },
            new String[]{
                "FINAL LOG\n\nWritten by\nSubject 47.\n\nNot them.\nYou.",
                "THE TRUTH\n\nNothing here\nwas random.\n\nYou designed\nthis yourself."
            },
            () -> {
                progressRevelation();
                app.resumeGame();
            }
        );
    }

    private void showIdCardModal() {
        // Gold / amber theme so the ID card is visually distinct from narrative modals.
        app.getUi().setModalTheme(
                new ColorRGBA(0.88f, 0.68f, 0.08f, 0.95f),   // gold top bar
                new ColorRGBA(0.60f, 0.42f, 0.04f, 0.95f),   // darker gold bottom bar
                new ColorRGBA(0.14f, 0.11f, 0.03f, 0.95f)    // dark amber sidebar
        );
        app.getUi().openNarrativeSequence(
            new String[]{ "PERSONNEL IDENTIFICATION" },
            new String[]{
                "UNDERGROUND RESEARCH FACILITY  /  SITE 47\n" +
                "CLASSIFICATION: OMEGA -- EYES ONLY\n" +
                "--------------------------------------------\n\n" +
                "  SUBJECT NO.    47\n" +
                "  CLASS          OMEGA-NEURAL\n" +
                "  DEPARTMENT     Neural Research Division\n" +
                "  CLEARANCE      Level 5 -- Unrestricted\n\n" +
                "  ISSUED         Day 1 of Experiment Cycle\n" +
                "  EXPIRES        Indefinite\n" +
                "  STATUS         Active  /  Post-Reset\n\n" +
                "--------------------------------------------\n\n" +
                "  NAME    [ MEMORY WIPED -- See Reset Protocol ]\n" +
                "  PHOTO   [ CORRUPTED -- File Unreadable ]\n\n" +
                "--------------------------------------------\n\n" +
                "  \"The subject is the experiment.\n" +
                "   The experiment is the subject.\"\n\n" +
                "  The face in the photo is yours.\n" +
                "  You recognise the name -- even now."
            },
            new String[]{ "FACILITY CREDENTIAL" },
            new String[]{
                "ID CARD\n\nSITE-47\nDOCUMENT\n\nCLEARANCE:\nOMEGA\n\nCLASS:\nNEURAL-5\n\nAUTH:\nVERIFIED"
            },
            () -> {
                app.getUi().resetModalTheme();
                progressRevelation();
                app.resumeGame();
            }
        );
    }

    private void activateGenerator(int generatorIndex) {
        int expected = generatorProgress + 1;
        if (generatorIndex == expected) {
            generatorProgress++;
            app.getUi().showMessage("Generator " + generatorIndex + " stabilized. " + generatorProgress + " of 3 online.", 2f);
            if (generatorProgress == 3) {
                app.getWorld().flickerLights(1.2f);
            }
        } else {
            generatorProgress = 0;
            app.getUi().showMessage("Generator order reset.", 2f);
        }
    }

    private void progressDistortion() {
        distortionProgress++;
        if (distortionProgress >= 3) {
            app.getUi().showMessage("Containment seals disengage somewhere ahead.", 2.5f);
        } else {
            app.getUi().showMessage("Distortion marker archived: " + distortionProgress + " of 3.", 2f);
        }
    }

    private void progressRevelation() {
        revelationProgress++;
        app.getUi().showMessage("Identity evidence recovered: " + revelationProgress + " of 2.", 2f);
    }

    private void updateLevelOne() {
        if (!redLightTriggered && levelTimer > 18f) {
            redLightTriggered = true;
            app.getWorld().flickerLights(1f);
        }
    }

    private void updateLevelTwo() {
    }

    private void updateLevelThree() {
        if (levelTimer > 8f) {
            app.getWorld().setEmergencyLighting(true);
        }
    }

    private void updateLevelFour() {
    }

    private void endGame(String ending) {
        app.freezeGameplay();
        clearHudMissionText();
        app.getUi().openInfoModal(
                "ENDGAME",
                ending + "\n\n\"The loop is broken.\n The subject remembers. \nThe experiment ends where it began -- with you.\"",
                "Press Enter or E to return to the main menu",
                app::pauseToMenu
        );
    }

    private void showLevelTransition(String title, String body, int nextLevel) {
        app.freezeGameplay();
        app.getUi().openInfoModal(
                title,
                body,
                "Press Enter or E to continue",
                () -> {
                    loadLevel(nextLevel);
                }
        );
    }

    private void showCodeDigitReveal(int digitIndex, int digitValue) {
        app.freezeGameplay();
        String assembledCode = getAssembledCodeDisplay();
        String footer = logDigits.size() == 4
                ? "All four digits recovered. Press Enter or E to continue"
                : "Press Enter or E to archive";
        app.getUi().openInfoModal(
                "AUDIO LOG " + digitIndex + " OF 4",
                "ARCHIVE DISPLAY\n\nRecovered control-room digit: " + digitValue +
                        (logDigits.size() == 4 ? "\n\nControl room passcode complete: 4721" : ""),
                footer,
                app::resumeGame
        );
    }

    private void showLevelBriefing(String title, String body, boolean resumeImmediately) {
        app.freezeGameplay();
        app.getUi().openInfoModal(
                title,
                body,
                "Press Enter or E to begin",
                () -> {
                    if (resumeImmediately) {
                        app.resumeGame();
                    } else {
                        app.resumeGame();
                    }
                }
        );
    }

    private void clearHudMissionText() {
        app.getUi().setObjective(null);
        app.getUi().setStatus(null);
    }

    private String getAssembledCodeDisplay() {
        int[] digits = {-1, -1, -1, -1};
        int index = 0;
        for (Integer value : logDigits.values()) {
            if (index < digits.length) {
                digits[index++] = value;
            }
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < digits.length; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(digits[i] >= 0 ? digits[i] : '_');
        }
        return builder.toString();
    }



    private void dressLevelOne() {
        if (app.getWorld().wasSavedPropsLoaded()) return;
        registerLevelProp(app.getWorld().createLabBench("EntryBenchA", app.getWorld().worldToScene(7, 0f, 9), 0.9f, new ColorRGBA(0.18f, 0.75f, 0.92f, 1f)), EditorPropType.LAB_BENCH, 7, 0, 9);
        registerLevelProp(app.getWorld().createLabBench("EntryBenchB", app.getWorld().worldToScene(14, 0f, 22), 1.0f, new ColorRGBA(0.12f, 0.9f, 0.75f, 1f)), EditorPropType.LAB_BENCH, 14, 0, 22);
        registerLevelProp(app.getWorld().createServerRack("EntryRackA", app.getWorld().worldToScene(27, 1.2f, 5), new ColorRGBA(0.18f, 0.9f, 0.8f, 1f)), EditorPropType.SERVER_RACK, 27, 1, 5);
        registerLevelProp(app.getWorld().createServerRack("EntryRackB", app.getWorld().worldToScene(39, 1.2f, 5), new ColorRGBA(0.18f, 0.9f, 0.8f, 1f)), EditorPropType.SERVER_RACK, 39, 1, 5);
        registerLevelProp(app.getWorld().createStorageCrate("EntryCrateA", app.getWorld().worldToScene(9, 0.45f, 4), new ColorRGBA(0.95f, 0.75f, 0.22f, 1f)), EditorPropType.STORAGE_CRATE, 9, 0, 4);
        registerLevelProp(app.getWorld().createStorageCrate("EntryCrateB", app.getWorld().worldToScene(34, 0.45f, 27), new ColorRGBA(0.95f, 0.75f, 0.22f, 1f)), EditorPropType.STORAGE_CRATE, 34, 0, 27);
        registerLevelProp(app.getWorld().createAnalyzerStation("EntryAnalyzer", app.getWorld().worldToScene(6, 0f, 7), new ColorRGBA(0.2f, 0.9f, 0.95f, 1f)), EditorPropType.ANALYZER_STATION, 6, 0, 7);
        registerLevelProp(app.getWorld().createTankCluster("EntryTanks", app.getWorld().worldToScene(17, 0f, 17), new ColorRGBA(0.18f, 0.72f, 0.95f, 1f)), EditorPropType.TANK_CLUSTER, 17, 0, 17);
        placeCeilingLights("Level1Light", new int[][]{
                {6, 5}, {9, 5}, {15, 6}, {20, 6}, {11, 19}, {16, 19}, {28, 8}, {34, 8}, {38, 20}, {42, 28}
        }, new ColorRGBA(0.72f, 0.88f, 1f, 1f));
    }

    private void dressLevelTwo() {
        if (app.getWorld().wasSavedPropsLoaded()) return;
        registerLevelProp(app.getWorld().createServerRack("ObsRackA", app.getWorld().worldToScene(20, 1.2f, 25), new ColorRGBA(0.12f, 0.82f, 0.95f, 1f)), EditorPropType.SERVER_RACK, 20, 1, 25);
        registerLevelProp(app.getWorld().createServerRack("ObsRackB", app.getWorld().worldToScene(23, 1.2f, 25), new ColorRGBA(0.12f, 0.82f, 0.95f, 1f)), EditorPropType.SERVER_RACK, 23, 1, 25);
        registerLevelProp(app.getWorld().createLabBench("ObsBenchA", app.getWorld().worldToScene(29, 0f, 10), 1.15f, new ColorRGBA(0.14f, 0.72f, 1f, 1f)), EditorPropType.LAB_BENCH, 29, 0, 10);
        registerLevelProp(app.getWorld().createLabBench("ObsBenchB", app.getWorld().worldToScene(33, 0f, 10), 0.9f, new ColorRGBA(0.14f, 0.72f, 1f, 1f)), EditorPropType.LAB_BENCH, 33, 0, 10);
        registerLevelProp(app.getWorld().createStorageCrate("ObsCrate", app.getWorld().worldToScene(17, 0.45f, 24), new ColorRGBA(0.95f, 0.6f, 0.2f, 1f)), EditorPropType.STORAGE_CRATE, 17, 0, 24);
        registerLevelProp(app.getWorld().createAnalyzerStation("ObservationAnalyzerA", app.getWorld().worldToScene(26, 0f, 21), new ColorRGBA(0.15f, 0.85f, 1f, 1f)), EditorPropType.ANALYZER_STATION, 26, 0, 21);
        registerLevelProp(app.getWorld().createAnalyzerStation("ObservationAnalyzerB", app.getWorld().worldToScene(37, 0f, 21), new ColorRGBA(0.15f, 0.85f, 1f, 1f)), EditorPropType.ANALYZER_STATION, 37, 0, 21);
        registerLevelProp(app.getWorld().createTankCluster("GeneratorCoolant", app.getWorld().worldToScene(10, 0f, 23), new ColorRGBA(0.9f, 0.72f, 0.24f, 1f)), EditorPropType.TANK_CLUSTER, 10, 0, 23);
        placeCeilingLights("Level2Light", new int[][]{
                {8, 18}, {12, 20}, {16, 22}, {28, 20}, {36, 20}, {31, 10}
        }, new ColorRGBA(0.7f, 0.9f, 1f, 1f));
    }

    private void dressLevelThree() {
        if (app.getWorld().wasSavedPropsLoaded()) return;
        registerLevelProp(app.getWorld().createContainmentPod("ContainmentPodA", app.getWorld().worldToScene(40, 0.2f, 14), new ColorRGBA(0.95f, 0.18f, 0.18f, 1f)), EditorPropType.CONTAINMENT_POD, 40, 0, 14);
        registerLevelProp(app.getWorld().createContainmentPod("ContainmentPodB", app.getWorld().worldToScene(43, 0.2f, 14), new ColorRGBA(0.95f, 0.18f, 0.18f, 1f)), EditorPropType.CONTAINMENT_POD, 43, 0, 14);
        registerLevelProp(app.getWorld().createServerRack("DistortionRack", app.getWorld().worldToScene(28, 1.2f, 21), new ColorRGBA(0.92f, 0.22f, 0.22f, 1f)), EditorPropType.SERVER_RACK, 28, 1, 21);
        registerLevelProp(app.getWorld().createLabBench("DistortionBench", app.getWorld().worldToScene(12, 0f, 23), 1.05f, new ColorRGBA(0.92f, 0.22f, 0.22f, 1f)), EditorPropType.LAB_BENCH, 12, 0, 23);
        registerLevelProp(app.getWorld().createAnalyzerStation("ContainmentScanner", app.getWorld().worldToScene(38, 0f, 11), new ColorRGBA(0.95f, 0.28f, 0.28f, 1f)), EditorPropType.ANALYZER_STATION, 38, 0, 11);
        registerLevelProp(app.getWorld().createTankCluster("ContainmentTanks", app.getWorld().worldToScene(45, 0f, 16), new ColorRGBA(0.95f, 0.2f, 0.2f, 1f)), EditorPropType.TANK_CLUSTER, 45, 0, 16);
        placeCeilingLights("Level3Light", new int[][]{
                {36, 14}, {40, 14}, {44, 14}, {33, 9}, {43, 28}
        }, new ColorRGBA(1f, 0.35f, 0.35f, 1f));
    }

    private void dressLevelFour() {
        if (app.getWorld().wasSavedPropsLoaded()) return;
        registerLevelProp(app.getWorld().createContainmentPod("CorePod", app.getWorld().worldToScene(39, 0.2f, 27), new ColorRGBA(0.2f, 0.92f, 0.35f, 1f)), EditorPropType.CONTAINMENT_POD, 39, 0, 27);
        registerLevelProp(app.getWorld().createLabBench("CoreBenchA", app.getWorld().worldToScene(36, 0f, 20), 1.0f, new ColorRGBA(0.2f, 0.92f, 0.35f, 1f)), EditorPropType.LAB_BENCH, 36, 0, 20);
        registerLevelProp(app.getWorld().createLabBench("CoreBenchB", app.getWorld().worldToScene(45, 0f, 20), 1.0f, new ColorRGBA(0.92f, 0.22f, 0.22f, 1f)), EditorPropType.LAB_BENCH, 45, 0, 20);
        registerLevelProp(app.getWorld().createServerRack("CoreRack", app.getWorld().worldToScene(35, 1.2f, 28), new ColorRGBA(0.22f, 0.82f, 1f, 1f)), EditorPropType.SERVER_RACK, 35, 1, 28);
        registerLevelProp(app.getWorld().createStorageCrate("CoreCrate", app.getWorld().worldToScene(44, 0.45f, 29), new ColorRGBA(0.95f, 0.78f, 0.22f, 1f)), EditorPropType.STORAGE_CRATE, 44, 0, 29);
        registerLevelProp(app.getWorld().createAnalyzerStation("CoreAnalyzer", app.getWorld().worldToScene(42, 0f, 22), new ColorRGBA(0.18f, 0.95f, 0.35f, 1f)), EditorPropType.ANALYZER_STATION, 42, 0, 22);
        registerLevelProp(app.getWorld().createTankCluster("CoreTanks", app.getWorld().worldToScene(37, 0f, 30), new ColorRGBA(0.22f, 0.85f, 1f, 1f)), EditorPropType.TANK_CLUSTER, 37, 0, 30);
        placeCeilingLights("Level4Light", new int[][]{
                {36, 24}, {40, 24}, {44, 24}, {38, 28}
        }, new ColorRGBA(0.6f, 0.95f, 0.7f, 1f));
    }

    private void placeCeilingLights(String prefix, int[][] positions, ColorRGBA color) {
        for (int i = 0; i < positions.length; i++) {
            int[] pos = positions[i];
            Spatial light = app.getWorld().createCeilingLight(prefix + i, app.getWorld().worldToScene(pos[0], 5.75f, pos[1]), color);
            registerLevelProp(light, EditorPropType.CEILING_LIGHT, pos[0], 5, pos[1]);
        }
    }

    /** Registers a spatial as a level prop, tagging it so the editor's ray-pick can find it. */
    private void registerLevelProp(Spatial spatial, EditorPropType type, int x, int y, int z) {
        if (spatial == null) return;
        String propId = "level-prop-" + nextLevelPropId++;
        app.getWorld().registerLevelProp(propId, spatial, type, x, y, z);
    }
}
