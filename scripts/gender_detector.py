#!/usr/bin/env python3
"""
PROJECT N.E.X.U.S - Deep Learning Gender Classification Engine
High-accuracy Vision Transformer (ViT) ONNX Model with OpenCV Face Localization.
Provides instant micro-HTTP prediction service and CLI inference for the Java core.
"""

import os
import sys
import io
import json
import base64
import argparse
from http.server import HTTPServer, BaseHTTPRequestHandler
import numpy as np

# Optional imports with graceful fallbacks
try:
    import cv2
except ImportError:
    cv2 = None

try:
    from PIL import Image
except ImportError:
    Image = None

try:
    import onnxruntime as ort
except ImportError:
    ort = None

DEFAULT_PORT = 5055
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
MODEL_PATH = os.path.join(PROJECT_ROOT, "models", "gender_vit_quantized.onnx")

# ViT Normalization constants
IMAGE_SIZE = (224, 224)
IMAGE_MEAN = np.array([0.5, 0.5, 0.5], dtype=np.float32)
IMAGE_STD = np.array([0.5, 0.5, 0.5], dtype=np.float32)

class GenderDetectorEngine:
    def __init__(self, model_path=None):
        self.session = None
        self.face_cascade = None
        self.model_path = model_path or MODEL_PATH
        self._load_face_detector()
        self._load_onnx_model()

    def _load_face_detector(self):
        if cv2 is not None:
            cascade_path = os.path.join(cv2.data.haarcascades, "haarcascade_frontalface_default.xml")
            if os.path.exists(cascade_path):
                self.face_cascade = cv2.CascadeClassifier(cascade_path)
                print(f"[Python-GenderEngine] OpenCV face cascade loaded from {cascade_path}")

    def _load_onnx_model(self):
        if ort is None:
            print("[Python-GenderEngine] ERROR: onnxruntime not installed!", file=sys.stderr)
            return

        # Check local path first
        if not os.path.exists(self.model_path):
            print(f"[Python-GenderEngine] Model not found at {self.model_path}. Checking Hugging Face cache...")
            try:
                from huggingface_hub import hf_hub_download
                downloaded = hf_hub_download(
                    repo_id="onnx-community/gender-classification-ONNX",
                    filename="onnx/model_quantized.onnx"
                )
                self.model_path = downloaded
                print(f"[Python-GenderEngine] Using Hugging Face model: {self.model_path}")
            except Exception as e:
                print(f"[Python-GenderEngine] Could not download model: {e}", file=sys.stderr)
                return

        try:
            opts = ort.SessionOptions()
            opts.intra_op_num_threads = 2
            opts.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
            self.session = ort.InferenceSession(self.model_path, sess_options=opts, providers=['CPUExecutionProvider'])
            self.input_name = self.session.get_inputs()[0].name
            print(f"[Python-GenderEngine] ONNX ViT session initialized successfully from {self.model_path}")
        except Exception as e:
            print(f"[Python-GenderEngine] Failed to load ONNX model: {e}", file=sys.stderr)

    def crop_face(self, bgr_image):
        """Locates face in image, returns cropped face image or original if no face detected."""
        if self.face_cascade is None or bgr_image is None:
            return bgr_image, False

        gray = cv2.cvtColor(bgr_image, cv2.COLOR_BGR2GRAY)
        faces = self.face_cascade.detectMultiScale(
            gray,
            scaleFactor=1.1,
            minNeighbors=4,
            minSize=(60, 60)
        )

        if len(faces) == 0:
            return bgr_image, False

        # Pick largest detected face
        faces = sorted(faces, key=lambda f: f[2] * f[3], reverse=True)
        x, y, w, h = faces[0]

        # Add 15% margin around the face
        pad_x = int(w * 0.15)
        pad_y = int(h * 0.15)
        ih, iw = bgr_image.shape[:2]
        x1 = max(0, x - pad_x)
        y1 = max(0, y - pad_y)
        x2 = min(iw, x + w + pad_x)
        y2 = min(ih, y + h + pad_y)

        cropped = bgr_image[y1:y2, x1:x2]
        return cropped, True

    def preprocess_image(self, bgr_or_rgb_image, is_bgr=True):
        """Preprocesses image into normalized ViT tensor (1, 3, 224, 224)."""
        if is_bgr and cv2 is not None:
            rgb = cv2.cvtColor(bgr_or_rgb_image, cv2.COLOR_BGR2RGB)
        else:
            rgb = bgr_or_rgb_image

        if cv2 is not None:
            resized = cv2.resize(rgb, IMAGE_SIZE, interpolation=cv2.INTER_LINEAR)
        elif Image is not None:
            pil_img = Image.fromarray(rgb).resize(IMAGE_SIZE, Image.BILINEAR)
            resized = np.array(pil_img)
        else:
            raise RuntimeError("Neither OpenCV nor Pillow is available for resizing.")

        # Normalize [0, 1] then subtract mean, divide by std
        arr = resized.astype(np.float32) / 255.0
        arr = (arr - IMAGE_MEAN) / IMAGE_STD
        # Transpose HWC -> CHW: (3, 224, 224)
        arr = np.transpose(arr, (2, 0, 1))
        # Add batch dim -> (1, 3, 224, 224)
        tensor = np.expand_dims(arr, axis=0)
        return tensor

    def predict_from_array(self, image_array, is_bgr=True, auto_crop=True):
        """Runs ViT inference on an image array."""
        if self.session is None:
            return {
                "status": "error",
                "message": "ONNX model session not loaded",
                "gender": "UNKNOWN",
                "confidence": 0.50
            }

        face_detected = False
        face_img = image_array
        if auto_crop and is_bgr and cv2 is not None:
            face_img, face_detected = self.crop_face(image_array)

        tensor = self.preprocess_image(face_img, is_bgr=is_bgr)
        outputs = self.session.run(None, {self.input_name: tensor})
        logits = outputs[0][0]

        # Softmax: index 0 = female, index 1 = male
        exp_logits = np.exp(logits - np.max(logits))
        probs = exp_logits / np.sum(exp_logits)
        female_prob = float(probs[0])
        male_prob = float(probs[1])

        if male_prob >= female_prob:
            gender = "MALE"
            confidence = male_prob
        else:
            gender = "FEMALE"
            confidence = female_prob

        return {
            "status": "success",
            "gender": gender,
            "confidence": round(confidence, 4),
            "male_prob": round(male_prob, 4),
            "female_prob": round(female_prob, 4),
            "face_detected": face_detected,
            "engine": "Python ViT-ONNX (rizvandwiki/gender-classification)"
        }

    def predict_from_bytes(self, image_bytes, auto_crop=True):
        """Decodes raw JPEG/PNG bytes and classifies gender."""
        if cv2 is not None:
            nparr = np.frombuffer(image_bytes, np.uint8)
            img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            if img is None:
                raise ValueError("Could not decode image from bytes with cv2")
            return self.predict_from_array(img, is_bgr=True, auto_crop=auto_crop)
        elif Image is not None:
            pil_img = Image.open(io.BytesIO(image_bytes)).convert("RGB")
            arr = np.array(pil_img)
            return self.predict_from_array(arr, is_bgr=False, auto_crop=False)
        else:
            raise RuntimeError("No image decoder (OpenCV or Pillow) available")

    def predict_from_file(self, file_path, auto_crop=True):
        """Reads image from disk and classifies gender."""
        if not os.path.exists(file_path):
            return {"status": "error", "message": f"File not found: {file_path}", "gender": "UNKNOWN", "confidence": 0.5}

        with open(file_path, "rb") as f:
            data = f.read()
        return self.predict_from_bytes(data, auto_crop=auto_crop)


