from app.services.analyzer import analyze_image
from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware

# DB 관련 함수/상수 import
from app.db_sqlite import (
    init_db,
    insert_camera_log,
    get_conn,
    DB_PATH,
)

app = FastAPI()

# ===============================
# CORS 설정 (앱/외부 노트북 접근 허용)
# ===============================
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ===============================
# 서버 시작 시 DB 초기화
# ===============================
@app.on_event("startup")
def on_startup():
    init_db()

# ===============================
# 서버 상태 확인용 (Health Check)
# ===============================
@app.get("/")
def health():
    return {"ok": True, "message": "server alive"}

# ===============================
# camera.db 내용 확인용 API
# expert_scene_stats 테이블 전체 조회
# ===============================
@app.get("/camera-db")
def get_camera_db_contents():
    conn = get_conn()
    try:
        rows = conn.execute("SELECT * FROM expert_scene_stats").fetchall()
        return {
            "database_path": str(DB_PATH),
            "database_name": "camera.db",
            "table_name": "expert_scene_stats",
            "count": len(rows),
            "data": [dict(row) for row in rows],
        }
    finally:
        conn.close()

# ===============================
# scene별 추천값 조회 함수
# expert_scene_stats 에서 iso_mean, shutter_mean 사용
# ===============================
def get_recommendation_by_scene(scene: str):
    conn = get_conn()
    try:
        row = conn.execute(
            """
            SELECT scene, iso_mean, shutter_mean
            FROM expert_scene_stats
            WHERE scene = ?
            """,
            (scene,),
        ).fetchone()

        if row is None:
            return None

        return {
            "scene": row["scene"],
            "recommended_iso": row["iso_mean"],
            "recommended_shutter": row["shutter_mean"],
        }
    finally:
        conn.close()

# ===============================
# 이미지 업로드 + 분석 + 추천값 조회 + 로그 저장
# ===============================
@app.post("/upload")
async def upload_image(
    user_id: str = Form(...),
    file: UploadFile = File(...)
):
    # 이미지 읽기
    image_bytes = await file.read()

    if not image_bytes:
        raise HTTPException(status_code=400, detail="빈 파일입니다.")

    # OpenCV 분석
    metrics = analyze_image(image_bytes)

    # analyzer.py 에서 계산한 scene 사용
    analyzed_scene = metrics.get("scene", "normal")

    # expert_scene_stats 테이블에 맞게 scene 매핑
    # 현재 DB에는 low_light / normal 이 없으므로 임시 매핑
    if analyzed_scene == "low_light":
        db_scene = "night"
    elif analyzed_scene == "high_contrast":
        db_scene = "contrast"
    else:
        db_scene = "landscape"

    # 추천값 조회
    recommendation = get_recommendation_by_scene(db_scene)

    if recommendation is None:
        rec_iso = None
        rec_shutter = None
    else:
        rec_iso = recommendation["recommended_iso"]
        rec_shutter = recommendation["recommended_shutter"]

    # 로그 저장
    log_id = insert_camera_log(
        user_id=user_id,
        scene_type=analyzed_scene,
        rec_iso=rec_iso,
        rec_shutter=rec_shutter,
    )

    # 응답 반환
    return {
        "ok": True,
        "message": "uploaded, analyzed & recommended",
        "log_id": log_id,
        "filename": file.filename,
        "scene_type": analyzed_scene,
        "db_scene": db_scene,
        "metrics": metrics,
        "recommendation": {
            "recommended_iso": rec_iso,
            "recommended_shutter": rec_shutter,
        },
    }
