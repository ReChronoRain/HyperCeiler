"""Pinned launcher7751 offline regression. Does not operate a device or modify an APK."""
from pathlib import Path
import argparse,sys,subprocess,zipfile,lzma,json,hashlib,re
W=Path(__file__).resolve().parents[2];T=W/'handover/launcher-7751-20261017';T.mkdir(parents=True,exist_ok=True)
a=argparse.ArgumentParser();a.add_argument('--root',default=str(W));a.add_argument('--apk',default=str(T/'launcher-7751.apk'));args=a.parse_args();R=Path(args.root).resolve();A=Path(args.apk).resolve()
assert hashlib.sha256(A.read_bytes()).hexdigest()=='f4fe410efcd72ae30550bf6beac9a991d06d1ce82f0d51b51f644b11658e7cc8','fixture APK hash differs'
with zipfile.ZipFile(A) as z:
 for entry,name in [('lib/arm64-v8a/libapp.so','launcher-7751-libapp.so'),('lib/arm64-v8a/libapp_launcher.so','libapp_launcher.so')]:
  dest=T/name;data=z.read(entry)
  if not dest.exists() or dest.read_bytes()!=data:dest.write_bytes(data)
sys.path.insert(0,str(W/'tests/home-layout-native'));from dart_dump import Elf
elf=Elf(T/'launcher-7751-libapp.so');sec=elf.sections[elf.section_names['.gnu_debugdata']];(T/'launcher-7751-mini.elf').write_bytes(lzma.decompress(elf.data[sec[4]:sec[4]+sec[5]]))
out=T/('regression-'+R.name);out.mkdir(exist_ok=True);rows=[]
def run(name,c):
 p=subprocess.run(c,capture_output=True,text=True,encoding='utf8',errors='replace');row=dict(case=name,command=c,input_root=str(R),stdout=p.stdout,stderr=p.stderr,exit=p.returncode);rows.append(row);(out/(name+'.log')).write_text(p.stdout+p.stderr);print(name+' EXIT='+str(p.returncode)+' '+(p.stdout.strip().splitlines()[-1] if p.stdout.strip() else p.stderr[-300:]),flush=True)
 return p.returncode
