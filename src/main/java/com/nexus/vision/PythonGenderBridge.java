package com.nexus.vision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * High-performance Java-Python Bridge for Deep Learning Gender Classification.
 * Spawns and manages the Python ViT-ONNX microservice on 127.0.0.1:5055
 * and asynchronously streams face crops for instant, sub-20ms inference.
 */
public class PythonGenderBridge {

    private static final int PORT = 5055;
    private static final String SERVER_URL = "http://127.0.0.1:" + PORT;
    private static final String PREDICT_URL = SERVER_URL + "/predict";
    private static final String HEALTH_URL = SERVER_URL + "/health";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService asyncWorker;

    private Process pythonProcess;
    private final AtomicBoolean pythonServiceReady = new AtomicBoolean(false);
    private final AtomicBoolean requestInProgress = new AtomicBoolean(false);

    private long lastInferenceTime = 0;
    private static final long INFERENCE_INTERVAL_MS = 350; // Throttle to prevent CPU saturation

    // Latest classified result
    public record GenderResult(
            boolean success,
            String gender,
            double confidence,
            double maleProb,
            double femaleProb,
            boolean faceDetected,
            String engine
    ) {}

    private volatile GenderResult latestResult = new GenderResult(
            false, "MALE", 0.90, 0.90, 0.10, false, "Initializing"
    );

    public PythonGenderBridge() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(800))
                .build();
        this.objectMapper = new ObjectMapper();
        this.asyncWorker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "nexus-python-gender-bridge");
            t.setDaemon(true);
            return t;
        });

        // Initialize connection or start python daemon
        initializeServiceAsync();

        // JVM shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void initializeServiceAsync() {
        asyncWorker.submit(() -> {
            if (checkHealth()) {
                pythonServiceReady.set(true);
                System.out.println("[PythonGenderBridge] Connected to existing Python ViT Gender Service at " + SERVER_URL);
                return;
            }

            // Start python service
            startPythonServiceProcess();

            // Wait up to 6 seconds for service readiness
            for (int i = 0; i < 12; i++) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
                if (checkHealth()) {
                    pythonServiceReady.set(true);
                    System.out.println("[PythonGenderBridge] Python ViT Gender Service successfully launched and ready!");
                    return;
                }
            }
            System.err.println("[PythonGenderBridge] Notice: Python service did not respond within timeout. Optical fallback active.");
        });
    }

    private boolean checkHealth() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HEALTH_URL))
                    .timeout(Duration.ofMillis(1000))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private synchronized void startPythonServiceProcess() {
        try {
            File scriptFile = new File("scripts", "gender_detector.py");
            if (!scriptFile.exists()) {
                scriptFile = new File(System.getProperty("user.dir"), "scripts/gender_detector.py");
            }

            if (!scriptFile.exists()) {
                System.err.println("[PythonGenderBridge] Script not found at: " + scriptFile.getAbsolutePath());
                return;
            }

            String pythonExe = resolvePythonExecutable();
            ProcessBuilder pb = new ProcessBuilder(
                    pythonExe,
                    scriptFile.getAbsolutePath(),
                    "--server",
                    "--port",
                    String.valueOf(PORT)
            );
            pb.redirectErrorStream(true);
            pythonProcess = pb.start();
            System.out.println("[PythonGenderBridge] Spawned Python process (PID: " + pythonProcess.pid() + ")");

        } catch (Exception e) {
            System.err.println("[PythonGenderBridge] Failed to launch Python process: " + e.getMessage());
        }
    }

    private String resolvePythonExecutable() {
        String[] candidates = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? new String[]{"python", "python3", "py"}
                : new String[]{"python3", "python"};

        for (String candidate : candidates) {
            try {
                Process testProc = new ProcessBuilder(candidate, "--version").start();
                if (testProc.waitFor(2, TimeUnit.SECONDS) && testProc.exitValue() == 0) {
                    return candidate;
                }
            } catch (Exception ignored) {}
        }
        return candidates[0];
    }

    /**
     * Asynchronously sends cropped face or frame to Python ViT server.
     */
    public void predictAsync(BufferedImage fullFrame, int fx, int fy, int fw, int fh, java.util.function.Consumer<GenderResult> callback) {
        if (!pythonServiceReady.get()) return;

        long now = System.currentTimeMillis();
        if (now - lastInferenceTime < INFERENCE_INTERVAL_MS) {
            return;
        }

        if (!requestInProgress.compareAndSet(false, true)) {
            return;
        }

        lastInferenceTime = now;

        asyncWorker.submit(() -> {
            try {
                if (fullFrame == null) return;

                // Crop face region with 15% margin
                int padX = (int) (fw * 0.15);
                int padY = (int) (fh * 0.15);
                int cropX = Math.max(0, fx - padX);
                int cropY = Math.max(0, fy - padY);
                int cropW = Math.min(fullFrame.getWidth() - cropX, fw + (padX * 2));
                int cropH = Math.min(fullFrame.getHeight() - cropY, fh + (padY * 2));

                if (cropW <= 20 || cropH <= 20) return;

                BufferedImage faceCrop = fullFrame.getSubimage(cropX, cropY, cropW, cropH);

                // Encode to in-memory JPEG bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(faceCrop, "jpg", baos);
                byte[] jpegBytes = baos.toByteArray();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(PREDICT_URL))
                        .header("Content-Type", "image/jpeg")
                        .timeout(Duration.ofMillis(1200))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(jpegBytes))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    if ("success".equalsIgnoreCase(root.path("status").asText())) {
                        String gender = root.path("gender").asText("MALE").toUpperCase();
                        double confidence = root.path("confidence").asDouble(0.90);
                        double maleProb = root.path("male_prob").asDouble(0.50);
                        double femaleProb = root.path("female_prob").asDouble(0.50);
                        boolean faceDetected = root.path("face_detected").asBoolean(true);
                        String engine = root.path("engine").asText("Python ViT-ONNX");

                        GenderResult res = new GenderResult(true, gender, confidence, maleProb, femaleProb, faceDetected, engine);
                        this.latestResult = res;

                        if (callback != null) {
                            callback.accept(res);
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore transient network hiccups
            } finally {
                requestInProgress.set(false);
            }
        });
    }

    public boolean isPythonServiceReady() {
        return pythonServiceReady.get();
    }

    public GenderResult getLatestResult() {
        return latestResult;
    }

    public synchronized void shutdown() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            System.out.println("[PythonGenderBridge] Terminating Python process...");
            pythonProcess.destroy();
            try {
                if (!pythonProcess.waitFor(2, TimeUnit.SECONDS)) {
                    pythonProcess.destroyForcibly();
                }
            } catch (InterruptedException ignored) {}
        }
        asyncWorker.shutdownNow();
    }
}
