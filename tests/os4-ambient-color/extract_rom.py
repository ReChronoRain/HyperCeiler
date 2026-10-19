#!/usr/bin/env python3
"""Read-only fresh OTA DEX/native ABI extraction; no proprietary code execution."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,re,struct,subprocess,sys,zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

sys.dont_write_bytecode=True
HERE=Path(__file__).resolve().parent;ROOT=test_paths.TEST_DATA_ROOT;CACHE=test_paths.BUILD_CACHE;APK=test_paths.ROM_ROOT / 'components/system_ext/priv-app/MiuiSystemUI/MiuiSystemUI.apk';NATIVE=test_paths.EXTRACTED_ROM_ROOT / 'system_ext/lib64/libMiMainColor.so';OBJDUMP=test_paths.ANDROID_NDK_HOME / 'toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objdump'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def module(name,path):
 s=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
DEX=module('ambient_dex',test_paths.TEST_DATA_ROOT / 'tools/dex_inventory.py');ELF=module('ambient_elf',test_paths.TEST_DATA_ROOT / 'tools/verify-test4-deliverables.py')
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
lib=test_paths.ANDROID_HOME / 'cmdline-tools/latest/lib/external';cp=':'.join(str(p)for pattern in ['**/smali-*.jar','**/guava-*.jar','**/failureaccess-*.jar','**/jcommander-*.jar']for p in lib.glob(pattern))
exact={'com.android.systemui.monet.ColorScheme','com.android.systemui.monet.TonalPalette','com.android.systemui.monet.Style','com.miui.utils.DrawableUtils','miuix.mipalette.MiPalette','com.android.systemui.media.controls.shared.model.MediaData','com.android.systemui.statusbar.notification.utils.NotificationUtil'}
found={};commands=[]
with zipfile.ZipFile(APK)as z:
 for n in z.namelist():
  if not re.fullmatch(r'classes(?:\d+)?\.dex',n):continue
  raw=z.read(n);classes=DEX.dex_classes(raw);selected=sorted(c for c in classes if c in exact or c.startswith(('com.android.systemui.monet.ColorScheme$', 'com.android.systemui.monet.TonalPalette$')))
  found.update({c:{'dex':n,'dex_sha256':hashlib.sha256(raw).hexdigest(),'members':classes[c]}for c in selected})
  if not selected:continue
  cmd=[str(test_paths.JAVA_HOME / 'bin/java'),'-cp',cp,'com.android.tools.smali.baksmali.Main','disassemble','--classes',','.join('L'+c.replace('.','/')+';'for c in selected),'-o',str(out/'smali'),str(APK)+'/'+n];r=subprocess.run(cmd,capture_output=True,text=True);(out/(n+'.log')).write_text(r.stdout+r.stderr);assert r.returncode==0;commands.append(cmd)
checks=[];methods=[]
def check(n,b,d=None):checks.append({'name':n,'passed':bool(b),'detail':d})
def cls(n):return(out/'smali'/Path(n.replace('.','/')+'.smali')).read_text()
def method(n,sig):
 s=cls(n);items=re.findall(r'^\.method ([^\n]+)\n.*?^\.end method',s,re.M|re.S);headers=[x for x in items if x.split()[-1]==sig];assert len(headers)==1,(n,sig,headers)
 h=headers[0];start=s.index('.method '+h+'\n');end=s.index('.end method',start)+len('.end method');body=s[start:end];methods.append({'owner':n,'signature':sig,'declaration':h,'start_line':s[:start].count('\n')+1,'end_line':s[:end].count('\n')+1,'complete_body_sha256':hashlib.sha256(body.encode()).hexdigest()});return body
D='com.miui.utils.DrawableUtils';P='miuix.mipalette.MiPalette';C='com.android.systemui.monet.ColorScheme';T='com.android.systemui.monet.TonalPalette';N='com.android.systemui.statusbar.notification.utils.NotificationUtil';M='com.android.systemui.media.controls.shared.model.MediaData'
d=method(D,'drawable2Bitmap(Landroid/graphics/drawable/Drawable;II)Landroid/graphics/Bitmap;');pc=method(P,'<clinit>()V');ph=method(P,'getMainColorHCT(Landroid/graphics/Bitmap;)I');pn=method(P,'getMainColorHCT([III)I');px=method(P,'getPixels([ILandroid/graphics/Bitmap;Ljava/util/List;)Landroid/graphics/Bitmap;');pi=method(P,'init(II)V');cc=method(C,'<init>(Landroid/app/WallpaperColors;ZI)V');cl=method(C,'<init>(Ljava/util/List;ZID)V');cs=method(C,'getSeedColors(Landroid/app/WallpaperColors;Z)Ljava/util/List;');tc=method(T,'<clinit>()V');tt=method(T,'<init>(Lcom/google/ux/material/libmonet/palettes/TonalPalette;)V');tg=method(T,'getS500()I');ni=method(N,'acquireApplicationIcon(Landroid/content/Context;Lcom/android/systemui/media/controls/shared/model/MediaData;)Landroid/graphics/drawable/Drawable;')
check('actual converter public static exact three parameters',d.startswith('.method public static drawable2Bitmap(') and all(x in d for x in ['->getBitmap()','->setBounds(IIII)','->tick(J)V','->getQuietDrawable()']))
check('old one-parameter converter does not exist','drawable2Bitmap(Landroid/graphics/drawable/Drawable;)Landroid/graphics/Bitmap;'not in cls(D))
check('bitmap HCT uses shared static pixels then original native array overload',all(x in ph for x in ['->pixels:[I','->getPixels([ILandroid/graphics/Bitmap;Ljava/util/List;)','->getMainColorHCT([III)I']))
check('native HCT public static native exact array,width,height,int result',pn.startswith('.method public static native getMainColorHCT([III)I'))
check('sampler private static; float quotient sqrt and ceil/filter false are original',px.startswith('.method private static ')and all(x in px for x in ['mul-int v1, v3, v7','div-float/2addr v2, v1','Math;->sqrt(D)D','Math;->ceil(D)D','const/4 v2, 0x0','createScaledBitmap(Landroid/graphics/Bitmap;IIZ)','if-eqz p2']))
check('only class initializer assigns MAX_AREA 12544 and allocates twice capacity',len(re.findall(r'^\s*sput[^\n]+->MAX_AREA:I',cls(P),re.M))==1 and all(x in pc for x in ['0x3100','->MAX_AREA:I','mul-int/lit8','new-array'])and '->MAX_AREA:I'not in pi)
check('native library actual clinit selection', '"MiMainColor"'in pc and 'System;->loadLibrary' in pc)
check('missing old enum class; actual integer style constructor', 'com.android.systemui.monet.Style'not in found and cc.startswith('.method public constructor <init>(Landroid/app/WallpaperColors;ZI)V')and 'const/4 v0, 0x6'in cc)
check('style six selects unfiltered seed path', 'if-eq p3, v0, :cond_5' in cc and ':cond_5\n    const/4 v0, 0x0'in cc and 'getSeedColors(Landroid/app/WallpaperColors;Z)'in cc)
switch=re.search(r'\.packed-switch 0x0\n(.*?)\.end packed-switch',cl,re.S);targets=re.findall(r':\w+',switch[1]);content=cl[cl.index('    '+targets[6]+'\n'):];content=content[:content.index('    :pswitch_',10)] if '    :pswitch_'in content[10:]else content
check('integer style six reaches SchemeContent and CONTENT variant',len(targets)>=7 and all(x in content for x in ['scheme/SchemeContent;','Variant;->CONTENT','DynamicScheme;-><init>']))
check('common constructor publishes mAccent1 from primary palette',all(x in cl for x in ['DynamicScheme;->primaryPalette','TonalPalette;-><init>','ColorScheme;->mAccent1:']))
check('accent and thirteen shades precise field shapes','.field public final mAccent1:Lcom/android/systemui/monet/TonalPalette;'in cls(C)and '.field public final allShades:Ljava/util/List;'in cls(T)and 'filled-new-array/range {v1 .. v13}, [Ljava/lang/Integer;'in tc)
tl=method(T+'$$ExternalSyntheticLambda0','apply(Ljava/lang/Object;)Ljava/lang/Object;')
check('shade map computes material tone from key, no list filtering',all(x in tl for x in ['1000.0f','10.0f','sub-float/2addr','div-float/2addr','->tone(I)I','Integer;->valueOf(I)']))
check('palette list builds all shades in key order and s500 index7',all(x in tt for x in ['->SHADE_KEYS:','Stream;->map','->allShades:'])and 'const/4 v0, 0x7'in tg)
check('cross-user app fallback retains original package/user and exception drawable',all(x in ni for x in ['MediaData;->userId:I','UserHandle;-><init>(I)V','createContextAsUser','MediaData;->packageName:','getApplicationIcon','0x7f0810eb','.catch Ljava/lang/Exception;']))
check('media artwork field exact type immutable','.field public final artwork:Landroid/graphics/drawable/Icon;'in cls(M))
# Exact native JNI registration ABI. Parse symbol-indexed ABS64 relocation as well as RELATIVE.
e=ELF.Elf(NATIVE.read_bytes());rel={}
for sec in e.sections:
 if sec[1]!=4:continue
 symtab=e.sections[sec[6]];strings=e.content(e.sections[symtab[6]])
 for pos in range(sec[4],sec[4]+sec[5],sec[9]):
  va,info,add=struct.unpack_from('<QQq',e.data,pos);typ=info&0xffffffff;idx=info>>32
  if typ==1027:rel[va]=add
  elif typ==257:
   at=symtab[4]+idx*symtab[9];_,_,_,_,sv,_=struct.unpack_from('<IBBHQQ',e.data,at);rel[va]=sv+add
readptr=lambda va:rel.get(va,struct.unpack('<Q',e.at(va,8))[0]);cstring=lambda va:e.at(va,256).split(b'\0',1)[0].decode()
rows=[{'slot':0x580a0+i*24,'name':cstring(readptr(0x580a0+i*24)),'signature':cstring(readptr(0x580a8+i*24)),'function':readptr(0x580b0+i*24)}for i in range(18)]
check('native class string exact MiPalette',cstring(0x46538)=='miuix/mipalette/MiPalette')
hct=[r for r in rows if r['name']=='getMainColorHCT'];check('registered HCT exact ([III)I static JNI thunk address',hct==[{'slot':0x58118,'name':'getMainColorHCT','signature':'([III)I','function':0x25ce8}])
funcs={}
for name in ['JNI_OnLoad','_Z15getMainColorHCTP7_JNIEnvP7_jclassP10_jintArrayii','_ZN9MainColor15getMainColorHCTEPiii','_ZN19MainColorSingleRect15getMainColorHCTEPiiiS0_']:
 sym=e.symbols[name];cmd=[str(OBJDUMP),'-d',f'--start-address={sym["va"]}',f'--stop-address={sym["va"]+sym["size"]}',str(NATIVE)];text=subprocess.check_output(cmd,text=True);file=out/(name+'.asm.txt');file.write_text(text);funcs[name]={'va':sym['va'],'size':sym['size'],'complete_bytes_sha256':hashlib.sha256(e.at(sym['va'],sym['size'])).hexdigest(),'assembly_path':str(test_paths.report_path(file, ROOT)),'assembly_sha256':sha(file)}
check('full static HCT JNI ABI width x3 height x4 array x2',e.symbols['_Z15getMainColorHCTP7_JNIEnvP7_jclassP10_jintArrayii']['size']==140)
limits=['Selected complete DEX method bodies and the style-6 branch/common publication are reviewed; other ColorScheme styles and all libmonet mathematical internals are not claimed exhaustively audited.','Private int[] removes Java static-pixels aliasing. Actual native thunk still calls a shared MainColor instance and writes instance state; native internal concurrency with OEM callers is not proven isolated.','JNI recorder host tests verify typed dispatch/arguments, not the proprietary HCT algorithm or Android graphics renderer.','MAML quiet/static source differs from animated tick. Null/unavailable is a retained-OEM fallback, never a successful color calculation.']
r={'passed':all(c['passed']for c in checks),'checks':checks,'check_count':len(checks),'apk':{'path':str(test_paths.report_path(APK, ROOT)),'sha256':sha(APK)},'native':{'path':str(NATIVE),'sha256':sha(NATIVE),'registration':rows,'functions':funcs},'found':found,'complete_methods':methods,'full_smali':{str(test_paths.report_path(f, ROOT)):sha(f)for f in sorted((out/'smali').rglob('*.smali'))},'commands':commands,'runner':{'path':str(test_paths.report_path(Path(__file__), ROOT)),'sha256':sha(Path(__file__))},'limits':limits,'device_status':'NOT_TESTED'};(out/'report.json').write_text(json.dumps(r,indent=2)+'\n');print({'passed':r['passed'],'checks':len(checks),'failed':[c['name']for c in checks if not c['passed']]});raise SystemExit(0 if r['passed']else 1)
