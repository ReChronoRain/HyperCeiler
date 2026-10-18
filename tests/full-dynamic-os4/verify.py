from pathlib import Path
import sys,subprocess,struct,bisect
W=Path(__file__).resolve().parents[2];ROOT=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
D=W/'handover/full-dynamic-20261009';H=ROOT/'app/src/main/cpp/targets/home'
if not (H/'home_dynamic_layout.h').exists():print('DYNAMIC_LAYOUT=missing runtime operand resolver; exit=1');sys.exit(1)
base=(W/'tests/dynamic-address-audit/verify_bindings.py').read_text(encoding='utf8');base=base[:base.index("pre+='int checks=")]
base=base.replace("D=Path(__file__).resolve().parent;ROOT=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else D.parents[1]", "D=W/'tests/dynamic-address-audit';ROOT=ROOT")
failed_total=0
for version,r in [('7722',Path('C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f')),('8244',W/'handover/folder-return-8244-20261009')]:
 a=base.index("R=Path('C:/");b=base.index('\nsec=',a)
 code=base[:a]+f"R=Path({str(r)!r});e=Elf(R/'launcher-{version}-libapp.so');sym=Symbols(R/'launcher-{version}-mini.elf');mini=Elf(R/'launcher-{version}-mini.elf')"+base[b:]
 code=code.replace("names=[", "names=['PhoneRowColHandler.handle','GadgetInfoModel.getCategoryId','AssistantDragDataHelper.putWidgetIntoBundle','GadgetInfoModel.cloneModel','BundleImpl.putInt','AssistantDragToPAHandler.canDragToPA','AssistantDragToPAHandler._isSpanSupportedByPa',")
 env=dict(globals());exec(code,env);pre=env['pre']
 pre=pre.replace('#include "home_dart_fields.h"','#include "home_dart_fields.h"\n#include "home_dynamic_layout.h"\n#include "home_gadget_bridge.h"\n#include "home_workspace_geometry.h"\n#include "home_title_render.h"\n#include "home_title_custom.h"')
 pre+=env['fun']('int imm9(uint32_t word) {')+'\n' # Include the legacy unsigned field decoder to catch namespace shadowing.
 pre+=env['fun']('bool gadget_scan_anchors(').replace('if(hits!=1)return false;', 'std::cout<<"SERIAL_HITS="<<hits<<"\\n";if(hits!=1)return false;').replace('if(spans!=1 || roots!=1', 'std::cout<<"SPAN/ROOT/W/H/G="<<spans<<"/"<<roots<<"/"<<widths<<"/"<<heights<<"/"<<next.widget_gate<<"\\n";if(spans!=1 || roots!=1')+'\n'+env['fun']('bool gadget_clone_compatible(').replace('if(factories!=1 || fields!=1', 'std::cout<<"FACTORY/FIELD/TYPE="<<factories<<"/"<<fields<<"/"<<types<<"\\n";if(factories!=1 || fields!=1')+'\n'
 # Declarations precede extracted production binders.
 pos=pre.index('bool bind_title_color()')
 new='\n#include "home_dynamic_layout.h"\nhome_dynamic::ColorLayout g_title_color_layout;home_dynamic::ModelLayout g_title_model_layout;\n'
 src=(H/'home_layout_hooks.cpp').read_text(encoding='utf8');aa=src.index('int64_t hc_title_color_frame');bb=src.index('void hc_drawer_title_entry',aa);new+=src[aa:bb]+'\n'
 pre=pre[:pos]+new+pre[pos:]
 pre+='\nint main(){unsigned checks=0,failed=0;auto ok=[&](bool v){++checks;failed+=!v;if(!v)std::cout<<"FAIL="<<checks<<"\\n";};\n'
 for n,(v,z) in env['sym'].by_name.items():
  if n in env['names']:pre+=f'symbols["{n}"]={{{v},{z}}};\n'
 for v,words in env['images'].items():pre+=f'images[{v}]={{{",".join(hex(w) for w in words)}}};\n'
 for v,z in env['spans'].items():pre+=f'spans[{v}]={z};\n'
 pre+='ok(bind_title_color());std::cout<<"COLOR="<<g_title_color_bound<<"\\n";ok(bind_title_custom());std::cout<<"CUSTOM="<<g_title_custom_bound<<"\\n";ok(bind_drawer_title());ok(bind_desktop_title());ok(bind_title_hide());\n'
 vals=[env['sym'].by_name[n][0] for n in ['AssistantDragDataHelper.putWidgetIntoBundle','BundleImpl.putInt','AssistantDragToPAHandler.canDragToPA','AssistantDragToPAHandler._isSpanSupportedByPa','GadgetInfoModel.cloneModel']]
 v,put,can,span,clone=vals
 pre+=f'home_layout::GadgetAnchors ga;ok(gadget_scan_anchors({v},spans[{v}],{put},{can},spans[{can}],{span},ga));uint32_t cid=0,field=0;ok(gadget_clone_compatible({clone},spans[{clone}],&cid,&field));std::cout<<"GADGET="<<ga.ok<<" cid="<<cid<<" field="<<field<<"\\n";'
 pre+=f'IndicatorPolicyAnchors anchors;ok(indicator_policy_compatible({env["sym"].by_name["LauncherIndicatorState._buildScreenIndicator"][0]},0,&anchors));'
 for name,occupied in [('GridCellDelegate.performLayout',False),('GridOccupiedCellDelegate.performLayout',True),('HotSeatLayoutDelegate.cellLayout',None)]:
  va=env['sym'].by_name[name][0]
  call=f'home_dynamic::workspace_frame(images[{va}],{str(occupied).lower()},f,p)' if occupied is not None else f'home_dynamic::hotseat_frame(images[{va}],f,p)'
  pre+=f'{{home_dynamic::GeometryFrame f;uint32_t p=0;ok({call});std::cout<<"GEOMETRY={name} "<<p<<"\\n";}}'
  call_shift=call.replace(f'images[{va}]','changed')
  pre+=f'{{auto changed=images[{va}];for(auto &word:changed)if((word&0x3b200c00u)==0x38000000u && home_dynamic::rn(word)==29 && home_dynamic::imm9(word)<0){{int32_t off=home_dynamic::imm9(word)-8;word=(word&~0x1ff000u)|((off&511)<<12);}}home_dynamic::GeometryFrame f;uint32_t p=0;ok({call_shift});}}'
 # Production phone-column consumer, executed on both real handlers and a moved field.
 plan=(H/'tweaks/ht_plan.cpp').read_text(encoding='utf8');a=plan.index('static bool PhoneColumnOffset');b=plan.index('bool LocateFeature19',a)
 helper=plan[a:b]
 declare='struct CodeView {const std::vector<uint32_t>&body;uint32_t base;bool Word(uint32_t va,uint32_t*w)const{if(va<base || ((va-base)&3) || (va-base)/4>=body.size())return false;*w=body[(va-base)/4];return true;}};\n'+helper
 pre=pre.replace('\nint main(){', '\n'+declare+'\nint main(){')
 phone=env['sym'].by_name['PhoneRowColHandler.handle'][0]
 pre+=f'{{uint32_t field=0;auto body=images[{phone}];ok(PhoneColumnOffset(CodeView{{body,{phone}}},{phone},body.size()*4,&field)&&field==19);for(auto&w:body)if((w&0x3b200c00u)==0x38000000u && home_dynamic::imm9(w)==19)w+=8<<12;ok(PhoneColumnOffset(CodeView{{body,{phone}}},{phone},body.size()*4,&field)&&field==27);body.insert(body.end(),body.begin()+0x200/4,body.begin()+0x20c/4);ok(!PhoneColumnOffset(CodeView{{body,{phone}}},{phone},body.size()*4,&field));}}'
 # Shift all native frame slots in the actual builders; published replay operands
 # must change with them. Pool slots and allocator CIDs are mutated independently.
 for name in ['ShortcutIconWidget._buildTextWidget','AppIcon.build']:
  va=env['sym'].by_name[name][0]
  pre+=f'{{auto original=images[{va}];for(auto &word:images[{va}])if((word&0x3b200c00u)==0x38000000u && home_dynamic::rn(word)==29 && home_dynamic::imm9(word)<0){{int32_t off=home_dynamic::imm9(word)-8;word=(word&~0x1ff000u)|((off&511)<<12);}}'
  if name=='AppIcon.build':pre+='g_drawer_title_bound=false;ok(bind_drawer_title());ok(hc_drawer_font_widget==-16&&hc_drawer_height_widget==-24);'
  else:pre+='g_title_color_bound=g_title_custom_bound=g_desktop_title_bound=false;ok(bind_title_color());ok(bind_title_custom());ok(bind_desktop_title());ok(hc_title_custom_widget_frame==-32&&hc_title_custom_label_frame==-64);'
  pre+=f'images[{va}]=original;}}'
 va=env['sym'].by_name['ShortcutIconWidget.getPrefixAssetName'][0]
 pre+=f'{{auto original=images[{va}];for(auto &word:images[{va}])if(home_dynamic::ux(word,27,1))word+=1<<10;g_title_hide_bound=false;ok(bind_title_hide());ok(hc_title_empty_pool==0x100);images[{va}]=original;}}'
 v,put,can,span,clone=vals
 pre+=f'{{auto original=images[{v}],gate=images[{can}];for(auto &word:images[{v}])if((word&0x3b200c00u)==0x38000000u && home_dynamic::rn(word)==29 && home_dynamic::imm9(word)<0){{int32_t off=home_dynamic::imm9(word)-8;word=(word&~0x1ff000u)|((off&511)<<12);}}for(auto &word:images[{can}])if((word&0x3b200c00u)==0x38000000u && home_dynamic::rn(word)==29 && home_dynamic::imm9(word)<0){{int32_t off=home_dynamic::imm9(word)-8;word=(word&~0x1ff000u)|((off&511)<<12);}}home_layout::GadgetAnchors moved;ok(gadget_scan_anchors({v},spans[{v}],{put},{can},spans[{can}],{span},moved));ok(moved.model_slot==ga.model_slot-8&&moved.bundle_slot==ga.bundle_slot-8&&moved.handler_slot==ga.handler_slot-8);images[{v}]=original;images[{can}]=gate;}}'
 pre+=f'{{uint32_t factory=0;ok(home_dynamic::target(images[{clone}][7],{clone}+28,factory));auto original=images[factory];images[factory][1]+=256<<5;uint32_t newcid=0,newfield=0;ok(gadget_clone_compatible({clone},spans[{clone}],&newcid,&newfield));ok(newcid==cid+4096);images[factory]=original;}}'
 policy=env['sym'].by_name['LauncherIndicatorState._buildScreenIndicator'][0]
 pre+=f'{{auto original=images[{policy}];auto &body=images[{policy}];body[anchors.result/4+3]+=1<<10;IndicatorPolicyAnchors shifted;ok(indicator_policy_compatible({policy},0,&shifted));body[anchors.result/4+2]^=1<<5;ok(!indicator_policy_compatible({policy},0,&shifted));body=original;}}'
 pre+='resolve_grid_fields("CellLayoutGetxController.calculateCenterGlobalPosition");ok(g_grid_field.usable());'
 for name,kind in [('GridCellDelegate.performLayout',0),('GridOccupiedCellDelegate.performLayout',1),('HotSeatLayoutDelegate.cellLayout',2)]:
  va=env['sym'].by_name[name][0];parse=f'home_dynamic::workspace_frame(images[{va}],{str(kind==1).lower()},f,p)' if kind<2 else f'home_dynamic::hotseat_frame(images[{va}],f,p)'
  pre+=f'{{home_dynamic::GeometryFrame f;uint32_t p=0;ok({parse});'
  pre+=r"""alignas(16) unsigned char stack[4096]{},gridmem[512]{},delegatemem[512]{},infomem[512]{},closuremem[512]{},itemmem[512]{};
uintptr_t fp=reinterpret_cast<uintptr_t>(stack+2048),grid=reinterpret_cast<uintptr_t>(gridmem)+1,delegate=reinterpret_cast<uintptr_t>(delegatemem)+1,info=reinterpret_cast<uintptr_t>(infomem)+1,closure=reinterpret_cast<uintptr_t>(closuremem)+1,item=reinterpret_cast<uintptr_t>(itemmem)+1;
auto heap=grid>>32;auto &field=g_grid_field;using home_layout::workspace_write;using home_layout::workspace_read;
workspace_write(grid,field.columns,int64_t{6});workspace_write(grid,field.rows,int64_t{5});workspace_write(grid,field.cell_width,100.0);workspace_write(grid,field.cell_height,100.0);
workspace_write(info,field.item_col,int64_t{2});workspace_write(info,field.item_row,int64_t{1});double result[4]{};
"""
  if kind==0:pre+='workspace_write(fp,f.grid,grid);workspace_write(fp,f.x,10.0);workspace_write(fp,f.y,20.0);workspace_write(fp,f.width,100.0);workspace_write(fp,f.height,100.0);ok(home_layout::inset_workspace_frame(fp,heap,false,&field,10,20,12,result,nullptr,&f));ok(result[0]==22&&result[1]==30&&result[2]==96&&result[3]==94);'
  elif kind==1:pre+='workspace_write(fp,f.grid,delegate);workspace_write(delegate,field.occupied_grid,static_cast<uint32_t>(grid));workspace_write(fp,f.info,info);workspace_write(fp,f.x,10.0);ok(home_layout::inset_workspace_frame(fp,heap,true,&field,10,20,12,result,nullptr,&f));ok(workspace_read<double>(fp,f.out_x)==214&&workspace_read<double>(fp,f.out_y)==104);'
  else:pre+='workspace_write(fp,f.grid,delegate);workspace_write(delegate,field.dock_columns,int64_t{6});workspace_write(fp,f.closure,closure);workspace_write(closure,field.dock_item,static_cast<uint32_t>(item));workspace_write(item,field.dock_info,static_cast<uint32_t>(info));workspace_write(fp,f.x,100.0);ok(home_layout::inset_hotseat_frame(fp,heap,12,result,&field,&f));ok(workspace_read<double>(fp,f.x)==102);'
  pre+='}'
 pre+=r"""{auto layout=g_title_color_layout;alignas(16) uint64_t original[32]{},nursery[64]{},thread[128]{};original[0]=layout.tag;auto top=reinterpret_cast<uintptr_t>(nursery);auto t=reinterpret_cast<uintptr_t>(thread);std::memcpy(reinterpret_cast<void*>(t+layout.top),&top,8);uint64_t end=top+sizeof(nursery);std::memcpy(reinterpret_cast<void*>(t+layout.end),&end,8);auto value=home_title::clone_color(reinterpret_cast<uintptr_t>(original)+1,t,static_cast<int32_t>(0xffff00ff),layout);ok(value==top+1);double alpha=0,green=1;std::memcpy(&alpha,reinterpret_cast<void*>(value+layout.channels[0]),8);std::memcpy(&green,reinterpret_cast<void*>(value+layout.channels[2]),8);ok(alpha==1&&green==0);}
{auto m=g_title_model_layout;alignas(16) uint64_t modelmem[128]{},intentmem[128]{},componentmem[128]{},packagemem[128]{},dispatchmem[32]{};auto model=reinterpret_cast<uintptr_t>(modelmem)+1,intent=reinterpret_cast<uintptr_t>(intentmem)+1,component=reinterpret_cast<uintptr_t>(componentmem)+1,package=reinterpret_cast<uintptr_t>(packagemem)+1;modelmem[0]=uint64_t(m.dispatch+5)<<12;dispatchmem[5]=0x1111;auto put=[](uintptr_t o,unsigned field,uintptr_t value){uint32_t v=static_cast<uint32_t>(value);std::memcpy(reinterpret_cast<void*>(o+field),&v,4);};put(model,m.flag,0);put(model,m.intent,intent);put(intent,m.component,component);put(component,m.component_package,package);ok(home_title::model_package(model,model>>32,reinterpret_cast<uintptr_t>(dispatchmem),1,0x1111,0x2222,m)==package);dispatchmem[5]=0x2222;put(model,m.pin_name,package);ok(home_title::model_package(model,model>>32,reinterpret_cast<uintptr_t>(dispatchmem),1,0x1111,0x2222,m)==package);dispatchmem[5]=0x3333;ok(home_title::model_package(model,model>>32,reinterpret_cast<uintptr_t>(dispatchmem),1,0x1111,0x2222,m)==0);}
"""
 pk=env['sym'].by_name['ShortcutInfoModel.getPackageName'][0];cb=env['sym'].by_name['ShortcutInfoModel.getComponentName'][0];pin=env['sym'].by_name['PinShortcutInfoModel.getComponentName'][0]
 pre+=f'uint32_t leaf=0;for(unsigned i=0;i<images[{pin}].size();++i)home_dynamic::target(images[{pin}][i],{pin}+i*4,leaf);home_dynamic::ModelLayout m;ok(home_dynamic::model_fields(images[{pk}],images[{cb}],images[leaf],m));\n'
 pre+=f'auto p=images[{pk}],c=images[{cb}],l=images[leaf];'
 pre+='for(auto&w:c){if(home_dynamic::movz(w,17))w+=8<<5;if(home_dynamic::ldw(w,1,2)||home_dynamic::ldw(w,2,0))w+=8<<12;}for(auto&w:p){if(home_dynamic::ldw(w,0,1)||home_dynamic::ldw(w,2,3))w+=8<<12;if((w&0xffc003ffu)==0xd100001eu)w+=8<<10;}for(auto&w:l)if(home_dynamic::movz(w,17))w+=8<<5;home_dynamic::ModelLayout shifted;ok(home_dynamic::model_fields(p,c,l,shifted));ok(shifted.flag==m.flag+8&&shifted.intent==m.intent+8&&shifted.component==m.component+8&&shifted.package==m.package+8&&shifted.component_package==m.component_package+8&&shifted.pin_name==m.pin_name+8&&shifted.dispatch==m.dispatch+8);'
 pre+=f'c=images[{cb}];c[1]^=1;ok(!home_dynamic::model_fields(p,c,l,shifted));'
 pre+=f'std::cout<<"TITLE_DYNAMIC_{version}="<<checks<<" checks; failed="<<failed<<"\\n";return failed?1:0;}}'
 out=D/('host-'+ROOT.name+'-'+version);out.mkdir(exist_ok=True);(out/'test.cpp').write_text(pre,encoding='utf8')
 zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe'
 for cmd in [[str(zig),'c++','-std=c++20','-I'+str(H),str(out/'test.cpp'),'-o',str(out/'test.exe')],[str(out/'test.exe')]]:
  r=subprocess.run(cmd,capture_output=True,text=True);print(r.stdout+r.stderr,end='')
  if r.returncode and cmd[0]==str(zig):sys.exit(r.returncode)
  failed_total|=r.returncode


sys.exit(failed_total)
