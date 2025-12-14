import os
import struct

def get_image_size(fname):
    with open(fname, 'rb') as fhead:
        data = fhead.read(24)
        if data.startswith(b'\x89PNG\r\n\x1a\n'):
            # PNG: width/height are at offset 16 and 20 (4 bytes each, big endian)
            w, h = struct.unpack('>II', data[16:24])
            return w, h
        elif data.startswith(b'\xff\xd8'):
            # JPEG: Scan for SOF0 marker (0xC0)
            fhead.seek(0)
            fhead.read(2) # skip SOI
            while True:
                byte = fhead.read(1)
                if not byte: break
                while byte != b'\xff':
                    byte = fhead.read(1)
                    if not byte: return None, None
                
                marker = fhead.read(1)
                if not marker: break
                marker_code = ord(marker)
                
                # Markers to skip
                if 0xd0 <= marker_code <= 0xd9: continue # RST markers
                if marker_code == 0x00: continue
                
                length = struct.unpack('>H', fhead.read(2))[0]
                
                if marker_code == 0xc0 or marker_code == 0xc2: # SOF0 or SOF2
                    # precision (1), height (2), width (2)
                    fhead.read(1)
                    h, w = struct.unpack('>HH', fhead.read(4))
                    return w, h
                
                fhead.seek(length - 2, 1) # skip segment body
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
            
            status = "OK"
            if not is_portrait: status = "BAD (Landscape)"
            elif not is_fullscreen: status = f"BAD (Ratio {ratio:.2f})"
            
            print(f"{f:<40} {w:<10} {h:<10} {ratio:<10.2f} {status}")
    except Exception as e:
        print(f"{f:<40} ERROR: {e}")
