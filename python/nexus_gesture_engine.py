#!/usr/bin/env python3
"""
PROJECT N.E.X.U.S - OpenCV Hand Gesture Recognition Engine
Analyzes camera frames using skin segmentation, convex hull, and convexity defects
to classify physical gestures: THUMBS_UP, STOP_PALM, PEACE, NONE.
"""

import cv2
import numpy as np

class GestureDetector:
    def __init__(self):
        # YCrCb skin threshold range
        self.lower_skin = np.array([0, 133, 77], dtype=np.uint8)
        self.upper_skin = np.array([255, 173, 127], dtype=np.uint8)
        self.min_contour_area = 5000

    def detect_gesture(self, frame_bgr, face_box=None):
        """
        Detects hand gestures from the frame, optionally excluding the face region
        to prevent false positive skin contours from the head.
        """
        if frame_bgr is None or frame_bgr.size == 0:
            return {"gesture": "NONE", "confidence": 0.0}

        h, w = frame_bgr.shape[:2]

        # Convert to YCrCb for robust lighting-invariant skin color detection
        ycrcb = cv2.cvtColor(frame_bgr, cv2.COLOR_BGR2YCrCb)
        mask = cv2.inRange(ycrcb, self.lower_skin, self.upper_skin)

        # Mask out face region so head doesn't trigger hand gesture
        if face_box:
            fx, fy, fw, fh = face_box
            pad = int(fw * 0.25)
            x1 = max(0, fx - pad)
            y1 = max(0, fy - pad)
            x2 = min(w, fx + fw + pad)
            y2 = min(h, fy + fh + pad)
            mask[y1:y2, x1:x2] = 0

        # Morphological operations to remove noise and fill holes
        kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
        mask = cv2.erode(mask, kernel, iterations=1)
        mask = cv2.dilate(mask, kernel, iterations=2)
        mask = cv2.GaussianBlur(mask, (5, 5), 0)

        contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        if not contours:
            return {"gesture": "NONE", "confidence": 0.0}

        # Filter contours by size and select largest
        valid_contours = [c for c in contours if cv2.contourArea(c) >= self.min_contour_area]
        if not valid_contours:
            return {"gesture": "NONE", "confidence": 0.0}

        hand_contour = max(valid_contours, key=cv2.contourArea)
        area = cv2.contourArea(hand_contour)

        hull = cv2.convexHull(hand_contour, returnPoints=False)
        if hull is None or len(hull) < 4:
            return {"gesture": "NONE", "confidence": 0.0}

        try:
            defects = cv2.convexityDefects(hand_contour, hull)
        except Exception:
            return {"gesture": "NONE", "confidence": 0.0}

        if defects is None:
            return {"gesture": "NONE", "confidence": 0.0}

        # Count significant finger separation valleys (defects)
        defect_count = 0
        for i in range(defects.shape[0]):
            s, e, f, d = defects[i, 0]
            start = tuple(hand_contour[s][0])
            end = tuple(hand_contour[e][0])
            far = tuple(hand_contour[f][0])

            # Compute sides of triangle
            a = np.linalg.norm(np.array(end) - np.array(start))
            b = np.linalg.norm(np.array(far) - np.array(start))
            c = np.linalg.norm(np.array(end) - np.array(far))

            # Cosine theorem for angle
            if b * c > 0:
                angle = np.arccos(np.clip((b**2 + c**2 - a**2) / (2 * b * c), -1.0, 1.0))
                # Only accept angles < 90 degrees with deep defect distance
                if angle <= np.pi / 2 and d > 2000:
                    defect_count += 1

        # Bounding box of hand
        bx, by, bw, bh = cv2.boundingRect(hand_contour)
        aspect = float(bw) / max(1, bh)

        # Classification rule set:
        # 4-5 defects -> All fingers spread wide -> STOP_PALM (Pause/Mute)
        # 1-2 defects -> Two fingers extended in V shape -> PEACE (Summarize)
        # 0 defects and vertical elongation -> THUMBS_UP (Confirm)
        if defect_count >= 3:
            return {"gesture": "STOP_PALM", "confidence": 0.92, "defects": defect_count}
        elif defect_count == 1 or defect_count == 2:
            return {"gesture": "PEACE", "confidence": 0.88, "defects": defect_count}
        elif defect_count == 0 and aspect < 0.85 and bh > bw * 1.2:
            return {"gesture": "THUMBS_UP", "confidence": 0.85, "defects": defect_count}

        return {"gesture": "NONE", "confidence": 0.0, "defects": defect_count}
