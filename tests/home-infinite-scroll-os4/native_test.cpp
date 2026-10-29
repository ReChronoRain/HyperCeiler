/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_infinite_scroll.h"
#include "targets/home/home_infinite_scroll_config.h"
#include "fixture_image.h"
#include "infinite_fixture.h"
#include <iostream>
#include <fstream>
#include <iterator>
using namespace home_layout::infinite_scroll;
static auto identify(const Functions& f){const auto bytes=image(f);auto c=Code::parse(bytes);assert(c);return resolve(*c);}
struct Registers {uint64_t x[32]{};double q[32]{};double d(unsigned i)const{return q[i];}void d(unsigned i,double v){q[i]=v;}};
struct Memory {
    std::map<uintptr_t,unsigned char> bytes;
    template<class T>void put(uintptr_t at,T value){unsigned char b[sizeof(T)];std::memcpy(b,&value,sizeof(T));for(size_t i=0;i<sizeof(T);++i)bytes[at+i]=b[i];}
    template<class T>bool get(uintptr_t at,T& v)const{unsigned char b[sizeof(T)];for(size_t i=0;i<sizeof(T);++i){auto it=bytes.find(at+i);if(it==bytes.end())return false;b[i]=it->second;}std::memcpy(&v,b,sizeof(T));return true;}
};
int main(int argc,char** argv){
    const auto f=infinite_fixture();auto p=identify(f);assert(p&&p->spring_returns.size()==6&&p->forward_returns==std::vector<uint32_t>{0x15bd654});
    std::map<uint32_t,uint32_t> moved;const auto rebase=relocated(f,moved);auto rp=identify(rebase);
    assert(rp&&rp->entry==moved.at(p->entry)&&rp->spring==moved.at(p->spring)&&rp->drag==moved.at(p->drag));
    std::vector<uint32_t> moved_returns;
    for(auto pc:p->factory_returns)moved_returns.push_back(moved.at(p->entry)+(pc-p->entry));
    assert(rp->factory_returns==moved_returns);
    assert(rp->forward_returns==std::vector<uint32_t>{moved.at(0x15bd610)+68});
    assert(p->metrics_frame!=p->argument_metrics_frame&&rp->context_allocator==moved.at(p->context_allocator));
    auto context_move=f;auto& context_owner=*std::find_if(context_move.begin(),context_move.end(),[&](const auto& fn){return fn.va==p->entry;});
    context_owner.words[6]+=1u<<5; // original allocator now receives four compressed Context variables
    context_owner.words[12]+=8u<<12;
    for(auto& word:context_owner.words)if(home_dynamic::ldw(word,0,2)&&home_dynamic::imm9(word)==p->metrics_field)word+=8u<<12;
    auto moved_root=identify(context_move);assert(moved_root&&moved_root->metrics_field==p->metrics_field+8);
    auto lost_context=f;auto& lost=*std::find_if(lost_context.begin(),lost_context.end(),[&](const auto& fn){return fn.va==p->entry;});
    lost.words[12]+=4u<<12;assert(!identify(lost_context));
    for(uint32_t required:{p->entry,p->spring,p->drag,p->begin,p->offset,p->context_allocator}){auto broken=f;std::erase_if(broken,[&](const auto& fn){return fn.va==required;});assert(!identify(broken));}
    auto ambiguity=f;auto duplicate=*std::find_if(f.begin(),f.end(),[&](const auto& fn){return fn.va==p->entry;});auto old=duplicate.va;duplicate.va+=0x2000000;
    for(size_t i=0;i<duplicate.words.size();++i){uint32_t to=0;if(home_dynamic::target(duplicate.words[i],old+uint32_t(i*4),to))duplicate.words[i]=0x94000000u|(uint32_t((int64_t(to)-duplicate.va-i*4)/4)&0x3ffffffu);}
    ambiguity.push_back(duplicate);assert(!identify(ambiguity));
    // All heap and stack offsets move in production instructions, not a copied
    // hard-coded fixture Plan. Scalar boundary and drag fields are decoded anew.
    auto field_move=f;
    for(auto& fn:field_move)for(auto& w:fn.words){
        const int off=home_dynamic::imm9(w);
        if((w&0xffe00c00u)==0xb8400000u||((w&0xffe00c00u)==0xb8000000u))
            if(off==p->drag_field||off==p->direction)w+=4u<<12;
    }
    auto changed=identify(field_move);assert(changed&&changed->drag_field==p->drag_field+4&&changed->direction==p->direction+4);
    auto wrong_forwarder=f;
    auto& forwarder=*std::find_if(wrong_forwarder.begin(),wrong_forwarder.end(),[](const auto& fn){return fn.va==0x15bd610;});
    forwarder.words[13]+=1u<<5;auto wrong=identify(wrong_forwarder);assert(wrong&&wrong->forward_returns.empty());
    auto swapped_spring=f;
    auto& ctor=*std::find_if(swapped_spring.begin(),swapped_spring.end(),[&](const auto& fn){return fn.va==p->spring;});
    std::swap(ctor.words[4],ctor.words[5]);assert(!identify(swapped_spring));
    const uintptr_t base=0x10000000,heap=0x1234,root=(heap<<32)+0x1001,drag=(heap<<32)+0x2001,null=(heap<<32)+0x81;
    const uintptr_t outer=0x70008000,owner=0x70007fe0,child=0x70007f00,pool=0x30000000;
    unsigned checks=0;
    for(int pages:{1,2,3,8,20})for(int where:{0,1,2,3,4})for(double velocity:{-1400.,-1.,0.,1.,1400.}){
        Memory m;auto read=[&](uintptr_t at,auto& v){return m.get(at,v);};
        const double maximum=pages*420.,pixel=where==0?0:where==1?maximum:where==3?-87.036:where==4?maximum+87.036:420.*pages*.5;
        m.put(outer+p->caller_metrics,uint64_t(root));m.put(child,uint64_t(owner));
        // A real helper frame returns into this active factory, whose saved
        // parent and return PC must still match the original entry activation.
        uint32_t factory_return=0,parent_spring_return=0;
        for(const auto& fn:f)if(fn.va==p->entry)for(size_t i=0;i<fn.words.size();++i){uint32_t to=0;
            if(home_dynamic::target(fn.words[i],fn.va+uint32_t(i*4),to)&&to==0x15bbab4)factory_return=fn.va+uint32_t((i+1)*4);}
        for(const auto& fn:f)if(fn.va==0x15bbab4)for(size_t i=0;i<fn.words.size();++i){uint32_t to=0;
            if(home_dynamic::target(fn.words[i],fn.va+uint32_t(i*4),to)&&to==p->spring)parent_spring_return=fn.va+uint32_t((i+1)*4);}
        assert(factory_return&&parent_spring_return);m.put(child+8,uint64_t(base+factory_return));
        m.put(owner,uint64_t(outer));m.put(owner+8,uint64_t(base+p->caller));
        const uintptr_t context=(heap<<32)+0x7001;
        m.put(owner+p->argument_metrics_frame,uint64_t(null)); // reused by original configuration lookup
        m.put(owner+p->metrics_frame,uint64_t(context));m.put(context+p->metrics_field,uint32_t(root));
        m.put(root+p->drag_field,uint32_t(drag));m.put(drag+p->drag_owner,uint32_t(root));
        const int32_t offsets[]={p->minimum,p->maximum,p->pixels,p->viewport};const double values[]={0,maximum,pixel,420};
        for(unsigned i=0;i<4;++i){auto box=(heap<<32)+0x3001+i*32;m.put(root+offsets[i],uint32_t(box));m.put(box+7,values[i]);}
        m.put(pool+p->forward_pool,uint64_t((heap<<32)+0x6001));m.put(pool+p->reverse_pool,uint64_t((heap<<32)+0x6101));
        m.put(root+p->direction,uint32_t((where==0||where==3)?0x6001:0x6101));
        Registers r;r.x[29]=outer;r.x[15]=owner+16;r.x[30]=base+p->caller;r.x[2]=root;r.x[28]=heap;r.x[22]=null;r.x[27]=pool;r.d(0,velocity);Pending state;
        for(unsigned invalid=0;invalid<13;++invalid){
            auto bad=m;auto input=r;
            switch(invalid){
                case 0:input.x[30]+=4;break;
                case 1:bad.put(root+p->drag_field,uint32_t(null));break;
                case 2:bad.put(drag+p->drag_owner,uint32_t(root+16));break;
                case 3:bad.put((heap<<32)+0x3061+7,0.0);break;
                case 4:bad.put((heap<<32)+0x3061+7,std::numeric_limits<double>::quiet_NaN());break;
                case 5:bad.put((heap<<32)+0x3021+7,0.0);break;
                case 6:input.x[15]+=1;break;
                case 7:input.x[2]+=4;break;
                case 8:bad.put((heap<<32)+0x3021+7,maximum+20);break;
                case 9:bad.bytes.erase((heap<<32)+0x3001+7);break;
                case 10:input.d(0,0);bad.put(root+p->direction,uint32_t(null));break;
                case 11:bad.put((heap<<32)+0x3041+7,-421.0);break;
                case 12:bad.put((heap<<32)+0x3041+7,maximum+421.0);break;
            }
            auto before=input;Pending rejected{owner,1,0,maximum};
            assert(!enter(input,rejected,*p,base,[&](uintptr_t at,auto& value){return bad.get(at,value);}));
            assert(!rejected.frame&&std::memcmp(&input,&before,sizeof(input))==0);++checks;
        }
        const bool expected=(where==0||where==3)?velocity<=0:(where==1||where==4)?velocity>=0:false;
        // The original ScrollPhysics parent forwarder preserves metrics/velocity
        // but inserts its own Dart frame before reaching workspace physics.
        auto delegated=r;const uintptr_t bridge=outer-0x40;
        m.put(bridge,uint64_t(outer));
        // First bridge is entered from goBallistic, so its saved LR is the
        // verified root call return, while the factory's LR is its own BLR.
        m.put(bridge+8,uint64_t(base+p->caller));
        delegated.x[29]=bridge;delegated.x[15]=bridge-0x20;delegated.x[30]=base+0x15bd654;
        Pending via_parent;assert(enter(delegated,via_parent,*p,base,read)==expected);++checks;
        for(unsigned depth=1;depth<=9;++depth){
            auto nested=r;auto chained=m;
            for(unsigned level=depth;level;--level){const auto frame=outer-level*0x40;
                chained.put(frame,uint64_t(frame+0x40));chained.put(frame+8,uint64_t(base+(level==1?p->caller:0x15bd654)));}
            nested.x[29]=outer-depth*0x40;nested.x[15]=nested.x[29]-0x20;nested.x[30]=base+0x15bd654;
            Pending chain;auto before=nested;
            assert(enter(nested,chain,*p,base,[&](uintptr_t at,auto& v){return chained.get(at,v);})==(expected&&depth<=8));
            if(depth>8)assert(!chain.frame&&std::memcmp(&before,&nested,sizeof(nested))==0);++checks;
        }
        for(unsigned invalid=0;invalid<7;++invalid){
            auto broken=m;auto input=delegated;input.d(0,velocity);
            switch(invalid){case 0:input.x[30]+=4;break;
                case 1:broken.put(bridge+8,uint64_t(base+p->caller+4));break;
                case 2:broken.put(bridge,uint64_t(bridge));break;
                case 3:broken.put(bridge,uint64_t(outer+1));break;
                case 4:broken.put(bridge,uint64_t(bridge+0x10008));break;
                case 5:broken.bytes.erase(bridge);break;
                case 6:broken.put(outer+p->caller_metrics,uint64_t(root+4));break;}
            auto before=input;Pending denied;
            assert(!enter(input,denied,*p,base,[&](uintptr_t at,auto& v){return broken.get(at,v);}));
            assert(!denied.frame&&std::memcmp(&before,&input,sizeof(input))==0);++checks;
        }
        assert(enter(r,state,*p,base,read)==expected);++checks;
        if(!expected){assert(r.d(0)==velocity&&!state.frame);continue;}
        assert(state.velocity==velocity&&state.pixels==pixel&&state.viewport==420);
        Registers spring;spring.x[29]=child;spring.x[30]=base+parent_spring_return;spring.x[28]=heap;spring.x[22]=null;spring.d(0,pixel);spring.d(1,420);spring.d(2,r.d(0));
        // Original managed allocations may move the Context before the
        // spring constructor. Only its live Dart stack root is authoritative.
        const auto moved_context=context+0x1000;
        m.put(owner+p->metrics_frame,uint64_t(moved_context));m.put(moved_context+p->metrics_field,uint32_t(root));
        m.bytes.erase(context+p->metrics_field);
        auto unrelated=spring;unrelated.x[30]+=4;assert(!finish(unrelated,state,*p,base,read)&&state.frame);++checks;
        auto changed=spring;changed.x[29]=outer;assert(!finish(changed,state,*p,base,read));++checks;
        // A guarded factory may return without constructing a spring. Its
        // numeric frame address is later reused by a different physics owner,
        // with coincidentally valid Context, drag, bounds and viewport. The
        // stale release MUST NOT rewrite this later, unrelated animation.
        auto reused=m;reused.put(child+8,uint64_t(base+p->caller));
        auto stale=state;auto later=spring;later.x[29]=owner;const auto before_later=later;
        assert(!finish(later,stale,*p,base,[&](uintptr_t at,auto& v){return reused.get(at,v);}));
        assert(!stale.frame&&std::memcmp(&before_later,&later,sizeof(later))==0);++checks;
        // A helper at a reused frame cannot fake the original activation by
        // keeping only its old Context or its numerical stack address.
        for(unsigned invalid=0;invalid<5;++invalid){
            auto broken=m;auto input=spring;auto pending=state;const auto before=input;
            switch(invalid){
                case 0:broken.put(owner,uint64_t(outer+16));break;
                case 1:broken.put(owner+8,uint64_t(base+p->caller+4));break;
                case 2:broken.put(child+8,uint64_t(base+factory_return+4));break;
                case 3:broken.bytes.erase(child+8);break;
                case 4:broken.bytes.erase(owner+8);break;
            }
            assert(!finish(input,pending,*p,base,[&](uintptr_t at,auto& v){return broken.get(at,v);}));
            assert(std::memcmp(&before,&input,sizeof(input))==0);++checks;
        }
        // Both direct factory construction and verified helper construction
        // remain valid. No new leaf callback or synthetic Dart frame is needed.
        auto direct=spring;direct.x[29]=owner;
        for(const auto pc:p->spring_returns)if(std::find(p->factory_returns.begin(),p->factory_returns.end(),pc)!=p->factory_returns.end()){
            direct.x[30]=base+pc;auto pending=state;
            assert(finish(direct,pending,*p,base,read)&&!pending.frame);++checks;
        }
        auto resized=m;resized.put((heap<<32)+0x3061+7,280.0);
        auto pending_resize=state;auto before_resize=spring;
        assert(!finish(spring,pending_resize,*p,base,[&](uintptr_t at,auto& v){return resized.get(at,v);}));
        assert(std::memcmp(&spring,&before_resize,sizeof(spring))==0);++checks;
        // The live position may have changed, but animation parameters must
        // use one release snapshot, including its original pull and velocity.
        m.put((heap<<32)+0x3041+7,where==0||where==3?0.0:maximum);
        assert(finish(spring,state,*p,base,read));const double dest=(where==0||where==3)?maximum:0;
        // A constant start=end spring is already done on its first tick and
        // visibly snaps. Preserve an in-range offset on the destination page,
        // then let the original managed spring settle without traversing pages.
        const int direction=(where==0||where==3)?-1:1;
        const double displacement=(spring.d(0)-dest)*direction;
        assert(spring.d(1)==dest&&displacement>0&&displacement<=420*.25);
        if(where==3||where==4)assert(std::abs(displacement-87.036)<1e-9);
        assert(spring.d(0)>=0&&spring.d(0)<=maximum&&std::isfinite(spring.d(2)));
        assert(spring.d(2)*direction<=0&&std::abs(spring.d(2))<=displacement*4.0);
        if(velocity==0)assert(spring.d(2)==0);
        assert(!state.frame);++checks;
        assert(!finish(spring,state,*p,base,read));++checks;
        // A released drag cannot arm a second wrap from a spring bounce.
        m.put(root+p->drag_field,uint32_t(null));r.d(0,velocity);assert(!enter(r,state,*p,base,read)&&!state.frame);++checks;
    }
    for(auto wire:std::vector<std::vector<int32_t>>{{},{home_layout::kInfiniteScrollMagic,0},{home_layout::kInfiniteScrollMagic,1},{home_layout::kInfiniteScrollMagic},{home_layout::kInfiniteScrollMagic,2},{17,1}}){
        size_t at=0;bool enabled=true,present=false;auto ok=home_layout::read_infinite_scroll([&](int32_t& v){if(at==wire.size())return false;v=wire[at++];return true;},enabled,true,&present);
        bool valid=wire.empty()||(wire.size()==2&&wire[0]==home_layout::kInfiniteScrollMagic&&(wire[1]==0||wire[1]==1));assert(ok==valid);if(ok)assert(enabled==(!wire.empty()&&wire[1]==1));++checks;}
    if(argc>1){std::ifstream in(argv[1],std::ios::binary);std::vector<char> raw{std::istreambuf_iterator<char>(in),{}};auto full=Code::parse({reinterpret_cast<std::byte*>(raw.data()),raw.size()});assert(full);auto a=resolve(*full);assert(a&&a->entry==p->entry&&a->spring==p->spring&&a->metrics_frame==p->metrics_frame&&a->metrics_field==p->metrics_field&&a->context_allocator==p->context_allocator);std::cout<<"Full launcher image discovery PASS without symbols or sections\n";}
    std::cout<<"Infinite scroll PASS "<<checks<<" runtime cases, stripped fixture, independent relocation, field moves, missing/ambiguous graphs, manual edge releases, coherent release snapshots and bounded original spring settling\n";
}
