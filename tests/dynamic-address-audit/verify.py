from pathlib import Path
import sys,struct,subprocess,json,os
D=Path(__file__).resolve().parent;ROOT=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else D.parents[1]
sys.path.insert(0,str(D.parents[1]/'tests/home-layout-native'))
from dart_dump import Elf,Symbols
R=Path('C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f')
e=Elf(R/'launcher-7722-libapp.so');sym=Symbols(R/'launcher-7722-mini.elf')
source=(ROOT/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text(encoding='utf8')
header=ROOT/'app/src/main/cpp/targets/home'
out=D/('run-'+ROOT.name);out.mkdir(exist_ok=True)
def function(text,name):
 a=text.index(name);a=text.rfind('\n',0,a)+1;b=text.index('{',a);depth=1;j=b+1
 while depth:
  depth+=(text[j]=='{')-(text[j]=='}');j+=1
 return text[a:j]
def cmd(c):
 r=subprocess.run(c,capture_output=True,text=True,encoding='utf8',errors='replace')
 with (D/'commands.jsonl').open('a',encoding='utf8') as f:f.write(json.dumps(dict(command=c,input='',stdout=r.stdout,stderr=r.stderr,exit=r.returncode),ensure_ascii=False)+'\n')
 if r.returncode or r.stdout: print(r.stdout+r.stderr)
 if r.returncode:sys.exit(r.returncode)
 return r
# Actual production functions, isolated only from APK IO/Binder/logging.
pre=r"""
#include <vector>
#include <cstdint>
#include <cstddef>
#include <algorithm>
#include <iterator>
#include <map>
#include <iostream>
#include <string>
#include <initializer_list>
#include "home_gadget_bridge.h"
#include "home_widget_move.h"
#include "home_dart_fields.h"
std::map<uint32_t,std::vector<uint32_t>> images;
bool dart_function_words(uint32_t va,uint32_t bytes,std::vector<uint32_t>&out){
 auto i=images.find(va);if(i==images.end()||bytes!=i->second.size()*4)return false;out=i->second;return true;
}
namespace dartscan {
bool function_span(uint32_t va,uint32_t*out){auto i=images.find(va);if(i==images.end())return false;*out=i->second.size()*4;return true;}
"""
pre+=function(source,'bool unique_sequence(')+'\n}\n'
pre+=function(source,'static bool require_runs(')+'\n#define HC_RUN(name) std::make_pair(static_cast<const uint32_t *>(name), std::size(name))\n'
pre+=function(source,'bool workspace_geometry_code_compatible(')+'\n'
pre+=function(source,'bool hotseat_geometry_code_compatible(')+'\n'
gfun=function(source,'bool gadget_scan_anchors(');pre+=gfun+'\n'
# Clone validator and callees use actual image bytes, no invented allocator.
pre+=function(source,'bool bl_target(uint32_t word, uint32_t pc, uint32_t *target) {')+'\n'
pre+='bool dart_words(uint32_t va,size_t count,std::vector<uint32_t>&out){auto i=images.find(va);if(i==images.end()||i->second.size()<count)return false;out.assign(i->second.begin(),i->second.begin()+count);return true;}\n'
if 'bool gadget_clone_compatible(' in source:
 pre+=function(source,'bool gadget_clone_compatible(')+'\n'
else:
 a=source.index('    // The model clone the clear path mirrors.');b=source.index('    Slot prepared[3]{};',a)
 old=source[a:b].replace('clone_size','clone_span').replace('if (!factory) return false;','return factory != 0;')
 pre+='bool gadget_clone_compatible(uint32_t clone,uint32_t clone_span){\n'+old+'}\n'
spanfun=function((ROOT/'app/src/main/cpp/targets/home/tweaks/symtab.cpp').read_text(encoding='utf8'),'bool SymbolIndex::SpanFrom(')
pre+='constexpr size_t kTargetCount=3; struct SymbolIndex {bool loaded_=true;bool has_[3]={true,true,true};uint32_t va_[3]={100,1000,1100};uint32_t span_[3]={100,100,100};bool SpanFrom(uint32_t,uint32_t*) const;};\n'+spanfun+'\n'
fixtures={}
for key,name in {'ser':'AssistantDragDataHelper.putWidgetIntoBundle','gate':'AssistantDragToPAHandler.canDragToPA','cell':'GridCellDelegate.performLayout','occ':'GridOccupiedCellDelegate.performLayout','dock':'HotSeatLayoutDelegate.cellLayout','drop':'CellLayoutGetxController.calculateCenterGlobalPosition','counts':'CellLayoutGetxController.isItemPosEmpty','clone':'GadgetInfoModel.cloneModel'}.items():
 va,size=sym.by_name[name]
 fixtures[key]=(va,list(struct.unpack('<'+'I'*(size//4),e.read(va,size))))
for key,(va,words) in fixtures.items():pre+=f'uint32_t {key}_va={va};std::vector<uint32_t> {key}={{'+','.join(hex(x) for x in words)+'};\n'
put=sym.by_name['BundleImpl.putInt'][0];span=sym.by_name['AssistantDragToPAHandler._isSpanSupportedByPa'][0]
call='gadget_scan_anchors(ser_va,images[ser_va].size()*4,'+str(put)+',gate_va,images[gate_va].size()*4,'+ (str(span)+',' if 'uint32_t span_callee' in gfun else '')+'a)'
geometry_new='uint32_t *patch_offset' in source[source.index('bool workspace_geometry_code_compatible'):source.index('bool hotseat_geometry_code_compatible')]
apply_new='uint32_t original = kWidgetMoveOriginal' in (header/'home_widget_move.h').read_text(encoding='utf8')
extra_images=''
clone_va,clone_words=fixtures['clone']
for i,word in enumerate(clone_words):
 if word&0xfc000000!=0x94000000:continue
 disp=word&0x3ffffff
 if disp&0x2000000:disp-=0x4000000
 va=clone_va+i*4+disp*4
 words=struct.unpack('<12I',e.read(va,48))
 extra_images+=f'images[{va}]={{'+','.join(hex(x) for x in words)+'};'
if 'read_grid_counts(' in (header/'home_dart_fields.h').read_text(encoding='utf8'):pre+='#define COMPLETE_GRID 1\n'
pre+=r"""
int checks=0,failed=0;void ok(bool v,const char*n){++checks;if(!v){++failed;std::cout<<"FAIL "<<n<<"\n";}}
int main(){for(auto p:{std::make_pair(ser_va,ser),std::make_pair(gate_va,gate),std::make_pair(cell_va,cell),std::make_pair(occ_va,occ),std::make_pair(dock_va,dock)})images[p.first]=p.second;
CLONE_IMAGES
images[clone_va]=clone;ok(gadget_clone_compatible(clone_va,clone.size()*4),"real clone allocator and field guards");
images[clone_va][0x44/4]^=1;ok(!gadget_clone_compatible(clone_va,clone.size()*4),"changed clone field register rejected");images[clone_va]=clone;
uint32_t spanout=0;ok(SymbolIndex{}.SpanFrom(100,&spanout)&&spanout==100,"symbol span must stop at unlisted function");
using namespace home_layout;
ok(branch_disp_words(0x37200000|((0x3fff)<<5))==-1,"signed TB imm14 backward");
ok(!is_ldr32_reg(0xb8400023,3,0),"unscaled load is not indexed load");
ok(!is_ldur_sp(0xf85e8fa0,-0x18,29),"preindexed load is not LDUR");
uint32_t target=0;ok(bl_target(0x97ffffff,0x1000,&target)&&target==0xffc,"BL signed backward");
ok(!bl_target(0x94000001,0xfffffffc,&target),"BL overflow rejected");
ok(!dart_ldur_w(0xb8406821,1,1,nullptr),"indexed load is not field LDUR");
ok(!dart_sign_extend_pair(0x8b1c8041,1),"heap ADD must extend same register");
GadgetAnchors a;bool bound=CALL;
ok(bound&&a.select==0x6ac&&a.put_int==0x27c&&a.ret==0x280&&a.common==0x704&&a.span_entry==0x808&&a.span_reject==0x898,"real Gadget anchors and original return PC");
for(auto index:{0x808/4,0x7f8/4})images[gate_va][index]=(images[gate_va][index]&~0x7ffe0u)|(((0x100-index*4)/4&0x3fff)<<5);
a={};ok(CALL&&a.span_reject==0x100,"backward Gadget reject preserved");images[gate_va]=gate;
images[ser_va].insert(images[ser_va].end(),{0xf11fb43f,0x540002a1,0xf85e83a0,0xf85f03a1});a={};ok(!CALL,"duplicate select rejected");images[ser_va]=ser;
images[ser_va][0x6b4/4]=0xf85e83a2;a={};ok(!CALL,"changed replay register rejected");images[ser_va]=ser;
images[gate_va][0x80c/4]=0xf85f83a1;a={};ok(!CALL,"changed span replay rejected");images[gate_va]=gate;
// Shift every instruction uniformly; relative internal branches stay valid, external BLs retarget.
for(auto key:{ser_va,gate_va}){auto& v=images[key];for(auto&w:v)if(is_bl(w))w=(w&0xfc000000u)|((w-16)&0x03ffffffu);v.insert(v.begin(),16,0xd503201f);}
a={};bound=CALL;ok(bound&&a.ret==0x2c0&&a.select==0x6ec&&a.span_entry==0x848&&a.span_reject==0x8d8,"Gadget follows shifted anchors");images[ser_va]=ser;images[gate_va]=gate;
uint32_t patch=0;
GEOMETRY
int32_t field=0;
auto reader=[](size_t bytes,std::vector<uint32_t>&o){if(bytes!=gate.size()*4)return false;o=gate;return true;};
WIDGET
uint32_t original=0x37200520,live=original;
auto read=[&](uintptr_t,uint32_t&w){w=live;return true;};auto write=[&](uintptr_t,uint32_t w){live=w;return true;};
ok(APPLY_ON&&live==kWidgetMoveReplacement,"enable relocated rejection branch");
ok(APPLY_OFF&&live==original,"restore exact relocated original branch");
live=original;auto dirty=[&](uintptr_t,uint32_t w){live=w;return false;};
ok(!APPLY_FAIL&&live==original,"dirty-write rollback preserves branch");
STRIDE_TESTS
auto fields=read_grid_field_offsets(drop);ok(fields.columns==-1&&fields.rows==-1&&fields.origin==0x3b,"bounded drop body must not learn neighbour fields");
#ifdef COMPLETE_GRID
 fields={};read_grid_cell_size(cell,fields);
 ok(read_grid_counts(counts,counts_va,11641464,fields)&&fields.columns==0x1b&&fields.rows==0x23,"bounded counts with named config and same reject");
 ok(read_occupied_fields(occ,fields)&&fields.item_col==0x37&&fields.item_row==0x3f&&fields.occupied_grid==0x17,"occupied stride arithmetic identifies item and grid");
 ok(read_hotseat_fields(dock,fields)&&fields.dock_columns==0x13&&fields.dock_item==0xf&&fields.dock_info==7,"Dock count/closure chain from actual consumer");
 auto dh=dock;dh[0x220/4]=(dh[0x220/4]&~0x1ff000u)|(0x2b<<12);dh[0x320/4]=(dh[0x320/4]&~0x1ff000u)|(0x1f<<12);dh[0x328/4]=(dh[0x328/4]&~0x1ff000u)|(0x17<<12);
 ok(read_hotseat_fields(dh,fields)&&fields.dock_columns==0x2b&&fields.dock_item==0x1f&&fields.dock_info==0x17,"Dock fields independently renumber");
 dh=dock;dh[0x330/4]^=1;ok(!read_hotseat_fields(dh,fields),"Dock column receiver change rejected");

 auto n=counts;n[0x134/4]=(n[0x134/4]&~0x1ff000u)|(0x4b<<12);n[0x1a8/4]=(n[0x1a8/4]&~0x1ff000u)|(0x57<<12);
 ok(read_grid_counts(n,counts_va,11641464,fields)&&fields.columns==0x4b&&fields.rows==0x57,"counts may reorder and gap changes");
 ok(!read_grid_counts(counts,counts_va,11641464+4,fields),"foreign currentConfig receiver rejected");
 n=counts;n[0x1c0/4]+=0x20;ok(!read_grid_counts(n,counts_va,11641464,fields),"different reject arm refused");
 n=counts;n.insert(n.end(),counts.begin()+0x130/4,counts.begin()+0x144/4);n[n.size()-5]=0x94000000;ok(!read_grid_counts(n,counts_va,11641464,fields)||fields.columns==0x1b,"noncallee bytes do not learn counts");
 n=occ;n[0x210/4]=(n[0x210/4]&~0x1ff000u)|(0x47<<12);n[0x224/4]=(n[0x224/4]&~0x1ff000u)|(0x53<<12);
 read_grid_cell_size(cell,fields);ok(read_occupied_fields(n,fields)&&fields.item_col==0x47&&fields.item_row==0x53,"item offsets move independently");
 n=occ;n[0x210/4]^=1;ok(!read_occupied_fields(n,fields),"wrong item receiver arithmetic refused");
 n=occ;fields.cell_height+=8;ok(!read_occupied_fields(n,fields),"occupied and empty stride contract must agree");
#else
for(int i=0;i<12;++i)ok(false,"missing bounded owning-field capability");
#endif
std::cout<<"DYNAMIC_AUDIT="<<checks<<" checks; failed="<<failed<<"\n";return failed?1:0;}
""".replace('CALL',call).replace('CLONE_IMAGES',extra_images)
geo=''
for key,occupied,expected in [('cell','false',0xd8),('occ','true',0x234),('dock','',0x568)]:
 if key=='dock':c=f'hotseat_geometry_code_compatible({key}_va'+(',&patch)' if geometry_new else ')')
 else:c=f'workspace_geometry_code_compatible({key}_va,{occupied}'+(',&patch)' if geometry_new else ')')
 if not geometry_new:geo+=f'patch={expected};'
 geo+=f'ok({c}&&patch=={expected},"real {key} patch");images[{key}_va].insert(images[{key}_va].begin(),8,0xd503201f);'
 geo+=f'ok({c}&&patch=={expected+32},"shifted {key} patch");images[{key}_va]={key};\n'
pre=pre.replace('GEOMETRY',geo)
new_widget='uint32_t span_callee = 0' in (header/'home_widget_move.h').read_text(encoding='utf8')
extra=f',gate_va,{span}' if new_widget else ''
widget=f'auto offset=widget_move_gate_offset(gate.size()*4,reader,&field{extra});ok(offset==0x7f8&&field==0xfb,"real widget gate bytes");'
widget+=f'gate[0x7f0/4]=0xb847b040;offset=widget_move_gate_offset(gate.size()*4,reader,&field{extra});ok(offset==0x7f8&&field==0x7b,"MIUI field may move below 0x80");'
pre=pre.replace('WIDGET',widget)
for token,enabled,writer in [('APPLY_ON','true','write'),('APPLY_OFF','false','write'),('APPLY_FAIL','true','dirty')]:
 pre=pre.replace(token,f'apply_widget_move_word(0x1000,{enabled},read,{writer}'+(',original)' if apply_new else ')'))
if 'read_grid_cell_size(' in (header/'home_dart_fields.h').read_text(encoding='utf8'):
 stride=r"""GridFieldOffsets fields2;read_grid_cell_size(cell,fields2);ok(fields2.cell_width==0x2b&&fields2.cell_height==0x33,"real stride fields from owner");
 auto moved=cell;moved[0x24/4]=(moved[0x24/4]&~0x1ff000u)|(0x43<<12);moved[0x2c/4]=(moved[0x2c/4]&~0x1ff000u)|(0x4b<<12);read_grid_cell_size(moved,fields2);ok(fields2.cell_width==0x43&&fields2.cell_height==0x4b,"renumbered stride fields");
 moved.insert(moved.end(),cell.begin()+0x24/4,cell.begin()+0x34/4);read_grid_cell_size(moved,fields2);ok(fields2.cell_width==-1&&fields2.cell_height==-1,"ambiguous stride fields rejected");
 moved=cell;moved[0x24/4]^=0x20;read_grid_cell_size(moved,fields2);ok(fields2.cell_width==-1,"stride receiver change rejected");"""
else: stride='ok(false,"real stride fields from owner");ok(false,"renumbered stride fields");ok(false,"ambiguous stride fields rejected");ok(false,"stride receiver change rejected");'
pre=pre.replace('STRIDE_TESTS',stride)
(out/'test.cpp').write_text(pre,encoding='utf8')
zig=str(D/'runtime/zig-windows-x86_64-0.13.0/zig.exe')
env=os.environ;env['ZIG_GLOBAL_CACHE_DIR']=str(D/'runtime/cache');env['ZIG_LOCAL_CACHE_DIR']=str(out/'cache')
cmd([zig,'c++','-std=c++20','-Wall','-Wextra','-Werror','-Wno-sign-compare','-I'+str(header),str(out/'test.cpp'),'-o',str(out/'test.exe')])
cmd([str(out/'test.exe')])
