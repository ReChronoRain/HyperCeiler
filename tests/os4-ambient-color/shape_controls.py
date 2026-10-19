#!/usr/bin/env python3
"""Isolated class loaders via fresh JVM processes test exact selected dependency shape rejection."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess,tempfile
HERE=Path(__file__).resolve().parent
s=importlib.util.spec_from_file_location('ambient_host',HERE/'run.py');h=importlib.util.module_from_spec(s);s.loader.exec_module(h)
CASES={
 'converter_wrong_arity':('com/miui/utils/DrawableUtils.java','drawable2Bitmap(Drawable d,int w,int h)','drawable2Bitmap(Drawable d,int w)',True,False),
 'native_hct_not_native':('miuix/mipalette/MiPalette.java','public static native int getMainColorHCT(int[] input,int width,int height);','public static int getMainColorHCT(int[] input,int width,int height){return 0;}',True,False),
 'sampler_wrong_visibility':('miuix/mipalette/MiPalette.java','private static Bitmap getPixels(','public static Bitmap getPixels(',True,False),
 'accent_not_final':('com/android/systemui/monet/ColorScheme.java','public final TonalPalette mAccent1','public TonalPalette mAccent1',False,True),
 'shades_not_final':('com/android/systemui/monet/TonalPalette.java','public final List<?> allShades','public List<?> allShades',False,True),
 'artwork_not_final':('com/android/systemui/media/controls/shared/model/MediaData.java','public final Icon artwork','public Icon artwork',True,True),
}
# Every mutation changes an actual Java declaration; a fresh VM exercises production create().
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False);before=h.inputs();checks=[]
for name,(file,old,new,normalFails,optFails)in CASES.items():
 case=out/name;case.mkdir();commands=[]
 with tempfile.TemporaryDirectory(prefix='ambient-shape-')as tmp:
  w=Path(tmp);f=w/'fixtures';f.mkdir();classes=w/'classes';classes.mkdir()
  for original in (HERE/'fixtures').rglob('*.java'):
   target=f/original.relative_to(HERE/'fixtures');target.parent.mkdir(parents=True,exist_ok=True);body=original.read_text()
   if str(original.relative_to(HERE/'fixtures'))==file:
    assert body.count(old)==1,(name,old);body=body.replace(old,new)
    # Keep unrelated adapter locals well formed when removing an argument.
    if name=='converter_wrong_arity':body=body.replace('{calls++;','{int h=0;calls++;')
   target.write_text(body)
  main=w/'ShapeMain.kt';main.write_text('''import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3.AmbientColorResolverOS4 as R
fun main(){val c=R::class.java.classLoader;check((R.create(c,true,false)==null)==%s);check((R.create(c,false,true)==null)==%s);println("PASS exact shape and unselected mode")}
'''%(str(normalFails).lower(),str(optFails).lower()))
  cmds=[[h.JAVA/'javac','--release','21','-proc:none','-d',classes,*sorted(f.rglob('*.java'))],[h.JAVA/'java','-cp',h.KH/'lib/kotlin-compiler.jar','org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',h.KH,'-jvm-target','21','-classpath',classes,'-d',classes,h.SOURCE,h.NATIVE_SOURCE,main],[h.JAVA/'java','-ea','-cp',os.pathsep.join(map(str,[classes,h.KH/'lib/kotlin-stdlib.jar'])),'ShapeMainKt']]
  ok=True
  for i,cmd in enumerate(cmds):
   commands.append(list(map(str,cmd)));r=subprocess.run(commands[-1],capture_output=True,text=True,timeout=90);(case/f'{i}.txt').write_text(r.stdout+r.stderr);ok=ok and r.returncode==0
   if r.returncode:break
  checks.append({'name':name,'passed':ok,'commands':commands})
d={'passed':all(c['passed']for c in checks)and before==h.inputs(),'checks':checks,'inputs_before':before,'inputs_after':h.inputs(),'runner':{'path':str(Path(__file__).relative_to(h.ROOT)),'sha256':h.sha(Path(__file__))},'device_status':'NOT_TESTED','scope':'Whole production selected-dependency reflection against deliberately wrong JVM type/modifier declarations, not ART or native algorithm execution.'};(out/'report.json').write_text(json.dumps(d,indent=2)+'\n');print('passed',d['passed'],[c['name']for c in checks if not c['passed']]);raise SystemExit(0 if d['passed']else 1)
