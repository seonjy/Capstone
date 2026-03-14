import os
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
    print("=== analyze_image called ===")

    np_arr = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

    if img is None:
        raise ValueError(
            "이미지 디코딩 실패: HEIC 형식이거나 지원되지 않는 이미지입니다. JPG/PNG로 변환 후 다시 시도하세요."
        )

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    brightness = float(np.mean(gray))
    contrast = float(np.std(gray))

    adjusted_img = adjust_brightness(img, brightness)

    current_dir = os.path.dirname(os.path.abspath(__file__))
    adjusted_filename = "adjusted.jpg"
    adjusted_path = os.path.join(current_dir, adjusted_filename)

    print("current_dir =", current_dir)
    print("adjusted_path =", adjusted_path)

    saved = cv2.imwrite(adjusted_path, adjusted_img)
    print("cv2.imwrite result =", saved)

    if not saved:
        raise ValueError("보정 이미지 저장 실패")

    return {
        "brightness": brightness,
        "contrast": contrast,
        "adjusted_image": adjusted_filename
    }
