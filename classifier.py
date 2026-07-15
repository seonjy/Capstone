import io
import os
from typing import Dict

import torch
import torch.nn as nn
import cv2
import numpy as np
from PIL import Image
from torchvision import models, transforms

# 클래스 이름
CLASS_NAMES = ["food", "landscape", "night", "portrait"]

# -------------------------------
# 모델 경로 (절대경로로 안전하게)
# -------------------------------
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(BASE_DIR, "../../scene_model_4class.pth")

# 디바이스
device = torch.device("cpu")

# 전처리
transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize(
        mean=[0.485, 0.456, 0.406],
        std=[0.229, 0.224, 0.225]
    )
])

# 명암(대비) 판정 기준
CONTRAST_THRESHOLD = 75.0


def measure_contrast(image: Image.Image):
    """
    이미지의 밝기 표준편차(std)를 계산하여
    고대비 여부를 판정한다.
    """
    gray = cv2.cvtColor(np.array(image), cv2.COLOR_RGB2GRAY)
    std = float(gray.astype(np.float32).std())
    is_high = std >= CONTRAST_THRESHOLD

    return std, is_high

# -------------------------------
# 모델 로드 (서버 시작 시 1번만)
# -------------------------------
model = models.resnet18(weights=None)
model.fc = nn.Linear(model.fc.in_features, len(CLASS_NAMES))

try:
    state_dict = torch.load(MODEL_PATH, map_location=device)
    model.load_state_dict(state_dict)
    model.to(device)
    model.eval()
    print("✅ AI 모델 로드 완료")
except Exception as e:
    print("❌ 모델 로드 실패:", e)


# -------------------------------
# 예측 함수
# -------------------------------
def classify_scene(image_bytes: bytes) -> Dict:
    try:
        # 바이트 → 이미지
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")

        # 전처리
        x = transform(image).unsqueeze(0).to(device)

        # 예측
        with torch.no_grad():
            outputs = model(x)
            probs = torch.softmax(outputs, dim=1)
            conf, pred = torch.max(probs, dim=1)
        scene = CLASS_NAMES[pred.item()]
        confidence = float(conf.item())

        # 명암 계산
        contrast_std, high_contrast = measure_contrast(image)

        return {
            "scene": scene,
            "confidence": round(confidence, 4),
            "contrast_std": round(contrast_std, 1),
            "high_contrast": high_contrast,
        }
    except Exception as e:
        raise ValueError(f"AI 분류 오류: {str(e)}")
