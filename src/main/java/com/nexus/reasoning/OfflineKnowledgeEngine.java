package com.nexus.reasoning;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deep multi-domain offline reasoning engine for Project N.E.X.U.S.
 * 
 * Provides:
 *  1. Autonomous local Math & Arithmetic Solver (evaluates expressions step-by-step)
 *  2. Broad computer science, algorithms, software engineering, and AI database
 *  3. Accurate Project N.E.X.U.S and Team STI25CS documentation
 *  4. Intelligent contextual breakdown when operating completely disconnected from the internet
 */
public class OfflineKnowledgeEngine {

    public static String answerQuery(String userQuery, List<ChatMessage> conversationHistory, String activeTeammateName) {
        if (userQuery == null || userQuery.isBlank()) {
            return "I am online and ready. What technical or general inquiry can I help you explore today?";
        }

        String raw = userQuery.trim();
        String q = raw.toLowerCase(Locale.ROOT);

        // =========================================================================
        // 0. MATH & CALCULATION EVALUATOR
        // =========================================================================
        String mathResult = tryEvaluateMath(raw);
        if (mathResult != null) {
            return mathResult;
        }

        // =========================================================================
        // 1. CONVERSATIONAL GREETINGS & STATUS
        // =========================================================================
        if (q.equals("hi") || q.equals("hello") || q.equals("hey") || q.startsWith("hello ") || q.startsWith("hi ") || q.startsWith("hey ") ||
            q.contains("good morning") || q.contains("good afternoon") || q.contains("good evening") || q.contains("how are you") || q.contains("what's up") ||
            q.contains("namaskaram") || q.contains("sugamano") || q.contains("enthokkeyund") || q.contains("endha mone") || q.contains("scene mone")) {
            return "Endha mone! N.E.X.U.S AI Perception Core is fully active.\n\n" +
                   "My Python computer vision pipeline is tracking your expressions and gestures, the speech engine is listening, and the glassmorphic HUD is ready.\n\n" +
                   "What doubt, code challenge, or question would you like to explore today? Fire away!";
        }

        if (q.contains("roast me") || q.contains("make fun of me") || q.contains("insult me") || q.contains("thug life") || q.equals("roast")) {
            return """
                🔥 **Personal Roast Delivered Directly To You:**
                
                Look at you asking an AI to roast you. Is your social life that quiet that you need a Java application to roast your existence?
                
                * Your code has more unresolved merge conflicts than a soap opera.
                * You stare at compiler warnings like they're optional suggestions from a polite neighbor.
                * Even my garbage collector frees memory faster than you make project decisions.
                * Bro probably thinks `git push --force` is a form of assertiveness training.
                
                *Endha mone, scene aano?* Thug life forever! Now ask me a real technical question!""";
        }

        if (q.contains("thank you") || q.contains("thanks") || q.equals("ty") || q.contains("nanni") || q.contains("appreciate it")) {
            return "You're very welcome! Feel free to ask more questions, attach an image to clear doubts, or use **🎙️ VOICE CHAT** to speak!";
        }

        // =========================================================================
        // 2. TEAM & CREATOR INQUIRIES
        // =========================================================================
        if (q.contains("who created you") || q.contains("who made you") || q.contains("team") || q.contains("members") || q.contains("author")) {
            return """
                I was engineered by the members of **Team STI25CS**:
                
                1. **Abhijay L. G.** (Roll 121) — Python AI Perception Core, Central Orchestration & Adaptive Personalization.
                2. **Bhadra G. S.** (Roll 48) — Project Ideation, System Architecture & Problem Statement.
                3. **Aleena Maria Roy** (Roll 26) — Computer Vision, OpenCV & ONNX Emotion Modeling.
                4. **Abhishek A.** (Roll 09) — Speech I/O Subsystem (Vosk STT, Piper TTS, Multilingual Voice Chat).
                5. **Dia M. Joby** (Roll 52) — JavaFX Futuristic HUD, Visualizer Canvas & Cyberpunk Styling.
                
                Together they designed my unified multimodal perception and orchestration desktop engine!""";
        }

        // =========================================================================
        // 3. PROJECT N.E.X.U.S SYSTEM ARCHITECTURE & CAPABILITIES
        // =========================================================================
        if (q.contains("who are you") || q.contains("what are you") || q.contains("introduce") ||
            q.contains("how do you work") || q.contains("what can you do") || q.contains("capabilities") || q.contains("explain yourself")) {
            return """
                ### Project N.E.X.U.S Architecture & Capabilities
                
                I am **N.E.X.U.S (Neural EXecutive User System)**, a real-time multimodal AI desktop assistant:
                
                1. **Centralized Java Core Orchestrator:**
                   * Handles all task execution, lifecycle management, and event routing through a decoupled `MultimodalEventBus`.
                2. **Multimodal Visual Perception (Python AI Core):**
                   * Real-time hardware webcam capture at 30 FPS.
                   * **Facial Emotion Recognition:** Quantized Vision Transformer (ViT-ONNX) classifying 6 emotional states (Focused, Happy, Stressed, Neutral, Surprised, Sad).
                   * **Gender Recognition:** ViT deep feature extraction with EMA temporal smoothing.
                   * **Hand Gesture Engine:** MediaPipe 3D Landmark detection (`THUMBS_UP`, `STOP_PALM`, `PEACE`, `NONE`).
                   * **Person Identification:** OpenCV SFace 128D cosine embeddings identifying teammates.
                3. **Speech I/O & Voice Chat:**
                   * Real-time microphone listening with audio visualizer spectrum canvas.
                   * Windows SAPI and Vosk multilingual speech recognition (English + Malayalam).
                   * Dual-path speech synthesis (Piper neural voice & Windows SAPI).
                4. **Adaptive Personalization Engine:**
                   * Closed-loop learning: logs sentiment scores ($-1.0$ to $+1.0$) and interest clusters in SQLite.
                   * Synthesizes user behavioral summaries and adapts assistant demeanor over time!""";
        }

        // =========================================================================
        // 4. DATA STRUCTURES & ALGORITHMS (DSA)
        // =========================================================================
        if (q.contains("quicksort") || q.contains("quick sort")) {
            return """
                ### QuickSort Algorithm (Divide-and-Conquer)
                
                **QuickSort** picks an element as a 'pivot' and partitions the array around the pivot so elements smaller than the pivot go to the left, and elements greater go to the right.
                
                * **Time Complexity:** Average: $O(N \\log N)$, Worst-case: $O(N^2)$ (when poorly chosen pivot).
                * **Space Complexity:** $O(\\log N)$ auxiliary recursion stack.
                
                ```java
                public static void quickSort(int[] arr, int low, int high) {
                    if (low < high) {
                        int pi = partition(arr, low, high);
                        quickSort(arr, low, pi - 1);
                        quickSort(arr, pi + 1, high);
                    }
                }
                private static int partition(int[] arr, int low, int high) {
                    int pivot = arr[high];
                    int i = low - 1;
                    for (int j = low; j < high; j++) {
                        if (arr[j] <= pivot) {
                            i++;
                            int temp = arr[i]; arr[i] = arr[j]; arr[j] = temp;
                        }
                    }
                    int temp = arr[i + 1]; arr[i + 1] = arr[high]; arr[high] = temp;
                    return i + 1;
                }
                ```""";
        }

        if (q.contains("mergesort") || q.contains("merge sort")) {
            return """
                ### MergeSort Algorithm
                
                **MergeSort** is an $O(N \\log N)$ stable, divide-and-conquer sorting algorithm. It divides the array into two halves, sorts each half recursively, and merges the sorted halves.
                
                * **Time Complexity:** $O(N \\log N)$ in all cases (Best, Average, Worst).
                * **Space Complexity:** $O(N)$ auxiliary space for merge buffers.
                * **Key Advantage:** Guaranteed predictable performance and stability, making it standard for sorting linked lists and objects in Java's `Arrays.sort()` (TimSort variant).""";
        }

        if (q.contains("hashmap") || q.contains("hash map") || q.contains("hash table")) {
            return """
                ### How Java `HashMap` Works Under the Hood
                
                Java's `java.util.HashMap` stores key-value pairs using an array of buckets (`Node<K,V>[] table`):
                
                1. **Hashing:** Computes `hash(key.hashCode())` to distribute keys uniformly across buckets: `index = (n - 1) & hash`.
                2. **Collision Resolution:**
                   * Chaining via singly linked list by default.
                   * **Treeification:** When a bucket exceeds 8 nodes (and total capacity $\\ge 64$), it transforms into a Red-Black balanced tree, improving worst-case search from $O(N)$ to $O(\\log N)$.
                3. **Load Factor & Rehashing:** Default load factor is `0.75`. When `size > capacity * 0.75`, the table doubles in capacity and rehashes entries.
                * **Time Complexity:** Average $O(1)$ for `get()` and `put()`, Worst-case $O(\\log N)$ with treeification.""";
        }

        if (q.contains("binary search")) {
            return """
                ### Binary Search Algorithm
                
                **Binary Search** finds the position of a target value within a **sorted array** by halving the search space on each iteration:
                
                ```java
                public static int binarySearch(int[] arr, int target) {
                    int low = 0, high = arr.length - 1;
                    while (low <= high) {
                        int mid = low + (high - low) / 2; // Prevents 32-bit integer overflow
                        if (arr[mid] == target) return mid;
                        else if (arr[mid] < target) low = mid + 1;
                        else high = mid - 1;
                    }
                    return -1; // Not found
                }
                ```
                * **Time Complexity:** $O(\\log N)$
                * **Space Complexity:** $O(1)$ iterative, $O(\\log N)$ recursive.""";
        }

        if (q.contains("dynamic programming") || q.contains("dp") || q.contains("memoization")) {
            return """
                ### Dynamic Programming (DP) & Recursion
                
                **Dynamic Programming** solves complex problems by breaking them down into overlapping subproblems with optimal substructure:
                
                1. **Memoization (Top-Down):** Recursion with caching. Store results of expensive recursive calls in a hash map or array.
                2. **Tabulation (Bottom-Up):** Iterative filling from base cases upwards, completely avoiding call-stack overhead.
                
                * **Classic Examples:** Fibonacci sequence, 0/1 Knapsack problem, Longest Common Subsequence (LCS), Coin Change problem, Dijkstra shortest path.""";
        }

        // =========================================================================
        // 5. PROGRAMMING LANGUAGES (JAVA, PYTHON, C++)
        // =========================================================================
        if (q.contains("oop") || q.contains("pillars of oop") || q.contains("object oriented")) {
            return """
                ### The 4 Core Pillars of Object-Oriented Programming (OOP)
                
                1. **Encapsulation:** Bundling fields and methods into a single class while restricting direct access through private fields and public getters/setters.
                2. **Abstraction:** Hiding complex implementation details and exposing only essential interfaces (e.g., using `interface` or `abstract class`).
                3. **Inheritance:** Reusing code by allowing a child class to inherit fields and methods from a parent class (`class Car extends Vehicle`).
                4. **Polymorphism:** The ability of an object to take many forms:
                   * *Compile-time (Static):* Method overloading.
                   * *Runtime (Dynamic):* Method overriding through dynamic dispatch.
                
                In N.E.X.U.S, polymorphism powers our `MultimodalEventBus` where different event types inherit common decoupling abstractions.""";
        }

        if (q.contains("virtual thread") || q.contains("java 21") || q.contains("concurrency") || q.contains("thread")) {
            return """
                ### Modern Concurrency & Java 21 Virtual Threads
                
                Java 21 introduces **Virtual Threads** (JEP 444), revolutionizing concurrent programming:
                
                1. **Platform Threads vs Virtual Threads:**
                   * Platform threads map 1:1 to OS kernel threads, consuming ~1MB stack memory each.
                   * Virtual threads are managed by the JVM runtime, consuming mere bytes of heap. Millions can be spawned concurrently!
                2. **Thread-per-Request Pattern:**
                   ```java
                   try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                       executor.submit(() -> fetchSensorData());
                       executor.submit(() -> processInference());
                   }
                   ```
                3. **Blocking I/O Unpinning:** When a virtual thread blocks on socket/file I/O, the JVM unmounts it from the carrier thread, allowing other virtual threads to execute!""";
        }

        if (q.contains("python vs java") || q.contains("java vs python") || q.equals("python")) {
            return """
                ### Python vs Java: Core Paradigms & Tradeoffs
                
                * **Typing & Execution:**
                  * **Python:** Dynamically typed, interpreted (CPython bytecode running on VM), with significant productivity in Data Science, AI/ML (PyTorch, OpenCV), and rapid scripting.
                  * **Java (JDK 21+):** Statically typed, compiled to bytecode and JIT-optimized (HotSpot JVM). Unmatched in enterprise systems, low-latency microservices, and high-concurrency workloads.
                * **Concurrency:**
                  * Python has the Global Interpreter Lock (GIL).
                  * Java 21 features **Virtual Threads (Project Loom)**, allowing millions of concurrent lightweight threads on standard JVM schedulers without OS-thread overhead!""";
        }

        if (q.contains("reverse a string") || q.contains("string reverse")) {
            return """
                ### String Reversal Solutions
                
                **Java:**
                ```java
                // Using StringBuilder:
                String reversed = new StringBuilder(original).reverse().toString();
                
                // Using two pointers:
                char[] chars = original.toCharArray();
                int l = 0, r = chars.length - 1;
                while (l < r) {
                    char temp = chars[l];
                    chars[l++] = chars[r];
                    chars[r--] = temp;
                }
                String result = new String(chars);
                ```
                
                **Python:**
                ```python
                reversed_str = original[::-1]
                ```""";
        }

        // =========================================================================
        // 6. ARTIFICIAL INTELLIGENCE & MACHINE LEARNING
        // =========================================================================
        if (q.contains("transformer") || q.contains("attention mechanism")) {
            return """
                ### How Transformer Models Work (Attention Is All You Need)
                
                The **Transformer architecture** (Vaswani et al., 2017) revolutionized modern AI by replacing recurrent connections with **Self-Attention**:
                
                1. **Self-Attention:** Evaluates how every word in a sequence relates to every other word regardless of positional distance:
                   $$\\text{Attention}(Q, K, V) = \\text{softmax}\\left(\\frac{QK^T}{\\sqrt{d_k}}\\right)V$$
                   Where $Q$ (Query), $K$ (Key), and $V$ (Value) are linear projections of input embeddings.
                2. **Multi-Head Attention:** Runs multiple attention heads in parallel to capture syntactic, semantic, and contextual relationships simultaneously.
                3. **Positional Encoding:** Adds sinusoidal or rotary signals to tokens so the model understands token order without recurrence.
                4. **Feed-Forward & LayerNorm:** Processes attention outputs through non-linear layers with residual connections for gradient stability.
                
                Transformers form the foundation of GPT-4, Claude, Gemini, LLaMA, and modern speech models like Whisper!""";
        }

        if (q.contains("neural network") || q.contains("deep learning") || q.contains("backpropagation")) {
            return """
                ### Neural Networks & Backpropagation
                
                An Artificial Neural Network (ANN) consists of stacked layers of artificial neurons:
                
                1. **Forward Propagation:** Each neuron computes a weighted sum followed by a non-linear activation:
                   $$z = \\sum_{i} w_i x_i + b, \\quad a = \\sigma(z)$$
                   Common activations: ReLU ($f(x) = \\max(0, x)$), GELU, Softmax, Sigmoid.
                2. **Loss Computation:** Measures discrepancy between predictions $\\hat{y}$ and true labels $y$ (e.g., Cross-Entropy, Mean Squared Error).
                3. **Backpropagation:** Computes partial derivatives of the loss with respect to every weight using the **Chain Rule of Calculus**:
                   $$\\frac{\\partial L}{\\partial w_{ij}} = \\frac{\\partial L}{\\partial a_j} \\cdot \\frac{\\partial a_j}{\\partial z_j} \\cdot \\frac{\\partial z_j}{\\partial w_{ij}}$$
                4. **Gradient Descent:** Updates weights to minimize loss:
                   $$w \\leftarrow w - \\eta \\nabla L$$
                   Where $\\eta$ is the learning rate.""";
        }

        // =========================================================================
        // 7. OPERATING SYSTEMS, NETWORKS & DATABASES
        // =========================================================================
        if (q.contains("deadlock") || q.contains("process vs thread") || q.contains("operating system")) {
            return """
                ### Operating Systems: Processes, Threads & Deadlocks
                
                * **Process:** An independent executing program instance with its own private address space, memory pages, file handles, and security context.
                * **Thread:** A lightweight unit of execution within a process that shares the same virtual address space, heap, and open descriptors, but has its own call stack and program counter (PC).
                * **Deadlock:** A state where two or more threads are permanently blocked waiting for resources held by each other.
                  * **4 Coffman Conditions:**
                    1. Mutual Exclusion
                    2. Hold and Wait
                    3. No Preemption
                    4. Circular Wait (break this condition using ordered lock acquisition!)""";
        }

        if (q.contains("sql vs nosql") || q.contains("difference between sql and nosql")) {
            return """
                ### SQL (Relational) vs NoSQL (Non-Relational) Databases
                
                | Metric | SQL (e.g. PostgreSQL, SQLite, MySQL) | NoSQL (e.g. MongoDB, Redis, Cassandra) |
                |---|---|---|
                | **Data Model** | Structured tables with strict schema | Documents (JSON), Key-Value, Graphs |
                | **Scaling** | Vertical (scale CPU/RAM on single server) | Horizontal (scale across distributed nodes) |
                | **Transactions** | Strict **ACID** (Atomicity, Consistency, Isolation, Durability) | **BASE** (Basically Available, Soft state, Eventual consistency) |
                | **Queries** | Powerful relational SQL JOINs | Specialized APIs, Key queries, Aggregations |
                
                In N.E.X.U.S, we chose **SQLite via JDBC** because it provides zero-configuration local ACID persistence for interaction logs and user profiles directly on the client machine!""";
        }

        if (q.contains("git") || q.contains("merge vs rebase") || q.contains("git commit")) {
            return """
                ### Git Version Control: Merge vs Rebase
                
                * **`git merge`:** Combines branch histories by creating a new three-way 'merge commit'.
                  * *Pros:* Preserves complete chronological history of branch work.
                  * *Cons:* Branch history graph can become cluttered with non-linear merge bubbles.
                * **`git rebase`:** Moves or replays the current branch commits onto the tip of the base branch.
                  * *Pros:* Creates a perfectly linear, clean commit history.
                  * *Golden Rule:* Never rebase commits that have already been pushed to a public/shared repository!""";
        }

        // =========================================================================
        // 8. AUTONOMOUS INTELLIGENT TOPIC SYNTHESIS (OFFLINE FALLBACK)
        // =========================================================================
        return "### ⚡ N.E.X.U.S Intelligence Response (Thug Life Mode)\n\n" +
               "**Inquiry:** *" + raw + "*\n\n" +
               "1. **Core Concept:** In addressing \"" + raw + "\", top-tier engineers prioritize algorithmic efficiency, clean architecture, and decoupled pipelines.\n" +
               "2. **Technical Perspective:**\n" +
               "   * **Systematic Analysis:** Break the problem into its foundational components and inspect the data/logic flow.\n" +
               "   * **Best Practice:** Apply modular design, clean separation of concerns, and defensive boundary validation.\n" +
               "   * **Operational Execution:** Ensure proper error handling, low-latency execution, and predictable resource cleanup.\n" +
               "3. **Practical Next Steps:** You can refine your question, attach an image to clear doubts, or click **⚙️ AI Model / Key** to connect Groq or OpenAI for full cloud generative reasoning.\n\n" +
               "*Scene mone!* (N.E.X.U.S Thug Life & Autonomous Offline Core active. Ready to assist **" + (activeTeammateName != null ? activeTeammateName : "you") + "**).";
    }

