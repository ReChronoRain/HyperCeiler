/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_big_folder_size.h"
#include "targets/home/home_grid_autofit.h"
#include "targets/home/home_drop_geometry.h"
#include "targets/home/home_runtime_layout_contracts.h"
#include "targets/home/home_indicator_pair.h"
#include <map>
#include <string>
#include <cassert>
#include <iostream>
#include <limits>
#include "fixture.h"
using namespace home_layout;
bool fixture_body(uint32_t address,home_dynamic::Body& b) {
    for(const auto& [name,at]:addresses)if(address==at&&name.find("anonymous_")!=0){b=images.at(name);return true;}
    for(const auto& [name,at]:addresses)if(address==at){b=images.at(name);return true;}
    return false;
}
namespace home_layout_engine {
bool HomeLayoutEngineFindSymbol(const char* name,uint32_t* va,uint32_t* size) {
    auto at=addresses.find(name);if(at==addresses.end())return false;
    *va=at->second;*size=images.at(name).size()*4;return true;
}
}
namespace dartscan {bool body(uint32_t va,home_dynamic::Body& out){return fixture_body(va,out);}}
bool dart_words(uint32_t va,size_t count,home_dynamic::Body& out) {
    if(!fixture_body(va,out)||out.size()<count)return false;out.resize(count);return true;
}
bool bl_target(uint32_t word,uint32_t pc,uint32_t* out){return home_dynamic::target(word,pc,*out);}
constexpr uint32_t kDartPrologue=0xa9bf79fdu;
#include "indicator-binder.inc"
uint32_t hc_grid_autofit_requested=0;
GridAutofitSites g_grid_autofit_fields;
#include "autofit-body.inc"
static void move_frame(home_dynamic::Body& b,int delta) {
    assert(home_dynamic::frame_size(b));
    // Symbol spans may include anonymous child functions with their own frames.
    for(size_t i=0;i+2<b.size();++i)
        if(b[i]==0xa9bf79fdu&&b[i+1]==0xaa0f03fdu
            &&(b[i+2]&0xffc003ffu)==0xd10001efu) {
            const auto size=(b[i+2]>>10)&4095;
            b[i+2]=(b[i+2]&~0x003ffc00u)|(uint32_t(int(size)-delta)<<10);
        }
    for(auto& w:b)if((w&0x3b200c00u)==0x38000000u&&home_dynamic::rn(w)==29
        &&home_dynamic::imm9(w)<0) {
        const auto off=home_dynamic::imm9(w)+delta;
        w=(w&~0x001ff000u)|((uint32_t(off)&511)<<12);
    }
}
static uint32_t signed_offset(uint32_t word,int32_t offset) {
    assert(offset>=-256&&offset<=255);
    return (word&~0x001ff000u)|((uint32_t(offset)&511)<<12);
}
static void check_indicator_callback(const hc::indicator::InsetsLayout& layout) {
    alignas(16) unsigned char objects[512]{},locals[2048]{};
    const auto edge=uintptr_t(objects)+1,fp=uintptr_t(locals+1024),heap=edge>>32;
    const auto null=edge+256;
    const uint64_t header=(uint64_t(layout.cid)<<12)|((layout.size/16)<<8)|0x1c;
    const double original=96;
    for(double delta:{-630.,-80.,80.,370.}) {
        std::memset(objects,0,sizeof(objects));workspace_write(edge,-1,header);
        workspace_write(edge,layout.field[3],original);workspace_write(fp,layout.source,original);
        unsigned char before[sizeof(objects)];std::memcpy(before,objects,sizeof(objects));
        assert(hc::indicator::inset_original_frame(fp,heap,edge,null,delta,layout));
        assert(workspace_read<double>(edge,layout.field[1])==delta);
        assert(workspace_read<double>(edge,layout.field[3])==original-delta);
        assert(workspace_read<double>(fp,layout.source)==original);
        // No unrelated bytes, frame locals or left/right Insets may change.
        workspace_write(edge,layout.field[1],0.);workspace_write(edge,layout.field[3],original);
        assert(!std::memcmp(before,objects,sizeof(objects)));
    }
    for(double delta:{0.,-631.,371.,std::numeric_limits<double>::infinity()})
        assert(!hc::indicator::inset_original_frame(fp,heap,edge,null,delta,layout));
    workspace_write(fp,layout.source,original+1);
    assert(!hc::indicator::inset_original_frame(fp,heap,edge,null,80,layout));
    workspace_write(fp,layout.source,original);workspace_write(edge,-1,header+(1<<12));
    assert(!hc::indicator::inset_original_frame(fp,heap,edge,null,80,layout));
}
static void check_callbacks(const GridFieldOffsets& fields,const BigFolderSizeSites& big,
    const GridAutofitSites& fit,const DropGeometryContract& drop,const FolderGeometryContract& folder) {
    alignas(16) unsigned char objects[32768]{},locals[8192]{},registers[1024]{};
    const auto base=uintptr_t(objects),fp=uintptr_t(locals+4096),saved=uintptr_t(registers);
    const uintptr_t grid=base+1,origin=base+1025,strategy=base+2049,box=base+3073;
    const uintptr_t thread=base+4096,table=base+8192,controller=base+12289,rx=base+14337;
    const uintptr_t null=base+16385;
    const auto heap=grid>>32;
    workspace_write(grid,fields.columns,int64_t{4});workspace_write(grid,fields.rows,int64_t{6});
    workspace_write(grid,fields.cell_width,90.);workspace_write(grid,fields.cell_height,100.);
    workspace_write(grid,fields.origin,uint32_t(origin));workspace_write(origin,7,20.);
    workspace_write(saved,0,grid);workspace_write(fp,drop.receiver,uintptr_t{123});
    workspace_write(fp,drop.column,int64_t{1});workspace_write(fp,drop.row,int64_t{2});
    workspace_write(fp,drop.width,90.);workspace_write(fp,drop.height,100.);
    assert(drop_geometry_body(fp,heap,saved,0,&fields,10,20,16,nullptr,&drop));
    assert(workspace_read<double>(fp,drop.width)==82&&workspace_read<double>(fp,drop.height)==95);
    assert(workspace_read<double>(saved,160)==36&&workspace_read<uintptr_t>(saved,0)==1);
    workspace_write(saved,0,grid);workspace_write(fp,folder.frame[0][0],uintptr_t{456});
    assert(folder_geometry_body(fp,heap,saved,0,10,20,16,nullptr,&fields,&folder));
    assert(workspace_read<double>(fp,folder.frame[0][3])==82
        &&workspace_read<double>(fp,folder.frame[0][4])==95
        &&workspace_read<double>(fp,folder.frame[0][5])==10
        &&workspace_read<uintptr_t>(saved,0)==456);
    workspace_write(thread,big.table,table);workspace_write(table,big.singleton,controller);
    workspace_write(table,big.device_singleton,controller);
    workspace_write(controller,big.config_rx,uint32_t(rx));workspace_write(rx,big.rx_value,uint32_t(grid));
    workspace_write(strategy,big.hotseat,uint32_t(null+0x30));workspace_write(box,7,90.);
    workspace_write(saved,16,strategy);workspace_write(saved,24,box);workspace_write(saved,160,100.);
    big_folder_size_body(fp,heap,saved,0,thread,null,big,fields,36,true,0x100000,0x110000);
    assert(workspace_read<double>(fp,big.producer_width)==72
        &&workspace_read<double>(fp,big.producer_height)==80
        &&workspace_read<double>(strategy,big.strategy_width)==72);
    const auto parent=fp+512;
    workspace_write(parent,big.producer_width,72.);workspace_write(parent,big.producer_box,box);
    workspace_write(parent,big.receiver[0],strategy);workspace_write(fp,0,parent);
    workspace_write(fp,8,uintptr_t{0x100000+big.base_return});workspace_write(fp,big.receiver[1],strategy);
    workspace_write(saved,0,grid);
    big_folder_size_body(fp,heap,saved,1,thread,null,big,fields,36,true,0x100000,0x110000);
    assert(workspace_read<double>(fp,big.base_local[0])==72);
    workspace_write(fp,8,uintptr_t{0x100000+big.helper_return[0]});
    workspace_write(fp,big.helper_receiver[0],strategy);workspace_write(saved,0,box);
    workspace_write(saved,160,2.);
    big_folder_size_body(fp,heap,saved,5,thread,null,big,fields,36,true,0x100000,0x110000);
    assert(workspace_read<double>(fp,big.helper_width_local[0])==72);
    g_grid_autofit_fields=fit;hc_grid_autofit_requested=1|(6<<8);
    workspace_write(fp,fit.owner[0],grid);workspace_write(grid,fit.legacy_dock,90.);
    workspace_write(fp,fit.legacy_total,90.);workspace_write(fp,fit.legacy_used,0.);
    workspace_write(fp,fit.legacy_remaining,60.);workspace_write(saved,0,uint64_t{7});
    assert(hc_grid_autofit_body(saved,fp,0)==1);
    assert(workspace_read<double>(grid,fit.legacy_height)==100.);
}
int main() {
    auto body=[](const char* n){return images.at(n);};
    auto va=[](const char* n){return addresses.at(n);};
    auto grid=read_grid_field_offsets(body("CellLayoutGetxController.calculateCenterGlobalPosition"));
    read_grid_cell_size(body("GridCellDelegate.performLayout"),grid);
    assert(read_grid_counts(body("CellLayoutGetxController.isItemPosEmpty"),va("CellLayoutGetxController.isItemPosEmpty"),va("GridController.currentConfig"),grid));
    assert(read_occupied_fields(body("GridOccupiedCellDelegate.performLayout"),grid));
    assert(grid.usable());
    std::array<home_dynamic::Body,5> b={body("BigFolderCommonStrategy.calItemSize"),
        body("FolderLayoutStrategy.calItemSize"),body("FolderIconGetxController.folderIconSize"),
        body("GridController.currentConfig"),body("RxObjectMixin.value")};
    const auto check=body("FolderIconGetxController.checkOrUpdateStrategy");
    std::array<home_dynamic::Body,2> helpers={body("BigFolderCommonStrategy._calItemSizeByWidth"),body("BigFolderCommonStrategy._calItemSizeByHeight")};
    const uint32_t helper_va[]={va("BigFolderCommonStrategy._calItemSizeByWidth"),va("BigFolderCommonStrategy._calItemSizeByHeight")};
    BigFolderSizeSites f;
    assert(big_folder_size_sites(b,check,grid,f));
    assert(big_folder_preview_sites(b[0],va("BigFolderCommonStrategy.calItemSize"),helpers,helper_va,f));
    std::cout<<"runtime big-folder baseline: PASS\n";
    GridAutofitSites fit;
    assert(grid_autofit_sites(body("GridSizeCalRules._calVariableHeight"),
        body("GridSizeCalRules.calVarCellHeight"),body("PhoneCellSizeHandler.calVariableValues"),fit));
    assert(fit.owner[0]==-8&&fit.owner[1]==-8&&fit.legacy_total==-48
        &&fit.legacy_used==-56&&fit.legacy_remaining==-40);
    std::cout<<"runtime grid-autofit baseline: PASS\n";
    DropGeometryContract drop;
    assert(drop_geometry_contract(body("CellLayoutGetxController.calculateCenterGlobalPosition"),
        va("CellLayoutGetxController.calculateCenterGlobalPosition"),va("GridController.currentConfig"),
        va("RxObjectMixin.value"),grid,drop));
    assert(drop.width==-56&&drop.height==-64&&drop.column==-16&&drop.row==-24&&drop.receiver==-8);
    std::cout<<"runtime drop geometry baseline: PASS\n";
    auto read=fixture_body;
    runtime_contract::ScalarSource scalar;
    assert(runtime_contract::layout_scalar(body("WidgetPositionUtil._getCellRectInHotSeatPad"),
        va("WidgetPositionUtil._getCellRectInHotSeatPad"),va("GridController.currentConfig"),
        va("GridController.screenMarginBottom"),true,read,scalar));
    assert(scalar.target==va("GridController.hotSeatsMarginBottom"));
    assert(runtime_contract::layout_scalar(body("FolderGridView._buildScrollableGrid"),
        va("FolderGridView._buildScrollableGrid"),va("GridController.currentConfig"),0,false,read,scalar));
    assert(scalar.target==va("FolderGridViewGetxController.folderCellHeight"));
    uint32_t ratio=0;
    const char* api="RustLibApiImpl.crateApiSceneAnimationTransitionAnimDurationApiGetAnimDurationRatio";
    assert(runtime_contract::animation_ratio(body("FlightCohort.animDurationRatio"),
        va("FlightCohort.animDurationRatio"),va(api),body(api),read,ratio));
    assert(ratio==va("getAnimDurationRatio"));
    assert(runtime_contract::animation_magic(body("Utilities.getDefaultGestureAnimMagicSpeed"),
        va("Utilities.getDefaultGestureAnimMagicSpeed"),va("DeviceLevelUtils.isHighAnimationRate"),va("DeviceLevelUtils.isLowAnimationRate")));
    std::cout<<"runtime scalar consumers and animation return chains: PASS\n";
    assert(runtime_contract::new_install_methods(body("FolderInfoModel.hasNewInstalledApp"),
        body("ShortcutIconWidget._addNewInstallLight")));
    std::cout<<"runtime new-install appearance methods: PASS\n";
    const uint32_t folder_va[]={va("WidgetPositionUtil.getCellPosition"),va("FolderIconGetxController.calOriginPreviewIconLoc")};
    std::array<home_dynamic::Body,2> folder_bodies={body("WidgetPositionUtil.getCellPosition"),body("FolderIconGetxController.calOriginPreviewIconLoc")};
    FolderGeometryCalls calls{va("GridController.currentConfig"),va("Inst|find"),
        va("WidgetPositionUtil.transformPointX"),va("RxObjectMixin.value"),va("SwipeController.isRtl")};
    const auto cfg=body("GridController.currentConfig");calls.table_word=cfg[3];calls.singleton_word=cfg[4];
    const auto translate=body("Offset.translate");
    for(size_t i=0;i+4<translate.size();++i)if(translate[i+2]==0xfc007000u&&translate[i+4]==0xfc00f000u)
        assert(home_dynamic::target(translate[i],va("Offset.translate")+i*4,calls.offset));
    auto prefix=[&](uint32_t address,size_t count,home_dynamic::Body& out){
        if(!read(address,out)||out.size()<count)return false;out.resize(count);return true;
    };
    FolderGeometryContract folder;
    assert(folder_geometry_contract(folder_bodies,folder_va,grid,calls,folder,prefix));
    assert(folder.frame[0][0]==-8&&folder.frame[0][5]==-48&&folder.frame[1][3]==-32);
    std::cout<<"runtime folder geometry frame roles: PASS\n";
    check_callbacks(grid,f,fit,drop,folder);
    auto mb=b;auto mh=helpers;auto mfolder=folder_bodies;
    for(auto& v:mb)move_frame(v,-16);
    for(auto& v:mh)move_frame(v,-16);
    for(auto& v:mfolder)move_frame(v,-16);
    auto old=body("GridSizeCalRules._calVariableHeight"),handler=body("PhoneCellSizeHandler.calVariableValues");
    auto dropped=body("CellLayoutGetxController.calculateCenterGlobalPosition");
    move_frame(old,-16);move_frame(handler,-16);move_frame(dropped,-16);
    BigFolderSizeSites bf;GridAutofitSites af;DropGeometryContract df;FolderGeometryContract ff;
    assert(big_folder_size_sites(mb,check,grid,bf));
    assert(big_folder_preview_sites(mb[0],va("BigFolderCommonStrategy.calItemSize"),mh,helper_va,bf));
    assert(grid_autofit_sites(old,body("GridSizeCalRules.calVarCellHeight"),handler,af));
    assert(drop_geometry_contract(dropped,va("CellLayoutGetxController.calculateCenterGlobalPosition"),
        va("GridController.currentConfig"),va("RxObjectMixin.value"),grid,df));
    assert(folder_geometry_contract(mfolder,folder_va,grid,calls,ff,prefix));
    assert(bf.producer_width==f.producer_width-16&&bf.producer_box==f.producer_box-16
        &&bf.base_local[1]==f.base_local[1]-16&&bf.size_edit==f.size_edit-16);
    assert(af.legacy_total==fit.legacy_total-16&&df.width==drop.width-16&&ff.frame[0][5]==folder.frame[0][5]-16);
    check_callbacks(grid,bf,af,df,ff);
    std::cout<<"runtime frame relocation and shipping C++ replays: PASS\n";
    hc::indicator::InsetsLayout insets;uint32_t patch=0,cid=0;
    assert(hc::indicator::inset_fields(body("EdgeInsets.deflateRect"),insets.field));
    assert(capsule_wrapper_layout_compatible(va("Workspace._createIndicator"),0,&patch,&cid,&insets));
    assert(insets.source==-72&&insets.root==-8&&insets.field[1]==15&&insets.field[3]==31&&insets.cid==cid);
    check_indicator_callback(insets);
    std::cout<<"runtime original indicator binder and Insets field roles: PASS\n";
    // A plausible opcode sequence with a broken live alias is not a contract.
    auto bad_big=b;
    bad_big[0][f.site[0]/4+1]=signed_offset(bad_big[0][f.site[0]/4+1],f.receiver[0]);
    assert(!big_folder_size_sites(bad_big,check,grid,bf));
    auto bad_fit=old;
    bad_fit[af.legacy/4]=signed_offset(bad_fit[af.legacy/4],af.legacy_total);
    assert(!grid_autofit_sites(bad_fit,body("GridSizeCalRules.calVarCellHeight"),handler,af));
    auto bad_drop=dropped;
    bad_drop[df.site[0]/4+3]=signed_offset(bad_drop[df.site[0]/4+3],df.row);
    assert(!drop_geometry_contract(bad_drop,va("CellLayoutGetxController.calculateCenterGlobalPosition"),
        va("GridController.currentConfig"),va("RxObjectMixin.value"),grid,df));
    bad_drop=dropped;
    bad_drop[3]=0x14000000u|((df.site[0]/4+1-3)&0x03ffffffu);
    assert(!drop_geometry_contract(bad_drop,va("CellLayoutGetxController.calculateCenterGlobalPosition"),
        va("GridController.currentConfig"),va("RxObjectMixin.value"),grid,df));
    auto bad_folder=mfolder;
    bad_folder[0][ff.site[0]/4+3]=signed_offset(bad_folder[0][ff.site[0]/4+3],ff.frame[0][1]);
    assert(!folder_geometry_contract(bad_folder,folder_va,grid,calls,ff,prefix));
    auto magic=body("Utilities.getDefaultGestureAnimMagicSpeed");
    assert(!runtime_contract::animation_magic(magic,va("Utilities.getDefaultGestureAnimMagicSpeed"),
        va("DeviceLevelUtils.isLowAnimationRate"),va("DeviceLevelUtils.isHighAnimationRate")));
    for(auto& word:magic)if(word==0x1e620820u)word=0x1e622820u; // FMUL -> FADD
    assert(!runtime_contract::animation_magic(magic,va("Utilities.getDefaultGestureAnimMagicSpeed"),
        va("DeviceLevelUtils.isHighAnimationRate"),va("DeviceLevelUtils.isLowAnimationRate")));
    auto wrong_api=body(api);
    for(auto& word:wrong_api)if(word==0xfc407000u)word=0xf8407000u; // boxed double -> integer
    assert(!runtime_contract::animation_ratio(body("FlightCohort.animDurationRatio"),
        va("FlightCohort.animDurationRatio"),va(api),wrong_api,read,ratio));
    assert(!runtime_contract::layout_scalar(body("WidgetPositionUtil._getCellRectInHotSeatPad"),
        va("WidgetPositionUtil._getCellRectInHotSeatPad"),va("GridController.currentConfig")+4,
        va("GridController.screenMarginBottom"),true,read,scalar));
    auto& margin_source=images.at("GridController.hotSeatsMarginBottom");
    const auto original_margin=margin_source;
    for(auto& word:margin_source)if(word==0x1e614001u)word=0x1e604001u; // FNEG -> FMOV breaks source transform
    assert(!runtime_contract::layout_scalar(body("WidgetPositionUtil._getCellRectInHotSeatPad"),
        va("WidgetPositionUtil._getCellRectInHotSeatPad"),va("GridController.currentConfig"),
        va("GridController.screenMarginBottom"),true,read,scalar));
    margin_source=original_margin;
    auto light=body("ShortcutIconWidget._addNewInstallLight");
    for(auto& word:light)if((word&0xffe00fffu)==0xb8000020u)word=0xd503201fu;
    assert(!runtime_contract::new_install_methods(body("FolderInfoModel.hasNewInstalledApp"),light));
    std::cout<<"runtime wrong aliases, branch entry, scalar producers and return types rejected: PASS\n";

    // Relocate the live Insets class, allocation size, all four fields and every
    // owning frame (including the anonymous Workspace child). No old offset is used.
    const auto original_images=images;
    auto& workspace=images.at("Workspace._createIndicator");
    const auto initializer=(patch-va("Workspace._createIndicator"))/4-6;
    uint32_t factory=0;
    assert(home_dynamic::target(workspace[initializer-1],va("Workspace._createIndicator")+(initializer-1)*4,factory));
    for(unsigned at=1;at<=3;++at)workspace[initializer+at]=signed_offset(workspace[initializer+at],home_dynamic::imm9(workspace[initializer+at])+16);
    workspace[initializer+5]=signed_offset(workspace[initializer+5],insets.field[3]+16);
    for(auto& word:images.at("EdgeInsets.deflateRect"))
        if(home_dynamic::ldd(word,1,1))word=signed_offset(word,home_dynamic::imm9(word)+16);
    for(auto& [name,at]:addresses)if(at==factory) {
        auto& allocator=images.at(name);uint32_t tag=0;assert(home_dynamic::tag(allocator,tag));
        tag=((insets.cid+37)<<12)|(4<<8)|(tag&255);
        allocator[0]=(allocator[0]&~0x001fffe0u)|((tag&65535)<<5);
        allocator[1]=(allocator[1]&~0x001fffe0u)|((tag>>16)<<5);
    }
    move_frame(workspace,-16);
    hc::indicator::InsetsLayout relocated;
    assert(capsule_wrapper_layout_compatible(va("Workspace._createIndicator"),0,&patch,&cid,&relocated));
    assert(relocated.source==insets.source-16&&relocated.root==insets.root-16
        &&relocated.cid==insets.cid+37&&relocated.size==64);
    for(unsigned i=0;i<4;++i)assert(relocated.field[i]==insets.field[i]+16);
    check_indicator_callback(relocated);
    workspace[initializer+1]=signed_offset(workspace[initializer+1],relocated.field[3]);
    assert(!capsule_wrapper_layout_compatible(va("Workspace._createIndicator"),0,&patch,&cid,&relocated));
    images=original_images;
    std::cout<<"runtime indicator field/class/size/frame relocation and refusal: PASS\n";
    // Every BL remains relative when the whole image moves. Owner addresses are
    // fixture truth only; the runtime contracts must follow the current image.
    const auto original_addresses=addresses;
    for(auto& [name,address]:addresses)address+=0x10000;
    assert(runtime_contract::layout_scalar(body("WidgetPositionUtil._getCellRectInHotSeatPad"),
        va("WidgetPositionUtil._getCellRectInHotSeatPad"),va("GridController.currentConfig"),
        va("GridController.screenMarginBottom"),true,read,scalar));
    assert(scalar.target==va("GridController.hotSeatsMarginBottom"));
    assert(runtime_contract::animation_ratio(body("FlightCohort.animDurationRatio"),
        va("FlightCohort.animDurationRatio"),va(api),body(api),read,ratio));
    assert(ratio==va("getAnimDurationRatio"));
    assert(capsule_wrapper_layout_compatible(va("Workspace._createIndicator"),0,&patch,&cid,&insets));
    addresses=original_addresses;
    auto duplicate=body("WidgetPositionUtil._getCellRectInHotSeatPad");
    const auto original_duplicate=duplicate;
    duplicate.insert(duplicate.end(),original_duplicate.begin(),original_duplicate.end());
    // Re-encode the copied owner's calls at its new PC, keeping their real targets.
    const size_t original_size=duplicate.size()/2;
    for(size_t i=0;i<original_size;++i)if((duplicate[i]&0xfc000000u)==0x94000000u)
        duplicate[original_size+i]=0x94000000u|((duplicate[i]-uint32_t(original_size))&0x03ffffffu);
    assert(!runtime_contract::layout_scalar(duplicate,va("WidgetPositionUtil._getCellRectInHotSeatPad"),
        va("GridController.currentConfig"),va("GridController.screenMarginBottom"),true,read,scalar));
    std::cout<<"runtime image rebase and ambiguous consumer rejection: PASS\n";
}
