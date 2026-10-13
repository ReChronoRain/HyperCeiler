#!/usr/bin/env python3
"""Exercise only the OS4 refusal of a proven unrelated legacy callback."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


BASE = Path(__file__).resolve().parent
SOURCE = BASE.parents[1]
ROOT = test_paths.TEST_DATA_ROOT
CACHE = test_paths.BUILD_CACHE
JAVA = test_paths.JAVA_HOME / 'bin'
KOTLIN = test_paths.KOTLIN_HOME
RULE = SOURCE / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/lockscreen/HideLockScreenHint.kt'
DEPS = [CACHE / 'changed-hooks-compile' / name for name in [
    'hook-xposed-102-1.3.0.jar', 'core-1.3.0.jar', 'api-102.0.0.jar']]
DEPS += [KOTLIN / 'lib/kotlin-stdlib.jar', test_paths.ANDROID_HOME / 'platforms/android-37.0/android.jar']
sha = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()


def run(output):
    output.mkdir(parents=True, exist_ok=False)
    sources = [RULE, Path(__file__), *sorted((BASE / 'fixtures').rglob('*.java'))]
    before = {str(test_paths.report_path(path, ROOT)): sha(path) for path in sources}
    checks = []
    commands = []
    with tempfile.TemporaryDirectory(prefix='hc-hint-decline-') as directory:
        work = Path(directory)
        cp = os.pathsep.join(map(str, DEPS))
        for mutation in [False, True]:
            name = 'wrong-route-restored' if mutation else 'production'
            classes = work / name; classes.mkdir()
            full_cp = str(classes) + os.pathsep + cp
            body = RULE.read_text()
            if mutation:
                assert body.count('if (isMoreHyperOSVersion(4f)) {') == 1
                body = body.replace('if (isMoreHyperOSVersion(4f)) {', 'if (false) {')
            source = classes / RULE.name; source.write_text(body)
            fixtures = [p for p in sources if p.suffix == '.java' and p.name != 'TestMain.java']
            sequence = [
                [JAVA / 'javac', '--release', '21', '-proc:none', '-cp', cp, '-d', classes, *fixtures],
                [JAVA / 'java', '-cp', KOTLIN / 'lib/kotlin-compiler.jar',
                 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-kotlin-home', KOTLIN,
                 '-jvm-target', '21', '-classpath', full_cp, '-d', classes, source],
                [JAVA / 'javac', '--release', '21', '-proc:none', '-cp', full_cp, '-d', classes,
                 BASE / 'fixtures/TestMain.java'],
            ]
            for index, command in enumerate(sequence):
                command = list(map(str, command)); commands.append(command)
                process = subprocess.run(command, capture_output=True, text=True)
                (output / (name + '-compile-' + str(index) + '.txt')).write_text(process.stdout + process.stderr)
                assert process.returncode == 0, process.stdout + process.stderr
            process = subprocess.run([str(JAVA / 'java'), '-ea', '-cp', full_cp, 'TestMain'],
                                     capture_output=True, text=True, timeout=60)
            (output / (name + '-execution.txt')).write_text(process.stdout + process.stderr)
            passed = process.returncode != 0 and 'No fingerprint hook on OS 4.0' in process.stderr if mutation else process.returncode == 0 and 'PASS 9' in process.stdout
            checks.append(dict(name=name, passed=passed, returncode=process.returncode))
    after = {str(test_paths.report_path(path, ROOT)): sha(path) for path in sources}
    checks.append(dict(name='Source files unchanged throughout', passed=before == after))
    report = dict(passed=all(row['passed'] for row in checks), checks=checks,
        inputs_before=before, inputs_after=after, dependencies={str(p): sha(p) for p in DEPS},
        commands=commands, device_status='NOT_TESTED', setting_status='NOT_ADAPTED_WRONG_ROUTE_DECLINED',
        limits=['Full production Kotlin is compiled with the real EzHook1.3.0/API102 and Android37 SDK.',
                'Version, logging, BaseHook and controller/installation backend are host fixtures.',
                'This checks refusal of an unrelated hook. It does not hide an OS4 hint or test device injection.'])
    (output / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(dict(passed=report['passed'], checks=checks)))
    assert report['passed']


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', required=True, type=Path)
    run(parser.parse_args().output)