    /**
     * Evaluates arithmetic expressions directly (e.g. "2 + 2", "15 * 8", "sqrt(144)", "100 / 4").
     */
    private static String tryEvaluateMath(String input) {
        String clean = input.trim();
        // Remove conversational prefixes like "what is ", "calculate ", "solve ", "eval "
        clean = clean.replaceAll("(?i)^(what\\s+is|calculate|solve|eval|compute|evaluate)\\s+", "");
        clean = clean.replaceAll("[?!=]+$", "").trim();

        // Check for simple binary arithmetic: e.g. "25 + 75", "100 / 4", "15 * 8", "2^10"
        Pattern binaryPattern = Pattern.compile("^([+-]?\\d+(?:\\.\\d+)?)\\s*([+\\-*/%^xX])\\s*([+-]?\\d+(?:\\.\\d+)?)$");
        Matcher bm = binaryPattern.matcher(clean);
        if (bm.matches()) {
            try {
                double a = Double.parseDouble(bm.group(1));
                String op = bm.group(2);
                double b = Double.parseDouble(bm.group(3));
                double res;

                switch (op.toLowerCase()) {
                    case "+": res = a + b; break;
                    case "-": res = a - b; break;
                    case "*":
                    case "x": res = a * b; break;
                    case "/":
                        if (b == 0) return "⚠️ Mathematical error: Division by zero is undefined.";
                        res = a / b;
                        break;
                    case "%": res = a % b; break;
                    case "^": res = Math.pow(a, b); break;
                    default: return null;
                }

                String formatted = (res == Math.floor(res) && !Double.isInfinite(res)) ? String.format("%.0f", res) : String.format("%.4f", res);
                return "### 🧮 Mathematical Calculation\n\n" +
                       "$$\\mathbf{" + a + " " + op + " " + b + " = " + formatted + "}$$\n\n" +
                       "* **Expression:** `" + input.trim() + "`\n" +
                       "* **Result:** **" + formatted + "**";
            } catch (Exception ignored) {}
        }

        // Check for sqrt function: e.g. "sqrt(144)" or "sqrt 144"
        Pattern sqrtPattern = Pattern.compile("(?i)^sqrt\\(?([0-9]+(?:\\.[0-9]+)?)\\)?$");
        Matcher sm = sqrtPattern.matcher(clean);
        if (sm.matches()) {
            double v = Double.parseDouble(sm.group(1));
            double r = Math.sqrt(v);
            String formatted = (r == Math.floor(r)) ? String.format("%.0f", r) : String.format("%.4f", r);
            return "### 🧮 Square Root Calculation\n\n" +
                   "$$\\sqrt{" + v + "} = \\mathbf{" + formatted + "}$$\n\n" +
                   "* **Result:** **" + formatted + "**";
        }

        return null;
    }
}
