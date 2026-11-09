package de.progle.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Persistente App-Einstellungen (z. B. gewähltes Design) in ~/.progle/settings.json.
 */
public class SettingsService {

    public static final Set<String> THEMES = Set.of("dark", "midnight", "nord", "light");
    public static final String DEFAULT_THEME = "dark";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Paths.get(System.getProperty("user.home"), ".progle", "settings.json");

    public static class Settings {
        public String theme;
    }

    private String theme = DEFAULT_THEME;

    public SettingsService() {
        load();
    }

    private void load() {
        if (!Files.exists(FILE)) {
            return;
        }
        try {
            String raw = Files.readString(FILE, StandardCharsets.UTF_8);
            if (raw.startsWith("\uFEFF")) {
                raw = raw.substring(1);
            }
            Settings settings = GSON.fromJson(raw, Settings.class);
            if (settings != null && settings.theme != null && THEMES.contains(settings.theme)) {
                theme = settings.theme;
            }
        } catch (Exception e) {
            System.err.println("Einstellungen konnten nicht geladen werden: " + e.getMessage());
        }
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        if (!THEMES.contains(theme)) {
            return;
        }
        this.theme = theme;
        try {
            Files.createDirectories(FILE.getParent());
            Settings settings = new Settings();
            settings.theme = theme;
            Files.writeString(FILE, GSON.toJson(settings), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Einstellungen konnten nicht gespeichert werden: " + e.getMessage());
        }
    }
}
