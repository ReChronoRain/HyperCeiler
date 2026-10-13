from pathlib import Path
import sys,subprocess,struct,bisect,json,re
D=Path(__file__).resolve().parent;W=D.parents[1];root=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
header=root/'app/src/main/cpp/targets/home/home_folder_close_preview.h'
if not header.exists():print('FOLDER_CLOSE_PREVIEW=FAIL; rendered-grid correction absent');sys.exit(1)
sys.path.insert(0,str(W/'tests/home-layout-native'));from dart_dump import Elf,Symbols
r=Path('C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f');e=Elf(r/'launcher-7722-libapp.so');s=Symbols(r/'launcher-7722-mini.elf');m=Elf(r/'launcher-7722-mini.elf');sec=m.sections[m.section_names['.symtab']];vas=sorted(set(v for _,t,_,_,v,_ in (struct.unpack_from('<IBBHQQ',m.data,a) for a in range(sec[4],sec[4]+sec[5],24)) if t&15==2 and v))
names=['FolderGridView._buildScrollableGrid','SliverGridDelegateWithFixedCrossAxisCount.getLayout','FolderAnimController._setCloseGridItemAnim','FolderAnimController._refreshCachedGridParams','FolderClingGetxController.gridContainerWidth','FolderGridViewGetxController.folderCellHeight','FolderAnimController._setGridEndLoc','FolderAnimController._calcRealIconPos','FolderAnimController._calcFolderPreviewLoc','FolderAnimController._initGridViewItemAnimParams','ShortcutIconDropMixin.folderItemPadding']
if 'width_owner_gap' in header.read_text(): names[4]='FolderGridViewGetxController.calGridWidth'
if 'preview_call_to' in header.read_text(): names+=['GridController.currentConfig','FolderGridViewGetxController.getFirstVisibleItemIndex','FolderClingGetxController.calcPositionForCellX','Offset.translate']
cpp=(root/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text(encoding='utf8')
a=cpp.index('extern "C" void hc_folder_preview_body(uintptr_t saved, uintptr_t frame, unsigned kind, uintptr_t heap) {')
b=cpp.index('static thread_local double folder_layout_gap',a)
body=cpp[a:b]
index_globals=('static thread_local home_layout::FolderPreviewIndexCarry g_folder_preview_index_carry;\n' if 'FolderPreviewIndexCarry' in header.read_text() else '')
def fun(source,name):
 a=source.index(name);a=source.rfind('\n',0,a)+1;b=source.index('{',a);depth=1;j=b+1
 while depth:depth+=(source[j]=='{')-(source[j]=='}');j+=1
 return source[a:j]
registry=(root/'app/src/main/cpp/targets/home/tweaks/symtab.cpp').read_text(encoding='utf8')
assert all(re.search(r'\{\s*"'+re.escape(name)+r'"\s*,\s*"'+re.escape(name)+r'"\s*,',registry) for name in names)
globals='#include <cstring>\n#include <map>\n#include <string>\n#include <algorithm>\nconstexpr int ANDROID_LOG_WARN=5,ANDROID_LOG_INFO=4;const char*kTag="test";int __android_log_print(int,const char*,const char*,...){return 0;}\nstatic bool g_folder_preview_bound=false;static bool g_folder_preview_checked=false;\nstatic bool g_folder_probe_bound=false;static bool g_folder_probe_checked=false;\nstatic int g_folder_probe_cached_x=-1,g_folder_probe_cached_y=-1;\nstatic home_layout::FolderPreviewPlan g_folder_preview_fields;\nstatic thread_local home_layout::FolderPreviewSnapshot g_folder_preview_snapshot;\nstatic thread_local int64_t g_folder_preview_index=-1;\nstatic uint64_t hc_folder_layout_requested=uint64_t(4)<<27;\nstatic bool folder_preview_requested(uint64_t p){int n=int((p>>27)&7);return n>=3&&n<=6&&(n!=3||(p&2));}\n'

extra='struct Segment{uintptr_t begin,end;uint32_t flags;};struct Image{uintptr_t base;Segment segments[16];size_t segmentCount;};constexpr uint32_t kMaxFunctionBytes=0x4000;\n'
extra+='std::map<uint32_t,std::vector<uint32_t>> images;std::map<std::string,std::pair<uint32_t,uint32_t>> symbols;\n'
if 'CodeView(image)' in registry:
 sys.path.insert(0,str(W/'tests/os4-audit-symbol-prologue-20261005'));from fixture_support import codeview_fixture
 extra+=codeview_fixture(root,'for(const auto& [va,b]:images){const uintptr_t begin=reinterpret_cast<uintptr_t>(b.data());const size_t bytes=b.size()*4;if(address>=begin&&address-begin<=bytes&&out.size()<=bytes-(address-begin)){std::memcpy(out.data(),reinterpret_cast<void*>(address),out.size());return true;}}return false;',True)
for n in ['bool NameEquals(', 'bool VaInExecSegment(', 'bool LooksLikeDartFunction(']:extra+=fun(registry,n)+'\n'

extra+='namespace hometweaks{bool HomeTweaksFindSymbol(const char*n,uint32_t*v,uint32_t*z){if(!symbols.count(n))return false;auto p=symbols[n];auto&b=images[p.first];uintptr_t a=uintptr_t(b.data());Image im{};im.base=a-p.first;im.segmentCount=1;im.segments[0]={a,a+b.size()*4,5};if(!LooksLikeDartFunction(im,p.first,n,p.second))return false;*v=p.first;*z=p.second;return true;}}\n'
extra+='namespace dartscan{bool body(uint32_t v,std::vector<uint32_t>&b){if(!images.count(v))return false;b=images[v];return true;}}\n'
extra+='namespace nhk{struct CodeSource{};}using Words=std::array<uint32_t,4>;struct Slot{uintptr_t address=0;nhk::CodeSource source;Words original_words{};void *replacement=nullptr;void **original=nullptr;};Slot g_slots[7];constexpr int kFolderPreviewSlotBase=0;bool g_dart=true;\n'
extra+='void hc_folder_preview_0_entry(){}void hc_folder_preview_1_entry(){}void hc_folder_preview_2_entry(){}void hc_folder_preview_3_entry(){}void hc_folder_preview_4_entry(){}void hc_folder_preview_5_entry(){}void hc_folder_preview_6_entry(){}void *hc_folder_preview_original[7]{};\n'
extra+='void hc_folder_probe_0_entry(){}void *hc_folder_probe_original[1]{};uint32_t hc_folder_probe_enabled=0;\n'
extra+='bool bind_dart_target(uint32_t v,uintptr_t&addr,nhk::CodeSource&,Words&w){for(auto&pair:images){if(v>=pair.first&&v+16<=pair.first+pair.second.size()*4){addr=v;std::copy_n(pair.second.begin()+(v-pair.first)/4,4,w.begin());return true;}}return false;}\n'
extra+=fun(cpp,'bool bind_folder_preview() {')+'\n'
pre='#include <iostream>\n#include <vector>\n#include "home_folder_close_preview.h"\n'+globals+index_globals+body+extra+'int main(){ using namespace home_layout; int failed=0,checks=0;auto ok_impl=[&](bool x,int line){checks++;if(!x){failed++;std::cerr<<"FAIL "<<checks<<" line "<<line<<"\\n";}};\n#define ok(x) ok_impl((x),__LINE__)\n'
for i,n in enumerate(names):
 v,z=s.by_name[n];span=vas[bisect.bisect_right(vas,v)]-v;words=struct.unpack('<'+'I'*(span//4),e.read(v,span));pre+=f'unsigned va{i}={v};std::vector<unsigned>b{i}={{'+','.join(hex(w) for w in words)+'};\n'
for i,n in enumerate(names):pre+=f'images[va{i}]=b{i};symbols["{n}"]={{va{i},unsigned(b{i}.size()*4)}};\n'
pre+='const char*names_fixture[]={'+','.join('\"'+n+'\"' for n in names[:11])+'};\n'
# Execute late symbol/body admission using the actual production binding function.
pre+='''for(unsigned delayed=0;delayed<11;++delayed){
 auto name=std::string(names_fixture[delayed]);auto found=symbols[name];symbols.erase(name);
 ok(!bind_folder_preview());ok(!g_folder_preview_checked);ok(!g_folder_preview_bound);
 for(auto&slot:g_slots)ok(slot.address==0);
 symbols[name]=found;
}
'''
pre+='ok(bind_folder_preview());if(!g_folder_preview_bound){std::cout<<"BIND_FAILED";return 1;}ok(g_folder_preview_bound);ok(g_slots[0].address==va0+0x180&&g_slots[1].address==va1+0xfc&&g_slots[2].address==va2+0x200&&g_slots[3].address==va8+0x4c&&g_slots[4].address==va8+0xfc&&g_slots[5].address==va9+0xb0&&g_slots[6].address==va3+0x6c);ok(bind_folder_preview());\n'
pre+='FolderPreviewPlan p;ok(folder_preview_plan(b0,b1,b2,b3,b4,va0,va5,b6,b7,b8,b9,b10,p));ok(p.site[0]==0x180&&p.site[1]==0xfc&&p.site[2]==0x200);ok(p.cache_width==0x11f&&p.cache_height==0x123&&p.item_local==-32&&p.context_local==-24&&p.context_controller==15&&p.grid_width==83);ok(p.cache_inset_x==0x137&&p.cache_inset_y==0x133&&p.probe_site==0);\n'
pre+='ok(p.cache_icon_width==0x127&&p.cache_icon_height==0x12b&&p.config_cell==0x73&&p.cell_padding==0x27&&p.padding_site==0x6c&&p.scale_x_frame==-80&&p.scale_y_frame==-88);ok(p.destination_site==0x4c&&p.destination_store==0xfc);ok(p.preview_index==-24&&p.destination_controller==-8&&p.destination_frame==-16);\n'
pre+='''FolderPreviewSnapshot q;ok(!q.correct(0,92,92,0,0));
q.expect(4,12,8,1,393.0797,123);ok(q.capture(4,104,93,92,85,123)==false);ok(q.capture(4,97.6595,93.6595,85.6595,85.6595,123));
for(int idx=0;idx<36;idx++){double dx=0,dy=0;ok(q.delta(idx,92.2699,92.2699,16.13495,123,dx,dy));ok(std::abs(dx-(9.9156-(idx%4)*6.6104))<0.001);ok(std::abs(dy-(-3.3052-(idx/4)*6.6104))<0.001);}
double dx=0,dy=0;ok(!q.delta(0,92,92,16.13495,124,dx,dy));ok(!q.delta(-1,92,92,16.13495,123,dx,dy));
for(int cols=3;cols<=6;cols++){q.expect(cols,12,8,1.5,392,125);double cw=(392.0-(cols-1)*8)/cols,ch=cw/1.5;ok(q.capture(cols,ch+12,cw+8,ch,cw,125));for(int i=0;i<cols*6;i++){ok(q.delta(i,92,92,16.13495,125,dx,dy));double actualX=i%cols*(cw+8)+(cw-60)/2;double oldX=i%cols*100+16;ok(std::abs(oldX+dx-actualX)<0.0001);}}
q.expect(4,12,8,1,393,129);ok(!q.delta(0,92,92,16.13495,129,dx,dy));ok(!q.capture(4,104,100,92,92,128));ok(!q.capture(4,104,100,92,92,129+1));ok(!q.capture(4,104,100,92,0,129));
// DESTINATION correction: the constrained SliverGrid the icons really land in. `cell` is the
// configured cell calcPositionForCellX advanced by; `anim` is _calcRealIconPos' cached result.
q.expect(4,12,8,1,393.0797,123);ok(q.capture(4,97.6595,93.6595,85.6595,85.6595,123));
for(int idx=0;idx<36;idx++){double x=0,y=0;ok(q.destination(idx,92.2699,92.2699,200.0,0,0,123,x,y));
 ok(std::abs(x-((393.0797-(4*85.6595+3*8))/2+(idx%4+0.5)*(85.6595-92.2699)))<0.0001);
 ok(std::abs(y-((idx/4)+0.5)*(85.6595-92.2699))<0.0001);}
double ddx=0,ddy=0;ok(!q.destination(0,92.2699,92.2699,200.0,0,0,124,ddx,ddy));ok(!q.destination(-1,92.2699,92.2699,200.0,0,0,123,ddx,ddy));
ok(!q.destination(0,0,0,200.0,0,0,123,ddx,ddy));ok(!q.destination(0,2001,2001,200.0,0,0,123,ddx,ddy));
ok(!q.destination(0,92.2699,92.2699,200.0,std::nan(""),0,123,ddx,ddy));
auto qInvalid=FolderPreviewSnapshot{};ok(!qInvalid.destination(0,92.2699,92.2699,200.,0,0,123,ddx,ddy));
// The destination must move EVERY row (that is the whole point) while the source kernel must not.
q.expect(4,12,8,1,393.0797,123);ok(q.capture(4,97.6595,93.6595,85.6595,85.6595,123));
for(int idx:{0,4,8,12,16,20}){double x=0,y=0;ok(q.destination(idx,92.2699,92.2699,200.0,0,0,123,x,y));ok(y<0);}
for(int cols=3;cols<=6;cols++){q.expect(cols,12,8,1.5,392,140+cols);double cw=(392.0-(cols-1)*8)/cols,ch=cw/1.5;
 ok(q.capture(cols,ch+12,cw+8,ch,cw,140+cols));
 for(int i=0;i<cols*6;i++){double x=0,y=0;ok(q.destination(i,92.2699,92.2699,200.0,100,200,140+cols,x,y));
  double viewport=cols*cw+(cols-1)*8;
  ok(std::abs(x-(100+(392-viewport)/2+(i%cols+0.5)*(cw-92.2699)))<0.0001);
  ok(std::abs(y-(200+((i/cols)+0.5)*(ch-92.2699)))<0.0001);}}
// The wide-viewport case: the constrained child can EXCEED the configured cell, so the correction
// must be able to move icons outward, not only inward.
q.expect(3,12,8,1,393.0797,150);ok(q.capture(3,110,106,98,98,150));{double x=0,y=0;ok(q.destination(1,92.2699,92.2699,200.0,0,0,150,x,y));ok(x>0);}
auto bad=b2;bad[p.site[2]/4+1]=0x94000000;FolderPreviewPlan refuse;ok(!folder_preview_plan(b0,b1,bad,b3,b4,va0,va5,b6,b7,b8,b9,b10,refuse));bad=b2;bad[0]=0x14000000|((p.site[2]+4)/4);ok(!folder_preview_plan(b0,b1,bad,b3,b4,va0,va5,b6,b7,b8,b9,b10,refuse));

// Exercise the production callback itself with freshly allocated tagged fixtures.
g_folder_preview_fields=p;ok(p.source_offset==0xe3&&p.edit_flag==0x13b&&p.cache_inset_y==0x133);
alignas(8) unsigned char arena[8192]{};uintptr_t base=uintptr_t(arena),heap=base&~uintptr_t(0xffffffffu);
uintptr_t saved=base,frame=base+800,delegate=base+2049,context=base+2449,controller=base+2689;
uintptr_t layout=base+3073,anim=base+4097,offset=base+4609,width=base+5105,height=base+5169,inset=base+5233;
auto u64=[&](uintptr_t a,uint64_t v){std::memcpy((void*)a,&v,8);};
auto u32=[&](uintptr_t a,uint32_t v){std::memcpy((void*)a,&v,4);};
auto dbl=[&](uintptr_t a,double v){std::memcpy((void*)a,&v,8);};
auto rd=[&](uintptr_t a){double v=0;std::memcpy(&v,(void*)a,8);return v;};
u64(saved+8,delegate);u64(frame+p.context_local,context);u32(context+p.context_controller,uint32_t(controller));
// Read-only destination observer: records cached insets and writes nothing.
g_folder_probe_bound=true;g_folder_probe_cached_x=p.cache_inset_x;g_folder_probe_cached_y=p.cache_inset_y;
{uintptr_t saved2=base+6000,item2=base+6401,cx=base+6657,cy=base+6721;
 u64(saved2+8,anim);u64(saved2+16,item2);u32(anim+p.cache_inset_x,uint32_t(cx));u32(anim+p.cache_inset_y,uint32_t(cy));
 dbl(item2+7,100.5);dbl(item2+15,200.25);dbl(cx+7,3.25);dbl(cy+7,-7.5);
 unsigned char snap[8192];std::memcpy(snap,arena,sizeof arena);
 hc_folder_probe_body(saved2,frame,0,heap);ok(std::memcmp(snap,arena,sizeof arena)==0);
 hc_folder_probe_body(saved2,frame,1,heap);ok(std::memcmp(snap,arena,sizeof arena)==0);
 // Untagged item pointer must be refused (no dereference, no write). Re-snapshot after setup.
 u64(saved2+16,0);std::memcpy(snap,arena,sizeof arena);hc_folder_probe_body(saved2,frame,0,heap);ok(std::memcmp(snap,arena,sizeof arena)==0);
 u64(saved2+16,item2);hc_folder_layout_requested=uint64_t(3)<<27;std::memcpy(snap,arena,sizeof arena);hc_folder_probe_body(saved2,frame,0,heap);ok(std::memcmp(snap,arena,sizeof arena)==0);
 hc_folder_layout_requested=uint64_t(4)<<27;}
g_folder_probe_bound=false;
// Re-establish the 4-column snapshot the destination callback tests rely on, stamped with the
// same value the callback sees at runtime (hc_folder_layout_requested), exactly as capture() does.
q.expect(4,12,8,1,393.0797,uint64_t(4)<<27);ok(q.capture(4,97.6595,93.6595,85.6595,85.6595,uint64_t(4)<<27));
u64(delegate+p.delegate_count,4);dbl(delegate+p.delegate_main,12);dbl(delegate+p.delegate_cross,8);
dbl(saved+160,1);dbl(controller+p.grid_width,393.0797);hc_folder_preview_body(saved,frame,0,heap);
ok(!g_folder_preview_snapshot.valid);u64(saved,layout);u64(layout+p.layout_count,4);
dbl(layout+p.layout_main_stride,97.6595);dbl(layout+p.layout_cross_stride,93.6595);
dbl(layout+p.layout_height,85.6595);dbl(layout+p.layout_width,85.6595);hc_folder_preview_body(saved,frame,1,heap);
ok(g_folder_preview_snapshot.valid);hc_folder_preview_body(saved,frame,0,heap);ok(g_folder_preview_snapshot.valid);
u64(saved+8,anim);u64(saved+32,offset);u32(anim+p.source_offset,uint32_t(offset));u32(anim+p.edit_flag,16);
u32(anim+p.cache_width,uint32_t(width));u32(anim+p.cache_height,uint32_t(height));dbl(width+7,92.2699);dbl(height+7,92.2699);u32(anim+p.cache_inset_y,uint32_t(inset));dbl(inset+7,6.3083);
uintptr_t iconW=base+5305,iconH=base+5369,insetX=base+5433,config=base+5553,cell=base+5681;
u32(anim+p.cache_icon_width,uint32_t(iconW));u32(anim+p.cache_icon_height,uint32_t(iconH));u32(anim+p.cache_inset_x,uint32_t(insetX));
dbl(iconW+7,60.7413);dbl(iconH+7,60.7413);dbl(insetX+7,(92.2699-60.7413)/2);
u32(config+p.config_cell,uint32_t(cell));dbl(cell+p.cell_padding,2.022686);u64(saved,config);
unsigned char paddingBefore[8192];std::memcpy(paddingBefore,arena,sizeof arena);hc_folder_preview_body(saved,frame,6,heap);
ok(g_folder_preview_snapshot.padding_valid&&g_folder_preview_snapshot.padding==2.022686);ok(std::memcmp(paddingBefore,arena,sizeof arena)==0);
for(int i=0;i<36;i++){dbl(frame+p.scale_x_frame,0.225);dbl(frame+p.scale_y_frame,0.225);u64(frame+p.item_local,i);dbl(offset+7,16+(i%4)*100.2699);dbl(offset+15,260+(i/4)*104.2699);
 double x=rd(offset+7),y=rd(offset+15);unsigned char before[8192];std::memcpy(before,arena,sizeof arena);
 hc_folder_preview_body(saved,frame,2,heap);ok(std::abs(rd(offset+7)-(x+9.9156-(i%4)*6.6104))<0.001);
 ok(std::abs(rd(offset+15)-(y-3.3052-(i/4)*6.6104))<0.001);
 std::memcpy(before+(offset+7-base),(void*)(offset+7),16);std::memcpy(before+(frame+p.scale_x_frame-base),(void*)(frame+p.scale_x_frame),8);std::memcpy(before+(frame+p.scale_y_frame-base),(void*)(frame+p.scale_y_frame),8);ok(std::memcmp(before,arena,sizeof arena)==0);}
dbl(offset+7,10);dbl(offset+15,20);u32(anim+p.edit_flag,0);hc_folder_preview_body(saved,frame,2,heap);ok(rd(offset+7)==10&&rd(offset+15)==20);
u32(anim+p.edit_flag,16);hc_folder_layout_requested=uint64_t(3)<<27;hc_folder_preview_body(saved,frame,2,heap);ok(rd(offset+7)==10);
hc_folder_layout_requested=(uint64_t(4)<<27)|2;hc_folder_preview_body(saved,frame,2,heap);ok(rd(offset+7)==10); // stale stamp
hc_folder_layout_requested=uint64_t(4)<<27;u64(frame+p.item_local,-1);hc_folder_preview_body(saved,frame,2,heap);ok(rd(offset+7)==10);
u64(frame+p.item_local,0);u32(anim+p.source_offset,uint32_t(offset+32));hc_folder_preview_body(saved,frame,2,heap);ok(rd(offset+7)==10);
u32(anim+p.source_offset,uint32_t(offset));g_folder_preview_bound=false;hc_folder_preview_body(saved,frame,2,heap);ok(rd(offset+7)==10);
g_folder_preview_bound=true;
// DESTINATION callback: kind 3 stashes the folder item index, kind 4 rewrites the Offset that
// _calcRealIconPos just produced so the preview lands on the CONSTRAINED grid.
// The configured cell anim[0x11f] is the value calcPositionForCellX advanced by.
uintptr_t frameAnim=anim;u64(frame+p.preview_index,uint64_t(6));u64(frame+p.destination_frame,frameAnim);
u32(frameAnim+p.cache_width,uint32_t(width));dbl(width+7,92.2699);
// _calcFolderPreviewLoc must only see the *constrained* snapshot: no capture, no correction.
g_folder_preview_snapshot=FolderPreviewSnapshot{};g_folder_preview_index=0;dbl(offset+7,1);dbl(offset+15,2);
u64(saved,0);hc_folder_preview_body(saved,frame,3,heap);ok(g_folder_preview_index==6);u64(saved,offset);
hc_folder_preview_body(saved,frame,4,heap);ok(rd(offset+7)==1&&rd(offset+15)==2); // snap invalid -> no write
g_folder_preview_snapshot=q;g_folder_preview_index=0;
// A refused request (3 columns without the shape bit) must not be acted on at all.
hc_folder_layout_requested=uint64_t(3)<<27;hc_folder_preview_body(saved,frame,3,heap);ok(g_folder_preview_index==0);
hc_folder_preview_body(saved,frame,4,heap);ok(rd(offset+7)==1&&rd(offset+15)==2);
hc_folder_layout_requested=uint64_t(4)<<27;
// Index zero is a valid first icon; invalid calls must invalidate a previous valid selector.
u64(saved,0); // firstVisibleItemIndex result, live x0 before MOV/ADD replay
u64(frame+p.preview_index,0);hc_folder_preview_body(saved,frame,3,heap);
u64(saved,offset);dbl(offset+7,115.813);dbl(offset+15,241.580);
double destX=0,destY=0;ok(q.destination(0,92.2699,92.2699,6.3083,115.813,241.580,uint64_t(4)<<27,destX,destY));
hc_folder_preview_body(saved,frame,4,heap);ok(std::abs(rd(offset+7)-destX)<0.0001&&std::abs(rd(offset+15)-destY)<0.0001);
u64(saved,0);u64(frame+p.preview_index,6);hc_folder_preview_body(saved,frame,3,heap);
u64(frame+p.preview_index,uint64_t(-1));hc_folder_preview_body(saved,frame,3,heap);
u64(saved,offset);dbl(offset+7,7);dbl(offset+15,8);hc_folder_preview_body(saved,frame,4,heap);ok(rd(offset+7)==7&&rd(offset+15)==8);
// firstVisible=4 plus local index=2 must correct absolute cell 6.
u64(saved,4);u64(frame+p.preview_index,2);hc_folder_preview_body(saved,frame,3,heap);
u64(saved,offset);dbl(offset+7,115.813);dbl(offset+15,241.580);
ok(q.destination(6,92.2699,92.2699,6.3083,115.813,241.580,uint64_t(4)<<27,destX,destY));
hc_folder_preview_body(saved,frame,4,heap);ok(std::abs(rd(offset+7)-destX)<0.0001&&std::abs(rd(offset+15)-destY)<0.0001);
// Every configured-cell read is load-bearing: an untagged anim refuses without writing.
uintptr_t keepAnim=0;std::memcpy(&keepAnim,(void*)(frame+p.destination_frame),8);u64(frame+p.destination_frame,0);
dbl(offset+7,7);dbl(offset+15,8);hc_folder_preview_body(saved,frame,4,heap);ok(rd(offset+7)==7&&rd(offset+15)==8);
u64(frame+p.destination_frame,keepAnim);
u64(saved,0);hc_folder_preview_body(saved,frame,4,heap);ok(rd(offset+7)==7&&rd(offset+15)==8);
u64(saved,offset);u32(frameAnim+p.cache_width,0);
hc_folder_preview_body(saved,frame,4,heap);ok(rd(offset+7)==7&&rd(offset+15)==8);
u32(frameAnim+p.cache_width,uint32_t(width));
u64(saved,4);u64(frame+p.preview_index,2);hc_folder_preview_body(saved,frame,3,heap);u64(saved,offset);
dbl(offset+7,115.813);dbl(offset+15,241.580);
hc_folder_preview_body(saved,frame,4,heap);ok(std::abs(rd(offset+7)-destX)<0.0001&&std::abs(rd(offset+15)-destY)<0.0001);
// Unknown kinds (>=5) must be inert: the entry table has exactly five.
dbl(offset+7,7);dbl(offset+15,8);hc_folder_preview_body(saved,frame,7,heap);ok(rd(offset+7)==7&&rd(offset+15)==8);
auto brokenEnd=b6;brokenEnd[0x38/4]=0xd503201f;ok(!folder_preview_plan(b0,b1,b2,b3,b4,va0,va5,brokenEnd,b7,b8,b9,b10,refuse));
// The destination observer's site must come from a real prologue: remove it and the plan refuses.
auto brokenReal=b7;brokenReal[2]=0xd503201f;ok(!folder_preview_plan(b0,b1,b2,b3,b4,va0,va5,b6,brokenReal,b8,b9,b10,refuse));
brokenEnd=b6;brokenEnd[0x2c/4]=0xd503201f;ok(!folder_preview_plan(b0,b1,b2,b3,b4,va0,va5,brokenEnd,b7,b8,b9,b10,refuse));
auto brokenClose=b2;brokenClose[p.site[2]/4+9]=0xd503201f;ok(!folder_preview_plan(b0,b1,brokenClose,b3,b4,va0,va5,b6,b7,b8,b9,b10,refuse));
// Each destination window is load-bearing: break either the index materialisation or the tail
// store and the whole plan must refuse rather than bind a slot against a foreign sequence.
auto brokenDest=b8;brokenDest[p.destination_site/4+2]=0xd503201f;ok(!folder_preview_plan(b0,b1,b2,b3,b4,va0,va5,b6,b7,brokenDest,b9,b10,refuse));
brokenDest=b8;brokenDest[p.destination_store/4+2]=0xd503201f;ok(!folder_preview_plan(b0,b1,b2,b3,b4,va0,va5,b6,b7,brokenDest,b9,b10,refuse));
// A stray second copy of either window must also refuse (uniqueness, not merely presence).
auto dupDest=b8;dupDest[p.destination_site/4]=0xd503201f;ok(!folder_preview_plan(b0,b1,b2,b3,b4,va0,va5,b6,b7,dupDest,b9,b10,refuse));
ok(p.anchor_site==0xb0&&p.anchor_frame==-8);
auto brokenAnchor=b9;brokenAnchor[p.anchor_site/4]=0xd503201f;ok(!folder_preview_plan(b0,b1,b2,b3,b4,va0,va5,b6,b7,b8,brokenAnchor,b10,refuse));
u64(saved+16,offset);u64(frame+p.anchor_frame,anim);u32(anim+p.edit_flag,16);
// Correct the paint anchor, including positive Y, using the captured layout.
g_folder_preview_snapshot=q;g_folder_preview_snapshot.padding_valid=true;
g_folder_preview_snapshot.padding=2.022686;g_folder_preview_snapshot.padding_stamp=hc_folder_layout_requested;
for(double iy:{-18.,0.,6.3083,24.}){
 dbl(inset+7,iy);dbl(saved+160,iy);dbl(offset+7,15.7643);
 unsigned char before[8192];std::memcpy(before,arena,sizeof arena);
 hc_folder_preview_body(saved,frame,5,heap);
 ok(std::abs(rd(offset+7)-(85.6595-60.7413)/2)<1e-6);
 ok(std::abs(rd(saved+160)-std::max(0.,iy+(85.6595-92.2699)/2))<1e-6);
 std::memcpy(before+160,arena+160,8);std::memcpy(before+(offset+7-base),(void*)(offset+7),8);
 ok(std::memcmp(before,arena,sizeof arena)==0);
}
dbl(inset+7,6.3083);dbl(saved+160,-9.456);u32(anim+p.edit_flag,0);
hc_folder_preview_body(saved,frame,5,heap);ok(rd(saved+160)==-9.456);
u32(anim+p.edit_flag,16);u64(saved+16,0);hc_folder_preview_body(saved,frame,5,heap);ok(rd(saved+160)==-9.456);
u64(saved+16,offset);
// Constrained narrow / fill-width / landscape geometries and non-square cells.
for(int cols=3;cols<=6;cols++)for(double viewport:{300.,366.638,393.0797,720.})for(double ar:{0.75,1.,1.5}){
 q.expect(cols,12,8,ar,393.0797,800+cols);double cw=(viewport-(cols-1)*8)/cols,ch=cw/ar;
 ok(q.capture(cols,ch+12,cw+8,ch,cw,800+cols));for(int idx=0;idx<cols*6;idx++){
  ok(q.delta(idx,92.2699,92.2699,16.13495,800+cols,dx,dy));
  double oldX=(idx%cols)*(92.2699+8)+(92.2699-60)/2;
  double oldY=(idx/cols)*(92.2699+12)+(92.2699-60)/2;
  double actualX=(393.0797-viewport)/2+(idx%cols)*(cw+8)+(cw-60)/2;
  double actualY=(idx/cols)*(ch+12)+std::max(0.,(ch-60)/2);
  ok(std::abs(oldX+dx-actualX)<0.0001&&std::abs(oldY+dy-actualY)<0.0001);
 }
}
for(int n:{5,6}){q.expect(n,12,8,1,393.0797,999);double cw=(366.638-(n-1)*8)/n;ok(q.capture(n,cw+12,cw+8,cw,cw,999));for(int idx=0;idx<11;idx++){ok(q.delta(idx,92.2699,92.2699,6.3083,999,dx,dy));ok(std::abs(dy-(-6.3083+(idx/n)*(cw-92.2699)))<0.0001);}}
q.expect(4,12,8,1,360,1001);ok(q.capture(4,96,92,84,84,1001));ok(q.delta(0,56,56,-18,1001,dx,dy));ok(dy==18);
// END-TO-END acceptance: source and destination kernels must reduce to the SAME constrained stride.
// Before this fix the destination advanced by the configured cell (cfg+gap) while the render used
// child+gap, so the gap grew by (cfg-child) per index - the 11-20px single-frame jump. The proof is
// structural: (configured stride + destination dx-step) == (constrained stride), for every combo.
for(int cols=3;cols<=6;cols++)for(double viewport:{300.,366.638,393.0797}){
 q.expect(cols,12,8,1,393.0797,2000+cols);double cw=(viewport-(cols-1)*8)/cols,ch=cw;
 ok(q.capture(cols,ch+12,cw+8,ch,cw,2000+cols));
 const double cfg=92.2699;
 for(int col=0;col+1<cols;col++){
  // Destination: calcPositionForCellX(col) + correction, minus the same for col+1 -> the stride.
  // Stay inside one row: the column term wraps at `cols`, so a full wrap must not be compared.
  double x0=0,y0=0,x1=0,y1=0;
  ok(q.destination(col,cfg,cfg,200.0,col*(cfg+8),0,2000+cols,x0,y0));
  ok(q.destination(col+1,cfg,cfg,200.0,(col+1)*(cfg+8),0,2000+cols,x1,y1));
  ok(std::abs((x1-x0)-(cw+8))<0.0001);
 }
 for(int row=0;row<3;row++){
  double x0=0,y0=0,x1=0,y1=0;
  ok(q.destination(row*cols,cfg,cfg,200.0,0,row*(cfg+12),2000+cols,x0,y0));
  ok(q.destination((row+1)*cols,cfg,cfg,200.0,0,(row+1)*(cfg+12),2000+cols,x1,y1));
  ok(std::abs((y1-y0)-(ch+12))<0.0001);
 }
 // The whole point: the residual no longer grows with index. The spread across one column is ~0.
 double minx=1e9,maxx=-1e9;
 for(int row=0;row<5;row++){double x=0,y=0;ok(q.destination(row*cols,cfg,cfg,200.0,0,0,2000+cols,x,y));minx=std::min(minx,x);maxx=std::max(maxx,x);}
 ok(maxx-minx<0.0001);
}
// Parity must include non-square cells and zero/negative free space.
for(int n=3;n<=6;n++)for(double ar:{0.75,1.0,1.5})for(double iy:{-18.,0.,6.3083,24.}){
 q.expect(n,12,8,ar,393.0797,7000+n);double cw=(366.638-(n-1)*8)/n,ch=cw/ar;
 ok(q.capture(n,ch+12,cw+8,ch,cw,7000+n));
 for(int i=0;i<11;i++){double dx=0,dy=0,x=0,y=0;
  ok(q.delta(i,92.2699,100.,iy,7000+n,dx,dy));
  ok(q.destination(i,92.2699,100.,iy,100.,200.,7000+n,x,y));
  ok(std::abs(x-100.-dx)<1e-8&&std::abs(y-200.-dy)<1e-8);
 }
}
// Real paint-size/scale handoff, including the measured non-square 6-column full-width grid.
for(int n=3;n<=6;n++)for(bool full:{false,true}){
 const uint64_t st=(uint64_t(n)<<27)|2;
 hc_folder_layout_requested=st;
 auto &ss=g_folder_preview_snapshot;
 double cw=((full?390.638:366.638)-(n-1)*8)/n,ch=full?91.74662:cw;
 ss.expect(n,12,8,cw/ch,393.0797,st);ok(ss.capture(n,ch+12,cw+8,ch,cw,st));
 ss.padding_valid=true;ss.padding=2.022686;ss.padding_stamp=st;
 for(int i=0;i<11;i++){
  double ax=0,ay=0,extra=0,ratio=0;
  ok(ss.painted(92.2699,92.2699,60.7413,60.7413,15.7643,6.3083,st,ax,ay,extra,ratio));
  double pw=std::min(60.7413,cw-4.045372);
  ok(std::abs(pw*0.225033*ratio-60.7413*0.225033)<1e-7);
  dbl(inset+7,6.3083);dbl(insetX+7,15.7643);
  u64(frame+p.anchor_frame,anim);u64(saved+16,offset);dbl(saved+160,6.3083);dbl(offset+7,15.7643);
  hc_folder_preview_body(saved,frame,5,heap);ok(std::abs(rd(offset+7)-ax)<1e-7&&std::abs(rd(saved+160)-ay)<1e-7);
  u64(saved+8,anim);u64(saved+32,offset);u64(frame+p.item_local,i);
  dbl(offset+7,15.7643+(i%n)*100.2699);dbl(offset+15,6.3083+(i/n)*104.2699);
  dbl(frame+p.scale_x_frame,0.225033);dbl(frame+p.scale_y_frame,0.225033);
  hc_folder_preview_body(saved,frame,2,heap);
  ok(std::abs(rd(frame+p.scale_x_frame)-0.225033*ratio)<1e-7);
  ok(std::abs(rd(frame+p.scale_y_frame)-0.225033*ratio)<1e-7);
  double viewport=n*cw+(n-1)*8;
  ok(std::abs(rd(offset+7)-((393.0797-viewport)/2+(i%n)*(cw+8)+ax))<1e-7);
  ok(std::abs(rd(offset+15)-((i/n)*(ch+12)+ay))<1e-7);
 }
 // A stale/invalid padding capture must make both correction callbacks inert.
 ss.padding_stamp=st-1;unsigned char before[8192];std::memcpy(before,arena,sizeof arena);
 hc_folder_preview_body(saved,frame,2,heap);hc_folder_preview_body(saved,frame,5,heap);ok(std::memcmp(before,arena,sizeof arena)==0);
 u64(saved,0);hc_folder_preview_body(saved,frame,6,heap);ok(!ss.padding_valid);
}
auto brokenPad=b10;brokenPad[0x64/4]=0xd503201f;
ok(!folder_preview_plan(b0,b1,b2,b3,b4,va0,va5,b6,b7,b8,b9,brokenPad,refuse));
auto brokenCache=b3;brokenCache[0x6c/4]=0xd503201f;
ok(!folder_preview_plan(b0,b1,b2,brokenCache,b4,va0,va5,b6,b7,b8,b9,b10,refuse));
auto brokenScale=b2;brokenScale[p.site[2]/4-5]=0xd503201f;
ok(!folder_preview_plan(b0,b1,brokenScale,b3,b4,va0,va5,b6,b7,b8,b9,b10,refuse));
std::cout<<"FOLDER_CLOSE_PREVIEW="<<checks<<" checks; failed="<<failed<<"\\n";return failed?1:0;}
'''.replace('ok(!q.correct(0,92,92,0,0));','double dummyX=0,dummyY=0;ok(!q.delta(0,92,92,16.13495,0,dummyX,dummyY));')
if 'FolderPreviewIndexCarry' in header.read_text():
 pre=pre.replace('ok(g_folder_preview_index==6);','ok(g_folder_preview_index_carry.take(frame,hc_folder_layout_requested)==6); g_folder_preview_index_carry.put(frame,hc_folder_layout_requested,6);')
out=D/('run-'+root.name);out.mkdir(exist_ok=True);(out/'test.cpp').write_text(pre,encoding='utf8')
zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe';p=subprocess.run([str(zig),'c++','-std=c++20','-O0','-I'+str(header.parent),str(out/'test.cpp'),'-o',str(out/'test.exe')],capture_output=True,text=True);print(p.stdout+p.stderr,end='');
if p.returncode:sys.exit(p.returncode)
p=subprocess.run([str(out/'test.exe')],capture_output=True,text=True);print(p.stdout+p.stderr,end='');sys.exit(p.returncode)




