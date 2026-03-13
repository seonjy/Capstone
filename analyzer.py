import cv2
import numpy as np


def adjust_brightness(img, brightness):
    if brightness < 80:
        adjusted = cv2.convertScaleAbs(img, alpha=1.0, beta=50)
    elif brightness > 180:
        adjusted = cv2.convertScaleAbs(img, alpha=1.0, beta=-40)
    else:
        adjusted = img
    return adjusted


def analyze_image(image_bytes: bytes) -> dict:
    np_arr = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

    # 디코딩 실패 방지
    if img is None:
        raise ValueError("이미지 디코딩 실패: HEIC 형식이거나 지원되지 않는 이미지입니다. JPG/PNG로 변환 후 다시 시도하세요.")

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    brightness = float(np.mean(gray))
    contrast = float(np.std(gray))

    adjusted_img = adjust_brightness(img, brightness)
    cv2.imwrite("adjusted.jpg", adjusted_img)

    return {
        "brightness": brightness,
        "contrast": contrast
        "adjusted_image": "/images/adjusted.jpg"
    }
