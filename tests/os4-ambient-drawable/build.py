#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json,os,subprocess,zipfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

BASE=Path(__file__).resolve().parent;SOURCE=BASE.parents[1];ROOT=test_paths.TEST_DATA_ROOT;CACHE=test_paths.BUILD_CACHE;JAVA=test_paths.JAVA_HOME / 'bin';KH=test_paths.KOTLIN_HOME;SDK=test_paths.ANDROID_HOME;BT=SDK/'build-tools/37.0.0';ANDROID=SDK/'platforms/android-37.0/android.jar'
REL='library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/utils/hookapi/systemui/controlcenter/drawable/AmbientLightDrawable.kt';MATH='library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/utils/api/MathUtils.java';SNAP=test_paths.SNAPSHOT_ROOT / 'source-os4-hyos-test12-20261009-r1';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('--variant',choices=['baseline','fixed'],required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();o=a.output.resolve();o.mkdir(parents=True,exist_ok=False)
 source=(SNAP if a.variant=='baseline' else SOURCE)/REL;math=SOURCE/MATH;animation=source.parent/'AnimationState.kt';inputs={str(q):sha(q)for q in [source,math,animation,BASE/'MainActivity.java',BASE/'AndroidManifest.xml',Path(__file__)]};body=source.read_bytes();(o/'AmbientLightDrawable.kt').write_bytes(body);(o/'classes').mkdir();(o/'dex').mkdir();commands=[]
 cp=os.pathsep.join(map(str,[ANDROID,CACHE/'changed-hooks-compile/core-1.3.0.jar',SNAP/'library/common/build/intermediates/compile_library_classes_jar/debug/bundleLibCompileToJarDebug/classes.jar']))
 def run(c):
  commands.append(list(map(str,c)));r=subprocess.run(list(map(str,c)),capture_output=True,text=True,env=dict(os.environ,JAVA_HOME=str(JAVA.parent),PATH=str(JAVA)+os.pathsep+os.environ['PATH']));(o/'build.log').open('a').write(r.stdout+r.stderr);assert r.returncode==0,r.stdout+r.stderr
 run([JAVA/'javac','--release','8','-proc:none','-cp',cp,'-d',o/'classes',math])
 run([JAVA/'java','-cp',KH/'lib/kotlin-compiler.jar','org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',KH,'-jvm-target','1.8','-classpath',str(o/'classes')+os.pathsep+str(ANDROID),'-d',o/'production.jar',o/'AmbientLightDrawable.kt',animation])
 (o/'BuildMetadata.java').write_text('package local.hyperceiler.ambientprobe; final class BuildMetadata { static final String VARIANT="'+a.variant+'";static final String SOURCE_SHA256="'+sha(source)+'";static final String MATH_SHA256="'+sha(math)+'";}')
 cp=os.pathsep.join(map(str,[ANDROID,o/'classes',o/'production.jar',KH/'lib/kotlin-stdlib.jar']))
 run([JAVA/'javac','--release','8','-proc:none','-cp',cp,'-d',o/'classes',BASE/'MainActivity.java',o/'BuildMetadata.java']);run([JAVA/'jar','--create','--file',o/'classes.jar','-C',o/'classes','.'])
 run([BT/'d8','--min-api','35','--lib',ANDROID,'--classpath',CACHE/'changed-hooks-compile/core-1.3.0.jar','--classpath',SNAP/'library/common/build/intermediates/compile_library_classes_jar/debug/bundleLibCompileToJarDebug/classes.jar','--output',o/'dex',o/'classes.jar',o/'production.jar',KH/'lib/kotlin-stdlib.jar'])
 run([BT/'aapt2','link','-I',ANDROID,'--manifest',BASE/'AndroidManifest.xml','-o',o/'resources.apk'])
 with zipfile.ZipFile(o/'resources.apk')as z,zipfile.ZipFile(o/'unaligned.apk','w')as dest:
  for item in z.infolist():dest.writestr(item,z.read(item.filename))
  for dex in sorted((o/'dex').glob('*.dex')):dest.write(dex,dex.name,compress_type=zipfile.ZIP_DEFLATED)
 run([BT/'zipalign','-f','-P','16','4',o/'unaligned.apk',o/'aligned.apk'])
 run([BT/'apksigner','sign','--ks',test_paths.TEST_DATA_ROOT / 'emulation/native-probe/build/test-only.keystore','--ks-key-alias','nativeprobe','--ks-pass','pass:android','--key-pass','pass:android','--out',o/'ambient-probe.apk',o/'aligned.apk']);run([BT/'apksigner','verify','--verbose',o/'ambient-probe.apk'])
 assert all(sha(Path(n))==h for n,h in inputs.items())
 dependencies={str(q):sha(q) for q in [ANDROID,KH/'lib/kotlin-compiler.jar',KH/'lib/kotlin-stdlib.jar',CACHE/'changed-hooks-compile/core-1.3.0.jar',SNAP/'library/common/build/intermediates/compile_library_classes_jar/debug/bundleLibCompileToJarDebug/classes.jar']};report=dict(passed=True,variant=a.variant,inputs=inputs,dependencies=dependencies,commands=commands,apk_sha256=sha(o/'ambient-probe.apk'),device_status='NOT_TESTED',scope='Probe build, actual complete drawable source; MathUtils full source compiled. Its unrelated fitRect logger/EzHook dependencies are compile-classpath only and not invoked by the drawable.')
 (o/'build-report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
if __name__=='__main__':main()
