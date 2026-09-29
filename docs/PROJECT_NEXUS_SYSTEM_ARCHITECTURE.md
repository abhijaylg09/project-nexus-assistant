# PROJECT N.E.X.U.S: SYSTEM ARCHITECTURE SPECIFICATION
**A Personal Real-Time Multimodal AI Assistant Built Around a Unified Java Core**

* **Project Title:** PROJECT N.E.X.U.S (Neural EXecutive User System)
* **Academic Batch / Stream:** STI25CS
* **Project Team:**
  - Bhadra G. S. (Roll No: 48, ID: STI25CS048)
  - Aleena Maria Roy (Roll No: 26, ID: STI25CS026)
  - Abhishek A. (Roll No: 09, ID: STI25CS009)
  - Dia M. Joby (Roll No: 52, ID: STI25CS052)
  - Abhijay L. G.
* **Core Technology:** Java 21 LTS (Central Orchestrator)

---

## 1. Executive Summary & Architectural Philosophy

Traditional virtual assistants (e.g., Alexa, Siri, Cortana, Google Assistant) are either:
1. **Cloud-tethered, closed/black-box monolithic services** with minimal local perception and zero explainable user modeling.
2. **Fragmented multi-stack prototypes** (e.g., disparate Python scripts for computer vision, Node.js servers for WebSockets, and separate desktop widgets) resulting in brittle IPC, thread contention, and high latency.

**Project N.E.X.U.S** demonstrates that **a single, high-performance Java core can act as the centralized "brain"** orchestrating real-time vision (mood and gesture detection), speech (wake word, STT, TTS), deep language reasoning (LLM API via HTTP/2), persistence (JDBC/SQLite), and an ultra-responsive JavaFX HUD interface.

```
+===================================================================================+
|                               PROJECT N.E.X.U.S                                   |
|                     UNIFIED JAVA ORCHESTRATION ARCHITECTURE                       |
+===================================================================================+
                                         |
     +-----------------------------------+-----------------------------------+
     |                                   |                                   |
     v                                   v                                   v
[ INPUT PERCEPTION ]             [ CENTRAL BRAIN ]                   [ OUTPUT / ACTIONS ]
--------------------             -----------------                   --------------------
* Camera Feed (OpenCV)           * NexusCore (Java)                  * JavaFX HUD Display
* Facial Emotion (ONNX)          * MultimodalEventBus                * Active Chat Stream
* Gesture Classifier             * PromptContextBuilder              * Audio Visualizer
* Vosk STT Audio Stream          * PersonalizationEngine             * SAPI / Piper TTS
* Porcupine Wake Detector        * SQLite Persistence (JDBC)         * Gesture Trigger Execution
```

---

## 2. Multi-Tier Subsystem Specifications

### 2.1 Central Orchestrator & Logic Layer (`com.nexus.core`)
* **`NexusCore.java`**: Lifecycle supervisor. Boots all background threads, maintains state synchronicity, and handles multimodal arbitration.
* **`MultimodalEventBus.java`**: Decoupled, asynchronous event broker implementing publish-subscribe patterns. Allows cross-module communication without cyclic dependencies.
* **Event Types**:
  - `UserInputEvent`: User speech transcript or console keyboard input.
  - `MoodDetectedEvent`: 6-class emotion (`NEUTRAL`, `HAPPY`, `SURPRISED`, `SAD`, `STRESSED`, `FOCUSED`) with bounding box and confidence score.
  - `GestureDetectedEvent`: Physical gestures (`THUMBS_UP`, `STOP_PALM`, `PEACE`, `POINT_UP`).
  - `AssistantResponseEvent`: Assistant reply, speech synthesis flag, and round-trip latency.
  - `TelemetryUpdateEvent`: Real-time CPU, RAM, vision FPS, and audio levels.

### 2.2 Visual Perception Layer (`com.nexus.vision`)
* **Webcam Capture**: Direct hardware feed captured via OpenCV / JavaCV bindings (`VideoCapture`). Supports fallback to an animated cybernetic sensor canvas if hardware cameras are disconnected or in use.
* **Facial Emotion Recognition**:
  - OpenCV Haar Cascade (`haarcascade_frontalface_default.xml`) isolates face coordinates $(x, y, w, h)$.
  - Pre-trained ONNX classification model (`emotion-ferplus.onnx`) executed via the native **ONNX Runtime Java API** (`ai.onnxruntime.OrtEnvironment`).
* **Hand Gesture Recognition**:
  - Convex hull and contour tracking or lightweight ONNX hand landmark model classifying dynamic user shortcuts:
    - *Open Palm*: Immediate pause / audio mute.
    - *Thumbs Up*: Acknowledge / confirm active suggestion.
    - *Peace Sign*: Trigger immediate context summarization.

