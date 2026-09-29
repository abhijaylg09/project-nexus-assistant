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
     * Delivers rich, structured ChatGPT-style answers across programming, AI, math, and architecture.
     */
    private String generateIntelligentFallback(List<ChatMessage> messages) {
        String lastUserMsg = "";
        String systemCtx = "";
        for (ChatMessage m : messages) {
            if ("user".equalsIgnoreCase(m.getRole())) lastUserMsg = m.getContent();
            if ("system".equalsIgnoreCase(m.getRole())) systemCtx = m.getContent();
        }

        String teammateName = "User";
        if (systemCtx.contains("User Identity Recognized:")) {
            int start = systemCtx.indexOf("User Identity Recognized:") + "User Identity Recognized:".length();
            int end = systemCtx.indexOf("[", start);
            if (end > start) {
                teammateName = systemCtx.substring(start, end).trim();
            }
        }

        return OfflineKnowledgeEngine.answerQuery(lastUserMsg, messages, teammateName);
    }
}
