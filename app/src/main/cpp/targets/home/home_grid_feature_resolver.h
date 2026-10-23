/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_grid_autofit.h"
#include "home_drop_geometry.h"
#include <optional>

namespace home_layout::features {
struct GridAutofitFeature {
    uint32_t legacy=0,cap=0,handler=0;
    GridAutofitSites fields{};
};
inline std::optional<GridAutofitFeature> grid_autofit_feature(const Code& code) {
    using namespace home_dynamic;
    std::vector<const Function*> caps,legacy,handlers;
    for(const auto& f:code.functions()) {
        const auto& b=f.words;if(!frame_size(b))continue;
        bool cap=false,old=false,handler=false;
        for(size_t i=0;i<b.size();++i) {
            if(i+2<b.size()&&b[i]==0x4ee0f422u&&std_(b[i+1],1,2)&&std_(b[i+2],1,2))cap=true;
            if(i>=2&&i+3<b.size()&&b[i-2]==0x91000420u&&b[i-1]==0x9e620000u&&ldd(b[i],29,1)
                &&b[i+1]==0x1e600822u&&ldd(b[i+2],29,0)&&b[i+3]==0x1e623801u)old=true;
            if(i+8<b.size()&&ldd(b[i],1,0)&&ldd(b[i+1],1,1)&&b[i+2]==0x1e613802u&&ldx(b[i+3],1,3)
                &&b[i+4]==0x91000464u&&b[i+5]==0x9e620080u&&b[i+6]==0x1e600841u
                &&b[i+7]==0xfc407000u&&b[i+8]==0x1e613802u)handler=true;
        }
        if(cap)caps.push_back(&f);if(old)legacy.push_back(&f);if(handler)handlers.push_back(&f);
    }
    std::optional<GridAutofitFeature> result;
    for(const auto* old:legacy)for(const auto* cap:caps) {
        unsigned linked=0;
        for(size_t i=0;i<old->words.size();++i) {
            uint32_t to=0;if(target(old->words[i],old->va+uint32_t(i*4),to)&&to==cap->va)++linked;
        }
        if(linked!=1)continue;
        for(const auto* handler:handlers) {
            GridAutofitSites fields;
            if(!grid_autofit_sites(old->words,cap->words,handler->words,fields))continue;
            unsigned cap_consumers=0;
            for(size_t i=0;i+2<old->words.size();++i) {
                uint32_t to=0;
                if(target(old->words[i],old->va+uint32_t(i*4),to)&&to==cap->va
                    &&ldx(old->words[i+1],29,1)&&imm9(old->words[i+1])==fields.owner[0]
                    &&ldd(old->words[i+2],1,0)&&imm9(old->words[i+2])==fields.legacy_height)++cap_consumers;
            }
            if(cap_consumers!=1)continue;
            if(result)return {};
            result=GridAutofitFeature{old->va,cap->va,handler->va,fields};
        }
    }
    return result;
}

struct GridGeometryFeature {
    uint32_t current=0,rx=0,cell=0,occupied=0,drop=0,counts=0,hotseat=0;
    GridFieldOffsets fields{};
    std::array<home_dynamic::GeometryFrame,3> frames{};
    std::array<uint32_t,3> patches{},sizes{};
    DropGeometryContract landing{};
};
// A local cell-centre helper has the same stride arithmetic as global drop
// landing. Prove the returned coordinate also traverses the original pair of
// projective matrix/Offset transforms. Neither body size nor a symbol is identity.
inline bool projective_offset(const Code& code,uint32_t va,uint32_t offset_allocator) {
    using namespace home_dynamic;
    Body b,tag_words;uint32_t header=0;
    if(!code.body(va,b)||!frame_size(b)||!code.prefix(offset_allocator,3,tag_words)
        ||!tag(tag_words,header)||((header>>8)&15)*16!=32
        ||(tag_words[2]&0xfc000000u)!=0x14000000u)return false;
    unsigned input=0,products=0,divisions=0,outputs=0,returns=0;
    for(size_t i=0;i<b.size();++i) {
        if(i+1<b.size()&&ldd(b[i],2,1)&&ldd(b[i+1],2,2)
            &&imm9(b[i])==7&&imm9(b[i+1])==15)++input; // Offset ABI double payloads.
        if(b[i]==0xd65f03c0u)++returns;
        if((b[i]&0xffe0fc00u)==0x1e600800u)++products;
        if((b[i]&0xffe0fc00u)==0x1e601800u)++divisions;
        uint32_t to=0;
        if(i+7<b.size()&&target(b[i],va+uint32_t(i*4),to)&&to==offset_allocator
            &&ldd(b[i+1],29,0)&&frame_slot(b,imm9(b[i+1]))&&std_(b[i+2],0,0)&&imm9(b[i+2])==7
            &&(ldd(b[i+3],29,0)||ldd(b[i+3],29,1))&&frame_slot(b,imm9(b[i+3]))
            &&std_(b[i+4],0,rd(b[i+3]))&&imm9(b[i+4])==15
            &&b[i+5]==0xaa1d03efu&&b[i+6]==0xa8c179fdu&&b[i+7]==0xd65f03c0u) {
            ++outputs;
        }
    }
    // Three homogeneous components (X,Y,W), each with two products; a
    // perspective branch divides X/Y by W, while the W==1 arm returns directly.
    return input==1&&products==6&&divisions==2&&outputs==2&&returns==outputs;
}
inline bool global_landing_consumer(const Code& code,const Function& fn,const DropGeometryContract& landing) {
    using namespace home_dynamic;
    const auto& b=fn.words;unsigned hits=0;
    for(size_t i=landing.site[1]/4+4;i+5<b.size();++i) {
        uint32_t allocation=0;
        if(!target(b[i],fn.va+uint32_t(i*4),allocation)||!ldd(b[i+1],29,0)
            ||imm9(b[i+1])!=landing.width||!stx(b[i+2],29,0)||!frame_slot(b,imm9(b[i+2]))
            ||!std_(b[i+3],0,0)||imm9(b[i+3])!=7||!ldd(b[i+4],29,0)
            ||imm9(b[i+4])!=landing.height||!std_(b[i+5],0,0)||imm9(b[i+5])!=15)continue;
        const auto offset_slot=imm9(b[i+2]);
        bool clobber=false;
        for(size_t j=i+6;j+8<b.size();++j) {
            if(stx(b[j],29,0)&&imm9(b[j])==offset_slot)clobber=true;
            uint32_t transform=0,again=0;
            if(clobber||!ldx(b[j],29,1)||!ldx(b[j+1],29,2)||imm9(b[j+1])!=offset_slot
                ||!target(b[j+2],fn.va+uint32_t((j+2)*4),transform)||!ldx(b[j+3],29,1)
                ||imm9(b[j])==imm9(b[j+3])||!frame_slot(b,imm9(b[j]))||!frame_slot(b,imm9(b[j+3]))
                ||b[j+4]!=0xaa0003e2u||!target(b[j+5],fn.va+uint32_t((j+5)*4),again)
                ||again!=transform||b[j+6]!=0xaa0003e2u||!ldx(b[j+7],29,0)
                ||imm9(b[j+7])!=landing.receiver||!stx(b[j+8],29,2)||imm9(b[j+8])!=offset_slot
                ||!projective_offset(code,transform,allocation))continue;
            const std::array<size_t,1> point{j+5};
            if(passes_return_points(b,point))++hits;
        }
    }
    return hits==1;
}
inline auto grid_field_identity(const GridFieldOffsets& f) {
    return std::array<int32_t,11>{f.columns,f.rows,f.origin,f.item_col,f.item_row,
        f.cell_width,f.cell_height,f.occupied_grid,f.dock_columns,f.dock_item,f.dock_info};
}
inline bool grid_config_source(const Code& code,uint32_t current,uint32_t rx) {
    using namespace home_dynamic;
    Body c,r;if(!code.body(current,c)||!code.body(rx,r)||!frame_size(c)||!frame_size(r))return false;
    std::vector<size_t> roots;unsigned values=0;int32_t receiver=0;
    if(!receiver_slot(r,1,receiver))return false;
    for(size_t i=0;i+5<c.size();++i) {
        uint32_t to=0;
        if(ldw(c[i],0,1)&&field(imm9(c[i]))&&c[i+1]==0x8b1c8021u
            &&target(c[i+2],current+uint32_t((i+2)*4),to)&&to==rx
            &&c[i+3]==0xaa1d03efu&&c[i+4]==0xa8c179fdu&&c[i+5]==0xd65f03c0u)roots.push_back(i+2);
    }
    for(size_t i=0;i+7<r.size();++i)
        if(ldx(r[i],29,1)&&imm9(r[i])==receiver&&ldw(r[i+1],1,0)&&field(imm9(r[i+1]))
            &&r[i+2]==0x8b1c8000u&&r[i+4]==0x6b10001fu
            &&(r[i+5]&0xff00001fu)==0x54000000u&&r[i+6]==0xaa1d03efu&&r[i+7]==0xa8c179fdu
            &&i+8<r.size()&&r[i+8]==0xd65f03c0u)++values;
    return roots.size()==1&&values==1&&passes_return_points(c,roots);
}
inline bool geometry_frame_safe(const Function& f,const home_dynamic::GeometryFrame& frame,uint32_t patch) {
    using namespace home_dynamic;
    for(auto slot:{frame.grid,frame.info,frame.closure,frame.x,frame.y,frame.width,frame.height,
                  frame.out_x,frame.out_y,frame.out_width,frame.out_height})
        if(slot&&!frame_slot(f.words,slot))return false;
    if(uint64_t(patch)+16>f.words.size()*4)return false;
    for(size_t i=0;i<f.words.size();++i) {
        int32_t delta=0;uint32_t mask=0;
        if(folder_relative_branch(f.words[i],delta,mask)) {
            const int64_t to=int64_t(i*4)+int64_t(delta)*4;
            if((i*4>=patch&&i*4<patch+16)||(to>patch&&to<patch+16))return false;
        }
    }
    return true;
}
// The same current-config return must supply counts, both raw strides and the
// drag origin. The occupied delegate then correlates those strides with item
// indices; its hotseat consumer must use that SAME column index. Names are not
// used even to find the configuration or reactive-value roots.
inline std::optional<GridGeometryFeature> grid_geometry_feature(const Code& code) {
    using namespace home_dynamic;
    struct Delegate {const Function* fn;GeometryFrame frame;uint32_t patch;};
    struct Counts {const Function* fn;uint32_t current;GridFieldOffsets fields;};
    std::vector<Delegate> cells,occupied,hotseats;
    std::vector<const Function*> drops;
    std::vector<Counts> counts;
    for(const auto& f:code.functions()) {
        const auto& b=f.words;if(!frame_size(b))continue;
        GeometryFrame frame;uint32_t patch=0;
        if(workspace_frame(b,false,frame,patch)&&geometry_frame_safe(f,frame,patch))cells.push_back({&f,frame,patch});
        if(workspace_frame(b,true,frame,patch)&&geometry_frame_safe(f,frame,patch))occupied.push_back({&f,frame,patch});
        if(hotseat_frame(b,frame,patch)&&geometry_frame_safe(f,frame,patch))hotseats.push_back({&f,frame,patch});
        int32_t receiver=0,column=0,row=0;
        if(receiver_slot(b,1,receiver)&&receiver_slot(b,2,column)&&receiver_slot(b,3,row)
            &&read_grid_field_offsets(b).origin>0)drops.push_back(&f);
        for(size_t i=0;i+4<b.size();++i) {
            uint32_t current=0;
            if(!target(b[i],f.va+uint32_t(i*4),current)||!ldx(b[i+1],0,1)
                ||!ldx(b[i+2],29,0)||b[i+3]!=0xeb01001fu
                ||(b[i+4]&0xff00001fu)!=0x5400000au)continue;
            GridFieldOffsets fields;
            if(read_grid_counts(b,f.va,current,fields))counts.push_back({&f,current,fields});
        }
    }
    std::optional<GridGeometryFeature> result;
    for(const auto& cell:cells) {
        GridFieldOffsets stride;read_grid_cell_size(cell.fn->words,stride);
        if(stride.cell_width<=0||stride.cell_height<=0)continue;
        for(const auto& item:occupied) {
            auto indices=stride;if(!read_occupied_fields(item.fn->words,indices))continue;
            for(const auto& count:counts)for(const auto* drop:drops) {
                auto fields=indices;fields.columns=count.fields.columns;fields.rows=count.fields.rows;
                fields.origin=read_grid_field_offsets(drop->words).origin;
                Body current_body;if(!fields.usable()||!code.body(count.current,current_body))continue;
                unsigned origins=0;
                for(size_t i=0;i+3<drop->words.size();++i) {
                    uint32_t to=0;
                    if(target(drop->words[i],drop->va+uint32_t(i*4),to)&&to==count.current
                        &&ldw(drop->words[i+1],0,1)&&imm9(drop->words[i+1])==fields.origin
                        &&drop->words[i+2]==0x8b1c8021u&&drop->words[i+3]==0xfc407020u)++origins;
                }
                if(origins!=1)continue;
                for(size_t i=0;i+6<drop->words.size();++i) {
                    uint32_t rx=0;const auto& b=drop->words;
                    if(!target(b[i],drop->va+uint32_t(i*4),rx)||b[i+1]!=0xaa0003e1u
                        ||!ldx(b[i+2],29,0)||b[i+3]!=0x9e620000u||!ldd(b[i+4],29,1)
                        ||b[i+5]!=0x1e610802u)continue;
                    if(!grid_config_source(code,count.current,rx))continue;
                    DropGeometryContract landing;
                    if(!drop_geometry_contract(b,drop->va,count.current,rx,fields,landing)
                        ||!global_landing_consumer(code,*drop,landing))continue;
                    GridGeometryFeature next;next.current=count.current;next.rx=rx;
                    next.cell=cell.fn->va;next.occupied=item.fn->va;next.drop=drop->va;next.counts=count.fn->va;
                    next.frames[0]=cell.frame;next.frames[1]=item.frame;
                    next.patches[0]=cell.patch;next.patches[1]=item.patch;
                    next.sizes[0]=uint32_t(cell.fn->words.size()*4);next.sizes[1]=uint32_t(item.fn->words.size()*4);
                    next.landing=landing;
                    bool ambiguous=false;
                    for(const auto& dock:hotseats) {
                        auto candidate=fields;
                        if(!read_hotseat_fields(dock.fn->words,candidate))continue;
                        if(next.hotseat){ambiguous=true;break;}
                        next.hotseat=dock.fn->va;next.frames[2]=dock.frame;next.patches[2]=dock.patch;
                        next.sizes[2]=uint32_t(dock.fn->words.size()*4);fields=candidate;
                    }
                    if(ambiguous) {
                        next.hotseat=0;fields.dock_columns=fields.dock_item=fields.dock_info=-1;
                    }
                    next.fields=fields;
                    if(result) {
                        // Count helpers are evidence, not hook targets. Multiple
                        // consumers of identical fields do not choose a bank.
                        if(result->current!=next.current||result->rx!=next.rx||result->cell!=next.cell
                            ||result->occupied!=next.occupied||result->drop!=next.drop
                            ||result->hotseat!=next.hotseat||grid_field_identity(result->fields)!=grid_field_identity(next.fields))return {};
                        continue;
                    }
                    result=next;
                }
            }
        }
    }
    return result;
}
} // namespace home_layout::features
