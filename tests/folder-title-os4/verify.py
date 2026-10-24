from pathlib import Path
import sys,struct,subprocess
W=Path(__file__).resolve().parents[2];root=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
H=root/'app/src/main/cpp/targets/home';D=W/'handover/folder-title-20261011'
if not (H/'home_folder_title.h').exists():print('FOLDER_TITLE=missing desktop folder consumer; failed=1');sys.exit(1)
sys.path.insert(0,str(W/'tests/home-layout-native'));from dart_dump import Elf,Symbols
paths=[('7722',Path('C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f')),('8244',W/'handover/folder-return-8244-20261009')];failed=0
for v,d in paths:
 elf=Elf(d/f'launcher-{v}-libapp.so');sym=Symbols(d/f'launcher-{v}-mini.elf');base,size=sym.by_name['FolderIconWidget._buildText'];end=next(x[0] for x in sym.entries if x[0]>base);b=list(struct.unpack('<'+'I'*((end-base)//4),elf.read(base,end-base)));rx=sym.by_name['RxObjectMixin.value'][0];font=sym.by_name['GridConfig.getTitleTextSize'][0]
 src='''#include "home_folder_title.h"
#include <iostream>
int checks=0,failed=0;void ok(bool v){++checks;if(!v){++failed;std::cout<<"FAIL="<<checks<<"\\n";}}
int main(){home_dynamic::Body b={'''+','.join(hex(x) for x in b)+f'''}};auto run=[&](auto &body,auto &s){{return home_folder_title::resolve(body,{base},{rx},{font},s);}};home_folder_title::Sites s;
 ok(run(b,s));if(failed)return 1;std::cout<<"SITES="<<s.color<<","<<s.font<<","<<s.color_frame<<","<<s.font_frame<<","<<s.nursery<<"\\n";
 auto moved=b;for(auto &w:moved)if((w&0x3b200c00u)==0x38000000u && home_dynamic::rn(w)==29 && home_dynamic::imm9(w)<0){{int off=home_dynamic::imm9(w)-8;w=(w&~0x1ff000u)|((off&511)<<12);}}
 home_folder_title::Sites m;ok(run(moved,m));ok(m.color_frame==s.color_frame-8&&m.font_frame==s.font_frame-8);
 moved=b;moved[s.color/4+1]+=8<<10;moved[s.color/4+2]+=8<<10;ok(run(moved,m));ok(m.thread==s.thread+64&&m.statics==s.statics+64);
 moved=b;moved[s.font/4+1]+=1<<15;ok(run(moved,m));ok(m.nursery==s.nursery+8);
 moved=b;moved[s.color/4-3]^=4;ok(!run(moved,m));
 moved=b;moved[s.font/4]^=1;ok(!run(moved,m));
 moved=b;moved[s.color/4]^=1;ok(!run(moved,m));
 auto twice=b;auto duplicate=b;for(auto &w:duplicate)if((w&0xfc000000u)==0x94000000u)w=(w&0xfc000000u)|((w-b.size())&0x3ffffffu);twice.insert(twice.end(),duplicate.begin(),duplicate.end());ok(!run(twice,m));
 ok(s.color_frame<0 && s.font_frame<0 && s.font>s.color);
 ok(s.end_register=={2 if v=='8244' else 4});
 moved=b;moved[s.font/4+2]+=16<<10;ok(run(moved,m));ok(m.allocation==s.allocation+16);
 moved=b;moved[s.font/4+2]=0x94000000;ok(!run(moved,m));

 std::cout<<"FOLDER_TITLE_{v}="<<checks<<" checks; failed="<<failed<<"\\n";return failed?1:0;}}
'''
 out=D/('host-'+root.name+'-'+v);out.mkdir(exist_ok=True);(out/'test.cpp').write_text(src)
 zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe'
 for cmd in [[str(zig),'c++','-std=c++20','-I'+str(H),'-I'+str(W/'app/src/main/cpp/targets/home'),str(out/'test.cpp'),'-o',str(out/'test.exe')],[str(out/'test.exe')]]:
  p=subprocess.run(cmd,capture_output=True,text=True);print(p.stdout+p.stderr,end='')
  if p.returncode and cmd[0]==str(zig):sys.exit(p.returncode)
  failed|=p.returncode
sys.exit(failed)
