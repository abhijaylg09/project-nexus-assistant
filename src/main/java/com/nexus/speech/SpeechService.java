package com.nexus.speech;

import com.nexus.core.MultimodalEventBus;
import com.nexus.core.events.UserInputEvent;

/**
 * Centralized Speech Service orchestrating Wake Word Detection,
 * Live Microphone capture, Vosk STT, and Piper/SAPI TTS synthesis.
 */
public class SpeechService {

    private final WakeWordDetector wakeWordDetector;
    private final VoskSttEngine sttEngine;
    private final PiperTtsEngine ttsEngine;
    private final LiveMicrophoneService liveMicService;
    private final MultimodalEventBus eventBus;

    public SpeechService() {
        this.wakeWordDetector = new WakeWordDetector();
        this.sttEngine = new VoskSttEngine();
        this.ttsEngine = new PiperTtsEngine();
        this.liveMicService = new LiveMicrophoneService();
        this.eventBus = MultimodalEventBus.getInstance();
    }

    public void start() {
        wakeWordDetector.startListening();
        liveMicService.startCapture(chunk -> {
            // Buffer can feed Vosk / acoustic models
        });
        sttEngine.startListening(transcript -> {
            if (wakeWordDetector.checkTextForWakeWord(transcript)) {
                System.out.println("[SpeechService] Wake word matched in: " + transcript);
                eventBus.publish(new UserInputEvent(transcript, UserInputEvent.InputSource.SPEECH));
            }
        });
    }

    public void stop() {
        wakeWordDetector.stopListening();
        liveMicService.stopCapture();
        sttEngine.stopListening();
    }

    public void speak(String text, Runnable onComplete) {
        ttsEngine.speakAsync(text, onComplete);
    }

    public double getLiveAudioLevel() {
        if (ttsEngine.isSpeaking()) {
            return 0.85; // High oscillation during TTS speech
        }
        return liveMicService.getCurrentVoiceLevel();
    }

    public boolean isSpeaking() {
        return ttsEngine.isSpeaking();
    }

    public boolean isListening() {
        return liveMicService.isRecording() || sttEngine.isListening();
    }

    public LiveMicrophoneService getLiveMicService() { return liveMicService; }
    public WakeWordDetector getWakeWordDetector() { return wakeWordDetector; }
    public VoskSttEngine getSttEngine() { return sttEngine; }
    public PiperTtsEngine getTtsEngine() { return ttsEngine; }
}
