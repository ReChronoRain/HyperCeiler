/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <climits>
#include "home_dart_fields.h"
#include "home_folder_geometry.h"

namespace home_layout {
inline constexpr const char *kDropGeometrySymbol =
    "CellLayoutGetxController.calculateCenterGlobalPosition";
/*
 * The two splice windows, identified by the instructions at them. The offsets are not stored:
 * each run is unique in this body, so the scan finds the site and a launcher that shifts it
 * moves the hook with it. See home_dart_fields.h for the field offsets read alongside them.
 */
struct DropGeometrySite {
    uint32_t words[4];
};
inline constexpr DropGeometrySite kDropGeometrySites[] = {
    {{0xb843b001, 0x8b1c8021, 0xfc407020, 0xf85f03a0}},
    {{0x1e610802, 0xfc407020, 0x1e622801, 0xf85f83a1}},
};

using DropGeometryFields = GridFieldOffsets;
inline DropGeometryFields drop_geometry_fields(const GridFieldOffsets &found) { return found; }
struct DropGeometryContract {
    uint32_t site[2]{};
    int32_t receiver=0,column=0,row=0,width=0,height=0;
};
inline bool drop_geometry_contract(const home_dynamic::Body& b,uint32_t va,uint32_t config,
    uint32_t rx,const GridFieldOffsets& fields,DropGeometryContract& out) {
    using namespace home_dynamic;DropGeometryContract f;
    if(!fields.usable()||!receiver_slot(b,1,f.receiver)||!receiver_slot(b,2,f.column)
        ||!receiver_slot(b,3,f.row))return false;
    unsigned producers[2]{},sites[2]{};size_t store[2]{};
    for(size_t i=0;i+2<b.size();++i)for(unsigned axis=0;axis<2;++axis) {
        uint32_t to=0;
        if(target(b[i],va+i*4,to)&&to==config&&ldd(b[i+1],0,0)
            &&imm9(b[i+1])==(axis?fields.cell_height:fields.cell_width)&&std_(b[i+2],29,0)) {
            (axis?f.height:f.width)=imm9(b[i+2]);store[axis]=i+2;++producers[axis];
        }
    }
    if(producers[0]!=1||producers[1]!=1)return false;
    const int32_t locals[]={f.receiver,f.column,f.row,f.width,f.height};
    for(unsigned i=0;i<5;++i){if(!frame_slot(b,locals[i]))return false;
        for(unsigned j=0;j<i;++j)if(locals[i]==locals[j])return false;}
    for(size_t i=1;i+9<b.size();++i){uint32_t to=0;
        if(target(b[i-1],va+(i-1)*4,to)&&to==config&&ldw(b[i],0,1)&&imm9(b[i])==fields.origin
            &&b[i+1]==0x8b1c8021u&&b[i+2]==0xfc407020u
            &&ldx(b[i+3],29,0)&&imm9(b[i+3])==f.column&&b[i+4]==0x9e620001u
            &&ldd(b[i+5],29,2)&&imm9(b[i+5])==f.width&&b[i+6]==0x1e620823u
            &&b[i+7]==0x1e632801u&&std_(b[i+8],29,1)&&imm9(b[i+8])==f.width) {
            f.site[0]=i*4;++sites[0];
        }
        if(i>=2&&target(b[i-2],va+(i-2)*4,to)&&to==rx&&b[i-1]==0xaa0003e1u
            &&ldx(b[i],29,0)&&imm9(b[i])==f.row&&b[i+1]==0x9e620000u
            &&ldd(b[i+2],29,1)&&imm9(b[i+2])==f.height&&b[i+3]==0x1e610802u
            &&b[i+4]==0xfc407020u&&b[i+5]==0x1e622801u
            &&ldx(b[i+6],29,1)&&imm9(b[i+6])==f.receiver
            &&std_(b[i+7],29,1)&&imm9(b[i+7])==f.height){f.site[1]=(i+3)*4;++sites[1];}
    }
    if(sites[0]!=1||sites[1]!=1||f.site[1]<=f.site[0]+16)return false;
    for(unsigned axis=0;axis<2;++axis) {
        if(store[axis]>=f.site[0]/4)return false;
        for(size_t i=store[axis]+1;i<f.site[0]/4;++i)
            if((b[i]&0x3b200c00u)==0x38000000u&&rn(b[i])==29
                &&!(b[i]&(1u<<22))&&imm9(b[i])==(axis?f.height:f.width))return false;
        // No original control-flow edge may enter the displaced replay interior.
        for(size_t i=0;i<b.size();++i){int32_t delta=0;uint32_t mask=0;
            if(folder_relative_branch(b[i],delta,mask)) {
                const int64_t to=int64_t(i*4)+int64_t(delta)*4;
                if((i*4>=f.site[axis]&&i*4<f.site[axis]+16)
                    ||(to>f.site[axis]&&to<f.site[axis]+16))return false;
            }
        }
    }
    out=f;return true;
}

// x0 is GridInfo after currentConfig returns. The stock code uses its raw
// cell width/height to compute the drop-back center, while RenderBox may
// already have painted the workspace with insets. Replace only local values.
inline bool drop_geometry_body(uintptr_t fp, uint64_t heap, uintptr_t saved,
    unsigned kind, const DropGeometryFields *field, double top, double bottom, double side,
    WorkspaceRenderSnapshot *rendered,const DropGeometryContract* contract=nullptr) {
    if(!contract)return false;
    const auto& f=*contract;
    const auto d = [saved](int n, double v) {
        workspace_write(saved, 160 + n * 16, v);
        workspace_write(saved, 168 + n * 16, uint64_t{0});
    };
    if (kind == 0) {
        const uintptr_t grid = workspace_read<uintptr_t>(saved, 0);
        if (field == nullptr || !field->usable()) return false;
        const uintptr_t origin = workspace_read<uint32_t>(grid, field->origin)
            + (heap << 32);
        const double raw_x = workspace_read<double>(origin, 7);
        const double raw_width = workspace_read<double>(fp,f.width);
        const double raw_height = workspace_read<double>(fp,f.height);
        const int64_t column = workspace_read<int64_t>(fp,f.column);
        const int64_t row = workspace_read<int64_t>(fp,f.row);
        double g[4]{};
        const int64_t columns = workspace_read<int64_t>(grid, field->columns);
        const int64_t rows = workspace_read<int64_t>(grid, field->rows);
        const bool valid = folder_grid_geometry(grid, top, bottom, side, g, rendered, field)
            && std::isfinite(raw_x) && std::isfinite(raw_width) && std::isfinite(raw_height)
            && raw_width == workspace_read<double>(grid, field->cell_width)
            && raw_height == workspace_read<double>(grid, field->cell_height)
            && columns >= 1 && rows >= 1
            && column >= 0 && column < columns
            && row >= 0 && row < rows;
        workspace_write(saved, 8, origin);
        d(0, raw_x + (valid ? g[0] : 0));
        workspace_write(saved, 0, column);
        if (valid) {
            workspace_write(fp,f.width,g[2]);
            workspace_write(fp,f.height,g[3]);
            if (rendered) rendered->begin_drop(fp, g[1]);
        }
        return valid;
    }
    if (kind == 1) {
        // Replay fmuls/ldur/fadd/ldur from +1b8 and add the same rendered
        // top inset that was used for X's stride in this frame.
        const uintptr_t origin = workspace_read<uintptr_t>(saved, 8);
        const double row = workspace_read<double>(saved, 160);
        const double height = workspace_read<double>(saved, 176);
        const double origin_y = workspace_read<double>(origin, 7);
        const double rendered_top = rendered ? rendered->finish_drop(fp, 0) : 0;
        const double product = row * height;
        d(2, product);
        d(0, origin_y);
        d(1, origin_y + product + rendered_top);
        workspace_write(saved, 8, workspace_read<uintptr_t>(fp,f.receiver));
        return rendered_top != 0;
    }
    return false;
}
} // namespace home_layout
