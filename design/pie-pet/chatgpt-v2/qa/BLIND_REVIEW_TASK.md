Review only `D:/Up-Skill/PIE/design/pie-pet/chatgpt-v2/qa/look-directions-blind.png` (384×3304; inspect at original resolution or crop into sections). Do not read any other project image, manifest, answer key, labeled direction sheet, or another reviewer's verdict. This is independent visual classification, not an intended-direction lookup.

For every Horizontal pair 1–7, independently classify A and B as `screen-left`, `screen-right`, or `ambiguous`. Use the character's head, muzzle projection, face plane and pupils. Screen means the viewer's image coordinates. For every Vertical pair 1–7, independently classify A and B as `up`, `down`, or `ambiguous`. Consider chin/head pitch and eye direction. Do not infer that A and B must oppose; if either is unclear mark it ambiguous. Include one short concrete visual reason per pair.

Write only your own result to the path assigned by the parent. Exact JSON shape:

```json
{
  "pairs": [
    {"pair": "horizontal-1", "A": "ambiguous", "B": "ambiguous", "reason": "Replace with actual visual evidence."},
    {"pair": "horizontal-2", "A": "ambiguous", "B": "ambiguous", "reason": "..."},
    {"pair": "horizontal-3", "A": "ambiguous", "B": "ambiguous", "reason": "..."},
    {"pair": "horizontal-4", "A": "ambiguous", "B": "ambiguous", "reason": "..."},
    {"pair": "horizontal-5", "A": "ambiguous", "B": "ambiguous", "reason": "..."},
    {"pair": "horizontal-6", "A": "ambiguous", "B": "ambiguous", "reason": "..."},
    {"pair": "horizontal-7", "A": "ambiguous", "B": "ambiguous", "reason": "..."},
    {"pair": "vertical-1", "A": "ambiguous", "B": "ambiguous", "reason": "..."},
    {"pair": "vertical-2", "A": "ambiguous", "B": "ambiguous", "reason": "..."},
    {"pair": "vertical-3", "A": "ambiguous", "B": "ambiguous", "reason": "..."},
    {"pair": "vertical-4", "A": "ambiguous", "B": "ambiguous", "reason": "..."},
    {"pair": "vertical-5", "A": "ambiguous", "B": "ambiguous", "reason": "..."},
    {"pair": "vertical-6", "A": "ambiguous", "B": "ambiguous", "reason": "..."},
    {"pair": "vertical-7", "A": "ambiguous", "B": "ambiguous", "reason": "..."}
  ]
}
```

All classifications above are placeholders, not observed verdicts. Replace each from actual image inspection. Do not access the hidden answer key.
