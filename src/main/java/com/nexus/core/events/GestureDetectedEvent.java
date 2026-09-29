package com.nexus.core.events;

public class GestureDetectedEvent {
    public enum Gesture {
        NONE("No Gesture", "Idle"),
        THUMBS_UP("Thumbs Up", "Quick Acknowledge / Confirm"),
        STOP_PALM("Open Palm", "Mute / Pause Response"),
        PEACE("Victory / Peace", "Summarize Last Topic"),
        POINT_UP("Pointing Up", "Repeat Wake Command");

        private final String displayName;
        private final String actionDescription;

        Gesture(String displayName, String actionDescription) {
            this.displayName = displayName;
            this.actionDescription = actionDescription;
        }

        public String getDisplayName() { return displayName; }
        public String getActionDescription() { return actionDescription; }
    }

    private final Gesture gesture;
    private final double confidence;
    private final long timestamp;

    public GestureDetectedEvent(Gesture gesture, double confidence) {
        this.gesture = gesture;
        this.confidence = confidence;
        this.timestamp = System.currentTimeMillis();
    }

    public Gesture getGesture() { return gesture; }
    public double getConfidence() { return confidence; }
    public long getTimestamp() { return timestamp; }
}
