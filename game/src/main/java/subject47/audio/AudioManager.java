package subject47.audio;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import javazoom.jl.player.Player;

import java.io.File;
import java.io.FileInputStream;


public class AudioManager {

    private static final String INTRO_MUSIC_FILE = "assets/Audio/Game intro music.mp3";
    private static final String MENU_MUSIC_FILE  = "assets/Audio/MainMenu bg music.mp3";

    private final SimpleApplication app;

    // Intro music (JLayer Player)
    private Player  introPlayer;
    private Thread  introThread;
    private volatile boolean introStopRequested = false;

    // Menu music (JLayer Decoder + SourceDataLine)
    private volatile javax.sound.sampled.SourceDataLine menuLine;
    private Thread  menuThread;
    private volatile boolean menuStopRequested = false;

    public AudioManager(SimpleApplication app) { this.app = app; }

    public void initialize() {}

    // ── Intro Music ──────────────────────────────────────────────────────────

    public void playIntroMusic() {
        stopIntroMusic();
        introStopRequested = false;
        introThread = new Thread(() -> {
            try (FileInputStream fis = new FileInputStream(INTRO_MUSIC_FILE)) {
                introPlayer = new Player(fis);
                introPlayer.play();
            } catch (Exception e) {
                if (!introStopRequested)
                    System.err.println("[AudioManager] Intro error: " + e.getMessage());
            }
        });
        introThread.setDaemon(true);
        introThread.setName("IntroMusicThread");
        introThread.start();
    }

    public void stopIntroMusic() {
        introStopRequested = true;
        if (introPlayer != null) { try { introPlayer.close(); } catch (Exception ignored) {} introPlayer = null; }
        if (introThread  != null) { introThread.interrupt(); introThread = null; }
    }


    public void playMenuMusic() {
        stopMenuMusic();           // ensure clean slate
        menuStopRequested = false;

        menuThread = new Thread(() -> {
            while (!menuStopRequested) {
                javax.sound.sampled.SourceDataLine line = null;
                try (FileInputStream fis = new FileInputStream(MENU_MUSIC_FILE)) {
                    javazoom.jl.decoder.Bitstream bs  = new javazoom.jl.decoder.Bitstream(fis);
                    javazoom.jl.decoder.Decoder   dec = new javazoom.jl.decoder.Decoder();
                    javazoom.jl.decoder.Header    hdr;

                    while (!menuStopRequested && (hdr = bs.readFrame()) != null) {
                        javazoom.jl.decoder.SampleBuffer sb =
                                (javazoom.jl.decoder.SampleBuffer) dec.decodeFrame(hdr, bs);

                        // Open the audio line on the first decoded frame
                        if (line == null) {
                            javax.sound.sampled.AudioFormat fmt =
                                new javax.sound.sampled.AudioFormat(
                                    hdr.frequency(), 16, sb.getChannelCount(), true, false);
                            javax.sound.sampled.DataLine.Info info =
                                new javax.sound.sampled.DataLine.Info(
                                    javax.sound.sampled.SourceDataLine.class, fmt);
                            line = (javax.sound.sampled.SourceDataLine)
                                    javax.sound.sampled.AudioSystem.getLine(info);
                            line.open(fmt);
                            line.start();
                            menuLine = line;    // expose so stopMenuMusic() can reach it
                        }

                        // Convert short[] PCM → byte[] little-endian and write
                        short[] pcm   = sb.getBuffer();
                        int     len   = sb.getBufferLength();
                        byte[]  bytes = new byte[len * 2];
                        for (int i = 0; i < len; i++) {
                            bytes[i * 2]     = (byte)  (pcm[i] & 0xFF);
                            bytes[i * 2 + 1] = (byte) ((pcm[i] >> 8) & 0xFF);
                        }
                        if (!menuStopRequested) line.write(bytes, 0, bytes.length);
                        bs.closeFrame();
                    }
                    bs.close();

                } catch (Exception e) {
                    if (!menuStopRequested)
                        System.err.println("[AudioManager] Menu music error: " + e.getMessage());
                    break;
                } finally {
                    // Always clean up the line for this iteration
                    javax.sound.sampled.SourceDataLine l = line;
                    if (l != null) {
                        menuLine = null;
                        try { l.stop();  } catch (Exception ignored) {}
                        try { l.flush(); } catch (Exception ignored) {}
                        try { l.close(); } catch (Exception ignored) {}
                    }
                }
            }
        });
        menuThread.setDaemon(true);
        menuThread.setName("MenuMusicThread");
        menuThread.start();
    }

    public void stopMenuMusic() {
        menuStopRequested = true;
        // stop()+flush() unblocks write() on the music thread instantly
        javax.sound.sampled.SourceDataLine line = menuLine;
        if (line != null) {
            menuLine = null;
            try { line.stop();  } catch (Exception ignored) {}
            try { line.flush(); } catch (Exception ignored) {}
            try { line.close(); } catch (Exception ignored) {}
        }
        Thread t = menuThread;
        if (t != null) { menuThread = null; t.interrupt(); }
    }

    // ── In-World Audio Logs ───────────────────────────────────────────────────

    public void playAudioLog(String path) {
        try {
            AudioNode audio = new AudioNode(app.getAssetManager(), path, AudioData.DataType.Buffer);
            audio.setPositional(false);
            audio.setLooping(false);
            audio.setVolume(1.2f);
            app.getRootNode().attachChild(audio);
            audio.playInstance();
        } catch (Exception e) {
            System.out.println("[AudioManager] Missing audio asset: " + path);
        }
    }
}