class GenderServerHandler(BaseHTTPRequestHandler):
    engine = None

    def log_message(self, format, *args):
        # Suppress routine access logs for clean console
        pass

    def _send_json(self, status_code, data):
        response_bytes = json.dumps(data).encode("utf-8")
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(response_bytes)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(response_bytes)

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

    def do_GET(self):
        if self.path == "/health":
            self._send_json(200, {
                "status": "online",
                "engine": "Python ViT-ONNX Gender Classifier",
                "ready": self.engine.session is not None
            })
        else:
            self._send_json(404, {"error": "Not Found"})

    def do_POST(self):
        content_length = int(self.headers.get("Content-Length", 0))
        post_data = self.rfile.read(content_length)

        if self.path in ("/predict", "/predict_gender", "/classify"):
            try:
                content_type = self.headers.get("Content-Type", "")
                if "application/json" in content_type:
                    body = json.loads(post_data.decode("utf-8"))
                    if "image_path" in body:
                        result = self.engine.predict_from_file(body["image_path"])
                    elif "image_base64" in body:
                        b64_str = body["image_base64"]
                        if "," in b64_str:
                            b64_str = b64_str.split(",", 1)[1]
                        raw_bytes = base64.b64decode(b64_str)
                        result = self.engine.predict_from_bytes(raw_bytes)
                    else:
                        result = {"status": "error", "message": "Expected image_path or image_base64 in JSON"}
                else:
                    # Treat raw binary payload as image (JPEG/PNG)
                    result = self.engine.predict_from_bytes(post_data)

                self._send_json(200, result)
            except Exception as e:
                self._send_json(500, {"status": "error", "message": str(e), "gender": "UNKNOWN", "confidence": 0.5})
        else:
            self._send_json(404, {"error": "Endpoint not found"})


def run_server(port=DEFAULT_PORT, model_path=None):
    engine = GenderDetectorEngine(model_path)
    GenderServerHandler.engine = engine
    server_address = ('127.0.0.1', port)
    httpd = HTTPServer(server_address, GenderServerHandler)
    print(f"[Python-GenderEngine] Micro-HTTP Gender Prediction Service active on http://127.0.0.1:{port}")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n[Python-GenderEngine] Server stopping...")
    finally:
        httpd.server_close()


def run_test(model_path=None):
    print("==================================================")
    print("PROJECT N.E.X.U.S - Python Gender Detection Self-Test")
    print("==================================================")
    engine = GenderDetectorEngine(model_path)
    if engine.session is None:
        print("FAIL: Could not load ONNX model.")
        return 1

    # Create synthetic test patterns
    dummy = np.full((300, 300, 3), 128, dtype=np.uint8)
    res = engine.predict_from_array(dummy, auto_crop=False)
    print("Synthetic Frame Test Result:", json.dumps(res, indent=2))
    print("PASS: Python Gender Engine is fully functional!")
    return 0


def main():
    parser = argparse.ArgumentParser(description="N.E.X.U.S Python Deep Learning Gender Classification Engine")
    parser.add_argument("--server", action="store_true", help="Start background HTTP micro-service")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT, help="Server port (default: 5055)")
    parser.add_argument("--image", type=str, help="Path to image file for one-shot prediction")
    parser.add_argument("--test", action="store_true", help="Run model initialization and inference self-test")
    parser.add_argument("--model", type=str, default=None, help="Custom ONNX model path")

    args = parser.parse_args()

    if args.test:
        sys.exit(run_test(args.model))
    elif args.image:
        engine = GenderDetectorEngine(args.model)
        res = engine.predict_from_file(args.image)
        print(json.dumps(res, indent=2))
    elif args.server or len(sys.argv) == 1:
        run_server(args.port, args.model)
    else:
        parser.print_help()


if __name__ == "__main__":
    main()
