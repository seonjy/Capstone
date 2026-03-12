from fastapi import FastAPI, File, UploadFile
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

SCENE_LABELS = ['contrast', 'food', 'landscape', 'night', 'portrait']

def load_model():
    model = models.resnet18(weights=None)
    model.fc = nn.Linear(model.fc.in_features, len(SCENE_LABELS))
    model.load_state_dict(torch.load('scene_model.pth', map_location='cpu'))
    model.eval()
    return model

model = load_model()

transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406],
                         std=[0.229, 0.224, 0.225])
])

def get_db():
    return pymysql.connect(
        host=os.getenv('DB_HOST'),
        port=int(os.getenv('DB_PORT')),
        user=os.getenv('DB_USER'),
        password=os.getenv('DB_PASSWORD'),
        db=os.getenv('DB_NAME'),
        charset='utf8mb4'
    )

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

def analyze_scene(image_bytes):
    img = Image.open(io.BytesIO(image_bytes)).convert('RGB')
    tensor = transform(img).unsqueeze(0)
    with torch.no_grad():
        output = model(tensor)
    predicted = torch.argmax(output, dim=1).item()
    return SCENE_LABELS[predicted]

@app.get("/")
def root():
    return {"message": "서버 정상 작동중"}

@app.post("/analyze")
async def analyze(file: UploadFile = File(...)):
    image_bytes = await file.read()
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
        "scene": scene,
        "brightness": brightness,
        "recommended_iso": setting[0] if setting else None,
        "recommended_shutter": setting[1] if setting else None,
        "message": guide[0] if guide else None,
        "tip": guide[1] if guide else None
    }