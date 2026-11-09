package de.progle.ui;

import de.progle.model.Curriculum;
import de.progle.model.CurriculumModule;
import de.progle.model.LessonData;
import de.progle.model.LessonProgress;
import de.progle.model.ProgressData;
import de.progle.service.CodeRunnerService;
import de.progle.service.ProgressStorageService;
import de.progle.service.SettingsService;
import de.progle.service.TutorService;
import de.progle.service.TtsService;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainView extends BorderPane {

    private final ProgressData data;
    private final Curriculum curriculum;
    private final ProgressStorageService storage;

    private final ProgressBar progressBar = new ProgressBar();
    private final Label progressLabel = new Label();
    private final Tab lessonTab = new Tab("Lektion");
    private final Tab chatTab = new Tab("Chat mit dem Tutor");
    private final TabPane tabs = new TabPane();
    private final LessonView lessonView;
    private final ChatView chatView;
    private final VBox rowsBox = new VBox(2);
    private final Map<String, HBox> lessonRows = new LinkedHashMap<>();
    private final List<LessonData> orderedLessons = new ArrayList<>();
    private String currentLessonId;
    private javafx.scene.control.ScrollPane sidebarScroll;
    private int shownCompletedCount = -1;

    public MainView(ProgressData data, Curriculum curriculum, TutorService tutor, ProgressStorageService storage,
                    SettingsService settings, String warning, Runnable onReset) {
        this.data = data;
        this.curriculum = curriculum;
        this.storage = storage;

        setPadding(new Insets(14));

        CodeRunnerService runner = new CodeRunnerService();
        TtsService tts = new TtsService();
        lessonView = new LessonView(data, curriculum, tutor, storage, runner, tts, this::refresh);
        chatView = new ChatView(data, tutor);

        lessonTab.setContent(lessonView);
        lessonTab.setClosable(false);
        lessonTab.setGraphic(Icons.create(Icons.BOOK, 15));
        chatTab.setContent(chatView);
        chatTab.setClosable(false);
        chatTab.setGraphic(Icons.create(Icons.CHAT, 15));
        tabs.getTabs().addAll(lessonTab, chatTab);
        tabs.getStyleClass().add("main-tabs");

        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && newTab.getContent() != null) {
                Anim.fadeSlideIn(newTab.getContent());
            }
        });

        StackPane center = new StackPane(new AuroraBackground(), tabs);
        buildSidebar(settings, onReset);
        setLeft(sidebarScroll);
        setCenter(center);

        buildRows();

        int index = Math.min(Math.max(data.getCurrentLessonIndex(), 0), orderedLessons.size() - 1);
        if (index >= 0 && !orderedLessons.isEmpty()) {
            selectLesson(orderedLessons.get(index));
        }
        refresh();

        chatView.greet(data.getProfile().getName());
        if (warning != null && !warning.isBlank()) {
            chatView.notice("Hinweis: " + warning);
        }
        if (!tutor.hasApiKey()) {
            chatView.notice("Noch ist kein OpenRouter-API-Key hinterlegt. Trage ihn in config.json ein "
                    + "(siehe README.md), dann kann ich dir wirklich antworten.");
        }
    }

    private void buildSidebar(SettingsService settings, Runnable onReset) {
        VBox sidebar = new VBox(16);
        sidebar.setPadding(new Insets(22));
        sidebar.setPrefWidth(340);
        sidebar.getStyleClass().add("sidebar-card");

        sidebarScroll = new ScrollPane(sidebar);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebarScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sidebarScroll.setVvalue(0);
        sidebarScroll.getStyleClass().addAll("pane-transparent", "sidebar-scroll");
        sidebarScroll.setPrefWidth(356);

        HBox logoRow = new HBox(10);
        logoRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label logoLabel = new Label("Progle");
        logoLabel.getStyleClass().add("logo-gradient");
        logoRow.getChildren().addAll(Icons.create(Icons.SPARKLE, 26), logoLabel);
        Label hello = new Label("Hallo, " + data.getProfile().getName() + "!");
        hello.getStyleClass().add("greeting");
        Label level = new Label(data.getProfile().getLevel().getDisplayName());
        level.getStyleClass().add("chip");

        Label curriculumTitel = new Label(curriculum.getTitel());
        curriculumTitel.getStyleClass().add("muted");
        curriculumTitel.setWrapText(true);

        VBox progressCard = new VBox(8);
        progressCard.getStyleClass().addAll("card", "progress-card");
        HBox progressHeader = new HBox(8);
        progressHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label progressHeading = new Label("Fortschritt");
        progressHeading.getStyleClass().add("section-heading");
        progressHeader.getChildren().addAll(Icons.create(Icons.TARGET, 15), progressHeading);
        progressLabel.getStyleClass().add("muted");
        progressCard.getChildren().addAll(progressHeader, progressBar, progressLabel);

        Label planTitle = new Label("Dein Lernplan");
        planTitle.getStyleClass().add("section-heading");

        rowsBox.getStyleClass().add("lesson-rows");

        Button resetButton = new Button("Zur Sprachauswahl");
        resetButton.getStyleClass().add("link");
        resetButton.setGraphic(Icons.create(Icons.REFRESH, 14, "icon-dim"));
        resetButton.setOnAction(event -> {
            persist();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Möchtest du zur Sprachauswahl zurückkehren? Dein Fortschritt bleibt erhalten.",
                    ButtonType.OK, ButtonType.CANCEL);
            confirm.setHeaderText("Sprachauswahl öffnen");
            confirm.showAndWait().ifPresent(type -> {
                if (type == ButtonType.OK) {
                    onReset.run();
                }
            });
        });

        String[][] themes = {
                {"dark", "Dunkel Violett"},
                {"midnight", "Mitternacht"},
                {"nord", "Nordisch"},
                {"light", "Hell"}
        };
        int currentIndex = 0;
        for (int i = 0; i < themes.length; i++) {
            if (themes[i][0].equals(settings.getTheme())) {
                currentIndex = i;
                break;
            }
        }
        Label designTitle = new Label("Design");
        designTitle.getStyleClass().add("section-heading");
        ComboBox<String> themeBox = new ComboBox<>();
        for (String[] theme : themes) {
            themeBox.getItems().add(theme[1]);
        }
        themeBox.getSelectionModel().select(currentIndex);
        themeBox.setMaxWidth(Double.MAX_VALUE);
        themeBox.setOnAction(event -> {
            int index = themeBox.getSelectionModel().getSelectedIndex();
            if (index < 0) {
                return;
            }
            String themeId = themes[index][0];
            settings.setTheme(themeId);
            Scene scene = getScene();
            if (scene != null) {
                scene.getStylesheets().removeIf(url -> url.contains("/themes/"));
                var themeUrl = getClass().getResource("/themes/theme-" + themeId + ".css");
                if (themeUrl != null) {
                    scene.getStylesheets().add(themeUrl.toExternalForm());
                }
            }
        });

        sidebar.getChildren().addAll(logoRow, hello, level, curriculumTitel, progressCard,
                planTitle, rowsBox, designTitle, themeBox, resetButton);
    }

    private void buildRows() {
        rowsBox.getChildren().clear();
        lessonRows.clear();
        orderedLessons.clear();

        for (int m = 0; m < curriculum.getModule().size(); m++) {
            CurriculumModule module = curriculum.getModule().get(m);

            HBox header = new HBox(8);
            header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            header.getStyleClass().add("module-header");
            header.setPadding(new Insets(14, 8, 6, 4));
            Label headerLabel = new Label(module.getTitel());
            header.getChildren().addAll(Icons.create(Icons.CHEVRON_RIGHT, 13, "icon-dim"), headerLabel);
            rowsBox.getChildren().add(header);

            boolean isProjectModule = "projekt".equalsIgnoreCase(module.getId())
                    || module.getTitel().toLowerCase().contains("projekt");
            for (int l = 0; l < module.getLektionen().size(); l++) {
                String lessonId = module.getLektionen().get(l);
                LessonData lessonData = curriculum.lessonById(lessonId);
                if (lessonData == null) {
                    continue;
                }
                String label;
                if (isProjectModule || "projekt".equals(lessonData.getTyp())) {
                    label = "Projekt " + (l + 1) + " - " + lessonData.getTitel();
                } else {
                    label = (m + 1) + "." + (l + 1) + " " + lessonData.getTitel();
                }

                HBox row = new HBox(10);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.getStyleClass().add("lesson-row");
                StackPane iconPane = new StackPane();
                iconPane.getStyleClass().add("row-icon");
                Label textLabel = new Label(label);
                textLabel.setWrapText(true);
                textLabel.getStyleClass().add("lesson-row-text");
                row.getChildren().addAll(iconPane, textLabel);
                HBox.setHgrow(textLabel, Priority.ALWAYS);
                row.setOnMouseClicked(event -> selectLesson(lessonData));
                rowsBox.getChildren().add(row);

                lessonRows.put(lessonId, row);
                orderedLessons.add(lessonData);
            }
        }
    }

    private void selectLesson(LessonData lessonData) {
        currentLessonId = lessonData.getId();
        int index = orderedLessons.indexOf(lessonData);
        if (index >= 0) {
            data.setCurrentLessonIndex(index);
        }
        lessonView.loadLesson(lessonData, data.progressFor(lessonData.getId()));
        chatView.setLesson(lessonData);
        tabs.getSelectionModel().select(lessonTab);
        updateRowStyles();
    }

    private void updateRowStyles() {
        for (Map.Entry<String, HBox> entry : lessonRows.entrySet()) {
            HBox row = entry.getValue();
            LessonProgress progress = data.progressFor(entry.getKey());
            boolean done = progress.isCompleted();
            boolean selected = entry.getKey().equals(currentLessonId);

            row.getStyleClass().remove("done");
            row.getStyleClass().remove("selected");
            if (done) {
                row.getStyleClass().add("done");
            }
            if (selected) {
                row.getStyleClass().add("selected");
            }

            StackPane iconPane = (StackPane) row.getChildren().get(0);
            String iconClass = done ? "icon-success" : "icon-dim";
            iconPane.getChildren().setAll(Icons.create(done ? Icons.CHECK : Icons.BOOK, 14, iconClass));
        }
    }

    private void refresh() {
        int done = (int) data.completedCount();
        int total = data.getLessons().size();
        Anim.animateProgress(progressBar, total == 0 ? 0 : (double) done / total);
        updateCountLabel(done, total);
        updateRowStyles();
    }

    private void updateCountLabel(int done, int total) {
        if (shownCompletedCount < 0 || shownCompletedCount == done) {
            shownCompletedCount = done;
            progressLabel.setText(done + " von " + total + " Lektionen abgeschlossen");
            return;
        }
        SimpleIntegerProperty counter = new SimpleIntegerProperty(shownCompletedCount);
        counter.addListener((obs, oldV, newV) ->
                progressLabel.setText(newV.intValue() + " von " + total + " Lektionen abgeschlossen"));
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(650), new KeyValue(counter, done)));
        timeline.play();
        shownCompletedCount = done;
    }

    public void persist() {
        lessonView.persist();
        storage.save(data);
    }
}
