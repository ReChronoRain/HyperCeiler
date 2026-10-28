/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_feature_code.h"
#include "home_title_render.h"
#include "home_recent_config.h"
#include "home_custom_title_feature.h"
#include <set>
namespace home_layout::features {
namespace recent {
using namespace home_dynamic;
inline bool epi(const Body& b,size_t i){return i+2<b.size()&&b[i]==0xaa1d03efu&&b[i+1]==0xa8c179fdu&&b[i+2]==0xd65f03c0u;}
inline bool pooladd(uint32_t w,unsigned reg){return (w&0xffc003ffu)==(0x91400360u|reg);}
inline uint32_t pooloff(uint32_t a,uint32_t l){return ((a>>10)&4095)*4096+uoff(l);}
inline bool dp(uint32_t w,unsigned base,unsigned reg){return (w&0xffc003ffu)==(0xfd400000u|(base<<5)|reg);}
inline bool straight(const Body& b,size_t i) {
    if(i+4>b.size())return false;
    for(size_t j=i;j<i+4;++j){auto w=b[j];
        if((w&0x7c000000u)==0x14000000u||(w&0xff000010u)==0x54000000u
            ||(w&0x7e000000u)==0x34000000u||(w&0x7e000000u)==0x36000000u
            ||(w&0xfffffc1fu)==0xd63f0000u)return false;
    }
    // No original internal edge may enter the middle of the replaced bank.
    for(size_t j=0;j<b.size();++j){const auto w=b[j];int64_t to=-1;
        if((w&0xfc000000u)==0x14000000u)to=int64_t(j)+(int32_t(w<<6)>>6);
        else if((w&0xff000010u)==0x54000000u||(w&0x7e000000u)==0x34000000u)to=int64_t(j)+(int32_t(w<<8)>>13);
        else if((w&0x7e000000u)==0x36000000u)to=int64_t(j)+(int32_t(w<<13)>>18);
        if(to>int64_t(i)&&to<int64_t(i+4))return false;
    }
    return true;
}
inline bool pattern(const Body& b,const uint32_t* words,size_t n,uint32_t& out) {
    unsigned hits=0;size_t selected=0;
    for(size_t i=0;i+n<=b.size();++i){bool match=true;
        for(size_t j=0;j<n;++j){uint32_t mask=pooladd(words[j],rd(words[j]))?0xffc003ffu:operand_mask(words[j]);
            if((b[i+j]&mask)!=(words[j]&mask)){match=false;break;}}
        if(match){++hits;selected=i*4;}}
    if(hits!=1)return false;out=uint32_t(selected);return true;
}
enum class Action {Scalar,FrameScalar,PoolReturn,ScaleReturn,Style,FrameStyle,Freeform,ScalarReturn,FreeformColor,HideIconReturn,IconChild,CornerFrame,CornerSurface};
struct Site {
    uint32_t va=0;Action action{};int setting=0;unsigned reg=0;
    int32_t frame=0;uint32_t pool=0;bool multiply=false;
    unsigned text_object=32; // Fresh Text object, not a persistent Dart reference.
    bool compressed_icon=false;
    int32_t selector_frame=0;
};
struct Plan {
    std::vector<Site> sites;
    std::set<uint32_t> card_roots,witnesses;std::array<uint32_t,2> header_pools{};
    std::array<uint32_t,3> fraction_returns{};uint32_t width_selector=0,stack_fraction=0;
    ColorLayout color{};int32_t color_space=0;uint32_t color_space_pool=0;
    uint32_t title_pool=0,text_allocator=0,header_content=0,metrics_owner=0;
    uint32_t style_tag=0,style_size=0,style_top=0,style_end=0,double_tag=0;
    int32_t style_font=0,style_color=0,text_style=0,double_field=0;
    int32_t text_data=0,string_length=0;
    uint32_t empty_string_pool=0,empty_widget_pool=0,inline_header=0;
    uint32_t string_one_cid=0,string_two_cid=0,text_tag=0;
    bool hide_icon_ready=false,hide_text_ready=false,corner_animation_ready=false;
    uint32_t radius_factory=0,border_factory=0;
};
inline bool add(Plan& p,const Function& f,size_t i,Action action,int setting,unsigned reg=0,
    int32_t frame=0,uint32_t pool=0,bool multiply=false) {
    if(action!=Action::Freeform&&!straight(f.words,i))return false;
    Site s{f.va+uint32_t(i*4),action,setting,reg,frame,pool,multiply};
    for(const auto& old:p.sites)if(old.va==s.va)return false;
    p.sites.push_back(s);p.witnesses.insert(f.va);return true;
}
// Width mode is established by the original enum's ordered four-way dispatcher,
// never by the getters' nearly identical shapes or a pool address.
inline bool selector(const Function& f,std::array<uint32_t,4>& targets) {
    const auto& b=f.words;
    if(b.size()<33||b[0]!=0xa9bf79fdu||b[1]!=0xaa0f03fdu||!ldx(b[2],1,0)
        ||!field(imm9(b[2])))return false;
    constexpr uint32_t shape[]={0xf100081f,0x5400022c,0xf100041f,0x5400016c,
        0xf100001f,0x540000ac,0x94000000,0xaa1d03ef,0xa8c179fd,0xd65f03c0,
        0x94000000,0xaa1d03ef,0xa8c179fd,0xd65f03c0,
        0x94000000,0xaa1d03ef,0xa8c179fd,0xd65f03c0,
        0xf100101f,0x540000ec,0xf1000c1f,0x540000ac,
        0x94000000,0xaa1d03ef,0xa8c179fd,0xd65f03c0,
        0x1e6e1000,0xaa1d03ef,0xa8c179fd,0xd65f03c0};
    // Branch immediates here are local CFG topology, not image-specific addresses.
    for(size_t j=0;j<std::size(shape);++j){const auto mask=(shape[j]&0xfc000000u)==0x94000000u?0xfc000000u:0xffffffffu;
        if((b[j+3]&mask)!=(shape[j]&mask))return false;}
    for(unsigned j=0;j<4;++j){size_t i=std::array<size_t,4>{9,13,17,25}[j];
        if(!target(b[i],f.va+uint32_t(i*4),targets[j]))return false;}
    return std::set<uint32_t>(targets.begin(),targets.end()).size()==4;
}
struct Fraction {uint32_t scale=0,ret=0,pool=0;size_t terminal=0;};
inline bool fraction(const auto& code,uint32_t va,Fraction& out) {
    Body b;if(!code.body(va,b)||!frame_size(b))return false;
    std::optional<Fraction> found;
    for(size_t i=2;i+8<b.size();++i){uint32_t scale=0;
        if(!target(b[i],va+uint32_t(i*4),scale)||!epi(b,i+1)
            ||!pooladd(b[i+4],17)||!dp(b[i+5],17,0)||!epi(b,i+6)
            ||!ldx(b[i-2],29,1)||!frame_slot(b,imm9(b[i-2]))||!ux(b[i-1],27,4))continue;
        Body s;if(!code.body(scale,s))continue;
        constexpr uint32_t ratio[]={0xfc417021,0xfc407022,0x1e623823,0xb840b001,0x8b1c8021,
            0xfc417021,0xfc407022,0x1e623824,0x1e641860,0xaa1d03ef,0xa8c179fd,0xd65f03c0};
        uint32_t at=0;if(!pattern(s,ratio,std::size(ratio),at))continue;
        if(found)return false;found=Fraction{scale,va+uint32_t((i+1)*4),pooloff(b[i+4],b[i+5]),i+4};
    }
    if(!found)return false;
    if(!passes_return_points(b,std::array<size_t,2>{(found->ret-va)/4-1,found->terminal}))return false;
    out=*found;return true;
}
// The enum switch alone cannot identify portrait/landscape/vertical roles:
// prove those callees again in their actual rectangle/grid consumers. Reordered
// enums or same-shaped scalar decoys cannot silently swap the UI controls.
inline bool width_roles(const auto& code,const std::array<uint32_t,4>& t,std::set<uint32_t>& witnesses) {
    bool horizontal=false,vertical=false;
    for(const auto& f:code.functions()){const auto& b=f.words;if(!frame_size(b))continue;
        constexpr uint32_t h[]={0x94000000,0x37200080,0x94000000,0x4ea01c02,0x14000009,
            0xf85f83a0,0x37200080,0x94000000,0x4ea01c01,0x14000003,0x94000000,
            0x4ea01c01,0x4ea11c22,0xfc5f03a1,0x1e620820,0xaa1d03ef,0xa8c179fd,0xd65f03c0};
        uint32_t at=0,to0=0,to1=0,to2=0;
        if(pattern(b,h,std::size(h),at)&&target(b[at/4+2],f.va+at+8,to0)
            &&target(b[at/4+7],f.va+at+28,to1)&&target(b[at/4+10],f.va+at+40,to2)
            &&to0==t[1]&&to1==t[3]&&to2==t[1]){
            if(horizontal)return false;horizontal=true;witnesses.insert(f.va);
        }
        constexpr uint32_t v[]={0xfc407001,0xfc1c03a1,0x94000000,0xfc5c03a1,
            0x1e600822,0xfc1b03a2,0xf9403f40};
        if(pattern(b,v,std::size(v),at)&&target(b[at/4+2],f.va+at+8,to0)&&to0==t[0]
            &&imm9(b[at/4+1])==imm9(b[at/4+3])){
            if(vertical)return false;vertical=true;witnesses.insert(f.va);
        }
    }
    return horizontal&&vertical;
}
inline bool scale_graph(const auto& code,Plan& p) {
    std::optional<std::array<uint32_t,4>> selected;uint32_t owner=0;
    for(const auto& f:code.functions()) {std::array<uint32_t,4> t{};
        if(!selector(f,t))continue;
        Fraction v,h,l;if(!fraction(code,t[0],v)||!fraction(code,t[1],h)||!fraction(code,t[3],l)
            ||v.scale!=h.scale||h.scale!=l.scale||v.pool==h.pool||h.pool==l.pool||v.pool==l.pool)continue;
        if(selected)return false;selected=t;owner=f.va;
    }
    if(!selected)return false;const auto t=*selected;
    if(!width_roles(code,t,p.witnesses))return false;
    p.card_roots.insert(t.begin(),t.end());p.witnesses.insert(owner);p.width_selector=owner;p.stack_fraction=t[2];
    Fraction v,h,l;fraction(code,t[0],v);fraction(code,t[1],h);fraction(code,t[3],l);
    const Fraction rows[]={v,h,l};const int settings[]={RecentConfig::Vertical,RecentConfig::HorizontalPortrait,RecentConfig::HorizontalLandscape};
    for(unsigned i=0;i<3;++i){p.fraction_returns[i]=rows[i].ret;Body b;code.body(t[i==2?3:i],b);
        Function f{t[i==2?3:i],b};if(!add(p,f,rows[i].terminal,Action::PoolReturn,settings[i],0,int32_t(((b[rows[i].terminal]>>10)&4095)*4096),rows[i].pool,true))return false;}
    Body b;code.body(h.scale,b);size_t hits=0;
    for(size_t i=0;i+3<b.size();++i)if(b[i]==0x1e641860u&&epi(b,i+1)) {
        if(!add(p,Function{h.scale,b},i,Action::ScaleReturn,0))return false;++hits;}
    return hits==1;
}
inline bool allocator(const auto& code,uint32_t va,uint32_t& tag_value,uint32_t& top,uint32_t& end) {
    Body stub,nursery;if(!code.prefix(va,3,stub)||!tag(stub,tag_value)
        ||(stub[2]&0xfc000000u)!=0x14000000u)return false;
    uint32_t to=0;if(!target(stub[2]|0x80000000u,va+8,to)||!code.prefix(to,7,nursery)
        ||nursery[0]!=0xd3482c44u||nursery[1]!=0xd37cec84u
        ||(nursery[2]&0xffc07fffu)!=0xa9401740u||nursery[3]!=0x8b040003u
        ||nursery[4]!=0xeb0300bfu||!ux(nursery[6]^0x00400000u,26,3))return false;
    top=((nursery[2]>>15)&127)*8;end=top+8;
    return top&&uoff(nursery[6])==top;
}
// Channel order is not inferred from store order alone. The three actual
// byte getters must share a converter that reads precisely these four fields
// and packs A/R/G/B at 24/16/8/0 bits.
inline bool color_consumers(const auto& code,const Function& owner,const ColorLayout& c) {
    const auto& b=owner.words;uint32_t converter=0;
    const uint32_t masks[]={0x12101c01u,0x12181c01u,0x12001c01u};
    const uint32_t shifts[]={0x9350fc20u,0x9348fc20u,0xaa0103e0u};
    for(unsigned k=0;k<3;++k){const size_t i=6+k*3;uint32_t getter=0,next=0;Body g;
        if(i>=b.size()||!target(b[i],owner.va+uint32_t(i*4),getter)||!code.prefix(getter,11,g)
            ||g[0]!=0xa9bf79fdu||g[1]!=0xaa0f03fdu||g[2]!=0xaa0103e0u
            ||!target(g[3],getter+12,next)||g[4]!=0xd3407c00u||g[5]!=masks[k]
            ||g[6]!=0xd3407c21u||g[7]!=shifts[k]||!epi(g,8))return false;
        if(k&&next!=converter)return false;converter=next;
    }
    Body pack;if(!code.prefix(converter,31,pack)||!frame_size(pack))return false;
    constexpr size_t fields[]={4,9,16,23},calls[]={5,10,17,24};
    uint32_t channel_converter=0;
    for(unsigned k=0;k<4;++k){uint32_t to=0;
        if(!ldd(pack[fields[k]],k?0:1,0)||imm9(pack[fields[k]])!=c.channels[k]
            ||!target(pack[calls[k]],converter+uint32_t(calls[k]*4),to))return false;
        if(k&&to!=channel_converter)return false;channel_converter=to;
    }
    return pack[6]==0xd3689c01u&&pack[11]==0xd370bc01u&&pack[18]==0xd378dc01u
        &&pack[13]==0xaa010002u&&pack[20]==0xaa010002u&&pack[26]==0xaa000022u&&epi(pack,28);
}
inline bool color_graph(const auto& code,Plan& p) {
    std::optional<Plan> found;
    for(const auto& f:code.functions()){const auto& b=f.words;ColorLayout c;
        if(!frame_size(b))continue;
        // Alpha normalization followed by one typed allocation, ColorSpace store,
        // four channel stores, and three component-producing calls.
        constexpr uint32_t conversion[]={0xd3407c21,0x12001c22,0xd3407c42,0x9e620040,0xfd6e1b61,0x1e611802,0xfc1d03a2};
        uint32_t at=0;if(!pattern(b,conversion,std::size(conversion),at))continue;
        const size_t a=at/4+std::size(conversion);uint32_t factory=0,tag_value=0,top=0,end=0;
        if(a+5>=b.size()||!target(b[a],f.va+uint32_t(a*4),factory)
            ||!pooladd(b[a+1],1)||!ux(b[a+2],1,1)||!mem(b[a+3],0xb8000000u,0,1)
            ||!allocator(code,factory,tag_value,top,end)||!color_fields(b,tag_value,c))continue;
        c.top=top;c.end=end;if(!c.usable()||!color_consumers(code,f,c))continue;
        if(found)return false;Plan next=p;next.color=c;
        next.witnesses.insert(f.va);next.color_space=imm9(b[a+3]);next.color_space_pool=pooloff(b[a+1],b[a+2]);found=next;
    }
    if(!found)return false;p=*found;return true;
}
inline bool height_graph(const auto& code,Plan& p) {
    std::optional<Site> found;
    // Device-dependent scalar -> fresh Double publication -> return. The
    // publication is shared by the original fast and slow allocation paths.
    constexpr uint32_t choose[]={0xb84e7022,0x8b1c8042,0x37200082,0x91413771,0xfd453a20,
        0x14000003,0x91413771,0xfd449a20,0xa9460740,0x91004000,0xeb00003f};
    for(const auto& f:code.functions()) {const auto& b=f.words;uint32_t at=0;
        if(!frame_size(b)||!pattern(b,choose,std::size(choose),at))continue;
        size_t a=at/4;
        if(a+20>=b.size()||!epi(b,a+18)||!std_(b[a+17],0,0)
            ||(b[a+15]&0xffe0001fu)!=0xf2a00001u||!movz(b[a+14],1))continue;
        if(found)return false;found=Site{f.va+uint32_t((a+17)*4),Action::Scalar,RecentConfig::HeaderHeight,0};
        p.witnesses.insert(f.va);p.header_pools={pooloff(b[a+3],b[a+4]),pooloff(b[a+6],b[a+7])};
    }
    if(!found)return false;Body b; // The current header unit is a logical pixel, as in the UI.
    p.sites.push_back(*found);return true;
}
inline bool pad_graph(const auto& code,Plan& p) {
    std::optional<Site> found;
    constexpr uint32_t dimensions[]={0xfc417046,0xfc407047,0x1e6738c8,0xfc1b83a8,
        0xfc41f046,0xfc40f047,0x1e6738c9,0x1e600926,0x1e620900,0x1e650922,
        0xfc1c03a2,0x1e610905,0xfc1c83a5,0x1e630901,0xfc1d03a1,0x1e640923,0xfc1d83a3};
    for(const auto& f:code.functions()){const auto& b=f.words;uint32_t at=0;
        if(!frame_size(b)||!pattern(b,dimensions,std::size(dimensions),at))continue;
        // Prove an actual caller consumes both dimensions and sends them to a
        // card-bounds builder. The six input scalars' roles come from the math.
        unsigned users=0;for(const auto& caller:code.functions())for(size_t i=0;i+7<caller.words.size();++i){uint32_t to=0;
            const auto& c=caller.words;
            if(target(c[i],caller.va+uint32_t(i*4),to)&&to==f.va&&c[i+1]==0xaa0003e1u
                &&ldx(c[i+2],29,0)&&stx(c[i+3],29,1)&&ldd(c[i+4],0,0)
                &&std_(c[i+5],29,0)&&ldd(c[i+6],0,1)&&std_(c[i+7],29,1)){
                    ++users;p.card_roots.insert(caller.va);p.witnesses.insert(caller.va);
                    // Admit the actual card-bounds consumer, not shared VM
                    // allocators/loggers that happen to be called by this owner.
                    for(size_t j=0;j<c.size();++j){uint32_t callee=0;Body child;
                        if(!target(c[j],caller.va+uint32_t(j*4),callee)||!code.body(callee,child))continue;
                        constexpr uint32_t bounds[]={0xfc40f023,0x1e633804,0x1e641820,0x372000a2,
                            0x9140af71,0xfd40a221,0x1e610803,0x4ea31c60};
                        uint32_t where=0;if(pattern(child,bounds,std::size(bounds),where))p.card_roots.insert(callee);
                    }
                }}
        if(users!=1||!straight(b,0))continue;
        if(found)return false;found=Site{f.va,Action::Scalar,RecentConfig::TaskHeight,0};
    }
    if(!found)return false;p.sites.push_back(*found);return true;
}
inline bool radius_graph(const auto& code,Plan& p) {
    std::optional<uint32_t> builder;std::vector<Site> sites;
    constexpr uint32_t circle[]={0xfc007000,0xfc00f000,0x94000000,0xaa0003e1,
        0xf85f03a0,0xb8007020,0xb800b020,0xb800f020,0xb8013020,0xaa0103e0,
        0xaa1d03ef,0xa8c179fd,0xd65f03c0};
    for(const auto& f:code.functions()){const auto& b=f.words;uint32_t at=0;
        if(!frame_size(b)||!pattern(b,circle,std::size(circle),at)||at<20)continue;
        // Rounded and rotated screen-corner arms must share the Radius and
        // BorderRadius allocators and the same saved input scalar.
        unsigned stores=0,circles=0;
        for(auto w:b)if(std_(w,0,0)||std_(w,0,1))++stores;
        for(auto w:b)if(w==0xb8007020u)++circles;
        if(circles!=2||stores<4||!std_(b[3],29,0))continue;
        if(builder)return false;builder=f.va;
    }
    if(!builder)return false;
    Body bb;code.body(*builder,bb);
    uint32_t circle_at=0;
    if(pattern(bb,circle,std::size(circle),circle_at)&&circle_at>=12){
        const size_t a=circle_at/4;
        if(ldd(bb[a-2],29,0)&&stx(bb[a-1],29,0)){
            (void)target(bb[a-3],*builder+uint32_t((a-3)*4),p.radius_factory);
            (void)target(bb[a+2],*builder+uint32_t((a+2)*4),p.border_factory);
        }
    }
    if(!add(p,Function{*builder,bb},0,Action::Scalar,RecentConfig::Corners,0))return false;
    // One thumbnail builder computes a common corner scalar before all of its
    // card-mode branches and calls that exact rounded-corner factory twice.
    bool thumbnail=false,animated=false;
    for(const auto& f:code.functions()){const auto& b=f.words;if(!frame_size(b))continue;
        unsigned links=0;for(size_t i=0;i<b.size();++i){uint32_t to=0;if(target(b[i],f.va+uint32_t(i*4),to)&&to==*builder)++links;}
        if(links==2){
            constexpr uint32_t join[]={0x1e669000,0xf85f83a0,0xfc1583a0,0xb840b001,0x8b1c8021,0x6b16003f};
            uint32_t at=0;if(!pattern(b,join,std::size(join),at))continue;
            if(thumbnail||!add(p,f,at/4+1,Action::Scalar,RecentConfig::Corners,0))return false;thumbnail=true;
        }
        if(links==1){
            constexpr uint32_t fresh[]={0xfc5e83a0,0xf81f83a0,0xfc007000,0xfc00f000,0x94000000,
                0xaa0003e1,0xf85f83a0,0xb8007020,0xb800b020,0xb800f020,0xb8013020};
            uint32_t at=0;if(!pattern(b,fresh,std::size(fresh),at))continue;
            if(animated||!add(p,f,at/4,Action::FrameScalar,RecentConfig::Corners,0,imm9(b[at/4])))return false;animated=true;
        }
    }
    if(!thumbnail||!animated)return false;
    std::optional<uint32_t> stacked;
    // Recover the scalar corner source used by the gesture/surface animation,
    // linked to the exact stack-width producer already selected above.
    for(const auto& f:code.functions()){const auto& b=f.words;
        constexpr uint32_t tail[]={0x1e669000,0xaa1d03ef,0xa8c179fd,0xd65f03c0,
            0x94000000,0xfc5f83a1,0x1e600822,0x1e67d001,0x4e62f420,0xaa1d03ef,0xa8c179fd,0xd65f03c0};
        uint32_t at=0,to=0;if(!frame_size(b)||!pattern(b,tail,std::size(tail),at)
            ||!target(b[at/4+4],f.va+at+16,to)||to!=p.stack_fraction)continue;
        if(stacked)return false;stacked=f.va;p.witnesses.insert(f.va);
    }
    if(!stacked)return false;bool source=false,current=false;
    for(const auto& f:code.functions()){const auto& b=f.words;
        constexpr uint32_t fork[]={0x1e669000,0xaa1d03ef,0xa8c179fd,0xd65f03c0,
            0x94000000,0xaa1d03ef,0xa8c179fd,0xd65f03c0};
        uint32_t at=0,to=0;
        if(frame_size(b)&&pattern(b,fork,std::size(fork),at)
            &&target(b[at/4+4],f.va+at+16,to)&&to==*stacked){
            if(source||!add(p,f,0,Action::ScalarReturn,RecentConfig::Corners))return false;source=true;
        }
        constexpr uint32_t product[]={0xaa0003e1,0x94000000,0xf85f83a0,0xfc44f001,
            0x1e600822,0x4ea21c40,0xaa1d03ef,0xa8c179fd,0xd65f03c0};
        if(frame_size(b)&&pattern(b,product,std::size(product),at)
            &&target(b[at/4+1],f.va+at+4,to)&&to==p.width_selector){
            // The no-width arm returns this very same cached radius field.
            bool cached=false;
            for(size_t j=0;j+3<b.size();++j)if(ldd(b[j],0,0)&&imm9(b[j])==imm9(b[at/4+3])&&epi(b,j+1))cached=true;
            if(!cached)continue;
            if(current||!add(p,f,0,Action::ScalarReturn,RecentConfig::Corners))return false;current=true;
        }
    }
    return source&&current;
}

// Flight-card radii have their own target and in-flight lerp, bypassing
// thumbnail/steady-state getters. Link BOTH owners through the original stack
// radius producer, typed Radius/BorderRadius factories, mode discriminator and
// Tween end store. Preserve screen-radius beginnings and original interpolation.
inline bool corner_animation_graph(const auto& code,Plan& p) {
    if(!p.radius_factory||!p.border_factory||!p.stack_fraction)return false;
    std::optional<Function> update,creator;uint32_t stack=0;size_t update_site=0,creator_site=0;
    int32_t target_frame=0,mode_frame=0;uint32_t mode_pool=0,tween_factory=0;
    constexpr uint32_t inflight[]={0x94000000,0x4ea01c03,0x14000002,0x1e669003,
        0xfc5c83a0,0xfc5d83a2,0xfc5d03a1,0x1e610864,0x1e621881,0xfc1d03a1,
        0x94000000,0xfc5d03a0,0xf81e83a0,0xfc007000,0xfc00f000,0x94000000,
        0xaa0003e4,0xf85e83a0,0xf81e03a4,0xb8007080,0xb800b080,0xb800f080,0xb8013080};
    for(const auto& f:code.functions()){const auto& b=f.words;uint32_t at=0,source=0,radius=0,border=0;
        if(!frame_size(b)||!pattern(b,inflight,std::size(inflight),at))continue;const size_t i=at/4;
        if(!target(b[i],f.va+at,source)||!target(b[i+10],f.va+at+40,radius)||radius!=p.radius_factory
            ||!target(b[i+15],f.va+at+60,border)||border!=p.border_factory
            ||imm9(b[i+9])!=imm9(b[i+11])||imm9(b[i+12])!=imm9(b[i+17]))continue;
        Body helper;uint32_t tail=0,ratio=0;
        constexpr uint32_t stack_tail[]={0x94000000,0xfc5f03a1,0x1e600822,0x1e67d003,
            0x1e612060,0x5400006b,0x1e669000,0x14000003,0x4e62f461,0x4ea11c20,
            0xaa1d03ef,0xa8c179fd,0xd65f03c0};
        if(!code.body(source,helper)||!pattern(helper,stack_tail,std::size(stack_tail),tail)
            ||!target(helper[tail/4],source+tail,ratio)||ratio!=p.stack_fraction)continue;
        if(update)return false;update=f;update_site=i+4;stack=source;
    }
    if(!update)return false;
    for(const auto& f:code.functions()){const auto& b=f.words;if(!frame_size(b))continue;
        // The SAME enum arm calls the proved stacked source, the other arm
        // selects stock 20; target local later becomes Tween.end, not begin.
        for(size_t i=3;i+8<b.size();++i){uint32_t source=0;
            if(!pooladd(b[i-3],16)||!ux(b[i-2],16,16)||b[i-1]!=0x6b10001fu
                ||(b[i]&0xff00001fu)!=0x54000001u||!ldx(b[i+1],29,0)
                ||!target(b[i+2],f.va+uint32_t((i+2)*4),source)||source!=stack)continue;
            // The enum compared in w0 was just restored from its entry root.
            if(i<6||!ldx(b[i-6],29,0)||!frame_slot(b,imm9(b[i-6])))continue;
            const auto enum_frame=imm9(b[i-6]);const auto pool=pooloff(b[i-3],b[i-2]);
            constexpr uint32_t choose[]={0x1e669001,0xfc5d83a0,0xf85e83a0,0xfc1c83a1,
                0x94000000,0xaa0003e2,0xfc5d83a0,0x1e641001,0xf81f83a2,0x1e610802};
            uint32_t chosen=0;if(!pattern(b,choose,std::size(choose),chosen))continue;
            const size_t c=chosen/4;const auto frame=imm9(b[c+3]);
            if(!frame_slot(b,frame)||frame==enum_frame||imm9(b[c+1])!=imm9(b[c+6]))continue;
            const int64_t default_at=int64_t(i)+(int32_t(b[i]<<8)>>13);
            if(default_at!=int64_t(c))continue;
            bool end=false;
            for(size_t j=c+10;j+8<b.size();++j){
                if(!ldd(b[j],29,0)||imm9(b[j])!=frame||!std_(b[j+1],29,1)
                    ||(b[j+2]&0xfc000000u)!=0x94000000u||b[j+3]!=0xaa0003e2u
                    ||!ldd(b[j+4],29,0)||!stx(b[j+5],29,2)||!std_(b[j+6],2,0)
                    ||!ldd(b[j+7],29,0)||imm9(b[j+7])!=frame||!std_(b[j+8],2,0)
                    ||imm9(b[j+8])!=imm9(b[j+6])+8)continue;
                uint32_t factory=0;
                if(!target(b[j+2],f.va+uint32_t((j+2)*4),factory))continue;
                if(end)return false;end=true;tween_factory=factory;
            }
            if(!end)continue;if(creator)return false;creator=f;creator_site=c+6;
            target_frame=frame;mode_frame=enum_frame;mode_pool=pool;
        }
    }
    if(!creator||!p.width_selector||!tween_factory)return false;
    std::optional<Function> surface;size_t surface_site=0;int32_t surface_frame=0;
    constexpr uint32_t window_target[]={0x94000000,0x4ea01c01,0x14000002,0x1e669001,
        0xfc5983a0,0xf85f83a1,0xfc1903a1,0x94000000,0x4ea01c01,0xfc5903a0,
        0x1e611802,0xfc5983a1,0x1e610843,0xf85f83a1,0xfc1883a3,
        0x94000000,0x4ea01c01,0xfc5903a0,0x1e611802,0xfc1803a2};
    for(const auto& f:code.functions()){const auto& b=f.words;uint32_t at=0,source=0,width1=0,width2=0;
        if(!frame_size(b)||!pattern(b,window_target,std::size(window_target),at))continue;const size_t i=at/4;
        if(!target(b[i],f.va+at,source)||source!=stack
            ||!target(b[i+7],f.va+at+28,width1)||width1!=p.width_selector
            ||!target(b[i+15],f.va+at+60,width2)||width2!=width1
            ||imm9(b[i+4])!=imm9(b[i+11])||imm9(b[i+5])!=imm9(b[i+13])
            ||imm9(b[i+6])!=imm9(b[i+9])||imm9(b[i+6])!=imm9(b[i+17]))continue;
        // The two outputs are physical and logical corner targets. Each must
        // become the end of four fresh scalar tweens from the SAME allocator
        // as the linked Flutter card, never the screen-radius begin values.
        const int32_t ends[]={imm9(b[i+14]),imm9(b[i+19])};unsigned count[2]{};
        if(ends[0]==ends[1]||!frame_slot(b,ends[0])||!frame_slot(b,ends[1]))continue;
        for(size_t j=i+20;j+1<b.size();++j){
            if(!ldd(b[j],29,0)||!std_(b[j+1],rn(b[j+1]),0)||j<5)continue;
            const auto object=rn(b[j+1]);int kind=imm9(b[j])==ends[0]?0:imm9(b[j])==ends[1]?1:-1;
            if(kind<0||!std_(b[j-1],object,0)||imm9(b[j+1])!=imm9(b[j-1])+8)continue;
            bool fresh=false;
            for(size_t k=j-5;k<j-1;++k){uint32_t factory=0;
                if(target(b[k],f.va+uint32_t(k*4),factory)&&factory==tween_factory
                    &&(object==0||(k+1<j&&b[k+1]==(0xaa0003e0u|object))))fresh=true;
            }
            if(fresh)++count[kind];
        }
        if(count[0]!=4||count[1]!=4)continue;
        if(surface)return false;surface=f;surface_site=i+8;surface_frame=imm9(b[i+6]);
    }
    if(!surface)return false;Body helper;code.body(stack,helper);Plan next=p;
    if(!add(next,*update,update_site,Action::Scalar,RecentConfig::Corners,3)
        ||!add(next,*creator,creator_site,Action::CornerFrame,RecentConfig::Corners,0,target_frame,mode_pool))return false;
    next.sites.back().selector_frame=mode_frame;
    if(!add(next,Function{stack,helper},0,Action::ScalarReturn,RecentConfig::Corners)
        ||!add(next,*surface,surface_site,Action::CornerSurface,RecentConfig::Corners,0,surface_frame))return false;
    p=std::move(next);return true;
}
inline bool header_graph(const auto& code,Plan& p) {
    std::optional<uint32_t> text_allocator;std::optional<Function> content;
    for(const auto& f:code.functions()){const auto& b=f.words;uint32_t at=0;
        constexpr uint32_t font[]={0x1e659000,0x14000002,0x1e65d000,0xf85d03a0,0xf85e03a1,0xfc1b83a0,
            0x94000000,0xaa0003e1,0x910082c0,0xf81d83a1,0xb8007020,0x91402f60,0xf9441400,0xb800b020};
        if(!frame_size(b)||!pattern(b,font,std::size(font),at))continue;
        const size_t a=at/4;uint32_t factory=0,tag_value=0,top=0,end=0;
        if(!target(b[a+6],f.va+uint32_t((a+6)*4),factory)||!allocator(code,factory,tag_value,top,end))continue;
        constexpr uint32_t box[]={0xfc5b83a0,0xa9460b40,0x91004000,0xeb00005f,0x54000009,
            0xf9003340,0xd1003c00,0xd29c2b82,0xf2a00062,0xf81ff002,0xfc007000,0xb801f020};
        uint32_t boxed=0;if(!pattern(b,box,std::size(box),boxed))continue;
        const size_t j=boxed/4;
        if(!frame_slot(b,imm9(b[a+9]))||imm9(b[j])!=imm9(b[a+5]))continue;
        const uint32_t double_tag=zimm(b[j+7])|(zimm(b[j+8])<<16);
        if(((double_tag>>8)&15)*16!=16)return false;
        // The actual Text receives this same style local, preserving its
        // original constructor and font-size boxing/GC return PCs.
        uint32_t text=0;size_t site=j+17;
        if(site+4>=b.size()||!target(b[j+15],f.va+uint32_t((j+15)*4),text)
            ||b[j+16]!=0xaa0003e1u||!ldx(b[site],29,0)||!stx(b[site+1],29,1)
            ||!mem(b[site+2],0xb8000000u,1,0)||!ldx(b[site+3],29,0)
            ||imm9(b[site+3])!=imm9(b[a+9])||!mem(b[site+4],0xb8000000u,1,0))continue;
        if(text_allocator)return false;text_allocator=text;
        p.style_tag=tag_value;p.style_size=((tag_value>>8)&15)*16;p.style_top=top;p.style_end=end;
        p.double_tag=double_tag;p.double_field=imm9(b[j+10]);p.style_font=imm9(b[j+11]);p.style_color=imm9(b[a+13]);
        if(!add(p,f,site+3,Action::FrameStyle,RecentConfig::TextSize,0,imm9(b[site+3])))return false;
        p.text_style=imm9(b[site+4]);p.text_data=imm9(b[site+2]);p.sites.back().text_object=1;
    }
    if(!text_allocator)return false;
    // Simple task titles use a shared const TextStyle; clone that style at its
    // real Text store. The Text allocator and fields must agree with the fresh
    // header above, so another similarly shaped label is not sufficient.
    bool simple=false;
    for(const auto& f:code.functions()){const auto& b=f.words;
        if(b.size()<23||!frame_size(b)||!ldw(b[3],1,0)||b[4]!=0x8b1c8000u
            ||b[5]!=0x6b16001fu||b[6]!=0x54000041u||!ux(b[7],27,0)
            ||!stx(b[8],29,0)||!ldx(b[10],29,1)||imm9(b[8])!=imm9(b[10])
            ||!mem(b[11],0xb8000000u,0,1)||imm9(b[11])!=p.text_data||!pooladd(b[12],1)||!ux(b[13],1,1)
            ||!mem(b[14],0xb8000000u,0,1)||imm9(b[14])!=p.text_style
            ||!pooladd(b[15],1)||!ux(b[16],1,1)||!mem(b[17],0xb8000000u,0,1)
            ||b[18]!=0xd2800041u||!mem(b[19],0xb8000000u,0,1)||!epi(b,20))continue;
        uint32_t text=0;if(!target(b[9],f.va+36,text)||text!=*text_allocator)continue;
        if(simple||!add(p,f,14,Action::Style,RecentConfig::TextSize,1))return false;simple=true;
        p.title_pool=pooloff(b[12],b[13]);p.text_allocator=text;
        p.empty_string_pool=uoff(b[7]);p.sites.back().text_object=0;
    }
    return simple;
}
inline bool offset_graph(const auto& code,Plan& p) {
    bool metrics=false,content=false;
    for(const auto& f:code.functions()){const auto& b=f.words;if(!frame_size(b))continue;
        constexpr uint32_t stores[]={0xf85f83a1,0xb8007001,0xfc5d83a0,0xfc00b000,
            0xfc5e03a0,0xfc013000,0xfc5e83a0,0xfc01b000,0xfc5f03a0,0xfc023000,
            0xaa1d03ef,0xa8c179fd,0xd65f03c0};
        uint32_t at=0;
        if(pattern(b,stores,std::size(stores),at)){
            constexpr uint32_t metric_input[]={0xf85f83a0,0x37200060,0x1e629000,0x14000002,
                0x1e631000,0xfc1d83a0,0x37200080,0x91402771,0xfd475e21,
                0x14000002,0x1e67d001,0xfc1e03a1};
            uint32_t metric_at=0;if(!pattern(b,metric_input,std::size(metric_input),metric_at))continue;
            // Producer selects all four metrics from the same device Boolean;
            // the fourth is consumed as the signed LTR/RTL title inset.
            const size_t a=at/4;
            if(a<2||(b[a-1]&0xfc000000u)!=0x94000000u)continue;
            const int32_t frame=imm9(b[a+8]);if(!frame_slot(b,frame))continue;
            if(metrics||!add(p,f,a+8,Action::FrameScalar,RecentConfig::HeaderOffset,0,frame))return false;metrics=true;p.metrics_owner=f.va;
        }
        for(size_t a=0;a+7<b.size();++a){
            const unsigned scalar=rd(b[a]);
            if((b[a]&~31u)!=0x1e629000u||b[a+1]!=0x14000002u
                ||b[a+2]!=(0x1e651000u|scalar)||!ldx(b[a+3],29,2)
                ||!std_(b[a+4],29,scalar)||b[a+5]!=0xa9460740u
                ||b[a+6]!=0x91004000u||b[a+7]!=0xeb00003fu)continue;
            unsigned boxes=0;
            for(size_t j=a+8;j+1<b.size();++j)if(std_(b[j],0,scalar)&&mem(b[j+1],0xb8000000u,2,0))++boxes;
            if(boxes!=1)continue;
            if(content)return false;content=true;p.header_content=f.va;p.witnesses.insert(f.va);
        }
    }
    return metrics&&content;
}
// The outer header's actual EdgeInsets consumer is a closure, absent from
// private-symbol inventories. Its direct call to the proved content builder,
// fresh Insets allocation and ordered left/top/right/bottom stores establish
// the position semantics. Internal icon spacing is deliberately left original.
inline bool header_outer_padding(const auto& code,Plan& p) {
    unsigned owners=0;
    for(const auto& f:code.functions()){const auto& b=f.words;if(!frame_size(b))continue;
        bool linked=false;for(size_t i=0;i<b.size();++i){uint32_t to=0;
            if(target(b[i],f.va+uint32_t(i*4),to)&&to==p.header_content)linked=true;}
        if(!linked)continue;
        constexpr uint32_t insets[]={0xfc5d03a0,0xf81e03a0,0xfc007000,0xf800f01f,
            0x1e645000,0xfc017000,0x1e621000,0xfc01f000,0xf85f03a1};
        uint32_t at=0;if(!pattern(b,insets,std::size(insets),at))continue;
        const size_t i=at/4;if(i<3||!ldd(b[i-3],2,0)||!field(imm9(b[i-3]))
            ||!std_(b[i-2],29,0)||imm9(b[i-2])!=imm9(b[i]))continue;
        // Insets must be the newly allocated object, rooted in this frame.
        uint32_t allocator=0,tag_value=0,top=0,end=0;
        if(!target(b[i-1],f.va+uint32_t((i-1)*4),allocator)
            ||!recent::allocator(code,allocator,tag_value,top,end)
            ||top!=p.style_top||end!=p.style_end||((tag_value>>8)&15)*16<40)continue;
        if(++owners>2||!add(p,f,i,Action::FrameScalar,RecentConfig::HeaderOffset,0,imm9(b[i]))
            ||!add(p,f,i+5,Action::Scalar,RecentConfig::HeaderOffset,0))return false;

    }
    return owners!=0;
}
// AOT inlines the simple title in the live icon-layout builder. The standalone
// _HeaderTitle witness is not necessarily called. Admit only direct children
// of the proved header-padding producer, using its actual Text allocator,
// style field and constant style identity, never the pool value alone.
inline bool inline_headers(const auto& code,Plan& p) {
    Body parent;if(!code.body(p.header_content,parent))return false;
    std::set<uint32_t> children;
    for(size_t i=0;i<parent.size();++i){uint32_t to=0;
        if(target(parent[i],p.header_content+uint32_t(i*4),to))children.insert(to);}
    bool owner=false;
    for(const auto& f:code.functions())if(children.count(f.va)){
        const auto& b=f.words;std::vector<std::pair<size_t,unsigned>> stores;
        for(size_t i=5;i+7<b.size();++i){
            const auto reg=rd(b[i]);const auto object=rn(b[i+2]);
            if(!pooladd(b[i],reg)||!ux(b[i+1],reg,reg)||pooloff(b[i],b[i+1])!=p.title_pool
                ||!mem(b[i+2],0xb8000000u,object,reg)||imm9(b[i+2])!=p.text_style
                ||!mem(b[i-1],0xb8000000u,object,rd(b[i-1]))||imm9(b[i-1])!=p.text_data)continue;
            bool fresh=false;
            for(size_t j=i-5;j<i;++j){uint32_t to=0;
                if(target(b[j],f.va+uint32_t(j*4),to)&&to==p.text_allocator
                    &&j+1<i&&b[j+1]==(0xaa0003e0u|object))fresh=true;}
            if(fresh)stores.emplace_back(i+2,reg);
        }
        if(stores.empty())continue;
        if(owner||stores.size()!=3)return false;owner=true;p.inline_header=f.va;
        for(auto [site,reg]:stores){
            if(!add(p,f,site,Action::Style,RecentConfig::TextSize,reg))return false;
            p.sites.back().text_object=rn(b[site]);
        }
    }
    return owner;
}
// Independent optional header contracts. Their absence never disables numeric
// recents controls on older launchers. All pool slots/fields come from THIS
// image; no name, RVA, class ID or pool index serves as a feature identity.
inline bool header_text_contract(const auto& code,Plan& p) {
    uint32_t one_cid=0,two_cid=0;int32_t length=0;unsigned found=0;
    for(const auto& f:code.functions())for(size_t i=0;i+29<=f.words.size();++i){
        if(f.words[i]!=0xf94001e2u||f.words[i+1]!=0x93407c42u)continue;
        Body b(f.words.begin()+i,f.words.end());uint32_t tag_value=0;int32_t top=0,end=0,len=0;bool one=false;
        if(!features::string_allocator(b,tag_value,top,end,len,one))continue;
        if(uint32_t(top)!=p.style_top||uint32_t(end)!=p.style_end||(found&&length!=len))return false;
        auto& cid=one?one_cid:two_cid;if(cid)return false;cid=tag_value>>12;length=len;++found;
    }
    uint32_t text_tag=0,top=0,end=0;
    if(found!=2||one_cid==two_cid||!p.empty_string_pool
        ||!allocator(code,p.text_allocator,text_tag,top,end)||!field(p.text_data)
        ||p.text_data==p.text_style||uint32_t(p.text_data+5)>((text_tag>>8)&15)*16)return false;
    p.text_tag=text_tag;p.string_length=length;p.string_one_cid=one_cid;p.string_two_cid=two_cid;
    return true;
}
// The live header receives a LIST OF ALREADY BUILT ICON WIDGETS, not always
// RowHeaderIcon widgets. Its cached children bypass both standalone builders.
// Prove index-zero/index-one and dynamic-index reads of that SAME collection
// inside the title-proved owner. Replace only the last-use child register,
// preserving list identity, bounds checks, spacers, locks and title children.
inline bool header_live_icons(const Body& b,const Function& owner,Plan& p) {
    uint32_t backing=0,data=0;int32_t saved_array=0;unsigned first=0,second=0,loop=0;
    const auto decompress=[](uint32_t w,unsigned reg){return w==(0x8b1c8000u|(reg<<5)|reg);};
    const auto bounds=[](uint32_t w){return (w&0xff00001fu)==0x54000002u;}; // Original unsigned bounds/throw edge.
    for(size_t i=3;i+7<b.size();++i){
        if(b[i-3]!=0xd2800001u||b[i-2]!=0xeb00003fu||!bounds(b[i-1])
            ||!ldw(b[i],rn(b[i]),0)||!decompress(b[i+1],0))continue;
        size_t j=i+2;int32_t array_frame=0;
        if(stx(b[j],29,0)){array_frame=imm9(b[j]);++j;}
        const auto reg=rd(b[j]);
        if(!ldw(b[j],0,reg)||!decompress(b[j+1],reg)||!stx(b[j+2],29,reg)
            ||!frame_slot(b,imm9(b[j+2]))||!field(imm9(b[i]))||!field(imm9(b[j])))continue;
        if(first&&(backing!=uint32_t(imm9(b[i]))||data!=uint32_t(imm9(b[j]))))return false;
        backing=imm9(b[i]);data=imm9(b[j]);if(array_frame){if(saved_array)return false;saved_array=array_frame;}
        if(!add(p,owner,j+2,Action::IconChild,RecentConfig::HideIcon,reg))return false;++first;
    }
    if(first!=2||!frame_slot(b,saved_array))return false;
    for(size_t i=4;i+5<b.size();++i){const auto reg=rd(b[i]);
        if(b[i-4]!=0xd2800021u||b[i-3]!=0xeb00003fu||!bounds(b[i-2])
            ||!ldx(b[i-1],29,0)||imm9(b[i-1])!=saved_array
            ||!ldw(b[i],0,reg)||uint32_t(imm9(b[i]))!=data+4
            ||!decompress(b[i+1],reg)||!stx(b[i+2],29,reg)||!frame_slot(b,imm9(b[i+2])))continue;
        // The following allocation call prevents a four-word post-decode
        // window. Admit its original compressed register BEFORE ADD heapbase.
        if(!add(p,owner,i+1,Action::IconChild,RecentConfig::HideIcon,reg))return false;
        p.sites.back().compressed_icon=true;++second;
    }
    for(size_t i=6;i+7<b.size();++i){const auto list=rn(b[i]);
        if(!ldw(b[i-6],list,0)||b[i-5]!=0x93417c01u||b[i-4]!=0xaa0103e0u
            ||b[i-3]!=0xaa0203e1u||b[i-2]!=0xeb00003fu||!bounds(b[i-1])
            ||!ldw(b[i],list,0)||uint32_t(imm9(b[i]))!=backing||!decompress(b[i+1],0)
            ||b[i+2]!=0x8b020810u)continue; // Original array[index * compressed-reference width].
        const auto reg=rd(b[i+3]);
        if(!ldw(b[i+3],16,reg)||uint32_t(imm9(b[i+3]))!=data||!decompress(b[i+4],reg)
            ||!stx(b[i+5],29,reg)||!frame_slot(b,imm9(b[i+5])))continue;
        if(!add(p,owner,i+5,Action::IconChild,RecentConfig::HideIcon,reg))return false;++loop;
    }
    return second==1&&loop==1;
}
inline bool header_icon_contract(const auto& code,Plan& p) {
    Body header;if(!code.body(p.inline_header,header))return false;
    // The real icon-layout owner's no-tasks branch returns the stock empty
    // widget. Require the length load, immediate CBNZ and complete return.
    uint32_t empty=0;unsigned empty_hits=0;
    for(size_t i=1;i+4<header.size();++i){const auto reg=rd(header[i-1]);
        if(!ldw(header[i-1],rn(header[i-1]),reg)||!field(imm9(header[i-1]))
            ||header[i]!=(0x350000a0u|reg)||!ux(header[i+1],27,0)||!epi(header,i+2))continue;
        empty=uoff(header[i+1]);++empty_hits;
    }
    if(empty_hits!=1)return false;
    std::optional<Function> cached,simple;
    for(const auto& f:code.functions()){const auto& b=f.words;
        // Cached icon build, after the original keep-alive superclass call.
        // The nullable cache fallback must be that SAME stock empty widget.
        if(b.size()==18&&frame_size(b)&&b[3]==0xaa0103e3u&&b[4]==0xaa0203e0u
            &&stx(b[5],29,1)&&ldx(b[7],29,1)&&imm9(b[5])==imm9(b[7])
            &&ldw(b[8],1,2)&&field(imm9(b[8]))&&b[9]==0x8b1c8042u
            &&b[10]==0x6b16005fu&&b[11]==0x54000061u&&ux(b[12],27,0)
            &&uoff(b[12])==empty&&b[13]==0x14000002u&&b[14]==0xaa0203e0u&&epi(b,15)){
            uint32_t mixin=0;Body m;
            if(target(b[6],f.va+24,mixin)&&code.body(mixin,m)&&m.size()==12
                &&m[0]==0xa9bf79fdu&&m[1]==0xaa0f03fdu&&ldw(m[2],1,0)
                &&field(imm9(m[2]))&&imm9(m[2])!=imm9(b[8])&&m[3]==0x8b1c8000u
                &&m[4]==0x6b16001fu&&m[5]==0x54000041u&&(m[6]&0xfc000000u)==0x94000000u
                &&pooladd(m[7],0)&&ux(m[8],0,0)&&epi(m,9)){
                if(cached)return false;cached=f;
            }
        }
        // Stateless transition icon: a fresh image receives the same model,
        // equal width/height, manager and key, then badge and keyed-subtree
        // constructors wrap that exact child. Frame offsets are decoded.
        if(!frame_size(b)||b.size()<70)continue;
        bool image=false,wrapper=false;
        for(size_t i=1;i+13<b.size();++i){
            uint32_t factory=0,tag_value=0,top=0,end=0;
            if(b[i]==0xaa0003e1u&&ldx(b[i+1],29,0)&&mem(b[i+2],0xb8000000u,1,0)
                &&ldd(b[i+3],29,0)&&std_(b[i+4],1,0)&&std_(b[i+5],1,0)
                &&imm9(b[i+5])==imm9(b[i+4])+8&&ldx(b[i+6],29,2)&&mem(b[i+7],0xb8000000u,1,2)
                &&ldx(b[i+8],29,2)&&mem(b[i+9],0xb8000000u,1,2)
                &&ldw(b[i+10],0,2)&&b[i+11]==0x8b1c8042u&&ldx(b[i+12],2,3)
                &&target(b[i-1],f.va+uint32_t((i-1)*4),factory)
                &&allocator(code,factory,tag_value,top,end)&&top==p.style_top&&end==p.style_end){
                // Distinct model, manager, key fields within this allocator.
                const auto size=((tag_value>>8)&15)*16;
                const int32_t fields[]={imm9(b[i+2]),imm9(b[i+7]),imm9(b[i+9])};
                std::set<int32_t> unique(fields,fields+3);
                if(unique.size()==3&&*unique.begin()>=7&&uint32_t(*unique.rbegin()+5)<=size
                    &&uint32_t(imm9(b[i+5])+9)<=size)image=true;
            }
            if(b[i]==0xaa0003e1u&&ldx(b[i+1],29,0)&&stx(b[i+2],29,1)
                &&ldw(b[i+3],0,2)&&b[i+4]==0x8b1c8042u&&stx(b[i+5],29,2)
                &&(b[i-1]&0xfc000000u)==0x94000000u
                &&target(b[i+6],f.va+uint32_t((i+6)*4),factory)
                &&allocator(code,factory,tag_value,top,end)&&ldx(b[i+7],29,1)
                &&imm9(b[i+7])==imm9(b[i+5])&&mem(b[i+8],0xb8000000u,0,1)
                &&ldx(b[i+9],29,1)&&imm9(b[i+9])==imm9(b[i+2])
                &&mem(b[i+10],0xb8000000u,0,1)&&imm9(b[i+10])!=imm9(b[i+8])&&epi(b,i+11))wrapper=true;
        }
        if(image&&wrapper){if(simple)return false;simple=f;}
    }
    if(!cached||!simple)return false;
    // Commit both paths atomically. Entry-return routes do not allocate or
    // execute a managed call from the native frame. Disabled routes replay.
    Plan next=p;
    if(!add(next,*cached,7,Action::HideIconReturn,RecentConfig::HideIcon,0,1)
        ||!add(next,*simple,0,Action::HideIconReturn,RecentConfig::HideIcon))return false;
    if(!header_live_icons(header,Function{p.inline_header,header},next))return false;
    next.empty_widget_pool=empty;p=std::move(next);return true;
}
inline bool background_graph(const auto& code,Plan& p) {
    std::optional<Function> found;
    constexpr uint32_t end[]={0xb8407001,0x8b1c8021,0xb8413022,0x8b1c8042,0x91406370,
        0xf947e210,0x6b10005f,0x54000081,0x91419760,0xf944c800,0x14000003,
        0x9142ef60,0xf945b800,0xaa1d03ef,0xa8c179fd,0xd65f03c0};
    for(const auto& f:code.functions()){const auto& b=f.words;uint32_t at=0;
        if(b.size()<30||b[0]!=0xa9bf79fdu||b[1]!=0xaa0f03fdu||b[2]!=0x6b16003fu
            ||b[3]!=0x540000c0u||!pooladd(b[4],0)||!ux(b[5],0,0)||!epi(b,6)
            ||!pattern(b,end,std::size(end),at))continue;
        if(found)return false;found=f;
    }
    // The complete nullable/freeform/theme decision must have one owner;
    // similarly colored settings pages are not admitted by color constants.
    if(!found)return false;
    return add(p,*found,0,Action::Freeform,RecentConfig::FreeformColor);
}
// Current thumbnails select the background in their content builder rather
// than call the legacy getter. Prove the real windowing-mode predicate (mode 5,
// Android's freeform ABI), its Boolean diamond and the same rooted color passed
// to the clipped-container constructor. Ordinary full-screen cards stay original.
inline bool freeform_content(const auto& code,Plan& p) {
    bool found=false;
    for(const auto& f:code.functions()){const auto& b=f.words;uint32_t at=0;
        constexpr uint32_t color[]={0xf85f83a1,0xf81d83a0,0xb840b022,0x8b1c8042,0x6b16005f,
            0x54003220,0x37200080,0x91414f63,0xf943d863,0x14000003,0x91403763,0xf941bc63,
            0xf81e03a3,0xf9403f40};
        // The throw edge is allowed to move; all local diamond edges remain
        // checked by their actual branch destinations below.
        if(!frame_size(b))continue;
        unsigned candidates=0;size_t selected=0;
        for(size_t i=0;i+std::size(color)<=b.size();++i){bool match=true;
            for(size_t j=0;j<std::size(color);++j){
                const auto mask=j==5?0xff00001fu:(pooladd(color[j],rd(color[j]))?0xffc003ffu:operand_mask(color[j]));
                if((b[i+j]&mask)!=(color[j]&mask)){match=false;break;}}
            if(match){++candidates;selected=i;}}
        if(!candidates)continue;if(candidates!=1||selected==0)return false;const size_t i=selected;
        if(b[i+6]!=0x37200080u||b[i+9]!=0x14000003u)continue;
        uint32_t predicate=0;if(!target(b[i-1],f.va+uint32_t((i-1)*4),predicate))continue;
        Body pred,boxing;
        // The inventory splits at this original boxing slow-path prologue.
        // Rejoin its adjacent tail before validating either branch's result.
        if(!code.body(predicate,pred)||pred.size()!=9
            ||!code.body(predicate+uint32_t(pred.size()*4),boxing)||boxing.size()<21)continue;
        pred.insert(pred.end(),boxing.begin(),boxing.begin()+21);
        constexpr uint32_t check[]={0xb8443061,0x8b1c8021,0xb8407022,0x8b1c8042,0xf8417041,
            0xf100143f,0x910082d0,0x9100c2d1,0x9a910202,0xaa0203e0,0xd65f03c0};
        if(!pattern(pred,check,std::size(check),at)||pred[15]!=0x7100281fu
            ||pred[17]!=0x910082c0u||pred[18]!=0x1400000bu)continue;
        const int32_t color_frame=imm9(b[i+12]);bool consumer=false;
        for(size_t j=i+14;j+4<b.size();++j){uint32_t to=0;Body child;
            if(!ldx(b[j],29,0)||!ldx(b[j+1],29,1)||!ldx(b[j+2],29,2)
                ||!ldx(b[j+3],29,3)||imm9(b[j+3])!=color_frame
                ||!target(b[j+4],f.va+uint32_t((j+4)*4),to)||!code.body(to,child))continue;
            constexpr uint32_t rooted[]={0xf81e83a3,0xd2800061,0x94000000,0xaa0003e1,
                0xf85f83a0,0xf81e03a1,0xb800f020,0xf85f03a0,0xb8013020,0xf85e83a0,0xb8017020};
            if(pattern(child,rooted,std::size(rooted),at))consumer=true;
        }
        if(!consumer)continue;
        if(found||!add(p,f,i+12,Action::FreeformColor,RecentConfig::FreeformColor,3))return false;
        found=true;p.witnesses.insert(predicate);
    }
    return found;
}
// Inlined header-height constants are admitted only inside owners linked to
// the real card-size producers above. Equal numeric constants in privacy or
// unrelated widgets do not establish a header-height relationship.
inline bool height_consumers(const auto& code,Plan& p) {
    auto owners=p.card_roots;
    for(unsigned depth=0;depth<3;++depth){auto previous=owners;
        for(const auto& f:code.functions())for(size_t i=0;i<f.words.size();++i){uint32_t to=0;
            if(target(f.words[i],f.va+uint32_t(i*4),to)&&previous.count(to)){owners.insert(f.va);break;}}
    }
    p.witnesses.insert(owners.begin(),owners.end());
    for(const auto& f:code.functions())if(owners.count(f.va)){
        const auto& b=f.words;
        for(size_t i=0;i+5<b.size();++i){const unsigned base=rd(b[i]),scalar=rd(b[i+1]);
            if(!pooladd(b[i],base)||!dp(b[i+1],base,scalar))continue;
            const auto pool=pooloff(b[i],b[i+1]);
            if(pool!=p.header_pools[0]&&pool!=p.header_pools[1])continue;
            if(!straight(b,i+2))continue;
            // This is the input to arithmetic or its immediate rooted frame
            // store, rather than an arbitrary same-valued scalar return.
            const auto w=b[i+2];
            if(!std_(w,29,scalar)&&((w&0xff200c00u)!=0x1e200800u))continue;
            if(!add(p,f,i+2,Action::Scalar,RecentConfig::HeaderHeight,scalar))return false;
        }
    }
    return true;
}
inline std::optional<Plan> resolve(const auto& code,const char** failure=nullptr) {
    Plan p;if(failure)*failure="object layout bounds or overlapping patch windows";
    const auto failed=[&](const char* why){if(failure)*failure=why;return std::optional<Plan>{};};
    if(!scale_graph(code,p))return failed("card-width dispatcher, consumer roles or shared ratio graph missing/ambiguous");
    if(!color_graph(code,p))return failed("Color allocator/channel producer/ARGB consumer graph missing/ambiguous");
    if(!height_graph(code,p))return failed("header-height typed publication missing/ambiguous");
    if(!pad_graph(code,p))return failed("tablet dimensions and card-bounds consumer missing/ambiguous");
    if(!radius_graph(code,p))return failed("card/thumbnail/gesture radius graph missing/ambiguous");
    if(!header_graph(code,p))return failed("fresh/constant task-title TextStyle graph missing/ambiguous");
    if(!offset_graph(code,p))return failed("header metrics and padding publication missing/ambiguous");
    if(!header_outer_padding(code,p))return failed("header outer Insets constructor/consumer missing/ambiguous");
    if(!inline_headers(code,p))return failed("inlined task-title allocator/style consumers missing/ambiguous");
    if(!background_graph(code,p))return failed("nullable freeform/theme background getter missing/ambiguous");
    if(!freeform_content(code,p))return failed("freeform mode predicate/background/clipped-container chain missing/ambiguous");
    if(!height_consumers(code,p))return failed("inlined header-height consumer cannot bind a safe window");
    p.corner_animation_ready=corner_animation_graph(code,p);
    p.hide_text_ready=header_text_contract(code,p);
    p.hide_icon_ready=header_icon_contract(code,p);
    if(p.sites.empty()||p.sites.size()>64||p.style_size<40||p.style_size>256
        ||p.color.top!=p.style_top||p.color.end!=p.style_end
        ||!field(p.style_font)||!field(p.style_color)||p.style_font==p.style_color
        ||uint32_t(p.style_font+5)>p.style_size||uint32_t(p.style_color+5)>p.style_size
        ||p.double_field<0||uint32_t(p.double_field+9)>16
        ||!field(p.color_space)||uint32_t(p.color_space+5)>p.color.size)return {};
    for(auto channel:p.color.channels)if(p.color_space>=channel&&p.color_space<channel+8)return {};
    std::sort(p.sites.begin(),p.sites.end(),[](const auto&a,const auto&b){return a.va<b.va;});
    for(size_t i=1;i<p.sites.size();++i)if(p.sites[i].va<p.sites[i-1].va+16)return {};
    if(failure)*failure=nullptr;return p;
}

}
using RecentPlan=recent::Plan;
}
