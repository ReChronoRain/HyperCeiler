#!/usr/bin/env python3
"""Build the experimental phone-router plan, retaining the sealed v5 families.

This does not write production. All original bytes are independently read from
the pinned ELF. The original generated header and JSONL are both hash pinned.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct

SOURCE = Path(__file__).resolve().parents[2]
BASE = SOURCE.parent
PINNED = 'a37339e922e0dbebda37d0e10aba599bdec449a0bb8d170305b6cb7d5cfec992'
BASE_HEADER = 'fcccfcf5021d84156a092b2bcbd130f40b2babda5d93a67ba427326ea0fa588c'
BASE_RECORDS = '4bed2e9c91645f6e4b97708e1971919a85d407fb0352c27715c602c85263953f'

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def generate(out):
    image = BASE / 'firmware/launcher-native/lib/arm64-v8a/libapp.so'
    baseline = BASE / 'acceptance/home-startup-next/generated-plan-v5/production-plans.jsonl'
    header = SOURCE / 'app/src/main/cpp/targets/home/home_startup_plan.generated.h'
    assert digest(image) == PINNED and digest(baseline) == BASE_RECORDS
    assert digest(header) == BASE_HEADER
    records = [json.loads(line) for line in baseline.read_text().splitlines()]
    raw = image.read_bytes()
    phoff = struct.unpack_from('<Q', raw, 32)[0]
    phentsize, phnum = struct.unpack_from('<HH', raw, 54)
    assert phentsize == 56
    loads = []
    for i in range(phnum):
        typ, flags, off, va, _, fs, ms, _ = struct.unpack_from('<IIQQQQQQ', raw, phoff + i*56)
        if typ == 1:
            loads.append((va, fs, ms, off, flags))
    def read(va, size):
        for start, fs, _, off, flags in loads:
            if start <= va and va + size <= start + fs:
                assert flags & 1
                return raw[off+va-start:off+va-start+size]
        raise ValueError(hex(va))
    def word(va):
        return struct.unpack('<I', read(va, 4))[0]
    def mov(rd, value):
        assert 0 <= value < 65536
        return 0xd2800000 | (value << 5) | rd
    def bl(at, target):
        delta = target - at
        assert not delta % 4 and -(1 << 27) <= delta < (1 << 27)
        return 0x94000000 | ((delta >> 2) & 0x3ffffff)

    # Three typed column producers and five original variable call sites.
    # Row selection and per-frame quantization belong to resident routers.
    calls = {0x1abec30: 0x15228e8, 0x19274f4: 0xa49adc,
             0x1927574: 0xa49adc, 0x1927d3c: 0xa49adc, 0x1927dd8: 0xa49adc}
    assert word(0x1abeab0)==0xd2800102 and word(0x1abead0)==0xd2800142
    assert word(0x1abe9ec)==0xf8407004  # LDUR x4, [x0,#7]; preserve raw x4 result.
    for at in calls:
        original_target = 0x152228c if at==0x1abec30 else 0xa4b458
        assert word(at)==bl(at,original_target)
    for r in records:
        if r['type'] == 'variant' and r['family'] == 0:
            x, y = r['key'] >> 8, r['key'] & 255
            assert 3 <= x <= 9 and 4 <= y <= 13
            values = {0x1abeab0: mov(2, x << 1), 0x1abead0: mov(2, x << 1),
                      0x1abe9ec: mov(4, x)}
            values.update({at: bl(at, target) for at, target in calls.items()})
            r['words'] = [[at, word(at), value] for at, value in sorted(values.items())]
    # Downstream replay/quantizer precedes both entry routers. Closing the gate
    # never removes these hooks from admitted frames.
    for i, va in enumerate((0xa4ab78, 0xa49adc, 0x15228e8), 15):
        records.append(dict(type='hook', index=i, va=va, feature=1))
    bodies = [(0x1abe8f8,624), (0x1abeb94,176), (0x15228e8,640),
              (0x152228c,1244), (0xa49adc,900), (0xa4b458,844),
              (0x192718c,7372), (0xa4418c,560), (0xa4ab58,168),
              # Full dependencies executed by the primary and secondary
              # arithmetic fixtures. Guarding only the splice heads would
              # silently accept a changed implementation in these callees.
              (0x1520f40,2392), (0x1521898,276), (0xa49e60,260),
              (0xa49f64,1568), (0xa4a890,648), (0xa4a698,288),
              (0xa4a7b8,132), (0xa4a83c,84), (0xa4a650,36), (0xa4a584,204)]
    for va, size in bodies:
        records.append(dict(type='guard', va=va, size=size))
    ranges = sorted((r['va'],r['va']+r['size']) for r in records if r['type']=='guard')
    merged = []
    for a,b in ranges:
        if merged and a <= merged[-1][1]:
            merged[-1] = merged[-1][0], max(merged[-1][1], b)
        else:
            merged.append((a,b))
    guards, guard_bytes = [], bytearray()
    for a,b in merged:
        guards.append((a,len(guard_bytes),b-a)); guard_bytes.extend(read(a,b-a))
    hooks = [r for r in records if r['type']=='hook']
    assert [r['index'] for r in hooks] == list(range(18))
    words, variants, wordsets = [], [], {}
    for r in records:
        if r['type'] != 'variant': continue
        key = tuple(tuple(w) for w in r['words'])
        for va, before, after in key:
            assert word(va) == before
        if key not in wordsets:
            wordsets[key] = len(words); words.extend(key)
        variants.append((r['family'],r['key'],wordsets[key],len(key)))
    capacities = [(r['va'],r['before'],r['after']) for r in records if r['type']=='capacity']
    for va, before, _ in capacities: assert word(va)==before
    sites = {w[0] for w in words+capacities}
    assert len(sites) <= 64
    for h in hooks:
        assert not any(h['va'] <= s < h['va']+16 for s in sites)
        assert any(a <= h['va'] and h['va']+16 <= b for a,b in merged)
    for i,h in enumerate(hooks):
        assert all(h['va']+16 <= j['va'] or j['va']+16 <= h['va'] for j in hooks[i+1:])
    for s in sites: assert any(a<=s and s+4<=b for a,b in merged)
    def rows(items,fmt): return '\n'.join('    {'+fmt(x)+'},' for x in items)
    def array(name,b):
        return 'inline constexpr uint8_t '+name+'[] = {\n' + '\n'.join(
            '    '+','.join(f'0x{x:02x}' for x in b[i:i+24])+',' for i in range(0,len(b),24))+'\n};\n'
    text = '''/* SPDX-License-Identifier: AGPL-3.0-or-later */
// EXPERIMENTAL: generated by tests/os4-home-grid-owner/generate_plan.py.
// Not an installed or production-approved recipe.
#pragma once
#include <cstdint>
namespace home_startup::plan {
struct Guard { uint32_t va, offset, size; };
struct Hook { uint32_t index, va; uint64_t feature; };
struct Word { uint32_t va, original, replacement; };
struct Variant { uint32_t family, key, begin, count; };
struct Segment { uint32_t va, file_size, memory_size, flags; };
'''
    text += f'inline constexpr char kImageSha256[] = "{PINNED}";\n'
    text += array('kIdentity',raw[:512]) + array('kGuardBytes',guard_bytes)
    text += 'inline constexpr Segment kSegments[] = {\n'+rows(loads,lambda x:f'0x{x[0]:x},0x{x[1]:x},0x{x[2]:x},{x[4]}')+'\n};\n'
    text += 'inline constexpr Guard kGuards[] = {\n'+rows(guards,lambda x:','.join(hex(v) for v in x))+'\n};\n'
    text += 'inline constexpr Hook kHooks[] = {\n'+rows(hooks,lambda x:f'{x["index"]},0x{x["va"]:x},{x["feature"]}ull')+'\n};\n'
    text += 'inline constexpr Word kWords[] = {\n'+rows(words,lambda x:','.join(hex(v) for v in x))+'\n};\n'
    text += 'inline constexpr Variant kVariants[] = {\n'+rows(variants,lambda x:','.join(str(v) for v in x))+'\n};\n'
    text += 'inline constexpr Word kCapacity[] = {\n'+rows(capacities,lambda x:','.join(hex(v) for v in x))+'\n};\n'
    branches = next(r['values'] for r in records if r['type']=='branches')
    text += 'inline constexpr uint32_t kIndicatorBranches[] = {'+','.join(hex(v) for v in branches)+'};\n'
    text += 'inline constexpr uint32_t kFolderResume[] = {'+','.join(hex(h['va']+16) for h in hooks[9:14])+'};\n'
    text += '} // namespace home_startup::plan\n'
    target = out/'home_startup_plan.generated.h'; target.write_text(text)
    report = dict(status='EXPERIMENTAL_HOST_OWNER_PLAN', device_status='NOT_TESTED',
        image_sha256=PINNED, baseline_records_sha256=BASE_RECORDS, baseline_header_sha256=BASE_HEADER,
        generator_sha256=digest(Path(__file__)), header_sha256=digest(target),
        hooks=18, phone_words=8, unique_sites=len(sites), guard_bytes=len(guard_bytes),
        guard_ranges=len(guards), variants=len(variants), original_bytes_match_elf=True,
        disjoint_code_sites=True, install_order=[hex(h['va']) for h in hooks[15:]],
        new_full_bodies=[{'va':hex(a),'size':n,'sha256':hashlib.sha256(read(a,n)).hexdigest()} for a,n in bodies],
        limits=['Host data/ownership plan only; no real inline backend or Dart runtime executed.',
                'Must bind reviewed resident primary/secondary/quantizer implementations before promotion.'])
    (out/'plan-report.json').write_text(json.dumps(report,indent=2)+'\n')
    return report

if __name__=='__main__':
    ap=argparse.ArgumentParser(description=__doc__); ap.add_argument('--output',type=Path,required=True)
    args=ap.parse_args(); args.output.mkdir(parents=True,exist_ok=False)
    print(json.dumps(generate(args.output)))
