import io
import os
from typing import Dict

import torch
import torch.nn as nn
from PIL import Image
from torchvision import models, transforms


CLASS_NAMES = ["contrast", "food", "landscape", "night", "portrait"]

# 모델 파일 경로
MODEL_PATH = "models/scene_model.pth"

# 디바이스 설정
device = torch.device("cpu")

# 이미지 전처리 (학습 코드와 동일)
transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize(
        mean=[0.485, 0.456, 0.406],
        std=[0.229, 0.224, 0.225]
    )
])

# 모델 생성
model = models.resnet18(weights=None)
model.fc = nn.Linear(model.fc.in_features, len(CLASS_NAMES))

# 가중치 로드
state_dict = torch.load(MODEL_PATH, map_location=device)
model.load_state_dict(state_dict)
model.to(device)
model.eval()


def classify_scene(image_bytes: bytes) -> Dict:
    image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    x = transform(image).unsqueeze(0).to(device)

    with torch.no_grad():
        outputs = model(x)
        probs = torch.softmax(outputs, dim=1)
        conf, pred = torch.max(probs, dim=1)

    scene = CLASS_NAMES[pred.item()]
    confidence = float(conf.item())

    return {
        "scene": scene,
        "confidence": round(confidence, 4)
    }
