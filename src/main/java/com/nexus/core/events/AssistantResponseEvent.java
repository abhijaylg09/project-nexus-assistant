package com.nexus.core.events;

public class AssistantResponseEvent {
    private final String responseText;
    private final String moodContext;
    private final long latencyMs;
    private final boolean error;

    public AssistantResponseEvent(String responseText, String moodContext, long latencyMs, boolean error) {
        this.responseText = responseText;
        this.moodContext = moodContext;
        this.latencyMs = latencyMs;
        this.error = error;
    }

    public static AssistantResponseEvent success(String text, String mood, long latencyMs) {
        return new AssistantResponseEvent(text, mood, latencyMs, false);
    }

    public static AssistantResponseEvent error(String text) {
        return new AssistantResponseEvent(text, "neutral", 0, true);
    }

    public String getResponseText() { return responseText; }
    public String getMoodContext() { return moodContext; }
    public long getLatencyMs() { return latencyMs; }
    public boolean isError() { return error; }
}
