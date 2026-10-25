package com.nexus.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * ChatGPT / Jarvis-Grade Cyberpunk Live Audio Visualizer Canvas.
 * 
 * Features:
 *  - 60 FPS ultra-smooth hardware-accelerated AnimationTimer
 *  - Holographic pulsating AI Voice Orb with dynamic radiating sound rings
 *  - 44-band neon gradient equalizer with peak drop-down dots
 *  - Dual flowing harmonic sine acoustic waves
 *  - Real-time reactivity to both live microphone speech and AI TalkBack voice output
 */
public class AudioVisualizerCanvas extends Canvas {

    private double audioLevel = 0.08;
    private double smoothedLevel = 0.08;
    private double phase = 0.0;
    private boolean active = true;

    private DoubleSupplier audioLevelSupplier = null;
    private BooleanSupplier isSpeakingSupplier = null;
    private BooleanSupplier isListeningSupplier = null;

    // Peak levels for equalizer bar gravity drops
    private final double[] peakHeights = new double[44];
    private final double[] peakVelocities = new double[44];

    public AudioVisualizerCanvas(double width, double height) {
        super(width, height);
        startAnimation();
    }

    public void setAudioLevelSupplier(DoubleSupplier supplier) {
        this.audioLevelSupplier = supplier;
    }

    public void setVoiceStateSuppliers(BooleanSupplier isSpeaking, BooleanSupplier isListening) {
        this.isSpeakingSupplier = isSpeaking;
        this.isListeningSupplier = isListening;
    }

    public void setAudioLevel(double level) {
        this.audioLevel = Math.max(0.04, Math.min(1.0, level));
    }

