package com.nexus.ui;

import com.nexus.core.AppConfig;
import com.nexus.core.MultimodalEventBus;
import com.nexus.core.NexusCore;
import com.nexus.core.events.*;
import com.nexus.personalization.TeammateProfile;
import com.nexus.personalization.UserProfile;
import com.nexus.vision.EyeStateClassifier;
import com.nexus.vision.VideoFrame;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Primary JavaFX HUD Controller for Project N.E.X.U.S.
 * Assembles chat streams, camera viewports, audio visualizer,
 * teammate identity recognition, gender detection, drowsiness alerts,
 * and live system telemetry.
 */
public class HudController {

    private final NexusCore core;
    private final MultimodalEventBus eventBus;

    // Root Container
    private final BorderPane rootPane = new BorderPane();

    // UI Nodes
    private final VBox chatMessagesBox = new VBox(10);
    private final ScrollPane chatScrollPane = new ScrollPane();
    private final TextField inputTextField = new TextField();
    private final Button sendButton = new Button("TRANSMIT");
    private final Button micButton = new Button("MIC ACTIVE");

    // Perceptual Viewports
    private CameraViewportCanvas cameraCanvas;
    private AudioVisualizerCanvas audioCanvas;

    // Telemetry Cards
    private TelemetryMeter cpuMeter;
    private TelemetryMeter ramMeter;
    private TelemetryMeter motionMeter;
    private TelemetryMeter moodMeter;

    // Personalization Inspector Labels
    private Label userProfileLabel;
    private Label preferredToneLabel;
    private TextArea behavioralSummaryArea;
    private Label topTopicsLabel;
    private Label interactionCountLabel;

    public HudController(NexusCore core) {
        this.core = core;
        this.eventBus = MultimodalEventBus.getInstance();
        buildUI();
        registerEventSubscriptions();
    }

    public Pane getRoot() {
        return rootPane;
    }

    private void buildUI() {
        rootPane.getStyleClass().add("hud-root");
        rootPane.setPadding(new Insets(12));

        // 1. Top Header Bar
        rootPane.setTop(createHeader());

        // 2. Center Chat & Audio Visualizer Pane
        rootPane.setCenter(createChatSection());

        // 3. Right Sidebar: Vision HUD + Teammate Selector + Personalization Inspector + Telemetry
        rootPane.setRight(createRightSidebar());
    }

