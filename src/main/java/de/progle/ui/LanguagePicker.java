package de.progle.ui;

import de.progle.service.CurriculumService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public final class LanguagePicker {

    public interface OnPick {
        void pick(CurriculumService.CurriculumInfo info);
    }

    private LanguagePicker() {
    }

    public static Scene createScene(List<CurriculumService.CurriculumInfo> curricula, OnPick onPick) {
        StackPane root = new StackPane();
        root.getStyleClass().add("wizard-root");
        root.getChildren().add(new AuroraBackground());

        VBox card = new VBox(18);
        card.getStyleClass().addAll("card", "wizard-card");
        card.setMaxWidth(520);
        StackPane.setMargin(card, new Insets(36));
        root.getChildren().add(card);

        HBox logoRow = new HBox(12);
        logoRow.setAlignment(Pos.CENTER);
        Node logoIcon = Icons.create(Icons.SPARKLE, 28);
        Anim.pulseGlow(logoIcon, javafx.scene.paint.Color.web("#8b7cff", 0.55));
        Label logo = new Label("Progle");
        logo.getStyleClass().add("wizard-logo");
        logoRow.getChildren().addAll(logoIcon, logo);

        Label title = new Label("Wähle deine Programmiersprache");
        title.getStyleClass().add("section-heading");
        title.setAlignment(Pos.CENTER);

        Label subtitle = new Label("Weitere Sprachen folgen – der Unterricht passt sich automatisch an.");
        subtitle.getStyleClass().add("subtitle");
        subtitle.setWrapText(true);
        subtitle.setAlignment(Pos.CENTER);

        card.getChildren().addAll(logoRow, title, subtitle);

        for (CurriculumService.CurriculumInfo info : curricula) {
            VBox choiceContent = new VBox(6);
            Label titel = new Label(info.titel());
            titel.getStyleClass().add("choice-titel");
            Label beschreibung = new Label(info.beschreibung());
            beschreibung.getStyleClass().add("choice-beschreibung");
            beschreibung.setWrapText(true);
            Label chip = new Label("verfügbar");
            chip.getStyleClass().add("chip");
            choiceContent.getChildren().addAll(titel, beschreibung, chip);

            StackPane choiceCard = new StackPane(choiceContent);
            choiceCard.getStyleClass().addAll("card", "choice-card");
            Anim.hoverLift(choiceCard);
            choiceCard.setOnMouseClicked(e -> onPick.pick(info));
            choiceCard.setOnMouseEntered(e -> choiceCard.getStyleClass().add("choice-hover"));
            choiceCard.setOnMouseExited(e -> choiceCard.getStyleClass().remove("choice-hover"));

            card.getChildren().add(choiceCard);
        }

        Anim.fadeSlideIn(card, 80);
        return new Scene(root, 560, 680);
    }
}
