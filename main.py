from fastapi import FastAPI, UploadFile, File, Form
from fastapi.middleware.cors import CORSMiddleware

# db_sqlite.py에서 함수 가져오기
from app.db_sqlite import init_db, insert_camera_log

app = FastAPI()

# CORS (앱/다른 노트북에서 접근 허용)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 서버 시작 시 DB 초기화 (테이블 자동 생성)
@app.on_event("startup")
def on_startup():
    init_db()

# 서버 살아있는지 확인용
@app.get("/")
def health():
    return {"ok": True, "message": "server alive"}

# ===============================
# 이미지 업로드 + 로그 저장 API
# ===============================
@app.post("/upload")
async def upload_image(
    user_id: str = Form(...),
    file: UploadFile = File(...)
):
    # 이미지 수신
    image_bytes = await file.read()

    # TODO: 나중에 실제 분석 결과로 교체
    scene_type = "unknown"
    rec_iso = None
    rec_shutter = None

    # SQLite에 camera_logs 저장
    log_id = insert_camera_log(
        user_id=user_id,
        scene_type=scene_type,
        rec_iso=rec_iso,
        rec_shutter=rec_shutter,
        is_satisfied=0
    )

    return {
        "ok": True,
        "log_id": log_id,
        "filename": file.filename
    }

