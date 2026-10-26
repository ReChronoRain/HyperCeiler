"""7751 real APK container attribution. POSIX/linker/maps/live reads are controlled host boundaries."""
from pathlib import Path
import sys,subprocess,struct,zipfile,shutil,json,hashlib,re
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
T=W/'handover/launcher-7751-20261017/container-compat';out=T/('host-'+R.name);out.mkdir(parents=True,exist_ok=True)
A=W/'handover/launcher-7751-20261017/launcher-7751.apk'
assert hashlib.sha256(A.read_bytes()).hexdigest()=='f4fe410efcd72ae30550bf6beac9a991d06d1ce82f0d51b51f644b11658e7cc8'
# Reuse established platform mocks and negative IO corpus, not a second imitation of the image parser.
f=W/'tests/os4-audit-image-discovery-20261005/verify.py';s=f.read_text();s=s[:s.index("(run/'test.cpp').write_text")]
s=s.replace("run=D/('run-'+R.name)","run=Path("+repr(str(out))+')')
s=re.sub(r"actual=Path\('[^']+'\)",lambda m:"actual=Path("+repr(str(A))+')',s)
argv=sys.argv;sys.argv=[str(f),str(R)];env={'__file__':str(f),'__name__':'fixture_build'}
try:exec(compile(s,str(f),'exec'),env)
finally:sys.argv=argv
pre,header,code,main=[env[n] for n in ['pre','header','code','main']]
neutral=out/'system_partition'/'OfficialHome.apk';neutral.parent.mkdir(exist_ok=True);shutil.copy2(A,neutral)
second=out/'alternate_partition'/'OtherHome.apk';second.parent.mkdir(exist_ok=True);shutil.copy2(A,second)
def lp(p):return str(p.resolve()).replace('\\','/')[2:]
def origin(name):
 with zipfile.ZipFile(A) as z:i=z.getinfo('lib/arm64-v8a/'+name)
 with A.open('rb') as f:f.seek(i.header_offset+26);a,b=struct.unpack('<HH',f.read(4))
 return i.header_offset+30+a+b
