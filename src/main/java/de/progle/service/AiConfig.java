package de.progle.service;

public record AiConfig(String apiKey, String model, String baseUrl) {

    public static final String DEFAULT_MODEL = "deepseek/deepseek-v4-flash-0731:free";
    public static final String DEFAULT_BASE_URL = "https://openrouter.ai/api/v1";

    public AiConfig {
        if (apiKey == null) {
            apiKey = "";
        }
        if (model == null || model.isBlank()) {
            model = DEFAULT_MODEL;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = DEFAULT_BASE_URL;
        }
        baseUrl = baseUrl.replaceAll("/+$", "");
        apiKey = apiKey.trim();
    }

    public boolean hasKey() {
        return !apiKey.isBlank() && !apiKey.toUpperCase().startsWith("HIER");
    }
}
