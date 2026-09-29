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
 * Features hybrid grammar weighting (domain commands + free-form dictation),
 * acoustic readiness signaling, and custom silence timeout optimization.
 */
public class WindowsSpeechRecognizer {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isListening = new AtomicBoolean(false);
    private Process activeProcess;

    public void listenAsync(Consumer<String> onTranscript, Runnable onStart, Runnable onReady, Runnable onEnd) {
        if (isListening.get()) return;
        isListening.set(true);

        executor.submit(() -> {
            try {
                if (onStart != null) onStart.run();

                String psScript = """
                    Add-Type -AssemblyName System.Speech;
                    $rec = $null;
                    try {
                        # Target en-US recognizer for optimal English dictation
                        $recInfo = [System.Speech.Recognition.SpeechRecognitionEngine]::InstalledRecognizers() | Where-Object { $_.Culture.Name -eq 'en-US' } | Select-Object -First 1;
                        if ($recInfo) {
                            $rec = New-Object System.Speech.Recognition.SpeechRecognitionEngine($recInfo.Id);
                        } else {
                            $rec = New-Object System.Speech.Recognition.SpeechRecognitionEngine;
                        }

                        $rec.SetInputToDefaultAudioDevice();

                        # 1. Domain Technical & Conversational Grammar for High Accuracy
                        $choices = New-Object System.Speech.Recognition.Choices;
                        $terms = @(
                            'hello', 'hi nexus', 'hello nexus', 'who created you', 'who made you',
                            'explain quicksort', 'how does quicksort work', 'explain mergesort',
                            'what is binary search', 'how does hashmap work', 'what is an api',
                            'what is a rest api', 'what are virtual threads', 'explain oop',
                            'what are the pillars of oop', 'explain transformers', 'how does attention work',
                            'what is a neural network', 'explain backpropagation', 'sql vs nosql',
                            'python vs java', 'what is recursion', 'what is dynamic programming',
                            'process vs thread', 'what is a deadlock', 'merge vs rebase',
                            'introduce yourself', 'what can you do', 'summarize', 'confirm',
                            'stop', 'mute', 'thank you', 'thanks', 'help', 'good morning', 'good evening',
                            'how are you', 'solve math equation', 'show me code', 'explain this image'
                        );
                        foreach ($t in $terms) { $choices.Add($t); }

                        $gb = New-Object System.Speech.Recognition.GrammarBuilder($choices);
                        $cmdGrammar = New-Object System.Speech.Recognition.Grammar($gb);
                        $cmdGrammar.Name = 'Commands';
                        $cmdGrammar.Weight = 1.0;
                        $rec.LoadGrammar($cmdGrammar);

                        # 2. General Dictation Grammar for Arbitrary Inquiries
                        $dictGrammar = New-Object System.Speech.Recognition.DictationGrammar;
                        $dictGrammar.Name = 'Dictation';
                        $dictGrammar.Weight = 0.8;
                        $rec.LoadGrammar($dictGrammar);

                        # Timeouts: Give user 8 seconds to start speaking, 15 seconds max speech
                        $rec.InitialSilenceTimeout = [TimeSpan]::FromSeconds(8);
                        $rec.BabbleTimeout = [TimeSpan]::FromSeconds(15);
                        $rec.EndSilenceTimeout = [TimeSpan]::FromMilliseconds(1200);

                        # Signal that microphone is open and ready to capture
                        [Console]::Beep(1000, 120);
                        Write-Output "EVENT:READY";

                        $res = $rec.Recognize([TimeSpan]::FromSeconds(12));
                        if ($res -and $res.Text) {
                            Write-Output "RESULT:$($res.Text)";
                        } else {
                            Write-Output "RESULT:NONE";
                        }
                    } catch {
                        Write-Output "ERR:$($_.Exception.Message)";
                        Write-Output "RESULT:NONE";
                    } finally {
                        if ($rec) { $rec.Dispose(); }
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
                        if (line.equals("EVENT:READY")) {
                            if (onReady != null) onReady.run();
                        } else if (line.startsWith("RESULT:")) {
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
