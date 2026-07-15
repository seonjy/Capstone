import asyncio
import os

from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from app.services.analyzer import analyze_image
from app.services.classifier import classify_scene
from app.db_mysql import (
    test_db,
    get_expert_setting,
    get_guide_text,
    save_user_history,
)

app = FastAPI()

# -------------------------------
# adjusted.jpg 저장 위치와 동일한 폴더를 정적 폴더로 공개
# app/main.py 기준 -> app/services
# -------------------------------
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
IMAGE_DIR = os.path.join(BASE_DIR, "services")

app.mount("/images", StaticFiles(directory=IMAGE_DIR), name="images")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/")
def health():
    return {"ok": True, "message": "server alive"}


@app.get("/db-test")
def db_test():
    return test_db()


def get_brightness_level(brightness: float) -> str:
    if brightness < 80:
        return "dark"
    elif brightness > 170:
        return "bright"
    return "normal"


@app.post("/upload")
async def upload_image(
    user_id: str = Form(...),
    file: UploadFile = File(...)
):
    image_bytes = await file.read()

    if not image_bytes:
        raise HTTPException(status_code=400, detail="빈 파일입니다.")

    try:
        # Track A, Track B 병렬 실행
        metrics, scene_result = await asyncio.gather(
            asyncio.to_thread(analyze_image, image_bytes),
            asyncio.to_thread(classify_scene, image_bytes),
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"이미지 분석 중 오류가 발생했습니다: {str(e)}")

    brightness = float(metrics.get("brightness", 0.0))
    contrast = float(metrics.get("contrast", 0.0))
    brightness_level = get_brightness_level(brightness)

    scene = scene_result["scene"]
    print("===== AI 분석 결과 =====")
    print("scene :", scene_result.get("scene"))
    print("confidence :", scene_result.get("confidence"))
    print("=======================")

    try:
        setting_row = get_expert_setting(scene, brightness_level)
        guide_row = get_guide_text(scene, brightness_level)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"추천값 조회 중 오류가 발생했습니다: {str(e)}")

    recommended_iso = None
    recommended_shutter = None
    recommended_aperture = None
    recommended_focal_length = None
    recommended_ev = None
    recommended_white_balance = None
    recommended_iso_min = None
    recommended_iso_max = None
    recommended_shutter_min = None
    recommended_shutter_max = None
    message = None
    tip = None

    if setting_row:
        recommended_iso = setting_row.get("iso")
        recommended_iso_min = setting_row.get("iso_min")
        recommended_iso_max = setting_row.get("iso_max")
        
        recommended_shutter = setting_row.get("shutter")
        recommended_shutter_min = setting_row.get("shutter_min")
        recommended_shutter_max = setting_row.get("shutter_max")
        recommended_aperture = setting_row.get("aperture")
        recommended_focal_length = setting_row.get("focal_length")
        recommended_ev = setting_row.get("ev")
        recommended_white_balance = setting_row.get("white_balance")

    print("===== 추천 카메라 설정값 =====")
    print("ISO :", recommended_iso)
    print("셔터속도 :", recommended_shutter)
    print("조리개 :", recommended_aperture)
    print("화이트밸런스 :", recommended_white_balance)
    print("노출값(EV) :", recommended_ev)
    print("초점거리 :", recommended_focal_length)
    print("============================")

    if guide_row:
        message = guide_row.get("message")
        tip = guide_row.get("tip")

    try:
        log_id = save_user_history(
            scene=scene,
            brightness=brightness_level,
            input_iso=None,
            input_shutter=None,
            recommended_iso=recommended_iso,
            recommended_shutter=recommended_shutter,
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"로그 저장 중 오류가 발생했습니다: {str(e)}")

    adjusted_filename = metrics.get("adjusted_image", "adjusted.jpg")
    adjusted_image_url = f"/images/{adjusted_filename}"

    return {
        "ok": True,
        "message": "image uploaded and analyzed",
        "user_id": user_id,
        "log_id": log_id,
        "filename": file.filename,
        "track_a": {
            "brightness": brightness,
            "contrast": contrast,
            "brightness_level": brightness_level,
        },
        "track_b": scene_result,
        "recommendation": {
        "scene": scene,
        "recommended_iso": recommended_iso,
        "recommended_iso_min": recommended_iso_min,
        "recommended_iso_max": recommended_iso_max,
        
        "recommended_shutter": recommended_shutter,
        "recommended_shutter_min": recommended_shutter_min,
        "recommended_shutter_max": recommended_shutter_max,
        "recommended_aperture": recommended_aperture,
        "recommended_focal_length": recommended_focal_length,
        "recommended_ev": recommended_ev,
        "recommended_white_balance": recommended_white_balance,
        },
        "guide": {
            "message": message,
            "tip": tip,
        },
        "adjusted_image_info": {
            "saved_as": adjusted_filename,
            "url": adjusted_image_url
        }
    }
@app.post("/preview-analyze")
async def preview_analyze(file: UploadFile = File(...)):
    image_bytes = await file.read()

    if not image_bytes:
        raise HTTPException(status_code=400, detail="빈 파일입니다.")

    try:
        metrics, scene_result = await asyncio.gather(
            asyncio.to_thread(analyze_image, image_bytes),
            asyncio.to_thread(classify_scene, image_bytes),
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"프리뷰 분석 중 오류가 발생했습니다: {str(e)}")

    brightness = float(metrics.get("brightness", 0.0))
    contrast = float(metrics.get("contrast", 0.0))
    brightness_level = get_brightness_level(brightness)

    scene = scene_result.get("scene")

    try:
        setting_row = get_expert_setting(scene, brightness_level)
        guide_row = get_guide_text(scene, brightness_level)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"프리뷰 추천값 조회 중 오류가 발생했습니다: {str(e)}")

    recommended_iso = None
    recommended_shutter = None
    recommended_aperture = None
    recommended_focal_length = None
    recommended_ev = None
    recommended_white_balance = None

    if setting_row:
        recommended_iso = setting_row.get("iso")
        recommended_shutter = setting_row.get("shutter")
        recommended_aperture = setting_row.get("aperture")
        recommended_focal_length = setting_row.get("focal_length")
        recommended_ev = setting_row.get("ev")
        recommended_white_balance = setting_row.get("white_balance")

    message = None
    tip = None

    if guide_row:
        message = guide_row.get("message")
        tip = guide_row.get("tip")

    print("===== 실시간 프리뷰 분석 =====")
    print("scene :", scene)
    print("confidence :", scene_result.get("confidence"))
    print("brightness :", brightness)
    print("brightness_level :", brightness_level)
    print("contrast :", contrast)
    print("ISO :", recommended_iso)
    print("셔터속도 :", recommended_shutter)
    print("조리개 :", recommended_aperture)
    print("화이트밸런스 :", recommended_white_balance)
    print("노출값(EV) :", recommended_ev)
    print("초점거리 :", recommended_focal_length)
    print("============================")

    return {
        "ok": True,
        "mode": "preview",
        "track_a": {
            "brightness": brightness,
            "contrast": contrast,
            "brightness_level": brightness_level,
        },
        "track_b": scene_result,
        "recommendation": {
            "scene": scene,
            "recommended_iso": recommended_iso,
            "recommended_shutter": recommended_shutter,
            "recommended_aperture": recommended_aperture,
            "recommended_focal_length": recommended_focal_length,
            "recommended_ev": recommended_ev,
            "recommended_white_balance": recommended_white_balance,
        },
        "guide": {
            "message": message,
            "tip": tip,
        }
    }
