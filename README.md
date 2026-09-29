# PROJECT N.E.X.U.S: A Personal Real-Time AI Assistant

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue.svg)](https://openjfx.io/)
[![SQLite](https://img.shields.io/badge/SQLite-JDBC-003B57.svg)](https://www.sqlite.org/)
[![Architecture](https://img.shields.io/badge/Architecture-Unified%20Core-green.svg)](#system-architecture)

**PROJECT N.E.X.U.S (Neural EXecutive User System)** is a software-based personal AI desktop assistant built around a **centralized Java orchestration core**. It seamlessly integrates computer vision (real-time facial mood detection & hand gestures), speech processing (STT & TTS), large language model (LLM) reasoning, embedded persistence, and an adaptive personalization engine into a high-performance, sci-fi cyberpunk Heads-Up Display (HUD).

---

## 👥 Project Team (Batch STI25CS)

| Name | Roll No | Student ID | Focus Area |
|---|---|---|---|
| **Bhadra G. S.** | 48 | STI25CS048 | Core Ideation, Architecture & Problem Statement |
| **Aleena Maria Roy** | 26 | STI25CS026 | Computer Vision, OpenCV & ONNX Emotion Modeling |
| **Abhishek A.** | 09 | STI25CS009 | Speech I/O (Vosk STT, Piper TTS, Porcupine Wake) |
| **Dia M. Joby** | 52 | STI25CS052 | JavaFX HUD Interface & Audio Visualizer |
| **Abhijay L. G.** | 121 | — | Central Java Orchestration & Adaptive Personalization |

---

## 🚀 Key Features

1. **Unified Java-Orchestrated Core ("The Brain")**:
   - Maintains centralized Java control over all multimodal pipelines instead of relying on fragmented multi-stack scripts.
   - Built on a decoupled, thread-safe `MultimodalEventBus` ensuring 60 FPS JavaFX rendering without UI freezes.

2. **Visual Environmental Awareness (OpenCV & ONNX)**:
   - **Facial Emotion Recognition**: Tracks user face and classifies 6 emotional states (`FOCUSED`, `HAPPY`, `STRESSED`, `NEUTRAL`, `SURPRISED`, `SAD`) with real-time confidence metrics.
   - **Hand Gesture Shortcuts**: Classifies physical gestures (`Thumbs-Up` $\rightarrow$ Confirm, `Open Palm` $\rightarrow$ Mute/Pause, `Peace` $\rightarrow$ Summarize).
   - Dynamic simulation fallback with cybernetic reticles and face tracking if no hardware camera is present.

3. **Speech I/O & Dual-Path Synthesis**:
   - **Wake Word Detection**: Low-latency keyword trigger (`"Nexus"`, `"Hey Nexus"`).
   - **Speech-to-Text (STT)**: Offline Vosk acoustic transcription.
   - **Text-to-Speech (TTS)**: Piper neural TTS subprocess with zero-dependency Windows SAPI speech synthesis fallback for immediate voice output.

4. **Adaptive Personalization Engine (Closed-Loop Learning)**:
   - Logs user input, emotional state, gesture, sentiment valence ($-1.0$ to $+1.0$), and latency into SQLite.
   - Periodically prompts the LLM to synthesize a natural-language behavioral summary from interaction batches.
   - Dynamically injects the synthesized profile into the LLM context window to tailor the assistant's demeanor to the user's personality over time.

5. **Cyberpunk Tactical HUD Interface (JavaFX)**:
   - Live camera viewport with glowing targeting reticles and facial bounding boxes.
   - Real-time animated audio visualizer reacting to mic input and speech synthesis.
   - Interactive chat stream with sentiment badges, mood tags, and latency indicators.
   - Explainable Personalization Inspector with instant re-synthesis controls.
   - System telemetry gauges (CPU load, RAM usage, Vision FPS, Mood telemetry).

---

## 📁 Project Directory Structure

```
nexus-ai-assistant/
├── pom.xml                                // Maven configuration (JavaFX, SQLite, Jackson, JNA)
├── build.bat                              // Quick compiler script
├── run.bat                                // Instant launcher script
├── config/
│   └── nexus-config.json                  // System, LLM, Vision, & Speech parameters
├── docs/
│   ├── PROJECT_NEXUS_SYSTEM_ARCHITECTURE.md // Complete architectural & concurrency specifications
│   ├── SRS_SPECIFICATION.md               // IEEE 830-compliant Software Requirements Specification
│   ├── ADAPTIVE_PERSONALIZATION_METHODOLOGY.md // Research report on closed-loop behavioral learning
│   └── VIVA_PRESENTATION_DEFENSE_GUIDE.md // Viva examination Q&A handbook & defense notes
├── src/main/
│   ├── java/com/nexus/
│   │   ├── NexusApp.java                  // JavaFX HUD Main Application
│   │   ├── NexusLauncher.java             // Bootstrap entry point
│   │   ├── core/                          // Central brain & event bus
│   │   │   ├── NexusCore.java
│   │   │   ├── AppConfig.java
│   │   │   ├── MultimodalEventBus.java
│   │   │   └── events/
│   │   ├── vision/                        // Computer vision, OpenCV/ONNX, gestures
│   │   │   ├── VisionService.java
│   │   │   ├── EmotionClassifier.java
│   │   │   ├── GestureClassifier.java
│   │   │   └── VideoFrame.java
│   │   ├── speech/                        // Vosk STT, Piper/SAPI TTS, Wake Word
│   │   │   ├── SpeechService.java
│   │   │   ├── VoskSttEngine.java
│   │   │   ├── PiperTtsEngine.java
│   │   │   └── WakeWordDetector.java
│   │   ├── reasoning/                     // LLM Client & Context injection
│   │   │   ├── LlmService.java
│   │   │   ├── PromptContextBuilder.java
│   │   │   └── ChatMessage.java
│   │   ├── personalization/               // Closed-loop behavioral learning
│   │   │   ├── PersonalizationEngine.java
│   │   │   └── UserProfile.java
│   │   ├── persistence/                   // SQLite JDBC storage
│   │   │   ├── DatabaseManager.java
│   │   │   ├── InteractionEntity.java
│   │   │   ├── InteractionRepository.java
│   │   │   └── UserProfileRepository.java
│   │   └── ui/                            // JavaFX HUD Viewports & Controls
│   │       ├── HudController.java
│   │       ├── AudioVisualizerCanvas.java
│   │       ├── CameraViewportCanvas.java
│   │       ├── ChatMessageCell.java
│   │       └── TelemetryMeter.java
│   └── resources/
│       ├── application.properties
│       └── styles/
│           └── hud-cyberpunk.css          // Futuristic sci-fi styling
```

---

## ⚡ Quick Start & Execution

### 1. Requirements
- **JDK 21** (Pre-installed: Microsoft Build of OpenJDK 21)
- **Apache Maven 3.9+** (Configured in tools folder)

### 2. Run with One Click
Simply double-click:
```bat
run.bat
```
Or execute in PowerShell / Terminal:
```powershell
.\run.bat
```

### 3. Compile from Source
```bat
build.bat
```
Or using Maven:
```powershell
mvn clean compile
mvn javafx:run
```

---

## ⚙️ Configuration (`config/nexus-config.json`)

To connect to cloud or local LLM backends:

```json
{
  "llm": {
    "provider": "openai-compatible",
    "endpoint": "https://api.openai.com/v1/chat/completions",
    "apiKey": "YOUR_OPENAI_API_KEY",
    "model": "gpt-4o-mini",
    "temperature": 0.7,
    "mockFallbackEnabled": true
  }
}
```

* **Local Ollama Support**: Set `"endpoint": "http://localhost:11434/v1/chat/completions"` and `"model": "llama3.2"`.
* **Zero-Setup Offline Fallback**: If no API key is set, N.E.X.U.S automatically engages its intelligent local heuristic response engine, allowing offline testing and viva demonstrations without network dependencies!

---

## 📚 Technical Documentation

Detailed documentation has been compiled in the [`docs/`](file:///C:/Users/abhij/.gemini/antigravity-ide/scratch/nexus-ai-assistant/docs) directory:
- [System Architecture Specification](file:///C:/Users/abhij/.gemini/antigravity-ide/scratch/nexus-ai-assistant/docs/PROJECT_NEXUS_SYSTEM_ARCHITECTURE.md)
- [Software Requirements Specification (SRS - IEEE 830)](file:///C:/Users/abhij/.gemini/antigravity-ide/scratch/nexus-ai-assistant/docs/SRS_SPECIFICATION.md)
- [Adaptive Personalization Methodology](file:///C:/Users/abhij/.gemini/antigravity-ide/scratch/nexus-ai-assistant/docs/ADAPTIVE_PERSONALIZATION_METHODOLOGY.md)
- [Viva & Presentation Defense Guide](file:///C:/Users/abhij/.gemini/antigravity-ide/scratch/nexus-ai-assistant/docs/VIVA_PRESENTATION_DEFENSE_GUIDE.md)
