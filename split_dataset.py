import random
import shutil
from pathlib import Path

# 원본 데이터
SOURCE_DIR = Path("dataset_raw")

# 결과 저장
TARGET_DIR = Path("dataset")

TRAIN_RATIO = 0.7
VAL_RATIO = 0.15
TEST_RATIO = 0.15

random.seed(42)

classes = [d.name for d in SOURCE_DIR.iterdir() if d.is_dir()]

for class_name in classes:
    class_dir = SOURCE_DIR / class_name

    image_files = [
        f for f in class_dir.iterdir()
        if f.suffix.lower() in [".jpg", ".jpeg", ".png"]
    ]

    random.shuffle(image_files)

    total = len(image_files)
    train_end = int(total * TRAIN_RATIO)
    val_end = train_end + int(total * VAL_RATIO)

    train_files = image_files[:train_end]
    val_files = image_files[train_end:val_end]
    test_files = image_files[val_end:]

    for split_name, files in {
        "train": train_files,
        "val": val_files,
        "test": test_files
    }.items():
        split_dir = TARGET_DIR / split_name / class_name
        split_dir.mkdir(parents=True, exist_ok=True)

        for file in files:
            shutil.copy(file, split_dir / file.name)

    print(f"{class_name}: {total}개 → train:{len(train_files)}, val:{len(val_files)}, test:{len(test_files)}")

print("데이터 분할 완료")