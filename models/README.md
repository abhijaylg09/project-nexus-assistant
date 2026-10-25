# Deep Learning Models for Project N.E.X.U.S

This directory contains deep learning models used by the computer vision perception pipeline.

## 🧠 Gender Classification Model: FairFace ViT-ONNX (Quantized)

- **Architecture:** Vision Transformer (`google/vit-base-patch16-224-in21k` fine-tuned on the balanced FairFace benchmark dataset)
- **Model Card:** [`dima806/fairface_gender_image_detection`](https://huggingface.co/dima806/fairface_gender_image_detection) / [`onnx-community/fairface_gender_image_detection-ONNX`](https://huggingface.co/onnx-community/fairface_gender_image_detection-ONNX)
- **Benchmark Accuracy:** **93.42%** on diverse demographics (Male precision: 91.9%, Female precision: 95.3%)
- **ONNX Optimization:** Quantized INT8 weights (`gender_fairface_quantized.onnx`) for real-time CPU execution (~10-15ms per face)
- **Input Dimensions:** `[1, 3, 224, 224]` normalized tensor
- **Output:** Softmax probabilities: index 0 = `FEMALE`, index 1 = `MALE` with hysteresis temporal stabilization

### Automatic Download
The model is automatically fetched and cached upon launching `python scripts/gender_detector.py` or running:
```bash
python models/download_model.py
```
