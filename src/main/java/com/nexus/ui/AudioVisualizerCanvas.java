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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * ChatGPT / Jarvis-Grade Cyberpunk Live Audio Visualizer Canvas.
 * 
 * Features:
 *  - 60 FPS ultra-smooth hardware-accelerated AnimationTimer
 *  - Holographic pulsating AI Voice Orb with celestial multi-ring harmonic waves
 *  - Floating energy spark motes / micro-particles that drift from audio peaks
 *  - Eye-friendly gradient equalizer with gravity peak drop indicators
 *  - Dual flowing harmonic acoustic wave ribbons with translucent glow fills
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

    // Floating micro-particles for peak audio excitation
    private static class AudioSpark {
        double x, y;
        double vx, vy;
        double alpha;
        double radius;
        Color color;

        AudioSpark(double x, double y, double vx, double vy, double radius, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.alpha = 0.95;
            this.radius = radius;
            this.color = color;
        }

        boolean update() {
            x += vx;
            y += vy;
            alpha -= 0.024;
            return alpha > 0.0;
        }
    }

    private final List<AudioSpark> sparks = new ArrayList<>();
    private final Random random = new Random();

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
                smoothedLevel = smoothedLevel * 0.84 + audioLevel * 0.16;
                phase += 0.065 + (smoothedLevel * 0.08);

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

        // 1. Dark Glassmorphic Backdrop (Eye-Comfort Midnight Slate)
        gc.setFill(Color.rgb(11, 19, 39, 0.82));
        gc.fillRoundRect(0, 0, w, h, 10, 10);

        // Neon border accent (Soft luminous glow)
        Color borderColor = speaking ? Color.rgb(244, 63, 94, 0.55)
                : (listening ? Color.rgb(56, 189, 248, 0.65) : Color.rgb(56, 189, 248, 0.25));
        gc.setStroke(borderColor);
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(0.5, 0.5, w - 1.0, h - 1.0, 10, 10);

        // 2. Holographic Pulsating Voice Orb (Left Section: x = 24, y = h/2)
        double orbX = 26.0;
        double orbY = h / 2.0;
        double baseRadius = 9.0 + (smoothedLevel * 9.0);

        // Radiating celestial pulse wave rings
        for (int r = 1; r <= 3; r++) {
            double ringRadius = baseRadius + ((phase * 14.0 + r * 9.0) % 22.0);
            double ringAlpha = Math.max(0.0, 1.0 - (ringRadius / 32.0)) * 0.40;
            Color ringColor = speaking
                    ? Color.rgb(251, 113, 133, ringAlpha)
                    : (listening ? Color.rgb(56, 189, 248, ringAlpha) : Color.rgb(129, 140, 248, ringAlpha * 0.6));
            gc.setStroke(ringColor);
            gc.setLineWidth(1.2);
            gc.strokeOval(orbX - ringRadius, orbY - ringRadius, ringRadius * 2.0, ringRadius * 2.0);
        }

        // Center glowing orb core with soft radial-like double circle
        Color coreColor = speaking ? Color.rgb(251, 113, 133) : (listening ? Color.rgb(56, 189, 248) : Color.rgb(96, 165, 250));
        gc.setFill(Color.rgb(coreColor.getRed() > 0.5 ? 255 : 56, (int)(coreColor.getGreen()*255), (int)(coreColor.getBlue()*255), 0.25));
        gc.fillOval(orbX - baseRadius * 0.9, orbY - baseRadius * 0.9, baseRadius * 1.8, baseRadius * 1.8);
        gc.setFill(coreColor);
        gc.fillOval(orbX - baseRadius * 0.55, orbY - baseRadius * 0.55, baseRadius * 1.1, baseRadius * 1.1);

        // 3. Status Tag Text Overlay (Ergonomic Tones)
        String statusText;
        Color tagColor;
        if (speaking) {
            statusText = "🔊 TALKBACK: READING ALOUD";
            tagColor = Color.rgb(251, 113, 133);
        } else if (listening) {
            statusText = "🎙️ LIVE VOICE: LISTENING (SPEAK NOW)";
            tagColor = Color.rgb(56, 189, 248);
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
                new Stop(0.0, speaking ? Color.rgb(225, 29, 72, 0.22) : Color.rgb(2, 132, 199, 0.22)),
                new Stop(0.5, speaking ? Color.rgb(251, 113, 133, 0.85) : Color.rgb(56, 189, 248, 0.85)),
                new Stop(1.0, speaking ? Color.rgb(251, 191, 36, 0.95) : Color.rgb(192, 132, 252, 0.95))
        );
        gc.setFill(barGrad);

        for (int i = 0; i < numBars; i++) {
            // Harmonic wave formula across frequencies
            double harmonic = Math.sin(phase * 1.6 + i * 0.22) * 0.45
                    + Math.cos(phase * 2.4 - i * 0.15) * 0.35
                    + 0.5;

            double targetHeight = Math.max(3.0, harmonic * (h * 0.66) * (smoothedLevel * 1.35));
            double bx = eqStartX + i * (barWidth + 2.5);
            double by = h - targetHeight - 4.0;

            gc.fillRoundRect(bx, by, barWidth, targetHeight, 3, 3);

            // Spawn floating sparkle particles from highest peaks
            if (smoothedLevel > 0.18 && targetHeight > (h * 0.35) && random.nextDouble() < 0.15) {
                Color sparkCol = speaking ? Color.rgb(253, 224, 71) : Color.rgb(125, 211, 252);
                sparks.add(new AudioSpark(
                        bx + barWidth / 2.0,
                        by,
                        (random.nextDouble() - 0.5) * 0.8,
                        -0.8 - random.nextDouble() * 1.2,
                        1.2 + random.nextDouble() * 1.5,
                        sparkCol
                ));
            }

            // Gravity peak drop indicator
            if (targetHeight > peakHeights[i]) {
                peakHeights[i] = targetHeight;
                peakVelocities[i] = 0.0;
            } else {
                peakVelocities[i] += 0.20;
                peakHeights[i] = Math.max(3.0, peakHeights[i] - peakVelocities[i]);
            }

            double peakY = h - peakHeights[i] - 6.0;
            gc.setFill(speaking ? Color.rgb(254, 240, 138, 0.9) : Color.rgb(125, 211, 252, 0.9));
            gc.fillRoundRect(bx, peakY, barWidth, 1.8, 1, 1);
            gc.setFill(barGrad); // Reset fill
        }

        // 5. Update & Render Floating Energy Sparkles
        Iterator<AudioSpark> iter = sparks.iterator();
        while (iter.hasNext()) {
            AudioSpark spark = iter.next();
            if (spark.update()) {
                gc.setFill(Color.color(spark.color.getRed(), spark.color.getGreen(), spark.color.getBlue(), Math.max(0.0, spark.alpha)));
                gc.fillOval(spark.x - spark.radius, spark.y - spark.radius, spark.radius * 2, spark.radius * 2);
            } else {
                iter.remove();
            }
        }

        // 6. Dual Harmonic Continuous Acoustic Sine Ribbons
        gc.setLineWidth(1.6);
        gc.setStroke(speaking ? Color.rgb(244, 63, 94, 0.85) : Color.rgb(56, 189, 248, 0.85));
        gc.beginPath();
        for (double x = eqStartX; x < w - 10.0; x += 3.0) {
            double normX = (x - eqStartX) / eqWidth;
            double waveY = (h / 2.0 + 5.0) + Math.sin(normX * 14.0 + phase * 2.8) * (h * 0.26 * smoothedLevel);
            if (x == eqStartX) gc.moveTo(x, waveY);
            else gc.lineTo(x, waveY);
        }
        gc.stroke();

        // Secondary harmonic counter-wave (Gentle lavender / warm amber)
        gc.setLineWidth(1.0);
        gc.setStroke(speaking ? Color.rgb(251, 146, 60, 0.6) : Color.rgb(168, 85, 247, 0.65));
        gc.beginPath();
        for (double x = eqStartX; x < w - 10.0; x += 3.0) {
            double normX = (x - eqStartX) / eqWidth;
            double waveY = (h / 2.0 + 5.0) + Math.cos(normX * 18.0 - phase * 2.2) * (h * 0.18 * smoothedLevel);
            if (x == eqStartX) gc.moveTo(x, waveY);
            else gc.lineTo(x, waveY);
        }
        gc.stroke();
    }
}
