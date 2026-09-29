package com.nexus.personalization;

import com.nexus.core.AppConfig;
import com.nexus.core.MultimodalEventBus;
import com.nexus.persistence.InteractionEntity;
import com.nexus.persistence.InteractionRepository;
import com.nexus.persistence.UserProfileRepository;
import com.nexus.reasoning.ChatMessage;
import com.nexus.reasoning.LlmService;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Adaptive Personalization Engine for Project N.E.X.U.S.
 * Continuously logs interactions, calculates sentiment trends and topic frequency,
 * and periodically triggers LLM-driven behavioral synthesis to update the user profile.
 */
public class PersonalizationEngine {

    private final InteractionRepository interactionRepo;
    private final UserProfileRepository profileRepo;
    private final LlmService llmService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private UserProfile currentProfile;
    private int interactionsSinceLastSynthesis = 0;

    public PersonalizationEngine(InteractionRepository interactionRepo,
                                 UserProfileRepository profileRepo,
                                 LlmService llmService) {
        this.interactionRepo = interactionRepo;
        this.profileRepo = profileRepo;
        this.llmService = llmService;
        this.currentProfile = profileRepo.getProfile();
    }

    public UserProfile getCurrentProfile() {
        return currentProfile;
    }

    /**
     * Process an interaction turn, log it, and trigger periodic synthesis if interval reached.
     */
    public void processInteraction(String userInput, String assistantReply, String source,
                                   String mood, String gesture, long latencyMs) {
        double sentiment = calculateSentiment(userInput);
        String topics = extractTopics(userInput);

        InteractionEntity entity = new InteractionEntity(
                userInput, assistantReply, source, mood, gesture, topics, sentiment, latencyMs
        );
        interactionRepo.save(entity);

        interactionsSinceLastSynthesis++;
        currentProfile.setTotalInteractions(currentProfile.getTotalInteractions() + 1);
        profileRepo.updateProfile(currentProfile);

        int threshold = AppConfig.getInstance().getSynthesisInterval();
        if (interactionsSinceLastSynthesis >= threshold) {
            triggerBehavioralSynthesisAsync();
            interactionsSinceLastSynthesis = 0;
        }
    }

    /**
     * Triggers asynchronous LLM synthesis of recent user interaction patterns.
     */
    public void triggerBehavioralSynthesisAsync() {
        executor.submit(() -> {
            try {
                System.out.println("[PersonalizationEngine] Initiating periodic user behavioral synthesis...");
                List<InteractionEntity> recent = interactionRepo.getRecent(AppConfig.getInstance().getMaxInteractionsAnalyze());
                if (recent.isEmpty()) return;

                StringBuilder interactionLogSummary = new StringBuilder();
                Map<String, Integer> topicCounts = new HashMap<>();

                for (InteractionEntity e : recent) {
                    interactionLogSummary.append("- User: \"").append(e.getUserInput())
                                         .append("\" | Mood: ").append(e.getDetectedMood())
                                         .append(" | Sentiment: ").append(String.format("%.2f", e.getSentimentScore()))
                                         .append("\n");

                    String[] tList = e.getExtractedTopics().split(",");
                    for (String t : tList) {
                        String clean = t.trim().toLowerCase();
                        if (!clean.isEmpty()) {
                            topicCounts.put(clean, topicCounts.getOrDefault(clean, 0) + 1);
                        }
                    }
                }

                // Determine top topics
                List<Map.Entry<String, Integer>> sortedTopics = new ArrayList<>(topicCounts.entrySet());
                sortedTopics.sort((a, b) -> b.getValue().compareTo(a.getValue()));
                StringBuilder topTopicsStr = new StringBuilder();
                for (int i = 0; i < Math.min(3, sortedTopics.size()); i++) {
                    if (i > 0) topTopicsStr.append(", ");
                    topTopicsStr.append(capitalize(sortedTopics.get(i).getKey()));
                }

                String synthesisPrompt = """
                    Analyze the following recent user interactions with an AI assistant.
                    Synthesize:
                    1. A concise, 2-sentence summary of the user's communication style, preferred demeanor, and emotional cadence.
                    2. A short preferred tone descriptor (e.g., 'Technical & Direct', 'Casual & Friendly', 'Supportive & Analytical').
                    
                    Format your response strictly as:
                    SUMMARY: <2 sentences>
                    TONE: <tone descriptor>
                    
                    Interactions batch:
                    """ + interactionLogSummary;

                List<ChatMessage> promptList = List.of(
                        new ChatMessage("system", "You are an expert psycholinguistic behavioral profiling engine for N.E.X.U.S."),
                        new ChatMessage("user", synthesisPrompt)
                );

                llmService.generateResponseAsync(promptList).thenAccept(reply -> {
                    parseAndApplySynthesis(reply, topTopicsStr.toString());
                });

            } catch (Exception e) {
                System.err.println("[PersonalizationEngine] Synthesis failed: " + e.getMessage());
            }
        });
    }

    private void parseAndApplySynthesis(String llmReply, String topTopics) {
        String summary = currentProfile.getBehavioralSummary();
        String tone = currentProfile.getPreferredTone();

        for (String line : llmReply.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("SUMMARY:")) {
                summary = trimmed.substring("SUMMARY:".length()).trim();
            } else if (trimmed.startsWith("TONE:")) {
                tone = trimmed.substring("TONE:".length()).trim();
            }
        }

        currentProfile.setBehavioralSummary(summary);
        currentProfile.setPreferredTone(tone);
        if (!topTopics.isBlank()) {
            currentProfile.setTopTopics(topTopics);
        }

        profileRepo.updateProfile(currentProfile);
        System.out.println("[PersonalizationEngine] Updated User Profile successfully: " + currentProfile.getPreferredTone());
        MultimodalEventBus.getInstance().publishOnFxThread(currentProfile);
    }

    public double calculateSentiment(String text) {
        if (text == null || text.isBlank()) return 0.0;
        String lower = text.toLowerCase();
        double score = 0.0;

        String[] positive = {"good", "great", "excellent", "awesome", "thanks", "perfect", "fast", "love", "yes", "nice", "helpful"};
        String[] negative = {"bad", "slow", "error", "fail", "wrong", "terrible", "hate", "issue", "problem", "broken", "stop"};

        for (String p : positive) if (lower.contains(p)) score += 0.25;
        for (String n : negative) if (lower.contains(n)) score -= 0.35;

        return Math.max(-1.0, Math.min(1.0, score));
    }

    public String extractTopics(String text) {
        if (text == null || text.isBlank()) return "General";
        String lower = text.toLowerCase();
        List<String> found = new ArrayList<>();

        if (lower.contains("java") || lower.contains("code") || lower.contains("programming")) found.add("Software");
        if (lower.contains("vision") || lower.contains("camera") || lower.contains("face") || lower.contains("gesture")) found.add("Computer Vision");
        if (lower.contains("voice") || lower.contains("speech") || lower.contains("mic") || lower.contains("audio")) found.add("Speech I/O");
        if (lower.contains("ai") || lower.contains("model") || lower.contains("llm") || lower.contains("intelligence")) found.add("AI Architecture");
        if (lower.contains("status") || lower.contains("system") || lower.contains("performance")) found.add("Telemetry");

        if (found.isEmpty()) return "General Inquiry";
        return String.join(", ", found);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
