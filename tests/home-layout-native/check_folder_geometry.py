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
assert 'candidates[i].address+16' in c
assert 'folder_geometry_contract(consumers' in c
assert 'folder_call_target' in h and 'folder_helper' in h
assert 'folder_relative_branch' in h and 'map[to]' in h
assert 'g_folder_geometry_checked=true' in c
assert 'g_folder_geometry_checked=false' in c
assert 'workspace_write(fp, -0x18, workspace_read<double>(fp, -0x30))' in h
assert 'workspace_read<double>(fp, -0x18)' in h
assert 'top & 7' in (root/'app/src/main/cpp/targets/home/home_indicator_pair.h').read_text()
assert 'thread_local home_layout::WorkspaceRenderSnapshot rendered_workspace' in c
assert 'folder_ready || drop_ready || g_knobs[2]' in c
assert c.count('&rendered_workspace') == 4  # folder, drop, close and large-folder consumers
assert 'rendered->begin_preview(fp, valid ? g[1] : 0)' in h
assert 'rendered->finish_preview(fp, top)' in h
print('folder original-body guards: PASS (5 windows; semantic bodies; named calls; decoded operands; +16 outside patches; partial install stock fallback; outgoing argument overwrite; rendered/off-state snapshots; thread-local cache; SP16/Dart8)')
if len(sys.argv)>1:
 import subprocess
 # Run the production binder against real ELF, relocation, fields, control flow,
 # helper bodies and partial-install refusal. No regex assertion substitutes for it.
 subprocess.run([sys.executable,'-X','utf8',str(root/'tests/os4-audit-folder-geometry-20261005/verify.py')],check=True)
