/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_folder_geometry.h"
#include "home_dynamic_layout.h"
#include <algorithm>
#include <array>

namespace home_layout {
// These are original-body consumers, not small accessor hooks (which AOT inlines).
// All instruction locations and object fields are recovered from owning functions.
struct BigFolderSizeSites {
    uint32_t site[7]{};
    uint32_t helper_return[2]{};
    int helper_width_local[2]{};
    int table = -1, singleton = -1, device_singleton = -1, config_rx = -1, rx_value = -1;
    int hotseat = -1, folder_info = -1, span_x = -1, span_y = -1;
    int strategy_width = -1, strategy_height = -1;
    int32_t receiver[3]{},producer_box=0,producer_width=0,producer_height=0;
    int32_t base_local[2]{},size_edit=0,helper_receiver[2]{},helper_input[2]{};
    uint32_t base_return = 0;
};
inline bool big_folder_sequence(const std::vector<uint32_t>& b,
    const std::vector<uint32_t>& words, uint32_t& off) {
    unsigned hits=0;
    for (size_t i=0;i+words.size()<=b.size();++i)
        if (std::equal(words.begin(),words.end(),b.begin()+i)) {off=uint32_t(i*4);++hits;}
    return hits==1;
}
inline bool big_folder_store(uint32_t w, unsigned rn, unsigned rt, int& off) {
    if((w&0xffe00fffu)!=(0xfc000000u|(rn<<5)|rt))return false;
    unsigned raw=(w>>12)&511;off=raw&256?int(raw)-512:int(raw);
    // Dart objects are 8-byte aligned, but compressed-pointer layouts can put
    // an unboxed double on a 4-byte boundary. LDUR/STUR and memcpy support both.
    // Do not reject the whole size bank merely because a field moved by four.
    return off>=7&&((off+1)&3)==0; // Payload must stay beyond the 8-byte header.
}
inline bool big_folder_window(const std::vector<uint32_t>& b, uint32_t at) {
    if(at%4||at/4+4>b.size())return false;
    for(size_t i=0;i<b.size();++i){uint32_t w=b[i];int32_t d=0;bool branch=false;
        if((w&0x7c000000)==0x14000000){d=int32_t(w<<6)>>6;branch=true;}
        else if((w&0xff000010)==0x54000000||(w&0x7e000000)==0x34000000){d=int32_t(w<<8)>>13;branch=true;}
        else if((w&0x7e000000)==0x36000000){d=int32_t(w<<13)>>18;branch=true;}
        if(i>=at/4&&i<at/4+4&&(branch||(w&0xfffffc1f)==0xd63f0000))return false;
        if(branch){int64_t target=int64_t(i*4)+int64_t(d)*4;if(target>at&&target<at+16)return false;}
    }return true;
}
inline int64_t big_folder_cond_target(uint32_t w, size_t index, unsigned condition) {
    if((w&0xff00001fu)!=(0x54000000u|condition))return -1;
    return int64_t(index)+(int32_t(w<<8)>>13);
}
inline bool big_folder_late_error(const std::vector<uint32_t>& b,int64_t at){
    if(at<0||size_t(at)+1>=b.size())return false;
    // Late-init errors load their original pool name, then retain the original throw BL.
    if((b[at]&0xffc003ff)==0xf9400369)return dart_is_bl(b[at+1]);
    return size_t(at)+2<b.size()&&(b[at]&0xffc003ff)==0x91400369
        &&(b[at+1]&0xffc003ff)==0xf9400129&&dart_is_bl(b[at+2]);
}
inline bool big_folder_size_sites(const std::array<std::vector<uint32_t>,5>& b,
    const std::vector<uint32_t>& check, const GridFieldOffsets& f, BigFolderSizeSites& out) {
    out={}; if(!f.usable())return false;
    // Producer, padding consumer, close-animation size consumer, currentConfig, Rx.value.
    using namespace home_dynamic;
    for(unsigned i=0;i<5;++i)if(b[i].size()<24||!frame_size(b[i]))return false;
    for(unsigned i=0;i<3;++i)if(!receiver_slot(b[i],1,out.receiver[i]))return false;
    unsigned producer_hits=0,tail_hits=0;
    for(size_t i=3;i+5<b[0].size();++i){int width=0,height=0,width_alias=0,height_alias=0;
        if(!ldx(b[0][i-3],29,2)||imm9(b[0][i-3])!=out.receiver[0]
            ||!stx(b[0][i-2],29,3)||!std_(b[0][i-1],29,0)
            ||b[0][i]!=0xfc407061||!std_(b[0][i+1],29,1)
            ||!big_folder_store(b[0][i+2],2,1,width)||!big_folder_store(b[0][i+3],2,0,height)
            ||!big_folder_store(b[0][i+4],2,1,width_alias)||!big_folder_store(b[0][i+5],2,0,height_alias))continue;
        const int offsets[]={width,height,width_alias,height_alias};bool distinct=true;
        for(unsigned a=0;a<4;++a)for(unsigned c=a+1;c<4;++c)
            distinct &= std::abs(offsets[a]-offsets[c])>=int(sizeof(double));
        if(!distinct)continue;
        out.site[0]=uint32_t(i*4);out.strategy_width=width;out.strategy_height=height;
        out.producer_box=imm9(b[0][i-2]);out.producer_height=imm9(b[0][i-1]);
        out.producer_width=imm9(b[0][i+1]);++producer_hits;
    }
    for(size_t i=0;i+4<b[0].size();++i)if(dart_is_bl(b[0][i])&&b[0][i+1]==0xaa1603e0
        &&b[0][i+2]==0xaa1d03ef&&b[0][i+3]==0xa8c179fd&&b[0][i+4]==0xd65f03c0){out.base_return=uint32_t((i+1)*4);++tail_hits;}
    if(producer_hits!=1||tail_hits!=1||out.base_return<=out.site[0]+16)return false;
    for(auto local:{out.producer_box,out.producer_height,out.producer_width})
        if(!frame_slot(b[0],local)||local==out.receiver[0])return false;
    if(out.producer_box==out.producer_width||out.producer_box==out.producer_height
        ||out.producer_width==out.producer_height)return false;
    // The decoded boxed input is a live GC root; the width local carries our scalar.
    // No native pointer cache crosses a Dart call. Reject writes that change either lifetime.
    for(size_t i=out.site[0]/4+4;i<out.base_return/4-1;++i){uint32_t w=b[0][i];
        if(((w>>5)&31)!=29)continue;
        if((w&0xffe00c00)==0xf8000000||(w&0xffe00c00)==0xb8000000||(w&0xffe00c00)==0xfc000000){
            int off=int((w>>12)&511);if(off&256)off-=512;
            if(off==out.receiver[0]||off==out.producer_box||off==out.producer_width)return false;}
    }
    auto load=[](int imm){return 0xfc400000u|(uint32_t(imm)<<12);};
    // Require the same live config fields as the workspace's actual layout contract.
    uint32_t width=0,height=0;
    for(size_t i=0;i+4<=b[1].size();++i) {
        if(b[1][i]==load(f.cell_width)&&std_(b[1][i+1],29,0)
            &&b[1][i+2]==b[3][3]&&b[1][i+3]==b[3][4]){
            out.site[1]=i*4;out.base_local[0]=imm9(b[1][i+1]);++width;}
        if(b[1][i]==load(f.cell_height)&&std_(b[1][i+1],29,0)
            &&b[1][i+2]==b[3][3]&&(b[1][i+3]&0xffc003ff)==0xf9400000){
            const uint32_t pair[]={b[1][i+2],b[1][i+3]};
            if(std::search(b[0].begin(),b[0].end(),pair,pair+2)==b[0].end())continue;
            out.device_singleton=((b[1][i+3]>>10)&4095)*8;out.site[2]=i*4;
            out.base_local[1]=imm9(b[1][i+1]);++height;}
    }
    if(width!=1||height!=1||out.base_local[0]==out.base_local[1])return false;
    for(auto local:out.base_local)if(!frame_slot(b[1],local)||local==out.receiver[1])return false;
    unsigned sizes[2]{};
    for(size_t i=0;i+3<b[2].size();++i)for(unsigned axis=0;axis<2;++axis)
        if(b[2][i]==load(axis?f.cell_height:f.cell_width)&&ldd(b[2][i+1],29,1)
            &&b[2][i+2]==0x1e610802&&ldx(b[2][i+3],29,0)
            &&imm9(b[2][i+3])==out.receiver[2]) {
            const auto edit=imm9(b[2][i+1]);
            if(!frame_slot(b[2],edit)||edit==out.receiver[2]||(axis&&edit!=out.size_edit))return false;
            out.size_edit=edit;out.site[3+axis]=i*4;++sizes[axis];
        }
    if(sizes[0]!=1||sizes[1]!=1)return false;
    // Decode the non-lazy currentConfig path. Do not invoke Dart or initialize a singleton.
    if((b[3][3]&0xffc003ff)!=0xf9400340 ||(b[3][4]&0xffc003ff)!=0xf9400000)return false;
    out.table=((b[3][3]>>10)&4095)*8;out.singleton=((b[3][4]>>10)&4095)*8;
    unsigned n=0;
    for(size_t i=0;i+2<b[3].size();++i) {
        int32_t off=0;
        if(dart_ldur_w(b[3][i],0,1,&off)&&b[3][i+1]==0x8b1c8021&&dart_is_bl(b[3][i+2])){out.config_rx=off;++n;}
    }
    if(n!=1)return false;
    n=0;
    for(size_t i=0;i+7<b[4].size();++i){
        int32_t off=0;
        if(dart_ldur_w(b[4][i],1,0,&off)&&b[4][i+1]==0x8b1c8000
            &&b[4][i+2]==0xf9402370&&b[4][i+3]==0x6b10001f
            &&big_folder_cond_target(b[4][i+4],i+4,0)>=int64_t(i+8)
            &&big_folder_late_error(b[4],big_folder_cond_target(b[4][i+4],i+4,0))
            &&b[4][i+5]==0xaa1d03ef&&b[4][i+6]==0xa8c179fd&&b[4][i+7]==0xd65f03c0){out.rx_value=off;++n;}
    }
    if(n!=1||out.config_rx<=0||out.rx_value<=0)return false;
    // Prove the entire dock-type classification feeding the compare, not one CMP word.
    const uint32_t dock_compare[]={0xb843f002,0x8b1c8042,0xb8483020,0x8b1c8000,0xf9402370,0x6b10001f,0x54000c20,0xf8447003,0xb101947f,0x540000e0,0xb101987f,0x540000a0,0xb1019c7f,0x54000060,0xb101a47f,0x54000061,0x910082c0,0x14000005,0xb101a87f,0x910082d0,0x9100c2d1,0x9a910200,0x6b00005f,0x54000920};
    unsigned dock_hits=0;int compare_info=0;
    for(size_t i=0;i+24<=check.size();++i){int hotseat=0,info=0,type=0;
        if(!dart_ldur_w(check[i],0,2,&hotseat)||!dart_ldur_w(check[i+2],1,0,&info)
            ||!dart_ldur_x(check[i+7],0,3,&type))continue;
        bool match=true;
        for(unsigned j=0;j<24;++j)if(j!=0&&j!=2&&j!=6&&j!=7&&j!=9&&j!=11&&j!=13&&j!=15&&j!=17&&j!=23)
            match &= check[i+j]==dock_compare[j];
        for(unsigned j:{9u,11u,13u})match &= big_folder_cond_target(check[i+j],i+j,0)==int64_t(i+16);
        match &= big_folder_cond_target(check[i+15],i+15,1)==int64_t(i+18);
        const uint32_t join=check[i+17];
        match &= (join&0xfc000000)==0x14000000
            &&int64_t(i+17)+(int32_t(join<<6)>>6)==int64_t(i+22);
        const auto error=big_folder_cond_target(check[i+6],i+6,0);
        match &= error>=int64_t(i+24)&&big_folder_late_error(check,error);
        const auto done=big_folder_cond_target(check[i+23],i+23,0);
        match &= done>=int64_t(i+24)&&done+3<int64_t(check.size());
        if(match)match &= check[done]==0xaa1603e0&&check[done+1]==0xaa1d03ef
            &&check[done+2]==0xa8c179fd&&check[done+3]==0xd65f03c0;
        if(match){out.hotseat=hotseat;compare_info=info;++dock_hits;}
    }
    if(dock_hits!=1||out.hotseat<=0)return false;
    // FolderInfo and spans are read by this SAME size consumer.
    n=0;
    for(size_t i=1;i+1<b[2].size();++i){int32_t off=0;
        if(stx(b[2][i-1],29,1)&&imm9(b[2][i-1])==out.receiver[2]
            &&dart_ldur_w(b[2][i],1,0,&off)&&b[2][i+1]==0x8b1c8000){out.folder_info=off;++n;}}
    if(n!=1)return false;
    n=0;
    for(size_t i=0;i+2<b[2].size();++i){int32_t off=0;
        if(dart_ldur_x(b[2][i],1,2,&off)&&b[2][i+1]==0x9e620040&&b[2][i+2]==0x1e600843){out.span_x=off;++n;}
        if(dart_ldur_x(b[2][i],1,0,&off)&&b[2][i+1]==0x9e620000&&b[2][i+2]==0x1e600841){out.span_y=off;++n;}
    }
    if(n!=2||out.folder_info!=compare_info||out.span_x<=0||out.span_y<=0)return false;
    const unsigned owners[]={0,1,1,2,2};
    for(unsigned i=0;i<5;++i)if(!big_folder_window(b[owners[i]],out.site[i]))return false;
    return true;
}
// The background producer deliberately keeps its original boxed width immutable.
// Both preview helpers still unbox that root, so scale their local arithmetic too.
// Resolve named original calls and prove the receiver/argument ABI; no BL is moved.
inline bool big_folder_preview_sites(const std::vector<uint32_t>& producer,uint32_t producer_va,
    const std::array<std::vector<uint32_t>,2>& helpers,const uint32_t helper_va[2],
    BigFolderSizeSites& out) {
    using namespace home_dynamic;
    for(unsigned k=0;k<2;++k){const auto& b=helpers[k];
        if(b.size()<32||!frame_size(b)||!receiver_slot(b,1,out.helper_receiver[k]))return false;
        const uint32_t width_args[]={0xaa0203e4,0xf81f03a2,0xaa0603e2,0xf81d83a6,
            0xaa0103e6,0xf81f83a1,0xf81e83a3,0xf81e03a5};
        const uint32_t height_args[]={0xaa0103e4,0xf81f83a1,0xf81f03a2,
            0xf81e83a3,0xf81e03a5,0xf81d83a6};
        const unsigned args=k==0?8:6;
        for(unsigned j=0;j<args;++j) {
            const auto original=k==0?width_args[j]:height_args[j];
            if((original&0xffe00fe0u)==0xf80003a0u) {
                if((b[3+j]&~0x001ff000u)!=(original&~0x001ff000u)
                    ||!frame_slot(b,imm9(b[3+j])))return false;
                if((original&31)==2)out.helper_input[k]=imm9(b[3+j]);
            }else if(b[3+j]!=original)return false;
        }
        unsigned roots=0;
        for(uint32_t w:b)roots+=stx(w,29,1)&&imm9(w)==out.helper_receiver[k];
        if(roots!=1)return false; // Original receiver remains a live GC root at FP-8.
        unsigned hits=0;
        for(size_t i=1;i+3<b.size();++i){int local=0;
            if(!ldx(b[i-1],29,0)||imm9(b[i-1])!=out.helper_input[k]||b[i]!=0xfc407001
                ||(b[i+1]&0xffe00fff)!=0xfc0003a1||b[i+2]!=0x1e600822
                ||(k==0?b[i+3]!=0x1e621000u:!ldx(b[i+3],29,2)
                    ||imm9(b[i+3])!=out.helper_receiver[k]))continue;
            local=int((b[i+1]>>12)&511);if(local&256)local-=512;
            if(!frame_slot(b,local)||local==out.helper_receiver[k]||local==out.helper_input[k])continue;
            unsigned uses=0;
            for(size_t j=i+4;j<b.size();++j)
                if((b[j]&0xffe00fe0u)==0xfc4003a0u&&imm9(b[j])==local)++uses;
            if(!uses)continue;
            out.site[5+k]=uint32_t(i*4);out.helper_width_local[k]=local;++hits;
        }
        if(hits!=1||!big_folder_window(b,out.site[5+k]))return false;
        unsigned calls=0;
        for(size_t i=4;i<producer.size();++i)
            if(dart_call_to(producer[i],producer_va+uint32_t(i*4),helper_va[k])){
                if(!ldx(producer[i-4],29,1)||imm9(producer[i-4])!=out.receiver[0]
                    ||!ldx(producer[i-3],29,2)||imm9(producer[i-3])!=out.producer_box
                    ||!ldx(producer[i-2],29,5)||!ldx(producer[i-1],29,6)
                    ||!frame_slot(producer,imm9(producer[i-2]))||!frame_slot(producer,imm9(producer[i-1]))
                    ||i*4<=out.site[0]+16||i*4>=out.base_return)return false;
                out.helper_return[k]=uint32_t((i+1)*4);++calls;
            }
        if(calls!=1)return false;
    }
    return out.helper_return[0]!=out.helper_return[1];
}
inline uintptr_t big_folder_config(uintptr_t thread,uint64_t heap,uintptr_t null,
    const BigFolderSizeSites& f) {
    auto good=[&](uintptr_t p){return (p&7)==1&&uint32_t(p)!=uint32_t(null)&& (p>>32)==heap;};
    if(!thread||f.table<=0||f.singleton<=0||f.config_rx<=0||f.rx_value<=0)return 0;
    uintptr_t table=workspace_read<uintptr_t>(thread,f.table);if(!table)return 0;
    uintptr_t controller=workspace_read<uintptr_t>(table,f.singleton);if(!good(controller))return 0;
    uintptr_t rx=workspace_read<uint32_t>(controller,f.config_rx)+(heap<<32);if(!good(rx))return 0;
    uintptr_t grid=workspace_read<uint32_t>(rx,f.rx_value)+(heap<<32);return good(grid)?grid:0;
}
inline double big_folder_width_ratio(uintptr_t grid,double side,const GridFieldOffsets& f,
    WorkspaceRenderSnapshot* rendered=nullptr) {
    if(!grid||!f.usable())return 1;
    double raw=workspace_read<double>(grid,f.cell_width),height=workspace_read<double>(grid,f.cell_height);
    const auto cols=workspace_read<int64_t>(grid,f.columns),rows=workspace_read<int64_t>(grid,f.rows);
    if(cols<1||cols>32||rows<2||rows>32||!std::isfinite(raw)||raw<1||raw>4096)return 1;
    double g[]={0,0,raw,height};
    // Rendering producers use new settings; animation targets use the last rendered geometry.
    if(!(rendered&&rendered->geometry(grid,cols,rows,raw,height,g))
        &&!inset_workspace(g,cols,rows,0,0,side))return 1;
    const double ratio=g[2]/raw;
    return std::isfinite(ratio)&&ratio>0&&ratio<=2?ratio:1;
}
inline void big_folder_size_body(uintptr_t fp,uint64_t heap,uintptr_t saved,unsigned kind,
    uintptr_t thread,uintptr_t null,const BigFolderSizeSites& f,const GridFieldOffsets& grid_fields,
    double side,bool enabled,uintptr_t big_begin,uintptr_t big_end,
    WorkspaceRenderSnapshot* rendered=nullptr) {
    if(kind>6)return;
    const auto readx=[&](int reg){return workspace_read<uintptr_t>(saved,reg*8);};
    const auto writex=[&](int reg,uintptr_t p){workspace_write(saved,reg*8,p);};
    const auto writed=[&](int reg,double d){
        workspace_write(saved,160+reg*16,d);
        workspace_write(saved,168+reg*16,uint64_t{0}); // Scalar D loads/arithmetic clear upper Q bits.
    };
    double ratio=1;
    if(enabled){
        if(kind==0){
            const uintptr_t strategy=readx(2);
            if(workspace_read<uint32_t>(strategy,f.hotseat)==uint32_t(null+0x30))
                ratio=big_folder_width_ratio(big_folder_config(thread,heap,null,f),side,grid_fields);
        }else if(kind<=2){
            // The base padding function is also called by 1x1 folders: only admit the large owner.
            const auto caller=workspace_read<uintptr_t>(fp,8);
            if(caller==big_begin+f.base_return&&caller<big_end
                &&workspace_read<uint32_t>(workspace_read<uintptr_t>(fp,f.receiver[1]),f.hotseat)==uint32_t(null+0x30)) {
                const uintptr_t parent=workspace_read<uintptr_t>(fp,0);
                // Read only the current verified parent frame; GC updates its original root.
                if(parent>fp&&parent-fp<=4096&&(parent&7)==0){
                    const uintptr_t boxed=workspace_read<uintptr_t>(parent,f.producer_box);
                    const double scaled=workspace_read<double>(parent,f.producer_width);
                    if((boxed&7)==1&&uint32_t(boxed)!=uint32_t(null)&&(boxed>>32)==heap){
                        const double raw=workspace_read<double>(boxed,7);
                        if(std::isfinite(raw)&&raw>0&&std::isfinite(scaled)&&scaled>0){
                            const double candidate=scaled/raw;
                            if(std::isfinite(candidate)&&candidate>0&&candidate<=2)ratio=candidate;
                        }
                    }
                }
            }
        }else if(kind<=4){
            const uintptr_t controller=workspace_read<uintptr_t>(fp,f.receiver[2]);
            const uintptr_t info=workspace_read<uint32_t>(controller,f.folder_info)+(heap<<32);
            const int64_t x=workspace_read<int64_t>(info,f.span_x),y=workspace_read<int64_t>(info,f.span_y);
            if(x>=1&&x<=32&&y>=1&&y<=32&&(x>1||y>1))
                ratio=big_folder_width_ratio(readx(0),side,grid_fields,rendered);
        }else{
            const unsigned helper=kind-5;
            const auto caller=workspace_read<uintptr_t>(fp,8);
            const auto parent=workspace_read<uintptr_t>(fp,0);
            // Only a CURRENT original parent frame supplies the ratio. Never cache
            // a movable Double/strategy pointer or reread a newer margin setting.
            if(caller==big_begin+f.helper_return[helper]&&caller<big_end
                &&parent>fp&&parent-fp<=4096&&(parent&7)==0){
                const auto boxed=workspace_read<uintptr_t>(parent,f.producer_box);
                const auto receiver=workspace_read<uintptr_t>(fp,f.helper_receiver[helper]);
                if(boxed==readx(0)&&(boxed&7)==1&&uint32_t(boxed)!=uint32_t(null)
                    &&(boxed>>32)==heap&&receiver==workspace_read<uintptr_t>(parent,f.receiver[0])){
                    const double raw=workspace_read<double>(boxed,7);
                    const double scaled=workspace_read<double>(parent,f.producer_width);
                    if(std::isfinite(raw)&&raw>0&&std::isfinite(scaled)&&scaled>0){
                        const double candidate=scaled/raw;
                        if(std::isfinite(candidate)&&candidate>0&&candidate<=2)ratio=candidate;
                    }
                }
            }
        }
    }
    if(kind==0){
        // Replay four original instructions. Only the producer's scalar outputs change;
        // its pristine boxed Size/Double inputs and shared GridConfig remain immutable.
        double width=workspace_read<double>(readx(3),7);
        double height=workspace_read<double>(saved,160);
        if(!std::isfinite(width)||!std::isfinite(height)||width*ratio<1||height*ratio<1)ratio=1;
        width*=ratio;height*=ratio;writed(1,width);if(ratio!=1)writed(0,height);
        workspace_write(fp,f.producer_width,width);workspace_write(readx(2),f.strategy_width,width);workspace_write(readx(2),f.strategy_height,height);
        workspace_write(fp,f.producer_height,height); // Original previous store must match the scaled height too.
    }else if(kind<=2){
        const double value=workspace_read<double>(readx(0),kind==1?grid_fields.cell_width:grid_fields.cell_height)*ratio;
        writed(0,value);workspace_write(fp,f.base_local[kind-1],value);
        auto table=workspace_read<uintptr_t>(thread,f.table);
        writex(0,workspace_read<uintptr_t>(table,kind==1?f.singleton:f.device_singleton));
    }else if(kind<=4){
        const double value=workspace_read<double>(readx(0),kind==3?grid_fields.cell_width:grid_fields.cell_height)*ratio;
        const double edit=workspace_read<double>(fp,f.size_edit);
        writed(0,value);writed(1,edit);writed(2,value*edit);writex(0,workspace_read<uintptr_t>(fp,f.receiver[2]));
    }else{
        const double width=workspace_read<double>(readx(0),7)*ratio;
        const double coefficient=workspace_read<double>(saved,160);
        writed(1,width);workspace_write(fp,f.helper_width_local[kind-5],width);
        writed(2,width*coefficient);
        if(kind==5)writed(0,4.0);
        else writex(2,workspace_read<uintptr_t>(fp,f.helper_receiver[kind-5]));
    }
}
} // namespace home_layout

