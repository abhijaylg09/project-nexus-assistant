package com.nexus.core.events;

public class MoodDetectedEvent {
    public enum Emotion {
        NEUTRAL, HAPPY, SURPRISED, SAD, STRESSED, FOCUSED
    }

    private final Emotion emotion;
    private final double confidence;
    private final int faceX;
    private final int faceY;
    private final int faceWidth;
    private final int faceHeight;
    private final long timestamp;

    public MoodDetectedEvent(Emotion emotion, double confidence, int x, int y, int w, int h) {
        this.emotion = emotion;
        this.confidence = confidence;
        this.faceX = x;
        this.faceY = y;
        this.faceWidth = w;
        this.faceHeight = h;
        this.timestamp = System.currentTimeMillis();
    }

    public Emotion getEmotion() { return emotion; }
    public double getConfidence() { return confidence; }
    public int getFaceX() { return faceX; }
    public int getFaceY() { return faceY; }
    public int getFaceWidth() { return faceWidth; }
    public int getFaceHeight() { return faceHeight; }
    public long getTimestamp() { return timestamp; }

    public String getFormattedConfidence() {
        return String.format("%.0f%%", confidence * 100);
    }
}
