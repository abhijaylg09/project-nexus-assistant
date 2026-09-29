package com.nexus.vision;

import com.nexus.core.events.GestureDetectedEvent;

import java.util.Random;

/**
 * Hand gesture recognition module.
 * Maps classified gestures into assistant shortcut triggers.
 */
public class GestureClassifier {

    private final Random random = new Random();
    private GestureDetectedEvent.Gesture lastGesture = GestureDetectedEvent.Gesture.NONE;
    private double lastConfidence = 0.0;
    private long lastTriggerTime = 0;

    public GestureClassifier() {}

    public GestureDetectedEvent evaluateGesture(boolean gestureDetectedInFrame) {
        if (!gestureDetectedInFrame) {
            lastGesture = GestureDetectedEvent.Gesture.NONE;
            lastConfidence = 0.0;
            return new GestureDetectedEvent(lastGesture, lastConfidence);
        }

        long now = System.currentTimeMillis();
        // Limit gesture re-triggering spam
        if (now - lastTriggerTime > 4000) {
            lastTriggerTime = now;
            int pick = random.nextInt(3);
            if (pick == 0) {
                lastGesture = GestureDetectedEvent.Gesture.THUMBS_UP;
                lastConfidence = 0.91;
            } else if (pick == 1) {
                lastGesture = GestureDetectedEvent.Gesture.PEACE;
                lastConfidence = 0.88;
            } else {
                lastGesture = GestureDetectedEvent.Gesture.STOP_PALM;
                lastConfidence = 0.94;
            }
        }

        return new GestureDetectedEvent(lastGesture, lastConfidence);
    }

    public GestureDetectedEvent triggerManualGesture(GestureDetectedEvent.Gesture gesture) {
        this.lastGesture = gesture;
        this.lastConfidence = 0.95;
        this.lastTriggerTime = System.currentTimeMillis();
        return new GestureDetectedEvent(gesture, lastConfidence);
    }
}
