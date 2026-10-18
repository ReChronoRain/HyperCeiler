from pathlib import Path
import sys,struct,subprocess,re
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W;H=R/'app/src/main/cpp/targets/home';D=W/'handover/auto-close-scroll-20261013'
header=(H/'home_folder_auto_close.h').read_text(encoding='utf8');new='resolve_folder_auto_close_immediate' in header
sys.path.insert(0,str(W/'tests/home-layout-native'));from dart_dump import Elf,Symbols
paths=[('7722',Path('C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f')),('8244',W/'handover/folder-return-8244-20261009')];failed=0
for version,d in paths:
 elf=Elf(d/f'launcher-{version}-libapp.so');sym=Symbols(d/f'launcher-{version}-mini.elf')
 def get(name):
  va,size=sym.by_name[name];return va,list(struct.unpack('<'+'I'*(size//4),elf.read(va,size)))
 va,body=get('AppIconRemoteClick.handleRemoteClick');close,cb=get('FolderClingGetxController.handleFolderClingClose')
 rx=sym.by_name['RxObjectMixin.value'][0];reg=sym.by_name['Inst|isRegistered'][0];anim=sym.by_name['FolderClingGetxController._shouldCloseWithAnimation'][0];immediate=sym.by_name['FolderClingGetxController.handleFolderClingCloseImmediately'][0];scroll=sym.by_name['FolderGridViewGetxController.scrollNearestRow'][0]
 src='#include "home_folder_auto_close.h"\n#include <vector>\n#include <iostream>\nint main(){unsigned checks=0,failed=0;auto ok=[&](bool v){++checks;if(!v)++failed;};std::vector<uint32_t> b={'+','.join(hex(w) for w in body)+'},c={'+','.join(hex(w) for w in cb)+'};\n'
 if new:
  src+=f'''auto run=[&](auto &b,auto &c){{return home_layout::resolve_folder_auto_close_immediate(b,{va},{close},{rx},{reg},c,{anim},{immediate},{scroll});}};
 auto sites=run(b,c);ok(sites.has_value());if(!sites)return 2;ok((*sites)[1]+8< b.size()*4);ok(b[(*sites)[1]/4]==0x910082c2u);ok(home_layout::kFolderAutoCloseImmediate==0x9100c2c2u);
 auto moved=b;for(auto &w:moved)if((w&0x3b200c00u)==0x38000000u && home_dynamic::rn(w)==29 && home_dynamic::imm9(w)<0){{int off=home_dynamic::imm9(w)-8;w=(w&~0x1ff000u)|((off&511)<<12);}}ok(run(moved,c).has_value());
 auto shifted=c;for(auto &w:shifted)if((w&0x3b200c00u)==0x38000000u && home_dynamic::rn(w)==29 && home_dynamic::imm9(w)<0){{int off=home_dynamic::imm9(w)-8;w=(w&~0x1ff000u)|((off&511)<<12);}}ok(run(b,shifted).has_value());
 for(size_t i=0;i<c.size();++i){{uint32_t target=0;if(!home_dynamic::target(c[i],{close}+uint32_t(i*4),target))continue;if(target=={anim} || target=={immediate} || target=={scroll}){{shifted=c;shifted[i]=0xd503201f;ok(!run(b,shifted));}}}}
 moved=b;moved[(*sites)[1]/4]=0x9100c2c2u;ok(!run(moved,c));
 moved=b;moved[(*sites)[0]/4]=0xd503201f;ok(!run(moved,c));
 auto patched=b;patched[(*sites)[0]/4]=home_layout::kFolderAutoCloseReplacement;patched[(*sites)[1]/4]=home_layout::kFolderAutoCloseImmediate;unsigned changed=0;for(size_t i=0;i<b.size();++i)changed+=patched[i]!=b[i];ok(changed==2);
 for(auto offset:*sites)patched[offset/4]=b[offset/4];ok(patched==b);
 std::cout<<"AUTO_CLOSE_SCROLL_{version}="<<checks<<" checks; failed="<<failed<<"\\n";return failed?1:0;}}
'''
 else:
  src+=f'auto at=home_layout::resolve_folder_auto_close(b,{va},{close},{rx},{reg});ok(at.has_value());ok(false);std::cout<<"AUTO_CLOSE_SCROLL_{version}="<<checks<<" checks; failed="<<failed<<"; animated close argument remains true\\n";return failed?1:0;}}'
 out=D/('host-'+R.name+'-'+version);out.mkdir(exist_ok=True);(out/'test.cpp').write_text(src,encoding='utf8')
 zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe'
 for cmd in [[str(zig),'c++','-std=c++20','-I'+str(H),'-I'+str(W/'app/src/main/cpp/targets/home'),str(out/'test.cpp'),'-o',str(out/'test.exe')],[str(out/'test.exe')]]:
  p=subprocess.run(cmd,capture_output=True,text=True);print(p.stdout+p.stderr,end='')
  if p.returncode and cmd[0]==str(zig):sys.exit(p.returncode)
  failed|=p.returncode
sys.exit(failed)
