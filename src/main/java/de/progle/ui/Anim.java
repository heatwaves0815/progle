package de.progle.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public final class Anim {

    private Anim() {
    }

    public static void fadeSlideIn(Node node) {
        fadeSlideIn(node, 0);
    }

    public static void fadeSlideIn(Node node, double delayMillis) {
        node.setOpacity(0);
        node.setTranslateY(12);
        FadeTransition fade = new FadeTransition(Duration.millis(280), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition slide = new TranslateTransition(Duration.millis(280), node);
        slide.setFromY(12);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition parallel = new ParallelTransition(fade, slide);
        parallel.setDelay(Duration.millis(delayMillis));
        parallel.setOnFinished(e -> {
            node.setOpacity(1);
            node.setTranslateY(0);
        });
        parallel.play();
    }

    public static void hoverLift(Node node) {
        node.setOnMouseEntered(e -> scaleTo(node, 1.03));
        node.setOnMouseExited(e -> scaleTo(node, 1.0));
        node.setOnMousePressed(e -> scaleTo(node, 0.97));
        node.setOnMouseReleased(e -> scaleTo(node, 1.0));
    }

    private static void scaleTo(Node node, double target) {
        KeyFrame frame = new KeyFrame(Duration.millis(160),
                new KeyValue(node.scaleXProperty(), target, Interpolator.EASE_BOTH),
                new KeyValue(node.scaleYProperty(), target, Interpolator.EASE_BOTH));
        new Timeline(frame).play();
    }

    public static void animateProgress(ProgressBar bar, double to) {
        double from = bar.getProgress();
        if (Math.abs(from - to) < 0.0001) {
            bar.setProgress(to);
            return;
        }
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(650),
                new KeyValue(bar.progressProperty(), to, Interpolator.EASE_BOTH)));
        timeline.play();
    }

    public static void pulseGlow(Node node, Color glowColor) {
        DropShadow glow = new DropShadow(10, glowColor);
        node.setEffect(glow);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 8)),
                new KeyFrame(Duration.seconds(1.8),
                        new KeyValue(glow.radiusProperty(), 18, Interpolator.EASE_BOTH)));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.setAutoReverse(true);
        timeline.play();
    }
}
