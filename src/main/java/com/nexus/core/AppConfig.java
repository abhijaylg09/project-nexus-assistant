package com.nexus.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.InputStream;

/**
 * Manages configuration for Project N.E.X.U.S across all multi-modal subsystems.
 */
public class AppConfig {

    private static AppConfig instance;

    private String assistantName = "N.E.X.U.S";
    private String systemRole = "You are N.E.X.U.S (Neural EXecutive User System), a personal real-time multimodal AI assistant.";
    private String wakeWord = "nexus";

    // LLM Config
    private String llmEndpoint = "https://api.openai.com/v1/chat/completions";
    private String llmApiKey = "";
    private String llmModel = "gpt-4o-mini";
    private double llmTemperature = 0.7;
    private int llmMaxTokens = 600;
    private boolean mockFallbackEnabled = true;

    // Vision Config
    private int webcamId = 0;
    private int visionFps = 30;
    private String faceDetectionCascade = "haarcascade_frontalface_default.xml";
    private String emotionModelPath = "assets/models/emotion-ferplus.onnx";
    private double gestureConfidenceThreshold = 0.65;
    private boolean simulationModeIfNoCamera = true;

    // Speech Config
    private String voskModelPath = "assets/models/vosk-model-small-en-us-0.15";
    private float audioSampleRate = 16000.0f;
    private String piperExecutable = "piper/piper.exe";
    private String piperModelPath = "piper/en_US-lessac-medium.onnx";
    private String ttsEngine = "sapi_or_piper";

    // Persistence & Personalization
    private String sqliteUrl = "jdbc:sqlite:nexus_assistant.db";
    private int synthesisInterval = 10;
    private int maxInteractionsAnalyze = 50;

    private AppConfig() {
        loadConfig();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    private void loadConfig() {
        ObjectMapper mapper = new ObjectMapper();
        File configFile = new File("config/nexus-config.json");

        try {
            JsonNode root = null;
            if (configFile.exists()) {
                root = mapper.readTree(configFile);
            } else {
                InputStream is = getClass().getClassLoader().getResourceAsStream("config/nexus-config.json");
                if (is != null) {
                    root = mapper.readTree(is);
                }
            }

            if (root != null) {
                if (root.has("assistant")) {
                    JsonNode asst = root.get("assistant");
                    if (asst.has("name")) assistantName = asst.get("name").asText(assistantName);
                    if (asst.has("systemRole")) systemRole = asst.get("systemRole").asText(systemRole);
                    if (asst.has("wakeWord")) wakeWord = asst.get("wakeWord").asText(wakeWord);
                }

                if (root.has("llm")) {
                    JsonNode llm = root.get("llm");
                    if (llm.has("endpoint")) llmEndpoint = llm.get("endpoint").asText(llmEndpoint);
                    if (llm.has("apiKey")) llmApiKey = llm.get("apiKey").asText(llmApiKey);
                    if (llm.has("model")) llmModel = llm.get("model").asText(llmModel);
                    if (llm.has("temperature")) llmTemperature = llm.get("temperature").asDouble(llmTemperature);
                    if (llm.has("maxTokens")) llmMaxTokens = llm.get("maxTokens").asInt(llmMaxTokens);
                    if (llm.has("mockFallbackEnabled")) mockFallbackEnabled = llm.get("mockFallbackEnabled").asBoolean(true);
                }

                if (root.has("vision")) {
                    JsonNode vis = root.get("vision");
                    if (vis.has("webcamId")) webcamId = vis.get("webcamId").asInt(webcamId);
                    if (vis.has("fps")) visionFps = vis.get("fps").asInt(visionFps);
                    if (vis.has("simulationModeIfNoCamera")) simulationModeIfNoCamera = vis.get("simulationModeIfNoCamera").asBoolean(true);
                }

                if (root.has("speech")) {
                    JsonNode sp = root.get("speech");
                    if (sp.has("voskModelPath")) voskModelPath = sp.get("voskModelPath").asText(voskModelPath);
                    if (sp.has("ttsEngine")) ttsEngine = sp.get("ttsEngine").asText(ttsEngine);
                }

                if (root.has("database")) {
                    JsonNode db = root.get("database");
                    if (db.has("sqliteUrl")) sqliteUrl = db.get("sqliteUrl").asText(sqliteUrl);
                }

                if (root.has("personalization")) {
                    JsonNode pers = root.get("personalization");
                    if (pers.has("synthesisIntervalInteractions")) synthesisInterval = pers.get("synthesisIntervalInteractions").asInt(10);
                }
            }
        } catch (Exception e) {
            System.err.println("[AppConfig] Notice: Using internal defaults (" + e.getMessage() + ")");
        }

        // Environment overrides
        String envKey = System.getenv("OPENAI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            llmApiKey = envKey.trim();
        }
        String groqKey = System.getenv("GROQ_API_KEY");
        if (groqKey != null && !groqKey.isBlank() && (llmApiKey == null || llmApiKey.isBlank() || llmApiKey.contains("YOUR_API_KEY"))) {
            llmApiKey = groqKey.trim();
            llmEndpoint = "https://api.groq.com/openai/v1/chat/completions";
            llmModel = "llama-3.3-70b-versatile";
        }
        String envEndpoint = System.getenv("LLM_ENDPOINT");
        if (envEndpoint != null && !envEndpoint.isBlank()) {
            llmEndpoint = envEndpoint.trim();
        }
        String envModel = System.getenv("LLM_MODEL");
        if (envModel != null && !envModel.isBlank()) {
            llmModel = envModel.trim();
        }
    }

    // Getters and Setters
    public String getAssistantName() { return assistantName; }
    public String getSystemRole() { return systemRole; }
    public String getWakeWord() { return wakeWord; }

    public String getLlmEndpoint() { return llmEndpoint; }
    public void setLlmEndpoint(String llmEndpoint) { this.llmEndpoint = llmEndpoint; }
    public String getLlmApiKey() { return llmApiKey; }
    public void setLlmApiKey(String llmApiKey) { this.llmApiKey = llmApiKey; }
    public String getLlmModel() { return llmModel; }
    public void setLlmModel(String llmModel) { this.llmModel = llmModel; }
    public double getLlmTemperature() { return llmTemperature; }
    public int getLlmMaxTokens() { return llmMaxTokens; }
    public boolean isMockFallbackEnabled() { return mockFallbackEnabled; }

    public int getWebcamId() { return webcamId; }
    public int getVisionFps() { return visionFps; }
    public String getFaceDetectionCascade() { return faceDetectionCascade; }
    public String getEmotionModelPath() { return emotionModelPath; }
    public double getGestureConfidenceThreshold() { return gestureConfidenceThreshold; }
    public boolean isSimulationModeIfNoCamera() { return simulationModeIfNoCamera; }

    public String getVoskModelPath() { return voskModelPath; }
    public float getAudioSampleRate() { return audioSampleRate; }
    public String getPiperExecutable() { return piperExecutable; }
    public String getPiperModelPath() { return piperModelPath; }
    public String getTtsEngine() { return ttsEngine; }

    public String getSqliteUrl() { return sqliteUrl; }
    public int getSynthesisInterval() { return synthesisInterval; }
    public int getMaxInteractionsAnalyze() { return maxInteractionsAnalyze; }
}
