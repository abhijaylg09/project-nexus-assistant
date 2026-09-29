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
import com.nexus.system.AppLauncherService;
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
    private final com.nexus.reasoning.VisionReasoningEngine visionReasoningEngine;
    private final PromptContextBuilder promptBuilder;
    private final InteractionRepository interactionRepo;
    private final UserProfileRepository userProfileRepo;
    private final PersonalizationEngine personalizationEngine;
    private final AppLauncherService appLauncher;

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
        this.visionReasoningEngine = new com.nexus.reasoning.VisionReasoningEngine();
        this.promptBuilder = new PromptContextBuilder();
        this.personalizationEngine = new PersonalizationEngine(interactionRepo, userProfileRepo, llmService);

        this.visionService = new VisionService();
        this.speechService = new SpeechService();
        this.appLauncher = new AppLauncherService();

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

    private GestureDetectedEvent.Gesture lastTriggeredGesture = GestureDetectedEvent.Gesture.NONE;
    private long lastGestureTriggerTime = 0;

    private void handleGestureTrigger(GestureDetectedEvent event) {
        if (event == null || event.getGesture() == GestureDetectedEvent.Gesture.NONE) {
            return;
        }

        long now = System.currentTimeMillis();
        // Debounce: only fire if gesture changed or 3 seconds passed
        if (event.getGesture() == lastTriggeredGesture && (now - lastGestureTriggerTime < 3000)) {
            return;
        }

        lastTriggeredGesture = event.getGesture();
        lastGestureTriggerTime = now;

        if (event.getGesture() == GestureDetectedEvent.Gesture.STOP_PALM) {
            System.out.println("[NexusCore] Open Palm Gesture: Pausing audio/speech.");
        } else if (event.getGesture() == GestureDetectedEvent.Gesture.PEACE) {
            System.out.println("[NexusCore] Peace Gesture: Triggering quick context summary.");
        } else if (event.getGesture() == GestureDetectedEvent.Gesture.THUMBS_UP) {
            System.out.println("[NexusCore] Thumbs Up Gesture: Action confirmed.");
        }
    }

    public void processUserInput(UserInputEvent inputEvent) {
        long startTime = System.currentTimeMillis();
        String userText = inputEvent.getText();

        if (userText == null || userText.isBlank()) return;
        System.out.println("[NexusCore] Processing input: \"" + userText + "\" from " + inputEvent.getSource());

        // ── App Launch Intent Detection ──
        String appLaunchTarget = AppLauncherService.extractAppLaunchIntent(userText);
        if (appLaunchTarget != null && !inputEvent.hasAttachedImage()) {
            System.out.println("[NexusCore] Detected App Launch intent: \"" + appLaunchTarget + "\"");
            handleAppLaunchRequest(appLaunchTarget, inputEvent, startTime);
            return;
        }

        // Gather real-time multimodal state
        MoodDetectedEvent currentMood = latestMood.get();
        GestureDetectedEvent currentGesture = latestGesture.get();
        List<InteractionEntity> recentHistory = interactionRepo.getRecent(6);

        // If user submitted an image doubt / inspection request
        if (inputEvent.hasAttachedImage()) {
            String activeName = (currentMood != null && currentMood.getRecognizedIdentity() != null)
                    ? currentMood.getRecognizedIdentity()
                    : personalizationEngine.getCurrentProfile().getUserName();

            java.util.concurrent.CompletableFuture.supplyAsync(() -> visionReasoningEngine.analyzeImageAndAnswer(
                    inputEvent.getAttachedImageFile(),
                    userText,
                    activeName
            )).thenAccept(reply -> {
                long latencyMs = System.currentTimeMillis() - startTime;
                String moodName = (currentMood != null) ? currentMood.getEmotion().name() : "FOCUSED";
                String gestureName = (currentGesture != null) ? currentGesture.getGesture().name() : "NONE";

                eventBus.publishOnFxThread(AssistantResponseEvent.success(reply, moodName, latencyMs));
                speechService.speak("Visual analysis completed. I have processed your image inquiry.", null);

                personalizationEngine.processInteraction(
                        "[Attached Image: " + inputEvent.getAttachedImageFile().getName() + "] " + userText,
                        reply,
                        "IMAGE_INQUIRY",
                        moodName,
                        gestureName,
                        latencyMs
                );
            }).exceptionally(ex -> {
                System.err.println("[NexusCore] Vision reasoning error: " + ex.getMessage());
                eventBus.publishOnFxThread(AssistantResponseEvent.error("Visual analysis exception: " + ex.getMessage()));
                return null;
            });
            return;
        }

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
    public AppLauncherService getAppLauncher() { return appLauncher; }

    // ── App Launch Handler ──
    private void handleAppLaunchRequest(String appName, UserInputEvent inputEvent, long startTime) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            AppLauncherService.LaunchResult result = appLauncher.launchApp(appName);
            long latencyMs = System.currentTimeMillis() - startTime;

            MoodDetectedEvent currentMood = latestMood.get();
            GestureDetectedEvent currentGesture = latestGesture.get();
            String moodName = (currentMood != null) ? currentMood.getEmotion().name() : "FOCUSED";
            String gestureName = (currentGesture != null) ? currentGesture.getGesture().name() : "NONE";

            String responseText;
            if (result.isSuccess()) {
                String roast = getThugAppRoast(result.getAppName());
                responseText = "✅ " + result.getMessage()
                        + "\n😎 *N.E.X.U.S Thug Verdict:* " + roast
                        + " (Running on " + AppLauncherService.getPlatformDisplayName() + ")";
                speechService.speak(result.getAppName() + " launched. " + roast, null);
            } else {
                responseText = "⚠️ " + result.getMessage()
                        + "\n\nAvailable apps I can open: "
                        + String.join(", ", appLauncher.getAvailableApps().subList(0, Math.min(12, appLauncher.getAvailableApps().size())))
                        + ", and more.";
                speechService.speak("Sorry, I could not launch " + appName + ". " + result.getMessage(), null);
            }

            eventBus.publishOnFxThread(AssistantResponseEvent.success(responseText, moodName, latencyMs));

            personalizationEngine.processInteraction(
                    inputEvent.getText(),
                    responseText,
                    "APP_LAUNCH",
                    moodName,
                    gestureName,
                    latencyMs
            );
        });
    }

    private String getThugAppRoast(String appName) {
        if (appName == null) return "Even clicking an app icon was too much cardio for you, huh? Thug life!";
        String lower = appName.toLowerCase();
        if (lower.contains("youtube")) {
            return "Don't waste the whole day watching brainrot reels now, okay? Scene mone!";
        } else if (lower.contains("calc")) {
            return "Finally decided to calculate how broke you are? Good luck with that.";
        } else if (lower.contains("file") || lower.contains("explorer")) {
            return "Looking for where you hid your questionable downloads? Don't worry, your secrets are safe with me.";
        } else if (lower.contains("chrome") || lower.contains("edge") || lower.contains("browser")) {
            return "Please don't search for 'how to get a life' again. Google can't help with that.";
        } else if (lower.contains("camera")) {
            return "Warning: Look at your own risk. The camera doesn't come with beauty filters!";
        } else if (lower.contains("notepad")) {
            return "Opening Notepad. Gonna write your groundbreaking startup idea that gets abandoned tomorrow?";
        } else if (lower.contains("code") || lower.contains("vscode")) {
            return "VS Code opened. Ready to write 3 lines of code and spend 4 hours debugging a missing semicolon?";
        } else if (lower.contains("spotify")) {
            return "Playing music to cope with your compilation errors? Respect the hustle.";
        } else {
            return "Even clicking an app icon was too much cardio for you, huh? Thug life!";
        }
    }
}
