"""Follow real typed lifecycle chains into the progress-controller mixin copies."""
from pathlib import Path
import sys, struct, subprocess, json
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
T=W/'handover/folder-icons-hotseat-20261018';out=T/('progress-'+R.name);out.mkdir(parents=True,exist_ok=True)
if 'inline bool icon_init_dimensions(' not in (R/'app/src/main/cpp/targets/home/home_icon_render.h').read_text(encoding='utf8'):
 print('PROGRESS_ICON_CONSUMER_MISSING');raise SystemExit(1)
sys.path.insert(0,str(W/'tests/home-layout-native'));from dart_dump import Elf,Symbols
fixtures=[('7751',W/'handover/launcher-7751-20261017/launcher-7751-libapp.so'),('7722',Path('C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f/launcher-7722-libapp.so')),('8244',next((W/'handover').rglob('launcher-8244-libapp.so')))]
code='#include "targets/home/home_icon_render.h"\n#include <map>\n#include <iostream>\nint main(){unsigned checks=0,failed=0;auto ok=[&](bool v){++checks;if(!v){++failed;std::cout<<"FAIL="<<checks<<"\\n";}};\n'
for version,path in fixtures:
 e=Elf(path);s=Symbols(path.with_name(path.name.replace('-libapp.so','-mini.elf')));root=s.resolve('ProgressShortcutIconGetxController.onInit')[0];rx=s.resolve('RxObjectMixin.value')[0];current=s.resolve('GridController.currentConfig')[0];app,asz,_=s.resolve('AppIcon.build');images={}
 def body(va):
  b=list(struct.unpack('<'+'I'*1024,e.read(va,4096)));return b[:b.index(0xd65f03c0)+1]
 def target(w,pc):
  if w&0xfc000000!=0x94000000:return None
  imm=w&0x3ffffff;imm=imm-(1<<26) if imm&(1<<25) else imm;return pc+imm*4
 cursor=root
 for depth in range(4):
  b=body(cursor);images[cursor]=b;first=None;hits=0
  for i,w in enumerate(b[:-4]):
   to=target(w,cursor+i*4)
   if to is None:continue
   forward=b[i-1]==0xaa0003e1 or (b[i-1]&0xffe00fff==0xf80003a1 and b[i-2]==0xaa0103e0)
   if first is None and forward:first=to
   if b[i+1:i+4]==[0xa9460740,0x91004000,0xeb00003f] and b[i+4]&0xff00001f==0x54000009:
    images[to]=body(to);hits+=1
  if hits==2:break
  assert first;cursor=first
 assert hits==2,version
 code+='{std::map<uint32_t,home_dynamic::Body> images;'
 for va,b in images.items():code+=f'images[{va}]={{'+','.join(hex(w) for w in b)+'};'
 b=struct.unpack('<'+'I'*(asz//4),e.read(app,asz));code+='home_dynamic::Body app={'+','.join(hex(w) for w in b)+'};home_layout::IconRenderSites render;'
 code+=f'ok(home_layout::icon_render_sites(app,{app},{current},render));'
 code+='auto read=[&](uint32_t va,home_dynamic::Body& b){auto i=images.find(va);if(i==images.end())return false;b=i->second;return true;};home_layout::IconInitDimensions result;'
 expr=f'home_layout::icon_init_dimensions({root},read,{rx},render.grid,render.value,result)'
 code+=f'ok({expr});ok(result.width!=result.height&&result.ws.value+8==result.hs.value);'
 code+=f'ok(!home_layout::icon_init_dimensions({root},read,{rx+4},render.grid,render.value,result));'
 code+='auto saved=images;images[result.width][result.ws.normal/4]=0xd503201f;'
 code+=f'ok(!{expr});images=saved;'
 code+='images[result.height][result.hs.dock/4]=0xd503201f;'
 code+=f'ok(!{expr});images=saved;'
 code+=f'for(size_t i=2;i<images[{root}].size();++i)if((images[{root}][i]&0xfc000000u)==0x94000000u){{images[{root}][i]=0x94000000u|((uint32_t(-int32_t(i)))&0x03ffffffu);break;}}ok(!{expr});'
 code+=f'std::cout<<"PROGRESS_{version} checks="<<checks<<" failed="<<failed<<"\\n";}}\n'
code+='std::cout<<"PROGRESS_ICON checks="<<checks<<" failed="<<failed<<"\\n";return failed?1:0;}\n'
(out/'test.cpp').write_text(code,encoding='utf8');zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe';rows=[]
for cmd in [[zig,'c++','-std=c++20','-I'+str(R/'app/src/main/cpp'),out/'test.cpp','-o',out/'test.exe'],[out/'test.exe']]:
 p=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,encoding='utf8',errors='replace');rows.append(dict(command=list(map(str,cmd)),input='three real AOT init/callee chains and malformed/foreign/cyclic mutations',stdout=p.stdout,stderr=p.stderr,exit=p.returncode));print(p.stdout+p.stderr,end='')
 if p.returncode:break
(out/'results.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8');raise SystemExit(rows[-1]['exit'])
