#!/usr/bin/env python3
"""Bind the resolver component's distinct host, SDK, ROM and ART evidence."""
from pathlib import Path
import argparse, hashlib, json

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


ROOT = test_paths.TEST_DATA_ROOT
BASE = test_paths.TEST_DATA_ROOT / 'acceptance/java-test13/ambient-color-v1'
PREFIX = 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/controlcenter/media3/'
SOURCES = [PREFIX+'AmbientColorResolverOS4.kt', PREFIX+'AmbientNativeColorOS4.kt']
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
sources = {p: sha(test_paths.SOURCE_ROOT/p) for p in SOURCES}
checks, evidence = [], {}
def check(name, value):
    checks.append({'name': name, 'passed': bool(value)})
def read(name, relative, passed=True):
    p = BASE/relative; d = json.loads(p.read_text())
    evidence[name] = {'path': str(test_paths.report_path(p, ROOT)), 'sha256': sha(p)}
    if passed: check(name+' reports success', d.get('passed') is True)
    return d
def current_inputs(name, d):
    before, after = d['inputs_before'], d['inputs_after']
    check(name+' before/after source inputs identical', before == after)
    check(name+' both complete current Kotlin sources',
          all(before.get(str(test_paths.report_path(test_paths.SOURCE_ROOT/p, ROOT))) == h for p,h in sources.items()))
    check(name+' all recorded source/fixture bytes still current',
          all((ROOT/p).is_file() and sha(ROOT/p) == h for p,h in before.items()))

host = read('host', 'private-bridge-host-v4/report.json')
current_inputs('host', host)
check('88 complete production host scenarios passed', host['tests'] == {'passed':88,'failed':0})
mutants = read('resolver_mutants', 'private-bridge-host-v4/negative-controls.json')
check('17 resolver compiled source mutations rejected', len(mutants['checks']) == 17
      and all(x['rejected'] is True and x['failed_tests'] > 0 for x in mutants['checks']))
bridge = read('bridge_protocol', 'bridge-controls-v3/report.json'); current_inputs('bridge', bridge)
check('17 fresh-JVM bridge assertions and six rejected mutations',
      sum(x['assertions'] for x in bridge['checks'][0]['modes']) == 17
      and len(bridge['checks']) == 7 and all(x['passed'] is True for x in bridge['checks']))
reload = read('reload_protocol', 'reload-controls-v1/report.json'); current_inputs('reload', reload)
check('28 reload assertions and six rejected mutations',
      sum(x['assertions'] for x in reload['checks'][0]['modes']) == 28
      and len(reload['checks']) == 7 and all(x['passed'] is True for x in reload['checks']))
shapes = read('dependency_shapes', 'shape-controls-v3/report.json'); current_inputs('shapes', shapes)
check('six wrong dependency shapes rejected', len(shapes['checks']) == 6
      and all(x['passed'] is True for x in shapes['checks']))
route = read('shared_native_route_rejected', 'shared-route-control-v2/report.json')
check('replacement by old shared JNI rejected by two exact protocol witnesses',
      route['source_sha256'] == sources[SOURCES[0]] and route['bridge_sha256'] == sources[SOURCES[1]]
      and route['witnesses'] == {'native_unavailable_is_not_transparent_success': True,
                                'native_invalid_success_word_rejected': True})
sdk = read('sdk37', 'android-compile-v3/report.json')
check('actual SDK37 compile binds both current sources', sdk['sources'] == sources and sdk['exit_code'] == 0)
rom = read('rom', 'rom-v3/report.json')
check('19 exact OTA ROM/ABI checks passed', rom['check_count'] == 19
      and all(x['passed'] is True for x in rom['checks']))
art = read('actual_art', 'android-art-v6/report.json')
check('49 real ART checks with actual media init and separate loaders', art['android_check_count'] == 49
      and art['failed_checks'] == [] and art['oem_media_init'] is True and art['single_color_loader'] is False)
check('actual ART binds current Kotlin source pair', art['source_sha256'] == sources[SOURCES[0]]
      and art['wrapper_sha256'] == sources[SOURCES[1]])
build_path = Path(art['build']['path']); build = json.loads(build_path.read_text())
check('ART exact APK build input source binding', build['passed'] is True and sha(build_path) == art['build']['sha256']
      and all(build['inputs'].get(str(test_paths.SOURCE_ROOT/p)) == h for p,h in sources.items()))
