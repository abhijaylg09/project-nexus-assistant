package com.nexus.reasoning;

import com.nexus.core.AppConfig;
import com.nexus.core.events.MoodDetectedEvent;
import com.nexus.core.events.GestureDetectedEvent;
import com.nexus.personalization.UserProfile;
import com.nexus.persistence.InteractionEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Constructs the contextual prompt window by combining:
 * 1. N.E.X.U.S Core Personality & Directives
 * 2. Adaptive User Profile & Synthesized Demeanor
 * 3. Real-time Environmental Perception (Facial Mood & Gestures)
 * 4. Recent Conversation History
 */
public class PromptContextBuilder {

    public List<ChatMessage> buildContext(String userMessage,
                                          UserProfile userProfile,
                                          MoodDetectedEvent currentMood,
                                          GestureDetectedEvent currentGesture,
                                          List<InteractionEntity> recentHistory) {
        List<ChatMessage> messages = new ArrayList<>();
        AppConfig config = AppConfig.getInstance();

        // 1. Base System Instruction + Adaptive Persona Injection
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append(config.getSystemRole()).append("\n\n");

        systemPrompt.append("=== ADAPTIVE USER PROFILE (LEARNED FROM INTERACTION HISTORY) ===\n");
        systemPrompt.append("User Name: ").append(userProfile.getUserName()).append("\n");
        systemPrompt.append("Communication Style & Tone: ").append(userProfile.getPreferredTone()).append("\n");
        systemPrompt.append("Synthesized Behavioral Summary: ").append(userProfile.getBehavioralSummary()).append("\n");
        systemPrompt.append("Frequent Interest Clusters: ").append(userProfile.getTopTopics()).append("\n\n");

        // 2. Real-Time Multimodal Perception Injection
        systemPrompt.append("=== REAL-TIME MULTIMODAL PERCEPTION SENSORS ===\n");
        if (currentMood != null) {
            systemPrompt.append("User Identity Recognized: ").append(currentMood.getRecognizedIdentity())
                        .append(" [Role: ").append(currentMood.getIdentityRole()).append("]\n");
            systemPrompt.append("Gender Detected: ").append(currentMood.getGender().name())
                        .append(" (Confidence: ").append(currentMood.getFormattedGenderConfidence()).append(")\n");
            systemPrompt.append("User Facial Emotion: ").append(currentMood.getEmotion().name())
                        .append(" (Confidence: ").append(currentMood.getFormattedConfidence()).append(")\n");
            systemPrompt.append("Optical Motion Level: ").append(String.format("%.0f%%", currentMood.getMotionLevel() * 100)).append("\n");
        } else {
            systemPrompt.append("User Facial Emotion: Neutral / Observing\n");
        }

        if (currentGesture != null && currentGesture.getGesture() != GestureDetectedEvent.Gesture.NONE) {
            systemPrompt.append("Active Hand Gesture: ").append(currentGesture.getGesture().getDisplayName())
                        .append(" [Action: ").append(currentGesture.getGesture().getActionDescription()).append("]\n");
        }

        systemPrompt.append("\n=== N.E.X.U.S CHATGPT-GRADE INTELLIGENCE DIRECTIVE ===\n");
        systemPrompt.append("- You are N.E.X.U.S, an advanced, highly intelligent multimodal AI desktop assistant modeled after ChatGPT (GPT-4o).\n");
        systemPrompt.append("- Always provide articulate, comprehensive, well-structured, and helpful answers to any question across coding, mathematics, science, engineering, and everyday topics.\n");
        systemPrompt.append("- Format responses using clean Markdown: use section headings (###), bullet points, and fenced code blocks with language tags (```python, ```java, etc.) for code.\n");
        systemPrompt.append("- Deliver production-ready code, step-by-step reasoning, and solutions with professional clarity.\n");
        systemPrompt.append("- Understand and speak both English and Malayalam / Manglish fluently.\n");
        systemPrompt.append("- If the user explicitly asks to be roasted ('roast me') or uses street slang ('scene mone', 'endha mone'), adopt a witty, humorous swagger, while still providing the requested answer.\n");

        messages.add(new ChatMessage("system", systemPrompt.toString()));

        // 3. Recent Conversation History (in chronological order)
        if (recentHistory != null && !recentHistory.isEmpty()) {
            int startIdx = Math.max(0, recentHistory.size() - 6);
            for (int i = recentHistory.size() - 1; i >= startIdx; i--) {
                InteractionEntity turn = recentHistory.get(i);
                if (turn.getUserInput() != null && !turn.getUserInput().isBlank()) {
                    messages.add(new ChatMessage("user", turn.getUserInput()));
                }
                if (turn.getAssistantReply() != null && !turn.getAssistantReply().isBlank()) {
                    messages.add(new ChatMessage("assistant", turn.getAssistantReply()));
                }
            }
        }

        // 4. Current User Input
        messages.add(new ChatMessage("user", userMessage));

        return messages;
    }
}
