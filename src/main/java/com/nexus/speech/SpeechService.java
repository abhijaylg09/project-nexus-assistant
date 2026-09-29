package com.nexus.speech;

import com.nexus.core.MultimodalEventBus;
import com.nexus.core.events.UserInputEvent;

/**
 * Centralized Speech Service orchestrating Wake Word Detection,
 * Vosk STT, and Piper/SAPI TTS synthesis.
 */
public class SpeechService {

    private final WakeWordDetector wakeWordDetector;
    private final VoskSttEngine sttEngine;
    private final PiperTtsEngine ttsEngine;
    private final MultimodalEventBus eventBus;

    public SpeechService() {
        this.wakeWordDetector = new WakeWordDetector();
        this.sttEngine = new VoskSttEngine();
        this.ttsEngine = new PiperTtsEngine();
        this.eventBus = MultimodalEventBus.getInstance();
    }

    public void start() {
        wakeWordDetector.startListening();
        sttEngine.startListening(transcript -> {
            if (wakeWordDetector.checkTextForWakeWord(transcript)) {
                System.out.println("[SpeechService] Wake word matched in: " + transcript);
                eventBus.publish(new UserInputEvent(transcript, UserInputEvent.InputSource.SPEECH));
            }
        });
    }

    public void stop() {
        wakeWordDetector.stopListening();
        sttEngine.stopListening();
    }

    public void speak(String text, Runnable onComplete) {
        ttsEngine.speakAsync(text, onComplete);
    }

    public boolean isSpeaking() {
        return ttsEngine.isSpeaking();
    }

    public boolean isListening() {
        return sttEngine.isListening();
    }

    public WakeWordDetector getWakeWordDetector() { return wakeWordDetector; }
    public VoskSttEngine getSttEngine() { return sttEngine; }
    public PiperTtsEngine getTtsEngine() { return ttsEngine; }
}
