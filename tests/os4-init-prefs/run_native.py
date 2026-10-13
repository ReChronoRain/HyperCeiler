#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""Execute production NativeHome Kotlin with a host JNI argument recorder."""
import argparse
import os
from pathlib import Path
import subprocess
import tempfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


ROOT=Path(__file__).resolve().parents[2]
TESTS=Path(__file__).resolve().parent
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--production',type=Path,default=ROOT/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/other/NativeHomeHooksOS4.kt')
parser.add_argument('--kotlin-home',type=Path,default=test_paths.KOTLIN_HOME)
parser.add_argument('--jdk',type=Path,default=test_paths.JAVA_HOME)
args=parser.parse_args()
with tempfile.TemporaryDirectory(prefix='hc-native-prefs-') as temp:
    temp=Path(temp)
    subprocess.run(['g++','-shared','-fPIC','-std=c++17','-I'+str(args.jdk/'include'),'-I'+str(args.jdk/'include/linux'),
                    str(TESTS/'native_probe.cpp'),'-o',str(temp/'libHyperCeilerNative.so')],check=True)
    clock=temp/'AndroidOs.kt'
    clock.write_text('package android.os\nclass Bundle { fun putStringArray(key:String,value:Array<String>)=Unit }\nobject SystemClock { fun sleep(millis:Long)=Unit; fun uptimeMillis()=0L }\n')
    excluded={'PrefsBridge.kt','AndroidOs.kt','NativeHomeDockListenerTest.kt'}
    stubs=[p for p in (ROOT/'tests/native-home-dock-listener').glob('*.kt') if p.name not in excluded]
    jar=temp/'tests.jar'
    subprocess.run([str(args.jdk/'bin/java'),'-cp',str(args.kotlin_home/'lib/kotlin-compiler.jar'),
        'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',str(args.kotlin_home),'-jvm-target','17','-d',str(jar),
        str(args.production),str(TESTS/'NativePrefsBridge.kt'),str(TESTS/'NativeRecoveryTest.kt'),str(clock),*[str(p) for p in stubs]],check=True)
    cases=['restore_and_idempotence','module_entry_cannot_recover','listener_retry_and_reentry','configuration_failure_retry',
           'read_failure_retry','failed_library_never_configures','healthy_bootstrap_unchanged','no_preferences_yet',
           'non_null_handle_failed_bootstrap','changed_preference_handle','late_concurrent_recovery']
    failures=0
    for case in cases:
        libdir=temp/'missing' if case=='failed_library_never_configures' else temp
        result=subprocess.run([str(args.jdk/'bin/java'),'--enable-native-access=ALL-UNNAMED','-Djava.library.path='+str(libdir),
            '-cp',str(jar)+os.pathsep+str(args.kotlin_home/'lib/kotlin-stdlib.jar'),'fixture.NativeRecoveryTestKt',case])
        failures+=result.returncode!=0
    print(f'Native preference recovery: {len(cases)-failures}/{len(cases)} PASS')
    raise SystemExit(bool(failures))
