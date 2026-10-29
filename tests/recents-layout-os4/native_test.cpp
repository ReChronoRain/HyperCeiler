/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_recent_render.h"
#include "../runtime-feature-addressing/fixture_image.h"
#include "recent_fixture.h"
#include <fstream>
#include <iostream>
#include <iterator>
using namespace home_layout::features::recent;
using namespace home_layout::recent_render;
static Plan resolve(const Functions& fs){auto bytes=image(fs);auto code=Code::parse(bytes);assert(code);auto p=recent::resolve(*code);if(!p){Plan debug;
#define DIAG(name) std::cerr<<#name<<"="<<recent::name(*code,debug)<<" ";
DIAG(scale_graph) DIAG(color_graph) DIAG(height_graph) DIAG(pad_graph) DIAG(radius_graph) DIAG(header_graph) DIAG(offset_graph) DIAG(background_graph) DIAG(inline_headers) DIAG(freeform_content) DIAG(height_consumers)
#undef DIAG
std::cerr<<"sites="<<debug.sites.size()<<"\n";
}assert(p);return *p;}
int main(int argc,char**argv){
    (void)&relocated; // The shared helper uses 8 KiB spacing; these larger owners need 64 KiB.
    const auto fixture=recent_fixture();const auto p=resolve(fixture);
    assert(p.sites.size()==expected_sites.size()+(p.corner_animation_ready?4:0));
    for(auto va:expected_sites)assert(std::any_of(p.sites.begin(),p.sites.end(),[&](auto&s){return s.va==va;}));
    std::map<uint32_t,uint32_t> addresses;
    for(size_t i=0;i<fixture.size();++i)addresses[fixture[i].va]=0x05000000u+uint32_t((fixture.size()-i)*0x10000);
    // Code inventory splits a leaf predicate at its interior boxing prologue.
    // Such adjacent fragments have a real fall-through edge and must move as
    // one unit. Relocating them apart would create an invalid input program.
    for(size_t i=1;i<fixture.size();++i){const auto& previous=fixture[i-1];
        const auto last=previous.words.back();
        if(previous.va+previous.words.size()*4==fixture[i].va
            &&(last&0xfc000000u)!=0x14000000u&&last!=0xd65f03c0u
            &&(last&0xffe0001fu)!=0xd4200000u)
            addresses[fixture[i].va]=addresses.at(previous.va)+uint32_t(previous.words.size()*4);
    }
    auto relocate_target=[&](uint32_t to){
        for(const auto& f:fixture)if(to>=f.va&&uint64_t(to)<uint64_t(f.va)+f.words.size()*4)
            return addresses.at(f.va)+to-f.va;
        return to;
    };
    Functions moved=fixture;
    for(size_t k=0;k<moved.size();++k){auto& f=moved[k];const auto old=f.va;f.va=addresses.at(old);
        for(size_t i=0;i<f.words.size();++i){auto& w=f.words[i];const auto op=w&0xfc000000u;
            if(op!=0x14000000u&&op!=0x94000000u)continue;
            const uint32_t to=old+uint32_t(i*4)+(int32_t(w<<6)>>6)*4;
            const auto target=relocate_target(to);
            const auto delta=int64_t(target)-f.va-int64_t(i*4);assert(delta>=-(1ll<<27)&&delta<(1ll<<27));
            w=op|(uint32_t(delta/4)&0x03ffffffu);
        }}
    auto relocated_plan=resolve(moved);assert(relocated_plan.sites.size()==p.sites.size());
    for(const auto& s:p.sites){const auto it=std::find_if(fixture.begin(),fixture.end(),[&](auto& f){return s.va>=f.va&&s.va<f.va+f.words.size()*4;});assert(it!=fixture.end());
        const auto address=addresses.at(it->va)+s.va-it->va;
        assert(std::any_of(relocated_plan.sites.begin(),relocated_plan.sites.end(),[&](auto& r){return r.va==address&&r.setting==s.setting;}));}
    // Move every rooted frame local independently of its old byte offset.
    // The original function's frame allocation grows with the shifted locals.
    auto frames=fixture;
    for(auto& f:frames){const auto frame_bytes=home_dynamic::frame_size(f.words);if(!frame_bytes||frame_bytes>4000)continue;
        bool fits=true;for(auto w:f.words)if((w&0x3b200c00u)==0x38000000u&&home_dynamic::rn(w)==29&&home_dynamic::imm9(w)<-248)fits=false;
        if(!fits)continue;
        f.words[2]+=8u<<10;
        for(auto& w:f.words)if((w&0x3b200c00u)==0x38000000u&&home_dynamic::rn(w)==29&&home_dynamic::imm9(w)<0)
            w=(w&~(511u<<12))|((uint32_t(home_dynamic::imm9(w)-8)&511u)<<12);
    }
    auto shifted_frames=resolve(frames);assert(shifted_frames.sites.size()==p.sites.size());
    for(size_t i=0;i<p.sites.size();++i)if(p.sites[i].action==Action::FrameScalar||p.sites[i].action==Action::FrameStyle)
        assert(shifted_frames.sites[i].frame==p.sites[i].frame-8);
    // A second complete background producer is ambiguity, not first-match wins.
    const auto bg=*std::find_if(p.sites.begin(),p.sites.end(),[](auto& s){return s.action==Action::Freeform;});
    auto duplicate=fixture;auto copy=*std::find_if(fixture.begin(),fixture.end(),[&](auto& f){return f.va==bg.va;});
    const auto old=copy.va;copy.va=0x04000000;
    for(size_t i=0;i<copy.words.size();++i){uint32_t to=0;if(home_dynamic::target(copy.words[i],old+i*4,to))copy.words[i]=0x94000000u|(uint32_t((int64_t(to)-copy.va-i*4)/4)&0x03ffffffu);}
    duplicate.push_back(copy);auto bytes=image(duplicate);auto code=Code::parse(bytes);assert(code&&!recent::resolve(*code));
    // Missing core production paths cannot leave an apparently complete bank.
    for(auto action:{Action::Freeform,Action::Style,Action::FrameStyle,Action::ScaleReturn}){
        auto bad=fixture;auto site=*std::find_if(p.sites.begin(),p.sites.end(),[&](auto& s){return s.action==action;});
        auto it=std::find_if(bad.begin(),bad.end(),[&](auto& f){return site.va>=f.va&&site.va<f.va+f.words.size()*4;});assert(it!=bad.end());
        bad.erase(it);auto image_bad=image(bad);auto parsed=Code::parse(image_bad);assert(parsed&&!recent::resolve(*parsed));
    }
    alignas(16) std::byte heap[2048]{},thread[1024]{},pool[0x200000]{},source[256]{},frame[2048]{};
    const auto top=reinterpret_cast<uintptr_t>(heap),th=reinterpret_cast<uintptr_t>(thread),pp=reinterpret_cast<uintptr_t>(pool),src=reinterpret_cast<uintptr_t>(source)+1,fp=reinterpret_cast<uintptr_t>(frame)+1024;
    const uint64_t base=0x100000000,caller=0x987654321,previous=0x123456789;
    store<uint64_t>(src-1,p.style_tag);store<uint64_t>(pp+p.color_space_pool,0x12345679);
    RecentConfig selected;selected.values={42,65,51,150,60,70,130,80,22,int32_t(0xff2468ac),int32_t(0xffaabbcc)};
    unsigned cases=0;
    for(const auto& s:p.sites){Registers r{};r.x[26]=th;r.x[27]=pp;r.x[29]=fp;r.x[30]=caller;r.branch=0xbeef;
        for(unsigned i=0;i<32;++i)r.d(i,100+i);
        store<uint64_t>(fp,previous);store<uint64_t>(fp+8,base+p.fraction_returns[0]);
        auto neutral=apply(p,s,r,RecentConfig{},base);assert(neutral==Result::Neutral&&r.branch==0xbeef);++cases;
        store<uint64_t>(th+p.style_top,top);store<uint64_t>(th+p.style_end,top+sizeof(heap));
        if(s.action==Action::PoolReturn)store<double>(pp+s.pool,.5);
        if(s.action==Action::Style)r.x[s.reg]=src;
        if(s.action==Action::FrameStyle)store<uint64_t>(fp+s.frame,src);
        if(s.action==Action::FreeformColor)r.x[0]=r.x[22]+0x20;
        if(s.action==Action::CornerFrame){store<uint64_t>(pp+s.pool,0x12345679);store<uint64_t>(fp+s.selector_frame,0x23456789);}
        auto result=apply(p,s,r,selected,base);assert(result==Result::Applied);++cases;
        if(s.action==Action::PoolReturn){assert(r.d(0)==.5*selected.values[s.setting]/100);assert(r.branch==0&&r.x[15]==fp+16&&r.x[29]==previous);}
        else if(s.action==Action::ScaleReturn){assert(r.d(0)==(103./104.)*.8);assert(r.x[30]==base+p.fraction_returns[0]);}
        else if(s.action==Action::Style||s.action==Action::FrameStyle){
            const auto value=s.action==Action::Style?r.x[s.reg]:load<uint64_t>(fp+s.frame);
            assert(value==top+1&&load<uint64_t>(value-1)==p.style_tag);
            assert(load<uint32_t>(value+p.style_font)==uint32_t(top+p.style_size+1));
            assert(load<double>(top+p.style_size+1+p.double_field)==22);
            assert(load<uint64_t>(src-1)==p.style_tag&&load<uint32_t>(src+p.style_font)==0);
        }else if(s.action==Action::Freeform){assert(r.branch==0&&r.x[0]==top+1);assert(load<uint64_t>(top)==p.color.tag);}
        else if(s.action==Action::FreeformColor){assert(r.branch==0xbeef&&r.x[s.reg]==top+1);}
        else if(s.action==Action::FrameScalar||s.action==Action::CornerFrame||s.action==Action::CornerSurface)assert(load<double>(fp+s.frame)==selected.values[s.setting]);
        else assert(r.d(s.reg)==(s.setting==RecentConfig::TaskHeight?selected.values[s.setting]/100.0:selected.values[s.setting]));
    }
    // The live freeform color path must preserve ordinary full-screen cards
    // and reserve no heap when its mode predicate is false.
    const auto live=*std::find_if(p.sites.begin(),p.sites.end(),[](auto& s){return s.action==Action::FreeformColor;});
    auto wrong_mode_edge=fixture;
    auto mode_owner=std::find_if(wrong_mode_edge.begin(),wrong_mode_edge.end(),[&](auto& f){
        return live.va>=f.va&&live.va<f.va+f.words.size()*4;});
    assert(mode_owner!=wrong_mode_edge.end());
    mode_owner->words[(live.va-mode_owner->va)/4-6]^=1u<<5;
    auto wrong_image=image(wrong_mode_edge);auto wrong_code=Code::parse(wrong_image);
    assert(wrong_code&&!recent::resolve(*wrong_code));
    Registers ordinary{};ordinary.x[22]=0x1001;ordinary.x[0]=ordinary.x[22]+0x30;
    ordinary.x[live.reg]=0x12345;ordinary.branch=0xbeef;
    assert(apply(p,live,ordinary,selected,base)==Result::Neutral);
    assert(ordinary.x[live.reg]==0x12345&&ordinary.branch==0xbeef);++cases;
    // Equal style constants in unrelated functions are not title evidence.
    const auto inline_site=*std::find_if(p.sites.begin(),p.sites.end(),[&](auto& site){
        return site.action==Action::Style&&site.reg==0;});
    auto decoys=fixture;const auto source_owner=*std::find_if(fixture.begin(),fixture.end(),[&](auto& f){
        return inline_site.va>=f.va&&inline_site.va<f.va+f.words.size()*4;});
    auto decoy=source_owner;decoy.va=0x04000000;
    for(size_t i=0;i<decoy.words.size();++i){uint32_t to=0;
        if(home_dynamic::target(decoy.words[i],source_owner.va+i*4,to))
            decoy.words[i]=0x94000000u|(uint32_t((int64_t(to)-decoy.va-i*4)/4)&0x03ffffffu);}
    decoys.push_back(decoy);assert(resolve(decoys).sites.size()==p.sites.size());
    for(unsigned alignment:{0u,8u,4u})for(unsigned room:{0u,48u,128u,192u,2048u}){
        store<uint64_t>(th+p.style_top,top+alignment);store<uint64_t>(th+p.style_end,top+room);
        Result why{};auto value=style(src,th,pp,selected,p,why);
        const auto bytes_needed=p.style_size+16+p.color.size;
        bool okay=alignment!=4&&room>alignment+bytes_needed;
        assert(value==(okay?top+alignment+1:src));assert(load<uint64_t>(th+p.style_top)==top+alignment+(okay?bytes_needed:0));++cases;
        store<uint64_t>(th+p.color.top,top+alignment);store<uint64_t>(th+p.color.end,top+room);
        auto c=make_color(th,pp,int32_t(0x80402010),p,why);okay=alignment!=4&&room>alignment+p.color.size;
        assert(c==(okay?top+alignment+1:0));
        if(okay){assert(load<double>(c+p.color.channels[0])==128./255.);assert(load<double>(c+p.color.channels[1])==64./255.);assert(load<uint32_t>(c+p.color_space)==0x12345679);}++cases;
    }
    // The original explicit/transparent background arm must stay transparent.
    Registers transparent{};transparent.x[1]=1;transparent.x[22]=0;transparent.branch=0xbeef;
    assert(apply(p,bg,transparent,selected,base)==Result::Neutral&&transparent.branch==0xbeef);
    // Foreign callers execute the original ratio division; no policy routing.
    auto dynamic=*std::find_if(p.sites.begin(),p.sites.end(),[](auto& s){return s.action==Action::ScaleReturn;});
    Registers foreign{};foreign.x[29]=fp;foreign.branch=0xbeef;store<uint64_t>(fp+8,0xdeadbeef);
    assert(apply(p,dynamic,foreign,selected,base)==Result::UnknownCaller&&foreign.branch==0xbeef);
    if(argc>1){std::ifstream in(argv[1],std::ios::binary);std::vector<char>raw((std::istreambuf_iterator<char>(in)),{});
        auto full=Code::parse(std::as_bytes(std::span(raw)));assert(full);auto found=recent::resolve(*full);assert(found);
        // Optional header witnesses are intentionally absent from the older
        // numeric-only fixture. A full image may add its six icon banks.
        auto numeric=found->sites;numeric.erase(std::remove_if(numeric.begin(),numeric.end(),[](auto&s){return s.action==Action::HideIconReturn||s.action==Action::IconChild;}),numeric.end());
        assert(numeric.size()==p.sites.size()+(found->corner_animation_ready?4:0)-(p.corner_animation_ready?4:0));
        auto anonymous=image(full->functions());auto stripped=Code::parse(anonymous);assert(stripped&&recent::resolve(*stripped));}
    std::cout<<"recents runtime graph and rendering PASS: "<<p.sites.size()<<" banks, "<<cases<<" execution/allocation cases; two heap alignments, unknown callers, graph relocation/ambiguity/missing links\n";
}
