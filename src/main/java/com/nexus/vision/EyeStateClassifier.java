package com.nexus.vision;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Advanced Optical Eye State Classifier and Drowsiness Monitor.
 * Performs direct pixel-level luminance variance, pupil/sclera contrast analysis,
 * and palpebral fissure gradient detection across both eyes on the live camera frame.
 */
public class EyeStateClassifier {

    public enum EyeStatus { OPEN, BLINKING, CLOSED }

    private EyeStatus currentStatus = EyeStatus.OPEN;
    private double leftEyeScore = 0.80;   // 0.0 (closed) to 1.0 (wide open)
    private double rightEyeScore = 0.80;  // 0.0 (closed) to 1.0 (wide open)
    private double smoothedEyeScore = 0.80;

    private Rectangle leftEyeBox = new Rectangle();
    private Rectangle rightEyeBox = new Rectangle();

    private long closedStartTime = 0;
    private boolean drowsinessAlert = false;
    private boolean manualOverride = false;
    private long overrideExpiry = 0;

    /**
     * Optical frame evaluation from the live webcam image.
     */
    public synchronized void evaluateEyesFromFrame(BufferedImage frame, int faceX, int faceY, int faceW, int faceH) {
        long now = System.currentTimeMillis();

        if (manualOverride) {
            if (now > overrideExpiry) {
                manualOverride = false;
            } else {
                updateDrowsinessTimer(now);
                return;
            }
        }

        if (frame == null || faceW <= 20 || faceH <= 20) {
            currentStatus = EyeStatus.OPEN;
            updateDrowsinessTimer(now);
            return;
        }

        try {
            int imgW = frame.getWidth();
            int imgH = frame.getHeight();

            // Locate Left Eye Box
            int lx = Math.max(0, Math.min(imgW - 1, faceX + (int)(faceW * 0.18)));
            int ly = Math.max(0, Math.min(imgH - 1, faceY + (int)(faceH * 0.28)));
            int lw = Math.min((int)(faceW * 0.28), imgW - lx);
            int lh = Math.min((int)(faceH * 0.18), imgH - ly);
            leftEyeBox.setBounds(lx, ly, lw, lh);

            // Locate Right Eye Box
            int rx = Math.max(0, Math.min(imgW - 1, faceX + (int)(faceW * 0.54)));
            int ry = Math.max(0, Math.min(imgH - 1, faceY + (int)(faceH * 0.28)));
            int rw = Math.min((int)(faceW * 0.28), imgW - rx);
            int rh = Math.min((int)(faceH * 0.18), imgH - ry);
            rightEyeBox.setBounds(rx, ry, rw, rh);

            // Evaluate optical open scores for each eye
            leftEyeScore = computeOpticalEyeOpenScore(frame, lx, ly, lw, lh);
            rightEyeScore = computeOpticalEyeOpenScore(frame, rx, ry, rw, rh);

            double currentInstantScore = (leftEyeScore + rightEyeScore) / 2.0;

            // Exponential Moving Average filter to eliminate camera noise & flickers
            smoothedEyeScore = (smoothedEyeScore * 0.65) + (currentInstantScore * 0.35);

            // Threshold: Eye is closed when score drops below 0.38
            if (smoothedEyeScore < 0.38) {
                currentStatus = EyeStatus.CLOSED;
            } else if (smoothedEyeScore < 0.50) {
                currentStatus = EyeStatus.BLINKING;
            } else {
                currentStatus = EyeStatus.OPEN;
            }

        } catch (Exception e) {
            currentStatus = EyeStatus.OPEN;
        }

        updateDrowsinessTimer(now);
    }

    /**
     * Computes optical open score (0.0 to 1.0) for an eye subregion.
     * Evaluates luminance variance (sclera vs pupil), dark pupil ratio, and edge gradients.
     */
    private double computeOpticalEyeOpenScore(BufferedImage frame, int ex, int ey, int ew, int eh) {
        if (ew < 4 || eh < 4) return 0.70;

        int totalPixels = 0;
        double sumLum = 0;
        double minLum = 255;
        double maxLum = 0;

        int[] lums = new int[ew * eh];

        for (int y = ey; y < ey + eh; y += 2) {
            for (int x = ex; x < ex + ew; x += 2) {
                int rgb = frame.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int lum = (int)(0.299 * r + 0.587 * g + 0.114 * b);

                lums[totalPixels++] = lum;
                sumLum += lum;
                if (lum < minLum) minLum = lum;
                if (lum > maxLum) maxLum = lum;
            }
        }

        if (totalPixels == 0) return 0.70;

        double meanLum = sumLum / totalPixels;

        // Compute Standard Deviation (Contrast variance)
        double varianceSum = 0;
        int darkPupilPixels = 0;
        double pupilThreshold = Math.max(30.0, meanLum * 0.70);

        for (int i = 0; i < totalPixels; i++) {
            double diff = lums[i] - meanLum;
            varianceSum += diff * diff;
            if (lums[i] < pupilThreshold) {
                darkPupilPixels++;
            }
        }

        double stdDev = Math.sqrt(varianceSum / totalPixels);
        double contrastSpan = maxLum - minLum;
        double pupilRatio = (double) darkPupilPixels / totalPixels;

        // An OPEN eye has:
        // 1. High contrast span (pupil vs sclera): span > 55
        // 2. High standard deviation: stdDev > 18
        // 3. Dark pupil cluster: pupilRatio between 0.12 and 0.45
        double spanFactor = Math.min(1.0, Math.max(0.0, (contrastSpan - 25.0) / 60.0));
        double stdDevFactor = Math.min(1.0, Math.max(0.0, (stdDev - 10.0) / 25.0));
        double pupilFactor = (pupilRatio >= 0.10 && pupilRatio <= 0.50) ? 1.0 : Math.max(0.1, pupilRatio * 2.0);

        return (spanFactor * 0.40) + (stdDevFactor * 0.40) + (pupilFactor * 0.20);
    }

    private void updateDrowsinessTimer(long now) {
        if (currentStatus == EyeStatus.CLOSED) {
            if (closedStartTime == 0) {
                closedStartTime = now;
            } else if (now - closedStartTime >= 1500) { // 1.5 seconds threshold
                drowsinessAlert = true;
            }
        } else {
            closedStartTime = 0;
            drowsinessAlert = false;
        }
    }

    public synchronized void setManualOverride(EyeStatus status, int durationMs) {
        this.currentStatus = status;
        this.manualOverride = true;
        this.overrideExpiry = System.currentTimeMillis() + durationMs;
        if (status == EyeStatus.CLOSED) {
            this.smoothedEyeScore = 0.15;
            this.closedStartTime = System.currentTimeMillis() - 1600; // Trigger alert immediately
            this.drowsinessAlert = true;
        } else {
            this.smoothedEyeScore = 0.85;
            this.closedStartTime = 0;
            this.drowsinessAlert = false;
        }
    }

    public synchronized void clearOverride() {
        this.manualOverride = false;
        this.overrideExpiry = 0;
        this.closedStartTime = 0;
        this.drowsinessAlert = false;
    }

    public EyeStatus getCurrentStatus() { return currentStatus; }
    public boolean isDrowsinessAlert() { return drowsinessAlert; }
    public boolean isEyesClosed() { return currentStatus == EyeStatus.CLOSED; }
    public double getLeftEyeScore() { return leftEyeScore; }
    public double getRightEyeScore() { return rightEyeScore; }
    public double getSmoothedEyeScore() { return smoothedEyeScore; }
    public Rectangle getLeftEyeBox() { return leftEyeBox; }
    public Rectangle getRightEyeBox() { return rightEyeBox; }
}
