#!/usr/bin/env python3
"""Generate exact Weather card/Flutter guards; only the supplied pinned ROM is accepted."""
from pathlib import Path
import argparse
import bisect
import contextlib
import hashlib
import importlib.util
import io
import json
import lzma
import struct
import subprocess
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


sys.dont_write_bytecode = True
ROOT = test_paths.TEST_DATA_ROOT
HEADER = test_paths.SOURCE_ROOT / 'app/src/main/cpp/targets/weather/weather_card_guard.generated.h'
V4 = test_paths.TEST_DATA_ROOT / 'acceptance/weather-card-next/guard-v4'
SEMANTICS = test_paths.TEST_DATA_ROOT / 'acceptance/weather-card-next/evidence-v1/report.json'
PARSER = test_paths.TEST_DATA_ROOT / 'acceptance/weather-port-next/dart-consumer/snapshot_probe.py'
ELF_READER = test_paths.SOURCE_ROOT / 'tests/weather-binding/generate.py'
SHA = lambda b: hashlib.sha256(b).hexdigest()


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def generate():
    v4 = load('card_extent_reader', V4 / 'extract.py')
    reader = load('card_relocation_reader', ELF_READER)
    sp = load('card_snapshot_parser', PARSER)
    semantics = json.loads(SEMANTICS.read_text())
    prior = json.loads((V4 / 'evidence-final/report.json').read_text())
    assert prior['passed'] and prior['range_count'] == 11
    paths = {'Dart': test_paths.TEST_DATA_ROOT / 'acceptance/native-evidence/weather-libapp.so',
             'Flutter': test_paths.ROM_ROOT / 'components/system_ext/lib64/libhyper_os_flutter.so'}
    hashes = {'Dart': v4.DART_SHA, 'Flutter': v4.FLUTTER_SHA}
    elves = {name: reader.Elf(path, hashes[name]) for name, path in paths.items()}
    dart = v4.Elf(elves['Dart'].data, hashes['Dart'])
    flutter = v4.Elf(elves['Flutter'].data, hashes['Flutter'])
    tables = {name: v4.instructions_table(dart, name) for name in ('Vm', 'Isolate')}
    debug = lzma.decompress(flutter.section(flutter.named['.gnu_debugdata']))
    assert SHA(debug) == v4.DEBUG_SHA
    debug_symbols = v4.Elf(debug, v4.DEBUG_SHA).function_symbols()
    # Parse the complete actual serialized object stream, not just copied report labels.
    with contextlib.redirect_stdout(io.StringIO()):
        allocated = sp.alloc(elves['Dart'].data, 0x4ac0)
        objects, spans = sp.first_fills(elves['Dart'].data, allocated)
        roots = sp.roots(elves['Dart'].data, allocated, spans)
    assert roots['end'] == int(tables['Isolate']['serialized_end_va'], 16)
    cls = objects[91186]
    assert cls['name'] == 'Class' and cls['classid'] == 254 and cls['fields'][:2] == [48, 47]
    assert cls['bitmap'] & (3 << 35) == 3 << 35
    assert [hex(cls['at']), hex(cls['end'])] == semantics['class']['serialized_span']
    regions = {name: [] for name in elves}

    def add(image, name, va, size, expected=None, code=False):
        data = elves[image].read(va, size)
        assert expected is None or SHA(data) == expected, (image, name, 'evidence hash')
        if code:
            (dart if image == 'Dart' else flutter).mapping(va, size, True)
        assert not any(va <= r['slot'] < va + size for r in elves[image].relocations()), (name, 'relocation inside exact guard')
        assert all(va + size <= r['va'] or r['va'] + r['size'] <= va for r in regions[image]), (name, 'overlapping guard')
        regions[image].append(dict(name=name, va=va, size=size, sha256=SHA(data), bytes=data,
                                   kind='complete-code-extent' if code else 'immutable-data'))

    for name, elf in elves.items():
        add(name, 'elf-and-program-headers', 0, elf.phend)
        for i, p in enumerate(p for p in elf.ph if p[0] == 4):
            add(name, 'elf-note-' + str(i), p[3], p[5])
    for prefix, table in tables.items():
        va = int(table['data_export_va'], 16)
        raw = dart.read(va, 256)
        end = raw.index(0, 52) + 1
        for _ in range(5):
            _, end = v4.read_varint(raw, end)
        add('Dart', prefix + '-snapshot-header-and-five-varints', va, end)
        add('Dart', prefix + '-InstructionsTable-header', int(table['table_payload_va'], 16), 16)
    add('Dart', 'serialized-Class254-record', cls['at'], cls['end'] - cls['at'])
    class_cluster = next(c for c in allocated['clusters'] if c['name'] == 'Class')
    add('Dart', 'serialized-Class-cluster-allocation-layout', class_cluster['at'], class_cluster['end'] - class_cluster['at'])
    entry_indices = {'Vm': {3, 4}, 'Isolate': set()}
    functions = []
    for start_hex, evidence in semantics['functions'].items():
        start = int(start_hex, 16)
        starts = tables['Isolate']['starts']
        index = bisect.bisect_left(starts, start)
        assert starts[index] == start
        end = starts[index + 1]
        assert end == int(evidence['end'], 16) and end - start == evidence['size']
        add('Dart', 'card-function-' + start_hex, start, end - start, evidence['sha256'], True)
        entry_indices['Isolate'].update((index, index + 1))
        functions.append(dict(va=start, end=end, size=end-start, table_index=index))
    vm = next(r for r in prior['ranges'] if r['name'] == 'vm-instructions-table-entry-3')
    assert tables['Vm']['starts'][3:5] == [int(vm['va'], 16), int(vm['end_va'], 16)]
    add('Dart', 'complete-VM-InstructionsTable-entry-3', int(vm['va'], 16), vm['size'], vm['sha256'], True)
    for prefix, indices in entry_indices.items():
        for index in sorted(indices):
            add('Dart', prefix + '-InstructionsTable-entry-' + str(index),
                int(tables[prefix]['table_payload_va'], 16) + 16 + index * 8, 8)
    for r in prior['ranges']:
        if r['image'] != 'flutter':
            continue
        va = int(r['va'], 16)
        for name in r['boundary_evidence']['elf_STT_FUNC_names']:
            assert (name, va, r['size']) in debug_symbols
        add('Flutter', r['name'], va, r['size'], r['sha256'], True)
    sites = [0x3a8408, 0x3aece0]
    epilogue = struct.pack('<4I', 0xaa1603e0, 0xaa1d03ef, 0xa8c179fd, 0xd65f03c0)
    for site in sites:
        assert dart.read(site, 16, True) == epilogue
        assert sum(r['va'] <= site and site + 16 <= r['va'] + r['size'] for r in regions['Dart']) == 1
    # Assert the frame/field/consumer anchors again against the source ROM.
    anchors = {0x3a7aec: [0xaa0103e3, 0xf81f03a1], 0x3aeb98: [0xd2800020],
               0x3aebec: [0xf808b080], 0x5bd2bc: [0xf848b001, 0xf1000c3f],
               0x903320: [0xd29d8382, 0xf2a001e2], 0x2d01e0: [0xf9433f50, 0xd140061f]}
    for va, words in anchors.items():
        assert dart.read(va, len(words) * 4, True) == struct.pack('<' + 'I' * len(words), *words)
    constants = {'kClassId': 254, 'kInstanceBytes': 192, 'kModeTaggedOffset': 0x8b,
                 'kLiveStackLimitOffset': 0x48, 'kSavedStackLimitOffset': 0x678,
                 'kGatewayScratchBytes': 96, 'kOriginalEpilogueWord': 0xaa1603e0,
                 'kDartVmDataVa': 0x340, 'kDartIsolateDataVa': 0x4ac0}
    text = ['/* SPDX-License-Identifier: AGPL-3.0-or-later */',
            '// Generated by tests/weather-card-binding/generate.py; exact inspected ROM only.',
            '#pragma once', '#include <array>', '#include <cstdint>', '#include <span>',
            'namespace weather_card_guard {',
            'struct GuardBytes {uintptr_t va; std::span<const uint8_t> bytes;};']
    for name, value in constants.items():
        text.append(f'inline constexpr uintptr_t {name}=0x{value:x};')
    text.append('inline constexpr std::array<uintptr_t,2> kEpilogueVa={0x3a8408,0x3aece0}; // recompute, constructor')
    for image, elf in elves.items():
        text += [f'inline constexpr uintptr_t k{image}ImageSpan=0x{elf.span:x};',
                 f'inline constexpr char k{image}SourceSha256[]="{SHA(elf.data)}";']
        for i, r in enumerate(regions[image]):
            text += [f'// {r["name"]}; {r["kind"]}; RVA0x{r["va"]:x}; {r["size"]} bytes; SHA256 {r["sha256"]}',
                     f'inline constexpr std::array<uint8_t,{r["size"]}> k{image}Bytes{i}={{']
            for j in range(0, r['size'], 16):
                text.append('    ' + ','.join(f'0x{x:02x}' for x in r['bytes'][j:j+16]) + ',')
            text.append('};')
        text.append(f'inline constexpr std::array<GuardBytes,{len(regions[image])}> k{image}Guards={{{{')
        text += [f'    {{0x{r["va"]:x},k{image}Bytes{i}}},' for i, r in enumerate(regions[image])]
        text.append('}};')
    text += ['} // namespace weather_card_guard', '']
    evidence = [V4 / 'extract.py', V4 / 'evidence-final/report.json', SEMANTICS, PARSER, ELF_READER,
                PARSER.parent / 'official-dart-3.10.1/runtime_vm_class_id.h',
                PARSER.parent / 'official-dart-3.10.1/runtime_vm_object_store.h']
    manifest = dict(inputs={name: dict(path=str(elf.path), sha256=SHA(elf.data), image_span=elf.span,
                    loads=[dict(va=p[3], file_offset=p[2], filesz=p[5], memsz=p[6], flags=p[1]) for p in elf.ph if p[0] == 1])
                    for name, elf in elves.items()},
        evidence_inputs={str(p): SHA(p.read_bytes()) for p in evidence},
        guards={name: [{k:v for k,v in r.items() if k != 'bytes'} for r in rows] for name,rows in regions.items()},
        constants=constants, sites=sites, functions=functions,
        serialized_class=dict(ref=91186, classid=cls['classid'], start=cls['at'], end=cls['end'], bitmap=cls['bitmap'], instance_bytes=192),
        scope='Selected complete code extents, exact snapshot class record/cluster/table entries and exact image headers/notes. No whole-process identity or installation claim.')
    return '\n'.join(text), manifest


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--header', type=Path, default=HEADER)
    p.add_argument('--check', action='store_true')
    args = p.parse_args()
    text, manifest = generate()
    again, _ = generate()
    assert text == again
    args.output.mkdir(parents=True, exist_ok=False)
    if args.check:
        assert args.header.read_text() == text, 'Generated header drift'
    else:
        args.header.write_text(text)
    manifest.update(passed=True, regeneration_identical=True, header=dict(path=str(args.header), sha256=SHA(args.header.read_bytes())),
                    generator=dict(path=str(Path(__file__).resolve()), sha256=SHA(Path(__file__).read_bytes())))
    (args.output / 'guard-manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print(json.dumps(dict(passed=True, guards={k:len(v) for k,v in manifest['guards'].items()},
                         bytes=sum(r['size'] for rows in manifest['guards'].values() for r in rows))))


if __name__ == '__main__':
    main()
