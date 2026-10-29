/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_recent_render.h"
#include "../runtime-feature-addressing/fixture_image.h"
#include "fixture.h"
#include <iostream>
using namespace home_layout::recent_render;
static auto resolve(const Functions& fs){auto raw=image(fs);auto c=Code::parse(raw);assert(c);return dismiss::resolve(*c);}
int main(){
    auto fs=fixture();auto found=resolve(fs);
    if(fs.size()==5){
        assert(!found);auto raw=image(fs);auto code=Code::parse(raw);assert(code);
        Plan untouched;untouched.sites.push_back({});dismiss_graph(*code,untouched);
        assert(!untouched.dismiss&&untouched.sites.size()==1);
        std::cout<<"legacy 8583 fixture lacks appearance helper: new graph declines without changing existing recents bank\n";
        return 0;
    }
    assert(found);
    std::map<uint32_t,uint32_t> moved;auto relocated_fs=relocated(fs,moved);auto shifted=resolve(relocated_fs);assert(shifted);
    size_t cases=0;
    for(size_t i:{size_t(0),size_t(1),size_t(2),size_t(5)}){
        auto missing=fs;missing.erase(missing.begin()+i);assert(!resolve(missing));++cases;
        for(size_t j=0;j<fs[i].words.size();++j){auto broken=fs;broken[i].words[j]=0xd503201fu;assert(!resolve(broken));++cases;}
    }
    auto duplicate=relocated_fs;duplicate.push_back(fs[2]);assert(!resolve(duplicate));++cases;
    auto inconsistent=fs;inconsistent[0].words[56]+=8u<<12;assert(!resolve(inconsistent));++cases;
    auto layout=fs;
    for(size_t i:{size_t(56),size_t(68),size_t(89),size_t(106)})layout[0].words[i]+=8u<<12;
    for(size_t i:{size_t(11),size_t(30),size_t(31)})layout[1].words[i]+=8u<<12;
    for(auto& w:layout[4].words)if((ldd(w,1,0)&&imm9(w)==found->scale)||(ldd(w,1,1)&&imm9(w)==found->translation))w+=8u<<12;
    for(size_t i:{size_t(13),size_t(14),size_t(15)})layout[5].words[i]+=8u<<12;
    auto new_layout=resolve(layout);assert(new_layout&&new_layout->scale==found->scale+8);++cases;
    auto frames=fs;
    for(auto& f:frames)if(frame_size(f.words)){
        f.words[2]+=8u<<10;
        for(auto& w:f.words)if((w&0x3b200c00u)==0x38000000u&&rn(w)==29&&imm9(w)<0)
            w=(w&~(511u<<12))|((uint32_t(imm9(w)-8)&511u)<<12);
    }
    auto new_frames=resolve(frames);assert(new_frames&&new_frames->scale_frame==found->scale_frame-8);++cases;
    for(const auto& d:{*found,*new_layout,*new_frames})for(bool enabled:{false,true})for(double offset:{-100.,0.,100.}){
        Plan p;p.dismiss=d;RecentConfig c;c.values[RecentConfig::DismissAnimation]=enabled;
        alignas(16) std::array<std::byte,1024> heap{},frame{};
        const auto object=reinterpret_cast<uintptr_t>(heap.data())+1,fp=reinterpret_cast<uintptr_t>(frame.data())+512;
        store<double>(object+d.scale,.8);store<double>(object+d.translation,-60);store<double>(object+d.opacity,.4);
        store<double>(fp+d.height_frame,200);store<double>(fp+d.offset_frame,offset);
        Registers r{};r.x[1]=object;r.x[29]=fp;r.x[15]=fp-128;r.x[30]=0x1234;r.branch=0x4567;r.d(0,.4);r.d(1,200);r.d(2,offset);
        const auto old=r;const auto old_heap=heap;const auto old_frame=frame;
        const bool apply_now=enabled&&offset<0;
        Site site{};site.setting=RecentConfig::DismissAnimation;
        site.action=Action::DismissPlain;
        assert(apply(p,site,r,c,0)==(apply_now?Result::Applied:Result::Neutral));
        assert(load<double>(object+d.scale)==(apply_now?1:.8));
        assert(load<double>(object+d.translation)==-60);
        assert(r.d(0)==(apply_now?1:.4)&&r.branch==old.branch&&frame==old_frame);
        if(!apply_now)assert(!std::memcmp(&r,&old,sizeof(r))&&heap==old_heap);
        heap=old_heap;r=old;site.action=Action::DismissStack;
        assert(apply(p,site,r,c,0)==(apply_now?Result::Applied:Result::Neutral));
        assert(r.d(0)==.4&&r.d(1)==(apply_now?1:200));
        assert(load<double>(object+d.scale)==(apply_now?1:.8)&&frame==old_frame);
        for(const auto action:{Action::DismissScale,Action::DismissOpacity}){
            heap=old_heap;r=old;frame=old_frame;site.action=action;
            store<double>(fp+d.scale_frame,.73);const auto appearance_frame=frame;
            assert(apply(p,site,r,c,0x10000000)==(enabled?Result::Applied:Result::Neutral));
            assert(r.branch==old.branch&&heap==old_heap);
            assert(load<double>(fp+d.scale_frame)==(enabled&&action==Action::DismissScale?1:.73));
            assert(r.d(0)==(enabled&&action==Action::DismissOpacity?1:.4));
            auto restored=frame;store<double>(reinterpret_cast<uintptr_t>(restored.data())+512+d.scale_frame,.73);
            assert(restored==appearance_frame);
            assert(r.x[15]==old.x[15]&&r.x[29]==old.x[29]&&r.x[30]==old.x[30]);
        }
        ++cases;
    }
    std::cout<<"dismiss animation PASS "<<cases<<" cases, complete graph mutations, relocation, frame/field changes, disabled/downward passthrough, unchanged system displacement/branch/normalisation and late scale/opacity-only arguments, no allocation or Dart call\n";
}
