#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""Run the actual Kotlin policy against typed WMS/Handler and hook-dispatch fixtures."""
import os,subprocess,tempfile
from pathlib import Path
root=Path(__file__).resolve().parents[2];here=Path(__file__).parent
jdk=Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home'))/'bin'
cache=Path(os.environ.get('GRADLE_USER_HOME',str(Path.home()/'.gradle')))/'caches/modules-2/files-2.1'
def jar(group,artifact,version):
    found=list((cache/group/artifact/version).glob('*/*.jar'))
    if len(found)!=1: raise SystemExit('Configure Gradle dependency cache for '+artifact+' '+version)
    return found[0]
stdlib=jar('org.jetbrains.kotlin','kotlin-stdlib','2.4.10')
hookdeps=[jar('io.github.lingqiqi5211.ezhooktool','hook-xposed-102','1.3.0'),jar('io.github.libxposed','api','102.0.0')]
compiler=[jar('org.jetbrains.kotlin',a,'2.4.10') for a in ['kotlin-compiler-embeddable','kotlin-daemon-embeddable','kotlin-script-runtime']]
compiler += [stdlib,jar('org.jetbrains.kotlin','kotlin-reflect','2.4.0'),jar('org.jetbrains.kotlinx','kotlinx-coroutines-core-jvm','1.8.0'),jar('org.jetbrains','annotations','13.0')]
def run(args):subprocess.run([str(x) for x in args],check=True)
with tempfile.TemporaryDirectory(prefix='hc-recents-policy-') as temp:
    out=Path(temp);lib=root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock'
    java=list((here/'policy').rglob('*.java'))+[lib/'HomeRecentsBarState.java',lib.parents[2]/'base/OriginalArgumentsHook.java']
    java += [root/'tests/home-dock-window/stubs/android/os'/f for f in ['Binder.java','IBinder.java','Parcel.java']]
    java += [root/'tests/home-layout-native/stubs/android/os/SystemClock.java']
    run([jdk/'javac','--release','21','-cp',os.pathsep.join(map(str,[stdlib,*hookdeps])),'-d',out,*java])
    kotlin=list((here/'policy').glob('*.kt'))+[lib/'HomeRecentsStatusBarOS4.kt']
    cp=os.pathsep.join(map(str,[out,stdlib,compiler[-1],*hookdeps]))
    run([jdk/'java','-cp',os.pathsep.join(map(str,compiler)),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-jvm-target','21','-classpath',cp,'-d',out,*kotlin])
    run([jdk/'java','-ea','-cp',cp,'com.sevtinge.hyperceiler.libhook.rules.home.dock.PolicyTestKt'])
