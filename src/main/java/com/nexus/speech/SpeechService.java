package com.nexus.speech;

import com.nexus.core.MultimodalEventBus;
import com.nexus.core.events.UserInputEvent;
import java.util.function.Consumer;

/**
 * Centralized Speech Service orchestrating Wake Word Detection,
 * Live Microphone capture, Vosk STT, and Piper/SAPI TTS synthesis.
 */
public class SpeechService {

    private final WakeWordDetector wakeWordDetector;
    private final VoskSttEngine sttEngine;
    private final PiperTtsEngine ttsEngine;
    private final LiveMicrophoneService liveMicService;
    private final WindowsSpeechRecognizer windowsRecognizer;
    private final MultimodalEventBus eventBus;

    public SpeechService() {
        this.wakeWordDetector = new WakeWordDetector();
        this.sttEngine = new VoskSttEngine();
        this.ttsEngine = new PiperTtsEngine();
        this.liveMicService = new LiveMicrophoneService();
        this.windowsRecognizer = new WindowsSpeechRecognizer();
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

    public void listenToVoiceChatAsync(Consumer<String> onTranscript, Runnable onStart, Runnable onReady, Runnable onEnd) {
        windowsRecognizer.listenAsync(onTranscript, onStart, onReady, onEnd);
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
    public WindowsSpeechRecognizer getWindowsRecognizer() { return windowsRecognizer; }
    public void setVoiceLanguageMode(WindowsSpeechRecognizer.LanguageMode mode) { windowsRecognizer.setLanguageMode(mode); }
    public WindowsSpeechRecognizer.LanguageMode getVoiceLanguageMode() { return windowsRecognizer.getLanguageMode(); }
}
