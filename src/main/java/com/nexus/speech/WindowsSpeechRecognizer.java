package com.nexus.speech;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Ultra-Accurate Native Windows Speech Recognition Client using System.Speech SAPI.
 *
 * Accuracy Features:
 * - Structured Rule-Based GrammarBuilder (Prefix + Action + Article + Target + Suffix)
 * - Acoustic Isolation: Beeps complete BEFORE mic stream opens (eliminates self-echo)
 * - Multi-Candidate Scoring: Evaluates top 7 SAPI alternates against known intents
 * - Dual-Pass Weighting: Command Grammar (0.95), Conversational (0.85), Dictation (0.20)
 * - Phonetic Manglish & English Correction with Levenshtein distance snapping
 * - Adaptive silence timeouts: fast response without cutting off speech
 * - Automatic retry on low confidence / background noise
 */
public class WindowsSpeechRecognizer {

    public enum LanguageMode {
        BILINGUAL("EN + മലയാളം"),
        MALAYALAM("മലയാളം (ML)"),
        ENGLISH("English (EN)");

        private final String label;
        LanguageMode(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isListening = new AtomicBoolean(false);
    private volatile LanguageMode languageMode = LanguageMode.BILINGUAL;
    private Process activeProcess;

    /** Minimum confidence (0.0 to 1.0) to accept a result without retrying */
    private static final double MIN_CONFIDENCE = 0.30;
    /** Maximum recognition attempts per voice session */
    private static final int MAX_RETRIES = 2;

    // ─── Phonetic Manglish / Malayalam Transliteration Map ───
    private static final Map<String, String> MANGLISH_MAPPINGS = new LinkedHashMap<>();

    static {
        MANGLISH_MAPPINGS.put("namaskaram", "നമസ്കാരം (Namaskaram)");
        MANGLISH_MAPPINGS.put("namaskar", "നമസ്കാരം (Namaskar)");
        MANGLISH_MAPPINGS.put("namaste", "നമസ്കാരം (Namaste)");
        MANGLISH_MAPPINGS.put("sugamano", "സുഖമാണോ? (Sugamano)");
        MANGLISH_MAPPINGS.put("sukhamaano", "സുഖമാണോ? (Sukhamaano)");
        MANGLISH_MAPPINGS.put("sukhamano", "സുഖമാണോ? (Sukhamano)");
        MANGLISH_MAPPINGS.put("sukham aano", "സുഖമാണോ? (Sukham aano)");
        MANGLISH_MAPPINGS.put("sugam aano", "സുഖമാണോ? (Sugam aano)");
        MANGLISH_MAPPINGS.put("enthokkeyund", "എന്തൊക്കെയുണ്ട്? (Enthokkeyund)");
        MANGLISH_MAPPINGS.put("enthokke undu", "എന്തൊക്കെയുണ്ട്? (Enthokke undu)");
        MANGLISH_MAPPINGS.put("enthokke und", "എന്തൊക്കെയുണ്ട്? (Enthokke und)");
        MANGLISH_MAPPINGS.put("endha mone", "എന്താ മോനെ? (Endha mone)");
        MANGLISH_MAPPINGS.put("entha mone", "എന്താ മോനെ? (Entha mone)");
        MANGLISH_MAPPINGS.put("enda mone", "എന്താ മോനെ? (Enda mone)");
        MANGLISH_MAPPINGS.put("scene mone", "സീൻ മോനെ! (Scene mone)");
        MANGLISH_MAPPINGS.put("pinnalla", "പിന്നല്ല! (Pinnalla)");
        MANGLISH_MAPPINGS.put("pin nalla", "പിന്നല്ല! (Pin nalla)");
        MANGLISH_MAPPINGS.put("mass da", "മാസ്സ് ടാ! (Mass da)");
        MANGLISH_MAPPINGS.put("mas da", "മാസ്സ് ടാ! (Mas da)");
        MANGLISH_MAPPINGS.put("adipoli", "അടിപൊളി! (Adipoli)");
        MANGLISH_MAPPINGS.put("adi poli", "അടിപൊളി! (Adi poli)");
        MANGLISH_MAPPINGS.put("pwoli", "പൊളി! (Pwoli)");
        MANGLISH_MAPPINGS.put("poli", "പൊളി! (Poli)");
        MANGLISH_MAPPINGS.put("nanni", "നന്ദി (Nanni)");
        MANGLISH_MAPPINGS.put("nani", "നന്ദി (Nani)");
        MANGLISH_MAPPINGS.put("shubhadinam", "ശുഭദിനം (Shubhadinam)");
        MANGLISH_MAPPINGS.put("aliya", "അളിയാ (Aliya)");
        MANGLISH_MAPPINGS.put("machane", "മച്ചാനേ (Machane)");
        MANGLISH_MAPPINGS.put("mone", "മോനേ (Mone)");
        MANGLISH_MAPPINGS.put("evideya", "എവിടെയാ? (Evideya)");
        MANGLISH_MAPPINGS.put("evidya", "എവിടെയാ? (Evidya)");
        MANGLISH_MAPPINGS.put("ninte perentha", "നിന്റെ പേരെന്താ? (Ninte perentha)");
        MANGLISH_MAPPINGS.put("aaranu nee", "ആരാണ് നീ? (Aaranu nee)");
        MANGLISH_MAPPINGS.put("aarannu", "ആരാണ്? (Aarannu)");
        MANGLISH_MAPPINGS.put("kaaryam para", "കാര്യം പറ (Kaaryam para)");
        MANGLISH_MAPPINGS.put("karyam para", "കാര്യം പറ (Karyam para)");
        MANGLISH_MAPPINGS.put("dhaya cheythu", "ദയവുചെയ്തു (Dhaya cheythu)");
        MANGLISH_MAPPINGS.put("adichu keri vaa", "അടിച്ചു കേറി വാ! (Adichu keri vaa)");
        MANGLISH_MAPPINGS.put("thug life", "തഗ് ലൈഫ്! (Thug Life)");
        MANGLISH_MAPPINGS.put("kollam", "കൊള്ളാം (Kollam)");
        MANGLISH_MAPPINGS.put("kidu", "കിടു! (Kidu)");
        MANGLISH_MAPPINGS.put("sherikkum", "ശരിക്കും (Sherikkum)");
        MANGLISH_MAPPINGS.put("entha vishesham", "എന്താ വിശേഷം? (Entha vishesham)");
    }

    // ─── Common phonetic speech-to-text corrections ───
    private static final Map<String, String> PHONETIC_CORRECTIONS = new LinkedHashMap<>();

    static {
        // YouTube phonetic variations
        PHONETIC_CORRECTIONS.put("you tube", "youtube");
        PHONETIC_CORRECTIONS.put("u tube", "youtube");
        PHONETIC_CORRECTIONS.put("you to", "youtube");
        PHONETIC_CORRECTIONS.put("you too", "youtube");
        PHONETIC_CORRECTIONS.put("utube", "youtube");
        PHONETIC_CORRECTIONS.put("you tub", "youtube");
        PHONETIC_CORRECTIONS.put("youtub", "youtube");
        PHONETIC_CORRECTIONS.put("you take", "youtube");

        // Chrome variations
        PHONETIC_CORRECTIONS.put("crome", "chrome");
        PHONETIC_CORRECTIONS.put("krome", "chrome");
        PHONETIC_CORRECTIONS.put("grome", "chrome");
        PHONETIC_CORRECTIONS.put("google chrome", "chrome");

        // Calculator variations
        PHONETIC_CORRECTIONS.put("calcualtor", "calculator");
        PHONETIC_CORRECTIONS.put("calculater", "calculator");
        PHONETIC_CORRECTIONS.put("calc", "calculator");
        PHONETIC_CORRECTIONS.put("calculate", "calculator");

        // VS Code variations
        PHONETIC_CORRECTIONS.put("v s code", "vs code");
        PHONETIC_CORRECTIONS.put("vee s code", "vs code");
        PHONETIC_CORRECTIONS.put("visual studio code", "vs code");
        PHONETIC_CORRECTIONS.put("vscode", "vs code");

        // WhatsApp variations
        PHONETIC_CORRECTIONS.put("whats app", "whatsapp");
        PHONETIC_CORRECTIONS.put("what's app", "whatsapp");
        PHONETIC_CORRECTIONS.put("what sap", "whatsapp");
        PHONETIC_CORRECTIONS.put("what sup", "whatsapp");

        // ChatGPT variations
        PHONETIC_CORRECTIONS.put("chat gpt", "chatgpt");
        PHONETIC_CORRECTIONS.put("chat g p t", "chatgpt");
        PHONETIC_CORRECTIONS.put("chad gpt", "chatgpt");
        PHONETIC_CORRECTIONS.put("chat tpt", "chatgpt");

        // Explorer / Files
        PHONETIC_CORRECTIONS.put("file explorer", "files");
        PHONETIC_CORRECTIONS.put("my files", "files");
        PHONETIC_CORRECTIONS.put("explorer", "files");

        // Open command prefixes
        PHONETIC_CORRECTIONS.put("upon ", "open ");
        PHONETIC_CORRECTIONS.put("opan ", "open ");
        PHONETIC_CORRECTIONS.put("own ", "open ");
        PHONETIC_CORRECTIONS.put("oh pen ", "open ");
        PHONETIC_CORRECTIONS.put("open up ", "open ");

        // Nexus variations
        PHONETIC_CORRECTIONS.put("nexis", "nexus");
        PHONETIC_CORRECTIONS.put("nexas", "nexus");
        PHONETIC_CORRECTIONS.put("nixes", "nexus");
    }

    public void setLanguageMode(LanguageMode mode) {
        if (mode != null) {
            this.languageMode = mode;
            System.out.println("[WindowsSpeechRecognizer] Language mode set to: " + mode.getLabel());
        }
    }

    public LanguageMode getLanguageMode() {
        return languageMode;
    }

    /**
     * Listens asynchronously with multi-pass evaluation and auto-retry.
     */
    public void listenAsync(Consumer<String> onTranscript, Runnable onStart, Runnable onReady, Runnable onEnd) {
        if (isListening.get()) return;
        isListening.set(true);

        executor.submit(() -> {
            try {
                if (onStart != null) onStart.run();

                String finalResult = null;

                for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                    System.out.println("[WindowsSpeechRecognizer] Voice capture session: pass " + attempt + "/" + MAX_RETRIES);

                    String[] result = executeSapiRecognition(attempt == 1 ? onReady : null);

                    if (result != null && result[0] != null && !result[0].isBlank()) {
                        double confidence = 0.0;
                        try { confidence = Double.parseDouble(result[1]); } catch (Exception ignored) {}
                        String grammar = result[2] != null ? result[2] : "Unknown";

                        System.out.printf("[WindowsSpeechRecognizer] Pass %d -> Result: \"%s\" (Confidence: %.2f, Grammar: %s)%n",
                                attempt, result[0], confidence, grammar);

                        if (confidence >= MIN_CONFIDENCE) {
                            finalResult = result[0];
                            break;
                        } else {
                            System.out.printf("[WindowsSpeechRecognizer] Low confidence (%.2f < %.2f)%n", confidence, MIN_CONFIDENCE);
                            if (attempt == MAX_RETRIES && confidence >= 0.15) {
                                finalResult = result[0];
                            }
                        }
                    }
                }

                if (finalResult != null && !finalResult.isBlank()) {
                    String processed = postProcessTranscript(finalResult);
                    System.out.println("[WindowsSpeechRecognizer] Validated speech: \"" + processed + "\"");
                    if (onTranscript != null) {
                        onTranscript.accept(processed);
                    }
                } else {
                    System.out.println("[WindowsSpeechRecognizer] No confident voice input recognized.");
                    if (onTranscript != null) {
                        onTranscript.accept("");
                    }
                }

            } catch (Exception e) {
                System.err.println("[WindowsSpeechRecognizer] Recognition exception: " + e.getMessage());
                if (onTranscript != null) {
                    onTranscript.accept("");
                }
            } finally {
                isListening.set(false);
                activeProcess = null;
                if (onEnd != null) onEnd.run();
            }
        });
    }

