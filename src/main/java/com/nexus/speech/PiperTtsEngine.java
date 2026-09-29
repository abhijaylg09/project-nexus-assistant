package com.nexus.speech;

import com.nexus.core.AppConfig;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Text-to-Speech engine supporting Piper neural voice synthesis
 * and Windows SAPI speech synthesis.
 */
public class PiperTtsEngine {

    private final AppConfig config;
    private final ExecutorService ttsExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean speaking = new AtomicBoolean(false);
    private boolean piperAvailable = false;

    public PiperTtsEngine() {
        this.config = AppConfig.getInstance();
        checkPiperBinary();
    }

    private void checkPiperBinary() {
        File piperBin = new File(config.getPiperExecutable());
        if (piperBin.exists()) {
            piperAvailable = true;
            System.out.println("[PiperTtsEngine] Piper binary found at: " + piperBin.getAbsolutePath());
        } else {
            System.out.println("[PiperTtsEngine] Piper binary not found at " + config.getPiperExecutable() + ". Using Windows Native SAPI voice synthesis.");
        }
    }

    /**
     * Synthesizes and speaks text asynchronously.
     */
    public void speakAsync(String text, Runnable onComplete) {
        if (text == null || text.isBlank()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        ttsExecutor.submit(() -> {
            try {
                speaking.set(true);

                if (piperAvailable) {
                    executePiperProcess(text);
                } else {
                    executeWindowsSapi(text);
                }
            } catch (Exception e) {
                System.err.println("[PiperTtsEngine] TTS execution error: " + e.getMessage());
            } finally {
                speaking.set(false);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    private void executePiperProcess(String text) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    config.getPiperExecutable(),
                    "--model", config.getPiperModelPath(),
                    "--output_raw"
            );
            Process process = pb.start();
            process.getOutputStream().write(text.getBytes());
            process.getOutputStream().flush();
            process.getOutputStream().close();
            process.waitFor();
        } catch (Exception e) {
            System.err.println("[PiperTtsEngine] Piper process failed, falling back to SAPI: " + e.getMessage());
            executeWindowsSapi(text);
        }
    }

    private void executeWindowsSapi(String text) {
        try {
            // Clean text for PowerShell command line
            String safeText = text.replace("\"", "\\\"").replace("'", " ").replace("\n", " ").trim();
            // Cap spoken length for responsiveness
            if (safeText.length() > 250) {
                safeText = safeText.substring(0, 250) + "...";
            }

            String psCommand = "Add-Type -AssemblyName System.Speech; $speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; $speak.Rate = 1; $speak.Speak(\"" + safeText + "\")";

            ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", psCommand);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();
        } catch (Exception e) {
            System.err.println("[PiperTtsEngine] SAPI synthesis error: " + e.getMessage());
        }
    }

    public boolean isSpeaking() {
        return speaking.get();
    }
}