    private Node createHeader() {
        HBox header = new HBox(16);
        header.getStyleClass().add("hud-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 14, 12, 14));

        VBox titleBox = new VBox(2);
        Label title = new Label("PROJECT N.E.X.U.S");
        title.getStyleClass().add("hud-title");

        Label subtitle = new Label("PERSONAL REAL-TIME MULTIMODAL ASSISTANT • TEAM STI25CS");
        subtitle.getStyleClass().add("hud-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status Badges
        Label coreStatus = new Label("CORE: ONLINE");
        coreStatus.getStyleClass().addAll("hud-status-badge", "hud-status-badge-active");

        Label visionStatus = new Label("VISION: LIVE WEBCAM");
        visionStatus.getStyleClass().add("hud-status-badge");

        Label speechStatus = new Label("SPEECH: LIVE MIC");
        speechStatus.getStyleClass().add("hud-status-badge");

        Label engineStatus = new Label("ADAPTIVE: ACTIVE");
        engineStatus.getStyleClass().addAll("hud-status-badge", "hud-status-badge-active");

        header.getChildren().addAll(titleBox, spacer, coreStatus, visionStatus, speechStatus, engineStatus);
        return header;
    }

    private Node createChatSection() {
        VBox chatContainer = new VBox(10);
        chatContainer.getStyleClass().addAll("hud-panel", "chat-container");
        chatContainer.setPadding(new Insets(14));
        BorderPane.setMargin(chatContainer, new Insets(8, 8, 8, 0));

        // Title
        Label chatTitle = new Label("REAL-TIME CONVERSATIONAL STREAM");
        chatTitle.getStyleClass().add("section-title");

        // Scrollable Chat Message Area
        chatMessagesBox.setFillWidth(true);
        chatScrollPane.setContent(chatMessagesBox);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.getStyleClass().add("chat-scroll");
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);

        // Seed welcome greeting
        chatMessagesBox.getChildren().add(new ChatMessageCell(
                "N.E.X.U.S online. Central Java Orchestrator active. Live camera perception, gender analysis, optical motion detection, and teammate recognition active. How can I assist you today?",
                false, "FOCUSED", 18
        ));

        // Real-Time Audio Visualizer Canvas
        audioCanvas = new AudioVisualizerCanvas(580, 48);

        // Input Box & Controls
        HBox inputBar = new HBox(8);
        inputBar.setAlignment(Pos.CENTER);

        inputTextField.setPromptText("Enter your query or prompt here (e.g. 'Who is our team?', 'Check eye state', 'Status')...");
        inputTextField.getStyleClass().add("hud-text-field");
        HBox.setHgrow(inputTextField, Priority.ALWAYS);

        sendButton.getStyleClass().add("hud-button");
        micButton.getStyleClass().addAll("hud-button-secondary", "hud-button-mic-active");

        // Transmit actions
        inputTextField.setOnAction(e -> handleSendMessage());
        sendButton.setOnAction(e -> handleSendMessage());

        // Mic toggle action
        micButton.setOnAction(e -> {
            if (core.getSpeechService().getLiveMicService().isRecording()) {
                core.getSpeechService().getLiveMicService().stopCapture();
                micButton.setText("MIC MUTED");
                micButton.getStyleClass().remove("hud-button-mic-active");
            } else {
                core.getSpeechService().getLiveMicService().startCapture(chunk -> {});
                micButton.setText("MIC ACTIVE");
                micButton.getStyleClass().add("hud-button-mic-active");
            }
        });

        inputBar.getChildren().addAll(inputTextField, sendButton, micButton);

        // Gesture Action Shortcut Toolbar
        HBox gestureToolbar = new HBox(8);
        gestureToolbar.setAlignment(Pos.CENTER_LEFT);
        Label gestLabel = new Label("GESTURE SHORTCUTS:");
        gestLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-weight: bold;");

        Button btnThumbs = new Button("Confirm");
        btnThumbs.getStyleClass().add("hud-button-secondary");
        btnThumbs.setOnAction(e -> core.getVisionService().getGestureClassifier().triggerManualGesture(GestureDetectedEvent.Gesture.THUMBS_UP));

        Button btnStop = new Button("Mute / Pause");
        btnStop.getStyleClass().add("hud-button-secondary");
        btnStop.setOnAction(e -> core.getVisionService().getGestureClassifier().triggerManualGesture(GestureDetectedEvent.Gesture.STOP_PALM));

        Button btnPeace = new Button("Summarize");
        btnPeace.getStyleClass().add("hud-button-secondary");
        btnPeace.setOnAction(e -> core.getVisionService().getGestureClassifier().triggerManualGesture(GestureDetectedEvent.Gesture.PEACE));

        gestureToolbar.getChildren().addAll(gestLabel, btnThumbs, btnStop, btnPeace);

        chatContainer.getChildren().addAll(chatTitle, chatScrollPane, audioCanvas, inputBar, gestureToolbar);
        return chatContainer;
    }

    private Node createRightSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(430);
        BorderPane.setMargin(sidebar, new Insets(8, 0, 8, 8));

        // 1. Live Vision Feed Panel
        VBox visionPanel = new VBox(6);
        visionPanel.getStyleClass().add("hud-panel");
        visionPanel.setPadding(new Insets(10));

        Label visionTitle = new Label("VISUAL PERCEPTION HUD (CAMERA / GENDER / MOTION)");
        visionTitle.getStyleClass().add("section-title");

        cameraCanvas = new CameraViewportCanvas(406, 210);

        // Teammate Switcher Toolbar
        VBox teamBox = new VBox(4);
        Label teamTitle = new Label("RECOGNIZE TEAM MEMBER (STI25CS):");
        teamTitle.setStyle("-fx-font-size: 9px; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");

        HBox teamButtons = new HBox(4);
        teamButtons.setAlignment(Pos.CENTER_LEFT);

        for (TeammateProfile t : TeammateProfile.getAllTeammates()) {
            Button b = new Button(t.getName().split(" ")[0]);
            b.getStyleClass().add("hud-button-secondary");
            b.setStyle("-fx-font-size: 10px; -fx-padding: 4px 8px;");
            b.setOnAction(e -> selectTeammate(t));
            teamButtons.getChildren().add(b);
        }
        teamBox.getChildren().addAll(teamTitle, teamButtons);

        // Perception & Eye Controls
        HBox perceptionControls = new HBox(5);
        perceptionControls.setAlignment(Pos.CENTER_LEFT);

        Button btnHappy = new Button("Happy");
        btnHappy.getStyleClass().add("hud-button-secondary");
        btnHappy.setStyle("-fx-font-size: 10px; -fx-padding: 4px 6px;");
        btnHappy.setOnAction(e -> core.getVisionService().getEmotionClassifier().setManualEmotionOverride(MoodDetectedEvent.Emotion.HAPPY, 0.94));

        Button btnStressed = new Button("Stressed");
        btnStressed.getStyleClass().add("hud-button-secondary");
        btnStressed.setStyle("-fx-font-size: 10px; -fx-padding: 4px 6px;");
        btnStressed.setOnAction(e -> core.getVisionService().getEmotionClassifier().setManualEmotionOverride(MoodDetectedEvent.Emotion.STRESSED, 0.91));

        Button btnEyesOpen = new Button("Eyes Open");
        btnEyesOpen.getStyleClass().add("hud-button-secondary");
        btnEyesOpen.setStyle("-fx-font-size: 10px; -fx-padding: 4px 6px;");
        btnEyesOpen.setOnAction(e -> core.getVisionService().getEyeClassifier().setManualOverride(EyeStateClassifier.EyeStatus.OPEN, 60000));

        Button btnEyesClosed = new Button("Drowsy Alert");
        btnEyesClosed.getStyleClass().add("hud-button-secondary");
        btnEyesClosed.setStyle("-fx-font-size: 10px; -fx-padding: 4px 6px; -fx-border-color: #ef4444; -fx-text-fill: #f87171;");
        btnEyesClosed.setOnAction(e -> {
            core.getVisionService().getEyeClassifier().setManualOverride(EyeStateClassifier.EyeStatus.CLOSED, 15000);
            core.getSpeechService().speak("Attention! Drowsiness detected. Please take a rest or stretch.", null);
        });

        perceptionControls.getChildren().addAll(btnHappy, btnStressed, btnEyesOpen, btnEyesClosed);
        visionPanel.getChildren().addAll(visionTitle, cameraCanvas, teamBox, perceptionControls);

        // 2. Adaptive Personalization Inspector Panel
        VBox profilePanel = new VBox(6);
        profilePanel.getStyleClass().add("hud-panel");
        profilePanel.setPadding(new Insets(10));

        HBox profileHeader = new HBox(8);
        profileHeader.setAlignment(Pos.CENTER_LEFT);
        Label profileTitle = new Label("ADAPTIVE PERSONALIZATION INSPECTOR");
        profileTitle.getStyleClass().add("section-title");
        Region profSpacer = new Region();
        HBox.setHgrow(profSpacer, Priority.ALWAYS);

        Button synthButton = new Button("Re-Synthesize");
        synthButton.getStyleClass().add("hud-button-secondary");
        synthButton.setStyle("-fx-font-size: 10px; -fx-padding: 4px 8px;");
        synthButton.setOnAction(e -> core.getPersonalizationEngine().triggerBehavioralSynthesisAsync());
        profileHeader.getChildren().addAll(profileTitle, profSpacer, synthButton);

        UserProfile currentProfile = core.getPersonalizationEngine().getCurrentProfile();

        userProfileLabel = new Label("Recognized: " + core.getVisionService().getActiveTeammate().getName());
        userProfileLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #00f2fe; -fx-font-size: 11px;");

        preferredToneLabel = new Label("Inferred Demeanor: " + currentProfile.getPreferredTone());
        preferredToneLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 10px;");

        topTopicsLabel = new Label("Focus: " + core.getVisionService().getActiveTeammate().getRole());
        topTopicsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");

        interactionCountLabel = new Label("Interaction Turns Logged: " + currentProfile.getTotalInteractions());
        interactionCountLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 9px;");

        behavioralSummaryArea = new TextArea(currentProfile.getBehavioralSummary());
        behavioralSummaryArea.setWrapText(true);
        behavioralSummaryArea.setEditable(false);
        behavioralSummaryArea.setPrefRowCount(2);
        behavioralSummaryArea.setStyle("-fx-background-color: rgba(7, 10, 19, 0.7); -fx-text-fill: #e2e8f0; -fx-font-size: 10px;");

        profilePanel.getChildren().addAll(profileHeader, userProfileLabel, preferredToneLabel, topTopicsLabel, interactionCountLabel, behavioralSummaryArea);

        // 3. System Telemetry Grid
        GridPane telemetryGrid = new GridPane();
        telemetryGrid.setHgap(6);
        telemetryGrid.setVgap(6);

        cpuMeter = new TelemetryMeter("CPU LOAD", "4.8%");
        ramMeter = new TelemetryMeter("RAM USAGE", "64 MB");
        motionMeter = new TelemetryMeter("OPTICAL MOTION", "12% [STABLE]");
        moodMeter = new TelemetryMeter("DETECTED MOOD", "FOCUSED");

        telemetryGrid.add(cpuMeter, 0, 0);
        telemetryGrid.add(ramMeter, 1, 0);
        telemetryGrid.add(motionMeter, 0, 1);
        telemetryGrid.add(moodMeter, 1, 1);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        telemetryGrid.getColumnConstraints().addAll(col1, col2);

        sidebar.getChildren().addAll(visionPanel, profilePanel, telemetryGrid);
        return sidebar;
    }

