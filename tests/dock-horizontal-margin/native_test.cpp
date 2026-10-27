// SPDX-License-Identifier: AGPL-3.0-or-later
#include "targets/home/home_layout_config.h"
#include "targets/home/home_workspace_geometry.h"
#include "geometry_frame_fixture.h"
#include <atomic>
#include <cassert>
#include <cstdio>
#include <cstring>
#include <vector>
struct Knob {std::atomic_int delta_dp{0};};Knob g_knobs[8];
std::atomic_int g_dock_horizontal_delta{0};
std::atomic_uint64_t g_dock_horizontal_hits{0},g_dock_horizontal_applied{0};
home_layout::GridFieldOffsets g_grid_field{0x1b,0x23,0x0b,0x37,0x3f,0x2b,0x33,0x17,0x13,0x0f,0x07};
home_dynamic::GeometryFrame g_geometry_frame[]{kWorkspaceFrame,kOccupiedFrame,kHotseatFrame};
home_layout::WorkspaceRenderSnapshot rendered_workspace;
#define __android_log_print(...) ((void)0)
#define ANDROID_LOG_INFO 4
#include "actual_callback.h"
#include "actual_cache.h"
unsigned checks;
void c(bool b){checks++;if(!b){std::fprintf(stderr,"failed %u\n",checks);std::abort();}}
int main(){
    using namespace home_layout;
    alignas(16) unsigned char storage[1024]{};
    auto fp=uintptr_t(storage+256),closure=uintptr_t(storage+384),delegate=uintptr_t(storage+512),info=uintptr_t(storage+640),item=uintptr_t(storage+768);
    workspace_write(fp,kHotseatFrame.grid,delegate);workspace_write(fp,kHotseatFrame.closure,closure);
    workspace_write(closure,g_grid_field.dock_item,uint32_t(item));workspace_write(item,g_grid_field.dock_info,uint32_t(info));
    for(int desktop:{-20,0,15,80})for(int dock:{-20,0,15,80})for(int64_t columns=1;columns<=12;++columns){
        workspace_write(delegate,g_grid_field.dock_columns,columns);g_knobs[4].delta_dp=desktop;g_dock_horizontal_delta=dock;
        double total=0;
        for(int64_t col=0;col<columns;++col){workspace_write(info,g_grid_field.item_col,col);double x=30+col*60;
            workspace_write(fp,kHotseatFrame.x,x);workspace_write(fp,-136,37.0);
            unsigned char before[1024];std::memcpy(before,storage,1024);
            hc_layout_workspace_layout(fp,info>>32,2);
            double got=workspace_read<double>(fp,kHotseatFrame.x);double shift=dock*(1-(2.*col+1)/columns);
            c(std::abs(got-x-shift)<1e-8);total+=got-x;
            std::memcpy(before+256+kHotseatFrame.x,storage+256+kHotseatFrame.x,8);c(std::memcmp(before,storage,1024)==0);
        }c(std::abs(total)<1e-8);
    }
    // Conversely, changing only Dock must leave the workspace's own inset intact.
    workspace_write(fp,kWorkspaceFrame.grid,closure);
    workspace_write(closure,g_grid_field.columns,int64_t{4});workspace_write(closure,g_grid_field.rows,int64_t{6});
    g_knobs[2].delta_dp=0;g_knobs[3].delta_dp=0;g_knobs[4].delta_dp=15;
    for(int dock:{-20,0,15,80}) {
        g_dock_horizontal_delta=dock;
        workspace_write(fp,kWorkspaceFrame.x,20.0);workspace_write(fp,kWorkspaceFrame.y,30.0);
        workspace_write(fp,kWorkspaceFrame.width,100.0);workspace_write(fp,kWorkspaceFrame.height,100.0);
        hc_layout_workspace_layout(fp,closure>>32,0);
        c(workspace_read<double>(fp,kWorkspaceFrame.out_x)==35.0);
        c(workspace_read<double>(fp,kWorkspaceFrame.out_width)==92.5);
        c(workspace_read<double>(fp,kWorkspaceFrame.out_y)==30.0);
    }
    for(int delta:{-20,0,80}) {
        Config config;config.cell_x=4;config.cell_y=6;config.dock_horizontal=delta;config.infinite_scroll=true;
        config.knobs[4]={true,-10};std::vector<uint8_t> raw;serialize_config(config,raw);Config restored;
        c(parse_config(raw,restored)&&restored.dock_horizontal==delta&&restored.knobs[4].delta_dp==-10&&restored.infinite_scroll);
        for(size_t remove=1;remove<=20;++remove){auto bad=raw;bad.resize(bad.size()-remove);c(!parse_config(bad,restored));}
        auto old=raw;old.resize(old.size()-8);old[4]=19;
        c(parse_config(old,restored)&&restored.dock_horizontal==delta&&!restored.infinite_scroll);
        old=raw;old.resize(old.size()-20);old[4]=18;
        c(parse_config(old,restored)&&restored.dock_horizontal==0&&restored.knobs[4].delta_dp==-10);
        auto bad=raw;bad.back()=0x7f;c(!parse_config(bad,restored));
    }
    for(std::vector<int32_t> wire:std::vector<std::vector<int32_t>>{{},{0x4843484d,1,-20},{0x4843484d,1,80},{0x4843484d,1,81},{0x4843484d,1,-21},{0x4843484d,2,0},{0x4843484d,1},{1,1,0}}){
        size_t at=0;int delta=999;bool present=false;
        bool result=read_dock_horizontal([&](int32_t& v){if(at==wire.size())return false;v=wire[at++];return true;},delta,true,&present);
        bool valid=wire.empty()||(wire.size()==3&&wire[0]==0x4843484d&&wire[1]==1&&wire[2]>=-20&&wire[2]<=80);
        c(result==valid);c(present==(!wire.empty()&&valid));
    }
    std::printf("dock horizontal actual callback/cache/wire %u checks passed\n",checks);
}