for name in ['android-report.json', 'hidden-api-policy.json']:
    read('actual_art_'+name, 'android-art-v6/'+name)
policy = json.loads((BASE/'android-art-v6/hidden-api-policy.json').read_text())
check('temporary emulator hidden API policy restored', policy['original'] == policy['restored'] == 'null')
art_reload = read('actual_art_reload', 'android-reload-v1/report.json')
check('eight actual ART same-path loader ownership checks passed', len(art_reload['checks']) == 8
      and art_reload['inputs_unchanged'] is True and all(x['passed'] is True for x in art_reload['checks']))

historical = []
for directory, reason in [
    ('android-art-v1','Ordinary app blocked platform hidden APIs; also dimensionless converter gap.'),
    ('android-art-v2','Actual allowed dimensionless adaptive input still returned unavailable before Canvas correction.'),
    ('android-art-v3','Original strict-NOLOAD private bridge failed across genuine distinct loader namespaces.'),
    ('android-art-v4','Same-loader control: uninitialized OEM colors differ from explicitly initialized private media algorithm.'),
    ('android-art-v5','Fixed namespace topology works; preserved three uninitialized-OEM differences among 48 checks.')]:
    p = BASE/directory/'report.json'; d = json.loads(p.read_text())
    check(directory+' original failed result preserved', d['passed'] is False)
    historical.append({'path':str(test_paths.report_path(p, ROOT)), 'sha256':sha(p), 'passed':False, 'meaning':reason})

report = {'passed':all(x['passed'] for x in checks), 'sources':sources,
    'checks':checks, 'check_count':len(checks), 'evidence':evidence,
    'counts':{'host_scenarios':88,'resolver_mutations':17,'bridge_protocol_assertions':17,
              'bridge_protocol_mutations':6,'reload_assertions':28,'reload_mutations':6,
              'shape_rejections':6,'old_shared_route_mutations':1,'rom_checks':19,
              'actual_art_checks':49,'actual_art_reload_checks':8},
    'historical_failures':historical,
    'art_native_input':{p:h for p,h in build['inputs'].items() if '/targets/systemui/' in p},
    'standard_contract':'Both NC and DI use the initialized OS4 media algorithm with private tone/chroma20/20. Actual DI attach calls complete MiPalette.init(); NC deliberately uses that same initialized algorithm independently of original singleton lifecycle. No claim of bit equivalence before OEM DI initialization; production never initializes or modifies the OEM singleton.',
    'limits':[
      'This report covers the two color helper classes only. Lifecycle selection, hook registration, holder publication, drawable rendering, hook hotreload gate, and complete release artifacts require separate evidence.',
      'Host graphics and sampling fixtures are adapters/transcription; host JNI is a recording/protocol adapter, not proprietary HCT execution.',
      'Real ART uses original complete OTA DEX and ARM vendor code via emulator nativebridge; private bridge test build changes only its fixed file path. Hidden APIs were temporarily allowed on the dedicated AVD and restored; this is not SystemUI or LSPosed permission proof.',
      'Fancy object was allocated without its live renderer constructor solely to exercise the actual quiet getter. Static quiet/private adaptive snapshots are not animated-frame equivalence. Unknown unsafe drawables may fall back to the original cross-user application icon; unavailable inputs retain OEM state and are not successful color coverage.',
      'The separate native owner must validate private ELF identity, native histogram isolation, concurrency, namespace loading, permanent handle/thread cost and failures. This resolver report does not substitute for that native review.',
      'A loaded standard backend cannot migrate to another live module classloader; the entry must reject hotreload before cleanup. Wrapper admission is independently tested here, but entry integration is not.',
      'No actual Xiaomi phone, LSPosed injection, work-profile user, NC/DI animation, restart or long-term-stability pass exists.'],
    'device_status':'NOT_TESTED','physical_device_pass_count':0,'artifact_runtime_approval':False,
    'builder':{'path':str(test_paths.report_path(Path(__file__), ROOT)),'sha256':sha(Path(__file__))}}
p = argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args()
with a.output.open('x') as f: json.dump(report,f,ensure_ascii=False,indent=2);f.write('\n')
print(json.dumps({'passed':report['passed'],'check_count':len(checks),
                  'failed':[x['name']for x in checks if not x['passed']],'sha256':sha(a.output)}))
raise SystemExit(0 if report['passed'] else 1)
