"""Extract the generated PIE cutouts and register eight screen-edge poses."""
import argparse
import json
from pathlib import Path
from PIL import Image, ImageDraw


def prepare(source, output, preview, top_repair=None):
    image = Image.open(source).convert("RGBA")
    alpha = image.getchannel("A")
    width, height = image.size

    def gutters(axis):
        length = width if axis == 0 else height
        cuts = [0]
        for fraction in (1 / 3, 2 / 3):
            center = round(length * fraction)
            empty = []
            for index in range(center - length // 12, center + length // 12):
                strip = alpha.crop((index, 0, index + 1, height)) if axis == 0 else alpha.crop((0, index, width, index + 1))
                if strip.getextrema()[1] < 32:
                    empty.append(index)
            runs = []
            for index in empty:
                if not runs or index > runs[-1][-1] + 1:
                    runs.append([index])
                else:
                    runs[-1].append(index)
            if not runs:
                raise ValueError("Missing transparent gutter")
            run = max(runs, key=len)
            cuts.append((run[0] + run[-1]) // 2)
        return cuts + [length]

    xs, ys = gutters(0), gutters(1)
    poses = [("TopLeft", 0, 0, -1, -1), ("Top", 0, 1, 0, -1),
             ("TopRight", 0, 2, 1, -1), ("Left", 1, 0, -1, 0),
             ("Right", 1, 2, 1, 0), ("BottomLeft", 2, 0, -1, 1),
             ("Bottom", 2, 1, 0, 1), ("BottomRight", 2, 2, 1, 1)]
    atlas = Image.new("RGBA", (256 * 8, 256))
    proof = Image.new("RGB", (256 * 4, 284 * 2), "#FFF4E8")
    draw = ImageDraw.Draw(proof)
    records = []
    for column, (name, row, col, horizontal, vertical) in enumerate(poses):
        sprite = image.crop((xs[col], ys[row], xs[col + 1], ys[row + 1]))
        sprite.putdata([(r, g, b, a) if a >= 128 else (0, 0, 0, 0) for r, g, b, a in sprite.getdata()])
        bounds = sprite.getchannel("A").getbbox()
        if not bounds:
            raise ValueError(name + " is empty")
        sprite = sprite.crop(bounds)
        # One scale across the family; preserve tilt/proportions, never fill each cell.
        sprite = sprite.resize((round(sprite.width * .46), round(sprite.height * .46)), Image.Resampling.NEAREST)
        if name == "Top" and top_repair:
            repair = Image.open(top_repair).convert("RGBA")
            repair.putdata([(r, g, b, a) if a >= 128 else (0, 0, 0, 0) for r, g, b, a in repair.getdata()])
            repair = repair.crop(repair.getchannel("A").getbbox())
            # Match the already accepted family's footprint, preserving aspect ratio.
            repair_scale = min(sprite.width / repair.width, sprite.height / repair.height)
            sprite = repair.resize((round(repair.width * repair_scale), round(repair.height * repair_scale)), Image.Resampling.NEAREST)
        x = 0 if horizontal < 0 else 256 - sprite.width if horizontal > 0 else (256 - sprite.width) // 2
        y = 0 if vertical < 0 else 256 - sprite.height if vertical > 0 else (256 - sprite.height) // 2
        if x < 0 or y < 0:
            raise ValueError(name + " does not fit")
        frame = Image.new("RGBA", (256, 256))
        frame.alpha_composite(sprite, (x, y))
        atlas.alpha_composite(frame, (column * 256, 0))
        px, py = column % 4 * 256, column // 4 * 284
        proof.paste(frame, (px, py), frame)
        draw.text((px + 8, py + 260), name, fill="#302A32")
        records.append({"edge": name, "column": column, "bounds": frame.getchannel("A").getbbox(), "shared_scale": .46})
    output.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(output)
    proof.save(preview)
    preview.with_suffix(".json").write_text(json.dumps({"source": str(source), "top_clip_repair": str(top_repair) if top_repair else None, "gutters": [xs, ys], "poses": records}, indent=2), encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("preview", type=Path)
    parser.add_argument("--top-repair", type=Path)
    args = parser.parse_args()
    prepare(args.source, args.output, args.preview, args.top_repair)
