"""Run the existing full native callback matrix against actual launcher 8244.
Only fixture addresses/expected compiler locations differ; production binder,
parser, callbacks and mutation rejection tests are still executed verbatim.
"""
from pathlib import Path
import sys
W=Path(__file__).resolve().parents[2]
source=(W/'tests/folder-close-preview-os4/verify.py').read_text(encoding='utf8')
a=source.index("r=Path('C:/");b=source.index('\nnames=',a)
source=source[:a]+"r=W/'handover/folder-return-8244-20261009';e=Elf(r/'launcher-8244-libapp.so');s=Symbols(r/'launcher-8244-mini.elf');m=Elf(r/'launcher-8244-mini.elf');sec=m.sections[m.section_names['.symtab']];vas=sorted(set(v for _,t,_,_,v,_ in (struct.unpack_from('<IBBHQQ',m.data,a) for a in range(sec[4],sec[4]+sec[5],24)) if t&15==2 and v))"+source[b:]
# Additional named span owns the new anonymous delegate builder, not a hook target.
source=source.replace("globals='#include", "names+=['FolderGridView._interruptScrollWhenClosing']\nglobals='#include")
source=source.replace('va0+0x180','va15+0x4b8')
source=source.replace("pre+='FolderPreviewPlan p;", "pre+='auto builder=b15;unsigned builder_va=va15;FolderPreviewPlan p;")
source=source.replace('folder_preview_plan(b0,','folder_preview_plan(builder,').replace('b4,va0,va5','b4,builder_va,va5')
source=source.replace('p.site[0]==0x180','p.site[0]==0x4b8').replace('p.context_local==-24&&p.context_controller==15','p.context_local==-8&&p.context_controller==19')
# Negative tests for the new admission route, before the normal delayed-owner tests.
needle="pre+='''for(unsigned delayed=0;delayed<11;++delayed){"
extra="""pre+='''{auto name=std::string("FolderGridView._interruptScrollWhenClosing");auto found=symbols[name];symbols.erase(name);
ok(!bind_folder_preview());ok(!g_folder_preview_checked);ok(!g_folder_preview_bound);for(auto& slot:g_slots)ok(slot.address==0);symbols[name]=found;}
'''
"""
source=source.replace(needle,extra+needle)

# Mutate every original instruction forming the new delegate/context flow.
source=source.replace('std::cout<<\"FOLDER_CLOSE_PREVIEW=', '''for(unsigned boundary:{0xa9bf79fdu,0xaa0f03fdu,0xd65f03c0u}){auto broken=builder;broken[p.site[0]/4-8]=boundary;ok(!folder_preview_plan(broken,b1,b2,b3,b4,builder_va,va5,b6,b7,b8,b9,b10,refuse));}
for(unsigned j=0;j<7;++j){auto broken=builder;broken[p.site[0]/4-6+j]=0xd503201f;ok(!folder_preview_plan(broken,b1,b2,b3,b4,builder_va,va5,b6,b7,b8,b9,b10,refuse));}
for(size_t j=4;j<builder.size();++j){if(preview_call_to(builder[j],builder_va+unsigned(j*4),va5)){for(unsigned k=0;k<5;++k){auto broken=builder;broken[j-k]=0xd503201f;ok(!folder_preview_plan(broken,b1,b2,b3,b4,builder_va,va5,b6,b7,b8,b9,b10,refuse));}}}
std::cout<<\"FOLDER_CLOSE_PREVIEW=''')
source=source.replace('FOLDER_CLOSE_PREVIEW=','FOLDER_PREVIEW_8244=')
exec(compile(source,str(__file__),'exec'),globals())
