package com.nexus.speech;

import com.nexus.core.AppConfig;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Text-to-Speech engine supporting Piper neural voice synthesis
 * and Windows Native SAPI speech synthesis with instant cancellation.
 */
public class PiperTtsEngine {

    private final AppConfig config;
    private final ExecutorService ttsExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "nexus-tts-worker");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean speaking = new AtomicBoolean(false);
    private volatile Process currentProcess = null;
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
     * Synthesizes and speaks text asynchronously with natural text sanitization.
     */
    public void speakAsync(String text, Runnable onComplete) {
        if (text == null || text.isBlank()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        // Cancel any previous speaking first
        stopSpeaking();

        final String spokenText = sanitizeForSpeech(text);
        if (spokenText.isBlank()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        ttsExecutor.submit(() -> {
            try {
                speaking.set(true);

                if (piperAvailable) {
                    executePiperProcess(spokenText);
                } else {
                    executeNativeVoiceSynthesis(spokenText);
                }
            } catch (Exception e) {
                System.err.println("[PiperTtsEngine] TTS execution error: " + e.getMessage());
            } finally {
                speaking.set(false);
                currentProcess = null;
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    /**
     * Immediately stops and cancels any active voice speech playback.
     */
    public void stopSpeaking() {
        speaking.set(false);
        Process p = currentProcess;
        if (p != null && p.isAlive()) {
            try {
                p.destroyForcibly();
            } catch (Exception ignored) {}
        }
        currentProcess = null;
    }

    /**
     * Sanitizes Markdown, code blocks, URLs, and formatting so the voice sounds natural like ChatGPT / Jarvis.
     */
    public static String sanitizeForSpeech(String markdown) {
        if (markdown == null) return "";

        String text = markdown;

        // Replace multi-line code blocks
        text = text.replaceAll("(?s)```[a-zA-Z]*\\n.*?```", " Here is the code solution. ");
        // Replace single-line code blocks
        text = text.replaceAll("`[^`]+`", " code ");
        // Remove markdown images and links: [text](url) -> text
        text = text.replaceAll("!\\[[^\\]]*\\]\\([^\\)]*\\)", "");
        text = text.replaceAll("\\[([^\\]]+)\\]\\([^\\)]*\\)", "$1");
        // Remove markdown headers: ### Header -> Header.
        text = text.replaceAll("(?m)^#{1,6}\\s*", "");
        // Remove bold and italics: **bold** or *italic*
        text = text.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        text = text.replaceAll("\\*([^*]+)\\*", "$1");
        text = text.replaceAll("__([^_]+)__", "$1");
        text = text.replaceAll("_([^_]+)_", "$1");
        // Remove bullet markers
        text = text.replaceAll("(?m)^\\s*[-*•]\\s*", "");
        text = text.replaceAll("(?m)^\\s*\\d+\\.\\s*", "");
        // Remove blockquotes
        text = text.replaceAll("(?m)^>\\s*", "");
        // Collapse multiple whitespace / newlines
        text = text.replaceAll("\\s+", " ").trim();

        // Limit length to ~500 chars for responsive conversational talkback
        if (text.length() > 500) {
            int lastPeriod = text.lastIndexOf('.', 490);
            if (lastPeriod > 200) {
                text = text.substring(0, lastPeriod + 1);
            } else {
                text = text.substring(0, 490) + "...";
            }
        }

        return text;
    }

    private void executePiperProcess(String text) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    config.getPiperExecutable(),
                    "--model", config.getPiperModelPath(),
                    "--output_raw"
            );
            currentProcess = pb.start();
            currentProcess.getOutputStream().write(text.getBytes(StandardCharsets.UTF_8));
            currentProcess.getOutputStream().flush();
            currentProcess.getOutputStream().close();
            currentProcess.waitFor();
        } catch (Exception e) {
            System.err.println("[PiperTtsEngine] Piper process failed, falling back to OS native TTS: " + e.getMessage());
            executeNativeVoiceSynthesis(text);
        }
    }

    private void executeNativeVoiceSynthesis(String text) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            executeMacSay(text);
        } else if (os.contains("win")) {
            executeWindowsSapi(text);
        } else {
            executeLinuxSpeech(text);
        }
    }

    private void executeMacSay(String text) {
        try {
            String safeText = text.replace("\"", "\\\"").trim();
            ProcessBuilder pb = new ProcessBuilder("say", safeText);
            pb.redirectErrorStream(true);
            currentProcess = pb.start();
            currentProcess.waitFor(20, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("[PiperTtsEngine] macOS 'say' TTS error: " + e.getMessage());
        }
    }

    private void executeLinuxSpeech(String text) {
        try {
            String safeText = text.replace("\"", "\\\"").trim();
            ProcessBuilder pb = new ProcessBuilder("spd-say", safeText);
            pb.redirectErrorStream(true);
            currentProcess = pb.start();
            currentProcess.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("[PiperTtsEngine] Linux speech synthesis notice: " + e.getMessage());
        }
    }

    private void executeWindowsSapi(String text) {
        File tempScript = null;
        try {
            // Write a tiny fast VBScript with SAPI.SpVoice for zero-latency speech
            tempScript = File.createTempFile("nexus_sapi_", ".vbs");
            String safeText = text.replace("\"", "\"\"");

            String vbsCode = "Set voice = CreateObject(\"SAPI.SpVoice\")\n"
                    + "voice.Rate = 1\n"
                    + "voice.Speak \"" + safeText + "\"\n";

            try (FileWriter writer = new FileWriter(tempScript, StandardCharsets.UTF_8)) {
                writer.write(vbsCode);
            }

            ProcessBuilder pb = new ProcessBuilder("cscript", "//nologo", tempScript.getAbsolutePath());
            pb.redirectErrorStream(true);
            currentProcess = pb.start();
            currentProcess.waitFor();

        } catch (Exception e) {
            System.err.println("[PiperTtsEngine] SAPI synthesis error: " + e.getMessage());
        } finally {
            if (tempScript != null && tempScript.exists()) {
                try { tempScript.delete(); } catch (Exception ignored) {}
            }
        }
    }

    public boolean isSpeaking() {
        return speaking.get();
    }

    /**
     * Dynamic cadence level for live visualizer animations during TTS speech.
     */
    public double getCadenceAudioLevel() {
        if (!speaking.get()) return 0.05;
        double t = (System.currentTimeMillis() % 1000) / 1000.0;
        return 0.40 + 0.48 * Math.abs(Math.sin(t * Math.PI * 3.5));
    }
}
