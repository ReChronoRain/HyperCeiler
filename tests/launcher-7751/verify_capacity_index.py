"""Run the real production SymbolIndex, including duplicate-name accounting."""
from pathlib import Path
import sys, subprocess, json
W=Path(__file__).resolve().parents[2]
R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
T=W/'handover/folder-icons-hotseat-20261018'
out=T/('index-'+R.name);out.mkdir(parents=True,exist_ok=True)
audit=W/'tests/os4-audit-symbol-loader-20261005/verify.py'
source=audit.read_text(encoding='utf8');source=source[:source.index("(run/'test.cpp').write_text")]
old=sys.argv;sys.argv=[str(audit),str(R)];env={'__file__':str(audit),'__name__':'fixture_builder'}
try:exec(compile(source,str(audit),'exec'),env)
finally:sys.argv=old
code=env['pre'];code=code[:code.index('int main(int argc,char**argv)')]
code+=r'''
int main(int argc,char**argv){if(argc!=3)return 2;
outerFile=read(argv[1]);decoded=read(argv[2]);Elf64_Ehdr h{};memcpy(&h,outerFile.data(),64);uint64_t end=0;
for(unsigned i=0;i<h.e_phnum;++i){Elf64_Phdr p{};memcpy(&p,outerFile.data()+h.e_phoff+i*sizeof p,sizeof p);if(p.p_type==1)end=std::max(end,p.p_vaddr+p.p_memsz);}
mapped.resize(end+4096);readable.push_back({uintptr_t(mapped.data()),mapped.size()});hometweaks::Image image{};image.base=uintptr_t(mapped.data());strcpy(image.path,"capacity-fixture.so");
for(unsigned i=0;i<h.e_phnum;++i){Elf64_Phdr p{};memcpy(&p,outerFile.data()+h.e_phoff+i*sizeof p,sizeof p);if(p.p_type==1){memcpy(mapped.data()+p.p_vaddr,outerFile.data()+p.p_offset,p.p_filesz);image.segments[image.segmentCount++]={image.base+p.p_vaddr,image.base+p.p_vaddr+p.p_memsz,p.p_flags};}}
auto& index=hometweaks::SymbolIndex::Instance();if(!index.EnsureLoaded(image)){std::cout<<"INDEX_ERROR="<<index.status()<<"\n";return 3;}
unsigned failed=0;for(const char* name:{"HotseatLayerGetxController.calculatePositionX","HotSeatsConstants2.hotseatMaxFixedCount","ProgressShortcutIconGetxController.onInit"}){
uint32_t va=0,size=0;if(!index.Find(name,&va,&size)){++failed;std::cout<<"MISSING="<<name<<"\n";}else std::cout<<"SELECTED="<<name<<"/"<<va<<"/"<<size<<"\n";}
std::cout<<"CAPACITY_INDEX checks=3 failed="<<failed<<"\n";return failed?1:0;}
'''
(out/'index.cpp').write_text(code,encoding='utf8')
zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe'
fixtures=[W/'handover/launcher-7751-20261017/launcher-7751-libapp.so',Path('C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f/launcher-7722-libapp.so'),W/'handover/folder-title-os4-20261012/launcher-8244-libapp.so']
cmds=[[zig,'c++','-std=c++20','-I'+str(R/'app/src/main/cpp'),out/'index.cpp','-o',out/'index.exe']]
fixtures=[f if f.exists() else next((W/'handover').rglob(f.name)) for f in fixtures]
cmds += [[out/'index.exe',f,f.with_name(f.name.replace('-libapp.so','-mini.elf'))] for f in fixtures]
rows=[]
for cmd in cmds:
 p=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,encoding='utf8',errors='replace');rows.append(dict(command=list(map(str,cmd)),input='production SymbolIndex + original real ELF/mini-ELF',stdout=p.stdout,stderr=p.stderr,exit=p.returncode));print(p.stdout+p.stderr,end='')
 if p.returncode and cmd==cmds[0]:break
(out/'results.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
raise SystemExit(int(any(x['exit'] for x in rows)))
