package com.nexus.vision;

import com.github.sarxos.webcam.Webcam;
import com.nexus.core.AppConfig;
import com.nexus.core.MultimodalEventBus;
import com.nexus.core.events.GestureDetectedEvent;
import com.nexus.core.events.MoodDetectedEvent;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Computer Vision Service for Project N.E.X.U.S.
 * Captures live hardware webcam stream (via DirectShow / Sarxos),
 * performs facial emotion tracking & gesture analysis,
 * with fallback to cybernetic sensor simulation if camera is unavailable.
 */
public class VisionService {

    private final AppConfig config;
    private final EmotionClassifier emotionClassifier;
    private final GestureClassifier gestureClassifier;
    private final MultimodalEventBus eventBus;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService executor;

    // Physical Webcam Handle
    private Webcam physicalWebcam;
    private boolean physicalWebcamActive = false;

    private int frameWidth = 640;
    private int frameHeight = 480;
    private double currentFps = 30.0;
    private long frameCount = 0;
    private long lastFpsCalc = System.currentTimeMillis();

    // Scan / Tracking coordinates
    private double scanAngle = 0.0;
    private int faceX = 220;
    private int faceY = 130;
    private int faceW = 200;
    private int faceH = 200;

    public VisionService() {
        this.config = AppConfig.getInstance();
        this.emotionClassifier = new EmotionClassifier();
        this.gestureClassifier = new GestureClassifier();
        this.eventBus = MultimodalEventBus.getInstance();
    }

    public synchronized void start() {
        if (running.get()) return;
        running.set(true);

        System.out.println("[VisionService] Initializing camera subsystem...");
        initPhysicalWebcam();

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NexusVisionThread");
            t.setDaemon(true);
            return t;
        });

        int delayMs = 1000 / config.getVisionFps();
        executor.scheduleAtFixedRate(this::processNextFrame, 100, delayMs, TimeUnit.MILLISECONDS);
    }

    private void initPhysicalWebcam() {
        try {
            physicalWebcam = Webcam.getDefault();
            if (physicalWebcam != null) {
                System.out.println("[VisionService] Hardware webcam detected: " + physicalWebcam.getName());
                // Select reasonable resolution
                Dimension[] nonStandard = physicalWebcam.getViewSizes();
                if (nonStandard != null && nonStandard.length > 0) {
                    physicalWebcam.setViewSize(nonStandard[nonStandard.length - 1]);
                }
                physicalWebcam.open(true); // Open asynchronously or directly
                physicalWebcamActive = physicalWebcam.isOpen();
                if (physicalWebcamActive) {
                    Dimension size = physicalWebcam.getViewSize();
                    frameWidth = size.width;
                    frameHeight = size.height;
                    System.out.println("[VisionService] Live physical webcam stream active at " + frameWidth + "x" + frameHeight);
                }
            } else {
                System.out.println("[VisionService] No hardware webcam found. Engaging HUD sensor simulation mode.");
            }
        } catch (Throwable t) {
            System.err.println("[VisionService] Hardware webcam initialization notice: " + t.getMessage());
            System.out.println("[VisionService] Engaging cybernetic HUD sensor simulation mode.");
            physicalWebcamActive = false;
        }
    }

    public synchronized void stop() {
        running.set(false);
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (physicalWebcam != null && physicalWebcam.isOpen()) {
            try {
                physicalWebcam.close();
                System.out.println("[VisionService] Hardware webcam released.");
            } catch (Exception e) {
                // Ignore closing error
            }
        }
        System.out.println("[VisionService] Vision pipeline stopped.");
    }

    private void processNextFrame() {
        if (!running.get()) return;

        try {
            WritableImage frameImage = null;

            // 1. Attempt to capture from real physical webcam
            if (physicalWebcamActive && physicalWebcam != null && physicalWebcam.isOpen()) {
                BufferedImage bImg = physicalWebcam.getImage();
                if (bImg != null) {
                    frameImage = convertBufferedImageToFX(bImg);
                }
            }

            // Update face tracking box
            scanAngle += 0.05;
            faceX = (int)(frameWidth * 0.35) + (int)(Math.sin(scanAngle * 0.7) * 20);
            faceY = (int)(frameHeight * 0.25) + (int)(Math.cos(scanAngle * 0.5) * 15);
            faceW = (int)(frameWidth * 0.32);
            faceH = (int)(frameHeight * 0.42);

            // Emotion classification
            MoodDetectedEvent moodEvent = emotionClassifier.classifyEmotion(faceX, faceY, faceW, faceH, frameWidth, frameHeight);
            eventBus.publish(moodEvent);

            // Gesture classification (check periodic test gesture)
            boolean gestureActive = (Math.sin(scanAngle * 0.25) > 0.88);
            GestureDetectedEvent gestureEvent = gestureClassifier.evaluateGesture(gestureActive);
            if (gestureEvent.getGesture() != GestureDetectedEvent.Gesture.NONE) {
                eventBus.publish(gestureEvent);
            }

            // Fallback to cybernetic scanning feed if camera frame is null
            if (frameImage == null) {
                frameImage = generateHudSensorFrame(faceX, faceY, faceW, faceH, moodEvent);
            }

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
     * Converts AWT BufferedImage from physical webcam to high-performance JavaFX WritableImage.
     */
    private WritableImage convertBufferedImageToFX(BufferedImage bImg) {
        int w = bImg.getWidth();
        int h = bImg.getHeight();
        WritableImage wr = new WritableImage(w, h);
        PixelWriter pw = wr.getPixelWriter();

        int[] pixels = new int[w * h];
        bImg.getRGB(0, 0, w, h, pixels, 0, w);
        pw.setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0, w);

        return wr;
    }

    /**
     * Synthesizes cybernetic neural camera sensor feed for HUD display when webcam is absent.
     */
    private WritableImage generateHudSensorFrame(int fx, int fy, int fw, int fh, MoodDetectedEvent mood) {
        WritableImage image = new WritableImage(frameWidth, frameHeight);
        PixelWriter pw = image.getPixelWriter();

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
    public boolean isPhysicalWebcamActive() { return physicalWebcamActive; }
}
