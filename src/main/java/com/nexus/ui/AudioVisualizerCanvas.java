package com.nexus.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

/**
 * Animated real-time audio visualizer canvas displaying waveform and frequency bars.
 */
public class AudioVisualizerCanvas extends Canvas {

    private double audioLevel = 0.1;
    private double phase = 0.0;
    private boolean active = true;

    public AudioVisualizerCanvas(double width, double height) {
        super(width, height);
        startAnimation();
    }

    public void setAudioLevel(double level) {
        this.audioLevel = Math.max(0.05, Math.min(1.0, level));
    }

    private void startAnimation() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!active) return;
                phase += 0.08;
                render();
            }
        };
        timer.start();
    }

    private void render() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        gc.clearRect(0, 0, w, h);

        // Background subtle grid
        gc.setFill(Color.rgb(8, 14, 28, 0.4));
        gc.fillRoundRect(0, 0, w, h, 8, 8);

        // Draw dynamic frequency bars
        int bars = 36;
        double barWidth = (w - (bars * 3)) / bars;
        LinearGradient barGrad = new LinearGradient(
                0, h, 0, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(0, 242, 254, 0.2)),
                new Stop(0.6, Color.rgb(0, 242, 254, 0.8)),
                new Stop(1.0, Color.rgb(155, 81, 224, 0.9))
        );
        gc.setFill(barGrad);

        for (int i = 0; i < bars; i++) {
            double barPhase = phase + (i * 0.25);
            double barHeight = (Math.sin(barPhase) * 0.5 + 0.5) * (h * 0.75 * audioLevel) + 4;
            double x = i * (barWidth + 3) + 4;
            double y = (h - barHeight) / 2.0;

            gc.fillRoundRect(x, y, barWidth, barHeight, 3, 3);
        }

        // Draw animated center sine waveform
        gc.setStroke(Color.rgb(0, 242, 254, 0.9));
        gc.setLineWidth(1.8);
        gc.beginPath();
        for (double x = 0; x < w; x += 3) {
            double normX = x / w;
            double y = (h / 2.0) + Math.sin(normX * 12.0 + phase * 2.0) * (h * 0.35 * audioLevel);
            if (x == 0) gc.moveTo(x, y);
            else gc.lineTo(x, y);
        }
        gc.stroke();
    }
}
