from pathlib import Path
import sys,subprocess,struct,json
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W;D=W/'handover/desktop-columns-20261018';O=D/('consumer-'+R.name);O.mkdir(parents=True,exist_ok=True)
header=R/'app/src/main/cpp/targets/home/home_native_columns.h'
if 'resolve_dart_columns' not in header.read_text('utf8'):print('DART_COLUMNS_CONSUMER_MISSING');raise SystemExit(1)
sys.path.insert(0,str(W/'tests/home-layout-native'));from dart_dump import Elf,Symbols
fixtures=[('7751',W/'handover/launcher-7751-20261017','launcher-7751'),('7722',Path('C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f'),'launcher-7722'),('8244',W/'handover/folder-title-os4-20261012','launcher-8244')]
code='#include "targets/home/home_native_columns.h"\n#include <iostream>\nint main(){unsigned checks=0,failed=0;auto ck=[&](bool v){checks++;if(!v){failed++;std::cout<<"FAIL="<<checks<<"\\n";}};\n'
for name,folder,prefix in fixtures:
 elf=folder/(prefix+'-libapp.so');mini=folder/(prefix+'-mini.elf')
 if not elf.exists():elf=next((W/'handover').rglob(prefix+'-libapp.so'));mini=elf.with_name(prefix+'-mini.elf')
 e=Elf(elf);s=Symbols(mini);va,size,_=s.resolve('PhoneRowColHandler.handle');copy=s.resolve('ComputeContext.copyWith')[0];b=list(struct.unpack('<'+'I'*(size//4),e.read(va,size)))
 code+='{home_dynamic::Body b={'+','.join(hex(x) for x in b)+'};'
 code+=f'auto shape=home_layout::resolve_dart_columns(b,{va},{copy});ck(bool(shape));if(!shape)return 1;std::cout<<"IMAGE_{name} splice="<<std::hex<<{va} + shape->splice<<std::dec<<" field="<<shape->field<<" frame="<<shape->frame<<"\\n";'
 code+=f'auto saved=b;auto i=shape->splice/4;ck(shape->frame<0&&shape->field>0);for(int delta:{{-4,-3,-2,-1,0,1,2,3,4,5,6}}){{b=saved;b[i+delta]=0xd503201f;ck(!home_layout::resolve_dart_columns(b,{va},{copy}));}}b=saved;'
 code+=f'ck(!home_layout::resolve_dart_columns(b,{va},{copy+4}));auto oldSize=b.size();b.insert(b.end(),saved.begin()+i-4,saved.begin()+i+8);b[oldSize+10]=0x94000000u|((uint32_t({copy}-({va}+4*(oldSize+10)))/4)&0x3ffffffu);ck(!home_layout::resolve_dart_columns(b,{va},{copy}));'
 pv,pz,_=s.resolve('GridController._persistCellCountIfEligible');cur=s.resolve('GridController.currentConfig')[0];put=s.resolve('PreferenceUtils.putInt')[0];pb=struct.unpack('<'+'I'*(pz//4),e.read(pv,pz))
 code+='home_dynamic::Body persist={'+','.join(hex(x) for x in pb)+'};'
 code+=f'ck(home_layout::grid_persist_boundary(persist,{pv},{cur},{put}));ck(!home_layout::grid_persist_boundary(persist,{pv},{cur+4},{put}));ck(!home_layout::grid_persist_boundary(persist,{pv},{cur},{put+4}));persist[0]=0xd503201f;ck(!home_layout::grid_persist_boundary(persist,{pv},{cur},{put}));'
 sv,sz,_=s.resolve('LauncherLayoutType.saveAndUpdateLayoutType');sb=struct.unpack('<'+'I'*(sz//4),e.read(sv,sz))
 code+='home_dynamic::Body save={'+','.join(hex(x) for x in sb)+'};'
 code+=f'auto sf=home_layout::grid_layout_save_field(save,{sv},{put});ck(bool(sf));ck(sf&&*sf>0);ck(!home_layout::grid_layout_save_field(save,{sv},{put+4}));for(size_t j=0;j<save.size();++j){{auto keep=save[j];save[j]=0xd503201f;if(j<5||j>=save.size()-4)ck(!home_layout::grid_layout_save_field(save,{sv},{put}));save[j]=keep;}}'
 box=list(sb).index(0x937f7880)
 code+=f'auto rawSave=save;for(int j:{{{box-1},{box},{box+1},{box+2},{box+3},{box+4},{box+5}}}){{save=rawSave;save[j]=0xd503201f;ck(!home_layout::grid_layout_save_field(save,{sv},{put}));}}'
 code+='}\n'
code+='std::cout<<"DART_COLUMNS_CONSUMER checks="<<checks<<" failed="<<failed<<"; three real images + branch/frame/descriptor/callee mutations\\n";return failed?1:0;}\n'
(O/'test.cpp').write_text(code,encoding='utf8');zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe'
p=subprocess.run([str(zig),'c++','-std=c++20','-I'+str(R/'app/src/main/cpp'),str(O/'test.cpp'),'-o',str(O/'test.exe')],capture_output=True,text=True,encoding='utf8',errors='replace')
if p.returncode:print(p.stdout+p.stderr);raise SystemExit(p.returncode)
p=subprocess.run([str(O/'test.exe')],capture_output=True,text=True,encoding='utf8');print(p.stdout+p.stderr,end='');(O/'result.json').write_text(json.dumps(dict(stdout=p.stdout,stderr=p.stderr,exit=p.returncode),indent=2),encoding='utf8');raise SystemExit(p.returncode)
