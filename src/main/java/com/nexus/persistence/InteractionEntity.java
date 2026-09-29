package com.nexus.persistence;

import java.time.LocalDateTime;

public class InteractionEntity {
    private long id;
    private String userInput;
    private String assistantReply;
    private String inputSource; // TEXT, SPEECH, GESTURE
    private String detectedMood;
    private String detectedGesture;
    private String extractedTopics;
    private double sentimentScore; // -1.0 to +1.0
    private long latencyMs;
    private LocalDateTime timestamp;

    public InteractionEntity() {
        this.timestamp = LocalDateTime.now();
    }

    public InteractionEntity(String userInput, String assistantReply, String inputSource,
                             String detectedMood, String detectedGesture, String extractedTopics,
                             double sentimentScore, long latencyMs) {
        this.userInput = userInput;
        this.assistantReply = assistantReply;
        this.inputSource = inputSource;
        this.detectedMood = detectedMood;
        this.detectedGesture = detectedGesture;
        this.extractedTopics = extractedTopics;
        this.sentimentScore = sentimentScore;
        this.latencyMs = latencyMs;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUserInput() { return userInput; }
    public void setUserInput(String userInput) { this.userInput = userInput; }

    public String getAssistantReply() { return assistantReply; }
    public void setAssistantReply(String assistantReply) { this.assistantReply = assistantReply; }

    public String getInputSource() { return inputSource; }
    public void setInputSource(String inputSource) { this.inputSource = inputSource; }

    public String getDetectedMood() { return detectedMood; }
    public void setDetectedMood(String detectedMood) { this.detectedMood = detectedMood; }

    public String getDetectedGesture() { return detectedGesture; }
    public void setDetectedGesture(String detectedGesture) { this.detectedGesture = detectedGesture; }

    public String getExtractedTopics() { return extractedTopics; }
    public void setExtractedTopics(String extractedTopics) { this.extractedTopics = extractedTopics; }

    public double getSentimentScore() { return sentimentScore; }
    public void setSentimentScore(double sentimentScore) { this.sentimentScore = sentimentScore; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
