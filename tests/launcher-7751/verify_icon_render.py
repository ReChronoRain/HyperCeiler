"""Resolve real AppIcon dimensions in three shipped AOT images; no device execution."""
from pathlib import Path
import sys,subprocess,struct,json
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
T=W/'handover/launcher-7751-20261017/visual-compat';out=T/('render-'+R.name);out.mkdir(parents=True,exist_ok=True)
header=R/'app/src/main/cpp/targets/home/home_icon_render.h'
if not header.exists():print('ICON_RENDER_IMPLEMENTATION_MISSING');raise SystemExit(1)
sys.path.insert(0,str(W/'tests/home-layout-native'));from dart_dump import Elf,Symbols
fixtures=[('7751',W/'handover/launcher-7751-20261017','launcher-7751'),('7722',Path('C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f'),'launcher-7722'),('8244',W/'handover/folder-title-os4-20261012','launcher-8244')]
source='#include "targets/home/home_icon_render.h"\n#include <iostream>\nint main(){unsigned checks=0,failed=0;auto ok=[&](bool v){++checks;if(!v){++failed;std::cout<<"FAIL="<<checks<<"\\n";}};\n'
for name,folder,prefix in fixtures:
 elf=folder/(prefix+'-libapp.so');mini=folder/(prefix+'-mini.elf')
 if not elf.exists() or not mini.exists():elf=next((W/'handover').rglob(prefix+'-libapp.so'));mini=elf.with_name(prefix+'-mini.elf')
 e=Elf(elf);s=Symbols(mini);va,size,_=s.resolve('AppIcon.build');current=s.resolve('GridController.currentConfig')[0];b=list(struct.unpack('<'+'I'*(size//4),e.read(va,size)))
 source+='{home_dynamic::Body b={'+','.join(hex(x) for x in b)+'};home_layout::IconRenderSites shape;'
 source+=f'ok(home_layout::icon_render_sites(b,{va},{current},shape));ok(shape.width>0&&shape.height>shape.width+16);ok(shape.grid>0&&shape.value>0&&shape.width_frame<0&&shape.height_frame<0);'
 source+='auto original=b;auto appShape=shape;auto width=shape.width/4;auto height=shape.height/4;'
 source+=f'b[width-1]=0xd503201f;ok(!home_layout::icon_render_sites(b,{va},{current},shape));b=original;'
 source+=f'b[height-3]^=0x4000;ok(!home_layout::icon_render_sites(b,{va},{current},shape));b=original;'
 source+=f'b[height+1]^=0x1000;ok(!home_layout::icon_render_sites(b,{va},{current},shape));b=original;'
 source+=f'ok(!home_layout::icon_render_sites(b,{va},{current+4},shape));'
 source+=f'b.insert(b.end(),original.begin(),original.end());ok(!home_layout::icon_render_sites(b,{va},{current},shape));'
 wbva,wbsize,_=s.resolve('ShortcutIconWidget.iconWidth');hbva,hbsize,_=s.resolve('ShortcutIconWidget.iconHeight');dv,ds,_=s.resolve('ShortcutIconWidget._buildNormalDrawable')
 for var,addr,count in [('wb',wbva,wbsize),('hb',hbva,hbsize),('desktop',dv,ds)]:
  words=struct.unpack('<'+'I'*(count//4),e.read(addr,count));source+='home_dynamic::Body '+var+'={'+','.join(hex(w) for w in words)+'};'
 source+='uint32_t wt=0,ht=0;'
 source+=f'ok(home_layout::icon_dimension_callee(wb,{wbva},wt));ok(home_layout::icon_dimension_callee(hb,{hbva},ht));ok(wt!=ht);ok(home_layout::desktop_icon_sites(desktop,{dv},wt,ht,shape));'
 source+=f'auto saved=desktop;desktop[shape.width/4+2]^=0x1000;ok(!home_layout::desktop_icon_sites(desktop,{dv},wt,ht,shape));desktop=saved;ok(!home_layout::desktop_icon_sites(desktop,{dv},ht,wt,shape));'
 def target_of_wrapper(addr,count):
  words=list(struct.unpack('<'+'I'*(count//4),e.read(addr,count)))
  for i,w in enumerate(words):
   if w&0xfc000000==0x94000000 and words[i+1:i+4]==[0xaa1d03ef,0xa8c179fd,0xd65f03c0]:
    imm=w&0x3ffffff;imm=imm-(1<<26) if imm&(1<<25) else imm;return addr+i*4+imm*4
  raise AssertionError('wrapper missing typed call')
 wt=target_of_wrapper(wbva,wbsize);ht=target_of_wrapper(hbva,hbsize);rx=s.resolve('RxObjectMixin.value')[0]
 for var,addr in [('wm',wt),('hm',ht)]:
  words=list(struct.unpack('<'+'I'*128,e.read(addr,512)));words=words[:words.index(0xd65f03c0)+1];source+='home_dynamic::Body '+var+'={'+','.join(hex(w) for w in words)+'};'
 source+='home_layout::IconMixinSites ws,hs;'
 source+=f'ok(home_layout::icon_mixin_sites(wm,{wt},{rx},appShape.grid,appShape.value,ws));ok(home_layout::icon_mixin_sites(hm,{ht},{rx},appShape.grid,appShape.value,hs));ok(ws.value+8==hs.value);'
 source+=f'wm[ws.normal/4+3]=0xd503201f;ok(!home_layout::icon_mixin_sites(wm,{wt},{rx},appShape.grid,appShape.value,ws));'
 source+=f'std::cout<<"REAL_IMAGE_{name} checks="<<checks<<" failed="<<failed<<"\\n";}}\n'
source+='std::cout<<"ICON_RENDER checks="<<checks<<" failed="<<failed<<"\\n";return failed?1:0;}\n'
(out/'test.cpp').write_text(source)
zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe';rows=[]
for cmd in [[zig,'c++','-std=c++20','-I'+str(R/'app/src/main/cpp'),out/'test.cpp','-o',out/'test.exe'],[out/'test.exe']]:
 p=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,encoding='utf8',errors='replace');rows.append(dict(command=list(map(str,cmd)),stdout=p.stdout,stderr=p.stderr,exit=p.returncode));print(p.stdout+p.stderr,end='')
 if p.returncode:break
(out/'results.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2));raise SystemExit(rows[-1]['exit'])