old="Path('C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f')"
# Actual production SymbolIndex and CodeView; decoder/cache/file/read boundaries supplied by existing host fixture.
src=W/'tests/os4-audit-symbol-loader-20261005/verify.py';s=src.read_text();s=s[:s.index("(run/'test.cpp').write_text")].replace(old,'Path('+repr(str(T))+')').replace('launcher-7722-libapp.so','launcher-7751-libapp.so').replace('launcher-7722-mini.elf','launcher-7751-mini.elf')
argv=sys.argv;sys.argv=[str(src),str(R)];env={'__file__':str(src),'__name__':'fixture_build'}
try:exec(compile(s,str(src),'exec'),env)
finally:sys.argv=argv
pre=env['pre'];pre=pre[:pre.index('int main(int argc,char**argv)')];H=R/'app/src/main/cpp/targets/home/tweaks'
def strip(p):return '\n'.join(x for x in p.read_text().splitlines() if not x.startswith(('#include','#pragma')))
sig=(H/'signatures.h').read_text();sig=sig[sig.index('struct FunctionSignature'):];sig=sig[:sig.index('};')+2];pre=pre.replace('struct FunctionSignature {};',sig)
pre+='\n#include "a64.h"\n#include <utility>\n#include <inttypes.h>\n'+strip(H/'blob.h')+'\n'+strip(H/'ht_plan.h')
pre+='\nnamespace hometweaks {bool CodeView::Word(uint32_t va,uint32_t*out)const{return ReadWords(va,out,1);}bool CodeView::InText(uint32_t va)const{return RangeOk(va,4);}bool CodeView::FindSignature(const FunctionSignature&,uint32_t*,int*)const{return false;}bool CodeView::MatchSignatureAt(const FunctionSignature&,uint32_t)const{return false;}const FunctionSignature* FindSignatureByName(const char*){return nullptr;}bool ReadWholeFile(const char*,std::vector<uint8_t>*,uint64_t*){return false;}}\n'
pre+=strip(H/'ht_plan.cpp')
pre+='int main(int argc,char**argv){if(argc!=4)return 2;outerFile=read(argv[1]);decoded=read(argv[2]);auto full=read(argv[1]);\nusing namespace hometweaks;Elf64_Ehdr h{};memcpy(&h,full.data(),64);uint64_t end=0;\nfor(unsigned i=0;i<h.e_phnum;i++){Elf64_Phdr p{};memcpy(&p,full.data()+h.e_phoff+i*sizeof p,sizeof p);if(p.p_type==1)end=std::max(end,p.p_vaddr+p.p_memsz);}\nmapped.resize(end+4096);readable.push_back({uintptr_t(mapped.data()),mapped.size()});Image image{};image.base=uintptr_t(mapped.data());strcpy(image.path,"fixture.so");\nfor(unsigned i=0;i<h.e_phnum;i++){Elf64_Phdr p{};memcpy(&p,full.data()+h.e_phoff+i*sizeof p,sizeof p);if(p.p_type!=1)continue;memcpy(mapped.data()+p.p_vaddr,full.data()+p.p_offset,p.p_filesz);image.segments[image.segmentCount++]={image.base+p.p_vaddr,image.base+p.p_vaddr+p.p_memsz,p.p_flags};}\nauto& index=SymbolIndex::Instance();if(!index.EnsureLoaded(image)){std::cout<<index.status()<<"\\n";return 1;}SetForceSymbolOnlyForTest(true);CodeView code(image);\nfor(size_t i=0;i<TargetFunctionCount();i++){uint32_t v=0,z=0,span=0;const auto& t=TargetFunctionAt(i);if(index.Find(t.needle,&v,&z)){index.SpanFrom(v,&span);std::cout<<"ROOT\\t"<<t.needle<<"\\t"<<v<<"\\t"<<z<<"\\t"<<span<<"\\n";}else std::cout<<"MISSING_ROOT\\t"<<t.needle<<"\\n";}\nfor(uint32_t f:{4,9,16,18,19,20,21}){Config c;c.flags=1;c.enabled={f};c.folderCols=5;c.phoneCols=6;c.phoneRows=8;c.padMajor=8;c.padMinor=5;c.foldMajor=8;c.foldMinor=5;c.iconScaleCode=0x70;LocatedSites sites;LocateSites(code,c,&sites);PlanResult result;DerivePatches(code,c,sites,&result);if(!FeatureOk(sites,f)||result.patches.empty()){std::cout<<"ADMISSION_FAIL="<<f<<"\\n";return 1;}Config off=c;off.flags=0;PlanResult disabled;DerivePatches(code,off,sites,&disabled);if(!disabled.patches.empty())return 1;std::cout<<"FEATURE="<<f<<" LOCATED="<<FeatureOk(sites,f)<<" PATCHES="<<result.patches.size()<<" ROWS="<<result.ok19r<<"\\n";std::cout<<"WHY="<<(f==4?result.why4:f==9?result.why9:f==16?result.why16:f==18?result.why18:f==19?result.why19:f==20?result.why20:result.why21)<<"\\n";for(auto p:result.patches){uint32_t w=0;if(!code.Word(p.va,&w)||w!=p.expect){std::cout<<"PREIMAGE_FAIL\\n";return 1;}std::cout<<"PATCH\\t"<<p.va<<"\\t"<<p.expect<<"\\t"<<p.patch<<"\\t"<<p.what<<"\\n";}}\nstd::cout<<"INDEX="<<index.status()<<"\\n";}\n'
(out/'plan.cpp').write_text(pre);zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe'
if run('plan-build',[str(zig),'c++','-std=c++20','-O0','-I'+str(H),'-I'+str(R/'app/src/main/cpp'),str(out/'plan.cpp'),'-o',str(out/'plan.exe')]):raise SystemExit(1)
run('plan7751',[str(out/'plan.exe'),str(T/'launcher-7751-libapp.so'),str(T/'launcher-7751-mini.elf'),'7751'])
if (R/'app/src/main/cpp/targets/home/home_icon_render.h').exists():
 for root in ['ShortcutIconWidget._buildNormalDrawable','ShortcutIconWidget.iconWidth','ShortcutIconWidget.iconHeight']:
  assert 'ROOT\t'+root+'\t' in rows[-1]['stdout'], 'production registry missing '+root

