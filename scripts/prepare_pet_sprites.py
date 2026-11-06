"""Deterministically register the user's existing sheets; never redraw the pet."""
import argparse
import hashlib
import json
from collections import deque
from pathlib import Path

from PIL import Image, ImageDraw


SHEETS = [
    ("idle", "สไปรต์ชีตสาวน้อยหูสุนัขชิบิ (1).png", [0, 0, 0, 4, 0, 0, 6, 0]),
    ("tap", "ชิบิสาวหูสุนัขกับปฏิกิริยาหัวใจ.png", list(range(8))),
    ("greeting", "สติ๊กเกอร์พิกเซลสวัสดีวันนี้.png", list(range(8))),
    ("saved", "สาวน้อยหูสุนัขฉลองบันทึกสำเร็จ.png", [0, 1, 2, 3, 3, 2, 1, 0]),
    ("rest", "犬耳ちび少女の休息アニメーション.png", [3, 3, 4, 4, 3, 4, 3, 4]),
]


def clear_connected_background(image):
    """Remove only pale neutral pixels connected to the frame's outside edge."""
    image = image.convert("RGBA")
    width, height = image.size
    pixels = image.load()
    visited = bytearray(width * height)
    queue = deque()

    def visit(x, y):
        index = y * width + x
        if visited[index]:
            return
        visited[index] = 1
        r, g, b, _ = pixels[x, y]
        if min(r, g, b) >= 220 and max(r, g, b) - min(r, g, b) <= 22:
            queue.append((x, y))

    for x in range(width):
        visit(x, 0)
        visit(x, height - 1)
    for y in range(height):
        visit(0, y)
        visit(width - 1, y)
    while queue:
        x, y = queue.popleft()
        pixels[x, y] = (0, 0, 0, 0)
        for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
            if 0 <= nx < width and 0 <= ny < height:
                visit(nx, ny)
    return image


def prepare(source_dir, output, preview):
    cell_width, cell_height = 192, 208
    atlas = Image.new("RGBA", (8 * cell_width, len(SHEETS) * cell_height))
    contact = Image.new("RGB", (8 * cell_width, len(SHEETS) * (cell_height + 28)), "#FFF4E8")
    labels = ImageDraw.Draw(contact)
    manifest = {"cell": [cell_width, cell_height], "rows": []}
    for row, (state, filename, sequence) in enumerate(SHEETS):
        path = source_dir / filename
        with Image.open(path) as opened:
            sheet = opened.convert("RGB")
        assert sheet.size == (1774, 887), f"Unexpected geometry: {filename}"
        frames = []
        for index in sequence:
            col, source_row = index % 4, index // 4
            left, top = round(col * sheet.width / 4), round(source_row * sheet.height / 2)
            right, bottom = round((col + 1) * sheet.width / 4), round((source_row + 1) * sheet.height / 2)
            # Crop borders/grid rules. Greeting speech is baked in above the character;
            # runtime greeting copy is real accessible text instead.
            top += 112 if state == "greeting" else 6
            frame = clear_connected_background(sheet.crop((left + 6, top, right - 6, bottom - 6)))
            if state == "greeting":
                clear_speech_fragments(frame)
            frames.append(frame)
        bounds = [frame.getbbox() for frame in frames]
        assert all(bounds), state
        crop = (
            min(b[0] for b in bounds), min(b[1] for b in bounds),
            max(b[2] for b in bounds), max(b[3] for b in bounds),
        )
        # One transform per strip: preserve relative positions and frame-to-frame scale.
        scale = min(176 / (crop[2] - crop[0]), 192 / (crop[3] - crop[1]))
        size = (round((crop[2] - crop[0]) * scale), round((crop[3] - crop[1]) * scale))
        for col, frame in enumerate(frames):
            registered = frame.crop(crop).resize(size, Image.Resampling.NEAREST)
            tile = Image.new("RGBA", (cell_width, cell_height))
            tile.alpha_composite(registered, ((cell_width - size[0]) // 2, 202 - size[1]))
            assert tile.getbbox() is not None
            assert tile.getpixel((0, 0))[3] == 0
            atlas.alpha_composite(tile, (col * cell_width, row * cell_height))
            contact.paste(tile, (col * cell_width, row * (cell_height + 28) + 28), tile)
            labels.text((col * cell_width + 8, row * (cell_height + 28) + 6), f"{state} {col + 1}", fill="#302A32")
        manifest["rows"].append({
            "state": state, "source": filename, "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "sequence": sequence, "shared_crop": crop, "registered_size": size,
        })
    output.parent.mkdir(parents=True, exist_ok=True)
    preview.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(output, optimize=True)
    contact.save(preview)
    preview.with_suffix(".json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"size": atlas.size, "frames": 40, "bytes": output.stat().st_size}))


def clear_speech_fragments(image):
    """Discard only tiny detached remnants of the excluded speech bubble."""
    pixels = image.load()
    visited = set()
    for y in range(min(24, image.height)):
        for x in range(image.width):
            if (x, y) in visited or pixels[x, y][3] == 0:
                continue
            queue = deque([(x, y)])
            component = []
            visited.add((x, y))
            while queue:
                cx, cy = queue.popleft()
                component.append((cx, cy))
                for nx, ny in ((cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)):
                    if (0 <= nx < image.width and 0 <= ny < image.height
                            and (nx, ny) not in visited and pixels[nx, ny][3] != 0):
                        visited.add((nx, ny))
                        queue.append((nx, ny))
            if len(component) < 160 and max(p[1] for p in component) < 24:
                for point in component:
                    pixels[point] = (0, 0, 0, 0)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-dir", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--preview", type=Path, required=True)
    args = parser.parse_args()
    prepare(args.source_dir, args.output, args.preview)
