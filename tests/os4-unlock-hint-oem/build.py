#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,json,os,subprocess,zipfile,importlib.util,sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

sys.dont_write_bytecode=True
BASE=Path(__file__).resolve().parent;SOURCE=BASE.parents[1];ROOT=test_paths.TEST_DATA_ROOT;CACHE=test_paths.BUILD_CACHE;JAVA=test_paths.JAVA_HOME / 'bin';KH=test_paths.KOTLIN_HOME;SDK=test_paths.ANDROID_HOME;BT=SDK/'build-tools/37.0.0';ANDROID=SDK/'platforms/android-37.0/android.jar';OTA=test_paths.ROM_ROOT / 'components/system_ext/priv-app/MiuiSystemUI/MiuiSystemUI.apk'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--rule-source',type=Path);a=p.parse_args();o=a.output.resolve();o.mkdir(parents=True,exist_ok=False)
 original=SOURCE/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/lockscreen/HideLockScreenHintOS4.kt'
 production=(a.rule_source or original).resolve()
 files=[q for q in BASE.rglob('*')if q.is_file()and q.suffix in {'.py','.java','.xml','.kt'}];inputs={str(q):sha(q)for q in files+[production]};commands=[]
 def run(c):
  commands.append(list(map(str,c)));r=subprocess.run(list(map(str,c)),capture_output=True,text=True,env=dict(os.environ,JAVA_HOME=str(JAVA.parent),PATH=str(JAVA)+os.pathsep+os.environ['PATH']));(o/'build.log').open('a').write(r.stdout+r.stderr);assert r.returncode==0,r.stdout+r.stderr
 for n in ['adapter-classes','adapter-dex','classes','dex']:(o/n).mkdir()
 stdlib=KH/'lib/kotlin-stdlib.jar';deps=[CACHE/'changed-hooks-compile'/n for n in ['hook-xposed-102-1.3.0.jar','core-1.3.0.jar','api-102.0.0.jar']]+[stdlib];cp=os.pathsep.join(map(str,[ANDROID,*deps]))
 adapter_src=sorted((BASE/'adapters').rglob('*.java'));compile_only=sorted((BASE/'compile-only').rglob('*.java'))
 run([JAVA/'javac','--release','8','-proc:none','-cp',cp,'-d',o/'adapter-classes',*adapter_src,*compile_only])
 (o/'adapter-classes/com/android/systemui/keyguard/KeyguardIndication.class').unlink()
 run([JAVA/'jar','--create','--file',o/'adapter-classes.jar','-C',o/'adapter-classes','.'])
 run([BT/'d8','--min-api','35','--lib',ANDROID,'--output',o/'adapter-dex',o/'adapter-classes.jar'])
 with zipfile.ZipFile(o/'adapters.jar','w')as z:
  for d in (o/'adapter-dex').glob('*.dex'):z.write(d,d.name)
 sp=importlib.util.spec_from_file_location('dexinv',test_paths.TEST_DATA_ROOT / 'tools/dex_inventory.py');m=importlib.util.module_from_spec(sp);sp.loader.exec_module(m)
 original_names=['com.android.systemui.keyguard.KeyguardIndicationRotateTextViewController','com.android.systemui.keyguard.KeyguardIndication','com.android.systemui.util.ViewController']
 classes={}
 with zipfile.ZipFile(o/'adapters.jar')as z:
  for n in z.namelist():classes.update(m.dex_classes(z.read(n)))
 assert not any(n.startswith(tuple(original_names))for n in classes),classes
 (o/'BuildMetadata.java').write_text('package local.hyperceiler.unlockhintoem; final class BuildMetadata { static final String OTA_SHA="'+sha(OTA)+'", ADAPTER_SHA="'+sha(o/'adapters.jar')+'", SOURCE_SHA="'+sha(BASE/'MainActivity.java')+'";}')
 run([JAVA/'javac','--release','8','-proc:none','-cp',cp,'-d',o/'classes',*sorted((BASE/'probe-support').rglob('*.java'))])
 cp=os.pathsep.join(map(str,[ANDROID,o/'classes',*deps]))
 run([JAVA/'java','-cp',KH/'lib/kotlin-compiler.jar','org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',KH,'-jvm-target','1.8','-classpath',cp,'-d',o/'classes',production])
 run([JAVA/'javac','--release','8','-proc:none','-cp',cp,'-d',o/'classes',BASE/'MainActivity.java',o/'BuildMetadata.java'])
 run([JAVA/'jar','--create','--file',o/'classes.jar','-C',o/'classes','.'])
 run([BT/'d8','--min-api','35','--lib',ANDROID,'--output',o/'dex',o/'classes.jar',*deps])
 main_classes={}
 for dex in (o/'dex').glob('*.dex'):main_classes.update(m.dex_classes(dex.read_bytes()))
 assert not any(n.startswith(tuple(original_names))for n in main_classes),main_classes
 run([BT/'aapt2','link','-I',ANDROID,'--manifest',BASE/'AndroidManifest.xml','-o',o/'resources.apk'])
 with zipfile.ZipFile(o/'resources.apk')as z,zipfile.ZipFile(o/'unaligned.apk','w')as dest:
  for item in z.infolist():dest.writestr(item,z.read(item.filename))
  dest.write(OTA,'assets/oem.apk',compress_type=zipfile.ZIP_DEFLATED);dest.write(o/'adapters.jar','assets/adapters.jar',compress_type=zipfile.ZIP_DEFLATED)
  for dex in sorted((o/'dex').glob('*.dex')):dest.write(dex,dex.name,compress_type=zipfile.ZIP_DEFLATED)
 run([BT/'zipalign','-f','-P','16','4',o/'unaligned.apk',o/'aligned.apk'])
 run([BT/'apksigner','sign','--ks',test_paths.TEST_DATA_ROOT / 'emulation/native-probe/build/test-only.keystore','--ks-key-alias','nativeprobe','--ks-pass','pass:android','--key-pass','pass:android','--out',o/'unlock-hint-oem-probe.apk',o/'aligned.apk']);run([BT/'apksigner','verify','--verbose',o/'unlock-hint-oem-probe.apk'])
 assert all(sha(Path(n))==h for n,h in inputs.items())
 with zipfile.ZipFile(OTA)as z:dexhash={n:hashlib.sha256(z.read(n)).hexdigest()for n in z.namelist()if n.endswith('.dex')}
 report=dict(passed=True,inputs=inputs,dependencies={str(q):sha(q)for q in [ANDROID,*deps,KH/'lib/kotlin-compiler.jar',test_paths.TEST_DATA_ROOT / 'tools/dex_inventory.py']},commands=commands,apk_sha256=sha(o/'unlock-hint-oem-probe.apk'),ota=dict(path=str(OTA),sha256=sha(OTA),dex=dexhash),adapters=dict(path=str(o/'adapters.jar'),sha256=sha(o/'adapters.jar'),classes=sorted(classes),main_dex_classes=sorted(main_classes),forbidden_prefixes=original_names,original_target_classes_absent_from_adapters_and_parent=True),production=dict(path=str(production),sha256=sha(production),canonical_path=str(original),canonical_sha256=sha(original),research_mutation=production!=original),device_status='NOT_TESTED',scope='Full unmodified OTA APK asset; explicit dependency adapters; original controller and indication classes excluded from adapters and main DEX.')
 (o/'build-report.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(dict(passed=True,apk_sha256=report['apk_sha256'],adapter_classes=len(classes))))
if __name__=='__main__':main()
