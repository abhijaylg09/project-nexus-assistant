package com.nexus.speech;

import com.nexus.core.AppConfig;
import com.nexus.core.MultimodalEventBus;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wake word detection engine inspired by Porcupine Java SDK architecture.
 * Listens for keyword activations like "Nexus" or "Hey Nexus".
 */
public class WakeWordDetector {

    private final String keyword;
    private final AtomicBoolean listening = new AtomicBoolean(false);

    public WakeWordDetector() {
        this.keyword = AppConfig.getInstance().getWakeWord().toLowerCase();
    }

    public void startListening() {
        listening.set(true);
        System.out.println("[WakeWordDetector] Active. Listening for trigger phrase: \"" + keyword + "\"");
    }

    public void stopListening() {
        listening.set(false);
    }

    public boolean checkTextForWakeWord(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase().trim();
        return lower.startsWith(keyword) || lower.startsWith("hey " + keyword) || lower.contains(keyword);
    }

    public boolean isListening() {
        return listening.get();
    }
}
