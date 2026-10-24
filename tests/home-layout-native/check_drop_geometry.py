#!/usr/bin/env python3
"""Pin the original drop-back center calculation to the known launcher image."""
from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[2]
header = root / 'app/src/main/cpp/targets/home/home_drop_geometry.h'
assert header.exists(), 'drop-back geometry path missing'
h = header.read_text()
a = (root / 'app/src/main/cpp/targets/home/home_layout_dart_arm64.S').read_text()
c = (root / 'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text()
s = (root / 'app/src/main/cpp/targets/home/tweaks/symtab.cpp').read_text()
assert 'CellLayoutGetxController.calculateCenterGlobalPosition' in s
assert 'drop_ready ||' in c and 'bind_drop_geometry();' in c
assert 'hc_layout_drop_enabled' in a and 'hc_layout_drop_body' in a
assert 'hc_layout_drop_resume' in a and 'hc_layout_drop_original' in a
assert a.count('drop_geometry_splice hc_layout_drop_') == 2
assert 'rendered->begin_drop(fp, g[1])' in h
assert 'rendered->finish_drop(fp, 0)' in h
assert 'top & 7' in (root / 'app/src/main/cpp/targets/home/home_indicator_pair.h').read_text()
print('drop-back original-body guards: PASS (two no-call windows; atomic bank; rendered frame; Dart8/nativeSP16)')

if len(sys.argv) > 1:
    from dart_dump import Elf, Symbols, engine
    e, syms = Elf(sys.argv[1]), Symbols(sys.argv[2])
    name = 'CellLayoutGetxController.calculateCenterGlobalPosition'
    va, size = max(((v, z) for v, z, n in syms.entries if n == name), key=lambda x: x[1])
    assert size == 0x5c0
    data = e.read(va, size)
    digest = 0xcbf29ce484222325
    for byte in data:
        digest = ((digest ^ byte) * 0x100000001b3) & 0xffffffffffffffff
    assert digest == int(re.search(r'kDropGeometryHash = (0x[0-9a-f]+)', h).group(1), 16)
    sites = re.findall(r'\{(0x[0-9a-f]+), \{(0x[0-9a-f, ]+)\}\}', h)
    assert len(sites) == 2
    for offset, words in sites:
        off = int(offset, 16)
        expected = b''.join(int(w.strip(), 16).to_bytes(4, 'little') for w in words.split(','))
        assert e.read(va + off, 16) == expected
        assert all(i.mnemonic not in ('bl', 'blr', 'b', 'ret')
                   for i in engine().disasm(expected, va + off))
    print('launcher 7722 drop-back body: PASS (0x5c0 bytes; full hash; +0x120/+0x1b8 exact windows)')
