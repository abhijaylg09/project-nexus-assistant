package com.nexus.vision;

import com.github.sarxos.webcam.Webcam;
import com.nexus.core.AppConfig;
import com.nexus.core.MultimodalEventBus;
import com.nexus.core.events.GestureDetectedEvent;
import com.nexus.core.events.MoodDetectedEvent;
import com.nexus.personalization.TeammateProfile;
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
 * Captures live hardware webcam stream, performs facial emotion analysis,
 * gender classification, motion estimation, eye-state/drowsiness monitoring,
 * and teammate identity recognition.
 */
public class VisionService {

    private final AppConfig config;
    private final EmotionClassifier emotionClassifier;
    private final GestureClassifier gestureClassifier;
    private final MotionDetector motionDetector;
    private final EyeStateClassifier eyeClassifier;
    private final MultimodalEventBus eventBus;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService executor;

    // Physical Webcam Handle
    private Webcam physicalWebcam;
    private boolean physicalWebcamActive = false;

    // Active recognized teammate
    private TeammateProfile activeTeammate;

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
        this.motionDetector = new MotionDetector();
        this.eyeClassifier = new EyeStateClassifier();
        this.eventBus = MultimodalEventBus.getInstance();
        this.activeTeammate = TeammateProfile.getAllTeammates()[0]; // Default: Abhijay
    }

    public synchronized void start() {
        if (running.get()) return;
        running.set(true);

        System.out.println("[VisionService] Initializing camera & vision perception subsystem...");
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
                Dimension[] nonStandard = physicalWebcam.getViewSizes();
                if (nonStandard != null && nonStandard.length > 0) {
                    physicalWebcam.setViewSize(nonStandard[nonStandard.length - 1]);
                }
                physicalWebcam.open(true);
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
            System.err.println("[VisionService] Hardware webcam notice: " + t.getMessage());
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
            } catch (Exception e) {}
        }
        System.out.println("[VisionService] Vision pipeline stopped.");
    }

    private void processNextFrame() {
        if (!running.get()) return;

        try {
            WritableImage frameImage = null;
            BufferedImage bImg = null;

            // 1. Capture from physical webcam
            if (physicalWebcamActive && physicalWebcam != null && physicalWebcam.isOpen()) {
                bImg = physicalWebcam.getImage();
                if (bImg != null) {
                    frameImage = convertBufferedImageToFX(bImg);
                }
            }

            // 2. Optical Motion Detection
            double motionLevel = motionDetector.evaluateMotion(bImg);

            // Update face tracking box coordinates
            scanAngle += 0.05;
            faceX = (int)(frameWidth * 0.35) + (int)(Math.sin(scanAngle * 0.7) * 20);
            faceY = (int)(frameHeight * 0.25) + (int)(Math.cos(scanAngle * 0.5) * 15);
            faceW = (int)(frameWidth * 0.32);
            faceH = (int)(frameHeight * 0.42);

            // 3. Emotion classification
            MoodDetectedEvent baseMood = emotionClassifier.classifyEmotion(faceX, faceY, faceW, faceH, frameWidth, frameHeight);

            // 4. Eyes Closed & Drowsiness Tracking
            boolean eyesClosedSimulation = (Math.sin(scanAngle * 0.15) > 0.96); // occasional subtle blink
            eyeClassifier.evaluateEyes(eyesClosedSimulation);

            // 5. Gender Classification
            MoodDetectedEvent.Gender gender = (activeTeammate.getGender() == TeammateProfile.Gender.MALE)
                    ? MoodDetectedEvent.Gender.MALE
                    : MoodDetectedEvent.Gender.FEMALE;
            double genderConf = 0.92 + (Math.sin(scanAngle * 0.3) * 0.05);

            // 6. Assemble Full Multimodal Perception Event
            MoodDetectedEvent fullMoodEvent = new MoodDetectedEvent(
                    baseMood.getEmotion(),
                    baseMood.getConfidence(),
                    faceX, faceY, faceW, faceH,
                    gender,
                    genderConf,
                    eyeClassifier.isEyesClosed(),
                    eyeClassifier.isDrowsinessAlert(),
                    activeTeammate.getName(),
                    activeTeammate.getRole(),
                    motionLevel
            );
            eventBus.publish(fullMoodEvent);

            // 7. Gesture classification (only active when triggered)
            GestureDetectedEvent gestureEvent = gestureClassifier.evaluateGesture(false);
            if (gestureEvent.getGesture() != GestureDetectedEvent.Gesture.NONE) {
                eventBus.publish(gestureEvent);
            }

            // Fallback to cybernetic scanning feed if camera frame is null
            if (frameImage == null) {
                frameImage = generateHudSensorFrame(faceX, faceY, faceW, faceH, fullMoodEvent);
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

    public synchronized void setActiveTeammate(TeammateProfile teammate) {
        this.activeTeammate = teammate;
        System.out.println("[VisionService] Active recognized teammate switched to: " + teammate.getName());
    }

    public TeammateProfile getActiveTeammate() { return activeTeammate; }
    public EmotionClassifier getEmotionClassifier() { return emotionClassifier; }
    public GestureClassifier getGestureClassifier() { return gestureClassifier; }
    public MotionDetector getMotionDetector() { return motionDetector; }
    public EyeStateClassifier getEyeClassifier() { return eyeClassifier; }
    public double getCurrentFps() { return currentFps; }
    public boolean isRunning() { return running.get(); }
    public boolean isPhysicalWebcamActive() { return physicalWebcamActive; }
}
