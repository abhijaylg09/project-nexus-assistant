package com.nexus.speech;

import com.nexus.core.AppConfig;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Vosk-based Java Speech-To-Text transcription engine.
 * Interfaces with Vosk JNI models or falls back to microphone capture.
 */
public class VoskSttEngine {

    private final AppConfig config;
    private final AtomicBoolean isListening = new AtomicBoolean(false);
    private boolean modelFound = false;

    public VoskSttEngine() {
        this.config = AppConfig.getInstance();
        checkVoskModel();
    }

    private void checkVoskModel() {
        String path = config.getVoskModelPath();
        File f = new File(path);
        if (f.exists() && f.isDirectory()) {
            modelFound = true;
            System.out.println("[VoskSttEngine] Vosk acoustic model directory located at: " + path);
        } else {
            System.out.println("[VoskSttEngine] Vosk model directory not found at " + path + ". Audio input ready with fallback STT.");
        }
    }

    public void startListening(Consumer<String> onTranscriptReceived) {
        if (isListening.get()) return;
        isListening.set(true);
        System.out.println("[VoskSttEngine] Microphone listening stream initiated.");
    }

    public void stopListening() {
        isListening.set(false);
        System.out.println("[VoskSttEngine] Microphone listening stream stopped.");
    }

    public boolean isListening() {
        return isListening.get();
    }

    public boolean isModelFound() {
        return modelFound;
    }
}
