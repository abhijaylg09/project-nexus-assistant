# Deep Learning Models for Project N.E.X.U.S

This directory contains deep learning models used by the computer vision perception pipeline.

## 🧠 Gender Classification Model: ViT-ONNX (Quantized)

- **Architecture:** Vision Transformer (`vit-base-patch16-224` fine-tuned for facial gender classification)
- **Model Card:** [`rizvandwiki/gender-classification`](https://huggingface.co/rizvandwiki/gender-classification)
- **ONNX Optimization:** Quantized INT8 weights (`model_quantized.onnx`) for real-time CPU execution (~15ms per face)
- **Input Dimensions:** `[1, 3, 224, 224]` normalized tensor
- **Output:** Calibrated probabilities for `MALE` and `FEMALE`

### Automatic Download
The model is automatically fetched and cached upon launching `python scripts/gender_detector.py` or running:
```bash
python models/download_model.py
```
