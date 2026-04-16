from pathlib import Path

from PIL import Image
from rembg import remove


SOURCE_DIR_CANDIDATES = (
    Path("frontend/static/customer/images"),
    Path("src/main/resources/static/customer/images"),
)
SUPPORTED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}


def process_image(source_path: Path, output_path: Path) -> None:
    with source_path.open("rb") as input_file:
        input_bytes = input_file.read()

    output_bytes = remove(input_bytes)
    output_path.write_bytes(output_bytes)

    # Normalize final files as PNG with transparency.
    with Image.open(output_path) as image:
        image.convert("RGBA").save(output_path, "PNG")


def main() -> None:
    source_dir = next((path for path in SOURCE_DIR_CANDIDATES if path.exists()), None)
    if source_dir is None:
        print("Source images directory not found.")
        print(f"Checked: {', '.join(str(path) for path in SOURCE_DIR_CANDIDATES)}")
        return

    output_dir = source_dir / "cutouts"
    
    output_dir.mkdir(parents=True, exist_ok=True)

    source_files = sorted(
        path for path in source_dir.iterdir()
        if path.is_file() and path.suffix.lower() in SUPPORTED_EXTENSIONS
    )

    if not source_files:
        print("No source images found.")
        return

    processed = 0
    skipped = 0

    for source_path in source_files:
        output_path = output_dir / f"{source_path.stem}.png"
        if output_path.exists():
            skipped += 1
            print(f"Skipped existing: {output_path.name}")
            continue

        print(f"Processing: {source_path.name}")
        process_image(source_path, output_path)
        processed += 1

    print(f"Done. Processed={processed}, Skipped={skipped}, OutputDir={output_dir}")


if __name__ == "__main__":
    main()
