package com.nexus.vision;

import com.nexus.core.events.GestureDetectedEvent;

/**
 * Hand Gesture Classifier.
 * Powered by Python OpenCV contour convex hull analysis,
 * detecting THUMBS_UP (Confirm), STOP_PALM (Pause/Mute), PEACE (Summarize).
 */
public class GestureClassifier {

    private PythonGenderBridge pythonBridge;
    private GestureDetectedEvent.Gesture manualGesture = GestureDetectedEvent.Gesture.NONE;
    private double manualConfidence = 0.0;
    private long manualHoldUntil = 0;

    public GestureClassifier() {}

    public void setPythonBridge(PythonGenderBridge bridge) {
        this.pythonBridge = bridge;
    }

    public GestureDetectedEvent evaluateGesture(boolean gestureDetectedInFrame) {
        long now = System.currentTimeMillis();
        if (manualGesture != GestureDetectedEvent.Gesture.NONE && now < manualHoldUntil) {
            return new GestureDetectedEvent(manualGesture, manualConfidence);
        }

        if (pythonBridge != null && pythonBridge.isPythonServiceReady()) {
            GestureDetectedEvent.Gesture pyGest = pythonBridge.getLatestGesture();
            double conf = pythonBridge.getLatestGestureConfidence();
            return new GestureDetectedEvent(pyGest, conf);
        }

        return new GestureDetectedEvent(GestureDetectedEvent.Gesture.NONE, 0.0);
    }

    public GestureDetectedEvent triggerManualGesture(GestureDetectedEvent.Gesture gesture) {
        this.manualGesture = gesture;
        this.manualConfidence = 0.95;
        this.manualHoldUntil = System.currentTimeMillis() + 4000;
        return new GestureDetectedEvent(gesture, manualConfidence);
    }
}
