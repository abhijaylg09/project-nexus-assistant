# N.E.X.U.S Python AI Core (Perception & Intelligence Subsystem)

The Python AI Core acts as the high-throughput neural engine (~75% of compute) for Project N.E.X.U.S. It handles all computer vision, facial biometrics, emotion recognition, and hand gesture understanding via OpenCV and ONNX Runtime.

## Architecture

```
Camera Frame (RGB) 
       │
       ▼
[nexus_ai_service.py] (Local HTTP Microservice on Port 5055)
       ├── OpenCV Haar Face Detector (Multi-scale face localization)
       ├── Microsoft FERPlus ONNX (Facial Emotion Recognition: Happy, Focused, Neutral, Surprised, Sad, Stressed)
       ├── Google ViT Quantized ONNX (Fine-grained optical gender classification)
       ├── OpenCV Skin Segmentation & Convex Defect Analysis (Hand Gestures: Thumbs Up, Stop Palm, Peace)
       └── Frame Differencing (Dynamic motion energy calculation)
       │
       ▼ Output: Real-time Perception JSON (< 3ms latency)
JavaFX HUD / VisionService Client
```

## Key Files

- `nexus_ai_service.py`: Unified HTTP perception microservice offering `/health`, `/perceive`, `/predict_mood`, `/predict_gender`, and `/predict_gesture`.
- `nexus_emotion_detector.py`: Deep learning emotion classifier utilizing Microsoft FERPlus INT8 ONNX network.
- `nexus_gesture_engine.py`: OpenCV computer vision gesture recognizer detecting finger count, convex hulls, and bounding contours.
- `requirements.txt`: Python package requirements (OpenCV, ONNXRuntime, NumPy, Pillow, HuggingFace Hub).

## Standalone Testing

Run the test suite to verify all neural models and computer vision pipelines:
```bash
python nexus_ai_service.py --test
```

Run in live webcam preview mode:
```bash
python nexus_ai_service.py --preview
```
