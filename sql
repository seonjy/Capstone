// DB설계도
// 1. 전문가 실측 통계 테이블 (실제 사진 EXIF 분석을 통한 장면별 가이드)
CREATE TABLE IF NOT EXISTS expert_scene_stats (
    scene TEXT PRIMARY KEY,
    sample_count INTEGER,
    iso_min INTEGER,
    iso_mean INTEGER,
    iso_max INTEGER,
    shutter_min REAL,
    shutter_mean REAL,
    shutter_max REAL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

// 2. 문헌 기반 가이드 테이블 (표본 부족 시나리오를 대비한 사진 이론/논문 기반 가이드 데이터)
CREATE TABLE IF NOT EXISTS expert_guide_docs (
    scene TEXT PRIMARY KEY,
    recommended_iso INTEGER,
    recommended_shutter TEXT,
    light_condition TEXT,
    source_ref TEXT
);

// 3. 실시간 촬영 및 피드백 로그 테이블 (사용자의 앱 사용 기록 및 시스템 만족도 데이터 축적)
CREATE TABLE IF NOT EXISTS camera_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    scene_type TEXT,
    rec_iso INTEGER,
    rec_shutter TEXT,
    is_satisfied INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(scene_type) REFERENCES expert_scene_stats(scene)
);
