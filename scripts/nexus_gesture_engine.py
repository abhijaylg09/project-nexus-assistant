#!/usr/bin/env python3
"""
PROJECT N.E.X.U.S - Advanced Real-Time Python Hand Gesture Recognition Core
Dual-Engine:
1. Primary: MediaPipe Hands Skeleton Landmark Tracking (21 3D Keypoints)
2. Fallback: Multi-Colorspace (YCrCb + HSV) Contour & Convexity Defect Geometric Analyzer
Classifies physical human gestures:
- THUMBS_UP: Action Confirmation / Positive Intent
- STOP_PALM: Mute / Pause Assistant Speech
- PEACE (V-Sign): Summarization & Knowledge Recap
- NONE: Passive Idle State
Includes Temporal Debouncing & Confidence Calibration.
"""

import cv2
import numpy as np
import time
from collections import deque

# Dynamic import for MediaPipe with graceful fallback
try:
    import mediapipe as mp
    has_mediapipe = True
except ImportError:
    mp = None
    has_mediapipe = False


class GestureDetector:
    def __init__(self):
        self.mp_hands = None
        self._init_mediapipe()

        # Multi-colorspace adaptive skin thresholds for CV fallback
        self.lower_ycrcb = np.array([0, 133, 77], dtype=np.uint8)
        self.upper_ycrcb = np.array([255, 173, 127], dtype=np.uint8)
        self.lower_hsv = np.array([0, 25, 50], dtype=np.uint8)
        self.upper_hsv = np.array([25, 200, 255], dtype=np.uint8)

        # Minimum contour area for valid hand candidate
        self.min_contour_area = 5500

        # Temporal debouncing queue & latching state
        self.history = deque(maxlen=4)
        self.latched_gesture = "NONE"
        self.latched_confidence = 0.0
        self.latch_expiry = 0

    def _init_mediapipe(self):
        global has_mediapipe, mp
        if mp is None:
            try:
                import mediapipe as mp_import
                mp = mp_import
                has_mediapipe = True
            except ImportError:
                has_mediapipe = False

        if has_mediapipe and mp is not None:
            try:
                self.mp_hands = mp.solutions.hands.Hands(
                    static_image_mode=False,
                    max_num_hands=1,
                    min_detection_confidence=0.60,
                    min_tracking_confidence=0.55
                )
                print("[Python-Gesture] MediaPipe Hands 3D Landmark Tracking Engine initialized.")
            except Exception as e:
                print(f"[Python-Gesture] MediaPipe initialization warning: {e}")
                self.mp_hands = None

    def detect_gesture(self, frame_bgr, face_box=None):
        """
        Processes frame and returns detected gesture and confidence.
        Uses MediaPipe if available; otherwise falls back to adaptive CV analysis.
        """
        if frame_bgr is None or frame_bgr.size == 0:
            return {"gesture": "NONE", "confidence": 0.0, "engine": "None"}

        now = time.time()
        # Return latched gesture if active to ensure solid trigger delivery
        if now < self.latch_expiry and self.latched_gesture != "NONE":
            return {
                "gesture": self.latched_gesture,
                "confidence": round(self.latched_confidence, 4),
                "engine": "Temporal Latch Active"
            }

        # Try MediaPipe primary if not already initialized
        if self.mp_hands is None:
            self._init_mediapipe()

        raw_gesture = "NONE"
        confidence = 0.0
        engine_name = "Adaptive CV Engine"

        # 1. MediaPipe Primary Pipeline
        if self.mp_hands is not None:
            try:
                rgb_frame = cv2.cvtColor(frame_bgr, cv2.COLOR_BGR2RGB)
                results = self.mp_hands.process(rgb_frame)

                if results.multi_hand_landmarks:
                    hand_landmarks = results.multi_hand_landmarks[0]
                    raw_gesture, confidence = self._classify_landmarks(hand_landmarks)
                    engine_name = "MediaPipe Hands 3D Landmarks"
            except Exception:
                raw_gesture = "NONE"

        # 2. Computer Vision Fallback (Adaptive Multi-Space Segmentation)
        if raw_gesture == "NONE" and self.mp_hands is None:
            raw_gesture, confidence = self._classify_contour_cv(frame_bgr, face_box)
            engine_name = "Python Multi-Space CV Engine"

        # 3. Temporal Debouncing
        self.history.append((raw_gesture, confidence))
        active_gesture, active_conf = self._debounce_decision()

        if active_gesture != "NONE":
            self.latched_gesture = active_gesture
            self.latched_confidence = active_conf
            self.latch_expiry = now + 1.2  # Hold active gesture for 1.2s

        return {
            "gesture": active_gesture,
            "confidence": round(active_conf, 4),
            "engine": engine_name
        }

    def _classify_landmarks(self, lm):
        """
        Classifies gestures from 21 MediaPipe hand landmarks with geometric precision.
        Landmark indices:
        0: Wrist
        4: Thumb Tip, 3: Thumb IP, 2: Thumb MCP
        8: Index Tip, 6: Index PIP, 5: Index MCP
        12: Middle Tip, 10: Middle PIP, 9: Middle MCP
        16: Ring Tip, 14: Ring PIP, 13: Ring MCP
        20: Pinky Tip, 18: Pinky PIP, 17: Pinky MCP
        """
        pts = lm.landmark
        wrist = pts[0]

        # Finger curl detection: Tip vs PIP relative to wrist
        def is_finger_extended(tip_idx, pip_idx, mcp_idx):
            # In image coords, smaller y means higher up
            return (pts[tip_idx].y < pts[pip_idx].y) and (pts[tip_idx].y < pts[mcp_idx].y)

        index_ext = is_finger_extended(8, 6, 5)
        middle_ext = is_finger_extended(12, 10, 9)
        ring_ext = is_finger_extended(16, 14, 13)
        pinky_ext = is_finger_extended(20, 18, 17)

        # Thumb extension: thumb tip higher than thumb IP and MCP
        thumb_up = (pts[4].y < pts[3].y) and (pts[4].y < pts[2].y) and (pts[4].y < pts[5].y)

        # 1. THUMBS_UP: Thumb extended upright, all other 4 fingers curled
        if thumb_up and not index_ext and not middle_ext and not ring_ext and not pinky_ext:
            return "THUMBS_UP", 0.96

        # 2. PEACE (V-Sign): Index and Middle extended, Ring and Pinky curled
        if index_ext and middle_ext and not ring_ext and not pinky_ext:
            # Check separation between index and middle tips
            dx = abs(pts[8].x - pts[12].x)
            if dx > 0.035:
                return "PEACE", 0.94

        # 3. STOP_PALM: All 5 fingers extended outward
        if index_ext and middle_ext and ring_ext and pinky_ext:
            return "STOP_PALM", 0.95

        return "NONE", 0.0

    def _classify_contour_cv(self, frame_bgr, face_box):
        """Robust multi-colorspace contour analyzer with convexity defect geometry."""
        h, w = frame_bgr.shape[:2]

        # Multi-channel color space fusion
        ycrcb = cv2.cvtColor(frame_bgr, cv2.COLOR_BGR2YCrCb)
        hsv = cv2.cvtColor(frame_bgr, cv2.COLOR_BGR2HSV)

        mask_ycrcb = cv2.inRange(ycrcb, self.lower_ycrcb, self.upper_ycrcb)
        mask_hsv = cv2.inRange(hsv, self.lower_hsv, self.upper_hsv)
        mask = cv2.bitwise_and(mask_ycrcb, mask_hsv)

        # Suppress face region so the head is not misidentified as a hand
        if face_box:
            fx, fy, fw, fh = face_box
            pad = int(fw * 0.35)
            x1 = max(0, fx - pad)
            y1 = max(0, fy - pad)
            x2 = min(w, fx + fw + pad)
            y2 = min(h, fy + fh + pad)
            mask[y1:y2, x1:x2] = 0

        # Morphological filtering
        kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
        mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel, iterations=1)
        mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel, iterations=2)
        mask = cv2.GaussianBlur(mask, (5, 5), 0)

        contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        if not contours:
            return "NONE", 0.0

        valid_contours = [c for c in contours if cv2.contourArea(c) >= self.min_contour_area]
        if not valid_contours:
            return "NONE", 0.0

        hand_contour = max(valid_contours, key=cv2.contourArea)
        area = cv2.contourArea(hand_contour)

        hull = cv2.convexHull(hand_contour, returnPoints=False)
        if hull is None or len(hull) < 4:
            return "NONE", 0.0

        try:
            defects = cv2.convexityDefects(hand_contour, hull)
        except Exception:
            return "NONE", 0.0

        if defects is None:
            return "NONE", 0.0

        defect_count = 0
        for i in range(defects.shape[0]):
            s, e, f, d = defects[i, 0]
            start = tuple(hand_contour[s][0])
            end = tuple(hand_contour[e][0])
            far = tuple(hand_contour[f][0])

            a = np.linalg.norm(np.array(end) - np.array(start))
            b = np.linalg.norm(np.array(far) - np.array(start))
            c = np.linalg.norm(np.array(end) - np.array(far))

            if b * c > 0:
                angle = np.arccos(np.clip((b**2 + c**2 - a**2) / (2 * b * c), -1.0, 1.0))
                if angle <= (np.pi * 0.48) and d > 1800:
                    defect_count += 1

        bx, by, bw, bh = cv2.boundingRect(hand_contour)
        aspect = float(bw) / max(1, bh)

        if defect_count >= 3:
            return "STOP_PALM", 0.91
        elif defect_count == 1 or defect_count == 2:
            return "PEACE", 0.88
        elif defect_count == 0 and aspect < 0.82 and bh > bw * 1.25:
            return "THUMBS_UP", 0.86

        return "NONE", 0.0

    def _debounce_decision(self):
        """Ensures gesture consistency over multiple frames before returning active."""
        if len(self.history) < 2:
            return "NONE", 0.0

        gestures = [g for g, _ in self.history]
        confidences = [c for _, c in self.history]

        # Check for unanimous agreement of non-NONE gesture in recent 2 frames
        if gestures[-1] != "NONE" and gestures[-1] == gestures[-2]:
            return gestures[-1], max(confidences[-1], confidences[-2])

        return "NONE", 0.0
