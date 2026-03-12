from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
import pymysql
import cv2
import numpy as np
import torch
import torch.nn as nn
from torchvision import models, transforms
from PIL import Image
import io
import os

load_dotenv()

app = FastAPI()

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 장면 분류 레이블
SCENE_LABELS = ['contrast', 'food', 'landscape', 'night', 'portrait']

# 학습된 모델 불러오기
def load_model():
    model = models.resnet18(weights=None)
    model.fc = nn.Linear(model.fc.in_features, len(SCENE_LABELS))
    model.load_state_dict(torch.load('scene_model.pth', map_location='cpu'))
    model.eval()
    return model

model = load_model()

# 이미지 전처리
transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406],
                         std=[0.229, 0.224, 0.225])
])

# DB 연결
def get_db():
    return pymysql.connect(
        host=os.getenv('DB_HOST'),
        port=int(os.getenv('DB_PORT')),
        user=os.getenv('DB_USER'),
        password=os.getenv('DB_PASSWORD'),
        db=os.getenv('DB_NAME'),
        charset='utf8mb4'
    )

# Track A — 밝기 분석
def analyze_brightness(image_bytes):
    np_arr = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    brightness = hsv[:, :, 2].mean()
    if brightness < 85:
        return "dark"
    elif brightness < 170:
        return "normal"
    else:
        return "bright"

# Track B — 장면 분류
def analyze_scene(image_bytes):
    img = Image.open(io.BytesIO(image_bytes)).convert('RGB')
    tensor = transform(img).unsqueeze(0)
    with torch.no_grad():
        output = model(tensor)
    predicted = torch.argmax(output, dim=1).item()
    return SCENE_LABELS[predicted]

# 서버 상태 확인
@app.get("/")
def health():
    return {"ok": True, "message": "server alive"}

# 이미지 업로드 + 분석 + DB 조회
@app.post("/upload")
async def upload_image(
    user_id: str = Form(...),
    file: UploadFile = File(...)
):
    image_bytes = await file.read()

    if not image_bytes:
        raise HTTPException(status_code=400, detail="빈 파일입니다.")

    brightness = analyze_brightness(image_bytes)
    scene = analyze_scene(image_bytes)

    db = get_db()
    cursor = db.cursor()
    cursor.execute(
        "SELECT iso, shutter FROM Expert_Setting_Statistics WHERE scene=%s AND brightness=%s",
        (scene, brightness)
    )
    setting = cursor.fetchone()
    cursor.execute(
        "SELECT message, tip FROM Guide_Text WHERE scene=%s AND brightness=%s",
        (scene, brightness)
    )
    guide = cursor.fetchone()
    db.close()

    return {
        "ok": True,
        "user_id": user_id,
        "filename": file.filename,
        "scene": scene,
        "brightness": brightness,
        "recommended_iso": setting[0] if setting else None,
        "recommended_shutter": setting[1] if setting else None,
        "message": guide[0] if guide else None,
        "tip": guide[1] if guide else None
    }