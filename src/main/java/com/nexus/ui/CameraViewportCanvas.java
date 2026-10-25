package com.nexus.ui;

import com.nexus.core.events.GestureDetectedEvent;
import com.nexus.core.events.MoodDetectedEvent;
import com.nexus.vision.VideoFrame;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Holographic Cyberpunk HUD Camera Viewport Canvas.
 * 
 * Features:
 *  - 60 FPS independent AnimationTimer for fluid, uninterrupted cyber visuals
 *  - Pulsing breathing corner brackets on detected faces with smooth alpha modulation
 *  - Rotating concentric target reticle and dashed crosshairs
 *  - High-tech HUD framing corner brackets and telemetry overlays
 *  - Real-time FERPlus neural mood tags, ViT-ONNX gender tags, and gesture badges
 *  - Eye-friendly, soothing cyan/slate/mint palette
 */
public class CameraViewportCanvas extends Canvas {

    private VideoFrame currentFrame;
    private MoodDetectedEvent currentMood;
    private GestureDetectedEvent currentGesture;
    private double reticleAngle = 0.0;
    private double sweepY = 0.0;
    private boolean active = true;

    public CameraViewportCanvas(double width, double height) {
        super(width, height);
        startAnimation();
    }

    private void startAnimation() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!active) return;
                reticleAngle += 0.035;
                sweepY = (sweepY + 2.2) % Math.max(1.0, getHeight());
                render();
            }
        };
        timer.start();
    }

    public void updateFrame(VideoFrame frame) {
        this.currentFrame = frame;
    }

    public void updateMood(MoodDetectedEvent mood) {
        this.currentMood = mood;
    }

    public void updateGesture(GestureDetectedEvent gesture) {
        this.currentGesture = gesture;
    }

    private void render() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        gc.clearRect(0, 0, w, h);

        // 1. Base video frame or Cyber Grid
        if (currentFrame != null && currentFrame.getFxImage() != null) {
            gc.drawImage(currentFrame.getFxImage(), 0, 0, w, h);

            // Subtle, eye-friendly cyber scanline overlay
            gc.setFill(Color.rgb(56, 189, 248, 0.06));
            gc.fillRect(0, sweepY, w, 2.5);
        } else {
            // Futuristic ergonomic dark slate viewport
            gc.setFill(Color.rgb(11, 19, 39));
            gc.fillRoundRect(0, 0, w, h, 14, 14);

            // Subtle cyber grid
            gc.setStroke(Color.rgb(56, 189, 248, 0.10));
            gc.setLineWidth(1.0);
            for (int x = 20; x < w; x += 32) {
                gc.strokeLine(x, 0, x, h);
            }
            for (int y = 20; y < h; y += 32) {
                gc.strokeLine(0, y, w, y);
            }

            // Radar circular range rings
            double cx = w / 2.0;
            double cy = h / 2.0;
            gc.setStroke(Color.rgb(56, 189, 248, 0.18));
            for (int r = 40; r <= 140; r += 35) {
                gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
            }

            // Rotating sweep radar line
            gc.save();
            gc.translate(cx, cy);
            gc.rotate(Math.toDegrees(reticleAngle));
            gc.setStroke(Color.rgb(56, 189, 248, 0.35));
            gc.strokeLine(0, 0, 140, 0);
            gc.restore();

            // Radar sweep horizontal bar
            gc.setFill(Color.rgb(56, 189, 248, 0.12));
            gc.fillRect(0, sweepY, w, 3);
        }

        // 2. High-Tech HUD Viewport Corner Framing Brackets
        double cornerSpan = 18.0;
        gc.setStroke(Color.rgb(56, 189, 248, 0.45));
        gc.setLineWidth(2.0);
        // Top-Left
        gc.strokeLine(8, 8, 8 + cornerSpan, 8);
        gc.strokeLine(8, 8, 8, 8 + cornerSpan);
        // Top-Right
        gc.strokeLine(w - 8, 8, w - 8 - cornerSpan, 8);
        gc.strokeLine(w - 8, 8, w - 8, 8 + cornerSpan);
        // Bottom-Left
        gc.strokeLine(8, h - 8, 8 + cornerSpan, h - 8);
        gc.strokeLine(8, h - 8, 8, h - 8 - cornerSpan);
        // Bottom-Right
        gc.strokeLine(w - 8, h - 8, w - 8 - cornerSpan, h - 8);
        gc.strokeLine(w - 8, h - 8, w - 8, h - 8 - cornerSpan);

        // 3. Top System Telemetry Watermark
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 9));
        gc.setFill(Color.rgb(148, 163, 184, 0.85));
        gc.fillText("SYS.VISION • 60 FPS • ViT BIOMETRICS", 16, 20);

        // 4. HUD Target Reticle / Face Bounding Box with Breathing Pulse Animation
        if (currentMood != null) {
            double frameW = (currentFrame != null && currentFrame.getWidth() > 0) ? currentFrame.getWidth() : 640.0;
            double frameH = (currentFrame != null && currentFrame.getHeight() > 0) ? currentFrame.getHeight() : 480.0;
            double scaleX = w / frameW;
            double scaleY = h / frameH;

            double bx = currentMood.getFaceX() * scaleX;
            double by = currentMood.getFaceY() * scaleY;
            double bw = Math.max(90, currentMood.getFaceWidth() * scaleX);
            double bh = Math.max(100, currentMood.getFaceHeight() * scaleY);

            // Clamp inside viewport
            bx = Math.max(10, Math.min(w - bw - 10, bx));
            by = Math.max(34, Math.min(h - bh - 34, by));

            // Smooth breathing pulse animation for corner brackets
            double pulseAlpha = 0.70 + 0.30 * Math.sin(reticleAngle * 4.0);
            Color bracketColor = Color.rgb(56, 189, 248, pulseAlpha);
            gc.setStroke(bracketColor);
            gc.setLineWidth(2.5);
            double cornerLen = Math.min(bw, bh) * 0.22;

            // Corners
            gc.strokeLine(bx, by, bx + cornerLen, by);
            gc.strokeLine(bx, by, bx, by + cornerLen);
            gc.strokeLine(bx + bw, by, bx + bw - cornerLen, by);
            gc.strokeLine(bx + bw, by, bx + bw, by + cornerLen);
            gc.strokeLine(bx, by + bh, bx + cornerLen, by + bh);
            gc.strokeLine(bx, by + bh, bx, by + bh - cornerLen);
            gc.strokeLine(bx + bw, by + bh, bx + bw - cornerLen, by + bh);
            gc.strokeLine(bx + bw, by + bh, bx + bw, by + bh - cornerLen);

            // Center rotating holographic crosshair
            double cx = bx + bw / 2.0;
            double cy = by + bh / 2.0;

            gc.save();
            gc.translate(cx, cy);
            gc.rotate(Math.toDegrees(reticleAngle));
            gc.setStroke(Color.rgb(56, 189, 248, 0.50));
            gc.setLineWidth(1.2);
            gc.strokeOval(-14, -14, 28, 28);
            gc.strokeLine(-18, 0, -8, 0);
            gc.strokeLine(8, 0, 18, 0);
            gc.strokeLine(0, -18, 0, -8);
            gc.strokeLine(0, 8, 0, 18);
            gc.restore();
        }

        // 5. Hand Gesture Status Badge (Top Right)
        if (currentGesture != null && currentGesture.getGesture() != GestureDetectedEvent.Gesture.NONE) {
            gc.setFill(Color.rgb(124, 58, 237, 0.88));
            gc.fillRoundRect(w - 195, 8, 185, 26, 8, 8);
            gc.setStroke(Color.rgb(192, 132, 252, 0.75));
            gc.setLineWidth(1.2);
            gc.strokeRoundRect(w - 195, 8, 185, 26, 8, 8);

            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.setFill(Color.WHITE);
            gc.fillText("✋ GESTURE: " + currentGesture.getGesture().getDisplayName() + " [AI]", w - 187, 25);
        }

        // 6. Active Telemetry Footer (Bottom)
        gc.setFill(Color.rgb(11, 19, 39, 0.88));
        gc.fillRoundRect(8, h - 24, w - 16, 18, 6, 6);
        gc.setStroke(Color.rgb(56, 189, 248, 0.35));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(8, h - 24, w - 16, 18, 6, 6);

        double motionPercent = (currentMood != null) ? currentMood.getMotionLevel() * 100 : 5.0;
        String motionTag = (motionPercent > 30) ? "ACTIVE" : "STABLE";

        String moodText = (currentMood != null)
                ? currentMood.getEmotion().name() + " (" + currentMood.getFormattedConfidence() + ")"
                : "ANALYZING";
        String genderText = (currentMood != null)
                ? (currentMood.getGender() == MoodDetectedEvent.Gender.MALE ? "♂ MALE" : "♀ FEMALE") + " (" + currentMood.getFormattedGenderConfidence() + ")"
                : "DETECTING";

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 9));
        gc.setFill(Color.rgb(56, 189, 248, 0.95));
        gc.fillText("⚡ ViT-MOOD: " + moodText + " • ViT-GENDER: " + genderText + " • MOTION: "
                + String.format("%.0f%%", motionPercent) + " [" + motionTag + "]", 16, h - 11);
    }
}
