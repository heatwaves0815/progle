package de.progle.service;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ConfigService {

    private static final Gson GSON = new Gson();
    private static final String FILE_NAME = "config.json";

    public AiConfig load() {
        List<Path> candidates = new ArrayList<>();
        Path primary = AppDirs.resolve(FILE_NAME);
        candidates.add(primary);
        candidates.add(Paths.get(System.getProperty("user.home"), ".progle", FILE_NAME));
        for (Path path : candidates) {
            if (Files.exists(path)) {
                try {
                    AiConfig config = GSON.fromJson(stripBom(Files.readString(path, StandardCharsets.UTF_8)), AiConfig.class);
                    if (config != null) {
                        return config;
                    }
                } catch (Exception e) {
                    System.err.println("config.json konnte nicht gelesen werden (" + path + "): " + e.getMessage());
                }
            }
        }
        String envKey = System.getenv("OPENROUTER_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return new AiConfig(envKey, AiConfig.DEFAULT_MODEL, AiConfig.DEFAULT_BASE_URL);
        }
        return new AiConfig("", "", "");
    }

    private static String stripBom(String content) {
        return content != null && content.startsWith("\uFEFF") ? content.substring(1) : content;
    }
}
