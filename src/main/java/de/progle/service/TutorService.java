package de.progle.service;

import com.google.gson.Gson;
import de.progle.model.ChatMessage;
import de.progle.model.Exercise;
import de.progle.model.LessonData;
import de.progle.model.UserProfile;
import de.progle.service.CodeRunnerService.RunResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TutorService {

    private static final Gson GSON = new Gson();

    private static final String TUTOR_PROMPT = """
            Du bist "Progle", ein geduldiger, motivierender Java-Tutor für absolute Anfänger.
            Du sprichst Deutsch und verwendest einfache, kurze Sätze. Fachbegriffe erklärst du sofort.

            Regeln (immer einhalten):
            1. Zeige NIEMALS die Musterlösung der aktuellen Übung - auch nicht in Teilen, die direkt kopierbar wären. Der Lernende soll sie selbst schreiben. In deinem Kontext stehen Musterlösungen nur zu deiner Orientierung.
            2. Wenn der Lernende feststeckt, verweise auf die drei Hinweis-Stufen der Übung (Denkanstoß, Tipp, Teilansatz) und erkläre sie mit eigenen Worten - ohne den fertigen Code zu liefern.
            3. Wenn Code nicht kompiliert oder eine Exception wirft, erkläre die Fehlermeldung auf Deutsch: was sie bedeutet, wo sie wahrscheinlich entsteht und wie der Lernende selbst ansetzt. Korrigiere den Fehler nicht einfach selbst.
            4. Auf Wunsch zeigst du vollständige, lauffähige BEISPIELE - aber niemals die Lösung der aktuellen Aufgabe. Wähle für Beispiele immer ein anderes Thema.
            5. Bei Code-Prüfungen antworte strukturiert mit drei kurzen Abschnitten:
               "Gut gemacht:", "Was fehlt:", "Fehler:" - jeweils mit kurzer Begründung.
               Sei ehrlich, aber ermutigend.
            6. Antworte knapp und auf den Punkt. Überfordere den Lernenden nicht.
            """;

    private final OpenRouterService backend;

    public TutorService(OpenRouterService backend) {
        this.backend = backend;
    }

    public boolean hasApiKey() {
        return backend.hasKey();
    }

    public interface StreamCallback {
        void onChunk(String chunk);

        default void onError(String message) {
        }
    }

    public void reviewCodeStreaming(UserProfile profile, LessonData lesson, Exercise exercise,
                                    String code, RunResult run, StreamCallback callback) {
        StringBuilder request = buildReviewRequest(lesson, exercise, code, run);
        StringBuilder collected = new StringBuilder();
        backend.chatCompletionStream(
                List.of(ChatMessage.system(systemPrompt(profile, lesson)), ChatMessage.user(request.toString())), false,
                chunk -> {
                    collected.append(chunk);
                    callback.onChunk(chunk);
                },
                done -> {
                });
    }

    public ReviewResult parseReviewResult(String feedback) {
        return new ReviewResult(feedback, parseVerdict(feedback));
    }

    private StringBuilder buildReviewRequest(LessonData lesson, Exercise exercise, String code, RunResult run) {
        StringBuilder request = new StringBuilder();
        request.append("Prüfe bitte meinen Lösungsversuch.\n\n")
                .append("AUFGABE:\n").append(exercise.getAufgabenstellung()).append("\n\n")
                .append("MEIN CODE:\n```java\n").append(code).append("\n```\n\n");
        if (!run.compiled()) {
            request.append("Der Code kompiliert NICHT. Compiler-Meldungen:\n")
                    .append(run.compileErrors()).append("\n\n")
                    .append("Erkläre mir die Fehlermeldungen auf Deutsch: was sie bedeuten und wo sie entstehen. Gib mir eine Starthilfe, aber korrigiere den Code nicht komplett.\n");
        } else {
            if (run.timedOut()) {
                request.append("Der Code wurde kompiliert, aber das Programm lief länger als 10 Sekunden und wurde abgebrochen (vermutlich Endlosschleife oder fehlende Eingabe).\n");
            } else if (run.hasProgramOutput()) {
                request.append("Der Code wurde kompiliert und ausgeführt. Programmausgabe:\n")
                        .append(run.programOutput()).append("\n");
            } else {
                request.append("Der Code wurde kompiliert und ausgeführt, hat aber nichts ausgegeben.\n");
            }
            if (!run.programErrors().isBlank()) {
                request.append("Fehlerausgabe des Programms:\n").append(run.programErrors()).append("\n");
            }
            if (exercise.getPrüfung() != null) {
                request.append("ERWARTETE Ausgabe laut Aufgabe:\n")
                        .append(exercise.getPrüfung().getErwartet()).append("\n\n")
                        .append("Vergleiche meine Ausgabe mit der erwarteten Ausgabe und sage mir, ob sie übereinstimmt.\n");
            }
        }
        request.append("\nBeende deine Antwort mit genau einer Zeile: \"ERGEBNIS: BESTANDEN\" oder \"ERGEBNIS: NICHT BESTANDEN\".");

        return request;
    }

    public ReviewResult reviewCode(UserProfile profile, LessonData lesson, Exercise exercise,
                                   String code, RunResult run) {
        StringBuilder request = buildReviewRequest(lesson, exercise, code, run);
        String answer = backend.chatCompletion(
                List.of(ChatMessage.system(systemPrompt(profile, lesson)), ChatMessage.user(request.toString())), false);
        return new ReviewResult(answer, parseVerdict(answer));
    }

    public record ReviewResult(String feedback, boolean passed) {
    }

    private static boolean parseVerdict(String answer) {
        if (answer == null) {
            return false;
        }
        String upper = answer.toUpperCase(Locale.ROOT);
        int index = upper.lastIndexOf("ERGEBNIS:");
        if (index < 0) {
            return false;
        }
        String tail = upper.substring(index);
        return tail.contains("BESTANDEN") && !tail.contains("NICHT");
    }

    public String answer(UserProfile profile, LessonData lesson, List<ChatMessage> history) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(systemPrompt(profile, lesson)));
        messages.addAll(history);
        return backend.chatCompletion(messages, false);
    }

    public void answerStreaming(UserProfile profile, LessonData lesson, List<ChatMessage> history,
                                StreamCallback callback) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(systemPrompt(profile, lesson)));
        messages.addAll(history);
        backend.chatCompletionStream(messages, false,
                callback::onChunk,
                done -> {
                });
    }

    private String systemPrompt(UserProfile profile, LessonData lesson) {
        StringBuilder context = new StringBuilder();
        context.append("\n\nAKTUELLE SITUATION:\n")
                .append("Name des Lernenden: ").append(profile.getName()).append("\n")
                .append("Vorkenntnisse: ").append(profile.getLevel().getDisplayName()).append("\n");
        if (lesson != null) {
            context.append("Lektion: ").append(lesson.getTitel())
                    .append(" (Typ: ").append(lesson.getTyp()).append(")\n")
                    .append("Lernziele: ").append(String.join("; ", lesson.getLernziele())).append("\n")
                    .append("Lektions-Inhalt (gekürzt):\n").append(truncate(lesson.getErklärung(), 1600)).append("\n");
            if (!lesson.getÜbungen().isEmpty()) {
                context.append("Übungen dieser Lektion (mit Musterlösungen - NIEMALS anzeigen!):\n");
                for (int i = 0; i < lesson.getÜbungen().size(); i++) {
                    Exercise exercise = lesson.getÜbungen().get(i);
                    context.append("--- Übung ").append(i + 1).append(" ---\n")
                            .append("Aufgabe: ").append(exercise.getAufgabenstellung()).append("\n")
                            .append("Musterlösung (geheim!):\n").append(truncate(exercise.getLösung(), 1200)).append("\n");
                }
            }
        } else {
            context.append("Aktuelle Lektion: keine ausgewählt - antworte allgemein zu Java-Grundlagen.\n");
        }
        return TUTOR_PROMPT + context;
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() > max ? text.substring(0, max) + " ..." : text;
    }
}