    private void selectTeammate(TeammateProfile teammate) {
        core.getVisionService().setActiveTeammate(teammate);

        // Update profile in memory
        UserProfile profile = core.getPersonalizationEngine().getCurrentProfile();
        profile.setUserName(teammate.getName());
        profile.setPreferredTone(teammate.getPreferredTone());
        profile.setTopTopics(teammate.getInterestClusters());
        profile.setBehavioralSummary(teammate.getName() + " (" + teammate.getStudentId() + ") - Focus area: " + teammate.getRole() + ".");

        // Speak welcoming announcement
        core.getSpeechService().speak(teammate.getWelcomePhrase(), null);

        // Update UI
        userProfileLabel.setText("Recognized: " + teammate.getName());
        preferredToneLabel.setText("Inferred Demeanor: " + teammate.getPreferredTone());
        topTopicsLabel.setText("Focus: " + teammate.getRole());
        behavioralSummaryArea.setText(profile.getBehavioralSummary());

        // Add greeting message to chat
        chatMessagesBox.getChildren().add(new ChatMessageCell(
                teammate.getWelcomePhrase(),
                false, "FOCUSED", 12
        ));
        chatScrollPane.setVvalue(1.0);
    }

    private void handleSendMessage() {
        String text = inputTextField.getText().trim();
        if (text.isEmpty()) return;

        inputTextField.clear();
        chatMessagesBox.getChildren().add(new ChatMessageCell(text, true, null, 0));
        chatScrollPane.setVvalue(1.0);

        eventBus.publish(new UserInputEvent(text, UserInputEvent.InputSource.TEXT));
    }