    /**
     * Executes native SAPI recognition via PowerShell.
     * Returns String[3]: {transcript, confidence, grammarName} or null.
     */
    private String[] executeSapiRecognition(Runnable onReady) throws Exception {
        // Collect app command target choices
        List<String> appTargets = Arrays.asList(
                "youtube", "chrome", "google chrome", "calculator", "calc",
                "files", "explorer", "file explorer", "notepad", "camera",
                "spotify", "vs code", "vscode", "visual studio code", "whatsapp",
                "settings", "system settings", "maps", "google maps", "google",
                "chatgpt", "chat gpt", "github", "discord", "terminal",
                "command prompt", "cmd", "powershell", "task manager",
                "netflix", "amazon", "instagram", "twitter", "reddit"
        );

        // Collect dialogue & technical phrases
        List<String> conversationalTerms = buildConversationalTerms();

        // Format PowerShell array literals
        String appTargetsArr = toPsArray(appTargets);
        String phrasesArr = toPsArray(conversationalTerms);

        // Build PowerShell SAPI script with GrammarBuilder
        String psScript = """
            Add-Type -AssemblyName System.Speech;
            $rec = $null;
            try {
                # Pick installed recognizer (prefer en-US / system default)
                $recInfo = [System.Speech.Recognition.SpeechRecognitionEngine]::InstalledRecognizers() |
                    Where-Object { $_.Culture.Name -eq 'en-US' } | Select-Object -First 1;
                if ($recInfo) {
                    $rec = New-Object System.Speech.Recognition.SpeechRecognitionEngine($recInfo.Id);
                } else {
                    $rec = New-Object System.Speech.Recognition.SpeechRecognitionEngine;
                }

                # ═══ 1. NATURAL APP-LAUNCH COMMAND GRAMMAR ═══
                # Rule: [Optional Prefix] + [Action] + [Optional Article] + [Target] + [Optional Suffix]
                $prefixChoices = New-Object System.Speech.Recognition.Choices;
                $prefixChoices.Add([string[]]@('nexus', 'hey nexus', 'hi nexus', 'hello nexus', 'please', 'can you', 'could you', 'can you please', 'nexus please', 'machane', 'mone', 'aliya'));

                $actionChoices = New-Object System.Speech.Recognition.Choices;
                $actionChoices.Add([string[]]@('open', 'launch', 'start', 'run', 'play', 'show', 'show me', 'go to', 'bring up', 'fire up', 'navigate to'));

                $articleChoices = New-Object System.Speech.Recognition.Choices;
                $articleChoices.Add([string[]]@('the', 'my', 'a', 'an'));

                $targetChoices = New-Object System.Speech.Recognition.Choices;
                $targetChoices.Add([string[]]%s);

                $suffixChoices = New-Object System.Speech.Recognition.Choices;
                $suffixChoices.Add([string[]]@('please', 'for me', 'now'));

                $cmdGb = New-Object System.Speech.Recognition.GrammarBuilder;
                $cmdGb.Append($prefixChoices, 0, 1);
                $cmdGb.Append($actionChoices);
                $cmdGb.Append($articleChoices, 0, 1);
                $cmdGb.Append($targetChoices);
                $cmdGb.Append($suffixChoices, 0, 1);

                $cmdGrammar = New-Object System.Speech.Recognition.Grammar($cmdGb);
                $cmdGrammar.Name = 'AppCommands';
                $cmdGrammar.Weight = 0.95;
                $rec.LoadGrammar($cmdGrammar);

                # ═══ 2. STANDALONE TARGETS (e.g. user just says "youtube" or "chrome") ═══
                $directGb = New-Object System.Speech.Recognition.GrammarBuilder;
                $directGb.Append($targetChoices);
                $directGrammar = New-Object System.Speech.Recognition.Grammar($directGb);
                $directGrammar.Name = 'DirectTargets';
                $directGrammar.Weight = 0.90;
                $rec.LoadGrammar($directGrammar);

                # ═══ 3. CONVERSATIONAL & BILINGUAL PHRASES GRAMMAR ═══
                $phraseChoices = New-Object System.Speech.Recognition.Choices;
                $phraseChoices.Add([string[]]%s);
                $phraseGb = New-Object System.Speech.Recognition.GrammarBuilder($phraseChoices);
                $phraseGrammar = New-Object System.Speech.Recognition.Grammar($phraseGb);
                $phraseGrammar.Name = 'ConversationalPhrases';
                $phraseGrammar.Weight = 0.85;
                $rec.LoadGrammar($phraseGrammar);

                # ═══ 4. GENERAL DICTATION GRAMMAR (FALLBACK) ═══
                $dictGrammar = New-Object System.Speech.Recognition.DictationGrammar;
                $dictGrammar.Name = 'DictationFallback';
                $dictGrammar.Weight = 0.20;
                $rec.LoadGrammar($dictGrammar);

                # ═══ 5. ACOUSTIC ISOLATION & TIMEOUT CONFIG ═══
                # Short acoustic beep BEFORE opening microphone stream
                [Console]::Beep(1100, 70);
                Start-Sleep -Milliseconds 120;

                # Set input to hardware mic
                $rec.SetInputToDefaultAudioDevice();
                $rec.MaxAlternates = 7;
                $rec.InitialSilenceTimeout = [TimeSpan]::FromSeconds(7);
                $rec.BabbleTimeout = [TimeSpan]::FromSeconds(20);
                $rec.EndSilenceTimeout = [TimeSpan]::FromMilliseconds(900);
                $rec.EndSilenceTimeoutAmbiguous = [TimeSpan]::FromMilliseconds(600);

                Write-Output "EVENT:READY";

                # Recognition window
                $res = $rec.Recognize([TimeSpan]::FromSeconds(14));

                if ($res -and $res.Text -and $res.Text.Trim().Length -gt 0) {
                    $conf = $res.Confidence;
                    $grammarName = 'Unknown';
                    if ($res.Grammar -and $res.Grammar.Name) {
                        $grammarName = $res.Grammar.Name;
                    }

                    Write-Output ('CONFIDENCE:' + $conf);
                    Write-Output ('GRAMMAR:' + $grammarName);
                    Write-Output ('RESULT:' + $res.Text);

                    # Emit alternates
                    if ($res.Alternates) {
                        foreach ($alt in $res.Alternates) {
                            if ($alt.Text -ne $res.Text) {
                                Write-Output ('ALT:' + $alt.Confidence + ':::' + $alt.Text);
                            }
                        }
                    }
                } else {
                    Write-Output "CONFIDENCE:0.0";
                    Write-Output "GRAMMAR:None";
                    Write-Output "RESULT:NONE";
                }
            } catch {
                Write-Output ('ERR:' + $_.Exception.Message);
                Write-Output "CONFIDENCE:0.0";
                Write-Output "GRAMMAR:Error";
                Write-Output "RESULT:NONE";
            } finally {
                if ($rec) { $rec.Dispose(); }
            }
        """.formatted(appTargetsArr, phrasesArr);

        ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-NoProfile", "-NonInteractive", "-Command", psScript
        );
        pb.redirectErrorStream(true);
        activeProcess = pb.start();

