import pymysql
from typing import Optional, Dict


def get_conn():
    return pymysql.connect(
        host="127.0.0.1",
        port=3306,
        user="root",
        password="passwd@123",
        database="camera_assistant",
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=True,
    )


def test_db():
    conn = get_conn()
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT 1 AS ok")
            return cursor.fetchone()
    finally:
        conn.close()


def get_expert_setting(scene: str, brightness: str) -> Optional[Dict]:
    conn = get_conn()
    try:
        with conn.cursor() as cursor:
            sql = """
                SELECT 
                    iso,
                    iso_min,
                    iso_max,
                    shutter,
                    shutter_min,
                    shutter_max,
                    aperture,
                    focal_length,
                    ev,
                    white_balance
                FROM expert_setting_statistics
                WHERE scene = %s AND brightness = %s
                LIMIT 1
            """
            cursor.execute(sql, (scene, brightness))
            return cursor.fetchone()
    finally:
        conn.close()


def get_guide_text(scene: str, brightness: str) -> Optional[Dict]:
    conn = get_conn()
    try:
        with conn.cursor() as cursor:
            sql = """
                SELECT
                    message,
                    tip,
                    guide_1_label,
                    guide_1_text,
                    guide_2_label,
                    guide_2_text,
                    guide_3_label,
                    guide_3_text
                FROM guide_text
                WHERE scene = %s AND brightness = %s
                LIMIT 1
            """
            cursor.execute(sql, (scene, brightness))
            return cursor.fetchone()
    finally:
        conn.close()


def save_user_history(
    scene: Optional[str],
    brightness: Optional[str],
    input_iso: Optional[int],
    input_shutter: Optional[str],
    recommended_iso: Optional[int],
    recommended_shutter: Optional[str],
) -> int:
    conn = get_conn()
    try:
        with conn.cursor() as cursor:
            sql = """
                INSERT INTO user_history_log
                (
                    scene,
                    brightness,
                    input_iso,
                    input_shutter,
                    recommended_iso,
                    recommended_shutter
                )
                VALUES (%s, %s, %s, %s, %s, %s)
            """
            cursor.execute(
                sql,
                (
                    scene,
                    brightness,
                    input_iso,
                    input_shutter,
                    recommended_iso,
                    recommended_shutter,
                ),
            )
            return cursor.lastrowid
    finally:
        conn.close()