fresh='FindImageFromMappedApks' in code
# Compile the actual layout container selector as well. An uninitialized HomeLayoutEngine
# discovery result models the very early manual-loader case, not a fabricated native owner.
hooks=(R/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text()
a=hooks.index('std::optional<std::string> launcher_apk(');b=hooks.index('\nbool ensure_dart_library()',a)
selector=hooks[a:b]
pre+='#include <optional>\n'
code+='\nnamespace home_layout_engine {bool HomeLayoutEngineTargetImage(char*,size_t){return false;}}\nusing home_layout_engine::makedev;\n'+selector
code+='\nbool discover(const char*lib,home_layout_engine::Image*out){return home_layout_engine::'+('FindImageFromMappedApks(lib,out);}' if fresh else 'FindImageFromApkMaps("/com.miui.home-",lib,out);}')
extra=r'''
Fixture neutral={"neutral","",NEUTRAL,DART_ORIGIN,4096,0};
Fixture other={"other","",SECOND,DART_ORIGIN,4096,0};
for(long pg:{4096,16384}) {
 neutral.page=pg;load(neutral,true);linker.clear();out.base=0x55;
 ok(discover("libapp.so",&out)&&out.base==bias&&std::string(out.path)==neutral.apk,"7751 neutral/system APK with no linker registration");
 std::istringstream maps(mapsText);auto selected=launcher_apk(nhk::parse_file_mappings(maps));
 ok(selected&&*selected==neutral.apk,"layout chooses actual mapped 7751 container, not package directory guess");
 ok(out.base==bias&&out.apkEntryOffset==neutral.origin,"actual source entry offset retained across page size");
}
neutral.page=4096;
// Rust can be mapped before Dart. Early geometry setup must use the same ownership proof.
Fixture rust=neutral;rust.origin=RUST_ORIGIN;load(rust,true);linker.clear();out.base=0x55;
ok(discover("libapp_launcher.so",&out)&&out.base==bias,"Rust-only early manual loader is discoverable");
{std::istringstream maps(mapsText);auto selected=launcher_apk(nhk::parse_file_mappings(maps));ok(selected&&*selected==neutral.apk,"layout pre-init accepts proven Rust owner before Dart mapping");}
// Pair the actual 7751 Dart/Rust mappings without destroying either mapped allocation.
held.clear();load(neutral,true);firstMaps=mapsText;firstReadable=readable;held.push_back(std::move(arena));
load(rust,true);mapsText=firstMaps+mapsText;readable.insert(readable.end(),firstReadable.begin(),firstReadable.end());linker.clear();
{std::istringstream maps(mapsText);auto selected=launcher_apk(nhk::parse_file_mappings(maps));ok(selected&&*selected==neutral.apk,"same-container Dart and Rust agree");}
// Two valid mapped containers never publish the first one.
held.clear();load(neutral,true);firstMaps=mapsText;firstReadable=readable;held.push_back(std::move(arena));
load(other,true);mapsText=firstMaps+mapsText;readable.insert(readable.end(),firstReadable.begin(),firstReadable.end());linker.clear();out.base=0x55;
ok(!discover("libapp.so",&out)&&out.base==0x55,"two real 7751 AOT owners remain ambiguous");
// A different Rust owner must not be paired with the selected Dart container.
held.clear();load(neutral,true);firstMaps=mapsText;firstReadable=readable;held.push_back(std::move(arena));
rust.apk=other.apk;load(rust,true);mapsText=firstMaps+mapsText;readable.insert(readable.end(),firstReadable.begin(),firstReadable.end());linker.clear();
{std::istringstream maps(mapsText);ok(!launcher_apk(nhk::parse_file_mappings(maps)),"mixed Dart/Rust APK generations rejected");}
load(neutral,true);linker.clear();safeFault=true;out.base=0x55;ok(!discover("libapp.so",&out)&&out.base==0x55,"no guessed image after unreadable live header");
load(neutral,true);linker.clear();wrongInode=true;ok(!discover("libapp.so",&out),"changed source inode still rejected");
load(neutral,true);linker.clear();wrongDevice=true;ok(!discover("libapp.so",&out),"changed source device still rejected");
load(neutral,true);linker.clear();mapsText.replace(mapsText.find("r-xp"),4,"rwxp");ok(!discover("libapp.so",&out),"RWX never becomes a candidate");
load(neutral,true);linker.clear();mapsText.replace(mapsText.find("r-xp"),4,"r--p");ok(!discover("libapp.so",&out),"non-executable APK mapping is not an owner");
load(neutral,true);linker.clear();for(size_t at=0;(at=mapsText.find(neutral.apk+"\n",at))!=std::string::npos;){mapsText.insert(at+neutral.apk.size()," (deleted)");at+=neutral.apk.size()+11;}ok(!discover("libapp.so",&out),"deleted APK cannot read replacement pathname");
load(neutral,true);linker.clear();
{std::istringstream maps(mapsText);auto all=nhk::parse_file_mappings(maps);for(auto& m:all)if(m.executable)++m.device_major;ok(!launcher_apk(all),"caller's stale device inventory rejected");}
load(neutral,true);linker.clear();
{std::istringstream maps(mapsText);auto all=nhk::parse_file_mappings(maps);for(auto& m:all)if(m.executable)++m.inode;ok(!launcher_apk(all),"caller's stale inode inventory rejected");}
ok(handles.empty(),"new discovery closes all descriptors");
'''
for n,v in [('NEUTRAL',json.dumps(lp(neutral))),('SECOND',json.dumps(lp(second))),('DART_ORIGIN',str(origin('libapp.so'))),('RUST_ORIGIN',str(origin('libapp_launcher.so')))]:extra=extra.replace(n,v)
main=main.replace('Image bad{};',extra+'\nImage bad{};') if 'Image bad{};' in main else main.replace('Image bad{};bad.segments',extra+'\nImage bad{};bad.segments')
assert 'neutral/system APK' in main
main=main.replace('IMAGE_DISCOVERY=','LAUNCHER7751_CONTAINER=')
# Run source call-edge checks rather than asserting a new helper merely exists.
entry=(R/'app/src/main/cpp/targets/home/tweaks/entry.cpp').read_text();edges=entry.count('FindImageFromMappedApks(kTargetLibName, &image)')>=2
main=main.replace('Image out{};out.base=0x55;', 'ok('+str(edges).lower()+',"worker and pre-layout priming both use path-independent discovery");Image out{};out.base=0x55;')
(out/'test.cpp').write_text(pre+'\n'+header+'\n'+code+'\n'+main)
zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe';rows=[]
for cmd in [[str(zig),'c++','-std=c++20','-O0','-I'+str(R/'app/src/main/cpp'),str(out/'test.cpp'),'-o',str(out/'test.exe')],[str(out/'test.exe')]]:
 p=subprocess.run(cmd,capture_output=True,text=True,encoding='utf8',errors='replace');print(p.stdout+p.stderr,end='');rows.append(dict(command=cmd,input_root=str(R),apk_sha256=hashlib.sha256(A.read_bytes()).hexdigest(),stdout=p.stdout,stderr=p.stderr,exit=p.returncode))
 if p.returncode:break
(out/'results.json').write_text(json.dumps(rows,indent=2));raise SystemExit(rows[-1]['exit'])
