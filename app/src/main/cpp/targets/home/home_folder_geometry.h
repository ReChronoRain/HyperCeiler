/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_workspace_geometry.h"
#include <array>
#include <span>
#include <map>
#include <algorithm>

namespace home_layout {
/* Five original scalar replays. `words` identify instructions in the semantic
 * profile, not literal offsets or field values in the loaded image. The complete
 * current body proves live frame slots, named call targets, role aliases and
 * branch destinations. A mapped four-instruction replay must remain contiguous.
 */
struct FolderGeometrySite {
    const char *symbol;
    uint32_t words[4];
};
inline constexpr FolderGeometrySite kFolderGeometrySites[] = {
    {"WidgetPositionUtil.getCellPosition",
        {0xb843b001, 0x8b1c8021, 0xfc407020, 0xf85f83a0}},
    {"WidgetPositionUtil.getCellPosition",
        {0x9e620000, 0xfc5d83a1, 0x1e610802, 0xfc407020}},
    {"FolderIconGetxController.calOriginPreviewIconLoc",
        {0xb843b001, 0x8b1c8021, 0xfc407020, 0xf85f83a1}},
    {"FolderIconGetxController.calOriginPreviewIconLoc",
        {0x9e620060, 0xfc5e03a1, 0x1e610802, 0xfc407020}},
    {"WidgetPositionUtil.getCellPosition",
        {0x1e620823, 0x1e632801, 0x4ea11c20, 0xf85e83a1}},
};

// Semantic ABI profile: calls are roles, not old BL immediates. Branch destinations
// are instruction identities mapped into the current body, allowing inert NOPs outside
// displaced windows. Native frame slots stay exact because their GC/scalar lifetimes
// must be unchanged. Heap fields and pool immediates are decoded below.
enum FolderCall : uint32_t {
    kFolderConfigCall=0xffff0000u, kFolderFindCall, kFolderTransformCall,
    kFolderRxCall, kFolderRtlCall, kFolderOffsetCall, kFolderInitCall, kFolderThrowCall
};
inline constexpr uint32_t kFolderPositionProfile[] = {
    0xa9bf79fd, 0xaa0f03fd, 0xd100c1ef, 0xaa0103e0, 0xf81f83a1, 0xaa0303e1,
    0xf81f03a2, 0xf81e83a3, 0xf9403f43, 0xf9538863, 0x6b16007f, 0x540001e1,
    0xf9403f40, 0xf94e6800, 0xf9402370, 0x6b10001f, 0x54000061, 0xf97c0362,
    kFolderInitCall, 0x91402370, 0xf947e210, 0xf90001f0, 0xf9402b64, kFolderFindCall,
    0xaa0003e1, 0x14000002, 0xaa0303e1, kFolderConfigCall, 0xfc42b000, 0xfc1e03a0,
    0xf9403f40, 0xf9538800, 0x6b16001f, 0x540001e1, 0xf9403f40, 0xf94e6800,
    0xf9402370, 0x6b10001f, 0x54000061, 0xf97c0362, kFolderInitCall, 0x91402370,
    0xf947e210, 0xf90001f0, 0xf9402b64, kFolderFindCall, 0xaa0003e1, 0x14000002,
    0xaa0003e1, kFolderConfigCall, 0xfc433000, 0xfc1d83a0, 0xf9403f40, 0xf9538800,
    0x6b16001f, 0x540001e1, 0xf9403f40, 0xf94e6800, 0xf9402370, 0x6b10001f,
    0x54000061, 0xf97c0362, kFolderInitCall, 0x91402370, 0xf947e210, 0xf90001f0,
    0xf9402b64, kFolderFindCall, 0xaa0003e1, 0x14000002, 0xaa0003e1, 0xf85f83a0,
    0xfc5e03a0, kFolderConfigCall, 0xb843b001, 0x8b1c8021, 0xfc407020, 0xf85f83a0,
    0x9e620001, 0xfc5e03a2, 0x1e620823, 0x1e632801, 0x4ea11c20, 0xf85e83a1,
    0xf9403b64, kFolderTransformCall, 0xfc1e03a0, 0xf9403f40, 0xf9538800, 0x6b16001f,
    0x540001e1, 0xf9403f40, 0xf94e6800, 0xf9402370, 0x6b10001f, 0x54000061,
    0xf97c0362, kFolderInitCall, 0x91402370, 0xf947e210, 0xf90001f0, 0xf9402b64,
    kFolderFindCall, 0xaa0003e1, 0x14000002, 0xaa0003e1, 0xf85f03a0, 0xfc5e03a0,
    0xfc5d83a1, 0xb84f3022, 0x8b1c8042, 0xaa0203e1, kFolderRxCall, 0xaa0003e1,
    0xf85f03a0, 0x9e620000, 0xfc5d83a1, 0x1e610802, 0xfc407020, 0x1e622801,
    0xfc1d83a1, kFolderOffsetCall, 0xfc5e03a0, 0xfc007000, 0xfc5d83a0, 0xfc00f000,
    0xaa1d03ef, 0xa8c179fd, 0xd65f03c0,
};
inline constexpr uint32_t kFolderPreviewProfile[] = {
    0xa9bf79fd, 0xaa0f03fd, 0xd100e1ef, 0xf81f83a1, 0xf81f03a2, 0xf9403f40,
    0xf9538800, 0x6b16001f, 0x540001e1, 0xf9403f40, 0xf94e6800, 0xf9402370,
    0x6b10001f, 0x54000061, 0xf97c0362, kFolderInitCall, 0x91402370, 0xf947e210,
    0xf90001f0, 0xf9402b64, kFolderFindCall, 0xaa0003e1, 0x14000002, 0xaa0003e1,
    kFolderConfigCall, 0xfc42b000, 0xfc1e83a0, 0xf9403f40, 0xf9538800, 0x6b16001f,
    0x540001e1, 0xf9403f40, 0xf94e6800, 0xf9402370, 0x6b10001f, 0x54000061,
    0xf97c0362, kFolderInitCall, 0x91402370, 0xf947e210, 0xf90001f0, 0xf9402b64,
    kFolderFindCall, 0xaa0003e1, 0x14000002, 0xaa0003e1, kFolderConfigCall, 0xfc433000,
    0xfc1e03a0, 0xf9403f40, 0xf9538800, 0x6b16001f, 0x540001e1, 0xf9403f40,
    0xf94e6800, 0xf9402370, 0x6b10001f, 0x54000061, 0xf97c0362, kFolderInitCall,
    0x91402370, 0xf947e210, 0xf90001f0, 0xf9402b64, kFolderFindCall, 0xaa0003e1,
    0x14000002, 0xaa0003e1, 0xf85f83a0, 0xfc5e83a0, kFolderConfigCall, 0xb843b001,
    0x8b1c8021, 0xfc407020, 0xf85f83a1, 0xb8483020, 0x8b1c8000, 0xf9402370,
    0x6b10001f, 0x54000ae0, 0xf8437002, 0x9e620041, 0xfc5e83a2, 0x1e620823,
    0x1e632801, 0xfc1d83a1, 0xf9403f40, 0xf9538800, 0x6b16001f, 0x540001e1,
    0xf9403f40, 0xf94e6800, 0xf9402370, 0x6b10001f, 0x54000061, 0xf97c0362,
    kFolderInitCall, 0x91402370, 0xf947e210, 0xf90001f0, 0xf9402b64, kFolderFindCall,
    0xaa0003e1, 0x14000002, 0xaa0003e1, 0xf85f83a0, 0xfc5e83a0, 0xfc5e03a1,
    0xb84f3022, 0x8b1c8042, 0xaa0203e1, kFolderRxCall, 0xaa0003e1, 0xf85f83a0,
    0xb8483002, 0x8b1c8042, 0xf843f043, 0x9e620060, 0xfc5e03a1, 0x1e610802,
    0xfc407020, 0x1e622801, 0xfc1e03a1, 0xf842f041, 0xfc5d83a0, 0xf9403b64,
    kFolderTransformCall, 0xf85f83a1, 0xfc1d03a0, 0xb8483020, 0x8b1c8000, 0xf841f002,
    0x9e620041, 0xfc5e83a2, 0x1e610843, 0xfc1d83a3, kFolderRtlCall, 0x37200160,
    0xf85f03a0, 0xfc5d03a0, 0xfc5d83a1, 0x1e612802, 0xfc417000, 0x1e603841,
    0xfc407000, 0x1e603822, 0x4ea21c41, 0x14000006, 0xf85f03a0, 0xfc5d03a0,
    0xfc417001, 0x1e612802, 0x4ea21c41, 0xfc5e03a0, 0xfc1d83a1, 0xfc41f002,
    0x1e622803, 0xfc1e83a3, kFolderOffsetCall, 0xfc5d83a0, 0xfc007000, 0xfc5e83a0,
    0xfc00f000, 0xaa1d03ef, 0xa8c179fd, 0xd65f03c0, 0x91402769, 0xf9470d29,
    kFolderThrowCall,
};

// Read-only binding contract; no callback allocations or movable Dart roots persist here.
struct FolderGeometryContract { uint32_t site[5]{}; };
struct FolderGeometryCalls {
    uint32_t config=0,find=0,transform=0,rx=0,rtl=0,offset=0;
    uint32_t table_word=0,singleton_word=0;
};
inline bool folder_relative_branch(uint32_t w, int32_t& delta, uint32_t& mask) {
    if((w&0xfc000000u)==0x14000000u){delta=int32_t(w<<6)>>6;mask=0xfc000000u;return true;}
    if((w&0xff000010u)==0x54000000u||(w&0x7e000000u)==0x34000000u){delta=int32_t(w<<8)>>13;mask=~0x00ffffe0u;return true;}
    if((w&0x7e000000u)==0x36000000u){delta=int32_t(w<<13)>>18;mask=~0x0007ffe0u;return true;}
    return false;
}
inline bool folder_call_target(uint32_t w,uint32_t pc,uint32_t& target){
    if(!dart_is_bl(w))return false;
    const int64_t value=int64_t(pc)+(int32_t(w<<6)>>6)*int64_t(4);
    if(value<0||value>UINT32_MAX||value%4)return false;
    target=uint32_t(value);return true;
}
// Every anonymous helper is identified by its actual callee body, never by old VA.
// InitStaticField returns a rooted object and updates a thread-table entry. Its
// original BLR/GC PCs are untouched. Late-error saves the Dart register bank.
template<class Read>
inline bool folder_helper(uint32_t target,bool init,const FolderGeometryCalls& c,Read read){
    std::vector<uint32_t> b;if(!read(target,init?19u:28u,b))return false;
    if(init){
        const uint32_t exact[]={0xa9bf79fd,0xaa0f03fd,0xf81f8de2,0xb8413040,0x8b1c8000,
            0xf840701e,0xd63f03c0,0xf84085e2,0xb8417044,0xf9403f43,0x8b040863,
            0xf9400064,0xf9402370,0x6b10009f,0x540000a1,0xf9000060,
            0xaa1d03ef,0xa8c179fd,0xd65f03c0};
        if(b.size()!=std::size(exact))return false;
        for(size_t i=0;i<b.size();++i){
            if(i==9){if((b[i]&~31u)!=(c.table_word&~31u))return false;}
            else if(b[i]!=exact[i])return false;
        }
        return true;
    }
    if(b.size()!=28||b[0]!=0xf81f8dfe||b[27]!=0xf9407758)return false;
    for(unsigned i=0;i<16;++i){const unsigned q=30-i*2;
        if(b[i+1]!=(0xadbf0000u|((q+1)<<10)|(15<<5)|q))return false;}
    const uint32_t regs[][2]={{24,25},{20,23},{14,19},{12,13},{10,11},{8,9},{6,7},{4,5},{2,3},{0,1}};
    for(unsigned i=0;i<10;++i)if(b[17+i]!=(0xa9bf0000u|(regs[i][1]<<10)|(15<<5)|regs[i][0]))return false;
    return true;
}
template<class Read>
inline bool folder_geometry_contract(const std::array<std::vector<uint32_t>,2>& body,
    const uint32_t* va,const GridFieldOffsets& f,const FolderGeometryCalls& calls,
    FolderGeometryContract& out,Read read){
    if(!f.usable()||!calls.config||!calls.find||!calls.transform||!calls.rx||!calls.rtl||!calls.offset)return false;
    out={};std::map<uint32_t,uint32_t> operands;
    uint32_t init_target=0,throw_target=0;
    std::array<std::vector<size_t>,2> at;
    const auto refs=std::array<std::span<const uint32_t>,2>{kFolderPositionProfile,kFolderPreviewProfile};
    for(unsigned owner=0;owner<2;++owner){
        const auto& b=body[owner];const auto ref=refs[owner];auto& map=at[owner];
        if(b.size()<ref.size()||b.size()>4096||b[0]!=ref[0]||b[1]!=ref[1])return false;
        for(size_t i=0;i<b.size();++i)if(b[i]!=0xd503201fu)map.push_back(i);
        if(map.size()!=ref.size())return false;
        for(size_t j=0;j<ref.size();++j){const uint32_t old=ref[j],w=b[map[j]];
            if(old>=kFolderConfigCall&&old<=kFolderThrowCall){
                uint32_t target=0;if(!folder_call_target(w,va[owner]+uint32_t(map[j]*4),target))return false;
                const uint32_t named[]={calls.config,calls.find,calls.transform,calls.rx,calls.rtl,calls.offset};
                const unsigned role=old-kFolderConfigCall;
                if(role<6){if(target!=named[role])return false;}
                else{uint32_t& previous=role==6?init_target:throw_target;
                    if(previous&&previous!=target)return false;
                    if(!previous&&!folder_helper(target,role==6,calls,read))return false;
                    previous=target;}
                continue;
            }
            int32_t delta=0,actual_delta=0;uint32_t mask=0,actual_mask=0;
            if(folder_relative_branch(old,delta,mask)){
                const int64_t to=int64_t(j)+delta;
                if(to<0||to>=int64_t(map.size())||!folder_relative_branch(w,actual_delta,actual_mask)
                    ||mask!=actual_mask||(w&mask)!=(old&mask)
                    ||int64_t(map[j])+actual_delta!=int64_t(map[to]))return false;
                continue;
            }
            // Native consumers use the same proven owning GridConfig / FolderInfo fields.
            int dynamic=-1;
            if(old==0xb843b001)dynamic=f.origin;
            if(old==0xfc42b000)dynamic=f.cell_width;
            if(old==0xfc433000)dynamic=f.cell_height;
            if(old==0xf8437002)dynamic=f.item_col;
            if(old==0xf843f043)dynamic=f.item_row;
            if(dynamic>=0){if(dynamic>255||(w&~0x001ff000u)!=(old&~0x001ff000u)
                ||int((w>>12)&511)!=dynamic)return false;
                continue;}
            // Original-only pool operands may move. Keep registers, width, addressing
            // form and role aliases exact; these are never native heap read offsets.
            uint32_t immediate_mask=0;
            const unsigned rn=(old>>5)&31;
            if((old&0xffc00000u)==0xf9400000u&&(rn==26||rn==27||rn==16||rn==0||rn==3||rn==9))immediate_mask=0x003ffc00u;
            if((old&0xffc003e0u)==0x91400360u)immediate_mask=0x003ffc00u;
            if(immediate_mask){
                if((w&~immediate_mask)!=(old&~immediate_mask))return false;
                const uint32_t key=(old&~31u),value=w&immediate_mask;
                const auto entry=operands.emplace(key,value);
                if(!entry.second&&entry.first->second!=value)return false;
                if((old&~31u)==0xf9403f40u&&(w&~31u)!=(calls.table_word&~31u))return false;
                if((old&~31u)==0xf9538800u&&(w&~31u)!=(calls.singleton_word&~31u))return false;
                continue;
            }
            // Untouched object consumers can relocate fields consistently across aliases.
            // The decompression/type/arithmetic and register ownership remain in the profile.
            const uint32_t op=old&0xffe00c00u;
            if((op==0xb8400000u||op==0xf8400000u||op==0xfc400000u)&&rn!=29){
                int off=int((w>>12)&511);if(off&256)off-=512;
                if((w&~0x001ff000u)!=(old&~0x001ff000u)||off<=0
                    ||((off+1)&(op==0xb8400000u?3:7)))return false;
                // ABI Double / Offset payloads are primitive fields, not application layouts.
                const unsigned raw=(old>>12)&511;
                if(raw==7||raw==15){if(w!=old)return false;continue;}
                const uint32_t key=(op|raw),value=uint32_t(off);
                const auto entry=operands.emplace(key,value);
                if(!entry.second&&entry.first->second!=value)return false;
                continue;
            }
            if(w!=old)return false;
        }
    }
    // Resolve each replay window by semantic instruction identity. NOPs inside a
    // replay are refused; every original branch/GC call was checked against this map.
    for(unsigned i=0;i<5;++i){const unsigned owner=(i<2||i==4)?0:1;const auto ref=refs[owner];
        const auto& spec=kFolderGeometrySites[i];size_t hits=0,index=0;
        for(size_t j=0;j+4<=ref.size();++j)if(std::equal(spec.words,spec.words+4,ref.begin()+j)){index=j;++hits;}
        if(hits!=1)return false;
        for(unsigned j=1;j<4;++j)if(at[owner][index+j]!=at[owner][index]+j)return false;
        out.site[i]=uint32_t(at[owner][index]*4);
        for(size_t j=0;j<ref.size();++j){int32_t delta=0;uint32_t mask=0;
            if(folder_relative_branch(ref[j],delta,mask)){
                const int64_t to=int64_t(j)+delta;
                if(to>int64_t(index)&&to<int64_t(index+4))return false;
            }
        }
    }
    return true;
}

inline bool folder_grid_geometry(uintptr_t grid, double top, double bottom,
    double side, double *g, const WorkspaceRenderSnapshot *rendered,
    const GridFieldOffsets *field) {
    if (!field || !field->usable()) return false;
    const int64_t columns = workspace_read<int64_t>(grid, field->columns);
    const int64_t rows = workspace_read<int64_t>(grid, field->rows);
    if (rows < 2) return false;
    g[0] = g[1] = 0;
    g[2] = workspace_read<double>(grid, field->cell_width);
    g[3] = workspace_read<double>(grid, field->cell_height);
    if (rendered && rendered->geometry(grid, columns, rows, g[2], g[3], g)) return true;
    return inset_workspace(g, columns, rows, top, bottom, side);
}

// The native save block has x0..x14 at 0..112, x18 at 120,
// x15/LR at 128/136, and q0..q31 at 160..671. Change only the
// outputs of the five strictly verified displaced instructions. Upper SIMD
// lanes retain original contents unless the original scalar instruction clears them.
inline bool folder_geometry_body(uintptr_t fp, uint64_t heap,
    uintptr_t saved, unsigned kind, double top, double bottom, double side,
    WorkspaceRenderSnapshot *rendered = nullptr, const GridFieldOffsets *field = nullptr) {
    if (!field || !field->usable()) return false;
    const uintptr_t x0 = workspace_read<uintptr_t>(saved, 0);
    const auto d = [saved](int n, double v, bool clear = true) {
        workspace_write(saved, 160 + n * 16, v);
        if (clear) workspace_write(saved, 168 + n * 16, uint64_t{0});
    };
    bool valid = false;
    if (kind == 0) {
        // +128: origin pointer, decompression, origin.x, saved column.
        const uintptr_t origin = workspace_read<uint32_t>(x0, field->origin) + (heap << 32);
        double g[4];
        valid = folder_grid_geometry(x0, top, bottom, side, g, rendered, field);
        workspace_write(saved, 8, origin);
        d(0, workspace_read<double>(origin, 7) + (valid ? g[0] : 0));
        workspace_write(saved, 0, workspace_read<uintptr_t>(fp, -8));
        if (valid) {
            workspace_write(fp, -0x20, g[2]);
            workspace_write(fp, -0x28, g[3]);
        }
        // The outgoing-argument slot is temporary ONLY until +140, with
        // no intervening Dart call. Kind 4 moves this scalar into the dead
        // screen-index local AFTER its last read, before transformPointX.
        // Thus later Inst.find outgoing arguments cannot overwrite the delta.
        workspace_write(fp, -0x30, valid ? g[1] : 0.0);
    } else if (kind == 1) {
        // +1cc: row conversion, changed stride, product, original origin.y.
        const uintptr_t origin = workspace_read<uintptr_t>(saved, 8);
        const double height = workspace_read<double>(fp, -0x28);
        d(0, workspace_read<double>(origin, 7) + workspace_read<double>(fp, -0x18));
        d(1, height);
        d(2, static_cast<double>(static_cast<int64_t>(x0)) * height);
        valid = true;
    } else if (kind == 4) {
        // +140: original column*stride and x-origin sum, MOV V0,V1,
        // final screen-index read. Only then reuse the dead unboxed local.
        const double left = workspace_read<double>(saved, 160);
        const double product = workspace_read<double>(saved, 176)
            * workspace_read<double>(saved, 192);
        d(3, product); d(1, left + product);
        std::memcpy(reinterpret_cast<void *>(saved + 160),
            reinterpret_cast<const void *>(saved + 176), 16);
        workspace_write(saved, 8, workspace_read<uintptr_t>(fp, -0x18));
        workspace_write(fp, -0x18, workspace_read<double>(fp, -0x30));
        valid = true;
    } else if (kind == 2) {
        // calOriginPreviewIconLoc +11c, not the rendered folder widget size.
        const uintptr_t origin = workspace_read<uint32_t>(x0, field->origin) + (heap << 32);
        double g[4]; valid = folder_grid_geometry(x0, top, bottom, side, g, rendered, field);
        if (rendered) rendered->begin_preview(fp, valid ? g[1] : 0);
        workspace_write(saved, 8, workspace_read<uintptr_t>(fp, -8));
        d(0, workspace_read<double>(origin, 7) + (valid ? g[0] : 0));
        if (valid) {
            workspace_write(fp, -0x18, g[2]);
            workspace_write(fp, -0x20, g[3]);
        }
    } else if (kind == 3) {
        // +1d4: row conversion/stride/product/original margin Rx double.
        // No pointer or scalar is carried through an outgoing-argument slot.
        const uintptr_t margin = workspace_read<uintptr_t>(saved, 8);
        const int64_t row = workspace_read<int64_t>(saved, 24);
        const double height = workspace_read<double>(fp, -0x20);
        if (rendered) top = rendered->finish_preview(fp, top);
        valid = std::isfinite(top) && top >= -30 && top <= 120
            && std::isfinite(height) && height >= 1 && row >= 0 && row < 32;
        d(0, workspace_read<double>(margin, 7) + (valid ? top : 0));
        d(1, height); d(2, static_cast<double>(row) * height);
    }

    return valid;
}
} // namespace home_layout
