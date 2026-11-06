"""Register image_gen motion strips without redrawing or individually stretching poses."""
import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image, ImageDraw


ACTIONS = ["wink", "ears_up", "head_tilt", "nod", "shy", "cheer", "stretch", "peekaboo"]


def prepare(source, output, review, reference):
    with Image.open(reference) as opened:
        idle = opened.convert("RGBA").crop((0, 0, 192, 208))
    reference_bounds = idle.getchannel("A").point(lambda value: 255 if value >= 100 else 0).getbbox()
    assert reference_bounds, "Missing original idle reference"
    target_width = reference_bounds[2] - reference_bounds[0]
    target_height = reference_bounds[3] - reference_bounds[1]
    target_center = (reference_bounds[0] + reference_bounds[2]) / 2 + 32
    target_baseline = reference_bounds[3] + 48
    atlas = Image.new("RGBA", (2048, 2048))
    contact = Image.new("RGB", (2048, 8 * 284), "#FFF4E8")
    manifest = {
        "cell": [256, 256], "actions": [],
        "reference_sha256": hashlib.sha256(reference.read_bytes()).hexdigest(),
        "reference_visible_size": [target_width, target_height],
        "reference_center_x": target_center, "reference_baseline": target_baseline,
        "measurement_alpha_cutoff": 100,
    }
    rows = []
    for row, name in enumerate(ACTIONS):
        path = source / f"{name}.png"
        with Image.open(path) as opened:
            sheet = opened.convert("RGBA")
        assert sheet.getchannel("A").getextrema() == (0, 255), f"Missing transparency: {name}"
        frames = [sheet.crop((round(i * sheet.width / 8), 0, round((i + 1) * sheet.width / 8), sheet.height)) for i in range(8)]
        bounds = [frame.getchannel("A").point(lambda value: 255 if value >= 100 else 0).getbbox() for frame in frames]
        assert all(bounds), f"Missing pose: {name}"
        crop = (min(b[0] for b in bounds), min(b[1] for b in bounds), max(b[2] for b in bounds), max(b[3] for b in bounds))
        neutral_width = (bounds[0][2] - bounds[0][0] + bounds[7][2] - bounds[7][0]) / 2
        neutral_height = max(bounds[i][3] - bounds[i][1] for i in (0, 7))
        neutral_center = (bounds[0][0] + bounds[0][2] + bounds[7][0] + bounds[7][2]) / 4
        baseline = max(bounds[0][3], bounds[7][3])
        # Match the actual old idle silhouette, not the padded tile or a nominal
        # width. A width-only fit made the taller new character ~20% too large.
        scale = min(target_width / neutral_width, target_height / neutral_height,
                    240 / (crop[2] - crop[0]), (target_baseline - 8) / (baseline - crop[1]))
        size = (round((crop[2] - crop[0]) * scale), round((crop[3] - crop[1]) * scale))
        top = target_baseline - round((baseline - crop[1]) * scale)
        left = round(target_center - (neutral_center - crop[0]) * scale)
        tiles = []
        for column, frame in enumerate(frames):
            # Identical crop, scale and placement across a row retains the hop and nod.
            registered = frame.crop(crop).resize(size, Image.Resampling.NEAREST)
            tile = Image.new("RGBA", (256, 256))
            tile.alpha_composite(registered, (left, top))
            bbox = tile.getbbox()
            assert bbox and bbox[0] > 0 and bbox[1] > 0 and bbox[2] < 256 and bbox[3] < 256, f"Clipped pose: {name} {column}"
            atlas.alpha_composite(tile, (column * 256, row * 256))
            contact.paste(tile, (column * 256, row * 284 + 28), tile)
            ImageDraw.Draw(contact).text((column * 256 + 8, row * 284 + 8), f"{name} {column + 1}", fill="#302A32")
            tiles.append(tile)
        for index in (0, 7):
            neutral = tiles[index].getchannel("A").point(lambda value: 255 if value >= 100 else 0).getbbox()
            assert neutral[2] - neutral[0] <= target_width + 2, f"Neutral too wide: {name}"
            assert abs(neutral[3] - neutral[1] - target_height) <= 2, f"Neutral size mismatch: {name}"
            assert abs(neutral[3] - target_baseline) <= 1, f"Neutral baseline mismatch: {name}"
        hashes = [hashlib.sha256(tile.tobytes()).hexdigest() for tile in tiles]
        assert len(set(hashes)) >= 5, f"Insufficient temporal drawings: {name}"
        if name == "cheer":
            assert tiles[3].getbbox()[3] < tiles[0].getbbox()[3] - 8, "Cheer must visibly leave baseline"
        rows.append(tiles)
        manifest["actions"].append({
            "name": name, "source_sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "source_size": sheet.size, "shared_crop": crop, "scale": scale,
            "registered_size": size, "left": left, "top": top, "unique_frames": len(set(hashes)),
            "frame_bounds": [tile.getbbox() for tile in tiles],
        })
    output.parent.mkdir(parents=True, exist_ok=True)
    review.mkdir(parents=True, exist_ok=True)
    atlas.save(output, optimize=True)
    contact.save(review / "contact.png")
    comparison = Image.new("RGB", (9 * 128, 156), "#FFF4E8")
    old_tile = Image.new("RGBA", (256, 256))
    old_tile.alpha_composite(idle, (32, 48))
    for index, (name, tile) in enumerate([("original idle", old_tile)] + [(name, row[0]) for name, row in zip(ACTIONS, rows)]):
        small = tile.resize((128, 128), Image.Resampling.NEAREST)
        comparison.paste(small, (index * 128, 24), small)
        ImageDraw.Draw(comparison).text((index * 128 + 4, 4), name, fill="#302A32")
    comparison.save(review / "size-comparison.png")
    motion = []
    for frame in range(8):
        canvas = Image.new("RGB", (512, 2 * 156), "#FFF4E8")
        draw = ImageDraw.Draw(canvas)
        for index, row in enumerate(rows):
            x, y = index % 4 * 128, index // 4 * 156
            draw.text((x + 4, y + 4), ACTIONS[index], fill="#302A32")
            tile = row[frame].resize((128, 128), Image.Resampling.NEAREST)
            canvas.paste(tile, (x, y + 24), tile)
        motion.append(canvas)
    motion[0].save(review / "preview.gif", save_all=True, append_images=motion[1:], duration=140, loop=0)
    manifest["atlas_sha256"] = hashlib.sha256(output.read_bytes()).hexdigest()
    manifest["frames"] = 64
    (review / "registration.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    print(json.dumps({"frames": 64, "size": atlas.size, "bytes": output.stat().st_size, "unique": [r["unique_frames"] for r in manifest["actions"]]}))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--review", type=Path, required=True)
    parser.add_argument("--reference", type=Path, default=Path("core/designsystem/src/main/res/drawable-nodpi/pie_companion.png"))
    args = parser.parse_args()
    prepare(args.source, args.output, args.review, args.reference)
