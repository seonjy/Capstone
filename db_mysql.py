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
                SELECT iso, shutter
                FROM Expert_Setting_Statistics
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
                SELECT message, tip
                FROM Guide_Text
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
                INSERT INTO User_History_Log
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
