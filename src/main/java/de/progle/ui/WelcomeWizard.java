package de.progle.ui;

import de.progle.model.Curriculum;
import de.progle.model.UserProfile;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.function.Consumer;

public final class WelcomeWizard {

    public interface PlanReady {
        void ready(UserProfile profile);
    }

    private WelcomeWizard() {
    }

    public static Scene createScene(Curriculum curriculum, PlanReady onReady) {
        StackPane root = new StackPane();
        root.getStyleClass().add("wizard-root");
        root.getChildren().add(new AuroraBackground());

        VBox card = new VBox(16);
        card.getStyleClass().addAll("card", "wizard-card");
        card.setMaxWidth(480);
        StackPane.setMargin(card, new Insets(36));
        root.getChildren().add(card);

        HBox logoRow = new HBox(12);
        logoRow.setAlignment(Pos.CENTER);
        Node logoIcon = Icons.create(Icons.SPARKLE, 30);
        Anim.pulseGlow(logoIcon, Color.web("#8b7cff", 0.55));
        Label logo = new Label("Progle");
        logo.getStyleClass().add("wizard-logo");
        logoRow.getChildren().addAll(logoIcon, logo);

        Label tagline = new Label(curriculum.getTitel());
        tagline.getStyleClass().add("subtitle");
        tagline.setWrapText(true);

        Label intro = new Label("In " + curriculum.getModule().size() + " Modulen bis zum Abschlussprojekt.");
        intro.getStyleClass().add("muted");

        Label nameTitle = new Label("Wie heißt du?");
        nameTitle.getStyleClass().add("section-heading");
        TextField nameField = new TextField();
        nameField.setPromptText("Vorname oder Spitzname");

        Label levelTitle = new Label("Wie viel Erfahrung hast du mit Programmieren?");
        levelTitle.getStyleClass().add("section-heading");
        ToggleGroup levelGroup = new ToggleGroup();
        RadioButton keine = new RadioButton("Keine - ich fange komplett bei null an");
        RadioButton etwas = new RadioButton("Etwas - ich kenne schon Grundbegriffe");
        RadioButton erfahren = new RadioButton("Erfahren - ich programmiere schon in anderen Sprachen");
        keine.setToggleGroup(levelGroup);
        etwas.setToggleGroup(levelGroup);
        erfahren.setToggleGroup(levelGroup);
        keine.setUserData(de.progle.model.KnowledgeLevel.KEINE);
        etwas.setUserData(de.progle.model.KnowledgeLevel.ETWAS);
        erfahren.setUserData(de.progle.model.KnowledgeLevel.ERFAHREN);
        keine.setSelected(true);

        Button startButton = new Button("Lernen starten");
        startButton.getStyleClass().add("primary");
        startButton.setGraphic(Icons.create(Icons.BOLT, 16));
        startButton.setMaxWidth(Double.MAX_VALUE);
        Anim.hoverLift(startButton);
        startButton.setOnAction(event -> {
            String name = de.progle.service.UmlautTransliterator.toUmlauts(nameField.getText().trim());
            if (name.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Bitte gib zuerst deinen Namen ein.", ButtonType.OK).showAndWait();
                return;
            }
            de.progle.model.KnowledgeLevel level =
                    (de.progle.model.KnowledgeLevel) levelGroup.getSelectedToggle().getUserData();
            onReady.ready(new UserProfile(name, level));
        });

        Region spacer1 = new Region();
        Region spacer2 = new Region();
        VBox.setVgrow(spacer1, Priority.ALWAYS);
        VBox.setVgrow(spacer2, Priority.ALWAYS);

        VBox levelBox = new VBox(10, keine, etwas, erfahren);
        card.getChildren().addAll(logoRow, tagline, intro, spacer1, nameTitle, nameField,
                levelTitle, levelBox, spacer2, startButton);
        Anim.fadeSlideIn(card, 80);

        return new Scene(root, 540, 660);
    }
}
