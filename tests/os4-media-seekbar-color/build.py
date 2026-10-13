#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json,os,subprocess,zipfile

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

BASE=Path(__file__).resolve().parent;SOURCE=BASE.parents[1];ROOT=test_paths.TEST_DATA_ROOT;CACHE=test_paths.BUILD_CACHE;JAVA=test_paths.JAVA_HOME / 'bin';KH=test_paths.KOTLIN_HOME;SDK=test_paths.ANDROID_HOME;BT=SDK/'build-tools/37.0.0';ANDROID=SDK/'platforms/android-37.0/android.jar'
REL='library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/controlcenter/media3/MediaSeekBarColorOS4.kt';MATH='library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/utils/hookapi/systemui/controlcenter/media/MediaViewColorConfig.kt';SNAP=test_paths.SNAPSHOT_ROOT / 'source-os4-hyos-test12-20261009-r1';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args();o=a.output.resolve();o.mkdir(parents=True,exist_ok=False)
 source=BASE/'MainActivity.java';inputs={str(q):sha(q)for q in [source,BASE/'AndroidManifest.xml',Path(__file__),SOURCE/REL]};(o/'classes').mkdir();(o/'dex').mkdir();commands=[]
 deps=[CACHE/'changed-hooks-compile'/n for n in ['hook-xposed-102-1.3.0.jar','core-1.3.0.jar','api-102.0.0.jar']]+[KH/'lib/kotlin-stdlib.jar'];cp=os.pathsep.join(map(str,[ANDROID,*deps]))
 def run(c):
  commands.append(list(map(str,c)));r=subprocess.run(list(map(str,c)),capture_output=True,text=True,env=dict(os.environ,JAVA_HOME=str(JAVA.parent),PATH=str(JAVA)+os.pathsep+os.environ['PATH']));(o/'build.log').open('a').write(r.stdout+r.stderr);assert r.returncode==0,r.stdout+r.stderr
 (o/'BuildMetadata.java').write_text('package local.hyperceiler.seekbarcolorprobe; final class BuildMetadata { static final String SOURCE_SHA256="'+sha(source)+'";}')
 run([JAVA/'java','-cp',KH/'lib/kotlin-compiler.jar','org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',KH,'-jvm-target','1.8','-classpath',cp,'-d',o/'classes',SOURCE/REL])
 cp=os.pathsep.join(map(str,[ANDROID,o/'classes',*deps]))
 run([JAVA/'javac','--release','8','-proc:none','-cp',cp,'-d',o/'classes',BASE/'MainActivity.java',o/'BuildMetadata.java']);run([JAVA/'jar','--create','--file',o/'classes.jar','-C',o/'classes','.'])
 run([BT/'d8','--min-api','35','--lib',ANDROID,'--output',o/'dex',o/'classes.jar',*deps])
 run([BT/'aapt2','link','-I',ANDROID,'--manifest',BASE/'AndroidManifest.xml','-o',o/'resources.apk'])
 ota=test_paths.ROM_ROOT / 'components/system_ext/priv-app/MiuiSystemUI/MiuiSystemUI.apk';asset_names=['res/raw/music_player_tracker.frag','res/raw/music_player_tracker_middle.frag','res/drawable/miuix_hyper_progressbar_light_head.png'];assets={}
 with zipfile.ZipFile(ota)as rom:
  assets={n:rom.read(n) for n in asset_names}
 with zipfile.ZipFile(o/'resources.apk')as z,zipfile.ZipFile(o/'unaligned.apk','w')as dest:
  for item in z.infolist():dest.writestr(item,z.read(item.filename))
  for n,data in assets.items():dest.writestr('assets/'+Path(n).name,data)
  for dex in sorted((o/'dex').glob('*.dex')):dest.write(dex,dex.name,compress_type=zipfile.ZIP_DEFLATED)
 run([BT/'zipalign','-f','-P','16','4',o/'unaligned.apk',o/'aligned.apk'])
 run([BT/'apksigner','sign','--ks',test_paths.TEST_DATA_ROOT / 'emulation/native-probe/build/test-only.keystore','--ks-key-alias','nativeprobe','--ks-pass','pass:android','--key-pass','pass:android','--out',o/'seekbar-color-probe.apk',o/'aligned.apk']);run([BT/'apksigner','verify','--verbose',o/'seekbar-color-probe.apk'])
 assert all(sha(Path(n))==h for n,h in inputs.items())
 dependencies={str(q):sha(q) for q in [ANDROID,KH/'lib/kotlin-compiler.jar',*deps]};report=dict(passed=True,inputs=inputs,dependencies=dependencies,commands=commands,apk_sha256=sha(o/'seekbar-color-probe.apk'),ota=dict(path=str(ota),sha256=sha(ota),members={n:hashlib.sha256(v).hexdigest() for n,v in assets.items()}),device_status='NOT_TESTED',scope='Compile production MediaSeekBarColorOS4 with actual API37 and EzHook/API102 types; manually dispatched callback target is explicit SeekBar fixture; original OTA shaders and head texture.')
 (o/'build-report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
if __name__=='__main__':main()
