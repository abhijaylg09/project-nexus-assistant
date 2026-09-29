package com.nexus;

import com.nexus.core.AppConfig;
import com.nexus.core.MultimodalEventBus;
import com.nexus.core.events.AssistantResponseEvent;
import com.nexus.core.events.MoodDetectedEvent;
import com.nexus.core.events.UserInputEvent;
import com.nexus.personalization.PersonalizationEngine;
import com.nexus.personalization.UserProfile;
import com.nexus.persistence.DatabaseManager;
import com.nexus.persistence.InteractionEntity;
import com.nexus.persistence.InteractionRepository;
import com.nexus.persistence.UserProfileRepository;
import com.nexus.reasoning.ChatMessage;
import com.nexus.reasoning.LlmService;
import com.nexus.reasoning.PromptContextBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class NexusCoreTest {

    @BeforeAll
    public static void setup() {
        assertNotNull(AppConfig.getInstance());
    }

    @Test
    @DisplayName("Verify DatabaseManager initializes SQLite tables")
    public void testDatabaseManager() {
        DatabaseManager db = DatabaseManager.getInstance();
        assertNotNull(db);

        InteractionRepository repo = new InteractionRepository();
        int initialCount = repo.getTotalCount();
        assertTrue(initialCount >= 0);

        InteractionEntity testEntity = new InteractionEntity(
                "Test input query", "Test assistant reply", "TEXT",
                "FOCUSED", "NONE", "AI Architecture", 0.5, 45
        );
        repo.save(testEntity);

        int newCount = repo.getTotalCount();
        assertEquals(initialCount + 1, newCount);

        List<InteractionEntity> recent = repo.getRecent(1);
        assertFalse(recent.isEmpty());
        assertEquals("Test input query", recent.get(0).getUserInput());
    }

    @Test
    @DisplayName("Verify UserProfileRepository loads and updates user profile")
    public void testUserProfileRepository() {
        UserProfileRepository repo = new UserProfileRepository();
        UserProfile profile = repo.getProfile();
        assertNotNull(profile);
        assertNotNull(profile.getUserName());

        profile.setPreferredTone("Analytical & Direct");
        repo.updateProfile(profile);

        UserProfile reloaded = repo.getProfile();
        assertEquals("Analytical & Direct", reloaded.getPreferredTone());
    }

    @Test
    @DisplayName("Verify PromptContextBuilder builds rich contextual prompt")
    public void testPromptContextBuilder() {
        PromptContextBuilder builder = new PromptContextBuilder();
        UserProfile profile = new UserProfile("Abhijay", "User is focused on AI development", "Technical", "Java, AI", 12);
        MoodDetectedEvent mood = new MoodDetectedEvent(MoodDetectedEvent.Emotion.FOCUSED, 0.92, 100, 100, 50, 50);

        List<ChatMessage> messages = builder.buildContext("How does N.E.X.U.S operate?", profile, mood, null, List.of());
        assertNotNull(messages);
        assertTrue(messages.size() >= 2);

        // System prompt contains profile and mood
        String systemContent = messages.get(0).getContent();
        assertTrue(systemContent.contains("Abhijay"));
        assertTrue(systemContent.contains("FOCUSED"));
    }

    @Test
    @DisplayName("Verify LlmService heuristic fallback executes synchronously")
    public void testLlmServiceFallback() throws Exception {
        LlmService llm = new LlmService();
        List<ChatMessage> prompt = List.of(
                new ChatMessage("system", "You are N.E.X.U.S assistant"),
                new ChatMessage("user", "Who are you?")
        );

        CompletableFuture<String> future = llm.generateResponseAsync(prompt);
        String reply = future.get(5, TimeUnit.SECONDS);

        assertNotNull(reply);
        assertTrue(reply.contains("N.E.X.U.S"));
    }

    @Test
    @DisplayName("Verify MultimodalEventBus dispatching")
    public void testMultimodalEventBus() {
        MultimodalEventBus bus = MultimodalEventBus.getInstance();
        AtomicBoolean received = new AtomicBoolean(false);

        bus.subscribe(UserInputEvent.class, event -> {
            if ("Hello EventBus".equals(event.getText())) {
                received.set(true);
            }
        });

        bus.publish(new UserInputEvent("Hello EventBus", UserInputEvent.InputSource.TEXT));
        assertTrue(received.get());
    }

    @Test
    @DisplayName("Verify PersonalizationEngine sentiment and topic analysis")
    public void testPersonalizationEngineCalculations() {
        InteractionRepository interactionRepo = new InteractionRepository();
        UserProfileRepository profileRepo = new UserProfileRepository();
        LlmService llm = new LlmService();

        PersonalizationEngine engine = new PersonalizationEngine(interactionRepo, profileRepo, llm);

        double positiveSentiment = engine.calculateSentiment("This assistant is awesome and great!");
        assertTrue(positiveSentiment > 0.0);

        double negativeSentiment = engine.calculateSentiment("This is bad, fail, and error.");
        assertTrue(negativeSentiment < 0.0);

        String topics = engine.extractTopics("Can you check my Java code for the camera vision and speech?");
        assertTrue(topics.contains("Software"));
        assertTrue(topics.contains("Computer Vision"));
        assertTrue(topics.contains("Speech I/O"));
    }

    @Test
    @DisplayName("Verify MotionDetector and EyeStateClassifier")
    public void testMotionAndEyeClassifiers() {
        com.nexus.vision.MotionDetector md = new com.nexus.vision.MotionDetector();
        assertEquals(0.0, md.getCurrentMotionLevel());

        com.nexus.vision.EyeStateClassifier ec = new com.nexus.vision.EyeStateClassifier();
        assertFalse(ec.isEyesClosed());
        assertFalse(ec.isDrowsinessAlert());

        ec.setManualOverride(com.nexus.vision.EyeStateClassifier.EyeStatus.CLOSED, 5000);
        assertTrue(ec.isEyesClosed());
        assertTrue(ec.isDrowsinessAlert());
    }

    @Test
    @DisplayName("Verify TeammateProfile catalog for STI25CS")
    public void testTeammates() {
        com.nexus.personalization.TeammateProfile[] list = com.nexus.personalization.TeammateProfile.getAllTeammates();
        assertEquals(5, list.length);
        assertNotNull(com.nexus.personalization.TeammateProfile.findById("ABHIJAY"));
        assertNotNull(com.nexus.personalization.TeammateProfile.findById("BHADRA"));
        assertNotNull(com.nexus.personalization.TeammateProfile.findById("ALEENA"));
        assertNotNull(com.nexus.personalization.TeammateProfile.findById("ABHISHEK"));
        assertNotNull(com.nexus.personalization.TeammateProfile.findById("DIA"));
    }

    @Test
    @DisplayName("Verify VisionReasoningEngine multimodal image analysis")
    public void testVisionReasoningEngine() throws Exception {
        com.nexus.reasoning.VisionReasoningEngine visionEngine = new com.nexus.reasoning.VisionReasoningEngine();

        // Create temporary test image
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(300, 200, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.DARK_GRAY);
        g.fillRect(0, 0, 300, 200);
        g.setColor(java.awt.Color.WHITE);
        g.drawString("class QuickSort { void sort() {} }", 20, 50);
        g.dispose();

        java.io.File tempImg = java.io.File.createTempFile("test_code_img_", ".png");
        tempImg.deleteOnExit();
        javax.imageio.ImageIO.write(img, "PNG", tempImg);

        String answer = visionEngine.analyzeImageAndAnswer(tempImg, "Explain the code in this image", "Abhijay");
        assertNotNull(answer);
        assertTrue(answer.contains("Multimodal Visual Analysis"));
        assertTrue(answer.contains("Resolution to Your Doubt"));
    }

    @Test
    @DisplayName("Verify optical gender calibration in FaceBiometricsEngine")
    public void testOpticalGenderCalibration() {
        com.nexus.vision.FaceBiometricsEngine fb = new com.nexus.vision.FaceBiometricsEngine();
        assertNotNull(fb.getDetectedGender());

        fb.setManualGenderOverride(com.nexus.core.events.MoodDetectedEvent.Gender.MALE);
        assertEquals(com.nexus.core.events.MoodDetectedEvent.Gender.MALE, fb.getDetectedGender());
        assertTrue(fb.getGenderConfidence() > 0.90);

        fb.setManualGenderOverride(com.nexus.core.events.MoodDetectedEvent.Gender.FEMALE);
        assertEquals(com.nexus.core.events.MoodDetectedEvent.Gender.FEMALE, fb.getDetectedGender());
    }
}
