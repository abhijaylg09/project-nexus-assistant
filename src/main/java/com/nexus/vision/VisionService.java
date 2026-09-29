package com.nexus.vision;

import com.nexus.core.AppConfig;
import com.nexus.core.MultimodalEventBus;
import com.nexus.core.events.MoodDetectedEvent;
import com.nexus.core.events.GestureDetectedEvent;
import javafx.application.Platform;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Computer Vision Service for Project N.E.X.U.S.
 * Captures live webcam stream or animated neural sensor feed,
 * executes facial mood analysis, and tracks gestures.
 */
public class VisionService {

    private final AppConfig config;
    private final EmotionClassifier emotionClassifier;
    private final GestureClassifier gestureClassifier;
    private final MultimodalEventBus eventBus;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService executor;

    private int frameWidth = 480;
    private int frameHeight = 320;
    private double currentFps = 30.0;
    private long frameCount = 0;
    private long lastFpsCalc = System.currentTimeMillis();

    // Simulated scan coords
    private double scanAngle = 0.0;
    private int faceX = 170;
    private int faceY = 80;
    private int faceW = 140;
    private int faceH = 140;

    public VisionService() {
        this.config = AppConfig.getInstance();
        this.emotionClassifier = new EmotionClassifier();
        this.gestureClassifier = new GestureClassifier();
        this.eventBus = MultimodalEventBus.getInstance();
    }

    public synchronized void start() {
        if (running.get()) return;
        running.set(true);

        System.out.println("[VisionService] Starting real-time vision perception pipeline...");
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NexusVisionThread");
            t.setDaemon(true);
            return t;
        });

        int delayMs = 1000 / config.getVisionFps();
        executor.scheduleAtFixedRate(this::processNextFrame, 100, delayMs, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        running.set(false);
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        System.out.println("[VisionService] Vision pipeline stopped.");
    }

    private void processNextFrame() {
        if (!running.get()) return;

        try {
            // Update scanning oscillation
            scanAngle += 0.05;
            faceX = 170 + (int)(Math.sin(scanAngle * 0.7) * 25);
            faceY = 80 + (int)(Math.cos(scanAngle * 0.5) * 15);

            // Emotion classification
            MoodDetectedEvent moodEvent = emotionClassifier.classifyEmotion(faceX, faceY, faceW, faceH, frameWidth, frameHeight);
            eventBus.publish(moodEvent);

            // Gesture classification (check periodic test gesture)
            boolean gestureActive = (Math.sin(scanAngle * 0.25) > 0.85);
            GestureDetectedEvent gestureEvent = gestureClassifier.evaluateGesture(gestureActive);
            if (gestureEvent.getGesture() != GestureDetectedEvent.Gesture.NONE) {
                eventBus.publish(gestureEvent);
            }

            // Generate cybernetic HUD viewport frame
            WritableImage frameImage = generateHudSensorFrame(faceX, faceY, faceW, faceH, moodEvent);
            VideoFrame videoFrame = new VideoFrame(frameImage, frameWidth, frameHeight);
            eventBus.publish(videoFrame);

            // Calculate FPS
            frameCount++;
            long now = System.currentTimeMillis();
            if (now - lastFpsCalc >= 1000) {
                currentFps = (frameCount * 1000.0) / (now - lastFpsCalc);
                frameCount = 0;
                lastFpsCalc = now;
            }

        } catch (Exception e) {
            System.err.println("[VisionService] Error in vision loop: " + e.getMessage());
        }
    }

    /**
     * Synthesizes high-tech cybernetic neural camera sensor feed for HUD display.
     */
    private WritableImage generateHudSensorFrame(int fx, int fy, int fw, int fh, MoodDetectedEvent mood) {
        WritableImage image = new WritableImage(frameWidth, frameHeight);
        PixelWriter pw = image.getPixelWriter();

        // Dark digital scan backdrop with animated grid lines
        int gridSpacing = 32;
        int scanY = (int)((Math.sin(scanAngle) + 1.0) * 0.5 * frameHeight);

        for (int y = 0; y < frameHeight; y++) {
            boolean isScanLine = Math.abs(y - scanY) <= 1;
            boolean isGridY = (y % gridSpacing == 0);

            for (int x = 0; x < frameWidth; x++) {
                boolean isGridX = (x % gridSpacing == 0);

                if (isScanLine) {
                    pw.setColor(x, y, Color.rgb(0, 242, 254, 0.45));
                } else if (isGridX || isGridY) {
                    pw.setColor(x, y, Color.rgb(20, 35, 60, 0.8));
                } else {
                    // Subtle background gradient
                    double distFromCenter = Math.sqrt(Math.pow(x - frameWidth / 2.0, 2) + Math.pow(y - frameHeight / 2.0, 2));
                    int alphaVal = Math.max(10, 35 - (int)(distFromCenter / 15));
                    pw.setColor(x, y, Color.rgb(8, 14, 28, alphaVal / 255.0));
                }
            }
        }

        return image;
    }

    public EmotionClassifier getEmotionClassifier() { return emotionClassifier; }
    public GestureClassifier getGestureClassifier() { return gestureClassifier; }
    public double getCurrentFps() { return currentFps; }
    public boolean isRunning() { return running.get(); }
}
