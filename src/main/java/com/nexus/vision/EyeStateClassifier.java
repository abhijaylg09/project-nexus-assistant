package com.nexus.vision;

/**
 * Classifies eye state (Open vs Closed) and monitors continuous closure for drowsiness detection.
 */
public class EyeStateClassifier {

    public enum EyeStatus { OPEN, BLINKING, CLOSED }

    private EyeStatus currentStatus = EyeStatus.OPEN;
    private long closedStartTime = 0;
    private boolean drowsinessAlert = false;
    private boolean manualOverride = false;
    private long overrideExpiry = 0;

    public synchronized void evaluateEyes(boolean eyesClosedDetected) {
        long now = System.currentTimeMillis();

        if (manualOverride) {
            if (now > overrideExpiry) {
                manualOverride = false;
            } else {
                updateDrowsinessTimer(now);
                return;
            }
        }

        if (eyesClosedDetected) {
            currentStatus = EyeStatus.CLOSED;
        } else {
            currentStatus = EyeStatus.OPEN;
        }

        updateDrowsinessTimer(now);
    }

    private void updateDrowsinessTimer(long now) {
        if (currentStatus == EyeStatus.CLOSED) {
            if (closedStartTime == 0) {
                closedStartTime = now;
            } else if (now - closedStartTime >= 1800) { // 1.8 seconds threshold
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
            this.closedStartTime = System.currentTimeMillis() - 2000; // Trigger alert immediately
            this.drowsinessAlert = true;
        } else {
            this.closedStartTime = 0;
            this.drowsinessAlert = false;
        }
    }

    public EyeStatus getCurrentStatus() { return currentStatus; }
    public boolean isDrowsinessAlert() { return drowsinessAlert; }
    public boolean isEyesClosed() { return currentStatus == EyeStatus.CLOSED; }
}
