"""Preview existing final atlas cells only; no art synthesis or geometry changes."""
import json
import subprocess
from pathlib import Path
from PIL import Image, ImageDraw

RUN = Path(__file__).resolve().parents[1]
OUT = RUN / 'qa' / 'previews-final'
OUT.mkdir(exist_ok=True)
atlas = Image.open(RUN / 'final/spritesheet-extended.png').convert('RGBA')
states = [('idle', 6), ('running-right', 8), ('running-left', 8), ('waving', 4), ('jumping', 5), ('failed', 8), ('waiting', 6), ('running', 6), ('review', 6)]

def cell(row, col):
    return atlas.crop((col*192, row*208, (col+1)*192, (row+1)*208))

def labelled(sprite, label):
    im = Image.new('RGB', (384, 456), '#FFF4E8')
    im.paste(sprite.resize((384, 416), Image.Resampling.NEAREST), (0, 40), sprite.resize((384, 416), Image.Resampling.NEAREST))
    ImageDraw.Draw(im).text((12, 12), label, fill='#302A32')
    return im

def gif(frames, path, duration=160):
    frames[0].save(path, save_all=True, append_images=frames[1:], loop=0, duration=duration, disposal=2)

all_frames = []
for row, (name, count) in enumerate(states):
    folder = RUN / 'qa/final-frames' / name
    folder.mkdir(parents=True, exist_ok=True)
    for col in range(count):
        sprite = cell(row, col)
        sprite.save(folder / f'{col:02}.png')
        all_frames.append(labelled(sprite, f'{name} / frame {col+1} / {len(all_frames)*160} ms'))
gif(all_frames, OUT / 'all-states.gif')
mp4_frames = OUT / 'mp4-frames'
mp4_frames.mkdir(exist_ok=True)
for i, frame in enumerate(all_frames):
    frame.save(mp4_frames / f'{i:03}.png')
subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-framerate', '6.25', '-i', str(mp4_frames/'%03d.png'), '-c:v', 'libx264', '-pix_fmt', 'yuv420p', str(OUT/'all-states.mp4')], check=True)
sequence = [(0,c) for c in range(6)] + [(4,c) for c in range(5)] + [(0,c) for c in range(6)]
jump_frames = [labelled(cell(r,c), f'idle-jump-idle / {i*160} ms') for i,(r,c) in enumerate(sequence)]
gif(jump_frames, OUT / 'idle-jump-idle.gif')
stills = OUT / 'jump-stills'
stills.mkdir(exist_ok=True)
for i in (0,6,8,10,16):
    jump_frames[i].save(stills/f'{i*160:04}-ms.png')
look = [labelled(cell(9+i//8,i%8), f'look direction {i*22.5:05.1f} degrees') for i in range(16)]
gif(look, OUT / 'look-loop.gif', 240)
print(json.dumps({'output':str(OUT),'all_state_frames':len(all_frames),'jump_stills':5}))
