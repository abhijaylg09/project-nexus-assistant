# SOFTWARE REQUIREMENTS SPECIFICATION (SRS)
## PROJECT N.E.X.U.S: A Personal Real-Time Multimodal AI Assistant

* **Standard:** Conforms to IEEE Std 830-1998 (Recommended Practice for Software Requirements Specifications)
* **Project Team:** Team STI25CS
  - Abhijay L. G. (STI25CS121) — Python AI Perception Core & Orchestration
  - Bhadra G. S. (STI25CS048) — Core Ideation & Architecture
  - Aleena Maria Roy (STI25CS026) — Computer Vision Subsystem
  - Abhishek A. (STI25CS009) — Speech I/O Subsystem
  - Dia M. Joby (STI25CS052) — JavaFX HUD & Visualizer
* **Target Version:** 1.0.0
* **Date:** 2026-09-29

---

## 1. Introduction

### 1.1 Purpose
This document specifies the software requirements for **N.E.X.U.S (Neural EXecutive User System)**, a real-time, desktop-based personal AI assistant. N.E.X.U.S coordinates computer vision, speech recognition, speech synthesis, and large language model (LLM) reasoning through a centralized Java orchestration architecture.

### 1.2 Scope
N.E.X.U.S provides:
- Continuous multimodal environmental awareness (facial emotion detection and physical hand gesture recognition).
- Hands-free and console-based conversational interaction via Vosk STT, Piper/SAPI TTS, and LLM APIs.
- Long-term adaptive personalization through interaction logging and LLM-driven behavioral synthesis.
- A futuristic JavaFX HUD displaying live camera overlays, audio waveforms, telemetry gauges, and an explainable user-modeling inspector.

### 1.3 Definitions, Acronyms, and Abbreviations
- **HUD:** Heads-Up Display.
- **LLM:** Large Language Model.
- **STT:** Speech-to-Text.
- **TTS:** Text-to-Speech.
- **ONNX:** Open Neural Network Exchange.
- **JDBC:** Java Database Connectivity.
- **SAPI:** Speech Application Programming Interface (Windows Native).

---

## 2. Overall Description

### 2.1 Product Perspective
N.E.X.U.S is a self-contained desktop software application. Unlike fragmented architectures that rely on separate Python/Node.js background processes, N.E.X.U.S utilizes a unified Java core where specialized models (OpenCV, ONNX Runtime, Vosk) are embedded directly or coordinated as callable services.

### 2.2 User Characteristics
The target user is an engineer, researcher, student, or power user who requires a responsive desktop companion capable of understanding both explicit queries (text/voice) and implicit context (facial mood and gestures).

### 2.3 Operating Environment
- **Operating System:** Windows 10/11 (x64), Linux (x64), macOS (Apple Silicon / Intel).
- **Runtime:** Java Runtime Environment (JRE/JDK) 21 LTS.
- **Input Peripherals:** Standard USB/built-in webcam, microphone.
- **Output Peripherals:** Audio output speakers/headphones, 1080p display.

---

## 3. Specific Functional Requirements

### 3.1 Speech Subsystem (FR-01)
* **FR-01.1 (Wake Word Detection):** The system shall listen for the wake phrase `"Nexus"` or `"Hey Nexus"` with latency $< 300\text{ ms}$.
* **FR-01.2 (Speech-to-Text):** The system shall transcribe speech to text using offline Vosk acoustic models or simulated mic input.
* **FR-01.3 (Voice Synthesis):** The system shall synthesize responses via Piper neural TTS or Windows SAPI with audio visualizer synchronization.

### 3.2 Computer Vision Subsystem (FR-02)
* **FR-02.1 (Live Video Capture):** The system shall capture frames from webcam 0 at $\ge 24\text{ FPS}$ with graceful degradation to simulated HUD frames if unavailable.
* **FR-02.2 (Facial Emotion Recognition):** The system shall detect faces and classify emotional states (`FOCUSED`, `HAPPY`, `STRESSED`, `NEUTRAL`, `SURPRISED`, `SAD`) with confidence metrics.
* **FR-02.3 (Gesture Classification):** The system shall detect hand gestures (`THUMBS_UP`, `STOP_PALM`, `PEACE`) mapped to system actions (Acknowledge, Mute, Summarize).

### 3.3 Reasoning & Context Orchestration (FR-03)
* **FR-03.1 (Context Construction):** The system shall compile system directives, adaptive user persona, real-time facial mood, recent gestures, and conversation history into an OpenAI-compatible payload.
* **FR-03.2 (LLM Interaction):** The system shall dispatch requests via Java HTTP/2 to the configured endpoint (OpenAI, Ollama, Groq) with an intelligent offline fallback.

### 3.4 Adaptive Personalization Engine (FR-04)
* **FR-04.1 (Interaction Logging):** Every interaction turn shall be persisted into SQLite with user query, response, sentiment score, detected mood, and latency.
* **FR-04.2 (Behavioral Synthesis):** Every $N$ interactions (default: 10), the system shall trigger background LLM analysis to synthesize user communication style and interest clusters.
* **FR-04.3 (Profile Transparency):** The JavaFX HUD shall provide an inspector displaying the current inferred user profile.

---

## 4. Non-Functional Requirements

### 4.1 Performance Requirements
* **Turnaround Latency:** Local heuristic responses shall be delivered in $< 150\text{ ms}$. Cloud LLM responses shall stream or complete within $< 2.5\text{ s}$ depending on network latency.
* **UI Responsiveness:** The JavaFX render loop shall maintain $60\text{ FPS}$ independently of background vision or LLM tasks.

### 4.2 Security & Privacy Requirements
* **Local Data Sovereignty:** All interaction logs, user profiles, and camera feeds are stored and processed locally in SQLite; raw webcam video is never transmitted over the network.
* **Transparent Modeling:** The user has full visibility into stored behavioral profiles and can trigger re-synthesis at will.

### 4.3 Reliability & Fault Tolerance
* If the external LLM endpoint is unreachable, N.E.X.U.S shall engage its local heuristic engine without crashing or locking the UI.
* If no webcam or microphone is detected, N.E.X.U.S shall transition to simulated sensor mode and text console input.
