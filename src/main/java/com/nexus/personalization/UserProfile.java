package com.nexus.personalization;

import java.time.LocalDateTime;

public class UserProfile {
    private String userName = "Abhijay";
    private String behavioralSummary = "User values clear, high-velocity technical execution, structured multi-modal workflows, and prompt responses.";
    private String preferredTone = "Technical, Precise, & Proactive";
    private String topTopics = "Multimodal AI, Java Architecture, Speech/Vision";
    private int totalInteractions = 0;
    private LocalDateTime lastSynthesizedAt = LocalDateTime.now();

    public UserProfile() {}

    public UserProfile(String userName, String behavioralSummary, String preferredTone, String topTopics, int totalInteractions) {
        this.userName = userName;
        this.behavioralSummary = behavioralSummary;
        this.preferredTone = preferredTone;
        this.topTopics = topTopics;
        this.totalInteractions = totalInteractions;
        this.lastSynthesizedAt = LocalDateTime.now();
    }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getBehavioralSummary() { return behavioralSummary; }
    public void setBehavioralSummary(String behavioralSummary) { this.behavioralSummary = behavioralSummary; }

    public String getPreferredTone() { return preferredTone; }
    public void setPreferredTone(String preferredTone) { this.preferredTone = preferredTone; }

    public String getTopTopics() { return topTopics; }
    public void setTopTopics(String topTopics) { this.topTopics = topTopics; }

    public int getTotalInteractions() { return totalInteractions; }
    public void setTotalInteractions(int totalInteractions) { this.totalInteractions = totalInteractions; }

    public LocalDateTime getLastSynthesizedAt() { return lastSynthesizedAt; }
    public void setLastSynthesizedAt(LocalDateTime lastSynthesizedAt) { this.lastSynthesizedAt = lastSynthesizedAt; }
}
