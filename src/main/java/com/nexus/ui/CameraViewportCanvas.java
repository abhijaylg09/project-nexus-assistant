package com.nexus.ui;

import com.nexus.core.events.GestureDetectedEvent;
import com.nexus.core.events.MoodDetectedEvent;
import com.nexus.vision.VideoFrame;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Holographic Cyberpunk HUD Camera Viewport Canvas.
 * Renders live camera frame, rotating target reticles, glowing corner brackets,
 * real-time FERPlus neural mood tags, ViT-ONNX gender tags, and teammate identification.
 */
public class CameraViewportCanvas extends Canvas {

    private VideoFrame currentFrame;
    private MoodDetectedEvent currentMood;
    private GestureDetectedEvent currentGesture;
    private double reticleAngle = 0.0;
    private double sweepY = 0.0;

    public CameraViewportCanvas(double width, double height) {
        super(width, height);
    }

    public void updateFrame(VideoFrame frame) {
        this.currentFrame = frame;
        render();
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
        reticleAngle += 0.04;
        sweepY = (sweepY + 2.5) % h;

        // 1. Base video frame or Cyber Grid
        if (currentFrame != null && currentFrame.getFxImage() != null) {
            gc.drawImage(currentFrame.getFxImage(), 0, 0, w, h);

            // Subtle cyber scanline overlay
            gc.setFill(Color.rgb(0, 242, 254, 0.08));
            gc.fillRect(0, sweepY, w, 2.5);
        } else {
            // Futuristic dark space viewport
            gc.setFill(Color.rgb(8, 14, 28));
            gc.fillRoundRect(0, 0, w, h, 12, 12);

            // Subtle cyber grid
            gc.setStroke(Color.rgb(0, 242, 254, 0.12));
            gc.setLineWidth(1.0);
            for (int x = 20; x < w; x += 30) {
                gc.strokeLine(x, 0, x, h);
            }
            for (int y = 20; y < h; y += 30) {
                gc.strokeLine(0, y, w, y);
            }

            // Radar sweep
            gc.setFill(Color.rgb(0, 242, 254, 0.15));
            gc.fillRect(0, sweepY, w, 3);
        }

        // 2. HUD Target Reticle / Face Bounding Box
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

            // Glowing corner brackets
            Color bracketColor = Color.rgb(0, 242, 254, 0.95);
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

            // Center rotating crosshair
            double cx = bx + bw / 2.0;
            double cy = by + bh / 2.0;

            gc.save();
            gc.translate(cx, cy);
            gc.rotate(Math.toDegrees(reticleAngle));
            gc.setStroke(Color.rgb(0, 242, 254, 0.45));
            gc.setLineWidth(1.0);
            gc.strokeOval(-14, -14, 28, 28);
            gc.strokeLine(-18, 0, -8, 0);
            gc.strokeLine(8, 0, 18, 0);
            gc.strokeLine(0, -18, 0, -8);
            gc.strokeLine(0, 8, 0, 18);
            gc.restore();

            // ─── Emotion Tag Card above face ───
            Color moodAccent = switch (currentMood.getEmotion()) {
                case HAPPY -> Color.rgb(16, 185, 129); // Emerald
                case STRESSED -> Color.rgb(244, 63, 94); // Coral Red
                case SURPRISED -> Color.rgb(245, 158, 11); // Amber
                case SAD -> Color.rgb(148, 163, 184); // Slate
                default -> Color.rgb(0, 242, 254); // Cyan (Focused/Neutral)
            };

            gc.setFill(Color.rgb(10, 18, 36, 0.88));
            gc.fillRoundRect(bx, by - 28, 175, 24, 8, 8);
            gc.setStroke(moodAccent);
            gc.setLineWidth(1.2);
            gc.strokeRoundRect(bx, by - 28, 175, 24, 8, 8);

            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.setFill(moodAccent);
            gc.fillText("● MOOD: " + currentMood.getEmotion().name() + " (" + currentMood.getFormattedConfidence() + " FERPlus)", bx + 8, by - 12);

            // ─── Glassmorphic Gender Tag below face box ───
            boolean isMale = currentMood.getGender() == MoodDetectedEvent.Gender.MALE;
            Color genderBg = isMale ? Color.rgb(10, 25, 52, 0.88) : Color.rgb(50, 14, 42, 0.88);
            Color genderBorder = isMale ? Color.rgb(56, 189, 248, 0.8) : Color.rgb(244, 114, 182, 0.8);
            Color genderText = isMale ? Color.rgb(56, 189, 248) : Color.rgb(244, 114, 182);

            gc.setFill(genderBg);
            gc.fillRoundRect(bx, by + bh + 6, 175, 24, 8, 8);
            gc.setStroke(genderBorder);
            gc.setLineWidth(1.2);
            gc.strokeRoundRect(bx, by + bh + 6, 175, 24, 8, 8);

            String genderSymbol = isMale ? "♂ MALE" : "♀ FEMALE";
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.setFill(genderText);
            gc.fillText(genderSymbol + " • " + currentMood.getFormattedGenderConfidence() + " [ViT-ONNX AI]", bx + 8, by + bh + 22);
        }

        // 3. Recognized Teammate Identity Banner (Top Left)
        if (currentMood != null && currentMood.getRecognizedIdentity() != null) {
            gc.setFill(Color.rgb(11, 20, 42, 0.88));
            gc.fillRoundRect(8, 8, 260, 38, 10, 10);
            gc.setStroke(Color.rgb(0, 242, 254, 0.75));
            gc.setLineWidth(1.2);
            gc.strokeRoundRect(8, 8, 260, 38, 10, 10);

            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            gc.setFill(Color.rgb(0, 242, 254));
            gc.fillText("👤 " + currentMood.getRecognizedIdentity(), 16, 24);

            gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9));
            gc.setFill(Color.rgb(148, 163, 184));
            String roleText = currentMood.getIdentityRole();
            if (roleText.length() > 38) roleText = roleText.substring(0, 38) + "...";
            gc.fillText(roleText, 16, 37);
        }

        // 4. Hand Gesture Status Badge (Top Right)
        if (currentGesture != null && currentGesture.getGesture() != GestureDetectedEvent.Gesture.NONE) {
            gc.setFill(Color.rgb(147, 51, 234, 0.92));
            gc.fillRoundRect(w - 185, 8, 175, 28, 8, 8);
            gc.setStroke(Color.rgb(192, 132, 252, 0.8));
            gc.setLineWidth(1.2);
            gc.strokeRoundRect(w - 185, 8, 175, 28, 8, 8);

            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.setFill(Color.WHITE);
            gc.fillText("✋ GESTURE: " + currentGesture.getGesture().getDisplayName(), w - 177, 26);
        }

        // 5. Active Telemetry Footer (Bottom)
        gc.setFill(Color.rgb(10, 18, 36, 0.85));
        gc.fillRoundRect(8, h - 24, w - 16, 18, 6, 6);
        gc.setStroke(Color.rgb(0, 242, 254, 0.35));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(8, h - 24, w - 16, 18, 6, 6);

        double motionPercent = (currentMood != null) ? currentMood.getMotionLevel() * 100 : 5.0;
        String motionTag = (motionPercent > 30) ? "ACTIVE" : "STABLE";

        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 9));
        gc.setFill(Color.rgb(0, 242, 254, 0.9));
        gc.fillText("⚡ PYTHON AI PERCEPTION CORE ONLINE • OPENCV • FERPLUS • ViT-ONNX • MOTION: "
                + String.format("%.0f%%", motionPercent) + " [" + motionTag + "]", 16, h - 11);
    }
}
