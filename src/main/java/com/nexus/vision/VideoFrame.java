package com.nexus.vision;

import javafx.scene.image.Image;

public class VideoFrame {
    private final Image fxImage;
    private final int width;
    private final int height;
    private final long timestamp;

    public VideoFrame(Image fxImage, int width, int height) {
        this.fxImage = fxImage;
        this.width = width;
        this.height = height;
        this.timestamp = System.currentTimeMillis();
    }

    public Image getFxImage() { return fxImage; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getTimestamp() { return timestamp; }
}
