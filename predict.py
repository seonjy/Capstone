"""
사진 → 장면 분류 + 명암 판정

- 장면: ResNet18 4-클래스 모델 (음식/풍경/인물/야경)
- 명암: 대비 수치(std) 기준으로 고대비/보통 판정
"""

import sys
import cv2
import numpy as np
import torch
import torch.nn as nn
from torchvision import models, transforms
from PIL import Image

SCENE_CLASSES = ["food", "landscape", "night", "portrait"]
SCENE_KOR = {"food": "음식", "landscape": "풍경", "night": "야경", "portrait": "인물"}
MODEL_PATH = "scene_model_4class.pth"

# 명암(대비) 판정 기준: 이미지 밝기 표준편차(std)
# 분포상 중앙 57 / 상위15% ≈ 75. std가 이 값 이상이면 '고대비'로 본다.
CONTRAST_THRESHOLD = 75.0

_device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
_tf = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize([0.485, 0.456, 0.406], [0.229, 0.224, 0.225]),
])

_model = models.resnet18(weights=None)
_model.fc = nn.Linear(_model.fc.in_features, len(SCENE_CLASSES))
_model.load_state_dict(torch.load(MODEL_PATH, map_location=_device))
_model.to(_device).eval()


def measure_contrast(image_path):
    """대비 수치(std)와 고대비 여부를 계산으로 판정."""
    g = cv2.imread(image_path, cv2.IMREAD_GRAYSCALE)
    if g is None:
        return None, None
    std = float(g.astype(np.float32).std())
    is_high = std >= CONTRAST_THRESHOLD
    return std, is_high


def predict(image_path):
    # 1) 장면: AI 분류
    img = Image.open(image_path).convert("RGB")
    x = _tf(img).unsqueeze(0).to(_device)
    with torch.no_grad():
        probs = torch.softmax(_model(x), dim=1)[0]
    conf, idx = torch.max(probs, 0)
    scene = SCENE_CLASSES[idx.item()]

    # 2) 명암: 계산으로 판정
    std, is_high = measure_contrast(image_path)

    return {
        "scene": scene,
        "scene_kor": SCENE_KOR[scene],
        "confidence": round(float(conf.item()) * 100, 1),
        "contrast_std": round(std, 1) if std is not None else None,
        "high_contrast": bool(is_high),
    }


if __name__ == "__main__":
    for path in sys.argv[1:]:
        r = predict(path)
        tag = " + 고대비" if r["high_contrast"] else ""
        print(f"{path}")
        print(f"  → 장면: {r['scene_kor']} (확신도 {r['confidence']}%){tag}")
        print(f"     [명암 수치 {r['contrast_std']}, 기준 {CONTRAST_THRESHOLD} 이상이면 고대비]")
