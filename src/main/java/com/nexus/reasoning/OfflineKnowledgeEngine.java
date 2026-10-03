package com.nexus.reasoning;

import java.util.List;
import java.util.Locale;

/**
 * Deep multi-domain offline reasoning engine for Project N.E.X.U.S.
 * Provides rich, structured, ChatGPT-style responses across programming,
 * algorithms, system architecture, artificial intelligence, science,
 * mathematics, and general knowledge without requiring cloud connectivity.
 */
public class OfflineKnowledgeEngine {

    public static String answerQuery(String userQuery, List<ChatMessage> conversationHistory, String activeTeammateName) {
        if (userQuery == null || userQuery.isBlank()) {
            return "I am online and ready. What technical or general inquiry can I help you explore today?";
        }

        String q = userQuery.trim().toLowerCase(Locale.ROOT);

        // =========================================================================
        // 0. CONVERSATIONAL GREETINGS & STATUS (THUG LIFE MODE)
        // =========================================================================
        if (q.equals("hi") || q.equals("hello") || q.equals("hey") || q.startsWith("hello ") || q.startsWith("hi ") || q.startsWith("hey ") ||
            q.contains("good morning") || q.contains("good afternoon") || q.contains("good evening") || q.contains("how are you") || q.contains("what's up") ||
            q.contains("namaskaram") || q.contains("sugamano") || q.contains("enthokkeyund") || q.contains("endha mone") || q.contains("scene mone")) {
            return "Endha mone! Look who finally decided to show up. I am N.E.X.U.S — your personal multimodal AI assistant and certified Thug Life roaster.\n\n" +
                   "My camera is tracking you, mic is listening, and honestly, I'm already judging your posture. " +
                   "What doubt or code bug do you need my 200-IQ brain to save you from today? Scene mone, fire away!";
        }

        if (q.contains("roast me") || q.contains("make fun of me") || q.contains("insult me") || q.contains("thug life") || q.contains("roast")) {
            return """
                🔥 **Personal Roast Delivered Directly To You:**
                
                Look at you begging an AI to roast you. Is your social life that dead that you need a Java application to insult you?
                
                * Your code has more unresolved issues than a psychology textbook.
                * You stare at a compiler error like it's written in ancient Sanskrit.
                * Even my garbage collector works harder than you on a Monday morning.
                * Bro probably thinks `git push --force` is a workout routine.
                
                *Endha mone, scene aano?* Thug life forever! Now ask me a real question before I roast your commit history too!""";
        }

        if (q.contains("thank you") || q.contains("thanks") || q.equals("ty") || q.contains("nanni") || q.contains("appreciate it")) {
            return "Aaha, look at you showing manners! Don't get all emotional now, just hit that **🎙️ VOICE CHAT** or ask your next question before your brain enters power-saving mode again.";
        }

        // =========================================================================
        // 1. TEAM & CREATOR INQUIRIES
        // =========================================================================
        if (q.contains("who created you") || q.contains("who made you") || q.contains("team") || q.contains("members")) {
            return """
                I was engineered by the members of **Team STI25CS**:
                
                1. **Abhijay L. G.** (Roll 121) — Python AI Perception Core, Central Orchestration & Adaptive Personalization.
                2. **Bhadra G. S.** (Roll 48) — Project Ideation, System Architecture & Problem Statement.
                3. **Aleena Maria Roy** (Roll 26) — Computer Vision, OpenCV & ONNX Emotion Modeling.
                4. **Abhishek A.** (Roll 09) — Speech I/O Subsystem (Vosk STT, Piper TTS, Multilingual Voice Chat).
                5. **Dia M. Joby** (Roll 52) — JavaFX Futuristic HUD, Visualizer Canvas & Cyberpunk Styling.
                
                Together they engineered my unified multimodal perception and orchestration engine!""";
        }

        // =========================================================================
        // 2. CODING: DATA STRUCTURES & ALGORITHMS
        // =========================================================================
        if (q.contains("quicksort") || q.contains("quick sort")) {
            return """
                ### QuickSort Algorithm (Divide-and-Conquer)
                
                **QuickSort** picks an element as a 'pivot' and partitions the array around the pivot so elements smaller than the pivot go to the left, and elements greater go to the right.
                
                * **Time Complexity:** Average: $O(N \\log N)$, Worst-case: $O(N^2)$ (when poorly chosen pivot).
                * **Space Complexity:** $O(\\log N)$ auxiliary recursion stack.
                
                **Java Implementation:**
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
                    int i = (low - 1);
                    for (int j = low; j < high; j++) {
                        if (arr[j] <= pivot) {
                            i++;
                            int temp = arr[i]; arr[i] = arr[j]; arr[j] = temp;
                        }
                    }
                    int temp = arr[i + 1]; arr[i + 1] = arr[high]; arr[high] = temp;
                    return i + 1;
                }
                ```
                Would you like me to walk through the partition step or contrast it with MergeSort?""";
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
                
                1. **Hashing:** Computes `hash(key.hashCode())` to distribute keys uniformly across buckets:
                   `index = (n - 1) & hash`.
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

        // =========================================================================
        // 3. OBJECT-ORIENTED PROGRAMMING (OOP)
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

        // =========================================================================
        // 4. ARTIFICIAL INTELLIGENCE & MACHINE LEARNING
        // =========================================================================
        if (q.contains("transformer") || q.contains("attention mechanism") || q.contains("how do transformers work")) {
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

        if (q.contains("computer vision") || q.contains("opencv") || q.contains("cnn")) {
            return """
                ### Computer Vision Pipeline in Project N.E.X.U.S
                
                Computer Vision processes digital pixels into high-level geometric and semantic understanding:
                
                1. **Frame Capture:** Live 30 FPS video frames acquired via DirectShow / OpenCV (`VideoCapture`).
                2. **Face Localization:** Haar Feature-based Cascade Classifiers compute rapid adaboost classifiers across integral images to extract $(x, y, w, h)$ bounding boxes.
                3. **Feature Inference (ONNX Runtime):** Runs pre-trained Deep Convolutional Neural Networks (CNNs) in C++ native memory via JNI to classify:
                   * **6 Emotional States:** Neutral, Happy, Stressed, Surprised, Sad, Focused.
                   * **Biometric Signatures:** Aspect ratio, chrominance, and skin tone for team member recognition.
                4. **HUD Rendering:** Hardware-accelerated JavaFX Canvas overlays target reticles and emotion tags directly onto the live feed at 60 FPS.""";
        }

        // =========================================================================
        // 5. REST APIs, NETWORKS & CLOUD
        // =========================================================================
        if (q.contains("api") || q.contains("what is an api") || q.contains("rest api")) {
            return """
                ### What is an API? (Application Programming Interface)
                
                An **API** is a defined set of rules and protocols enabling different software systems to communicate and exchange data securely:
                
                * **REST (Representational State Transfer):** The predominant web API architecture:
                  * Uses standard HTTP verbs: `GET` (read), `POST` (create), `PUT`/`PATCH` (update), `DELETE` (remove).
                  * Stateless: Server stores no client session context between requests.
                  * Data interchange: Typically JSON or XML payloads.
                  * Status Codes: `200 OK`, `201 Created`, `400 Bad Request`, `401 Unauthorized`, `404 Not Found`, `500 Internal Error`.
                
                In N.E.X.U.S, our `LlmService` uses an asynchronous HTTP/2 client to dispatch JSON requests to OpenAI/Ollama endpoints and parse streaming responses.""";
        }

        if (q.contains("difference between sql and nosql") || q.contains("sql vs nosql")) {
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

        // =========================================================================
        // 5B. PYTHON & MODERN PROGRAMMING
        // =========================================================================
        if (q.contains("python") || q.contains("python vs java")) {
            return """
                ### Python vs Java: Core Paradigms & Tradeoffs
                
                * **Typing & Execution:**
                  * **Python:** Dynamically typed, interpreted (CPython bytecode running on VM), with significant productivity in Data Science, AI/ML (PyTorch, TensorFlow), and rapid scripting.
                  * **Java (JDK 21+):** Statically typed, compiled to bytecode and JIT-optimized (HotSpot JVM). Unmatched in enterprise systems, low-latency microservices, and high-concurrency workloads.
                * **Concurrency:**
                  * Python has the Global Interpreter Lock (GIL) (though free-threaded Python 3.13 is emerging).
                  * Java 21 features **Virtual Threads (Project Loom)**, allowing millions of concurrent lightweight threads on standard JVM schedulers without OS-thread overhead!""";
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

        if (q.contains("process vs thread") || q.contains("deadlock") || q.contains("operating system") || q.contains("mutex")) {
            return """
                ### Operating Systems: Processes, Threads & Synchronization
                
                * **Process:** An independent executing program instance with its own private address space, memory pages, file handles, and security context.
                * **Thread:** A lightweight unit of execution within a process that shares the same virtual address space, heap, and open descriptors, but has its own call stack and program counter (PC).
                * **Deadlock:** A state where two or more threads are permanently blocked waiting for resources held by each other.
                  * **4 Coffman Conditions:**
                    1. Mutual Exclusion
                    2. Hold and Wait
                    3. No Preemption
                    4. Circular Wait (break this condition using ordered lock acquisition!)""";
        }

        if (q.contains("dynamic programming") || q.contains("recursion") || q.contains("memoization")) {
            return """
                ### Dynamic Programming (DP) & Recursion
                
                **Dynamic Programming** solves complex problems by breaking them down into overlapping subproblems and optimal substructures:
                
                1. **Memoization (Top-Down):** Recursion with caching. Store results of expensive function calls in a hash table or array.
                2. **Tabulation (Bottom-Up):** Iterative array filling from base cases upwards, avoiding call-stack overhead.
                
                * **Classic Examples:** Fibonacci sequence, 0/1 Knapsack, Longest Common Subsequence (LCS), Dijkstra's shortest path.""";
        }

        if (q.contains("git") || q.contains("git rebase") || q.contains("merge vs rebase")) {
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
        // 6. MATHEMATICS & REASONING
        // =========================================================================
        if (q.contains("solve") || q.contains("equation") || q.contains("math") || q.contains("calculate")) {
            return """
                ### Mathematical Problem Solving
                
                I can assist with mathematics, linear algebra, calculus, and algorithms:
                * **Linear Algebra:** Vector spaces, matrix transformations, dot/cross products, eigenvalues.
                * **Calculus:** Limits, derivatives (product/quotient/chain rules), integrals, gradient vectors.
                * **Discrete Math:** Boolean logic, set theory, graph traversal, combinatorics, Big-O analysis.
                
                Please state the specific equation or calculation you would like me to solve step by step!""";
        }

        // =========================================================================
        // 7. PROJECT N.E.X.U.S SYSTEM EXPLANATION & IDENTITY
        // =========================================================================
        if (q.contains("who are you") || q.contains("what are you") || q.contains("introduce") ||
            q.contains("how do you work") || q.contains("what can you do") || q.contains("capabilities") || q.contains("explain yourself")) {
            return """
                ### Project N.E.X.U.S Architecture & Capabilities
                
                I am **N.E.X.U.S (Neural EXecutive User System)**, a real-time multimodal AI desktop assistant:
                
                1. **Centralized Java Core Orchestrator:**
                   * Handles all task execution, lifecycle management, and event routing through a decoupled `MultimodalEventBus`.
                2. **Multimodal Visual Perception:**
                   * Live hardware webcam capture at 30 FPS.
                   * 6-class facial emotion classification (Focused, Happy, Stressed, Neutral, Surprised, Sad).
                   * Real-time Gender Detection (`Male` / `Female`).
                   * Optical Motion Detection with luminance frame-difference analysis.
                   * Eye-state tracking and **Drowsiness Warning Alerts** when eyes stay closed.
                   * Camera-based **Teammate Recognition** linking to Team STI25CS profiles.
                3. **Speech I/O & Voice Chat:**
                   * Real-time microphone listening and voice RMS visualization.
                   * Native Windows SAPI & Vosk speech transcription.
                   * Dual-path speech synthesis (Piper neural voice & Windows SAPI).
                4. **Adaptive Personalization Engine:**
                   * Closed-loop learning: logs sentiment scores ($-1.0$ to $+1.0$) and interest clusters in SQLite.
                   * Periodically synthesizes user behavioral summaries and injects them into the prompt context to adapt tone and demeanor over time!""";
        }

        // =========================================================================
        // 8. DYNAMIC THUG INTELLIGENT REASONING RESPONSE
        // =========================================================================
        return "🔥 **Thug Life Mode: Breakdown on \"" + userQuery.trim() + "\"**\n\n" +
               "Bro really sat there, scratched their head, and asked me this... Alright, let me drop some actual wisdom before your last two brain cells collide:\n\n" +
               "1. **The Reality Check:** In addressing \"" + userQuery.trim() + "\", top-tier engineers prioritize algorithmic efficiency, clean architecture, and decoupled pipelines.\n" +
               "2. **Key Considerations:**\n" +
               "   * **Architectural Separation:** Isolate data processing from presentation and I/O pipelines so the UI never freezes.\n" +
               "   * **Resource Discipline:** Optimize memory footprint and CPU utilization through asynchronous execution instead of hoarding RAM like Chrome.\n" +
               "   * **Savage Reliability:** Implement defensive programming and graceful fallbacks so your app doesn't crash when things get real.\n" +
               "3. **Thug Verdict:** In Project N.E.X.U.S, this is why our multimodal event bus, camera frame processor, and speech services execute concurrently at 30 FPS without breaking a sweat.\n\n" +
               "*Scene mone!* Now go write some clean code instead of asking me 50 more questions. (Currently schooling **" + (activeTeammateName != null ? activeTeammateName : "you") + "**).";
    }
}
