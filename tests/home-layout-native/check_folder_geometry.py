#!/usr/bin/env python3
"""Guard the original-body folder animation path, including partial install fallback."""
from pathlib import Path
import re,sys
root=Path(__file__).resolve().parents[2]
h=(root/'app/src/main/cpp/targets/home/home_folder_geometry.h').read_text()
a=(root/'app/src/main/cpp/targets/home/home_layout_dart_arm64.S').read_text()
c=(root/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text(encoding='utf-8')
assert len(re.findall(r'\{"(?:WidgetPositionUtil.getCellPosition|FolderIconGetxController.calOriginPreviewIconLoc)"',h))==5
stub=a[a.index('.macro folder_geometry_splice'):a.index('/* Both finished animation branches')]
assert 'sub sp, sp, #704' in stub and 'add sp, sp, #704' in stub
assert 'mov x2, sp' in stub and 'bl hc_layout_folder_body' in stub
assert 'hc_layout_folder_resume' in stub and 'hc_layout_folder_original' in stub
assert 'ldar w10, [x9]' in stub and 'cbz w10, 2f' in stub
assert 'candidates[i].address + 16' in c and 'full.size()' in c
assert 'frame[2] != full[2]' in c
assert 'workspace_write(fp, -0x18, workspace_read<double>(fp, -0x30))' in h
assert 'workspace_read<double>(fp, -0x18)' in h
assert 'top & 7' in (root/'app/src/main/cpp/targets/home/home_indicator_pair.h').read_text()
print('folder original-body guards: PASS (5 windows; whole bodies; +16 outside patches; partial install stock fallback; outgoing argument overwrite; SP16/Dart8)')
if len(sys.argv)>1:
 from dart_dump import Elf,Symbols,engine
 e=Elf(sys.argv[1]);s=Symbols(sys.argv[2]);windows=[]
 for name,size,offset,words in re.findall(r'\{"([^"]+)", (0x\w+), (0x\w+),\s*\{([^}]+)\}\}',h):
  va,z=max([(v,z) for v,z,n in s.entries if n==name],key=lambda t:t[1]);assert z==int(size,16)
  off=int(offset,16);expect=b''.join(int(w.strip(),16).to_bytes(4,'little') for w in words.split(','));assert e.read(va+off,16)==expect
  assert all(i.mnemonic not in ['bl','blr','b','ret'] for i in engine().disasm(expect,va+off))
  windows.append((va+off,va+off+16));print(f'fixture {name}+{off:#x}: exact 4 instructions; no relocated Dart call')
 assert all(not any(a<=b<b2 for a,b2 in windows) for _,b in windows)
 for name,label in [('WidgetPositionUtil.getCellPosition','kFolderPositionOriginal'),('FolderIconGetxController.calOriginPreviewIconLoc','kFolderSizeOriginal')]:
  text=h.split(label+'[] = {')[1].split('};')[0];expect=b''.join(int(w,16).to_bytes(4,'little') for w in re.findall('0x[0-9a-f]+',text))
  va,z=max([(v,z) for v,z,n in s.entries if n==name],key=lambda t:t[1]);assert len(expect)==z and e.read(va,z)==expect
 print('launcher 7722 full bodies: PASS (129 + 169 original instructions)')
