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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

/**
 * Primary JavaFX HUD Controller for Project N.E.X.U.S.
 * Assembles chat streams, camera viewports, audio visualizer,
 * teammate identity recognition, optical gender detection & calibration,
 * optical eye closure & drowsiness alerts, multimodal image doubt Q&A,
 * and high-accuracy microphone voice chat.
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
    private final Button voiceChatButton = new Button("🎙️ VOICE CHAT");
    private final Button micToggleButton = new Button("MIC ON");

    // Image Attachment / Doubt Support
    private File currentAttachedImageFile = null;
    private HBox attachmentPreviewBar;
    private ImageView attachmentThumbView;
    private Label attachmentNameLabel;

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

        // 2. Central Split: Left Chat Stream + Right Vision & Telemetry HUD
        HBox mainSplit = new HBox(12);
        mainSplit.setAlignment(Pos.TOP_LEFT);

        Node chatSection = createChatSection();
        Node rightSidebar = createRightSidebar();

        HBox.setHgrow(chatSection, Priority.ALWAYS);
        mainSplit.getChildren().addAll(chatSection, rightSidebar);
        rootPane.setCenter(mainSplit);
    }

    private Node createHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 12, 12, 12));
        header.getStyleClass().add("hud-header");

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

        Label visionStatus = new Label("VISION: CAMERA ACTIVE");
        visionStatus.getStyleClass().add("hud-status-badge");

        // AI Model Configuration Dialog Button
        Button aiSettingsBtn = new Button("⚙️ AI Model / Key");
        aiSettingsBtn.getStyleClass().add("hud-button-secondary");
        aiSettingsBtn.setOnAction(e -> openAiSettingsDialog());

        header.getChildren().addAll(titleBox, spacer, coreStatus, visionStatus, aiSettingsBtn);
        return header;
    }

    private Node createChatSection() {
        VBox chatContainer = new VBox(8);
        chatContainer.getStyleClass().addAll("hud-panel", "chat-container");
        chatContainer.setPadding(new Insets(12));
        BorderPane.setMargin(chatContainer, new Insets(8, 8, 8, 0));

        // Title
        Label chatTitle = new Label("REAL-TIME CONVERSATIONAL STREAM (CHATGPT-LEVEL MULTIMODAL REASONING)");
        chatTitle.getStyleClass().add("section-title");

        // Scrollable Chat Message Area
        chatMessagesBox.setFillWidth(true);
        chatScrollPane.setContent(chatMessagesBox);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.getStyleClass().add("chat-scroll");
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);

        // Welcome greeting
        chatMessagesBox.getChildren().add(new ChatMessageCell(
                "N.E.X.U.S online. Central Java Orchestrator active. Live optical eye tracking, gender biometrics, and multimodal image inspection ready. Ask anything, click 'Attach Image' to send a doubt, or press 'Voice Chat' to speak!",
                false, "FOCUSED", 18
        ));

        // Real-Time Audio Visualizer Canvas
        audioCanvas = new AudioVisualizerCanvas(580, 48);

        // Attachment Preview Banner
        attachmentPreviewBar = new HBox(8);
        attachmentPreviewBar.setAlignment(Pos.CENTER_LEFT);
        attachmentPreviewBar.setPadding(new Insets(4, 10, 4, 10));
        attachmentPreviewBar.setStyle("-fx-background-color: #111d33; -fx-border-color: #00f2fe; -fx-border-radius: 4px; -fx-background-radius: 4px;");
        attachmentPreviewBar.setVisible(false);
        attachmentPreviewBar.setManaged(false);

        attachmentThumbView = new ImageView();
        attachmentThumbView.setFitWidth(48);
        attachmentThumbView.setFitHeight(36);
        attachmentThumbView.setPreserveRatio(true);

        attachmentNameLabel = new Label();
        attachmentNameLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: bold;");

        Button removeAttachBtn = new Button("✖ Remove");
        removeAttachBtn.getStyleClass().add("hud-button-secondary");
        removeAttachBtn.setStyle("-fx-font-size: 9px; -fx-padding: 2px 6px; -fx-border-color: #ef4444; -fx-text-fill: #f87171;");
        removeAttachBtn.setOnAction(e -> clearAttachedImage());

        Region attachSpacer = new Region();
        HBox.setHgrow(attachSpacer, Priority.ALWAYS);
        attachmentPreviewBar.getChildren().addAll(attachmentThumbView, attachmentNameLabel, attachSpacer, removeAttachBtn);

        // Input Box & Controls
        HBox inputBar = new HBox(6);
        inputBar.setAlignment(Pos.CENTER);

        inputTextField.setPromptText("Ask anything like ChatGPT or ask doubts about an image...");
        inputTextField.getStyleClass().add("hud-text-field");
        HBox.setHgrow(inputTextField, Priority.ALWAYS);

        Button attachImgBtn = new Button("🖼️ Image");
        attachImgBtn.getStyleClass().add("hud-button-secondary");
        attachImgBtn.setStyle("-fx-font-size: 10px; -fx-padding: 5px 8px;");
        attachImgBtn.setTooltip(new Tooltip("Attach an image file to ask questions or doubts"));
        attachImgBtn.setOnAction(e -> handleAttachImage());

        Button snapCamBtn = new Button("📸 Snap");
        snapCamBtn.getStyleClass().add("hud-button-secondary");
        snapCamBtn.setStyle("-fx-font-size: 10px; -fx-padding: 5px 8px;");
        snapCamBtn.setTooltip(new Tooltip("Snap current camera frame to ask doubts about it"));
        snapCamBtn.setOnAction(e -> handleSnapCamera());

        sendButton.getStyleClass().add("hud-button");
        voiceChatButton.getStyleClass().addAll("hud-button", "hud-button-mic-active");
        micToggleButton.getStyleClass().add("hud-button-secondary");

        // Transmit text action
        inputTextField.setOnAction(e -> handleSendMessage());
        sendButton.setOnAction(e -> handleSendMessage());

        // Live Voice Chat Button Action
        voiceChatButton.setOnAction(e -> handleVoiceChatButton());

        // Mic toggle action
        micToggleButton.setOnAction(e -> {
            if (core.getSpeechService().getLiveMicService().isRecording()) {
                core.getSpeechService().getLiveMicService().stopCapture();
                micToggleButton.setText("MIC OFF");
                micToggleButton.setStyle("-fx-border-color: #ef4444; -fx-text-fill: #f87171;");
            } else {
                core.getSpeechService().getLiveMicService().startCapture(chunk -> {});
                micToggleButton.setText("MIC ON");
                micToggleButton.setStyle("");
            }
        });

        inputBar.getChildren().addAll(inputTextField, attachImgBtn, snapCamBtn, sendButton, voiceChatButton, micToggleButton);

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

        chatContainer.getChildren().addAll(chatTitle, chatScrollPane, audioCanvas, attachmentPreviewBar, inputBar, gestureToolbar);
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

        HBox visionHeader = new HBox(8);
        visionHeader.setAlignment(Pos.CENTER_LEFT);
        Label visionTitle = new Label("VISUAL PERCEPTION HUD (CAMERA / BIOMETRICS)");
        visionTitle.getStyleClass().add("section-title");
        Region vSpacer = new Region();
        HBox.setHgrow(vSpacer, Priority.ALWAYS);

        Button scanFaceBtn = new Button("📸 Scan & Identify Face");
        scanFaceBtn.getStyleClass().add("hud-button");
        scanFaceBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3px 8px;");
        scanFaceBtn.setOnAction(e -> handleScanFaceTrigger());
        visionHeader.getChildren().addAll(visionTitle, vSpacer, scanFaceBtn);

        cameraCanvas = new CameraViewportCanvas(406, 210);

        // Teammate Switcher Toolbar
        VBox teamBox = new VBox(4);
        Label teamTitle = new Label("RECOGNIZE TEAM MEMBER (CAMERA / SELECTION):");
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

        // Row 1: Emotion & Optical Eye Controls
        HBox eyeControls = new HBox(4);
        eyeControls.setAlignment(Pos.CENTER_LEFT);
        Label eyeCtrlLabel = new Label("EYES & EMOTION:");
        eyeCtrlLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #64748b; -fx-font-weight: bold;");

        Button btnHappy = new Button("Happy");
        btnHappy.getStyleClass().add("hud-button-secondary");
        btnHappy.setStyle("-fx-font-size: 9px; -fx-padding: 3px 6px;");
        btnHappy.setOnAction(e -> core.getVisionService().getEmotionClassifier().setManualEmotionOverride(MoodDetectedEvent.Emotion.HAPPY, 0.94));

        Button btnStressed = new Button("Stressed");
        btnStressed.getStyleClass().add("hud-button-secondary");
        btnStressed.setStyle("-fx-font-size: 9px; -fx-padding: 3px 6px;");
        btnStressed.setOnAction(e -> core.getVisionService().getEmotionClassifier().setManualEmotionOverride(MoodDetectedEvent.Emotion.STRESSED, 0.91));

        Button btnEyesBlink = new Button("👁️ Blink");
        btnEyesBlink.getStyleClass().add("hud-button-secondary");
        btnEyesBlink.setStyle("-fx-font-size: 9px; -fx-padding: 3px 6px;");
        btnEyesBlink.setOnAction(e -> core.getVisionService().getEyeClassifier().setManualOverride(EyeStateClassifier.EyeStatus.CLOSED, 400));

        Button btnDrowsyAlert = new Button("⚠️ Drowsy Alert");
        btnDrowsyAlert.getStyleClass().add("hud-button-secondary");
        btnDrowsyAlert.setStyle("-fx-font-size: 9px; -fx-padding: 3px 6px; -fx-border-color: #ef4444; -fx-text-fill: #f87171;");
        btnDrowsyAlert.setOnAction(e -> {
            core.getVisionService().getEyeClassifier().setManualOverride(EyeStateClassifier.EyeStatus.CLOSED, 15000);
            core.getSpeechService().speak("Attention! Drowsiness detected. Please take a rest or stretch.", null);
        });

        Button btnAutoEyes = new Button("Auto");
        btnAutoEyes.getStyleClass().add("hud-button-secondary");
        btnAutoEyes.setStyle("-fx-font-size: 9px; -fx-padding: 3px 6px;");
        btnAutoEyes.setOnAction(e -> core.getVisionService().getEyeClassifier().clearOverride());

        eyeControls.getChildren().addAll(eyeCtrlLabel, btnHappy, btnStressed, btnEyesBlink, btnDrowsyAlert, btnAutoEyes);

        // Row 2: Optical Gender Calibration
        HBox genderControls = new HBox(4);
        genderControls.setAlignment(Pos.CENTER_LEFT);
        Label genderCtrlLabel = new Label("GENDER CALIBRATION:");
        genderCtrlLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #64748b; -fx-font-weight: bold;");

        Button btnMale = new Button("♂ Male");
        btnMale.getStyleClass().add("hud-button-secondary");
        btnMale.setStyle("-fx-font-size: 9px; -fx-padding: 3px 6px; -fx-text-fill: #38bdf8;");
        btnMale.setOnAction(e -> {
            core.getVisionService().getFaceBiometrics().setManualGenderOverride(MoodDetectedEvent.Gender.MALE);
            chatMessagesBox.getChildren().add(new ChatMessageCell("Gender calibrated to MALE.", false, "FOCUSED", 0));
        });

        Button btnFemale = new Button("♀ Female");
        btnFemale.getStyleClass().add("hud-button-secondary");
        btnFemale.setStyle("-fx-font-size: 9px; -fx-padding: 3px 6px; -fx-text-fill: #f472b6;");
        btnFemale.setOnAction(e -> {
            core.getVisionService().getFaceBiometrics().setManualGenderOverride(MoodDetectedEvent.Gender.FEMALE);
            chatMessagesBox.getChildren().add(new ChatMessageCell("Gender calibrated to FEMALE.", false, "FOCUSED", 0));
        });

        Button btnAutoGender = new Button("Auto Optical");
        btnAutoGender.getStyleClass().add("hud-button-secondary");
        btnAutoGender.setStyle("-fx-font-size: 9px; -fx-padding: 3px 6px;");
        btnAutoGender.setOnAction(e -> {
            core.getVisionService().getFaceBiometrics().setManualGenderOverride(null);
            chatMessagesBox.getChildren().add(new ChatMessageCell("Gender set to automatic optical analysis.", false, "FOCUSED", 0));
        });

        genderControls.getChildren().addAll(genderCtrlLabel, btnMale, btnFemale, btnAutoGender);

        visionPanel.getChildren().addAll(visionHeader, cameraCanvas, teamBox, eyeControls, genderControls);

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

        profilePanel.getChildren().addAll(
                profileHeader, userProfileLabel, preferredToneLabel,
                topTopicsLabel, interactionCountLabel, behavioralSummaryArea
        );

        // 3. System Telemetry Panel
        GridPane telemetryGrid = new GridPane();
        telemetryGrid.setHgap(8);
        telemetryGrid.setVgap(8);

        cpuMeter = new TelemetryMeter("CPU LOAD", "%");
        ramMeter = new TelemetryMeter("RAM COMMITTED", "MB");
        motionMeter = new TelemetryMeter("OPTICAL MOTION", "%");
        moodMeter = new TelemetryMeter("CONFIDENCE", "%");

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

    private void handleAttachImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image for Visual Analysis / Doubts");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.bmp, *.gif, *.webp)", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File selectedFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (selectedFile != null && selectedFile.exists()) {
            setAttachedImage(selectedFile);
        }
    }

    private void handleSnapCamera() {
        File snapshot = core.getVisionService().captureSnapshotToFile();
        if (snapshot != null && snapshot.exists()) {
            setAttachedImage(snapshot);
            chatMessagesBox.getChildren().add(new ChatMessageCell("📸 Captured instant webcam snapshot. Type your question or click Voice Chat to ask!", false, "FOCUSED", 0));
            chatScrollPane.setVvalue(1.0);
        }
    }

    private void setAttachedImage(File file) {
        this.currentAttachedImageFile = file;
        try {
            Image img = new Image(file.toURI().toString(), 64, 48, true, true);
            attachmentThumbView.setImage(img);
            attachmentNameLabel.setText("Attached: " + file.getName() + " (" + (file.length() / 1024) + " KB)");
            attachmentPreviewBar.setVisible(true);
            attachmentPreviewBar.setManaged(true);
        } catch (Exception e) {
            System.err.println("Error displaying thumbnail: " + e.getMessage());
        }
    }

    private void clearAttachedImage() {
        this.currentAttachedImageFile = null;
        attachmentThumbView.setImage(null);
        attachmentNameLabel.setText("");
        attachmentPreviewBar.setVisible(false);
        attachmentPreviewBar.setManaged(false);
    }

    private void handleSendMessage() {
        String text = inputTextField.getText().trim();
        File imgToSend = currentAttachedImageFile;

        if (text.isEmpty() && imgToSend == null) return;
        if (text.isEmpty() && imgToSend != null) {
            text = "Please inspect this attached image and explain any findings or answer my questions about it.";
        }

        inputTextField.clear();
        clearAttachedImage();

        chatMessagesBox.getChildren().add(new ChatMessageCell(text, imgToSend, true, null, 0));
        chatScrollPane.setVvalue(1.0);

        eventBus.publish(new UserInputEvent(text, UserInputEvent.InputSource.TEXT, imgToSend));
    }

    private void handleVoiceChatButton() {
        voiceChatButton.setText("⏳ INITIALIZING MIC...");
        voiceChatButton.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: black; -fx-font-weight: bold;");

        core.getSpeechService().listenToVoiceChatAsync(
                transcript -> Platform.runLater(() -> {
                    voiceChatButton.setText("🎙️ VOICE CHAT");
                    voiceChatButton.setStyle("");

                    if (transcript != null && !transcript.isBlank()) {
                        File imgToSend = currentAttachedImageFile;
                        chatMessagesBox.getChildren().add(new ChatMessageCell("🎙️ \"" + transcript + "\"", imgToSend, true, null, 0));
                        chatScrollPane.setVvalue(1.0);
                        clearAttachedImage();
                        eventBus.publish(new UserInputEvent(transcript, UserInputEvent.InputSource.SPEECH, imgToSend));
                    }
                }),
                () -> Platform.runLater(() -> {
                    voiceChatButton.setText("⏳ INITIALIZING MIC...");
                    voiceChatButton.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: black; -fx-font-weight: bold;");
                }),
                () -> Platform.runLater(() -> {
                    voiceChatButton.setText("🔴 SPEAK NOW!");
                    voiceChatButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, #ef4444, 10, 0, 0, 0);");
                }),
                () -> Platform.runLater(() -> {
                    voiceChatButton.setText("🎙️ VOICE CHAT");
                    voiceChatButton.setStyle("");
                })
        );
    }

    private void handleScanFaceTrigger() {
        TeammateProfile identified = core.getVisionService().getActiveTeammate();
        selectTeammate(identified);
        core.getSpeechService().speak("Camera scan completed. Face confirmed as " + identified.getName() + ". Welcome to N.E.X.U.S.", null);
    }

    private void selectTeammate(TeammateProfile teammate) {
        core.getVisionService().setActiveTeammate(teammate);

        // Update profile
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

    private void openAiSettingsDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("AI Model & API Key Settings");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0d1629; -fx-text-fill: white;");

        Label heading = new Label("Configure Cloud / Local LLM Model");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00f2fe;");

        Label desc = new Label("N.E.X.U.S answers all questions offline via deep knowledge reasoning. You can also connect OpenAI, Groq, or Ollama for live cloud intelligence:");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        TextField endpointField = new TextField(AppConfig.getInstance().getLlmEndpoint());
        endpointField.setPromptText("API Endpoint (e.g. https://api.openai.com/v1/chat/completions or http://localhost:11434/v1/chat/completions)");
        endpointField.getStyleClass().add("hud-text-field");

        PasswordField apiKeyField = new PasswordField();
        apiKeyField.setText(AppConfig.getInstance().getLlmApiKey());
        apiKeyField.setPromptText("Enter API Key (OpenAI 'sk-...' or Groq 'gsk_...' or leave blank for Ollama/Offline)");
        apiKeyField.getStyleClass().add("hud-text-field");

        TextField modelField = new TextField(AppConfig.getInstance().getLlmModel());
        modelField.setPromptText("Model Name (e.g. gpt-4o-mini, llama-3.3-70b-versatile, llama3.2)");
        modelField.getStyleClass().add("hud-text-field");

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button saveBtn = new Button("SAVE & APPLY");
        saveBtn.getStyleClass().add("hud-button");
        saveBtn.setOnAction(e -> {
            AppConfig.getInstance().setLlmEndpoint(endpointField.getText().trim());
            AppConfig.getInstance().setLlmApiKey(apiKeyField.getText().trim());
            AppConfig.getInstance().setLlmModel(modelField.getText().trim());
            dialog.close();
            chatMessagesBox.getChildren().add(new ChatMessageCell("AI Model Settings Updated: Model set to " + modelField.getText().trim(), false, "FOCUSED", 0));
        });

        Button closeBtn = new Button("CANCEL");
        closeBtn.getStyleClass().add("hud-button-secondary");
        closeBtn.setOnAction(e -> dialog.close());

        btnRow.getChildren().addAll(closeBtn, saveBtn);

        content.getChildren().addAll(heading, desc, new Label("LLM Endpoint:"), endpointField, new Label("API Key:"), apiKeyField, new Label("Model:"), modelField, btnRow);

        Scene scene = new Scene(content, 480, 360);
        scene.getStylesheets().add(getClass().getResource("/styles/hud-cyberpunk.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
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

        // Perceptual Mood & Biometrics Event -> Update meters and labels
        eventBus.subscribe(MoodDetectedEvent.class, mood -> {
            Platform.runLater(() -> {
                cameraCanvas.updateMood(mood);
                moodMeter.update(String.format("%.0f%%", mood.getConfidence() * 100.0), mood.getConfidence());
                motionMeter.update(String.format("%.0f%%", mood.getMotionLevel() * 100.0), mood.getMotionLevel());
            });
        });

        // Gesture Event -> Update canvas gesture tag
        eventBus.subscribe(GestureDetectedEvent.class, gesture -> {
            Platform.runLater(() -> cameraCanvas.updateGesture(gesture));
        });

        // Telemetry Event -> Update CPU and RAM meters
        eventBus.subscribe(TelemetryUpdateEvent.class, tele -> {
            Platform.runLater(() -> {
                cpuMeter.update(String.format("%.1f%%", tele.getCpuUsagePercent()), tele.getCpuUsagePercent() / 100.0);
                ramMeter.update(tele.getMemoryUsedMB() + " MB", (double) tele.getMemoryUsedMB() / Math.max(1, tele.getMemoryTotalMB()));
                audioCanvas.setAudioLevel(core.getSpeechService().getLiveAudioLevel());
            });
        });
    }
}
