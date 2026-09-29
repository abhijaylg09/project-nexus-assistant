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
 * Renders live camera frame, target reticle, face detection box, and emotion indicators.
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
            // Standby dark background
            gc.setFill(Color.rgb(10, 16, 30));
            gc.fillRoundRect(0, 0, w, h, 8, 8);
        }

        // 2. HUD Target Reticle / Face Bounding Box
        if (currentMood != null) {
            double scaleX = w / 480.0;
            double scaleY = h / 320.0;

            double bx = currentMood.getFaceX() * scaleX;
            double by = currentMood.getFaceY() * scaleY;
            double bw = currentMood.getFaceWidth() * scaleX;
            double bh = currentMood.getFaceHeight() * scaleY;

            // Draw glowing cyan corner brackets
            gc.setStroke(Color.rgb(0, 242, 254, 0.95));
            gc.setLineWidth(2.5);
            double cornerLen = Math.min(bw, bh) * 0.22;

            // Top-Left
            gc.strokeLine(bx, by, bx + cornerLen, by);
            gc.strokeLine(bx, by, bx, by + cornerLen);
            // Top-Right
            gc.strokeLine(bx + bw, by, bx + bw - cornerLen, by);
            gc.strokeLine(bx + bw, by, bx + bw, by + cornerLen);
            // Bottom-Left
            gc.strokeLine(bx, by + bh, bx + cornerLen, by + bh);
            gc.strokeLine(bx, by + bh, bx, by + bh - cornerLen);
            // Bottom-Right
            gc.strokeLine(bx + bw, by + bh, bx + bw - cornerLen, by + bh);
            gc.strokeLine(bx + bw, by + bh, bx + bw, by + bh - cornerLen);

            // Center crosshair
            gc.setStroke(Color.rgb(0, 242, 254, 0.4));
            gc.setLineWidth(1.0);
            double cx = bx + bw / 2.0;
            double cy = by + bh / 2.0;
            gc.strokeLine(cx - 10, cy, cx + 10, cy);
            gc.strokeLine(cx, cy - 10, cx, cy + 10);

            // Emotion Tag Card above face
            gc.setFill(Color.rgb(13, 22, 41, 0.85));
            gc.fillRoundRect(bx, by - 28, 140, 22, 4, 4);
            gc.setStroke(Color.rgb(0, 242, 254, 0.6));
            gc.strokeRoundRect(bx, by - 28, 140, 22, 4, 4);

            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.setFill(Color.rgb(0, 255, 135));
            gc.fillText("MOOD: " + currentMood.getEmotion().name() + " (" + currentMood.getFormattedConfidence() + ")", bx + 6, by - 13);
        }

        // 3. Top-Right Gesture Status Badge
        if (currentGesture != null && currentGesture.getGesture() != GestureDetectedEvent.Gesture.NONE) {
            gc.setFill(Color.rgb(155, 81, 224, 0.9));
            gc.fillRoundRect(w - 180, 12, 168, 26, 6, 6);
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.setFill(Color.WHITE);
            gc.fillText("GESTURE: " + currentGesture.getGesture().getDisplayName(), w - 172, 29);
        }

        // 4. Viewport Corner Sci-Fi Borders
        gc.setStroke(Color.rgb(0, 242, 254, 0.35));
        gc.setLineWidth(1.0);
        gc.strokeRect(4, 4, w - 8, h - 8);

        // Animated scan line indicator at bottom
        reticleAngle += 0.05;
        gc.setFill(Color.rgb(0, 242, 254, 0.7));
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 9));
        gc.fillText("[LIVE PERCEPTION FEED - OPENCV/ONNX RUNTIME]", 12, h - 12);
    }
}
