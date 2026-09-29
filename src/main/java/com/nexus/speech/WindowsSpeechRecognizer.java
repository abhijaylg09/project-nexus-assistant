package com.nexus.speech;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Native Windows Speech Recognition Client using System.Speech SAPI.
 * Transcribes live speech from the default system microphone.
 */
public class WindowsSpeechRecognizer {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isListening = new AtomicBoolean(false);
    private Process activeProcess;

    public void listenAsync(Consumer<String> onTranscript, Runnable onStart, Runnable onEnd) {
        if (isListening.get()) return;
        isListening.set(true);

        executor.submit(() -> {
            try {
                if (onStart != null) onStart.run();

                String psScript = """
                    Add-Type -AssemblyName System.Speech;
                    $rec = New-Object System.Speech.Recognition.SpeechRecognitionEngine;
                    try {
                        $rec.SetInputToDefaultAudioDevice();
                        $rec.LoadGrammar((New-Object System.Speech.Recognition.DictationGrammar));
                        $res = $rec.Recognize([TimeSpan]::FromSeconds(8));
                        if ($res -and $res.Text) {
                            Write-Output "RESULT:$($res.Text)"
                        } else {
                            Write-Output "RESULT:NONE"
                        }
                    } catch {
                        Write-Output "RESULT:NONE"
                    } finally {
                        $rec.Dispose();
                    }
                """;

                ProcessBuilder pb = new ProcessBuilder(
                        "powershell", "-NoProfile", "-NonInteractive", "-Command", psScript
                );
                pb.redirectErrorStream(true);
                activeProcess = pb.start();

                String recognizedText = null;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(activeProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("RESULT:")) {
                            String result = line.substring("RESULT:".length()).trim();
                            if (!result.equalsIgnoreCase("NONE") && !result.isEmpty()) {
                                recognizedText = result;
                            }
                        }
                    }
                }
                activeProcess.waitFor();

                if (recognizedText != null && !recognizedText.isBlank()) {
                    if (onTranscript != null) {
                        onTranscript.accept(recognizedText);
                    }
                }

            } catch (Exception e) {
                System.err.println("[WindowsSpeechRecognizer] Voice recognition notice: " + e.getMessage());
            } finally {
                isListening.set(false);
                activeProcess = null;
                if (onEnd != null) onEnd.run();
            }
        });
    }

    public void cancel() {
        if (activeProcess != null && activeProcess.isAlive()) {
            activeProcess.destroyForcibly();
        }
        isListening.set(false);
    }

    public boolean isListening() {
        return isListening.get();
    }
}
