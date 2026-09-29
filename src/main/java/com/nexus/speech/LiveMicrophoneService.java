package com.nexus.speech;

import com.nexus.core.MultimodalEventBus;

import javax.sound.sampled.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Real-time Hardware Microphone Capture Service using javax.sound.sampled.
 * Continuously measures live voice RMS energy and detects speech bursts.
 */
public class LiveMicrophoneService {

    private final AtomicBoolean recording = new AtomicBoolean(false);
    private TargetDataLine line;
    private Thread captureThread;

    private double currentVoiceLevel = 0.05; // 0.0 to 1.0
    private long lastSpeechTime = 0;
    private Consumer<byte[]> audioChunkConsumer;

    public LiveMicrophoneService() {}

    public synchronized void startCapture(Consumer<byte[]> onChunkReceived) {
        if (recording.get()) return;
        this.audioChunkConsumer = onChunkReceived;

        try {
            AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("[LiveMicrophone] Notice: 16kHz TargetDataLine not supported by current soundcard.");
                return;
            }

            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format, 4096);
            line.start();
            recording.set(true);

            captureThread = new Thread(this::captureLoop, "NexusLiveMicThread");
            captureThread.setDaemon(true);
            captureThread.start();
            System.out.println("[LiveMicrophone] Live microphone listening initiated (16kHz 16-bit PCM).");

        } catch (Exception e) {
            System.err.println("[LiveMicrophone] Microphone initialization note: " + e.getMessage());
            recording.set(false);
        }
    }

    public synchronized void stopCapture() {
        recording.set(false);
        if (line != null) {
            try {
                line.stop();
                line.close();
            } catch (Exception e) {
                // Ignore closing error
            }
            line = null;
        }
        System.out.println("[LiveMicrophone] Live microphone stopped.");
    }

    private void captureLoop() {
        byte[] buffer = new byte[1024];

        while (recording.get() && line != null) {
            int bytesRead = line.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                // Compute RMS energy level
                double sum = 0;
                for (int i = 0; i < bytesRead - 1; i += 2) {
                    short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                    sum += sample * sample;
                }
                double numSamples = bytesRead / 2.0;
                double rms = Math.sqrt(sum / numSamples);

                // Scale RMS: quiet room is ~50-200, normal speaking is ~1500-12000
                double normalized = Math.min(1.0, Math.max(0.04, rms / 8000.0));
                currentVoiceLevel = (currentVoiceLevel * 0.6) + (normalized * 0.4);

                if (currentVoiceLevel > 0.18) {
                    lastSpeechTime = System.currentTimeMillis();
                }

                if (audioChunkConsumer != null) {
                    audioChunkConsumer.accept(buffer);
                }
            }
        }
    }

    public double getCurrentVoiceLevel() {
        return currentVoiceLevel;
    }

    public boolean isSpeakingNow() {
        return (System.currentTimeMillis() - lastSpeechTime) < 500;
    }

    public boolean isRecording() {
        return recording.get();
    }
}
