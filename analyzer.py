import cv2
import numpy as np

def analyze_image(image_bytes: bytes) -> dict:

    np_arr = np.frombuffer(image_bytes, np.uint8)

    img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
    if img is None:
        raise ValueError("이미지 디코딩 실패")

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    brightness = float(np.mean(gray))
    contrast = float(np.std(gray))

    # 장면 판단 (임시 로직)
    if brightness < 80:
        scene = "low_light"
    elif contrast > 50:
        scene = "high_contrast"
    else:
        scene = "normal"

    return {
        "brightness": round(brightness, 2),
        "contrast": round(contrast, 2),
        "scene": scene
    }
