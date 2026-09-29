package com.nexus.vision;

import java.awt.image.BufferedImage;

/**
 * High-performance Optical Motion Detector.
 * Compares consecutive video frames using downsampled luminance delta analysis.
 */
public class MotionDetector {

    private static final int SAMPLE_W = 64;
    private static final int SAMPLE_H = 48;
    private final int[] prevLuminance = new int[SAMPLE_W * SAMPLE_H];
    private boolean hasPrevFrame = false;

    private double currentMotionLevel = 0.0; // 0.0 to 1.0
    private boolean motionDetected = false;

    public synchronized double evaluateMotion(BufferedImage currentFrame) {
        if (currentFrame == null) return 0.0;

        int frameW = currentFrame.getWidth();
        int frameH = currentFrame.getHeight();
        if (frameW == 0 || frameH == 0) return 0.0;

        double stepX = (double) frameW / SAMPLE_W;
        double stepY = (double) frameH / SAMPLE_H;

        int totalDiff = 0;
        int idx = 0;

        for (int y = 0; y < SAMPLE_H; y++) {
            int py = (int) (y * stepY);
            for (int x = 0; x < SAMPLE_W; x++) {
                int px = (int) (x * stepX);
                int rgb = currentFrame.getRGB(px, py);

                // Quick luminance estimate: 0.299*R + 0.587*G + 0.114*B
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int lum = (r * 77 + g * 150 + b * 29) >> 8;

                if (hasPrevFrame) {
                    totalDiff += Math.abs(lum - prevLuminance[idx]);
                }
                prevLuminance[idx++] = lum;
            }
        }

        if (!hasPrevFrame) {
            hasPrevFrame = true;
            return 0.0;
        }

        // Average difference per pixel (0 to 255)
        double avgDiff = (double) totalDiff / (SAMPLE_W * SAMPLE_H);
        // Normalize: motion noticeable around 8-50
        double normalized = Math.min(1.0, avgDiff / 40.0);

        // Exponential smoothing
        currentMotionLevel = (currentMotionLevel * 0.7) + (normalized * 0.3);
        motionDetected = currentMotionLevel > 0.12;

        return currentMotionLevel;
    }

    public double getCurrentMotionLevel() {
        return currentMotionLevel;
    }

    public boolean isMotionDetected() {
        return motionDetected;
    }

    public String getMotionStatus() {
        if (currentMotionLevel > 0.45) return "HIGH MOTION";
        if (currentMotionLevel > 0.12) return "ACTIVE";
        return "STABLE / IDLE";
    }
}
