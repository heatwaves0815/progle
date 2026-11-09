package de.progle.ui;

import de.progle.model.ChatMessage;
import de.progle.model.LessonData;
import de.progle.model.ProgressData;
import de.progle.service.AiException;
import de.progle.service.TutorService;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatView extends VBox {

    private static final Pattern CODE_FENCE = Pattern.compile("```(?:java\\r?\\n)?([\\s\\S]*?)```");

    private final ProgressData data;
    private final TutorService tutor;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "progle-chat");
        thread.setDaemon(true);
        return thread;
    });

    private final VBox bubbleBox = new VBox(12);
    private final ScrollPane scroll = new ScrollPane(bubbleBox);
    private final TextField input = new TextField();
    private final Button sendButton = new Button("Senden");
    private final HBox inputBar = new HBox(10);
    private final HBox thinkingBox = new HBox(5);
    private final List<Circle> dots = new ArrayList<>();
    private final List<FadeTransition> dotAnimations = new ArrayList<>();
    private final Node userAvatar;

    private final List<ChatMessage> history = new ArrayList<>();
    private LessonData lesson;
    private boolean busy;

    public ChatView(ProgressData data, TutorService tutor) {
        this.data = data;
        this.tutor = tutor;
        this.userAvatar = buildUserAvatar();
        buildUi();
    }

    private void buildUi() {
        getStyleClass().add("pane-transparent");
        setSpacing(10);
        setPadding(new Insets(16));

        bubbleBox.setPadding(new Insets(6));
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("chat-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        bubbleBox.heightProperty().addListener((obs, oldHeight, newHeight) -> scroll.setVvalue(1.0));

        input.getStyleClass().add("chat-input");
        input.setPromptText("Frag den Tutor etwas - z. B. \"Wie funktioniert eine for-Schleife?\"");
        HBox.setHgrow(input, Priority.ALWAYS);
        input.focusedProperty().addListener((obs, oldFocus, newFocus) -> {
            if (newFocus) {
                inputBar.getStyleClass().add("focused");
            } else {
                inputBar.getStyleClass().remove("focused");
            }
        });

        sendButton.getStyleClass().add("primary");
        sendButton.setGraphic(Icons.create(Icons.SEND, 15));
        sendButton.setOnAction(event -> send());
        input.setOnAction(event -> send());

        inputBar.getStyleClass().add("chat-input-bar");
        inputBar.setAlignment(Pos.CENTER_LEFT);
        inputBar.getChildren().addAll(input, sendButton);

        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(3.5);
            dot.getStyleClass().add("dot");
            dots.add(dot);
            FadeTransition pulse = new FadeTransition(Duration.millis(520), dot);
            pulse.setFromValue(1.0);
            pulse.setToValue(0.25);
            pulse.setCycleCount(FadeTransition.INDEFINITE);
            pulse.setAutoReverse(true);
            pulse.setInterpolator(Interpolator.EASE_BOTH);
            pulse.setDelay(Duration.millis(i * 170));
            dotAnimations.add(pulse);
        }
        thinkingBox.getStyleClass().add("thinking-box");
        thinkingBox.setAlignment(Pos.CENTER_LEFT);
        thinkingBox.getChildren().addAll(dots);
        thinkingBox.setVisible(false);
        thinkingBox.setManaged(false);

        getChildren().addAll(scroll, thinkingBox, inputBar);
    }

    public void setLesson(LessonData lesson) {
        this.lesson = lesson;
    }

    public void greet(String name) {
        history.add(ChatMessage.assistant("""
                Hallo %s! Ich bin Progle, dein Java-Tutor.
                Du kannst mich jederzeit fragen - ich erkläre dir alles in einfacher Sprache.
                Tipp: Keine Umlaut-Tastatur? Schreibe einfach ae/oe/ue/ss - ich mache automatisch ä/ö/ü/ß daraus.
                Wähle links eine Lektion und leg los. Wenn du nicht weiterweißt, frag mich einfach hier im Chat.""".formatted(name)));
        bubbleBox.getChildren().add(bubble(history.get(0).content(), false));
    }

    public void notice(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("notice-text");
        VBox box = new VBox(label);
        box.getStyleClass().add("bubble-notice");
        box.setMaxWidth(520);
        bubbleBox.getChildren().add(new HBox(box));
    }

    private Node buildAiAvatar() {
        StackPane avatar = new StackPane(Icons.create(Icons.SPARKLE, 14));
        avatar.getStyleClass().add("avatar");
        return avatar;
    }

    private Node buildUserAvatar() {
        String name = data.getProfile().getName();
        String initial = name == null || name.isBlank() ? "Du" : name.substring(0, 1).toUpperCase();
        Label initialLabel = new Label(initial);
        initialLabel.getStyleClass().add("avatar-text");
        StackPane avatar = new StackPane(initialLabel);
        avatar.getStyleClass().addAll("avatar", "avatar-user");
        return avatar;
    }

    private void send() {
        if (busy) {
            return;
        }
        String text = input.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        if (lesson == null) {
            notice("Wähle zuerst eine Lektion aus, dann beantworte ich deine Frage.");
            return;
        }
        input.clear();
        String converted = de.progle.service.UmlautTransliterator.toUmlauts(text);
        history.add(ChatMessage.user(converted));
        bubbleBox.getChildren().add(bubble(converted, true));
        setBusy(true);
        LessonData currentLesson = lesson;
        VBox liveBubble = new VBox();
        TextFlow liveFlow = new TextFlow();
        liveFlow.getStyleClass().add("bubble-tutor");
        liveFlow.setMaxWidth(520);
        liveBubble.getChildren().add(liveFlow);
        bubbleBox.getChildren().add(liveBubble);
        executor.submit(() -> {
            StringBuilder collected = new StringBuilder();
            try {
                tutor.answerStreaming(data.getProfile(), currentLesson, history,
                        new de.progle.service.TutorService.StreamCallback() {
                            @Override
                            public void onChunk(String chunk) {
                                collected.append(chunk);
                                Platform.runLater(() -> {
                                    renderMessage(liveFlow, collected.toString());
                                });
                            }

                            @Override
                            public void onError(String message) {
                                Platform.runLater(() -> {
                                    bubbleBox.getChildren().add(bubble("Fehler: " + message, false));
                                });
                            }
                        });
            } catch (AiException e) {
                collected.append("Entschuldigung, ich kann gerade nicht antworten: ").append(e.getMessage());
            }
            history.add(ChatMessage.assistant(collected.toString()));
            Platform.runLater(() -> {
                bubbleBox.getChildren().remove(liveBubble);
                bubbleBox.getChildren().add(bubble(collected.toString(), false));
                setBusy(false);
            });
        });
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        input.setDisable(busy);
        sendButton.setDisable(busy);
        thinkingBox.setVisible(busy);
        thinkingBox.setManaged(busy);
        if (busy) {
            dotAnimations.forEach(FadeTransition::playFromStart);
        } else {
            dotAnimations.forEach(FadeTransition::stop);
            dots.forEach(dot -> dot.setOpacity(1.0));
        }
    }

    private Node bubble(String text, boolean user) {
        TextFlow flow = new TextFlow();
        renderMessage(flow, text);
        flow.setMinWidth(0);
        VBox box = new VBox(flow);
        box.getStyleClass().add(user ? "bubble-user" : "bubble-tutor");
        box.setMaxWidth(520);
        HBox row = new HBox(10);
        row.setAlignment(Pos.BOTTOM_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        if (user) {
            row.getChildren().addAll(spacer, box, userAvatar);
        } else {
            row.getChildren().addAll(buildAiAvatar(), box, spacer);
        }
        return row;
    }

    private void renderMessage(TextFlow flow, String text) {
        Matcher matcher = CODE_FENCE.matcher(text);
        int last = 0;
        while (matcher.find()) {
            addPlainText(flow, text.substring(last, matcher.start()));
            for (JavaSyntaxHighlighter.Token token : JavaSyntaxHighlighter.tokens(matcher.group(1))) {
                Text code = new Text(token.text());
                code.getStyleClass().add("msg-code");
                if (token.style() != null) {
                    code.getStyleClass().add(token.style());
                }
                code.wrappingWidthProperty().bind(flow.widthProperty().subtract(24));
                flow.getChildren().add(code);
            }
            last = matcher.end();
        }
        addPlainText(flow, text.substring(last));
    }

    private void addPlainText(TextFlow flow, String text) {
        if (text.isBlank()) {
            return;
        }
        Text textNode = new Text(text);
        textNode.getStyleClass().add("msg-text");
        textNode.wrappingWidthProperty().bind(flow.widthProperty().subtract(24));
        flow.getChildren().add(textNode);
    }
}
