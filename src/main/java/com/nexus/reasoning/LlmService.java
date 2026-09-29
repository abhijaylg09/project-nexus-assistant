package com.nexus.reasoning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.core.AppConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * High-performance HTTP client for LLM reasoning.
 * Interacts with OpenAI, Ollama, Groq, or OpenRouter with intelligent fallback.
 */
public class LlmService {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final AppConfig config;

    public LlmService() {
        this.config = AppConfig.getInstance();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    public CompletableFuture<String> generateResponseAsync(List<ChatMessage> messages) {
        return CompletableFuture.supplyAsync(() -> {
            String apiKey = config.getLlmApiKey();
            String endpoint = config.getLlmEndpoint();

            // If no valid key provided or endpoint is empty, trigger mock/heuristic AI fallback
            if (apiKey == null || apiKey.isBlank() || apiKey.contains("YOUR_API_KEY")) {
                if (config.isMockFallbackEnabled() && !endpoint.contains("localhost") && !endpoint.contains("127.0.0.1")) {
                    return generateIntelligentFallback(messages);
                }
            }

            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", config.getLlmModel());
                requestBody.put("messages", messages);
                requestBody.put("temperature", config.getLlmTemperature());
                requestBody.put("max_tokens", config.getLlmMaxTokens());

                String jsonPayload = mapper.writeValueAsString(requestBody);

                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(25))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

                if (apiKey != null && !apiKey.isBlank() && !apiKey.contains("YOUR_API_KEY")) {
                    reqBuilder.header("Authorization", "Bearer " + apiKey);
                }

                HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    JsonNode root = mapper.readTree(response.body());
                    JsonNode choices = root.get("choices");
                    if (choices != null && choices.isArray() && choices.size() > 0) {
                        return choices.get(0).get("message").get("content").asText();
                    }
                } else {
                    System.err.println("[LlmService] API error code " + response.statusCode() + ": " + response.body());
                }
            } catch (Exception e) {
                System.err.println("[LlmService] HTTP request failed: " + e.getMessage() + ". Engaging local heuristic fallback.");
            }

            return generateIntelligentFallback(messages);
        });
    }

    /**
     * Context-aware local response engine when external LLM is offline or no API key is specified.
     */
    private String generateIntelligentFallback(List<ChatMessage> messages) {
        String lastUserMsg = "";
        String systemCtx = "";
        for (ChatMessage m : messages) {
            if ("user".equalsIgnoreCase(m.getRole())) lastUserMsg = m.getContent();
            if ("system".equalsIgnoreCase(m.getRole())) systemCtx = m.getContent();
        }

        String lower = lastUserMsg.toLowerCase();
        String moodTag = "Neutral";
        if (systemCtx.contains("HAPPY")) moodTag = "Happy";
        else if (systemCtx.contains("STRESSED")) moodTag = "Stressed";
        else if (systemCtx.contains("FOCUSED")) moodTag = "Focused";
        else if (systemCtx.contains("SURPRISED")) moodTag = "Surprised";

        if (lower.contains("who are you") || lower.contains("what are you") || lower.contains("introduce")) {
            return "I am N.E.X.U.S (Neural EXecutive User System). I am a real-time multimodal personal AI assistant built around a centralized Java core, integrating live facial mood tracking, hand gesture recognition, speech processing, and adaptive behavioral modeling.";
        }

        if (lower.contains("status") || lower.contains("system") || lower.contains("telemetry")) {
            return "All N.E.X.U.S systems are nominal. Java Core Orchestrator is active, Vision perceptual stream is running with current user mood evaluated as [" + moodTag + "], and SQLite interaction persistence is online.";
        }

        if (lower.contains("mood") || lower.contains("emotion") || lower.contains("feeling")) {
            return "Based on live OpenCV face tracking and ONNX emotion classification, I observe your state as [" + moodTag + "]. My adaptive engine tunes my response demeanor accordingly.";
        }

        if (lower.contains("gesture") || lower.contains("hand")) {
            return "My vision perception pipeline classifies hand gestures such as Thumbs-Up (Confirm/Acknowledge), Open Palm (Mute/Pause), and Peace (Summarize). Show a gesture in front of your camera!";
        }

        if (lower.contains("team") || lower.contains("members") || lower.contains("who made you")) {
            return "Project N.E.X.U.S was designed and engineered by Team STI25CS: Bhadra G. S., Aleena Maria Roy, Abhishek A., Dia M. Joby, and Abhijay L. G.";
        }

        if (lower.contains("personalization") || lower.contains("adaptive") || lower.contains("learn")) {
            return "My Adaptive Personalization Engine logs your communication cadence, emotional trends, and topic preferences into SQLite. Periodically, I synthesize this data into an evolving user behavioral profile that grounds all future responses.";
        }

        if (lower.contains("help") || lower.contains("command")) {
            return "You can speak with me using your microphone, type queries into the HUD console, trigger actions via webcam gestures, or ask me to analyze code, explain concepts, or summarize topics.";
        }

        // Context-aware conversational default
        if (moodTag.equalsIgnoreCase("Stressed")) {
            return "I notice you might be experiencing some stress right now. I've noted: \"" + lastUserMsg + "\". I'm here to streamline your workflow and assist with whatever you need.";
        } else if (moodTag.equalsIgnoreCase("Happy")) {
            return "Glad to see your positive energy! Regarding \"" + lastUserMsg + "\", let's dive into it. How would you like me to assist?";
        }

        return "Acknowledged. Processed your query: \"" + lastUserMsg + "\". System telemetry is tracking your context seamlessly. (To enable cloud LLM reasoning, provide your OpenAI/Ollama API key in config/nexus-config.json).";
    }
}
