#!/usr/bin/env python3
"""Run actual production card binding against exact ROM bytes and source fault controls."""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import re
import subprocess
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
ROOT = test_paths.TEST_DATA_ROOT
CPP = test_paths.SOURCE_ROOT / 'app/src/main/cpp'
TARGET = CPP / 'targets/weather'
SHA = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output', required=True, type=Path)
    args = p.parse_args()
    out = args.output.resolve()
    out.mkdir(parents=True, exist_ok=False)
    headers = [TARGET / n for n in ['weather_card_binding.h', 'weather_card_guard.generated.h']]
    tests = [HERE / n for n in ['generate.py', 'run.py', 'binding_test.cpp', 'compile_probe.cpp']]
    spec = importlib.util.spec_from_file_location('card_generate', HERE / 'generate.py')
    generator = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(generator)
    _, manifest = generator.generate()
    images = ['Dart', 'Flutter']
    inputs = headers + tests + [Path(r['path']) for r in manifest['inputs'].values()] + [Path(p) for p in manifest['evidence_inputs']]
    before = {str(p): SHA(p) for p in inputs}
    commands = []

    def command(argv, stem, timeout=240):
        r = subprocess.run([str(x) for x in argv], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=timeout)
        (out / (stem + '.stdout.txt')).write_text(r.stdout)
        (out / (stem + '.stderr.txt')).write_text(r.stderr)
        commands.append(dict(argv=[str(x) for x in argv], exit_code=r.returncode))
        return r

    generated = command([sys.executable, '-B', HERE/'generate.py', '--check', '--output', out/'regeneration'], 'regeneration')
    assert generated.returncode == 0, generated.stdout + generated.stderr
    manifest = json.loads((out/'regeneration/guard-manifest.json').read_text())
    regions = [(i, r['va'], r['size']) for i, name in enumerate(images) for r in manifest['guards'][name]]
    bits = sum(row[2] for row in regions) * 8
    assert [len(manifest['guards'][n]) for n in images] == [44,10]
    assert bits == 130592
    lines = ['#pragma once', '#include <array>', '#include <span>', 'namespace fixture {',
             'struct Load {uint64_t va,offset,filesz,memsz;};',
             'struct Region {size_t image;uintptr_t va;size_t size;};',
             'inline constexpr std::array<uintptr_t,2> bases={0x100000000ULL,0x200000000ULL};',
             f'inline constexpr uint64_t guard_bits={bits};']
    for name in images:
        loads = manifest['inputs'][name]['loads']
        lines.append(f'inline constexpr std::array<Load,{len(loads)}> {name.lower()}_loads={{{{')
        lines += ['{%s,%s,%s,%s},' % (x['va'],x['file_offset'],x['filesz'],x['memsz']) for x in loads]
        lines.append('}};')
    lines.append('inline constexpr std::array<std::span<const Load>,2> loads={dart_loads,flutter_loads};')
    lines.append(f'inline constexpr std::array<Region,{len(regions)}> regions={{{{')
    lines += ['{%s,%s,%s},' % row for row in regions]
    lines += ['}};', '}']
    fixture = out / 'fixture.generated.h'
    fixture.write_text('\n'.join(lines) + '\n')

    def host(include, stem):
        exe = out / stem
        built = command(['g++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-Wno-mismatched-new-delete',
                         '-fsanitize=undefined','-fno-sanitize-recover=all','-I',include,'-I',out,
                         HERE/'binding_test.cpp','-o',exe], stem+'-build')
        assert built.returncode == 0, built.stdout + built.stderr
        ran = command([exe, *[manifest['inputs'][n]['path'] for n in images]], stem+'-tests')
        m = re.search(r'RESULT passes=(\d+) failures=(\d+) guard_bit_mutations=(\d+)', ran.stdout)
        counts = dict(passed=int(m[1]),failed=int(m[2]),guard_bit_mutations=int(m[3])) if m else None
        return ran, counts

    positive, counts = host(CPP, 'positive')
    assert positive.returncode == 0 and counts and counts['failed'] == 0 and counts['guard_bit_mutations'] == bits, positive.stdout + positive.stderr
    body = headers[0].read_text()
    mutations = [
        ('skip_all_guard_bytes', [('std::memcmp(actual.data(), guard.bytes.data() + offset, count) != 0','false')],
         'every original guard bit rejected'),
        ('allow_wrong_branch_word', [('observed != words[i]','(observed != words[i] && false)'), ('observed != expected','(observed != expected && false)')],
         'wrong branch target rejected 0'),
        ('skip_final_site_reread', [('observed != expected','(observed != expected && false)')],
         'final site reread catches change mask 0 site 0'),
        ('allow_BL_plan', [('(word & 0xfc000000u) != 0x14000000u','(word & 0x7c000000u) != 0x14000000u')],
         'BL opcode rejected as approved plan'),
        ('truncate_owned_mask', [('const BranchWords &expected_branch, unsigned owned_mask)', 'const BranchWords &expected_branch, uint8_t owned_mask)')],
         'invalid nontruncated ownership mask 256'),
        ('publish_output_early', [('if (!detail::bases(dart_base, flutter_base)) return false;', 'out = {};\n    if (!detail::bases(dart_base, flutter_base)) return false;')],
         'output published only after all original reads'),
        ('skip_Flutter_guards', [('if (!match(binding.flutter_base, weather_card_guard::kFlutterImageSpan, guard, words, 0, false, read)) return false;',
                                 'if (false && !match(binding.flutter_base, weather_card_guard::kFlutterImageSpan, guard, words, 0, false, read)) return false;')],
         'every original guard bit rejected'),
    ]
    negatives = []
    for name, changes, witness in mutations:
        changed = body
        for old, new in changes:
            assert changed.count(old) == 1, (name,old)
            changed = changed.replace(old,new)
        include = out / ('mutation-' + name)
        dest = include / 'targets/weather'
        dest.mkdir(parents=True)
        for header in headers:
            (dest/header.name).write_bytes(changed.encode() if header==headers[0] else header.read_bytes())
        result, summary = host(include,name)
        detected = result.returncode != 0 and summary and summary['failed'] > 0 and ('FAIL '+witness) in result.stdout
        negatives.append(dict(name=name,detected=bool(detected),witness=witness,exit_code=result.returncode,
                              counts=summary,mutated_header_sha256=SHA(dest/headers[0].name)))
    compiler = test_paths.ANDROID_NDK_HOME / 'toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android35-clang++'
    ndk = command([compiler,'-std=c++20','-O2','-Wall','-Wextra','-Werror','-fno-exceptions','-fno-rtti',
                   '-I',CPP,'-c',HERE/'compile_probe.cpp','-o',out/'ndk-api35.o'],'ndk-api35')
    after = {str(p):SHA(p) for p in inputs}
    passed = before==after and all(x['detected'] for x in negatives) and ndk.returncode==0
    report = dict(passed=passed, tests=counts, negatives=negatives,
        ndk=dict(passed=ndk.returncode==0,exit_code=ndk.returncode,compiler=str(compiler),compiler_sha256=SHA(compiler),
                 object_sha256=SHA(out/'ndk-api35.o') if ndk.returncode==0 else None),
        inputs_before=before,inputs_after=after,inputs_unchanged=before==after,
        production_inputs={str(p):before[str(p)] for p in headers},
        fixture=dict(path=str(fixture),sha256=SHA(fixture)),
        extraction_report=dict(path=str(out/'regeneration/guard-manifest.json'),sha256=SHA(out/'regeneration/guard-manifest.json')),
        commands=commands,limitations=[
            'Tests compile and call the production binding header using actual ROM PT_LOAD bytes with simulated memory reads. No Dart VM instructions execute here.',
            '130592 guarded bits are mutated individually in original state; every guarded byte is also mutated in the fully owned state.',
            'The four exact ownership masks cover both original, first-only, second-only and both-installed word states. This is not a multiword atomic installation proof.',
            'The supplied branch plan is an installer-owned promise; binding checks exact words/B encoding/range but does not validate executable gateway template ownership.',
            'No heap, Binder, loader, locks or writes in production component; bounded reader may still perform syscalls/pagefaults.',
            'Caller must bind actual Flutter caller/loader/handle to the Dart image and preserve image lifetimes. Final word reread does not eliminate TOCTOU.',
            'No installer, HWS publisher, lifecycle authorization or phone approval. Device status NOT_TESTED.'])
    (out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(dict(passed=passed,tests=counts,negative_controls=len(negatives),ndk=ndk.returncode,report=str(out/'report.json')),indent=2))
    return 0 if passed else 1


if __name__ == '__main__':
    raise SystemExit(main())