    private void startAnimation() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!active) return;

                // Query live level
                if (audioLevelSupplier != null) {
                    try {
                        double target = audioLevelSupplier.getAsDouble();
                        audioLevel = Math.max(0.04, Math.min(1.0, target));
                    } catch (Exception ignored) {}
                }

                // Smooth exponential lerp (60fps fluid decay)
                smoothedLevel = smoothedLevel * 0.82 + audioLevel * 0.18;
                phase += 0.06 + (smoothedLevel * 0.08);

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

        boolean speaking = isSpeakingSupplier != null && isSpeakingSupplier.getAsBoolean();
        boolean listening = isListeningSupplier != null && isListeningSupplier.getAsBoolean();

        // 1. Dark Glassmorphic Cyber Backdrop
        gc.setFill(Color.rgb(8, 16, 34, 0.72));
        gc.fillRoundRect(0, 0, w, h, 8, 8);

        // Neon border accent
        Color borderColor = speaking ? Color.rgb(244, 63, 94, 0.65)
                : (listening ? Color.rgb(0, 242, 254, 0.75) : Color.rgb(0, 242, 254, 0.28));
        gc.setStroke(borderColor);
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(0.5, 0.5, w - 1.0, h - 1.0, 8, 8);

        // 2. Holographic Pulsating Voice Orb (Left Section: x = 24, y = h/2)
        double orbX = 26.0;
        double orbY = h / 2.0;
        double baseRadius = 10.0 + (smoothedLevel * 10.0);

        // Radiating pulse wave rings
        for (int r = 1; r <= 3; r++) {
            double ringRadius = baseRadius + ((phase * 16.0 + r * 10.0) % 24.0);
            double ringAlpha = Math.max(0.0, 1.0 - (ringRadius / 36.0)) * 0.45;
            Color ringColor = speaking
                    ? Color.rgb(244, 63, 94, ringAlpha)
                    : (listening ? Color.rgb(0, 242, 254, ringAlpha) : Color.rgb(56, 189, 248, ringAlpha * 0.6));
            gc.setStroke(ringColor);
            gc.setLineWidth(1.2);
            gc.strokeOval(orbX - ringRadius, orbY - ringRadius, ringRadius * 2.0, ringRadius * 2.0);
        }

        // Center glowing orb core
        Color coreColor = speaking ? Color.rgb(244, 114, 182) : (listening ? Color.rgb(0, 242, 254) : Color.rgb(56, 189, 248));
        gc.setFill(coreColor);
        gc.fillOval(orbX - baseRadius * 0.6, orbY - baseRadius * 0.6, baseRadius * 1.2, baseRadius * 1.2);

        // 3. Status Tag Text Overlay
        String statusText;
        Color tagColor;
        if (speaking) {
            statusText = "🔊 TALKBACK: READING ALOUD";
            tagColor = Color.rgb(244, 114, 182);
        } else if (listening) {
            statusText = "🎙️ LIVE VOICE: LISTENING (SPEAK NOW)";
            tagColor = Color.rgb(0, 242, 254);
        } else {
            statusText = "⚡ VOICE PERCEPTION STREAM • STANDBY";
            tagColor = Color.rgb(148, 163, 184);
        }

        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        gc.setFill(tagColor);
        gc.fillText(statusText, 52, 13);

        // 4. Multi-Band Equalizer (x = 52 to w - 10)
        double eqStartX = 52.0;
        double eqWidth = w - eqStartX - 10.0;
        int numBars = 36;
        double barWidth = (eqWidth - (numBars * 2.5)) / numBars;

        LinearGradient barGrad = new LinearGradient(
                0, h, 0, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, speaking ? Color.rgb(244, 63, 94, 0.25) : Color.rgb(0, 242, 254, 0.25)),
                new Stop(0.5, speaking ? Color.rgb(244, 114, 182, 0.85) : Color.rgb(0, 242, 254, 0.85)),
                new Stop(1.0, speaking ? Color.rgb(251, 191, 36, 0.95) : Color.rgb(168, 85, 247, 0.95))
        );
        gc.setFill(barGrad);

        for (int i = 0; i < numBars; i++) {
            // Harmonic wave formula across frequencies
            double harmonic = Math.sin(phase * 1.6 + i * 0.22) * 0.45
                    + Math.cos(phase * 2.4 - i * 0.15) * 0.35
                    + 0.5;

            double targetHeight = Math.max(3.0, harmonic * (h * 0.68) * (smoothedLevel * 1.35));
            double bx = eqStartX + i * (barWidth + 2.5);
            double by = h - targetHeight - 4.0;

            gc.fillRoundRect(bx, by, barWidth, targetHeight, 2, 2);

            // Gravity peak drop dot
            if (targetHeight > peakHeights[i]) {
                peakHeights[i] = targetHeight;
                peakVelocities[i] = 0.0;
            } else {
                peakVelocities[i] += 0.22;
                peakHeights[i] = Math.max(3.0, peakHeights[i] - peakVelocities[i]);
            }

            double peakY = h - peakHeights[i] - 6.0;
            gc.setFill(speaking ? Color.rgb(254, 240, 138, 0.9) : Color.rgb(0, 242, 254, 0.9));
            gc.fillRect(bx, peakY, barWidth, 1.5);
            gc.setFill(barGrad); // Reset fill
        }

        // 5. Dual Harmonic Continuous Acoustic Sine Lines
        gc.setLineWidth(1.6);
        gc.setStroke(speaking ? Color.rgb(244, 63, 94, 0.85) : Color.rgb(0, 242, 254, 0.85));
        gc.beginPath();
        for (double x = eqStartX; x < w - 10.0; x += 3.0) {
            double normX = (x - eqStartX) / eqWidth;
            double waveY = (h / 2.0 + 5.0) + Math.sin(normX * 14.0 + phase * 2.8) * (h * 0.28 * smoothedLevel);
            if (x == eqStartX) gc.moveTo(x, waveY);
            else gc.lineTo(x, waveY);
        }
        gc.stroke();

        // Secondary harmonic counter-wave
        gc.setLineWidth(1.0);
        gc.setStroke(speaking ? Color.rgb(251, 146, 60, 0.6) : Color.rgb(168, 85, 247, 0.65));
        gc.beginPath();
        for (double x = eqStartX; x < w - 10.0; x += 3.0) {
            double normX = (x - eqStartX) / eqWidth;
            double waveY = (h / 2.0 + 5.0) + Math.cos(normX * 18.0 - phase * 2.2) * (h * 0.20 * smoothedLevel);
            if (x == eqStartX) gc.moveTo(x, waveY);
            else gc.lineTo(x, waveY);
        }
        gc.stroke();
    }
}
