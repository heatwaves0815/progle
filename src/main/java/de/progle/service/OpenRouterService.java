package de.progle.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import de.progle.model.ChatMessage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class OpenRouterService {

    private static final Gson GSON = new Gson();

    private final AiConfig config;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public OpenRouterService(AiConfig config) {
        this.config = config;
    }

    public boolean hasKey() {
        return config.hasKey();
    }

    public void chatCompletionStream(List<ChatMessage> messages, boolean jsonMode,
                                     java.util.function.Consumer<String> onChunk,
                                     java.util.function.Consumer<String> onComplete) {
        if (!config.hasKey()) {
            throw new AiException("Kein API-Key konfiguriert. Trage deinen OpenRouter-API-Key in config.json ein "
                    + "(siehe README.md, Key erstellen auf https://openrouter.ai/keys).");
        }
        try {
            sendStreaming(messages, jsonMode, onChunk, onComplete);
        } catch (AiException e) {
            if (jsonMode && e.getStatusCode() != null && e.getStatusCode() == 400) {
                sendStreaming(messages, false, onChunk, onComplete);
                return;
            }
            throw e;
        }
    }

    public String chatCompletion(List<ChatMessage> messages, boolean jsonMode) {
        if (!config.hasKey()) {
            throw new AiException("Kein API-Key konfiguriert. Trage deinen OpenRouter-API-Key in config.json ein "
                    + "(siehe README.md, Key erstellen auf https://openrouter.ai/keys).");
        }
        try {
            return sendRequest(messages, jsonMode);
        } catch (AiException e) {
            if (jsonMode && e.getStatusCode() != null && e.getStatusCode() == 400) {
                return sendRequest(messages, false);
            }
            throw e;
        }
    }

    private void sendStreaming(List<ChatMessage> messages, boolean jsonMode,
                               java.util.function.Consumer<String> onChunk,
                               java.util.function.Consumer<String> onComplete) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", config.model());
            body.addProperty("temperature", 0.6);
            body.addProperty("stream", true);
            if (jsonMode) {
                JsonObject responseFormat = new JsonObject();
                responseFormat.addProperty("type", "json_object");
                body.add("response_format", responseFormat);
            }
            JsonArray messageArray = new JsonArray();
            for (ChatMessage message : messages) {
                JsonObject entry = new JsonObject();
                entry.addProperty("role", message.role());
                entry.addProperty("content", message.content());
                messageArray.add(entry);
            }
            body.add("messages", messageArray);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.baseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(180))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", "https://localhost/progle")
                    .header("X-Title", "Progle")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new AiException(friendlyHttpError(response.statusCode())
                        + " Details: " + snippet(errorBody), response.statusCode());
            }
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if (data.equals("[DONE]")) break;
                    try {
                        JsonObject chunk = JsonParser.parseString(data).getAsJsonObject();
                        var choices = chunk.getAsJsonArray("choices");
                        if (choices != null && choices.size() > 0) {
                            var delta = choices.get(0).getAsJsonObject().getAsJsonObject("delta");
                            if (delta != null && delta.has("content")) {
                                String content = delta.get("content").getAsString();
                                if (!content.isEmpty()) {
                                    onChunk.accept(content);
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            onComplete.accept("");
        } catch (IOException e) {
            throw new AiException("Netzwerkfehler bei der KI-Anfrage: " + e.getMessage()
                    + ". Prüfe deine Internetverbindung.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("Die KI-Anfrage wurde abgebrochen.", e);
        }
    }

    private String sendRequest(List<ChatMessage> messages, boolean jsonMode) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", config.model());
            body.addProperty("temperature", 0.6);
            if (jsonMode) {
                JsonObject responseFormat = new JsonObject();
                responseFormat.addProperty("type", "json_object");
                body.add("response_format", responseFormat);
            }
            JsonArray messageArray = new JsonArray();
            for (ChatMessage message : messages) {
                JsonObject entry = new JsonObject();
                entry.addProperty("role", message.role());
                entry.addProperty("content", message.content());
                messageArray.add(entry);
            }
            body.add("messages", messageArray);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.baseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(180))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", "https://localhost/progle")
                    .header("X-Title", "Progle")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new AiException(friendlyHttpError(response.statusCode())
                        + " Details: " + snippet(response.body()), response.statusCode());
            }
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            return root.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        } catch (IOException e) {
            throw new AiException("Netzwerkfehler bei der KI-Anfrage: " + e.getMessage()
                    + ". Prüfe deine Internetverbindung.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("Die KI-Anfrage wurde abgebrochen.", e);
        } catch (JsonSyntaxException | IllegalStateException | ClassCastException e) {
            throw new AiException("Unerwartete Antwort der KI-API: " + e.getMessage(), e);
        }
    }

    private static String friendlyHttpError(int statusCode) {
        return switch (statusCode) {
            case 401 -> "Der API-Key ist ungültig oder abgelaufen (HTTP 401). Prüfe den Key in config.json bzw. erstelle einen neuen auf https://openrouter.ai/keys.";
            case 402 -> "Das Guthaben für diesen API-Key ist aufgebraucht (HTTP 402). Lade Guthaben auf https://openrouter.ai/credits auf.";
            case 404 -> "Das Modell wurde nicht gefunden (HTTP 404). Prüfe den Modellnamen in config.json (Modelle: https://openrouter.ai/models).";
            case 429 -> "Rate-Limit erreicht (HTTP 429). Bitte kurz warten und es erneut versuchen.";
            case 502, 503 -> "Der KI-Anbieter ist gerade nicht erreichbar (HTTP " + statusCode + "). Bitte später erneut versuchen.";
            default -> "Die KI-API hat einen Fehler gemeldet (HTTP " + statusCode + ").";
        };
    }

    private String snippet(String body) {
        if (body == null) {
            return "";
        }
        String compact = body.replace("\n", " ").trim();
        return compact.length() > 300 ? compact.substring(0, 300) + " ..." : compact;
    }
}
