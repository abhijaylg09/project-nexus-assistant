package com.nexus.core;

import com.nexus.core.events.*;
import com.nexus.personalization.PersonalizationEngine;
import com.nexus.persistence.InteractionEntity;
import com.nexus.persistence.InteractionRepository;
import com.nexus.persistence.UserProfileRepository;
import com.nexus.reasoning.ChatMessage;
import com.nexus.reasoning.LlmService;
import com.nexus.reasoning.PromptContextBuilder;
import com.nexus.speech.SpeechService;
import com.nexus.vision.VisionService;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * N.E.X.U.S Core Orchestrator & Central Brain.
 * Maintains centralized Java control over Vision, Speech, Reasoning,
 * Persistence, and Adaptive Personalization subsystems.
 */
public class NexusCore {

    private final AppConfig config;
    private final MultimodalEventBus eventBus;

    // Subsystems
    private final VisionService visionService;
    private final SpeechService speechService;
    private final LlmService llmService;
    private final PromptContextBuilder promptBuilder;
    private final InteractionRepository interactionRepo;
    private final UserProfileRepository userProfileRepo;
    private final PersonalizationEngine personalizationEngine;

    // Perceptual Cache
    private final AtomicReference<MoodDetectedEvent> latestMood = new AtomicReference<>();
    private final AtomicReference<GestureDetectedEvent> latestGesture = new AtomicReference<>();

    private final ScheduledExecutorService telemetryExecutor = Executors.newSingleThreadScheduledExecutor();
    private final OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    public NexusCore() {
        this.config = AppConfig.getInstance();
        this.eventBus = MultimodalEventBus.getInstance();

        // 1. Initialize Subsystems
        this.interactionRepo = new InteractionRepository();
        this.userProfileRepo = new UserProfileRepository();
        this.llmService = new LlmService();
        this.promptBuilder = new PromptContextBuilder();
        this.personalizationEngine = new PersonalizationEngine(interactionRepo, userProfileRepo, llmService);

        this.visionService = new VisionService();
        this.speechService = new SpeechService();

        // 2. Wire Event Bus Subscriptions
        registerEventHandlers();
    }

    public void start() {
        System.out.println("=================================================");
        System.out.println("   INITIALIZING PROJECT N.E.X.U.S CENTRAL BRAIN  ");
        System.out.println("   Team STI25CS - Java Multimodal AI Assistant   ");
        System.out.println("=================================================");

        visionService.start();
        speechService.start();
        startTelemetryMonitoring();

        // Speak welcoming announcement
        speechService.speak("Nexus online. Central orchestrator active.", null);
    }

    public void stop() {
        System.out.println("[NexusCore] Shutting down N.E.X.U.S subsystems...");
        visionService.stop();
        speechService.stop();
        telemetryExecutor.shutdownNow();
    }

    private void registerEventHandlers() {
        // Cache latest mood
        eventBus.subscribe(MoodDetectedEvent.class, latestMood::set);

        // Cache and react to gestures
        eventBus.subscribe(GestureDetectedEvent.class, gestureEvent -> {
            latestGesture.set(gestureEvent);
            handleGestureTrigger(gestureEvent);
        });

        // Handle multimodal inputs (Text console, Speech STT)
        eventBus.subscribe(UserInputEvent.class, this::processUserInput);
    }

    private void handleGestureTrigger(GestureDetectedEvent event) {
        if (event.getGesture() == GestureDetectedEvent.Gesture.STOP_PALM) {
            System.out.println("[NexusCore] Open Palm Gesture: Pausing audio/speech.");
        } else if (event.getGesture() == GestureDetectedEvent.Gesture.PEACE) {
            System.out.println("[NexusCore] Peace Gesture: Triggering quick context summary.");
        }
    }

    public void processUserInput(UserInputEvent inputEvent) {
        long startTime = System.currentTimeMillis();
        String userText = inputEvent.getText();

        if (userText == null || userText.isBlank()) return;
        System.out.println("[NexusCore] Processing input: \"" + userText + "\" from " + inputEvent.getSource());

        // Gather real-time multimodal state
        MoodDetectedEvent currentMood = latestMood.get();
        GestureDetectedEvent currentGesture = latestGesture.get();
        List<InteractionEntity> recentHistory = interactionRepo.getRecent(6);

        // Build prompt with adaptive user profile and perception
        List<ChatMessage> promptPayload = promptBuilder.buildContext(
                userText,
                personalizationEngine.getCurrentProfile(),
                currentMood,
                currentGesture,
                recentHistory
        );

        // Dispatch to LLM reasoning engine
        llmService.generateResponseAsync(promptPayload).thenAccept(reply -> {
            long latencyMs = System.currentTimeMillis() - startTime;
            String moodName = (currentMood != null) ? currentMood.getEmotion().name() : "NEUTRAL";
            String gestureName = (currentGesture != null) ? currentGesture.getGesture().name() : "NONE";

            // Publish response for UI HUD
            AssistantResponseEvent responseEvent = AssistantResponseEvent.success(reply, moodName, latencyMs);
            eventBus.publishOnFxThread(responseEvent);

            // Synthesize voice via TTS
            speechService.speak(reply, null);

            // Log interaction into SQLite and feed personalization engine
            personalizationEngine.processInteraction(
                    userText,
                    reply,
                    inputEvent.getSource().name(),
                    moodName,
                    gestureName,
                    latencyMs
            );
        }).exceptionally(ex -> {
            System.err.println("[NexusCore] Reasoning failed: " + ex.getMessage());
            eventBus.publishOnFxThread(AssistantResponseEvent.error("Processing exception: " + ex.getMessage()));
            return null;
        });
    }

    private void startTelemetryMonitoring() {
        telemetryExecutor.scheduleAtFixedRate(() -> {
            try {
                double cpu = osBean.getCpuLoad() * 100.0;
                if (cpu < 0) cpu = 4.5; // fallback if initial sampling

                long totalMem = Runtime.getRuntime().totalMemory() / (1024 * 1024);
                long freeMem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
                long usedMem = totalMem - freeMem;

                double fps = visionService.getCurrentFps();
                double audioLevel = speechService.getLiveAudioLevel();

                MoodDetectedEvent m = latestMood.get();
                String moodStr = (m != null) ? m.getEmotion().name() : "NEUTRAL";

                TelemetryUpdateEvent telemetry = new TelemetryUpdateEvent(
                        cpu, usedMem, totalMem, fps, audioLevel,
                        speechService.isListening(), speechService.isSpeaking(), moodStr
                );
                eventBus.publishOnFxThread(telemetry);
            } catch (Exception e) {
                // Ignore telemetry sampling blips
            }
        }, 500, 300, TimeUnit.MILLISECONDS);
    }

    // Getters for subsystems
    public VisionService getVisionService() { return visionService; }
    public SpeechService getSpeechService() { return speechService; }
    public LlmService getLlmService() { return llmService; }
    public PersonalizationEngine getPersonalizationEngine() { return personalizationEngine; }
    public InteractionRepository getInteractionRepo() { return interactionRepo; }
    public UserProfileRepository getUserProfileRepo() { return userProfileRepo; }
}
