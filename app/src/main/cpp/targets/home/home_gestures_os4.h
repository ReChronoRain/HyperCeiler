/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_layout_elf_targets.h"
#include "nativehook/elf_image.h"
#include <cmath>

namespace home_gestures {
struct Config { bool enabled=false,quick=false; unsigned mask=0; int density_dpi=160,width=1; };
constexpr int32_t kMagic=0x48434731;
template<class Read> bool read_config(Read read,Config &out,bool optional=true) {
    out={};int32_t magic=0,enabled=0,quick=0,mask=0,dpi=0,width=0;
    if(!read(magic))return optional;
    if(magic!=kMagic||!read(enabled)||!read(quick)||!read(mask)||!read(dpi)||!read(width)
        ||enabled<0||enabled>1||quick<0||quick>1||mask<0||mask>31||dpi<80||dpi>1280||width<1||width>32768)return false;
    out={enabled!=0,quick!=0,static_cast<unsigned>(mask),dpi,width};return true;
}
// Android MotionEvent and AAPCS are contracts, not launcher object offsets.
enum Gesture { None=0, DoubleTap=1, Up=2, Down=3, Up2=4, Down2=5, Previous=6 };
struct Sample { int action=0, pointers=1; float x=0,y=0; int64_t time=0; uintptr_t owner=0; };
struct Decision { bool cancel=false, consume=false; int gesture=None; uint64_t revision=0; };
struct Settings { unsigned mask=0; bool quick=false; float density=1,width=0; uint64_t revision=0; };
class Recognizer {
    uintptr_t owner_=0;
    uint64_t revision_=0;
    int pointers_=1, claimed_=None;
    bool active_=false, cancelled_=false, wandered_=false;
    float x_=0,y_=0,last_x_=0,last_y_=0,tap_x_=0,tap_y_=0;
    int64_t start_=0,moved_=0,tap_=0;
public:
    void reset() { *this=Recognizer{}; }
    Decision accept(Sample e, Settings s, bool back) {
        if (!std::isfinite(e.x)||!std::isfinite(e.y)||!std::isfinite(s.density)
            ||s.density<0.5f||s.density>8||e.time<=0||e.pointers<1||e.pointers>10) {
            reset(); return {};
        }
        if (owner_!=e.owner||revision_!=s.revision) { reset();owner_=e.owner;revision_=s.revision; }
        if (e.action==0) {
            active_=true;cancelled_=false;wandered_=false;claimed_=None;pointers_=e.pointers;
            x_=last_x_=e.x;y_=last_y_=e.y;start_=moved_=e.time;
            return {};
        }
        if (!active_) return {};
        if (e.time<start_||e.time-start_>10000) {reset();return {};}
        pointers_=std::max(pointers_,e.pointers);
        if (e.action==3) {active_=false;tap_=0;return {};}
        if (e.action!=1&&e.action!=2&&e.action!=5&&e.action!=6) return {};
        float dx=e.x-x_,dy=e.y-y_;
        const float slop=12*s.density, distance=64*s.density;
        wandered_|=std::hypot(dx,dy)>slop;
        if (std::hypot(e.x-last_x_,e.y-last_y_)>4*s.density) {
            moved_=e.time;last_x_=e.x;last_y_=e.y;
        }
        if (back) {
            // This owner receives only accepted left/right back-panel input: no guessed edge width.
            // An inward move, hold, then release switches once; outward/short/fast/cancel stays stock.
            const bool inward=s.width>0&&(x_<s.width/2?dx>0:dx<0);
            if (s.quick&&inward&&pointers_==1&&std::abs(dx)>=distance&&std::abs(dy)<std::abs(dx)*0.5f
                &&e.action==1&&e.time-moved_>=300&&e.time-start_>=350) claimed_=Previous;
        } else if (claimed_==None&&std::abs(dy)>=distance&&std::abs(dy)>std::abs(dx)*1.5f) {
            int g=dy<0?(pointers_>=2?Up2:Up):(pointers_>=2?Down2:Down);
            if (s.mask&(1u<<(g-1))) claimed_=g;
        } else if (claimed_==None&&!wandered_&&e.action==1&&pointers_==1&&std::hypot(dx,dy)<=slop
            &&e.time-start_<=250) {
            if (tap_>0&&e.time-tap_<=500&&std::hypot(e.x-tap_x_,e.y-tap_y_)<=slop
                &&(s.mask&1)) {claimed_=DoubleTap;tap_=0;}
            else {tap_=e.time;tap_x_=e.x;tap_y_=e.y;}
        }
        Decision out;
        if (claimed_!=None) {
            out.revision=s.revision;
            out.consume=true;out.cancel=!cancelled_;cancelled_=true;
            if (e.action==1) out.gesture=claimed_;
        }
        if (e.action==1) {
            active_=false;
            if (wandered_) tap_=0;
        }
        return out;
    }
};

struct Targets {
    uint64_t home=0,back=0,scene=0;
    uint32_t home_mode=0;
    // Validated PLT addresses; calls use the current image's imported ABI, never RTLD_DEFAULT guesses.
    std::array<uint64_t,10> imports{};
};
inline constexpr std::array<const char*,10> kImports={"input_MotionEvent_getActionMasked",
    "input_MotionEvent_getAction", "input_MotionEvent_getRawX", "input_MotionEvent_getRawY",
    "input_MotionEvent_getEventTime", "input_MotionEvent_getPointerCount", "input_MotionEvent_setAction",
    "input_MotionEvent_clone", "input_MotionEvent_drop", "input_MotionEvent_getPointerId"};
inline std::optional<Targets> resolve(std::span<const std::byte> bytes) {
    using namespace home_layout::elf_targets;
    auto image=parse(bytes);if(!image)return {};
    // ElfImage expects a vaddr-domain snapshot rather than APK file offsets.
    uint64_t end=0;
    for(auto &s:image->segments)if(s.type==1) {
        if(s.vaddr>128*1024*1024||s.memsz>128*1024*1024-s.vaddr)return {};
        end=std::max(end,s.vaddr+s.memsz);
    }
    if(end==0||end>128*1024*1024)return {};
    std::vector<std::byte> virtual_image(end);
    for(auto &s:image->segments)if(s.type==1) {
        if(!nhk::in_image(bytes.size(),s.offset,s.filesz)||s.filesz>s.memsz)return {};
        std::memcpy(virtual_image.data()+s.vaddr,bytes.data()+s.offset,s.filesz);
    }
    auto dynamic=nhk::elf::ElfImage::make(0,virtual_image);if(!dynamic)return {};
    Targets t;
    std::array<uint64_t,kImports.size()> slots{};
    for(unsigned i=0;i<kImports.size();++i) {
        auto slot=dynamic->unique_slot(kImports[i]);if(!slot)return {};slots[i]=slot->rva;
    }
    // Enumerate PLT shapes and bind them to the exact named relocation, refusing duplicate stubs.
    for(auto &s:image->segments)if(s.type==1&&(s.flags&1))
        for(uint64_t pc=s.vaddr;pc+16<=s.vaddr+s.filesz;pc+=4) {
            std::array<uint32_t,4>w{};for(unsigned j=0;j<4;++j)w[j]=*image->word(pc+4*j);
            auto got=nhk::arm64::decode_plt_got(w,0,pc);if(!got)continue;
            for(unsigned i=0;i<slots.size();++i)if(*got==slots[i]) {
                if(t.imports[i])return {};t.imports[i]=pc;
            }
        }
    for(auto p:t.imports)if(!p)return {};
    auto owner=[&](const char*label){auto literal=unique_literal(*image,label);
        return literal?unique_function(*image,*literal):std::optional<home_layout::unwind::FunctionBounds>{};};
    auto h=owner(" Home on_touch_event , action="),b=owner(" on_swipe_stop, is_finish: ");
    auto handled=owner(" Home on_touch_event: event already handled by another handler");
    auto mid=owner(" on_swipe_stop mid_state: mid_state_swipe_outward=");
    if(!h||!b||!handled||!mid||h->begin!=handled->begin||b->begin!=mid->begin
        ||h->end-h->begin>0x4000||b->end-b->begin>0x5000||h->begin==b->begin)return {};
    // The original home mode gate is retained. Decode its receiver field, not a version offset.
    auto w0=image->word(h->begin),w1=image->word(h->begin+4),w2=image->word(h->begin+8),w3=image->word(h->begin+12);
    if(!w0||!w1||!w2||!w3||(*w0&0xffc003ff)!=0xb9400008||*w1!=0x51000508
        ||*w2!=0x7100091f||(*w3&0xff00001f)!=0x54000002)return {};
    t.home_mode=((*w0>>10)&0xfff)*4;
    for(auto f:{*h,*b}) {
        unsigned action_calls=0,x_calls=0,y_calls=0;bool receiver=false,event=false;
        for(uint64_t pc=f.begin;pc+4<=f.end;pc+=4) {
            auto w=*image->word(pc);
            if(pc-f.begin<0x80&&(w&0xfffffc1f)==0xaa000013)receiver=true; // mov x19,x0
            if(pc-f.begin<0x80&&(w&0xfffffc1f)==0xaa010014)event=true; // x20 or x21 below
            if(pc-f.begin<0x80&&w==0xaa0103f5)event=true;
            if((w&0xfc000000)!=0x94000000)continue;
            const uint64_t target=uint64_t(int64_t(pc)+(int64_t(int32_t(w<<6))>>4));
            action_calls+=target==t.imports[0];x_calls+=target==t.imports[2];y_calls+=target==t.imports[3];
        }
        if(!receiver||!event||!action_calls||!x_calls||!y_calls)return {};
    }
    // The helper may be wrapped by another module. Observe the owning window dispatcher,
    // not its helper entry. Its paired UP/CANCEL logs and direct helper edge establish identity.
    auto dispatch=owner(" dispatch_touch_event: ACTION_UP received");
    auto cancel=owner(" dispatch_touch_event: ACTION_CANCEL received (touch transferred or gesture ended)");
    if(!dispatch||!cancel||dispatch->begin!=cancel->begin||dispatch->end-dispatch->begin>0x4000
        ||dispatch->begin==b->begin||dispatch->begin==h->begin)return {};
    bool self=false,event=false,masked=false,helper=false,drop=false;
    for(uint64_t pc=dispatch->begin;pc+4<=dispatch->end;pc+=4) {
        auto w=*image->word(pc);
        if(pc-dispatch->begin<0x80&&w==0xaa0003f4)self=true; // mov x20,x0
        if(pc-dispatch->begin<0x80&&w==0xaa0103f3)event=true; // mov x19,x1
        auto target=nhk::arm64::branch_target(pc,w,true);if(!target)continue;
        masked|=*target==t.imports[0];
        if(*target==b->begin) {
            helper|=pc>=8&&*image->word(pc-8)==0xaa1403e0&&*image->word(pc-4)==0xaa1303e1;
        }
        // Owned MotionEvent: the original dispatcher drops X19 on its common epilogue.
        if(*target==t.imports[8]&&pc>=4&&*image->word(pc-4)==0xaa1303e0)drop=true;
    }
    if(!self||!event||!masked||!helper||!drop)return {};
    // The normal workspace arrives through PlatformScene, not the recents Home
    // handler above. Two log encodings share the prefix; prove the MotionEvent
    // consumer's ABI/imports and borrowed lifetime, never select the first string.
    for(const auto& seg:image->segments)if(seg.type==1&&!(seg.flags&1)) {
        std::string_view data(reinterpret_cast<const char*>(bytes.data()+seg.offset),seg.filesz);
        constexpr std::string_view label="PlatformScene on_motion_event, ui_token:";
        size_t at=0;
        while((at=data.find(label,at))!=std::string_view::npos) {
            auto f=unique_function(*image,seg.vaddr+at++);if(!f||f->end-f->begin>0x2000)continue;
            bool self=false,event=false,action=false,pointers=false,borrowed=false,drop=false;
            for(uint64_t pc=f->begin;pc+4<=f->end;pc+=4) {
                uint32_t w=*image->word(pc);
                if(pc-f->begin<0x80){self|=w==0xaa0003f4u;event|=w==0xaa0103f3u;}
                auto dest=nhk::arm64::branch_target(pc,w,true);if(!dest)continue;
                action|=*dest==t.imports[0];pointers|=*dest==t.imports[5];drop|=*dest==t.imports[8];
                // The normal consumer receives a BORROW of X19 and returns to
                // this owner's epilogue; this function never drops that handle.
                if(pc>=8&&*image->word(pc-4)==0xaa1303e1u
                    &&(*image->word(pc-8)&0xffc003ffu)==0x91000280u)borrowed=true;
            }
            if(!self||!event||!action||!pointers||!borrowed||drop)continue;
            if(t.scene&&t.scene!=f->begin)return {};t.scene=f->begin;
        }
    }
    t.home=h->begin;t.back=dispatch->begin;return t;
}
} // namespace home_gestures

// Installed through the existing source/preimage hook bank (including its ownership checks).
extern void *hc_home_touch_original;
extern void *hc_back_touch_original;
extern "C" void hc_home_touch(void*,void*,uintptr_t,uintptr_t);
extern "C" void hc_back_touch(void*,void*);
extern void *hc_workspace_scene_original;
extern "C" void hc_workspace_scene(void*,void*);
bool hc_gestures_bind(uintptr_t base,const home_gestures::Targets&);
void hc_gestures_config(const home_gestures::Config&);

// Called by the live bank only after BOTH original-body adapters are verified.
void hc_workspace_gestures_ready(bool ready);
void hc_workspace_scene_ready(bool ready);
