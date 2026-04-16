import pandas as pd
from pathlib import Path

# 1. 폴더 경로 설정
metadata_dir = Path("metadata")
dataset_dir = Path("dataset_raw")

# 2. metadata 폴더 안의 모든 xlsx 파일 찾기
excel_files = list(metadata_dir.glob("*.xlsx"))

print("엑셀 파일 개수:", len(excel_files))

if len(excel_files) == 0:
    print(" metadata 폴더 안에 xlsx 파일이 없습니다.")
    exit()

total_missing = 0

# 3. 각 엑셀 파일마다 검사
for excel_path in excel_files:
    print("\n==============================")
    print("검사 중인 엑셀:", excel_path.name)

    try:
        df = pd.read_excel(excel_path)
    except Exception as e:
        print(" 엑셀 읽기 실패:", e)
        continue

    print("컬럼명:", df.columns.tolist())
    print("행 개수:", len(df))

    # 4. 'name' 컬럼 존재 여부 확인
    if "name" not in df.columns:
        print(" 'name' 컬럼이 없습니다. 엑셀 컬럼명을 확인하세요.")
        continue

    missing_files = []
    matched_files = 0

    # 5. 엑셀의 각 파일명이 실제 이미지로 존재하는지 검사
    for _, row in df.iterrows():
        filename = str(row["name"]).strip()

        found = False

        for class_dir in dataset_dir.iterdir():
            if class_dir.is_dir():
                file_path = class_dir / filename
                if file_path.exists():
                    found = True
                    matched_files += 1
                    break

        if not found:
            missing_files.append(filename)

    print("매칭된 파일 수:", matched_files)
    print("누락된 파일 수:", len(missing_files))

    if len(missing_files) > 0:
        print("누락 파일 예시 10개:")
        for name in missing_files[:10]:
            print("-", name)

    total_missing += len(missing_files)

print("\n==============================")
print("전체 누락 파일 수:", total_missing)
print("==============================")