### 2.3 Speech I/O Subsystem (`com.nexus.speech`)
* **Wake Word Engine**: Low-latency Porcupine Java SDK architecture monitoring acoustic stream for trigger phrases (`"Nexus"`, `"Hey Nexus"`).
* **Speech-to-Text (STT)**: Java-native Vosk acoustic library (`com.alphacephei:vosk`), running offline at 16 kHz mono.
* **Text-to-Speech (TTS)**: Dual-path synthesis:
  - *Primary*: Fast subprocess integration with **Piper TTS** (`piper.exe --model en_US-lessac-medium.onnx`).
  - *Zero-Dependency Fallback*: Direct Windows SAPI Speech Synthesizer invoked asynchronously via PowerShell COM automation.

### 2.4 Language Reasoning Layer (`com.nexus.reasoning`)
* **Modern HTTP Client**: Built using Java 21's asynchronous `java.net.http.HttpClient` with HTTP/2 and connection pooling.
* **Target Backends**: OpenAI GPT-4o-mini, Local Ollama (`http://localhost:11434/v1/chat/completions`), Groq, or OpenRouter.
* **Local Heuristic Fallback Engine**: If no API key is specified or internet access drops, N.E.X.U.S seamlessly triggers its internal context-aware rule engine so user evaluation and HUD interactions never freeze or fail.

### 2.5 Persistence & Database Tier (`com.nexus.persistence`)
* **Engine**: Embedded SQLite 3 via JDBC (`org.xerial:sqlite-jdbc`).
* **Tables**:
  - `interactions`: Turn ID, user input, assistant reply, detected mood, gesture, topics, sentiment, latency, timestamp.
  - `user_profile`: Singleton user model containing name, behavioral summary, preferred tone, top topic clusters, total turns.

### 2.6 Adaptive Personalization Engine (`com.nexus.personalization`)
* **Continuous Interaction Logging**: Evaluates real-time sentiment score ($-1.0$ to $+1.0$) and keyword interest clusters on each turn.
* **Periodic Behavioral Synthesis**: Every $N$ interactions (default: 10), the engine invokes an asynchronous LLM task that analyzes recent batches and synthesizes a high-level natural language description of user cadence, mood shifts, and communication preferences.
* **Context Window Injection**: On every prompt generation, `PromptContextBuilder` injects the synthesized profile directly into the system prompt, dynamically adapting tone, brevity, and empathy.

### 2.7 Frontend HUD Interface (`com.nexus.ui`)
* **Framework**: JavaFX 21 with hardware-accelerated Canvas rendering.
* **Design Aesthetic**: Futuristic Cyberpunk / Glassmorphic HUD inspired by sci-fi tactical interfaces.
  - Deep space palette (`#070a13`, `#0d1629`), electric cyan (`#00f2fe`), ultraviolet purple (`#9b51e0`), and luminous green (`#00ff87`).
  - Animated live audio visualizer reacting to mic input and TTS output.
  - Target reticle overlays and face bounding boxes drawn directly on camera viewport.
  - Real-time telemetry gauges (CPU load, heap RAM, camera FPS, active emotion).
  - Adaptive Personalization Inspector providing total transparency into what the assistant has learned about the user.

---

## 3. Concurrency & Threading Model

```
[ OS Hardware ]
       |
       +---> [ Camera Thread (Daemon) ] --------> OpenCV / ONNX -> Mood/Gesture Event
       |
       +---> [ Audio In Thread (Daemon) ] ------> Vosk STT / Porcupine -> Input Event
       |
       +---> [ NexusCore Orchestrator ] --------> Event Bus Dispatch
       |             |
       |             +---> [ HTTP LLM Worker (ForkJoinPool) ] -> Reply Event
       |             |
       |             +---> [ TTS Thread (ExecutorService) ] ----> Piper/SAPI Audio
       |             |
       |             +---> [ Personalization Worker ] ----------> LLM Profile Synthesis -> SQLite
       |
       +---> [ JavaFX Application Thread ] -----> 60 FPS HUD Render Loop (Platform.runLater)
```

1. **Non-Blocking UI**: The JavaFX Application Thread strictly handles layout, Canvas repainting, and user input capture. Heavy vision processing, audio streaming, and network I/O are executed on dedicated background daemon pools.
2. **Thread Safety**: Cross-thread communication uses `Platform.runLater()` and thread-safe queues inside `MultimodalEventBus`.
3. **Graceful Shutdown**: All thread pools are registered with a Java shutdown hook and `Stage.setOnCloseRequest()` to guarantee no orphaned subprocesses or open camera handles.
