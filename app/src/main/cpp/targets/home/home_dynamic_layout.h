/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
#include <vector>
#include <array>
namespace home_dynamic {
using Body = std::vector<uint32_t>;
inline int32_t imm9(uint32_t w) { return static_cast<int32_t>(w << 11) >> 23; }
inline unsigned rn(uint32_t w) { return (w >> 5) & 31; }
inline unsigned rd(uint32_t w) { return w & 31; }
inline bool mem(uint32_t w, uint32_t op, unsigned base, unsigned reg) {
    return (w & 0xffe00fffu) == (op | (base << 5) | reg);
}
inline bool ldx(uint32_t w,unsigned base,unsigned reg) {return mem(w,0xf8400000,base,reg);}
inline bool ldw(uint32_t w,unsigned base,unsigned reg) {return mem(w,0xb8400000,base,reg);}
inline bool ldd(uint32_t w,unsigned base,unsigned reg) {return mem(w,0xfc400000,base,reg);}
inline bool stx(uint32_t w,unsigned base,unsigned reg) {return mem(w,0xf8000000,base,reg);}
inline bool std_(uint32_t w,unsigned base,unsigned reg) {return mem(w,0xfc000000,base,reg);}
inline bool ux(uint32_t w,unsigned base,unsigned reg) {return (w&0xffc003ffu)==(0xf9400000u|(base<<5)|reg);}
inline uint32_t uoff(uint32_t w) {return ((w>>10)&4095)*8;}
inline bool movz(uint32_t w,unsigned reg) {return (w&0xffe0001fu)==(0xd2800000u|reg);}
inline uint32_t zimm(uint32_t w) {return (w>>5)&65535;}
inline bool tag(const Body &b,uint32_t &value) {
    if(b.size()<2 || !movz(b[0],2) || (b[1]&0xffe0001fu)!=0xf2a00002u) return false;
    value=zimm(b[0])|(zimm(b[1])<<16);
    return (value>>12)!=0;
}
inline bool field(int32_t off){return off>0 && off<4096 && ((off+1)&3)==0;}
inline bool slot(int32_t off){return off<0 && off>=-4096 && !(off&7);}
// A frame's size and slots belong to the current AOT body, not to the VM ABI.
inline int32_t frame_size(const Body& b) {
    if(b.size()<4||b[0]!=0xa9bf79fdu||b[1]!=0xaa0f03fdu
        ||(b[2]&0xffc003ffu)!=0xd10001efu)return 0;
    const int32_t size=(b[2]>>10)&4095;
    return size>0&&!(size&7)?size:0;
}
inline bool frame_slot(const Body& b,int32_t off) {
    const auto size=frame_size(b);return size&&slot(off)&&off>=-size;
}
inline bool receiver_slot(const Body& b,unsigned reg,int32_t& out) {
    unsigned hits=0;int32_t found=0;
    if(!frame_size(b))return false;
    for(size_t i=3;i<b.size()&&i<20;++i) {
        if((b[i]&0xfc000000u)==0x94000000u||(b[i]&0xfffffc1fu)==0xd63f0000u)break;
        if(stx(b[i],29,reg)&&frame_slot(b,imm9(b[i]))){found=imm9(b[i]);++hits;}
    }
    if(hits!=1)return false;out=found;return true;
}
inline bool target(uint32_t w,uint32_t pc,uint32_t &out){
    if((w&0xfc000000u)!=0x94000000u)return false;
    int64_t v=static_cast<int32_t>(w<<6)>>6;v=pc+v*4;
    if(v<4 || v>UINT32_MAX || (v&3))return false;out=static_cast<uint32_t>(v);return true;
}
// Opcode/register ABI contracts are fixed. All image-specific operands are outputs;
// callers additionally verify producer/consumer and unique owning-function relationships.
inline uint32_t operand_mask(uint32_t w) {
    if((w&0x3b200c00u)==0x38000000u) return 0xffe00fffu; // LDUR/STUR signed field/frame
    if((w&0x3b000000u)==0x39000000u) return 0xffc003ffu; // LDR/STR unsigned field/pool
    if((w&0xfc000000u)==0x94000000u)return 0xfc000000u; // BL target separately validated
    if((w&0xff000010u)==0x54000000u)return 0xff00001fu;
    if((w&0x7e000000u)==0x36000000u)return 0xfff8001fu;
    return 0xffffffffu;
}
inline bool unique(const Body&b,const uint32_t*p,size_t n,uint32_t &at){
    if(!n || b.size()<n)return false;unsigned hits=0;uint32_t candidate=0;
    for(size_t i=0;i+n<=b.size();++i){bool same=true;
        for(size_t j=0;j<n;++j){auto mask=operand_mask(p[j]);if((b[i+j]&mask)!=(p[j]&mask)){same=false;break;}}
        if(same){++hits;candidate=static_cast<uint32_t>(i*4);}}
    if(hits!=1)return false;at=candidate;return true;
}
struct ColorLayout {uint32_t tag=0,size=0,top=0,end=0;std::array<int32_t,4> channels{};
    bool usable()const{return tag && size>=40 && size<=256 && top && end==top+8;}};
inline bool color_fields(const Body&b,uint32_t tag_value,ColorLayout &out){
    ColorLayout next;next.tag=tag_value;next.size=((tag_value>>8)&15)*16;
    size_t count=0;
    // withAlpha writes alpha first, followed by the three converted getter channels.
    // Every store must be a bounded unboxed double field of the same fresh receiver.
    for(auto word:b)if((word&0xffe00fe0u)==0xfc000000u){
        if(count==4)return false;int32_t off=imm9(word);
        if(off<=0 || ((off+1)&7) || static_cast<uint32_t>(off+9)>next.size)return false;
        for(size_t j=0;j<count;++j)if(next.channels[j]==off)return false;
        next.channels[count++]=off;
    }
    if(count!=4 || (tag_value>>12)==0)return false;out=next;return true;
}
struct ModelLayout {int32_t dispatch=0,flag=0,intent=0,component=0,package=0,component_package=0,pin_name=0;uint32_t flag_mask=0;
    bool usable()const{return dispatch>0 && field(flag) && field(intent) && field(component) && field(package) && field(component_package) && field(pin_name) && flag_mask;}};
inline bool model_fields(const Body&package,const Body&component,const Body&pin,ModelLayout &out){
    ModelLayout next;unsigned dispatch_hits=0,comp_hits=0,fallback_hits=0,pin_hits=0,package_hits=0;
    for(size_t i=0;i+2<package.size();++i){auto w=package[i];
        if((w&0xffc003ffu)==0xd100001eu && package[i+1]==0xf87e7abeu && package[i+2]==0xd63f03c0u){next.dispatch=(w>>10)&4095;++dispatch_hits;}
        if(ldw(w,0,1) && package[i+1]==0x8b1c8021u && package[i+2]==0x6b16003fu){next.component_package=imm9(w);++package_hits;}
        if(ldw(w,2,3) && package[i+1]==0x8b1c8063u && package[i+2]==0xaa0303e0u){next.package=imm9(w);++fallback_hits;}
    }
    for(size_t i=0;i+13<component.size();++i){
        if(!movz(component[i],17) || component[i+1]!=0xb8716822u || component[i+2]!=0x8b1c8042u
          || (component[i+3]&0x7e00001fu)!=0x36000002u || component[i+4]!=0xaa1603e0u || component[i+5]!=0xd65f03c0u
          || !ldw(component[i+6],1,2) || component[i+7]!=0x8b1c8042u || !ux(component[i+8],27,16)
          || component[i+9]!=0x6b10005fu || (component[i+10]&0xff00001fu)!=0x54000000u
          || !ldw(component[i+11],2,0) || component[i+12]!=0x8b1c8000u || component[i+13]!=0xd65f03c0u)continue;
        // The flag branch must reach the original Intent load, not a neighbouring arm.
        int32_t disp=static_cast<int32_t>(component[i+3]<<13)>>18;
        if(disp!=3)continue;
        const unsigned bit=((component[i+3]>>19)&31)|((component[i+3]>>26)&32);if(bit>=32)continue;
        next.flag=zimm(component[i]);next.flag_mask=1u<<bit;
        next.intent=imm9(component[i+6]);next.component=imm9(component[i+11]);++comp_hits;
    }
    for(size_t i=0;i+2<pin.size();++i)if(movz(pin[i],17) && pin[i+1]==0xb8716803u && pin[i+2]==0x8b1c8063u){next.pin_name=zimm(pin[i]);++pin_hits;}
    if(dispatch_hits!=1 || comp_hits!=1 || fallback_hits!=1 || pin_hits!=1 || package_hits!=1 || !next.usable())return false;
    out=next;return true;
}
struct GeometryFrame {
    int32_t grid=0,info=0,closure=0,x=0,y=0,width=0,height=0,out_x=0,out_y=0,out_width=0,out_height=0;
    bool usable(bool hotseat=false)const {
        if(!slot(grid) || !slot(x))return false;
        if(hotseat)return slot(closure);
        return slot(width)&&slot(height)&&slot(out_width)&&slot(out_height)&&slot(out_x)&&slot(out_y);
    }
};
inline bool workspace_frame(const Body&b,bool occupied,GeometryFrame &out,uint32_t &patch) {
    GeometryFrame f;uint32_t constraints=0;
    constexpr uint32_t bounds[]={0xfc5803a0,0xf81c83a0,0xfc007000,0xfc00f000,0xfc5883a1,0xfc017001,0xfc01f001,0xf85f83a3};
    constexpr uint32_t occupied_bounds[]={0xfc5803a0,0xf81c83a0,0xfc007000,0xfc00f000,0xfc5883a0,0xfc017000,0xfc01f000,0xf85f83a3};
    if(!unique(b,occupied?occupied_bounds:bounds,8,constraints))return false;
    size_t c=constraints/4;f.out_width=imm9(b[c]);f.out_height=imm9(b[c+4]);f.grid=imm9(b[c+7]);
    if(!occupied){
        constexpr uint32_t origin[]={0xf85f83a0,0xf81d83a2,0xb843b003,0x8b1c8063,0xf81e03a3,0xfc407060,0xfc1a83a0,0xf85f03a4,0xfc5b03a1,0xfc5b83a2,0xfc5c03a3};
        uint32_t at=0;if(!unique(b,origin,11,at))return false;size_t a=at/4;
        f.x=imm9(b[a+6]);f.y=imm9(b[a+10]);f.width=imm9(b[a+8]);f.height=imm9(b[a+9]);
        f.grid=imm9(b[a]);
        constexpr uint32_t grid[]={0xb841b003,0x8b1c8063,0xf81f83a3,0xfc42b060,0xfc1b03a0,0xfc433061,0xfc1b83a1};
        uint32_t setup=0;if(!unique(b,grid,7,setup) || imm9(b[setup/4+2])!=f.grid
            || imm9(b[setup/4+4])!=f.width || imm9(b[setup/4+6])!=f.height
            || f.width!=f.out_width || f.height!=f.out_height)return false;
        f.out_x=f.x;f.out_y=f.y;patch=at+28;
    } else {
        constexpr uint32_t xy[]={0xfc5a03a0,0xfc5a83a1,0xfc5b03a2,0xf85f03a0,0xf85e83a1,0xf8437002,0x9e620043,0x1e600864,0x1e642843,0xfc1903a3,0xf843f002,0x9e620044,0x1e610885,0xfc1983a5};
        uint32_t at=0;if(!unique(b,xy,14,at) || at/4+18>b.size())return false;size_t a=at/4;
        f.width=imm9(b[a]);f.height=imm9(b[a+1]);f.x=imm9(b[a+2]);f.info=imm9(b[a+3]);
        f.out_x=imm9(b[a+9]);f.out_y=imm9(b[a+13]);
        constexpr uint32_t span[]={0xf85f03a0,0xf841f001,0x9e620020,0xfc5a03a1,0x1e600822,0xfc1803a2,0xf8427001,0x9e620020,0xfc5a83a3,0x1e600864,0xfc1883a4};
        uint32_t mul=0;if(!unique(b,span,11,mul) || imm9(b[mul/4])!=f.info
            || imm9(b[mul/4+3])!=f.width || imm9(b[mul/4+8])!=f.height
            || imm9(b[mul/4+5])!=f.out_width || imm9(b[mul/4+10])!=f.out_height)return false;
        f.out_width=f.width;f.out_height=f.height;patch=at+56;
    }
    if(!f.usable())return false;out=f;return true;
}
inline bool hotseat_frame(const Body&b,GeometryFrame &out,uint32_t &patch) {
    if(b.size()<4)return false;
    GeometryFrame f;unsigned roots=0;
    for(size_t i=2;i<b.size();++i){
        if((b[i]&0xfc000000u)==0x94000000u)break;
        if(stx(b[i],29,1)){++roots;f.grid=imm9(b[i]);}
    }
    if(roots!=1)return false;
    constexpr uint32_t bounds[]={0xfc5803a0,0xf81983a0,0xfc007000,0xfc00f000,0xfc5903a0,0xfc017000,0xfc01f000};
    constexpr uint32_t column[]={0xf85b03a2,0xb840f040,0x8b1c8000,0xb8407001,0x8b1c8021,0xf8437024,0x937f7880,0xeb80049f};
    uint32_t at=0,chain=0;
    if(!unique(b,bounds,7,at) || !unique(b,column,8,chain) || at<4 || (b[at/4-1]&0xfc000000u)!=0x94000000u)return false;
    f.x=imm9(b[at/4]);f.closure=imm9(b[chain/4]);
    unsigned producers=0;
    for(size_t i=0;i+4<b.size();++i)if(ldw(b[i],3,0) && b[i+1]==0x8b1c8000u && ldd(b[i+2],0,3)
        && std_(b[i+3],29,3) && imm9(b[i+3])==f.x && (b[i+4]&0xfc000000u)==0x94000000u)++producers;
    if(producers!=1 || !f.usable(true))return false;out=f;patch=at;return true;
}

}
