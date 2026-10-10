#!/usr/bin/env python3
import argparse
import os
from pathlib import Path
import subprocess
import tempfile

p = argparse.ArgumentParser()
p.add_argument('--java', default='java')
p.add_argument('--kotlin-home', required=True, type=Path)
p.add_argument('--production', type=Path)
a = p.parse_args()
test = Path(__file__).resolve().parent
root = test.parents[1]
production = a.production or root / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/statusbar/icon/all/SwapWiFiAndMobileNetwork.kt'
with tempfile.TemporaryDirectory(prefix='hc-slot-order-') as output:
    jar = Path(output) / 'test.jar'
    subprocess.run([a.java, '-cp', str(a.kotlin_home/'lib/kotlin-compiler.jar'),
        'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-kotlin-home', str(a.kotlin_home),
        '-jvm-target', '17', '-d', str(jar), str(production),
        *[str(f) for f in sorted(test.glob('*.kt'))]], check=True)
    subprocess.run([a.java, '-cp', os.pathsep.join([str(jar), str(a.kotlin_home/'lib/kotlin-stdlib.jar')]),
        'fixture.SlotOrderTestKt'], check=True)
