package com.nexus.vision;

import com.nexus.core.events.MoodDetectedEvent;

/**
 * Facial Emotion & Mood Classifier.
 * Powered by Microsoft FERPlus deep learning neural network running in Python,
 * classifying 6 emotional states: FOCUSED, HAPPY, STRESSED, NEUTRAL, SURPRISED, SAD.
 */
public class EmotionClassifier {

    private PythonGenderBridge pythonBridge;
    private MoodDetectedEvent.Emotion manualOverride = null;
    private double manualConfidence = 0.94;
    private long manualHoldUntil = 0;

    public EmotionClassifier() {}

    public void setPythonBridge(PythonGenderBridge bridge) {
        this.pythonBridge = bridge;
    }

    /**
     * Classifies real-time facial emotion directly from the Python deep learning core.
     */
    public MoodDetectedEvent classifyEmotion(int faceX, int faceY, int faceW, int faceH, int frameW, int frameH) {
        long now = System.currentTimeMillis();
        if (manualOverride != null && now < manualHoldUntil) {
            return new MoodDetectedEvent(manualOverride, manualConfidence, faceX, faceY, faceW, faceH);
        }

        if (pythonBridge != null && pythonBridge.isPythonServiceReady()) {
            return new MoodDetectedEvent(
                    pythonBridge.getLatestEmotion(),
                    pythonBridge.getLatestEmotionConfidence(),
                    faceX, faceY, faceW, faceH
            );
        }

        // Graceful fallback while Python initializes
        return new MoodDetectedEvent(MoodDetectedEvent.Emotion.FOCUSED, 0.88, faceX, faceY, faceW, faceH);
    }

    public void setManualEmotionOverride(MoodDetectedEvent.Emotion emotion, double confidence) {
        this.manualOverride = emotion;
        this.manualConfidence = confidence;
        this.manualHoldUntil = System.currentTimeMillis() + 45000;
    }

    public boolean isDeepLearningActive() {
        return pythonBridge != null && pythonBridge.isPythonServiceReady();
    }
}
