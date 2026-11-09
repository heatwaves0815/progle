package de.progle.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Findet Programm-Ordner (Lehrplan, TTS, Konfiguration) - im Entwickler-Ordner
 * oder, wenn als Progle.exe gestartet, neben der Exe-Datei - unabhaengig vom Arbeitsverzeichnis.
 */
public final class AppDirs {

    private AppDirs() {
    }

    public static Path resolve(String child) {
        Path local = Paths.get(child).toAbsolutePath();
        if (Files.exists(local)) {
            return local;
        }
        Path appHome = appHome();
        if (appHome != null) {
            Path bundled = appHome.resolve(child).toAbsolutePath();
            if (Files.exists(bundled)) {
                return bundled;
            }
        }
        return local;
    }

    private static Path appHome() {
        String prop = System.getProperty("jpackage.app-home");
        if (prop != null && !prop.isBlank()) {
            return Paths.get(prop);
        }
        try {
            String command = ProcessHandle.current().info().command().orElse(null);
            if (command != null && command.toLowerCase().endsWith(".exe")) {
                Path exeDir = Paths.get(command).toAbsolutePath().getParent();
                if (exeDir != null && Files.exists(exeDir.resolve("app"))) {
                    return exeDir;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
