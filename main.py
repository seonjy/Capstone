from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app.services.analyzer import analyze_image

# MySQL DB 테스트용 import
from app.db_mysql import test_db

app = FastAPI()

# ===============================
# CORS 설정 (앱/외부 접근 허용)
# ===============================
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ===============================
# 서버 상태 확인용
# ===============================
@app.get("/")
def health():
    return {
        "ok": True,
        "message": "server alive"
    }

# ===============================
# MySQL 연결 테스트 API
# ===============================
@app.get("/db-test")
def db_test():
    return test_db()

# ===============================
# 이미지 업로드 + 분석 (현재는 DB 저장 없음)
# ===============================
@app.post("/upload")
async def upload_image(
    user_id: str = Form(...),
    file: UploadFile = File(...)
):

    image_bytes = await file.read()

    if not image_bytes:
        raise HTTPException(status_code=400, detail="빈 파일입니다.")

    # OpenCV 분석
    metrics = analyze_image(image_bytes)

    # analyzer.py에서 계산한 scene
    scene = metrics.get("scene", "unknown")

    return {
        "ok": True,
        "message": "image uploaded & analyzed",
        "user_id": user_id,
        "filename": file.filename,
        "scene": scene,
        "metrics": metrics
    }
