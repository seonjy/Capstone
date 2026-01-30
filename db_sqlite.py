import os
import sqlite3
from typing import Optional

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DB_PATH = os.path.join(BASE_DIR, "data", "camera.db")


def get_conn() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    return conn


def init_db() -> None:
    os.makedirs(os.path.join(BASE_DIR, "data"), exist_ok=True)

    conn = get_conn()
    try:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS camera_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT NOT NULL,
                scene_type TEXT,
                rec_iso INTEGER,
                rec_shutter TEXT,
                is_satisfied INTEGER DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            );
            """
        )
        conn.commit()
    finally:
        conn.close()


def insert_camera_log(
    user_id: str,
    scene_type: Optional[str] = None,
    rec_iso: Optional[int] = None,
    rec_shutter: Optional[str] = None,
    is_satisfied: int = 0,
) -> int:
    conn = get_conn()
    try:
        cur = conn.execute(
            """
            INSERT INTO camera_logs (user_id, scene_type, rec_iso, rec_shutter, is_satisfied)
            VALUES (?, ?, ?, ?, ?)
            """,
            (user_id, scene_type, rec_iso, rec_shutter, is_satisfied),
        )
        conn.commit()
        return int(cur.lastrowid)
    finally:
        conn.close()

