package de.progle.ui;

import de.progle.model.Curriculum;
import de.progle.model.LessonData;
import de.progle.model.ProgressData;
import de.progle.model.UserProfile;
import de.progle.service.ConfigService;
import de.progle.service.CurriculumService;
import de.progle.service.OpenRouterService;
import de.progle.service.ProgressStorageService;
import de.progle.service.SettingsService;
import de.progle.service.TutorService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;
import java.util.Optional;

public class App extends Application {

    private Stage primaryStage;
    private Stage splashStage;
    private ProgressBar splashBar;
    private Label splashStep;
    private TutorService tutor;
    private ProgressStorageService storage;
    private CurriculumService curriculumService;
    private SettingsService settings;
    private List<CurriculumService.CurriculumInfo> availableCurricula;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        settings = new SettingsService();
        showSplash();
        Thread loader = new Thread(this::initializeAndOpen, "progle-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void showSplash() {
        splashStage = new Stage(StageStyle.UNDECORATED);
        splashStage.setTitle("Progle wird geladen");

        StackPane root = new StackPane();
        root.getStyleClass().add("wizard-root");
        root.getChildren().add(new AuroraBackground());

        VBox card = new VBox(14);
        card.getStyleClass().addAll("card", "wizard-card");
        card.setMaxWidth(460);
        StackPane.setMargin(card, new Insets(36));
        root.getChildren().add(card);

        HBox logoRow = new HBox(12);
        logoRow.setAlignment(Pos.CENTER);
        javafx.scene.Node logoIcon = Icons.create(Icons.SPARKLE, 30);
        Anim.pulseGlow(logoIcon, Color.web("#8b7cff", 0.55));
        Label logo = new Label("Progle");
        logo.getStyleClass().add("wizard-logo");
        logoRow.getChildren().addAll(logoIcon, logo);

        Label headline = new Label("Wird gestartet ...");
        headline.getStyleClass().add("section-heading");
        headline.setAlignment(Pos.CENTER);

        splashBar = new ProgressBar(0.05);
        splashBar.setMaxWidth(Double.MAX_VALUE);

        splashStep = new Label("Initialisiere ...");
        splashStep.getStyleClass().add("muted");

        Label timeHint = new Label("Ungefähre Ladezeit: 3 bis 10 Sekunden");
        timeHint.getStyleClass().add("muted");
        timeHint.setAlignment(Pos.CENTER);

        card.getChildren().addAll(logoRow, headline, splashBar, splashStep, timeHint);
        Anim.fadeSlideIn(card, 60);

        Scene scene = new Scene(root, 520, 340);
        applyStylesheet(scene);
        splashStage.setScene(scene);
        splashStage.centerOnScreen();
        splashStage.show();
    }

    private void step(String text, double progress) {
        Platform.runLater(() -> {
            splashStep.setText(text);
            splashBar.setProgress(progress);
        });
        try {
            Thread.sleep(220);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void initializeAndOpen() {
        step("Lade Konfiguration ...", 0.15);
        ConfigService configService = new ConfigService();
        tutor = new TutorService(new OpenRouterService(configService.load()));
        storage = new ProgressStorageService();
        curriculumService = new CurriculumService();
        settings = new SettingsService();

        step("Lade Lehrplan (Module und Lektionen) ...", 0.45);
        List<CurriculumService.CurriculumInfo> curricula = curriculumService.listAvailable();
        availableCurricula = curricula;
        sleep(250);

        step("Lade deinen Fortschritt ...", 0.7);
        final Optional<ProgressData> savedProgress = storage.load();
        sleep(250);

        step("Bereit!", 1.0);
        sleep(400);

        Platform.runLater(() -> {
            splashStage.close();
            if (curricula.isEmpty()) {
                new Alert(Alert.AlertType.ERROR,
                        "Kein Curriculum gefunden. Lege einen Ordner 'curricula/<sprache>/curriculum.json' an (siehe README.md).",
                        ButtonType.OK).showAndWait();
                Platform.exit();
                return;
            }
            primaryStage.show();
            if (savedProgress.isPresent() && curriculumIdExists(curricula, savedProgress.get().getCurriculumId())) {
                Curriculum curriculum = loadCurriculum(savedProgress.get().getCurriculumId());
                if (curriculum != null) {
                    openMain(savedProgress.get(), curriculum, null);
                    return;
                }
            }
            showPicker();
        });
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean curriculumIdExists(List<CurriculumService.CurriculumInfo> curricula, String id) {
        return curricula.stream().anyMatch(info -> info.id().equals(id));
    }

    private Curriculum loadCurriculum(String id) {
        try {
            return curriculumService.load(id);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Curriculum konnte nicht geladen werden: " + e.getMessage(),
                    ButtonType.OK).showAndWait();
            return null;
        }
    }

    private void showPicker() {
        Scene scene = LanguagePicker.createScene(availableCurricula, info -> {
            Curriculum curriculum = loadCurriculum(info.id());
            if (curriculum == null) {
                return;
            }
            Optional<ProgressData> saved = storage.load();
            if (saved.isPresent() && info.id().equals(saved.get().getCurriculumId())) {
                openMain(saved.get(), curriculum, null);
            } else {
                openWelcome(curriculum);
            }
        });
        applyStylesheet(scene);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Progle - Sprachauswahl");
        primaryStage.sizeToScene();
        primaryStage.centerOnScreen();
        primaryStage.setOnCloseRequest(null);
    }

    private void openWelcome(Curriculum curriculum) {
        Scene scene = WelcomeWizard.createScene(curriculum, profile -> {
            List<LessonData> ordered = curriculum.getModule().stream()
                    .flatMap(module -> module.getLektionen().stream())
                    .map(curriculum::lessonById)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            ProgressData data = new ProgressData(curriculum.getId(), profile, ordered);
            storage.save(data);
            openMain(data, curriculum, null);
        });
        applyStylesheet(scene);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Progle - Willkommen");
        primaryStage.sizeToScene();
        primaryStage.centerOnScreen();
    }

    private void openMain(ProgressData data, Curriculum curriculum, String warning) {
        MainView mainView = new MainView(data, curriculum, tutor, storage, settings, warning, this::showPicker);
        Scene scene = new Scene(mainView, 1250, 840);
        applyStylesheet(scene);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Progle v2 - " + curriculum.getTitel());
        primaryStage.centerOnScreen();
        primaryStage.setOnCloseRequest(e -> mainView.persist());
    }

    private void applyStylesheet(Scene scene) {
        var base = App.class.getResource("/styles.css");
        if (base != null) {
            scene.getStylesheets().add(base.toExternalForm());
        }
        var theme = App.class.getResource("/themes/theme-" + settings.getTheme() + ".css");
        if (theme != null) {
            scene.getStylesheets().add(theme.toExternalForm());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
