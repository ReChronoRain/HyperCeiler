#include "targets/home/home_recents_haptic_render.h"
#include "../runtime-feature-addressing/fixture_image.h"
#include "fixture.h"
#include <iostream>
using namespace home_layout::haptic;
static std::optional<Plan> identify(const Functions& f){auto bytes=image(f);auto c=Code::parse(bytes);return c?haptic::resolve(*c):std::nullopt;}
int main(){
    const auto fs=fixture();const auto p=identify(fs);
#ifdef HC_SHARED_LIST
    assert(p&&p->site_mask==0x77&&p->shared_list);
#else
    assert(p&&p->site_mask==0x13&&!p->shared_list);
#endif
    std::map<uint32_t,uint32_t> addresses;const auto moved=relocated(fs,addresses);auto r=identify(moved);assert(r);
    for(unsigned i=0;i<7;++i)if(p->site_mask&(1u<<i))
        assert(r->sites[i]==(i==5?addresses.at(p->sites[i]-31*4)+31*4:addresses.at(p->sites[i])));
    auto ambiguous=fs;ambiguous.insert(ambiguous.end(),moved.begin(),moved.end());assert(!identify(ambiguous));
    unsigned mutations=0;
    for(size_t owner=0;owner<fs.size();++owner){
        const auto refused=[&](const Functions& broken){auto result=identify(broken);assert(!result||(p->shared_list&&!result->shared_list&&result->site_mask==0x13));};
        auto missing=fs;missing.erase(missing.begin()+owner);refused(missing);
        // The viewport owner proves its paired inverse-index call edges rather
        // than its unrelated paint code. Small bodies are complete contracts.
        if(fs[owner].words.size()>512)continue;
        for(size_t i=0;i<fs[owner].words.size();++i){
            auto broken=fs;broken[owner].words[i]=0xd503201f;refused(broken);++mutations;
        }
    }
    // Borrow real register roles; all object fields come from the resolved plan.
    std::array<std::byte,65536> heap{};const uintptr_t base=uint64_t(1)<<32;
    const auto notification=base+0x101,metrics=base+0x201,pixels=base+0x301,layout=base+0x401,pool=base+0x1000;
    const auto put=[&](uintptr_t at,auto value){assert(at>=base&&at-base+sizeof(value)<=heap.size());std::memcpy(heap.data()+at-base,&value,sizeof(value));};
    const auto read=[&](uintptr_t at,void* out,size_t count){if(at<base||at-base>heap.size()||count>heap.size()-(at-base))return false;std::memcpy(out,heap.data()+at-base,count);return true;};
    recent_render::Registers s{};s.x[2]=notification;s.x[3]=layout;s.x[22]=base+0x901;s.x[27]=pool;s.x[28]=1;
    put(notification-1,uint64_t(p->start_cid)<<12);put(notification+p->depth,int64_t(0));
    put(notification+p->metrics,uint32_t(metrics));put(metrics+p->pixels,uint32_t(pixels));
    put(notification+p->drag_details,uint32_t(layout));put(pixels-1,uint64_t(p->double_cid)<<12);put(pixels+p->payload,123.5);
    const auto before=s;
    for(unsigned lane:{0u,1u,2u}){if(!(p->site_mask&(1u<<lane)))continue;const auto o=observe(s,*p,lane,read);assert(o.valid&&o.event==Event::Start&&o.value==123.5);}
    for(unsigned lane=0;lane<=7;++lane)if(!(p->site_mask&(1u<<lane)))assert(!observe(s,*p,lane,read).valid);
    put(notification+p->drag_details,uint32_t(s.x[22]));assert(observe(s,*p,0,read).programmatic);
    put(notification-1,uint64_t(p->update_cid)<<12);assert(observe(s,*p,0,read).event==Event::Update);
    put(notification+p->depth,int64_t(1));assert(!observe(s,*p,0,read).valid);put(notification+p->depth,int64_t(0));
    put(pixels-1,uint64_t(p->double_cid+1)<<12);assert(!observe(s,*p,0,read).valid);
    put(notification-1,uint64_t(p->end_cid)<<12);assert(observe(s,*p,0,read).event==Event::End);
    put(pool+p->horizontal_pool,uint64_t(0x1231));put(layout+p->axis,uint32_t(0x1231));put(layout+p->spacing,20.0);
    s.d(0,180);s.d(1,90);assert(observe(s,*p,4,read).value==110);
    put(layout+p->axis,uint32_t(0x2231));assert(observe(s,*p,4,read).value==200);
    if(p->shared_list){
        s.x[1]=layout;put(layout+p->stack_extent,75.0);s.d(2,15);
        const auto stack=observe(s,*p,6,read),linear=observe(s,*p,5,read);
        assert(stack.valid&&stack.extent&&stack.lane==2&&stack.value==75);
        assert(linear.valid&&linear.extent&&linear.lane==2&&linear.value==105);
        s.x[1]=before.x[1];s.d(2,before.d(2));
    }
    s.d(0,before.d(0));s.d(1,before.d(1));assert(std::memcmp(&s,&before,sizeof(s))==0);
    std::cout<<"Tracker grid haptic PASS: "<<mutations<<" corruptions; relocation, ownership, borrowed notifications/metrics, programmatic entry and independent family mask\n";
}
