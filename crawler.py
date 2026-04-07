import os
import time
import requests
import pandas as pd
from io import BytesIO
from PIL import Image
import imagehash
from openpyxl import load_workbook
from openpyxl.styles import Alignment

API_KEY = "a085ca875a968a2a3296d7b1bf729808"

BASE_DIR = "photo_data"
TARGET_COUNT = 1000
PER_PAGE = 100
MAX_PAGES_PER_QUERY = 30
NO_PROGRESS_LIMIT = 5
REQUEST_TIMEOUT = 20

PHASH_DUP_THRESHOLD = 5

os.makedirs(BASE_DIR, exist_ok=True)

CATEGORY_CONFIG = {
    "food": {
        "queries": [
            "food photography",
            "dish food",
            "meal photography",
            "dessert photography",
            "restaurant food",
            "close up food"
        ],
        "positive_keywords": [
            "food", "dish", "meal", "dessert", "cake", "bread", "pasta",
            "pizza", "burger", "coffee", "drink", "restaurant", "lunch",
            "dinner", "brunch"
        ],
        "negative_keywords": [
            "landscape", "mountain", "sky", "forest", "portrait", "selfie",
            "wedding", "car", "street", "building"
        ]
    },

    "landscape": {
        "queries": [
            "mountain scenery",
            "sea landscape",
            "ocean view",
            "sky landscape",
            "forest landscape",
            "lake landscape",
            "natural scenery",
            "sunset landscape"
        ],
        "positive_keywords": [
            "landscape", "scenery", "mountain", "sea", "ocean", "sky",
            "forest", "lake", "nature", "sunset", "river", "valley",
            "waterfall", "beach"
        ],
        "negative_keywords": [
            "portrait", "selfie", "food", "dish", "meal", "wedding",
            "car", "motorcycle", "building interior", "menu", "product"
        ]
    },

    "night": {
        "queries": [
            "night photography",
            "city night",
            "night street",
            "nightscape",
            "night city lights",
            "low light photography"
        ],
        "positive_keywords": [
            "night", "nightscape", "city", "lights", "street", "dark",
            "low light", "midnight", "neon", "evening"
        ],
        "negative_keywords": [
            "food", "dish", "meal", "portrait studio", "daylight",
            "sunny", "mountain day", "beach day"
        ]
    },

    "portrait": {
        "queries": [
            "portrait photography",
            "face portrait",
            "person portrait",
            "outdoor portrait",
            "close up portrait",
            "human portrait"
        ],
        "positive_keywords": [
            "portrait", "face", "person", "people", "woman", "man",
            "girl", "boy", "human", "model"
        ],
        "negative_keywords": [
            "food", "dish", "meal", "landscape", "mountain", "ocean",
            "forest", "building", "car"
        ]
    },

    "contrast": {
        "queries": [
            "high contrast photography",
            "dramatic lighting",
            "light and shadow",
            "shadow photography",
            "contrast lighting",
            "strong shadows"
        ],
        "positive_keywords": [
            "contrast", "dramatic", "shadow", "light", "lighting",
            "silhouette", "high contrast", "moody"
        ],
        "negative_keywords": [
            "menu", "product", "food menu", "building plan"
        ]
    }
}

def flickr_call(method: str, params: dict):
    url = "https://www.flickr.com/services/rest/"
    base_params = {
        "method": method,
        "api_key": API_KEY,
        "format": "json",
        "nojsoncallback": 1
    }
    base_params.update(params)

    response = requests.get(url, params=base_params, timeout=REQUEST_TIMEOUT)
    response.raise_for_status()
    data = response.json()

    if data.get("stat") != "ok":
        raise RuntimeError(f"Flickr API 오류: {data}")

    return data


def search_photos(query: str, page: int):
    data = flickr_call(
        "flickr.photos.search",
        {
            "text": query,
            "page": page,
            "per_page": PER_PAGE,
            "sort": "relevance",
            "safe_search": 1,
            "content_types": 0,
            "media": "photos",
            "extras": "url_l,url_c,url_z,url_m,tags,title,owner"
        }
    )
    return data["photos"]["photo"]


def choose_image_url(photo: dict):
    for key in ["url_l", "url_c", "url_z", "url_m"]:
        if photo.get(key):
            return photo[key]
    return None


def is_relevant_photo(photo: dict, positive_keywords: list, negative_keywords: list):
    title = str(photo.get("title", "")).lower()
    tags = str(photo.get("tags", "")).lower()
    combined = f"{title} {tags}"

    for bad in negative_keywords:
        if bad.lower() in combined:
            return False

    for good in positive_keywords:
        if good.lower() in combined:
            return True

    return False


def get_exif(photo_id: str):
    try:
        data = flickr_call("flickr.photos.getExif", {"photo_id": photo_id})
        exif_list = data.get("photo", {}).get("exif", [])
    except Exception:
        return {}

    exif_map = {}

    for item in exif_list:
        tag = item.get("tag", "")
        label = item.get("label", "")
        raw = item.get("raw", {})

        if isinstance(raw, dict):
            raw_value = raw.get("_content", "")
        else:
            raw_value = str(raw)

        if tag:
            exif_map[tag] = raw_value
        if label:
            exif_map[label] = raw_value

    return exif_map


