package de.progle.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class SpotlightCard extends StackPane {

    private static final double GLOW_SIZE = 340;

    private final Region glow = new Region();

    public SpotlightCard(Node content) {
        getStyleClass().addAll("card", "spotlight-card");
        glow.getStyleClass().add("spotlight-glow");
        glow.setMouseTransparent(true);
        glow.setVisible(false);
        glow.setPrefSize(GLOW_SIZE, GLOW_SIZE);
        StackPane.setAlignment(glow, Pos.TOP_LEFT);
        getChildren().addAll(glow, content);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        clip.setArcWidth(14);
        clip.setArcHeight(14);
        setClip(clip);

        setOnMouseEntered(e -> glow.setVisible(true));
        setOnMouseExited(e -> glow.setVisible(false));
        setOnMouseMoved(e -> {
            glow.setTranslateX(e.getX() - GLOW_SIZE / 2);
            glow.setTranslateY(e.getY() - GLOW_SIZE / 2);
        });
    }
}
