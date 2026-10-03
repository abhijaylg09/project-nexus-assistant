#!/usr/bin/env python3
"""
PROJECT N.E.X.U.S - Deep Learning Emotion & Mood Recognition Engine
Uses Microsoft FERPlus ONNX neural network to classify 6 core emotional states:
FOCUSED, HAPPY, STRESSED, NEUTRAL, SURPRISED, SAD.
"""

import os
import sys
import cv2
import numpy as np

try:
    import onnxruntime as ort
except ImportError:
    ort = None

# FERPlus output classes
FERPLUS_CLASSES = ['neutral', 'happiness', 'surprise', 'sadness', 'anger', 'disgust', 'fear', 'contempt']

class EmotionDetector:
    def __init__(self, model_path=None):
        self.session = None
        self.model_path = model_path or os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "models", "emotion_ferplus.onnx")
        self.last_emotion = "FOCUSED"
        self.last_confidence = 0.88
        self._load_model()

    def _load_model(self):
        if ort is None:
            print("[Python-Emotion] onnxruntime not installed!", file=sys.stderr)
            return

        if not os.path.exists(self.model_path):
            print(f"[Python-Emotion] Model not found at {self.model_path}, downloading from Hugging Face...")
            try:
                from huggingface_hub import hf_hub_download
                import shutil
                downloaded = hf_hub_download(
                    repo_id="onnxmodelzoo/emotion-ferplus-8",
                    filename="emotion-ferplus-8.onnx"
                )
                os.makedirs(os.path.dirname(self.model_path), exist_ok=True)
                shutil.copy(downloaded, self.model_path)
                print(f"[Python-Emotion] Successfully downloaded model to {self.model_path}")
            except Exception as e:
                print(f"[Python-Emotion] Could not download FERPlus model: {e}", file=sys.stderr)
                return

        try:
            opts = ort.SessionOptions()
            opts.intra_op_num_threads = 2
            opts.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
            self.session = ort.InferenceSession(self.model_path, sess_options=opts, providers=['CPUExecutionProvider'])
            self.input_name = self.session.get_inputs()[0].name
            print(f"[Python-Emotion] FERPlus ONNX model loaded from {self.model_path}")
        except Exception as e:
            print(f"[Python-Emotion] Failed to initialize emotion session: {e}", file=sys.stderr)

    def preprocess_face(self, bgr_face):
        """Converts face crop to [1, 1, 64, 64] float32 grayscale tensor for FERPlus."""
        gray = cv2.cvtColor(bgr_face, cv2.COLOR_BGR2GRAY) if len(bgr_face.shape) == 3 else bgr_face
        resized = cv2.resize(gray, (64, 64), interpolation=cv2.INTER_AREA)
        tensor = resized.astype(np.float32).reshape(1, 1, 64, 64)
        return tensor

    def predict(self, face_bgr):
        """Classifies facial emotion from a cropped face image."""
        if self.session is None or face_bgr is None or face_bgr.size == 0:
            return {
                "emotion": self.last_emotion,
                "confidence": self.last_confidence,
                "probabilities": {self.last_emotion.lower(): self.last_confidence}
            }

        try:
            tensor = self.preprocess_face(face_bgr)
            outputs = self.session.run(None, {self.input_name: tensor})
            logits = outputs[0][0]

            # Softmax
            exp_logits = np.exp(logits - np.max(logits))
            probs = exp_logits / np.sum(exp_logits)

            prob_dict = {FERPLUS_CLASSES[i]: float(probs[i]) for i in range(len(FERPLUS_CLASSES))}

            # Map FERPlus emotions to N.E.X.U.S schema
            happy_score = prob_dict.get('happiness', 0.0)
            surprise_score = prob_dict.get('surprise', 0.0)
            sad_score = prob_dict.get('sadness', 0.0)
            stressed_score = max(prob_dict.get('anger', 0.0), prob_dict.get('fear', 0.0), prob_dict.get('disgust', 0.0))
            neutral_score = prob_dict.get('neutral', 0.0)

            # Heuristic decision logic
            if happy_score > 0.45:
                emotion = "HAPPY"
                confidence = happy_score
            elif surprise_score > 0.45:
                emotion = "SURPRISED"
                confidence = surprise_score
            elif stressed_score > 0.40:
                emotion = "STRESSED"
                confidence = stressed_score
            elif sad_score > 0.40:
                emotion = "SAD"
                confidence = sad_score
            elif neutral_score > 0.50:
                # Differentiate between attentive FOCUSED and NEUTRAL
                emotion = "FOCUSED" if neutral_score > 0.65 else "NEUTRAL"
                confidence = neutral_score
            else:
                emotion = "FOCUSED"
                confidence = 0.82

            self.last_emotion = emotion
            self.last_confidence = round(confidence, 4)

            return {
                "emotion": emotion,
                "confidence": self.last_confidence,
                "probabilities": {k: round(v, 4) for k, v in prob_dict.items()},
                "engine": "Python FERPlus Deep Learning"
            }
        except Exception as e:
            return {
                "emotion": self.last_emotion,
                "confidence": self.last_confidence,
                "error": str(e)
            }
