package de.progle.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.progle.model.ProgressData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class ProgressStorageService {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path DIRECTORY = Paths.get(System.getProperty("user.home"), ".progle");
    private static final Path FILE = DIRECTORY.resolve("progress.json");

    public Optional<ProgressData> load() {
        if (!Files.exists(FILE)) {
            return Optional.empty();
        }
        try {
            ProgressData data = GSON.fromJson(stripBom(Files.readString(FILE, StandardCharsets.UTF_8)), ProgressData.class);
            if (data == null || data.getProfile() == null || data.getLessons() == null || data.getLessons().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(data);
        } catch (Exception e) {
            System.err.println("Fortschritt konnte nicht geladen werden: " + e.getMessage());
            return Optional.empty();
        }
    }

    private static String stripBom(String content) {
        return content.startsWith("\uFEFF") ? content.substring(1) : content;
    }

    public void save(ProgressData data) {
        try {
            Files.createDirectories(DIRECTORY);
            Files.writeString(FILE, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Fortschritt konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    public void clear() {
        try {
            Files.deleteIfExists(FILE);
        } catch (IOException e) {
            System.err.println("Fortschritt konnte nicht gelöscht werden: " + e.getMessage());
        }
    }
}