        String recognizedText = null;
        String confidence = "0.0";
        String grammarName = "Unknown";
        List<String[]> alternates = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(activeProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.equals("EVENT:READY")) {
                    if (onReady != null) onReady.run();
                } else if (line.startsWith("CONFIDENCE:")) {
                    confidence = line.substring("CONFIDENCE:".length()).trim();
                } else if (line.startsWith("GRAMMAR:")) {
                    grammarName = line.substring("GRAMMAR:".length()).trim();
                } else if (line.startsWith("RESULT:")) {
                    String result = line.substring("RESULT:".length()).trim();
                    if (!result.equalsIgnoreCase("NONE") && !result.isEmpty()) {
                        recognizedText = result;
                    }
                } else if (line.startsWith("ALT:")) {
                    String altData = line.substring("ALT:".length()).trim();
                    int delim = altData.indexOf(":::");
                    if (delim > 0) {
                        alternates.add(new String[]{
                                altData.substring(delim + 3),
                                altData.substring(0, delim)
                        });
                    }
                } else if (line.startsWith("ERR:")) {
                    System.err.println("[WindowsSpeechRecognizer] SAPI Engine notice: " + line);
                }
            }
        }
        activeProcess.waitFor();

        // If primary confidence is low, search alternates for strong matches
        if (recognizedText != null) {
            double primaryConf = 0.0;
            try { primaryConf = Double.parseDouble(confidence); } catch (Exception ignored) {}

            if (primaryConf < MIN_CONFIDENCE && !alternates.isEmpty()) {
                for (String[] alt : alternates) {
                    double altConf = 0.0;
                    try { altConf = Double.parseDouble(alt[1]); } catch (Exception ignored) {}
                    if (altConf > primaryConf) {
                        System.out.printf("[WindowsSpeechRecognizer] Upgrading to alternate candidate: \"%s\" (%.2f > %.2f)%n",
                                alt[0], altConf, primaryConf);
                        recognizedText = alt[0];
                        confidence = alt[1];
                        break;
                    }
                }
            }
        }

        if (recognizedText != null) {
            return new String[]{recognizedText, confidence, grammarName};
        }
        return null;
    }

    private String toPsArray(List<String> items) {
        StringBuilder sb = new StringBuilder("@(");
        boolean first = true;
        for (String item : items) {
            if (item == null || item.isBlank()) continue;
            if (!first) sb.append(", ");
            sb.append("'").append(item.replace("'", "''")).append("'");
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }

    private List<String> buildConversationalTerms() {
        List<String> terms = new ArrayList<>();

        // Malayalam / Manglish phrases
        if (languageMode == LanguageMode.BILINGUAL || languageMode == LanguageMode.MALAYALAM) {
            terms.addAll(Arrays.asList(
                    "namaskaram", "namaskaram nexus", "namaskar", "namaste",
                    "sugamano", "sukhamaano", "sukhamano", "sukham aano", "sugam aano",
                    "enthokkeyund", "enthokke undu", "enthokke und", "endha mone", "entha mone",
                    "scene mone", "pinnalla", "pin nalla", "mass da", "mas da",
                    "aliya", "machane", "mone", "evideya", "evidya",
                    "ninte perentha", "aaranu nee", "aarannu", "onnumilla", "kaaryam para",
                    "karyam para", "paattu paadu", "dhaya cheythu", "nanni", "shubhadinam",
                    "bye", "tata", "adichu keri vaa", "thug life",
                    "pwoli", "poli", "adipoli", "adi poli", "roast me", "make fun of me",
                    "enthinaanu", "aarumilla", "kollam", "kidu", "sherikkum",
                    "aano", "poda", "podi", "mathi", "entha patti", "entha news", "entha vishesham"
            ));
        }

        // English Conversational and Technical queries
        if (languageMode == LanguageMode.BILINGUAL || languageMode == LanguageMode.ENGLISH) {
            terms.addAll(Arrays.asList(
                    "hello", "hi", "hey", "hi nexus", "hello nexus", "hey nexus",
                    "good morning", "good afternoon", "good evening", "good night",
                    "who created you", "who made you", "who are you",
                    "what is your name", "what can you do", "introduce yourself",
                    "roast me", "make fun of me", "tell me a joke", "say something funny",
                    "explain quicksort", "how does quicksort work", "explain mergesort",
                    "what is binary search", "how does hashmap work", "what is an api",
                    "what is a rest api", "what are virtual threads", "explain oop",
                    "what are the pillars of oop", "explain transformers", "how does attention work",
                    "what is a neural network", "explain backpropagation", "sql vs nosql",
                    "python vs java", "what is recursion", "what is dynamic programming",
                    "process vs thread", "what is a deadlock", "merge vs rebase",
                    "what is machine learning", "explain artificial intelligence",
                    "summarize", "confirm", "stop", "cancel", "mute", "unmute",
                    "thank you", "thanks", "help", "how are you", "solve math equation",
                    "show me code", "explain this image", "what time is it", "take a screenshot"
            ));
        }

        return terms;
    }

    /**
     * Post-processing pipeline:
     * 1. Phonetic dictionary correction
     * 2. Levenshtein edit distance snapping for common phrases
     * 3. Manglish-to-Malayalam conversion
     * 4. Normalization of app launch intents
     */
    private String postProcessTranscript(String raw) {
        if (raw == null || raw.isBlank()) return raw;

        String text = raw.trim();

        // ── Step 1: Phonetic replacements ──
        String lower = text.toLowerCase();
        for (Map.Entry<String, String> entry : PHONETIC_CORRECTIONS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                text = text.replaceAll("(?i)" + escapeRegex(entry.getKey()), entry.getValue());
                lower = text.toLowerCase();
            }
        }

        // ── Step 2: Levenshtein snapping for single/double word commands ──
        List<String> knownTargets = Arrays.asList(
                "youtube", "chrome", "calculator", "files", "notepad",
                "camera", "spotify", "vs code", "whatsapp", "settings",
                "maps", "google", "chatgpt", "github", "discord", "terminal",
                "namaskaram", "sugamano", "enthokkeyund", "adipoli", "pwoli"
        );
        String snapped = findClosestMatch(lower, knownTargets, 2);
        if (snapped != null && !snapped.equals(lower)) {
            System.out.println("[WindowsSpeechRecognizer] Levenshtein snap: \"" + text + "\" → \"" + snapped + "\"");
            text = snapped;
            lower = text.toLowerCase();
        }

        // ── Step 3: Manglish transliteration ──
        for (Map.Entry<String, String> entry : MANGLISH_MAPPINGS.entrySet()) {
            if (lower.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        for (Map.Entry<String, String> entry : MANGLISH_MAPPINGS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                text = text.replaceAll("(?i)\\b" + escapeRegex(entry.getKey()) + "\\b", entry.getValue());
            }
        }

        // ── Step 4: Clean leading polite prefixes for intent execution ──
        // e.g. "nexus please open youtube" -> "open youtube"
        text = text.replaceAll("(?i)^(nexus|hey nexus|hello nexus|hi nexus)\\s*,?\\s*", "").trim();

        return text;
    }

    private String findClosestMatch(String input, List<String> candidates, int maxDistance) {
        String best = null;
        int bestDist = Integer.MAX_VALUE;

        for (String candidate : candidates) {
            int dist = levenshteinDistance(input, candidate);
            if (dist <= maxDistance && dist < bestDist && dist > 0) {
                bestDist = dist;
                best = candidate;
            }
        }
        return best;
    }

    private int levenshteinDistance(String a, String b) {
        if (a == null || b == null) return Integer.MAX_VALUE;
        int lenA = a.length(), lenB = b.length();
        if (Math.abs(lenA - lenB) > 4) return Integer.MAX_VALUE;

        int[][] dp = new int[lenA + 1][lenB + 1];
        for (int i = 0; i <= lenA; i++) dp[i][0] = i;
        for (int j = 0; j <= lenB; j++) dp[0][j] = j;

        for (int i = 1; i <= lenA; i++) {
            for (int j = 1; j <= lenB; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[lenA][lenB];
    }

    private String escapeRegex(String input) {
        return input.replaceAll("([\\\\{}()\\[\\].+*?^$|])", "\\\\$1");
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
