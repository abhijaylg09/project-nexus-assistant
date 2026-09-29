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
 * High-tech HUD Camera Viewport Canvas.
 * Renders live camera frame, target reticle, face bounding box,
 * gender detection, teammate recognition, and optical motion.
 */
public class CameraViewportCanvas extends Canvas {

    private VideoFrame currentFrame;
    private MoodDetectedEvent currentMood;
    private GestureDetectedEvent currentGesture;
    private double reticleAngle = 0.0;

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

        // 1. Draw base video frame
        if (currentFrame != null && currentFrame.getFxImage() != null) {
            gc.drawImage(currentFrame.getFxImage(), 0, 0, w, h);
        } else {
            gc.setFill(Color.rgb(10, 16, 30));
            gc.fillRoundRect(0, 0, w, h, 8, 8);
        }

        // 2. HUD Target Reticle / Face Bounding Box
        if (currentMood != null && currentFrame != null) {
            double frameW = currentFrame.getWidth() > 0 ? currentFrame.getWidth() : 640.0;
            double frameH = currentFrame.getHeight() > 0 ? currentFrame.getHeight() : 480.0;
            double scaleX = w / frameW;
            double scaleY = h / frameH;

            double bx = currentMood.getFaceX() * scaleX;
            double by = currentMood.getFaceY() * scaleY;
            double bw = currentMood.getFaceWidth() * scaleX;
            double bh = currentMood.getFaceHeight() * scaleY;

            // Clamp inside viewport
            bx = Math.max(10, Math.min(w - bw - 10, bx));
            by = Math.max(30, Math.min(h - bh - 30, by));

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

            // Center target crosshair
            gc.setStroke(Color.rgb(0, 242, 254, 0.4));
            gc.setLineWidth(1.0);
            double cx = bx + bw / 2.0;
            double cy = by + bh / 2.0;
            gc.strokeLine(cx - 8, cy, cx + 8, cy);
            gc.strokeLine(cx, cy - 8, cx, cy + 8);



            // Emotion Tag Card above face
            gc.setFill(Color.rgb(13, 22, 41, 0.88));
            gc.fillRoundRect(bx, by - 26, 140, 22, 4, 4);
            gc.setStroke(Color.rgb(0, 242, 254, 0.6));
            gc.strokeRoundRect(bx, by - 26, 140, 22, 4, 4);

            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.setFill(Color.rgb(0, 255, 135));
            gc.fillText("MOOD: " + currentMood.getEmotion().name() + " (" + currentMood.getFormattedConfidence() + ")", bx + 6, by - 11);

            // Gender Tag below face box
            gc.setFill(Color.rgb(13, 22, 41, 0.88));
            gc.fillRoundRect(bx, by + bh + 4, 140, 22, 4, 4);
            gc.setStroke(Color.rgb(56, 189, 248, 0.4));
            gc.strokeRoundRect(bx, by + bh + 4, 140, 22, 4, 4);

            String genderSymbol = (currentMood.getGender() == MoodDetectedEvent.Gender.MALE) ? "MALE" : "FEMALE";

            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
            gc.setFill(currentMood.getGender() == MoodDetectedEvent.Gender.MALE ? Color.rgb(56, 189, 248) : Color.rgb(244, 114, 182));
            gc.fillText(genderSymbol + " (" + currentMood.getFormattedGenderConfidence() + ")", bx + 6, by + bh + 19);
        }

        // 3. Recognized Teammate Identity Banner (Top Left)
        if (currentMood != null && currentMood.getRecognizedIdentity() != null) {
            gc.setFill(Color.rgb(13, 22, 41, 0.9));
            gc.fillRoundRect(8, 8, 240, 32, 6, 6);
            gc.setStroke(Color.rgb(0, 242, 254, 0.7));
            gc.strokeRoundRect(8, 8, 240, 32, 6, 6);

            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.setFill(Color.rgb(0, 242, 254));
            gc.fillText("TEAMMATE: " + currentMood.getRecognizedIdentity(), 16, 22);

            gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 8));
            gc.setFill(Color.rgb(148, 163, 184));
            String roleText = currentMood.getIdentityRole();
            if (roleText.length() > 36) roleText = roleText.substring(0, 36) + "...";
            gc.fillText(roleText, 16, 33);
        }

        // 4. Gesture Status Badge (Top Right)
        if (currentGesture != null && currentGesture.getGesture() != GestureDetectedEvent.Gesture.NONE) {
            gc.setFill(Color.rgb(155, 81, 224, 0.92));
            gc.fillRoundRect(w - 175, 8, 165, 26, 6, 6);
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.setFill(Color.WHITE);
            gc.fillText("GESTURE: " + currentGesture.getGesture().getDisplayName(), w - 167, 25);
        }



        // 6. Motion Indicator & Feed Status Banner (Bottom)
        reticleAngle += 0.05;
        gc.setFill(Color.rgb(0, 242, 254, 0.75));
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 9));

        double motionPercent = (currentMood != null) ? currentMood.getMotionLevel() * 100 : 5.0;
        String motionTag = (motionPercent > 30) ? "ACTIVE" : "STABLE";
        gc.fillText("[LIVE FEED] MOTION: " + String.format("%.0f%%", motionPercent) + " [" + motionTag + "]", 12, h - 10);
    }
}