(out/'rust.cpp').write_text('#include "targets/home/home_layout_elf_targets.h"\n#include <fstream>\n#include <iostream>\nint main(int argc,char**argv){std::ifstream f(argv[1],std::ios::binary);std::vector<char>b((std::istreambuf_iterator<char>(f)),{});std::span<const std::byte> raw{reinterpret_cast<const std::byte*>(b.data()),b.size()};auto t=home_layout::elf_targets::resolve(raw);std::cout<<"RUST_LAYOUT="<<bool(t)<<"\\n";if(t)std::cout<<std::hex<<t->grid_config_initializer<<" "<<t->grid_height<<" "<<t->cell_count_x<<" "<<t->cell_count_y<<" "<<t->resolve_dimen_px<<"\\n";std::cout<<"ANIMATION="<<std::hex<<home_layout::elf_targets::resolve_animation_duration_update(raw).value_or(0)<<"\\n";return t?0:1;}\n')
if run('rust-build',[str(zig),'c++','-std=c++20','-I'+str(R/'app/src/main/cpp'),str(out/'rust.cpp'),'-o',str(out/'rust.exe')]):raise SystemExit(1)
run('rust7751',[str(out/'rust.exe'),str(T/'libapp_launcher.so')])
for name,script in [('full-dynamic','tests/full-dynamic-os4/verify.py'),('folder-title','tests/folder-title-os4/verify.py'),('auto-close','tests/folder-auto-close-scroll-os4/verify.py'),('workspace-double','tests/home-workspace-gesture/verify.py'),('workspace-swipe','tests/home-workspace-swipe/verify.py'),('back-area','tests/back-gesture-os4/verify.py')]:
 p=W/script;s=p.read_text();replacement="[('7751',Path("+repr(str(T))+"))]";lines=s.splitlines(True);changed=False
 for i,line in enumerate(lines):
  if "('7722',Path(" in line and "('8244'," in line:
   start=line.index('[');end=line.index(']',start)+1;lines[i]=line[:start]+replacement+line[end:];changed=True
 if name=='back-area':s=s.replace("str(D/'libapp_launcher.so')",'str(Path('+repr(str(T/'libapp_launcher.so'))+'))');changed=True
 else:s=''.join(lines)
 assert changed,name
 if name=='workspace-swipe':s=s.replace("str(W/'tests/back-gesture-os4/libapp_launcher.so'),str(W/'handover/gestures-os4-20261014/libapp_launcher.so')",'str(Path('+repr(str(T/'libapp_launcher.so'))+'))')
 if name=='full-dynamic':
  needle=" pre+='resolve_grid_fields(";assert needle in s;s=s.replace(needle,' # Cover the actual margin/indicator and drop-back gate, not just field decoders.\n pre=\'#include "home_drop_geometry.h"\\n\'+pre\n ws=env[\'sym\'].by_name[\'Workspace._createIndicator\'][0];sh=env[\'sym\'].by_name[\'LauncherIndicatorState._showIndicator\'][0];drop=env[\'sym\'].by_name[\'CellLayoutGetxController.calculateCenterGlobalPosition\'][0]\n pre+=f\'{{uint32_t patch=0,off=0,caller=0;ok(capsule_wrapper_layout_compatible({ws},0,&patch));ok(patch>={ws}&&patch+16<={ws}+spans[{ws}]);ok(indicator_slide_compatible({sh},&off,&caller));uint32_t pos=0;for(const auto& site:home_layout::kDropGeometrySites)ok(dartscan::unique_sequence(images[{drop}],site.words,4,&pos));}}\\n\'\n'+needle)
 f=out/(name+'-wrapper.py');f.write_text(f'__file__={str(p)!r};__name__="__main__";exec(compile({s!r},__file__,"exec"))');run(name,['python',str(f),str(R)])
run('mapped-container',['python',str(W/'tests/launcher-7751/verify_container.py'),str(R)])
(out/'results.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2));failed=sum(r['exit']!=0 for r in rows);print('LAUNCHER7751_OFFLINE cases='+str(len(rows))+' failed='+str(failed)+'; static contracts only, no device acceptance');raise SystemExit(bool(failed))
