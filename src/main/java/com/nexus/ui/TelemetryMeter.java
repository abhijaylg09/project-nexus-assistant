package com.nexus.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Telemetry Meter card for system status HUD display with smooth animated progress bar.
 */
public class TelemetryMeter extends VBox {

    private final Label titleLabel;
    private final Label valueLabel;
    private final ProgressBar progressBar;
    private Timeline transitionTimeline;

    public TelemetryMeter(String title, String initialValue) {
        getStyleClass().add("telemetry-card");
        setSpacing(5);
        setPadding(new Insets(8, 12, 8, 12));

        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("telemetry-label");

        valueLabel = new Label(initialValue);
        valueLabel.getStyleClass().add("telemetry-value");

        progressBar = new ProgressBar(0.5);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(6);

        getChildren().addAll(titleLabel, valueLabel, progressBar);
    }

    public void update(String value, double progress) {
        valueLabel.setText(value);
        double target = Math.max(0.0, Math.min(1.0, progress));
        if (transitionTimeline != null) {
            transitionTimeline.stop();
        }
        transitionTimeline = new Timeline(
                new KeyFrame(Duration.millis(260), new KeyValue(progressBar.progressProperty(), target))
        );
        transitionTimeline.play();
    }

    public void updateTextOnly(String value) {
        valueLabel.setText(value);
    }
}
