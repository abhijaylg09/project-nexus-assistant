package com.nexus.reasoning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.core.AppConfig;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * High-performance, Multi-Tiered Intelligent LLM Engine for Project N.E.X.U.S.
 * 
 * Pipeline:
 *  1. Immediate Local/Identity Fast-Path (Who are you, Team info) -> Instant offline response (<5ms)
 *  2. User-Configured API (OpenAI, Groq, Ollama, OpenRouter) -> If key or local endpoint provided
 *  3. Zero-Config Free Cloud AI (Pollinations OpenAI & GET Endpoints) -> Answers ANY question without API keys
 *  4. Deep Offline Knowledge Engine & Math Solver -> Autonomous offline fallback when network is unavailable
 */
public class LlmService {

    private static final String FREE_CLOUD_AI_OPENAI = "https://text.pollinations.ai/openai";
    private static final String FREE_CLOUD_AI_DIRECT = "https://text.pollinations.ai/";

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final AppConfig config;

    public LlmService() {
        this.config = AppConfig.getInstance();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(8))
                .build();
        this.mapper = new ObjectMapper();
    }

    public CompletableFuture<String> generateResponseAsync(List<ChatMessage> messages) {
        return CompletableFuture.supplyAsync(() -> {
            String lastUserMsg = "";
            String systemCtx = "";
            for (ChatMessage m : messages) {
                if ("user".equalsIgnoreCase(m.getRole())) lastUserMsg = m.getContent();
                if ("system".equalsIgnoreCase(m.getRole())) systemCtx = m.getContent();
            }

            if (lastUserMsg == null || lastUserMsg.isBlank()) {
                return "I am online and ready. What would you like to explore or solve?";
            }

            String qLower = lastUserMsg.toLowerCase(Locale.ROOT).trim();

            // ── FAST PATH: Immediate identity & system inquiries ──
            if (isImmediateLocalQuery(qLower)) {
                return generateIntelligentFallback(messages);
            }

            // ── TIER 1: User-Configured Cloud or Local LLM (OpenAI, Groq, Ollama) ──
            String apiKey = config.getLlmApiKey();
            String endpoint = config.getLlmEndpoint();
            boolean hasValidKey = apiKey != null && !apiKey.isBlank() && !apiKey.contains("YOUR_API_KEY");
            boolean isLocalhost = endpoint != null && (endpoint.contains("localhost") || endpoint.contains("127.0.0.1"));

            if (hasValidKey || isLocalhost) {
                try {
                    String reply = callOpenAiCompatibleApi(endpoint, apiKey, config.getLlmModel(), messages, 18);
                    if (reply != null && !reply.isBlank()) {
                        return reply;
                    }
                } catch (Exception e) {
                    System.err.println("[LlmService] Primary LLM failed: " + e.getMessage() + ". Engaging Free Cloud AI.");
                }
            }

            // ── TIER 2: Zero-Config Free Cloud Generative AI ──
            // Answers any question (math, code, history, general Qs) without requiring an API key
            try {
                String freeAiReply = callFreeCloudAi(messages, lastUserMsg);
                if (freeAiReply != null && !freeAiReply.isBlank()) {
                    return freeAiReply;
                }
            } catch (Exception e) {
                System.err.println("[LlmService] Free Cloud AI fallback error: " + e.getMessage() + ". Engaging Offline Core.");
            }

            // ── TIER 3: Autonomous Deep Offline Knowledge Engine ──
            return generateIntelligentFallback(messages);
        });
    }

    /**
     * Checks if query is an immediate identity / assistant info inquiry that should resolve instantly.
     */
    private boolean isImmediateLocalQuery(String qLower) {
        String clean = qLower.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        return clean.equals("who are you") || clean.startsWith("who are you") ||
               clean.equals("what are you") || clean.startsWith("what are you") ||
               clean.contains("who made you") || clean.contains("who created you") ||
               clean.contains("introduce yourself") || clean.contains("team members") ||
               clean.contains("team sti25cs");
    }

    /**
     * Executes standard OpenAI-compatible Chat Completions API call.
     */
    private String callOpenAiCompatibleApi(String endpoint, String apiKey, String model,
                                           List<ChatMessage> messages, int timeoutSeconds) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", (model != null && !model.isBlank()) ? model : "gpt-4o-mini");
        requestBody.put("messages", messages);
        requestBody.put("temperature", config.getLlmTemperature());
        requestBody.put("max_tokens", Math.max(config.getLlmMaxTokens(), 400));

        String jsonPayload = mapper.writeValueAsString(requestBody);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(timeoutSeconds))
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
                JsonNode messageNode = choices.get(0).get("message");
                if (messageNode != null && messageNode.has("content")) {
                    return messageNode.get("content").asText();
                }
            }
        } else {
            System.err.println("[LlmService] API returned status " + response.statusCode() + ": " + response.body());
        }
        return null;
    }

    /**
     * Queries Free Cloud AI endpoint to answer questions without requiring API keys.
     */
    private String callFreeCloudAi(List<ChatMessage> messages, String userQuery) {
        // Method A: OpenAI JSON format on Pollinations
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", "openai");
            body.put("messages", messages);
            body.put("temperature", 0.7);

            String jsonPayload = mapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FREE_CLOUD_AI_OPENAI))
                    .timeout(Duration.ofSeconds(14))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = mapper.readTree(response.body());
                JsonNode choices = root.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode msg = choices.get(0).get("message");
                    if (msg != null && msg.has("content")) {
                        String ans = msg.get("content").asText();
                        if (ans != null && !ans.isBlank()) return ans.trim();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[LlmService] Free Cloud AI POST attempt: " + e.getMessage());
        }

        // Method B: Direct URL Query on Pollinations GET endpoint (ultra-resilient fallback)
        try {
            String encoded = URLEncoder.encode(userQuery, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(FREE_CLOUD_AI_DIRECT + encoded))
                    .timeout(Duration.ofSeconds(12))
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                String text = resp.body();
                if (text != null && !text.isBlank() && !text.startsWith("<!DOCTYPE")) {
                    return text.trim();
                }
            }
        } catch (Exception e) {
            System.err.println("[LlmService] Free Cloud AI GET attempt: " + e.getMessage());
        }

        return null;
    }

    /**
     * Context-aware local response engine when external LLM is offline or unreachable.
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
