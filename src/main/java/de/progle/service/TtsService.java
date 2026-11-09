package de.progle.service;

import javafx.application.Platform;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Live-Sprachausgabe mit Piper: Die Stimme wird Satz fuer Satz direkt beim
 * Generieren abgespielt (Streaming), ohne dass vorher die ganze Datei fertig sein muss.
 */
public class TtsService {

    public interface Listener {
        void onSpeakingChanged(boolean speaking);

        default void onError(String message) {
        }
    }

    private static final AudioFormat PIPER_FORMAT =
            new AudioFormat(22050f, 16, 1, true, false);

    private final Path piperExe;
    private final Path voice;
    private final String preferredDevice;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "progle-tts");
        thread.setDaemon(true);
        return thread;
    });

    private volatile Process process;
    private volatile SourceDataLine line;
    private volatile boolean speaking;
    private volatile boolean stopRequested;

    public TtsService() {
        Path base = AppDirs.resolve("tts");
        this.piperExe = base.resolve("piper").resolve("piper.exe");
        this.voice = findVoice(base.resolve("voices"));
        this.preferredDevice = readPreferredDevice(base);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    private Path findVoice(Path voicesDir) {
        if (!Files.isDirectory(voicesDir)) {
            return null;
        }
        try (Stream<Path> files = Files.list(voicesDir)) {
            return files
                    .filter(p -> p.toString().toLowerCase().endsWith(".onnx"))
                    .sorted()
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String readPreferredDevice(Path base) {
        Path file = base.resolve("tts-device.txt");
        if (!Files.isRegularFile(file)) {
            return "";
        }
        try {
            String content = Files.readString(file).trim();
            return content.isEmpty() ? "" : content.toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }

    public boolean isAvailable() {
        return Files.isRegularFile(piperExe) && voice != null;
    }

    public List<String> outputDeviceNames() {
        List<String> names = new ArrayList<>();
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            String name = info.getName().toLowerCase();
            if (!name.contains("micro") && !name.contains("mikro") && !name.contains("capture")) {
                names.add(info.getName());
            }
        }
        return names;
    }

    public void speak(String text, Listener listener) {
        stop();
        if (!isAvailable() || text == null || text.isBlank()) {
            return;
        }
        stopRequested = false;
        speaking = true;
        notify(listener, true);
        executor.submit(() -> {
            try {
                speakStreaming(text, listener);
            } catch (Exception e) {
                if (!stopRequested) {
                    error(listener, "Sprachausgabe-Fehler: " + e.getMessage());
                }
            } finally {
                cleanup();
                speaking = false;
                notify(listener, false);
            }
        });
    }

    private void speakStreaming(String text, Listener listener) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(
                piperExe.toString(),
                "--model", voice.toAbsolutePath().toString(),
                "--output-raw");
        builder.directory(piperExe.getParent().toFile());
        Process started = builder.start();
        process = started;

        Thread stderrDrain = drain(started.getErrorStream());

        try (OutputStream stdin = started.getOutputStream()) {
            stdin.write(toLines(text).getBytes(StandardCharsets.UTF_8));
            stdin.flush();
        }

        long start = System.currentTimeMillis();
        line = openLine(PIPER_FORMAT);
        line.start();

        byte[] buffer = new byte[8192];
        long total = 0;
        InputStream stdout = started.getInputStream();
        int read;
        while (speaking && !stopRequested && (read = stdout.read(buffer)) != -1) {
            if (total == 0) {
                long latency = System.currentTimeMillis() - start;
                System.out.println("Sprachausgabe: erster Ton nach " + latency + " ms");
            }
            total += read;
            line.write(buffer, 0, read);
        }
        stderrDrain.join(500);

        if (stopRequested) {
            return;
        }
        if (total == 0) {
            error(listener, "Keine Audiodaten erhalten. Prüfe den Ordner tts/.");
            return;
        }
        line.drain();
        if (!started.waitFor(10, TimeUnit.SECONDS)) {
            started.destroyForcibly();
        }
    }

    private String toLines(String text) {
        String[] sentences = text
                .replaceAll("(?<=[.!?])\\s+", "\n")
                .replace("\r\n", "\n")
                .split("\n", -1);
        StringBuilder out = new StringBuilder();
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            while (trimmed.length() > 220) {
                int cut = trimmed.lastIndexOf(' ', 220);
                if (cut <= 20) {
                    cut = 220;
                }
                out.append(trimmed, 0, cut).append("\n");
                trimmed = trimmed.substring(cut).trim();
            }
            out.append(trimmed).append("\n");
        }
        return out.toString();
    }

    private Thread drain(InputStream stream) {
        Thread thread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            try {
                while (stream.read(buffer) != -1) {
                    // nur abgrasen, damit Pipers Fehlerausgabe den Prozess nicht blockiert
                }
            } catch (Exception ignored) {
            }
        }, "progle-tts-stderr");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    public void stop() {
        stopRequested = true;
        speaking = false;
        Process running = process;
        if (running != null) {
            running.destroyForcibly();
            process = null;
        }
        closeLine();
    }

    private void cleanup() {
        Process running = process;
        if (running != null) {
            running.destroyForcibly();
            process = null;
        }
        closeLine();
    }

    private void closeLine() {
        SourceDataLine active = line;
        if (active != null) {
            try {
                active.stop();
                active.close();
            } catch (Exception ignored) {
            }
            line = null;
        }
    }

    private SourceDataLine openLine(AudioFormat format) throws Exception {
        if (preferredDevice != null && !preferredDevice.isBlank()) {
            for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                if (info.getName().toLowerCase().contains(preferredDevice)) {
                    SourceDataLine selected = (SourceDataLine) AudioSystem.getMixer(info)
                            .getLine(new DataLine.Info(SourceDataLine.class, format));
                    selected.open(format, 65536);
                    return selected;
                }
            }
        }
        SourceDataLine defaultLine = (SourceDataLine) AudioSystem.getLine(
                new DataLine.Info(SourceDataLine.class, format));
        defaultLine.open(format, 65536);
        return defaultLine;
    }

    private void error(Listener listener, String message) {
        System.err.println("Sprachausgabe: " + message);
        if (listener != null) {
            Platform.runLater(() -> listener.onError(message));
        }
    }

    private void notify(Listener listener, boolean nowSpeaking) {
        if (listener != null) {
            Platform.runLater(() -> listener.onSpeakingChanged(nowSpeaking));
        }
    }
}
