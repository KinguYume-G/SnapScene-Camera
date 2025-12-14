import os
import struct
import imghdr

def get_image_size(fname):
    with open(fname, 'rb') as fhead:
        if imghdr.what(fname) == 'png':
            fhead.seek(16)
            return struct.unpack('>II', fhead.read(8))
        elif imghdr.what(fname) == 'jpeg':
           # minimal jpeg verification
           fhead.seek(0)
           size = 2
           ftype = 0
           while not 0xc0 <= ftype <= 0xcf:
               fhead.seek(size, 1)
               byte = fhead.read(1)
               while ord(byte) == 0xff:
                   byte = fhead.read(1)
               ftype = ord(byte)
               size = struct.unpack('>H', fhead.read(2))[0] - 2
           # We are at a SOFn block
           fhead.seek(1, 1)  # precision
           h, w = struct.unpack('>HH', fhead.read(4))
           return w, h
    return None, None

drawable_dir = r'd:\Androidapp\app\src\main\res\drawable'
bg_files = [f for f in os.listdir(drawable_dir) if f.startswith('bg_') and (f.endswith('.png') or f.endswith('.jpg'))]

print(f"{'File':<40} {'Width':<10} {'Height':<10} {'Ratio':<10} {'Status'}")
print("-" * 80)

for f in bg_files:
    try:
        path = os.path.join(drawable_dir, f)
        w, h = get_image_size(path)
        if w and h:
            ratio = w / h
            target_ratio = 9/16  # 0.5625
            is_portrait = h > w
            is_fullscreen = abs(ratio - target_ratio) < 0.1
            status = "OK" if is_portrait and is_fullscreen else "BAD (Not 9:16)"
            if not is_portrait: status = "BAD (Landscape)"
            
            print(f"{f:<40} {w:<10} {h:<10} {ratio:.2f:<10} {status}")
    except Exception as e:
        print(f"{f:<40} ERROR: {e}")
