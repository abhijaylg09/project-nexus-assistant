package com.nexus.core.events;

import java.io.File;

public class UserInputEvent {
    public enum InputSource { TEXT, SPEECH, GESTURE_TRIGGER, IMAGE_INQUIRY }

    private final String text;
    private final InputSource source;
    private final File attachedImageFile;
    private final long timestamp;

    public UserInputEvent(String text, InputSource source) {
        this(text, source, null);
    }

    public UserInputEvent(String text, InputSource source, File attachedImageFile) {
        this.text = text;
        this.source = source;
        this.attachedImageFile = attachedImageFile;
        this.timestamp = System.currentTimeMillis();
    }

    public String getText() { return text; }
    public InputSource getSource() { return source; }
    public File getAttachedImageFile() { return attachedImageFile; }
    public boolean hasAttachedImage() { return attachedImageFile != null && attachedImageFile.exists(); }
    public long getTimestamp() { return timestamp; }
}
