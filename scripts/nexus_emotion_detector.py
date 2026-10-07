#!/usr/bin/env python3
"""
PROJECT N.E.X.U.S - State-of-the-Art Deep Learning Emotion & Mood Recognition Engine
Dual-Engine:
1. Primary: Vision Transformer (ViT-ONNX, dima806/facial_emotions_image_detection)
2. Fallback: Microsoft FERPlus Neural Network with CLAHE Illumination Equalization
Supports Temporal EMA Smoothing & Real-Time Low-Latency Inference.
"""

import os
import sys
import cv2
import numpy as np

try:
    import onnxruntime as ort
except ImportError:
    ort = None

VIT_EMOTION_CLASSES = ['sad', 'disgust', 'angry', 'neutral', 'fear', 'surprise', 'happy']
FERPLUS_CLASSES = ['neutral', 'happiness', 'surprise', 'sadness', 'anger', 'disgust', 'fear', 'contempt']

VIT_IMAGE_SIZE = (224, 224)
VIT_MEAN = np.array([0.5, 0.5, 0.5], dtype=np.float32)
VIT_STD = np.array([0.5, 0.5, 0.5], dtype=np.float32)

class EmotionDetector:
    def __init__(self, vit_path=None, ferplus_path=None):
        self.session = None
        self.engine_type = "NONE"
        project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        self.vit_path = vit_path or os.path.join(project_root, "models", "emotion_vit_quantized.onnx")
        self.ferplus_path = ferplus_path or os.path.join(project_root, "models", "emotion_ferplus.onnx")

        self.last_emotion = "FOCUSED"
        self.last_confidence = 0.88
        self.smoothed_probs = None
        self.clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8)) if cv2 is not None else None

        self._load_best_model()

    def _load_best_model(self):
        if ort is None:
            print("[Python-Emotion] ERROR: onnxruntime not installed!", file=sys.stderr)
            return

        opts = ort.SessionOptions()
        opts.intra_op_num_threads = 2
        opts.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL

        # 1. Try ViT Emotion Model First (State of the art accuracy)
        if os.path.exists(self.vit_path):
            try:
                self.session = ort.InferenceSession(self.vit_path, sess_options=opts, providers=['CPUExecutionProvider'])
                self.input_name = self.session.get_inputs()[0].name
                self.engine_type = "VIT"
                print(f"[Python-Emotion] Vision Transformer (ViT-ONNX) Emotion Engine loaded from {self.vit_path}")
                return
            except Exception as e:
                print(f"[Python-Emotion] ViT model load warning: {e}", file=sys.stderr)

        # 2. Fallback to Microsoft FERPlus Model
        if os.path.exists(self.ferplus_path):
            try:
                self.session = ort.InferenceSession(self.ferplus_path, sess_options=opts, providers=['CPUExecutionProvider'])
                self.input_name = self.session.get_inputs()[0].name
                self.engine_type = "FERPLUS"
                print(f"[Python-Emotion] Microsoft FERPlus Neural Engine loaded from {self.ferplus_path}")
                return
            except Exception as e:
                print(f"[Python-Emotion] FERPlus model load failed: {e}", file=sys.stderr)

        print("[Python-Emotion] Notice: No emotion ONNX model found locally.", file=sys.stderr)

    def preprocess_vit(self, bgr_face):
        """Preprocesses face crop into normalized [1, 3, 224, 224] RGB tensor for ViT."""
        rgb = cv2.cvtColor(bgr_face, cv2.COLOR_BGR2RGB)
        resized = cv2.resize(rgb, VIT_IMAGE_SIZE, interpolation=cv2.INTER_LINEAR)
        arr = resized.astype(np.float32) / 255.0
        arr = (arr - VIT_MEAN) / VIT_STD
        arr = np.transpose(arr, (2, 0, 1))
        return np.expand_dims(arr, axis=0)

    def preprocess_ferplus(self, bgr_face):
        """Preprocesses face crop using CLAHE illumination equalization for FERPlus [1, 1, 64, 64]."""
        gray = cv2.cvtColor(bgr_face, cv2.COLOR_BGR2GRAY) if len(bgr_face.shape) == 3 else bgr_face
        if self.clahe is not None:
            gray = self.clahe.apply(gray)
        resized = cv2.resize(gray, (64, 64), interpolation=cv2.INTER_AREA)
        tensor = resized.astype(np.float32).reshape(1, 1, 64, 64)
        return tensor

    def predict(self, face_bgr):
        """Classifies facial emotion with temporal smoothing and robust confidence scoring."""
        if self.session is None or face_bgr is None or face_bgr.size == 0:
            return {
                "emotion": self.last_emotion,
                "confidence": self.last_confidence,
                "probabilities": {self.last_emotion.lower(): self.last_confidence},
                "engine": "Fallback Default"
            }

        try:
            if self.engine_type == "VIT":
                tensor = self.preprocess_vit(face_bgr)
                outputs = self.session.run(None, {self.input_name: tensor})
                logits = outputs[0][0]
                exp_l = np.exp(logits - np.max(logits))
                probs = exp_l / np.sum(exp_l)
                prob_map = {VIT_EMOTION_CLASSES[i]: float(probs[i]) for i in range(len(VIT_EMOTION_CLASSES))}

                happy_score = prob_map.get('happy', 0.0)
                surprise_score = prob_map.get('surprise', 0.0)
                sad_score = prob_map.get('sad', 0.0)
                angry_score = prob_map.get('angry', 0.0)
                fear_score = prob_map.get('fear', 0.0)
                disgust_score = prob_map.get('disgust', 0.0)
                neutral_score = prob_map.get('neutral', 0.0)
                stressed_score = max(angry_score, fear_score, disgust_score)
                engine_name = "Python ViT-ONNX Deep Learning (State-of-the-Art)"

            else:  # FERPLUS
                tensor = self.preprocess_ferplus(face_bgr)
                outputs = self.session.run(None, {self.input_name: tensor})
                logits = outputs[0][0]
                exp_l = np.exp(logits - np.max(logits))
                probs = exp_l / np.sum(exp_l)
                prob_map = {FERPLUS_CLASSES[i]: float(probs[i]) for i in range(len(FERPLUS_CLASSES))}

                happy_score = prob_map.get('happiness', 0.0)
                surprise_score = prob_map.get('surprise', 0.0)
                sad_score = prob_map.get('sadness', 0.0)
                stressed_score = max(prob_map.get('anger', 0.0), prob_map.get('fear', 0.0), prob_map.get('disgust', 0.0))
                neutral_score = prob_map.get('neutral', 0.0)
                engine_name = "Python FERPlus Deep Learning"

            # Temporal Exponential Moving Average (EMA) smoothing: alpha = 0.55
            current_vector = np.array([happy_score, surprise_score, sad_score, stressed_score, neutral_score], dtype=np.float32)
            if self.smoothed_probs is None:
                self.smoothed_probs = current_vector
            else:
                self.smoothed_probs = (self.smoothed_probs * 0.45) + (current_vector * 0.55)

            s_happy, s_surprise, s_sad, s_stressed, s_neutral = self.smoothed_probs

            # Decision Logic with hysteresis
            if s_happy > 0.38:
                emotion = "HAPPY"
                confidence = float(s_happy)
            elif s_surprise > 0.40:
                emotion = "SURPRISED"
                confidence = float(s_surprise)
            elif s_stressed > 0.35:
                emotion = "STRESSED"
                confidence = float(s_stressed)
            elif s_sad > 0.36:
                emotion = "SAD"
                confidence = float(s_sad)
            elif s_neutral > 0.45:
                emotion = "FOCUSED" if s_neutral > 0.60 else "NEUTRAL"
                confidence = float(s_neutral)
            else:
                emotion = "FOCUSED"
                confidence = 0.85

            self.last_emotion = emotion
            self.last_confidence = round(confidence, 4)

            return {
                "emotion": emotion,
                "confidence": self.last_confidence,
                "probabilities": {
                    "happy": round(float(s_happy), 4),
                    "surprise": round(float(s_surprise), 4),
                    "sad": round(float(s_sad), 4),
                    "stressed": round(float(s_stressed), 4),
                    "neutral": round(float(s_neutral), 4)
                },
                "engine": engine_name
            }

        except Exception as e:
            return {
                "emotion": self.last_emotion,
                "confidence": self.last_confidence,
                "error": str(e)
            }
