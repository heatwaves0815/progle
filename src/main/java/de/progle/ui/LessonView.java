package de.progle.ui;

import de.progle.model.Curriculum;
import de.progle.model.Exercise;
import de.progle.model.LessonData;
import de.progle.model.LessonProgress;
import de.progle.model.ProgressData;
import de.progle.service.AiException;
import de.progle.service.CodeRunnerService;
import de.progle.service.ProgressStorageService;
import de.progle.service.TutorService;
import de.progle.service.TtsService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LessonView extends ScrollPane {

    private final ProgressData data;
    private final Curriculum curriculum;
    private final TutorService tutor;
    private final ProgressStorageService storage;
    private final CodeRunnerService runner;
    private final TtsService tts;
    private final Runnable onProgressChanged;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "progle-lesson");
        thread.setDaemon(true);
        return thread;
    });

    private LessonData lesson;
    private LessonProgress progress;
    private int exIndex;
    private boolean busy;
    private int speakRequest;
    private Button activeSpeakerButton;

    private final VBox lessonCard = new VBox(14);
    private final Label title = new Label();
    private final Label typChip = new Label();
    private final VBox goalsBox = new VBox(6);
    private final VBox erklaerungBox = new VBox(7);
    private final VBox quizBox = new VBox(10);
    private final VBox fehlerBox = new VBox(8);
    private final Label exHeader = new Label();
    private final Label exStatusChip = new Label();
    private final Label taskText = new Label();
    private final Button hintButton = new Button("Hinweis (Stufe 1)");
    private final Label hintInfo = new Label();
    private final Label hintText = new Label();
    private final CodeArea codeArea = new CodeArea();
    private final Button submitButton = new Button("Abgeben und prüfen");
    private final TextArea resultArea = new TextArea();
    private final Button prevExButton = new Button("Zurück");
    private final Button nextExButton = new Button("Weiter");
    private final Button speakLessonButton = new Button("Vorlesen");
    private final Button speakExerciseButton = new Button("Aufgabe vorlesen");
    private final VBox quizResultBox = new VBox(8);
    private final Label completionChip = new Label();

    public LessonView(ProgressData data, Curriculum curriculum, TutorService tutor,
                      ProgressStorageService storage, CodeRunnerService runner, TtsService tts,
                      Runnable onProgressChanged) {
        this.data = data;
        this.curriculum = curriculum;
        this.tutor = tutor;
        this.storage = storage;
        this.runner = runner;
        this.tts = tts;
        this.onProgressChanged = onProgressChanged;

        getStyleClass().add("pane-transparent");
        setFitToWidth(true);
        setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setMinWidth(0);
        VBox.setVgrow(this, Priority.ALWAYS);

        buildStaticUi();
    }

    private void buildStaticUi() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        title.getStyleClass().add("lesson-title");
        typChip.getStyleClass().add("chip");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        speakLessonButton.getStyleClass().add("link");
        speakLessonButton.setGraphic(Icons.create(Icons.VOLUME, 15, "icon-accent"));
        speakLessonButton.setOnAction(e -> {
            if (tts.isAvailable() && activeSpeakerButton == speakLessonButton) {
                stopSpeaking();
            } else {
                startSpeaking(lessonSpeech(lesson), speakLessonButton);
            }
        });
        header.getChildren().addAll(title, typChip, headerSpacer, speakLessonButton);

        Label goalsHeading = new Label("Lernziele");
        goalsHeading.getStyleClass().add("section-heading");
        Label erklaerungHeading = new Label("Erklärung");
        erklaerungHeading.getStyleClass().add("section-heading");

        VBox goalsCard = new VBox(8, goalsHeading, goalsBox);
        goalsCard.getStyleClass().addAll("card", "sub-card");
        goalsCard.setMinWidth(0);

        VBox erklaerungCard = new VBox(10, erklaerungHeading, erklaerungBox);
        erklaerungCard.getStyleClass().addAll("card", "sub-card");
        erklaerungCard.setMinWidth(0);

        Label beispielHeading = new Label("Beispiel-Code");
        beispielHeading.getStyleClass().add("section-heading");
        CodeArea beispielAreaLocal = new CodeArea();
        beispielAreaLocal.getStyleClass().add("code-area");
        beispielAreaLocal.setEditable(false);
        beispielAreaLocal.setPrefHeight(210);
        beispielAreaLocal.textProperty().addListener((obs, oldText, newText) -> {
            try {
                StyleSpans<Collection<String>> spans = JavaSyntaxHighlighter.compute(newText);
                if (spans != null) {
                    beispielAreaLocal.setStyleSpans(0, spans);
                }
            } catch (Exception ignored) {
            }
        });
        VBox beispielCard = new VBox(10, beispielHeading, beispielAreaLocal);
        beispielCard.getStyleClass().addAll("card", "sub-card");
        beispielCard.setMinWidth(0);
        this.beispielArea = beispielAreaLocal;

        Label fehlerHeading = new Label("Typische Anfängerfehler");
        fehlerHeading.getStyleClass().add("section-heading");
        VBox fehlerCard = new VBox(10, fehlerHeading, fehlerBox);
        fehlerCard.getStyleClass().addAll("card", "sub-card");
        fehlerCard.setMinWidth(0);

        quizResultBox.getStyleClass().add("quiz-section");
        VBox quizCard = new VBox(10, quizResultBox);
        quizCard.getStyleClass().addAll("card", "sub-card");
        quizCard.setMinWidth(0);

        HBox exHeaderBar = new HBox(12);
        exHeaderBar.setAlignment(Pos.CENTER_LEFT);
        exHeader.getStyleClass().add("section-heading");
        exStatusChip.getStyleClass().add("chip");
        Region exSpacer = new Region();
        HBox.setHgrow(exSpacer, Priority.ALWAYS);
        prevExButton.getStyleClass().add("link");
        nextExButton.getStyleClass().add("link");
        speakExerciseButton.getStyleClass().add("link");
        speakExerciseButton.setGraphic(Icons.create(Icons.VOLUME, 13, "icon-accent"));
        speakExerciseButton.setOnAction(e -> {
            if (tts.isAvailable() && activeSpeakerButton == speakExerciseButton) {
                stopSpeaking();
            } else if (lesson != null && !lesson.getÜbungen().isEmpty()) {
                Exercise exercise = lesson.getÜbungen().get(exIndex);
                startSpeaking("Aufgabe: " + stripMarkdown(exercise.getAufgabenstellung()), speakExerciseButton);
            }
        });
        exHeaderBar.getChildren().addAll(exHeader, exStatusChip, exSpacer, prevExButton, nextExButton, speakExerciseButton);

        taskText.setWrapText(true);
        taskText.getStyleClass().add("task-text");

        hintInfo.getStyleClass().add("muted");
        hintText.setWrapText(true);
        hintText.getStyleClass().add("hint-text");
        hintText.setVisible(false);
        hintText.setManaged(false);

        codeArea.getStyleClass().add("code-area");
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.setPrefHeight(240);
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            if (lesson != null && progress != null) {
                progress.setCode(exIndex, newText);
            }
            try {
                StyleSpans<Collection<String>> spans = JavaSyntaxHighlighter.compute(newText);
                if (spans != null) {
                    codeArea.setStyleSpans(0, spans);
                }
            } catch (Exception ignored) {
            }
        });
        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB && !event.isShiftDown() && !event.isControlDown()) {
                codeArea.insertText(codeArea.getCaretPosition(), "    ");
                event.consume();
            }
        });

        submitButton.getStyleClass().add("primary");
        submitButton.setGraphic(Icons.create(Icons.SEND, 15));
        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setPrefHeight(150);
        resultArea.setPromptText("Ergebnis und Feedback vom Tutor erscheinen hier.");
        resultArea.getStyleClass().add("feedback");

        VBox exerciseCard = new VBox(12, exHeaderBar, taskText, hintButton, hintText, hintInfo,
                codeArea, submitButton, resultArea);
        exerciseCard.getStyleClass().addAll("card", "sub-card", "exercise-card");
        exerciseCard.setMinWidth(0);

        completionChip.getStyleClass().add("chip-success");
        completionChip.setVisible(false);
        completionChip.setManaged(false);

        prevExButton.setOnAction(e -> { if (exIndex > 0) { exIndex--; loadExercise(); } });
        nextExButton.setOnAction(e -> {
            if (lesson != null && exIndex < lesson.getÜbungen().size() - 1) {
                exIndex++;
                loadExercise();
            }
        });
        hintButton.setOnAction(e -> giveHint());
        submitButton.setOnAction(e -> submit());
        Anim.hoverLift(submitButton);

        ScrollFix.forwardTo(codeArea, this);
        ScrollFix.forwardTo(beispielArea, this);
        ScrollFix.forwardTo(resultArea, this);

        lessonCard.getChildren().addAll(header, completionChip, goalsCard, erklaerungCard,
                beispielCard, exerciseCard, quizCard, fehlerCard);
        lessonCard.getStyleClass().addAll("card", "lesson-card");
        lessonCard.setMinWidth(0);
        lessonCard.setPadding(new Insets(24));
        lessonCard.setMaxWidth(Double.MAX_VALUE);

        VBox outer = new VBox(lessonCard);
        outer.setPadding(new Insets(16));
        outer.setMinWidth(0);
        outer.getStyleClass().add("pane-transparent");
        setContent(outer);
    }

    private CodeArea beispielArea;

    public void loadLesson(LessonData lessonData, LessonProgress lessonProgress) {
        stopSpeaking();
        this.lesson = lessonData;
        this.progress = lessonProgress;
        this.exIndex = firstOpenExercise();
        progress.ensureSize(lesson.getÜbungen().size(), lesson.getQuiz().size());

        title.setText(lesson.getTitel());
        typChip.setText(typLabel(lesson.getTyp()));

        goalsBox.getChildren().clear();
        for (String ziel : lesson.getLernziele()) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            Label zielLabel = new Label(ziel);
            zielLabel.setWrapText(true);
            zielLabel.getStyleClass().add("md-p");
            row.getChildren().addAll(Icons.create(Icons.CHECK, 14, "icon-success"), zielLabel);
            goalsBox.getChildren().add(row);
        }

        erklaerungBox.getChildren().clear();
        erklaerungBox.getChildren().addAll(MarkdownView.build(lesson.getErklärung()));

        beispielArea.replaceText(lesson.getBeispielCode());

        fehlerBox.getChildren().clear();
        for (de.progle.model.TypicalError error : lesson.getTypischeFehler()) {
            Label fehler = new Label(error.getFehler());
            fehler.getStyleClass().add("md-bold");
            fehler.setWrapText(true);
            Label erklaerung = new Label(error.getErklärung());
            erklaerung.getStyleClass().add("md-p");
            erklaerung.setWrapText(true);
            fehlerBox.getChildren().addAll(fehler, erklaerung);
        }
        if (lesson.getTypischeFehler().isEmpty()) {
            fehlerBox.getChildren().add(new Label("Keine bekannten Stolperfallen - gut aufgepasst!"));
        }

        buildQuiz();
        loadExercise();
        updateCompletionChip();
        updateSpeakButtons(false);
        Anim.fadeSlideIn(lessonCard);
        Platform.runLater(() -> setVvalue(0));
    }

    private int firstOpenExercise() {
        if (progress == null) {
            return 0;
        }
        for (int i = 0; i < lesson.getÜbungen().size(); i++) {
            if (!progress.isÜbungFertig(i)) {
                return i;
            }
        }
        return 0;
    }

    private void loadExercise() {
        if (lesson == null || lesson.getÜbungen().isEmpty()) {
            exHeader.setText("Keine Übung in dieser Lektion");
            taskText.setText("");
            hintButton.setDisable(true);
            hintText.setText("");
            codeArea.replaceText("");
            submitButton.setDisable(true);
            updateExHeaderBar();
            return;
        }
        Exercise exercise = lesson.getÜbungen().get(exIndex);
        boolean projekt = "projekt".equals(lesson.getTyp());
        exHeader.setText((projekt ? "Schritt " : "Übung ") + (exIndex + 1) + " von " + lesson.getÜbungen().size());
        taskText.setText(exercise.getAufgabenstellung());
        codeArea.replaceText(progress.getCode(exIndex).isBlank()
                ? exercise.getStartCode()
                : progress.getCode(exIndex));
        submitButton.setDisable(busy);
        updateHintControls();
        updateExHeaderBar();
        Anim.fadeSlideIn(taskText);
    }

    private void updateExHeaderBar() {
        if (lesson == null || lesson.getÜbungen().isEmpty()) {
            exStatusChip.setText("");
            prevExButton.setVisible(false);
            nextExButton.setVisible(false);
            exStatusChip.setManaged(false);
            prevExButton.setManaged(false);
            nextExButton.setManaged(false);
            return;
        }
        boolean done = progress.isÜbungFertig(exIndex);
        exStatusChip.setText(done ? "erledigt" : "offen");
        exStatusChip.setVisible(true);
        exStatusChip.setManaged(true);
        prevExButton.setVisible(exIndex > 0);
        prevExButton.setManaged(exIndex > 0);
        nextExButton.setVisible(exIndex < lesson.getÜbungen().size() - 1);
        nextExButton.setManaged(exIndex < lesson.getÜbungen().size() - 1);
    }

    private void updateHintControls() {
        if (lesson == null || lesson.getÜbungen().isEmpty()) {
            return;
        }
        Exercise exercise = lesson.getÜbungen().get(exIndex);
        int stufe = progress.getHintStufe(exIndex);
        int max = exercise.getHinweise().size();
        hintInfo.setText("Hinweise genutzt: " + stufe + " von " + max);
        hintButton.setDisable(busy || stufe >= max);
        if (stufe >= max) {
            hintButton.setText("Alle Hinweise genutzt");
        } else {
            hintButton.setText("Hinweis (Stufe " + (stufe + 1) + ")");
        }
        if (stufe > 0) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < stufe && i < max; i++) {
                text.append("Stufe ").append(i + 1).append(": ")
                        .append(exercise.getHinweise().get(i)).append("\n");
            }
            hintText.setText(text.toString().strip());
            hintText.setVisible(true);
            hintText.setManaged(true);
        } else {
            hintText.setVisible(false);
            hintText.setManaged(false);
        }
    }

    private void giveHint() {
        if (busy || lesson == null || lesson.getÜbungen().isEmpty()) {
            return;
        }
        Exercise exercise = lesson.getÜbungen().get(exIndex);
        int stufe = progress.getHintStufe(exIndex);
        if (stufe >= exercise.getHinweise().size()) {
            return;
        }
        progress.setHintStufe(exIndex, stufe + 1);
        storage.save(data);
        updateHintControls();
    }

    private void submit() {
        if (busy || lesson == null || lesson.getÜbungen().isEmpty()) {
            return;
        }
        Exercise exercise = lesson.getÜbungen().get(exIndex);
        String code = codeArea.getText();
        if (code.isBlank()) {
            resultArea.setText("Schreibe zuerst etwas Code, dann schicke ich ihn zur Prüfung ein.");
            return;
        }
        progress.setCode(exIndex, code);
        persist();
        setBusy(true);
        final String testCode = de.progle.service.UmlautTransliterator.convertJavaStrings(code);
        executor.submit(() -> {
            String stdin = exercise.getPrüfung() != null && exercise.getPrüfung().hasEingabe()
                    ? exercise.getPrüfung().getEingabe() : null;
            CodeRunnerService.RunResult run = runner.compileAndRun(testCode, stdin);
            boolean passed = judge(run, exercise);

            StringBuilder local = new StringBuilder();
            local.append(passed ? "✓ Richtig! " : "✗ Noch nicht ganz. ");
            if (!run.compiled()) {
                local.append("Der Code kompiliert nicht:\n").append(run.compileErrors());
            } else if (run.timedOut()) {
                local.append("Das Programm lief länger als 10 Sekunden (Endlosschleife oder fehlende Eingabe).");
            } else if (exercise.getPrüfung() != null && !"enthält".equals(exercise.getPrüfung().getArt())) {
                local.append("Erwartete Ausgabe:\n").append(exercise.getPrüfung().getErwartet())
                        .append("\n\nDeine Ausgabe:\n").append(run.programOutput().isBlank() ? "(nichts)" : run.programOutput());
            }

            String localText = local.toString();
            boolean finalPassed = passed;
            // Lokales Testergebnis SOFORT zeigen
            Platform.runLater(() -> {
                if (lesson != LessonView.this.lesson) return;
                resultArea.setText(localText + "\n\n--- Tutor (live) ---\n");
                if (finalPassed) {
                    progress.setÜbungFertig(exIndex, true);
                    storage.save(data);
                    checkCompletion();
                }
                updateExHeaderBar();
            });

            // KI-Feedback live dazustreamen
            StringBuilder collected = new StringBuilder(localText).append("\n\n--- Tutor (live) ---\n");
            try {
                tutor.reviewCodeStreaming(data.getProfile(), lesson, exercise, testCode, run,
                        new de.progle.service.TutorService.StreamCallback() {
                            @Override
                            public void onChunk(String chunk) {
                                collected.append(chunk);
                                Platform.runLater(() -> {
                                    if (lesson != LessonView.this.lesson) return;
                                    resultArea.setText(collected.toString());
                                    resultArea.setScrollTop(Double.MAX_VALUE);
                                });
                            }

                            @Override
                            public void onError(String message) {
                                Platform.runLater(() -> {
                                    if (lesson != LessonView.this.lesson) return;
                                });
                            }
                        });
            } catch (AiException e) {
                Platform.runLater(() -> {
                    if (lesson != LessonView.this.lesson) return;
                    resultArea.setText(collected + "(Tutor nicht erreichbar: " + e.getMessage() + ")");
                });
            }
            Platform.runLater(() -> setBusy(false));
        });
    }

    private boolean judge(CodeRunnerService.RunResult run, Exercise exercise) {
        if (!run.compiled() || run.timedOut()) {
            return false;
        }
        if (exercise.getPrüfung() == null) {
            return run.programErrors().isBlank();
        }
        de.progle.model.Pruefung pruefung = exercise.getPrüfung();
        String expected = normalize(pruefung.getErwartet());
        String actual = normalize(run.programOutput());
        if ("enthält".equals(pruefung.getArt())) {
            return actual.contains(expected)
                    || de.progle.service.UmlautTransliterator.foldToAscii(actual)
                            .contains(de.progle.service.UmlautTransliterator.foldToAscii(expected));
        }
        return actual.equals(expected)
                || de.progle.service.UmlautTransliterator.foldToAscii(actual)
                        .equals(de.progle.service.UmlautTransliterator.foldToAscii(expected));
    }

    private static String normalize(String output) {
        if (output == null) {
            return "";
        }
        String[] lines = output.replace("\r\n", "\n").split("\n", -1);
        List<String> kept = new ArrayList<>();
        for (String line : lines) {
            kept.add(line.stripTrailing());
        }
        while (!kept.isEmpty() && kept.get(0).isBlank()) {
            kept.remove(0);
        }
        while (!kept.isEmpty() && kept.get(kept.size() - 1).isBlank()) {
            kept.remove(kept.size() - 1);
        }
        return String.join("\n", kept);
    }

    private void buildQuiz() {
        quizResultBox.getChildren().clear();
        if (lesson.getQuiz().isEmpty()) {
            Label keine = new Label("Kein Quiz in dieser Lektion.");
            keine.getStyleClass().add("muted");
            quizResultBox.getChildren().addAll(new Label("Quiz"), keine);
            return;
        }
        Label heading = new Label("Quiz - teste dich selbst!");
        heading.getStyleClass().add("section-heading");
        quizResultBox.getChildren().add(heading);

        for (int q = 0; q < lesson.getQuiz().size(); q++) {
            quizResultBox.getChildren().add(buildQuizQuestion(q));
        }
    }

    private VBox buildQuizQuestion(int qIndex) {
        de.progle.model.QuizQuestion question = lesson.getQuiz().get(qIndex);
        VBox card = new VBox(8);
        card.getStyleClass().add("quiz-question");
        card.setMinWidth(0);

        Label frage = new Label((qIndex + 1) + ". " + question.getFrage());
        frage.setWrapText(true);
        frage.getStyleClass().add("md-bold");

        ToggleGroup group = new ToggleGroup();
        List<RadioButton> options = new ArrayList<>();
        for (int o = 0; o < question.getOptionen().size(); o++) {
            RadioButton option = new RadioButton(question.getOptionen().get(o));
            option.setWrapText(true);
            option.setToggleGroup(group);
            option.getStyleClass().add("quiz-option");
            options.add(option);
        }

        Label result = new Label();
        result.setWrapText(true);
        result.setVisible(false);
        result.setManaged(false);

        Button checkButton = new Button("Antwort prüfen");
        checkButton.getStyleClass().add("link");
        checkButton.setOnAction(e -> {
            RadioButton selected = (RadioButton) group.getSelectedToggle();
            if (selected == null) {
                result.setText("Bitte erst eine Antwort auswählen.");
                result.getStyleClass().setAll("quiz-wrong");
                result.setVisible(true);
                result.setManaged(true);
                return;
            }
            int chosen = options.indexOf(selected);
            if (chosen == question.getRichtig()) {
                result.setText("Richtig! " + question.getErklärung());
                result.getStyleClass().setAll("quiz-right");
                checkButton.setDisable(true);
                progress.setQuizRichtig(qIndex, true);
                storage.save(data);
                checkCompletion();
            } else {
                result.setText("Leider falsch. Richtig ist: " + question.getOptionen().get(question.getRichtig())
                        + " - " + question.getErklärung());
                result.getStyleClass().setAll("quiz-wrong");
            }
            result.setVisible(true);
            result.setManaged(true);
        });

        card.getChildren().addAll(frage);
        card.getChildren().addAll(options);
        card.getChildren().addAll(checkButton, result);
        return card;
    }

    private void checkCompletion() {
        if (lesson == null || progress.isCompleted()) {
            return;
        }
        boolean allExercises = progress.isAllÜbungenFertig();
        boolean allQuiz = lesson.getQuiz().isEmpty() || progress.isAllQuizRichtig();
        if (allExercises && allQuiz) {
            progress.setCompleted(true);
            storage.save(data);
            onProgressChanged.run();
        }
        updateCompletionChip();
    }

    private void updateCompletionChip() {
        boolean done = progress != null && progress.isCompleted();
        completionChip.setText("Lektion abgeschlossen - weiter geht's!");
        completionChip.setVisible(done);
        completionChip.setManaged(done);
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        submitButton.setDisable(busy);
        submitButton.setText(busy ? "Wird geprüft ..." : "Abgeben und prüfen");
        updateHintControls();
    }

    private String typLabel(String typ) {
        return switch (typ) {
            case "modul-quiz" -> "Modul-Quiz";
            case "projekt" -> "Projekt";
            default -> "Lektion";
        };
    }

    private void startSpeaking(String text, Button sourceButton) {
        if (!tts.isAvailable()) {
            hintInfo.setText("Sprachausgabe nicht installiert (Ordner 'tts' fehlt oder ist leer).");
            return;
        }
        final int request = ++speakRequest;
        activeSpeakerButton = sourceButton;
        updateSpeakButtons(true);
        hintInfo.setText("Liest vor ... (auf den Stopp-Button klicken zum Beenden)");
        tts.speak(text, new TtsService.Listener() {
            @Override
            public void onSpeakingChanged(boolean speakingState) {
                if (request == speakRequest) {
                    Platform.runLater(() -> updateSpeakButtons(speakingState));
                }
            }

            @Override
            public void onError(String message) {
                if (request == speakRequest) {
                    Platform.runLater(() -> hintInfo.setText("Sprachausgabe: " + message));
                }
            }
        });
    }

    private void stopSpeaking() {
        speakRequest++;
        activeSpeakerButton = null;
        tts.stop();
        updateSpeakButtons(false);
    }

    private void updateSpeakButtons(boolean speaking) {
        boolean available = tts.isAvailable();
        Button other = activeSpeakerButton == speakLessonButton ? speakExerciseButton : speakLessonButton;
        if (speaking && activeSpeakerButton != null) {
            activeSpeakerButton.setText("Stopp");
            activeSpeakerButton.setGraphic(Icons.create(Icons.STOP, 13, "icon-warn"));
            other.setDisable(true);
        } else {
            activeSpeakerButton = null;
            speakLessonButton.setDisable(!available);
            speakExerciseButton.setDisable(!available || lesson == null || lesson.getÜbungen().isEmpty());
            speakLessonButton.setText("Vorlesen");
            speakLessonButton.setGraphic(Icons.create(Icons.VOLUME, 15, "icon-accent"));
            speakExerciseButton.setText("Aufgabe vorlesen");
            speakExerciseButton.setGraphic(Icons.create(Icons.VOLUME, 13, "icon-accent"));
        }
    }

    private String lessonSpeech(LessonData lessonData) {
        if (lessonData == null) {
            return "";
        }
        return stripMarkdown(lessonData.getTitel() + ". " + lessonData.getErklärung());
    }

    static String stripMarkdown(String markdown) {
        if (markdown == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        boolean inCode = false;
        for (String line : markdown.replace("\r\n", "\n").split("\n", -1)) {
            String text = line.trim();
            if (text.startsWith("```")) {
                inCode = !inCode;
                continue;
            }
            if (inCode || text.isEmpty()) {
                continue;
            }
            text = text.replaceAll("^#+\\s*", "");
            text = text.replaceAll("^[-*]\\s+", "");
            text = text.replace("**", "");
            text = text.replace("`", "");
            text = text.replace("|", ", ");
            if (text.endsWith(".") || text.endsWith("!") || text.endsWith("?") || text.endsWith(":")) {
                out.append(text).append(" ");
            } else {
                out.append(text).append(". ");
            }
        }
        return out.toString().replaceAll("\\s+", " ").trim();
    }

    public void persist() {
        if (progress != null) {
            progress.setCode(exIndex, codeArea.getText());
        }
    }
}
