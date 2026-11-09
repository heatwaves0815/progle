package de.progle.ui;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class AuroraBackground extends Pane {

    public AuroraBackground() {
        setMouseTransparent(true);
        addBlob(0.22, 0.28, 360, Color.web("#8b7cff", 0.20), 26, -70);
        addBlob(0.78, 0.24, 420, Color.web("#57a1ff", 0.15), 32, 90);
        addBlob(0.60, 0.82, 400, Color.web("#b06cff", 0.12), 38, -60);
        addBlob(0.10, 0.85, 300, Color.web("#57a1ff", 0.10), 30, 70);
    }

    private void addBlob(double xFactor, double yFactor, double radius, Color color, long seconds, double driftX) {
        Circle blob = new Circle(radius);
        blob.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, color), new Stop(1, Color.TRANSPARENT)));
        blob.setMouseTransparent(true);
        blob.layoutXProperty().bind(widthProperty().multiply(xFactor));
        blob.layoutYProperty().bind(heightProperty().multiply(yFactor));
        getChildren().add(blob);

        TranslateTransition drift = new TranslateTransition(Duration.seconds(seconds), blob);
        drift.setByX(driftX);
        drift.setByY(-driftX * 0.6);
        drift.setCycleCount(Animation.INDEFINITE);
        drift.setAutoReverse(true);
        drift.setInterpolator(Interpolator.EASE_BOTH);
        drift.play();
    }
}
