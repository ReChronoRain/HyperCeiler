// SPDX-License-Identifier: AGPL-3.0-or-later
#include "targets/home/home_recent_statusbar_resolver.h"
#include <cassert>
#include <fstream>
#include <iostream>
using Bytes=std::vector<std::byte>;
void put(Bytes& b,size_t at,uint64_t v,unsigned n=4){for(unsigned i=0;i<n;++i)b.at(at+i)=std::byte(v>>(8*i));}
uint32_t adrp(uint64_t pc,uint64_t target,unsigned reg){int64_t d=(int64_t(target&~4095ULL)-int64_t(pc&~4095ULL))/4096;return 0x90000000|((d&3)<<29)|(((d>>2)&0x7ffff)<<5)|reg;}
constexpr const char* labels[]={"set_is_in_portrait_overview: value=","set_is_in_landscape_overview: value=","set_recents_window_showing: "};
Bytes fixture(uint64_t ro,uint64_t code,uint64_t data){
    Bytes b(0x2000);b[0]=std::byte{0x7f};b[1]=std::byte{'E'};b[2]=std::byte{'L'};b[3]=std::byte{'F'};b[4]=std::byte{2};b[5]=std::byte{1};b[6]=std::byte{1};
    put(b,16,3,2);put(b,18,183,2);put(b,20,1);put(b,32,64,8);put(b,52,64,2);put(b,54,56,2);put(b,56,4,2);
    auto ph=[&](int i,uint32_t type,uint32_t flags,uint64_t off,uint64_t va,uint64_t files,uint64_t mem){size_t a=64+i*56;put(b,a,type);put(b,a+4,flags);put(b,a+8,off,8);put(b,a+16,va,8);put(b,a+24,va,8);put(b,a+32,files,8);put(b,a+40,mem,8);put(b,a+48,4096,8);};
    ph(0,1,4,0,ro,0x1000,0x1000);ph(1,1,5,0x1000,code,0x1000,0x1000);ph(2,1,6,0x2000,data,0,0x1000);ph(3,0x6474e550,4,0x700,ro+0x700,44,44);
    b[0x700]=std::byte{1};b[0x701]=std::byte{0x1b};b[0x702]=std::byte{3};b[0x703]=std::byte{0x3b};put(b,0x708,4);
    for(unsigned k=0;k<4;++k)put(b,0x70c+k*8,code+k*0x80-(ro+0x700));
    for(unsigned k=0;k<3;++k){
        const size_t text=0x400+k*0x80,off=0x1000+k*0x80;const auto va=code+k*0x80,flag=data+0x100+k*8;
        std::memcpy(b.data()+text,labels[k],strlen(labels[k])+1);
        for(unsigned i=0;i<32;++i)put(b,off+i*4,0xd503201f);
        put(b,off,0xd10103ff);put(b,off+4,0x2a0003f3); // SP frame, preserve bool W0 in W19
        put(b,off+8,adrp(va+8,ro+text,8));put(b,off+12,0x91000108|(((ro+text)&4095)<<10));
        put(b,off+16,adrp(va+16,flag,9));
        if(k<2) put(b,off+20,0x39000133|((flag&4095)<<10));
        else {put(b,off+20,0x91000129|((flag&4095)<<10));put(b,off+24,0x085f7d2a);put(b,off+28,0x080b7d33);put(b,off+32,0x35ffffcb);}
        put(b,off+124,0xd65f03c0);
    }return b;
}
int main(int argc,char**argv){
    unsigned n=0;auto check=[&](bool b){++n;if(!b){std::cerr<<"failed check "<<n<<"\n";std::abort();}};
    for(unsigned seed=0;seed<12;++seed){
        auto ro=0x100000ULL+seed*0x3000,code=0x300000ULL+seed*0x7000,data=0xa00000ULL+seed*0xb000;
        auto b=fixture(ro,code,data);auto t=home_recent_bar::resolve(b);check(bool(t));
        for(unsigned k=0;k<3;++k)check(t->values[k].entry==code+k*0x80&&t->values[k].flag==data+0x100+k*8);
        auto reject=[&](auto mutate){auto bad=b;mutate(bad);check(!home_recent_bar::resolve(bad));};
        for(unsigned k=0;k<3;++k){
            reject([&](auto& x){x[0x400+k*0x80]=std::byte{'X'};});
            reject([&](auto& x){std::memcpy(x.data()+0x800,labels[k],strlen(labels[k]));});
            reject([&](auto& x){put(x,0x1000+k*0x80+4,0xd503201f);});
            reject([&](auto& x){put(x,0x1000+k*0x80,0xd65f03c0);});
            reject([&](auto& x){put(x,0x1000+k*0x80+20,0xd503201f);});
            reject([&](auto& x){put(x,0x1000+k*0x80+16,0xd65f03c0);});
        }
        reject([&](auto& x){put(x,64+2*56+4,4);}); // flag must be writable native data
        reject([&](auto& x){put(x,64+2*56+4,7);}); // never executable data
        reject([&](auto& x){x[0x703]=std::byte{0xff};});
    }
    for(int i=1;i<argc;++i){std::ifstream f(argv[i],std::ios::binary);check(bool(f));std::vector<char> raw((std::istreambuf_iterator<char>(f)),{});auto t=home_recent_bar::resolve(std::as_bytes(std::span(raw)));check(bool(t));std::cout<<"real image verified "<<argv[i]<<"\n";}
    std::cout<<"recents bar resolver "<<n<<" checks passed\n";
}
