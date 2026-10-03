package com.nexus.core.events;

public class MoodDetectedEvent {
    public enum Emotion {
        NEUTRAL, HAPPY, SURPRISED, SAD, STRESSED, FOCUSED
    }

    public enum Gender {
        MALE, FEMALE
    }

    private final Emotion emotion;
    private final double confidence;
    private final int faceX;
    private final int faceY;
    private final int faceWidth;
    private final int faceHeight;
    private final long timestamp;

    // Advanced Perceptual Attributes
    private final Gender gender;
    private final double genderConfidence;
    private final boolean eyesClosed;
    private final boolean drowsinessAlert;
    private final String recognizedIdentity;
    private final String identityRole;
    private final double motionLevel;

    public MoodDetectedEvent(Emotion emotion, double confidence, int x, int y, int w, int h,
                             Gender gender, double genderConfidence,
                             boolean eyesClosed, boolean drowsinessAlert,
                             String recognizedIdentity, String identityRole,
                             double motionLevel) {
        this.emotion = emotion;
        this.confidence = confidence;
        this.faceX = x;
        this.faceY = y;
        this.faceWidth = w;
        this.faceHeight = h;
        this.gender = gender;
        this.genderConfidence = genderConfidence;
        this.eyesClosed = eyesClosed;
        this.drowsinessAlert = drowsinessAlert;
        this.recognizedIdentity = recognizedIdentity;
        this.identityRole = identityRole;
        this.motionLevel = motionLevel;
        this.timestamp = System.currentTimeMillis();
    }

    // Backwards-compatible constructor
    public MoodDetectedEvent(Emotion emotion, double confidence, int x, int y, int w, int h) {
        this(emotion, confidence, x, y, w, h, Gender.FEMALE, 0.95, false, false, "Bhadra G. S.", "Project Ideation & Architecture", 0.05);
    }

    public Emotion getEmotion() { return emotion; }
    public double getConfidence() { return confidence; }
    public int getFaceX() { return faceX; }
    public int getFaceY() { return faceY; }
    public int getFaceWidth() { return faceWidth; }
    public int getFaceHeight() { return faceHeight; }
    public long getTimestamp() { return timestamp; }

    public Gender getGender() { return gender; }
    public double getGenderConfidence() { return genderConfidence; }
    public boolean isEyesClosed() { return eyesClosed; }
    public boolean isDrowsinessAlert() { return drowsinessAlert; }
    public String getRecognizedIdentity() { return recognizedIdentity; }
    public String getIdentityRole() { return identityRole; }
    public double getMotionLevel() { return motionLevel; }

    public String getFormattedConfidence() {
        return String.format("%.0f%%", confidence * 100);
    }

    public String getFormattedGenderConfidence() {
        return String.format("%.0f%%", genderConfidence * 100);
    }
}
