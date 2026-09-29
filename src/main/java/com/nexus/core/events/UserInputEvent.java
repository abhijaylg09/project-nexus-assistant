package com.nexus.core.events;

public class UserInputEvent {
    public enum InputSource { TEXT, SPEECH, GESTURE_TRIGGER }

    private final String text;
    private final InputSource source;
    private final long timestamp;

    public UserInputEvent(String text, InputSource source) {
        this.text = text;
        this.source = source;
        this.timestamp = System.currentTimeMillis();
    }

    public String getText() { return text; }
    public InputSource getSource() { return source; }
    public long getTimestamp() { return timestamp; }
}
