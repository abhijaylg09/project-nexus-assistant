# ADAPTIVE PERSONALIZATION METHODOLOGY & CLOSED-LOOP LEARNING
## PROJECT N.E.X.U.S: Research & Implementation Report

**Team STI25CS:** Bhadra G. S., Aleena Maria Roy, Abhishek A., Dia M. Joby, Team STI25CS

---

## 1. Problem Formulation: The Stagnant Persona Problem

Contemporary conversational assistants operate in a **stateless, memory-impaired vacuum**:
- Every session resets context or relies only on hardcoded toggle switches (e.g. "Formal mode" vs "Casual mode").
- Assistants are oblivious to the user's ongoing emotional state, communication pacing, and long-term subject matter evolution.
- Hand-crafted statistical counters (e.g. counting occurrences of "please" or "thank you") fail to capture nuanced psycholinguistic cadence, sarcasm, urgency, or technical depth.

**Project N.E.X.U.S solves this via a closed-loop Adaptive Personalization Engine** built directly into the centralized Java core.

---

## 2. The N.E.X.U.S Closed-Loop Architecture

```
                                [ User Interaction ]
                                         |
                                         v
                      +------------------------------------+
                      |    Multimodal Feature Capture      |
                      |  - User Query Text                 |
                      |  - Facial Mood (OpenCV / ONNX)     |
                      |  - Hand Gesture Shortcut           |
                      |  - Lexical Sentiment Score         |
                      +------------------------------------+
                                         |
                                         v
                      +------------------------------------+
                      | SQLite Interaction Logging (JDBC)  |
                      |  - Batched persistence of turns    |
                      +------------------------------------+
                                         |
                                (Every N turns)
                                         v
                      +------------------------------------+
                      |  LLM Behavioral Synthesis Engine   |
                      |  - Distills patterns into concise  |
                      |    natural-language profile        |
                      +------------------------------------+
                                         |
                                         v
                      +------------------------------------+
                      |   Evolving User Profile Store      |
                      |  - Inferred Demeanor & Tone        |
                      |  - Top Topic Clusters              |
                      +------------------------------------+
                                         |
                                         v
                      +------------------------------------+
                      |   Context Injection (System Prompt)|
                      |  - Shapes tone, brevity, & empathy |
                      +------------------------------------+
                                         |
                                         v
                             [ Adaptive AI Response ]
```

---

## 3. Mathematical & Algorithmic Formulation

### 3.1 Lexical Sentiment Metric
For each interaction turn $t$, sentiment score $S(t) \in [-1.0, 1.0]$ is computed as:
$$S(t) = \operatorname{clamp}\left(\sum_{w \in W^+} \omega^+(w) - \sum_{w \in W^-} \omega^-(w), -1.0, 1.0\right)$$
where $W^+$ and $W^-$ represent positive and negative valence lexicons weighted by emotional salience.

### 3.2 Moving Average Sentiment Trend
Long-term mood trend $\bar{S}_K$ over a window of $K$ turns is maintained via SQLite aggregation:
$$\bar{S}_K = \frac{1}{K}\sum_{i=0}^{K-1} S(t-i)$$

### 3.3 Frequency-Based Topic Clustering
User queries are tokenized and cross-referenced against technical taxonomy dictionaries:
$$\text{TopicScore}(C) = \sum_{w \in \text{Query}} \mathbb{I}(w \in \text{Keywords}(C))$$
where clusters $C \in \{\text{Software}, \text{Computer Vision}, \text{Speech I/O}, \text{AI Architecture}, \text{Telemetry}\}$.

---

## 4. LLM Behavioral Synthesis: From Metrics to Persona

Rather than writing hundreds of brittle if-else rules to interpret numbers, **the LLM itself is utilized as a qualitative behavioral synthesizer**:

### Synthesis Prompt Template:
```text
Analyze the following recent user interactions with an AI assistant.
Synthesize:
1. A concise, 2-sentence summary of the user's communication style, preferred demeanor, and emotional cadence.
2. A short preferred tone descriptor (e.g., 'Technical & Direct', 'Casual & Friendly', 'Supportive & Analytical').

Format your response strictly as:
SUMMARY: <2 sentences>
TONE: <tone descriptor>

Interactions batch:
- User: "Summarize the vision pipeline" | Mood: FOCUSED | Sentiment: +0.25
- User: "Why is latency 180ms?" | Mood: STRESSED | Sentiment: -0.35
...
```

### Context Injection on Subsequent Turns:
When building the prompt for a new query, `PromptContextBuilder` automatically injects:
```text
=== ADAPTIVE USER PROFILE (LEARNED FROM INTERACTION HISTORY) ===
User Name: Team STI25CS
Communication Style & Tone: Technical & Direct
Synthesized Behavioral Summary: User values clear, high-velocity technical execution, structured multi-modal workflows, and prompt responses.
Frequent Interest Clusters: Computer Vision, Java Architecture
```

---

## 5. Transparency & Ethical Controls

1. **HUD Visibility:** The JavaFX interface includes a dedicated **Adaptive Personalization Inspector** that displays the exact synthesized profile and tone in real time.
2. **User Agency:** Users can view their profile summary, wipe interaction history, or force an instant re-synthesis via the `"⚡ Re-Synthesize"` HUD button.
3. **No External Leakage:** Behavioral profiles remain inside the local SQLite database on the user's computer.