def extract_required_fields(exif_map: dict):
    iso = (
        exif_map.get("ISO")
        or exif_map.get("ISO Speed")
        or exif_map.get("ISOSpeedRatings")
        or ""
    )

    shutter_speed = (
        exif_map.get("ExposureTime")
        or exif_map.get("Exposure")
        or exif_map.get("Shutter Speed")
        or ""
    )

    focal_length = (
        exif_map.get("FocalLength")
        or exif_map.get("Focal Length")
        or ""
    )

    exposure_bias = (
        exif_map.get("ExposureBiasValue")
        or exif_map.get("Exposure Bias")
        or exif_map.get("Exposure Compensation")
        or ""
    )

    white_balance = (
        exif_map.get("WhiteBalance")
        or exif_map.get("White Balance")
        or ""
    )

    if white_balance == "0":
        white_balance = "Auto"
    elif white_balance == "1":
        white_balance = "Manual"

    return {
        "ISO": str(iso).strip(),
        "셔터스피드": str(shutter_speed).strip(),
        "초점거리": str(focal_length).strip(),
        "노출보정": str(exposure_bias).strip(),
        "화이트밸런스": str(white_balance).strip()
    }


def count_valid_fields(extracted: dict):
    return sum(1 for v in extracted.values() if str(v).strip() != "")


def download_image_bytes(url: str):
    response = requests.get(url, timeout=REQUEST_TIMEOUT)
    response.raise_for_status()
    return response.content


def get_image_phash(image_bytes: bytes):
    try:
        img = Image.open(BytesIO(image_bytes)).convert("RGB")
        return imagehash.phash(img)
    except Exception:
        return None


def is_similar_duplicate(new_hash, existing_hashes, threshold=PHASH_DUP_THRESHOLD):
    if new_hash is None:
        return False

    for old_hash in existing_hashes:
        if old_hash is None:
            continue
        if abs(new_hash - old_hash) <= threshold:
            return True
    return False


def save_excel(records: list, excel_path: str):
    df = pd.DataFrame(records)
    df.to_excel(excel_path, index=False)

    wb = load_workbook(excel_path)
    ws = wb.active

    center = Alignment(horizontal="center", vertical="center")

    for row in ws.iter_rows():
        for cell in row:
            cell.alignment = center

    for col in ws.columns:
        max_len = 0
        col_letter = col[0].column_letter
        for cell in col:
            value = "" if cell.value is None else str(cell.value)
            max_len = max(max_len, len(value))
        ws.column_dimensions[col_letter].width = max(14, min(max_len + 2, 30))

    wb.save(excel_path)


def collect_category(category_name: str):
    config = CATEGORY_CONFIG[category_name]
    category_dir = os.path.join(BASE_DIR, category_name)
    os.makedirs(category_dir, exist_ok=True)

    records = []
    saved_photo_ids = set()
    saved_hashes = []
    current_count = 1

    for query in config["queries"]:
        if current_count > TARGET_COUNT:
            break

        no_progress_pages = 0

        for page in range(1, MAX_PAGES_PER_QUERY + 1):
            if current_count > TARGET_COUNT:
                break

            before_count = current_count

            try:
                photos = search_photos(query, page)
            except Exception:
                break

            if not photos:
                break

            for photo in photos:
                if current_count > TARGET_COUNT:
                    break

                photo_id = str(photo.get("id", "")).strip()
                if not photo_id or photo_id in saved_photo_ids:
                    continue

                if not is_relevant_photo(
                    photo,
                    config["positive_keywords"],
                    config["negative_keywords"]
                ):
                    continue

                img_url = choose_image_url(photo)
                if not img_url:
                    continue

                exif_map = get_exif(photo_id)
                extracted = extract_required_fields(exif_map)

                if count_valid_fields(extracted) < 3:
                    continue

                try:
                    image_bytes = download_image_bytes(img_url)
                except Exception:
                    continue

                phash = get_image_phash(image_bytes)
                if is_similar_duplicate(phash, saved_hashes):
                    continue

                file_name = f"{category_name}{current_count}.jpg"
                file_path = os.path.join(category_dir, file_name)

                try:
                    with open(file_path, "wb") as f:
                        f.write(image_bytes)
                except Exception:
                    continue

                record = {
                    "name": file_name,
                    "ISO": extracted["ISO"],
                    "셔터스피드": extracted["셔터스피드"],
                    "초점거리": extracted["초점거리"],
                    "노출보정": extracted["노출보정"],
                    "화이트밸런스": extracted["화이트밸런스"],
                }

                records.append(record)
                saved_photo_ids.add(photo_id)
                saved_hashes.append(phash)

                current_count += 1

                time.sleep(0.05)

            if current_count == before_count:
                no_progress_pages += 1
            else:
                no_progress_pages = 0
                excel_path = os.path.join(category_dir, f"{category_name}.xlsx")
                save_excel(records, excel_path)

            if no_progress_pages >= NO_PROGRESS_LIMIT:
                break

    excel_path = os.path.join(category_dir, f"{category_name}.xlsx")
    save_excel(records, excel_path)


def collect_all_categories():
    for category_name in ["food", "landscape", "night", "portrait", "contrast"]:
        collect_category(category_name)


if __name__ == "__main__":
    collect_all_categories()