    private void registerEventSubscriptions() {
        // Assistant Reply -> Add to chat stream
        eventBus.subscribe(AssistantResponseEvent.class, replyEvent -> {
            Platform.runLater(() -> {
                chatMessagesBox.getChildren().add(new ChatMessageCell(
                        replyEvent.getResponseText(),
                        false,
                        replyEvent.getMoodContext(),
                        replyEvent.getLatencyMs()
                ));
                chatScrollPane.setVvalue(1.0);
            });
        });

        // Vision Frame -> Update camera viewport
        eventBus.subscribe(VideoFrame.class, frame -> {
            Platform.runLater(() -> cameraCanvas.updateFrame(frame));
        });

        // Mood / Perception Detected -> Update reticle and telemetry
        eventBus.subscribe(MoodDetectedEvent.class, moodEvent -> {
            Platform.runLater(() -> {
                cameraCanvas.updateMood(moodEvent);
                moodMeter.updateTextOnly(moodEvent.getEmotion().name() + " (" + moodEvent.getFormattedConfidence() + ")");
                double motionPercent = moodEvent.getMotionLevel() * 100;
                String motionStatus = (motionPercent > 35) ? "ACTIVE" : "STABLE";
                motionMeter.update(String.format("%.0f%% [%s]", motionPercent, motionStatus), moodEvent.getMotionLevel());
            });
        });

        // Gesture Detected -> Update reticle
        eventBus.subscribe(GestureDetectedEvent.class, gestureEvent -> {
            Platform.runLater(() -> cameraCanvas.updateGesture(gestureEvent));
        });

        // Telemetry Update -> Update meters and audio visualizer
        eventBus.subscribe(TelemetryUpdateEvent.class, telem -> {
            Platform.runLater(() -> {
                cpuMeter.update(String.format("%.1f%%", telem.getCpuUsagePercent()), telem.getCpuUsagePercent() / 100.0);
                ramMeter.update(telem.getMemoryUsedMB() + " MB", (double) telem.getMemoryUsedMB() / telem.getMemoryTotalMB());
                audioCanvas.setAudioLevel(telem.getAudioLevel());
            });
        });

        // User Profile Updated -> Update Adaptive Personalization Inspector
        eventBus.subscribe(UserProfile.class, profile -> {
            Platform.runLater(() -> {
                userProfileLabel.setText("User: " + profile.getUserName());
                preferredToneLabel.setText("Inferred Demeanor: " + profile.getPreferredTone());
                topTopicsLabel.setText("Interest Clusters: " + profile.getTopTopics());
                interactionCountLabel.setText("Interaction Turns Logged: " + profile.getTotalInteractions());
                behavioralSummaryArea.setText(profile.getBehavioralSummary());
            });
        });
    }
}
