# PROJECT N.E.X.U.S: VIVA & PRESENTATION DEFENSE GUIDE
**Academic / Seminar Evaluation Q&A Handbook**

* **Project:** PROJECT N.E.X.U.S (Neural EXecutive User System)
* **Team STI25CS:** Abhijay L. G., Bhadra G. S., Aleena Maria Roy, Abhishek A., Dia M. Joby
* **Core Language:** Java 21 LTS + Python Deep Learning AI Core

---

## 1. Top Viva Examination Questions & Model Answers

### Q1: "Why did you build the core orchestrator in Java instead of Python?"
> **Model Answer:**  
> "Most AI projects use Python for rapid prototyping, but Python faces fundamental constraints in real-time desktop orchestration:
> 1. **The Global Interpreter Lock (GIL)** severely throttles true parallel multi-threading when managing live video feeds, audio streams, and UI updates concurrently.
> 2. **Process Fragmentation:** In Python, developers often create separate scripts running on different ports, introducing inter-process communication (IPC) overhead and instability.
> 
> In N.E.X.U.S, Java serves as the **central brain**. Through JNI and C++ bindings (JavaCV, ONNX Runtime, Vosk JNI), Java coordinates hardware threads with zero GIL contention, manages memory predictably through the JVM, and renders a smooth 60 FPS hardware-accelerated HUD interface with JavaFX."

---

### Q2: "How does N.E.X.U.S process multimodal inputs without UI freezing?"
> **Model Answer:**  
> "We implement a strict separation of concerns through our **`MultimodalEventBus`**:
> - The **JavaFX Application Thread** is strictly reserved for UI layout and 60 FPS Canvas rendering.
> - The **Vision Service** runs on a scheduled background thread at 30 FPS capturing frames and executing ONNX inference.
> - The **Speech Service** captures audio and wake words on a dedicated daemon thread.
> - Cross-thread events are posted into the EventBus, which safely routes updates to the UI via `Platform.runLater()`. This guarantees zero UI lag even during intensive AI processing."

---

### Q3: "Explain how the Adaptive Personalization Engine works."
> **Model Answer:**  
> "Instead of relying on rigid rule-based counters, N.E.X.U.S utilizes a **closed-loop qualitative synthesis loop**:
> 1. Every turn (user input, mood, gesture, sentiment) is persisted to SQLite via JDBC.
> 2. Every $N$ interactions (default: 10), a background worker pulls recent interaction logs and instructs the LLM to synthesize a concise, 2-sentence behavioral summary of the user's communication style and preferred tone.
> 3. This profile is saved in SQLite and dynamically injected into the system prompt of future requests via `PromptContextBuilder`.
> 4. This allows N.E.X.U.S to adapt its demeanor (e.g. switching to concise, encouraging answers when the user is stressed) without inflating prompt token counts."

---

### Q4: "How do you prevent prompt bloat and runaway token costs in the LLM?"
> **Model Answer:**  
> "A common problem in naive memory systems is appending all past chats to the prompt, which quickly causes context window explosion. N.E.X.U.S prevents this through **bounded context aggregation**:
> - We only pass the last 6 turns for immediate conversational flow.
> - Long-term memory is represented exclusively by the **synthesized 2-sentence behavioral profile**, maintaining a constant token footprint ($\approx 120\text{ tokens}$) regardless of whether the user has interacted 10 times or 10,000 times."

---

### Q5: "How does N.E.X.U.S handle real-world webcam accuracy constraints?"
> **Model Answer:**  
> "Webcam lighting, angles, and occlusions naturally cause fluctuations in facial emotion accuracy. We address this through:
> 1. **Temporal Smoothing & Confidence Thresholding:** Emotions are evaluated over moving temporal windows rather than raw single-frame spikes.
> 2. **Graceful Fallbacks:** If the webcam is unavailable or in use, N.E.X.U.S activates its internal simulated neural sensor feed so all HUD overlays remain fully functional.
> 3. **Manual Simulation Overrides:** For testing and defense demonstrations, the HUD includes quick-action buttons allowing evaluators to verify how N.E.X.U.S responds to specific moods (Happy, Stressed, Focused) and gestures (Thumbs-up, Stop-palm, Peace)."

---

## 2. Team Member Role Allocation & Presentation Breakdown

| Team Member | Presentation Focus / Slide Topics | Key Technical Talking Point |
|---|---|---|
| **Abhijay L. G.** | Python AI Core, Central Orchestrator & Adaptive Personalization | Explain FERPlus & ViT neural microservice, event bus thread safety, and closed-loop personalization |
| **Bhadra G. S.** | Introduction, Project Ideation, & Problem Statement | Explain how unified Java core eliminates fragmented architectures |
| **Aleena Maria Roy** | Computer Vision (OpenCV & ONNX Runtime) | Discuss face detection cascade, ONNX emotion inference, and gesture shortcuts |
| **Abhishek A.** | Speech I/O Subsystem (Vosk STT, Piper TTS, Porcupine) | Detail low-latency wake word triggers and dual-path speech synthesis |
| **Dia M. Joby** | Frontend HUD Interface & JavaFX Architecture | Showcase the HUD design, audio visualizer canvas, and real-time telemetry |
