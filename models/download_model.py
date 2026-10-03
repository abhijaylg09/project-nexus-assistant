import os
import shutil

def download_gender_model():
    models_dir = os.path.dirname(os.path.abspath(__file__))
    target_path = os.path.join(models_dir, "gender_vit_quantized.onnx")
    
    if os.path.exists(target_path):
        print(f"[NEXUS] Model already present at {target_path} ({os.path.getsize(target_path) / (1024*1024):.1f} MB)")
        return target_path

    print("[NEXUS] Downloading quantized ViT gender classification model from Hugging Face...")
    try:
        from huggingface_hub import hf_hub_download
        cached_file = hf_hub_download(
            repo_id="onnx-community/gender-classification-ONNX",
            filename="onnx/model_quantized.onnx"
        )
        shutil.copy(cached_file, target_path)
        print(f"[NEXUS] Successfully saved model to {target_path}")
        return target_path
    except Exception as e:
        print(f"[NEXUS] Download failed: {e}")
        return None

if __name__ == "__main__":
    download_gender_model()
