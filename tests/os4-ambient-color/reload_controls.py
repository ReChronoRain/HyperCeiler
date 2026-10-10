#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,re,subprocess,tempfile
HERE=Path(__file__).resolve().parent;s=importlib.util.spec_from_file_location('ambient_reload_host',HERE/'run.py');h=importlib.util.module_from_spec(s);s.loader.exec_module(h)
p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False);before=h.inputs();rows=[]
MUTANTS={
 'allow_loaded_reload':('if (loadState == 1) return false','if (loadState == 2) return false'),
 'ignore_pause_in_loader':('if (hotReloadPaused) return false','if (false) return false'),
 'resume_does_not_unfreeze':('hotReloadPaused = false\n    }','hotReloadPaused = true\n    }'),
 'ensure_uses_instance_monitor':('@JvmStatic\n    @Synchronized\n    private fun ensureLoaded','@Synchronized\n    private fun ensureLoaded'),
 'pause_not_synchronized':('@JvmStatic\n    @Synchronized\n    fun pauseForHotReload','@JvmStatic\n    fun pauseForHotReload'),
 'failed_library_reload_rejected':('if (loadState == 1) return false','if (loadState != 0) return false'),
}

for label,change in [('baseline',None),*MUTANTS.items()]:
 case=out/label;case.mkdir();commands=[]
 with tempfile.TemporaryDirectory(prefix='ambient-bridge-')as tmp:
  w=Path(tmp);classes=w/'classes';classes.mkdir();src=w/h.NATIVE_SOURCE.name;body=h.NATIVE_SOURCE.read_text()
  if change:assert body.count(change[0])==1,(label,body.count(change[0]));body=body.replace(*change)
  src.write_text(body)
  cmds=[['g++','-shared','-fPIC','-std=c++20','-Wall','-Wextra','-Werror','-I'+str(h.JAVA.parent/'include'),'-I'+str(h.JAVA.parent/'include/linux'),HERE/'native.cpp','-o',w/'libHyperCeilerAmbientOS4.so'],[h.JAVA/'javac','--release','21','-proc:none','-d',classes,*sorted((HERE/'fixtures').rglob('*.java'))],[h.JAVA/'java','-cp',h.KH/'lib/kotlin-compiler.jar','org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',h.KH,'-jvm-target','21','-classpath',classes,'-d',classes,h.SOURCE,src,HERE/'fixtures/BridgeReloadMain.kt']]
  for i,cmd in enumerate(cmds):
   commands.append(list(map(str,cmd)));r=subprocess.run(commands[-1],capture_output=True,text=True,timeout=90);(case/f'build-{i}.txt').write_text(r.stdout+r.stderr);assert r.returncode==0,r.stdout+r.stderr
  modes=[]
  for mode in ['loaded','missing']:
   cmd=[h.JAVA/'java','-ea','-Djava.library.path='+str(w if mode=='loaded'else w/'missing'),'-cp',os.pathsep.join(map(str,[classes,h.KH/'lib/kotlin-stdlib.jar'])),'BridgeReloadMainKt',mode];commands.append(list(map(str,cmd)));r=subprocess.run(commands[-1],capture_output=True,text=True,timeout=90);(case/(mode+'.txt')).write_text(r.stdout+r.stderr);m=re.search(r'PASS reload checks=(\d+)',r.stdout);modes.append({'mode':mode,'exit_code':r.returncode,'passed':r.returncode==0 and m is not None,'assertions':int(m[1])if m else 0})
  rows.append({'name':label,'passed':all(x['passed']for x in modes)if change is None else any(x['exit_code']!=0 for x in modes),'modes':modes,'commands':commands,'meaning':'positive'if change is None else'compiled mutation rejected'})
d={'passed':all(x['passed']for x in rows)and before==h.inputs(),'checks':rows,'inputs_before':before,'inputs_after':h.inputs(),'runner':{'path':str(Path(__file__).relative_to(h.ROOT)),'sha256':h.sha(Path(__file__))},'scope':'Actual Kotlin resolver+bridge, fresh JVM per mode, actual host JNI recorder only. Actual static synchronized Class monitor across pause/resume/ensureLoaded; controlled worker blocked before loading, paused admission, failed reload resumption, and loaded-but-native-unavailable refusal. Vendor private ELF isolation is independently tested by native owner.','device_status':'NOT_TESTED'};(out/'report.json').write_text(json.dumps(d,indent=2)+'\n');print('passed',d['passed']);raise SystemExit(0 if d['passed']else 1)
