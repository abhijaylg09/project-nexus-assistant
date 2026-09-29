package com.nexus.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

/**
 * Telemetry Meter card for system status HUD display.
 */
public class TelemetryMeter extends VBox {

    private final Label titleLabel;
    private final Label valueLabel;
    private final ProgressBar progressBar;

    public TelemetryMeter(String title, String initialValue) {
        getStyleClass().add("telemetry-card");
        setSpacing(4);
        setPadding(new Insets(8, 12, 8, 12));

        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("telemetry-label");

        valueLabel = new Label(initialValue);
        valueLabel.getStyleClass().add("telemetry-value");

        progressBar = new ProgressBar(0.5);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #00f2fe;");

        getChildren().addAll(titleLabel, valueLabel, progressBar);
    }

    public void update(String value, double progress) {
        valueLabel.setText(value);
        progressBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
    }

    public void updateTextOnly(String value) {
        valueLabel.setText(value);
    }
}
