"""Real OS4 AOT capacity/scalar contracts; no device or Dart GC emulation."""
from pathlib import Path
import sys, struct, subprocess, json
W = Path(__file__).resolve().parents[2]
R = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else W
T = W / 'handover/folder-icons-hotseat-20261018'
O = T / ('test-' + R.name)
O.mkdir(parents=True, exist_ok=True)
H = R / 'app/src/main/cpp/targets/home/home_hotseat_capacity.h'
if 'inline bool capacity_site(' not in H.read_text(encoding='utf8'):
    print('DYNAMIC_CAPACITY_CONSUMER_MISSING')
    raise SystemExit(1)
sys.path.insert(0, str(W / 'tests/home-layout-native'))
from dart_dump import Elf, Symbols
fixtures = [
    ('7751', W / 'handover/launcher-7751-20261017/launcher-7751-libapp.so'),
    ('7722', Path('C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f/launcher-7722-libapp.so')),
    ('8244', W / 'handover/folder-title-os4-20261012/launcher-8244-libapp.so')]
names = ['calculatePositionX', '_isSeatsFull', '_isSeatsFull', None,
         'setHotSeatItems', 'resetAnimationAndUpdateDatabase', '_updateDragItemCount', '_pushDragItem']
code = '''#include "targets/home/home_hotseat_capacity.h"
#include <iostream>
using namespace home_layout;
int main(){int checks=0,failed=0;auto ok=[&](bool v){++checks;if(!v){++failed;std::cout<<"FAIL="<<checks<<"\\n";}};
'''
for version, f in fixtures:
    if not f.exists():
        f = next((W / 'handover').rglob('launcher-' + version + '-libapp.so'))
    e = Elf(f)
    s = Symbols(f.with_name('launcher-' + version + '-mini.elf'))
    max_count = s.resolve('DeviceConfig.hotSeatMaxCount')[0]
    fixed_count = s.resolve('HotSeatsConstants2.hotseatMaxFixedCount')[0]
    for kind, name in enumerate(names):
        name = 'HotseatDragHandler._isSeatsFull' if name is None else 'HotseatLayerGetxController.' + name
        entries = [(a, z) for a, z, n in s.entries if n == name]
        if kind == 0:
            entries = [(a, z) for a, z in entries if b'\x01\xf8\x7f\xd3\x30\x7c\x41\x93' in e.read(a, z)]
        assert len(entries) == 1, (version, name, entries)
        va, size = entries[0]
        b = struct.unpack('<' + 'I' * (size // 4), e.read(va, size))
        code += '{home_dynamic::Body b={' + ','.join(hex(w) for w in b) + '};CapacityResolved r;'
        expr = f'capacity_site(b,{va},{kind},{max_count},{fixed_count},r)'
        code += f'ok({expr});std::cout<<"SITE_{version}_{kind}="<<r.offset<<"/"<<std::hex<<r.original<<"/"<<r.replacement<<std::dec<<"\\n";'
        code += 'auto orig=b;auto good=r;ok(r.offset+4<=b.size()*4&&r.original!=r.replacement);'
        code += f'b[good.offset/4]=0xd503201f;ok(!{expr});b=orig;'
        code += f'ok(!capacity_site(b,{va},{kind},{max_count+4},{fixed_count+4},r));'
        code += f'auto clone=orig;for(auto &w:clone)if((w&0xfc000000u)==0x94000000u)w=(w&0xfc000000u)|((w-uint32_t(orig.size()))&0x03ffffffu);b.insert(b.end(),clone.begin(),clone.end());ok(!{expr});b=orig;'
        if kind >= 4:
            code += 'int d=int32_t(good.original<<8)>>13;int p=int32_t(good.replacement<<6)>>6;ok(d==p);'
            # Relocate the original branch: the replacement must follow, not reuse a frozen offset.
            code += f'b[good.offset/4]+=32;ok({expr});ok(r.replacement==good.replacement+1);b=orig;'
        if kind == 0:
            code += f'ok(capacity_position_shape(b));b[good.offset/4+3]=0xd503201f;ok(!capacity_position_shape(b));'
        code += '}\n'
code += 'std::cout<<"DYNAMIC_CAPACITY checks="<<checks<<" failed="<<failed<<"\\n";return failed?1:0;}\n'
(O / 'test.cpp').write_text(code, encoding='utf8')
zig = W / 'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe'
rows = []
for c in [[zig, 'c++', '-std=c++20', '-I' + str(R / 'app/src/main/cpp'), O / 'test.cpp', '-o', O / 'test.exe'], [O / 'test.exe']]:
    p = subprocess.run(list(map(str, c)), capture_output=True, text=True, encoding='utf8', errors='replace')
    rows.append(dict(command=list(map(str, c)), input='7751/7722/8244 original AOT words and negative mutations', stdout=p.stdout, stderr=p.stderr, exit=p.returncode))
    print(p.stdout + p.stderr, end='')
    if p.returncode:
        break
(O / 'results.json').write_text(json.dumps(rows, indent=2), encoding='utf8')
raise SystemExit(rows[-1]['exit'])
