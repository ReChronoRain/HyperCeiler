/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include <array>
#include <atomic>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <iostream>
#include <limits>
#include <new>
#include <span>
#include <stdexcept>
#include <string>
#include <vector>
#include "targets/weather/weather_material_binding.h"
#include "fixture.generated.h"
static bool forbid_alloc=false;
void* operator new(std::size_t n){if(forbid_alloc)throw std::bad_alloc();if(void*p=std::malloc(n?n:1))return p;throw std::bad_alloc();}
void operator delete(void*p)noexcept{std::free(p);}void operator delete(void*p,std::size_t)noexcept{std::free(p);}
void* operator new[](std::size_t n){return ::operator new(n);}void operator delete[](void*p)noexcept{::operator delete(p);}void operator delete[](void*p,std::size_t)noexcept{::operator delete(p);}
using weather_material_binding::Binding;
static int passed=0,failed=0;static uint64_t bit_rejections=0;
void require(bool b,const char*why="assertion"){if(!b)throw std::runtime_error(why);}
void run(const std::string&name,auto test){try{test();passed++;std::cout<<"PASS "<<name<<'\n';}catch(const std::exception&e){failed++;std::cout<<"FAIL "<<name<<" "<<e.what()<<'\n';}}
struct Segment {uint64_t va,memsz;std::vector<std::byte> data;};
struct Image {uint64_t base;std::vector<Segment>segments;};
struct Reader {
 std::array<Image,4> images;
 uintptr_t mutation=0,unmapped=0,change_slot=0;uint8_t xor_mask=0;int slot_reads=0,change_after=0;
 size_t calls=0,max_read=0;
 explicit Reader(const char*const*paths){for(size_t i=0;i<4;i++){images[i].base=fixture::bases[i];std::ifstream f(paths[i],std::ios::binary);require(bool(f),"ELF missing");std::vector<char>file((std::istreambuf_iterator<char>(f)),{});for(const auto&s:fixture::loads[i]){Segment segment{s.va,s.memsz,std::vector<std::byte>(s.memsz)};require(s.offset+s.filesz<=file.size());std::memcpy(segment.data.data(),file.data()+s.offset,s.filesz);images[i].segments.push_back(std::move(segment));}}reset_imports();}
 bool raw(uintptr_t address,std::span<std::byte>out){for(auto&image:images){if(address<image.base)continue;uintptr_t va=address-image.base;for(auto&s:image.segments){if(va<s.va||va-s.va>s.memsz||out.size()>s.memsz-(va-s.va))continue;std::memcpy(out.data(),s.data.data()+va-s.va,out.size());return true;}}return false;}
 bool operator()(uintptr_t address,std::span<std::byte>out){calls++;max_read=std::max(max_read,out.size());if(unmapped>=address&&unmapped-address<out.size())return false;if(!raw(address,out))return false;if(change_slot==address&&out.size()==8&&++slot_reads>=change_after)out[0]^=std::byte{4};if(mutation>=address&&mutation-address<out.size())out[mutation-address]^=std::byte{xor_mask};return true;}
 void pointer(uintptr_t base,uintptr_t slot,uintptr_t value){for(auto&image:images)if(image.base==base)for(auto&s:image.segments){if(slot<s.va||slot+8>s.va+s.memsz)continue;std::memcpy(s.data.data()+slot-s.va,&value,8);return;}throw std::runtime_error("fixture bad pointer slot");}
 void reset_imports(){mutation=unmapped=change_slot=0;xor_mask=0;slot_reads=change_after=0;for(auto&s:fixture::weather_imports)pointer(images[0].base,s.slot,images[1].base+s.target);for(auto&s:fixture::private_imports)pointer(images[1].base,s.slot,images[2].base+s.target);for(auto&s:fixture::vtables)pointer(images[0].base,s.slot,images[0].base+s.target);}
};
namespace wb=weather_material_binding;
namespace wg=weather_material_guard;
Binding sentinel(){return {11,22,33,44,reinterpret_cast<weather_material_support::GetBool>(uintptr_t(0x1110))};}
bool same(const Binding&a,const Binding&b){return a.weather_base==b.weather_base&&a.public_base==b.public_base&&a.private_base==b.private_base&&a.slot==b.slot&&a.original==b.original;}
bool resolve(Reader&r,Binding&out,uintptr_t base=fixture::bases[0]){forbid_alloc=true;bool ok;try{ok=wb::resolve(base,r,out);}catch(...){forbid_alloc=false;throw;}forbid_alloc=false;return ok;}
void reject(Reader&r,uintptr_t base=fixture::bases[0]){Binding out=sentinel();require(!resolve(r,out,base),"invalid binding accepted");require(same(out,sentinel()),"failed resolve changed output");}
bool dart(Reader&r,uintptr_t base=fixture::bases[3]){forbid_alloc=true;bool ok;try{ok=wb::verify_dart(base,r);}catch(...){forbid_alloc=false;throw;}forbid_alloc=false;return ok;}
int main(int argc,char**argv){require(argc==5,"Weather/public/core-private/Dart ELF paths required");Reader r(const_cast<const char*const*>(argv+1));
 run("actual ELF bytes and exact resolved import chain bind without heap",[&]{Binding out;require(resolve(r,out));require(out.weather_base==fixture::bases[0]&&out.public_base==fixture::bases[1]&&out.private_base==fixture::bases[2]);require(out.slot==fixture::bases[0]+0x6badc8);require(reinterpret_cast<uintptr_t>(out.original)==fixture::bases[1]+0x129b8);require(r.max_read<=256);});
 run("independent Dart complete functions prefix and notes accept",[&]{require(dart(r));});
 run("precise imported and snapshot offsets",[]{require(wg::kWeatherPropertySlot==0x6badc8&&wg::kPublicPrivateSlot==0x18a48&&wg::kPrivateGetVa==0x34b14&&wg::kLazyStateVa==0x6c1548);require(wg::kDartVmDataVa==0x340&&wg::kDartVmInstructionsVa==0x2d0000&&wg::kDartIsolateDataVa==0x4ac0&&wg::kDartIsolateInstructionsVa==0x2e6940);});
 run("all guard bit flips rejected without partial output",[&]{for(const auto&g:fixture::regions)for(size_t n=0;n<g.size;n++){r.mutation=fixture::bases[g.image]+g.va+n;for(uint8_t bit=1;bit;bit<<=1){r.xor_mask=bit;if(g.image==3)require(!dart(r),"Dart mutation accepted");else reject(r);++bit_rejections;}}r.reset_imports();require(bit_rejections==fixture::guard_bits);});
 run("every guard first middle last unreadable rejected",[&]{for(const auto&g:fixture::regions)for(size_t n:{size_t(0),g.size/2,g.size-1}){r.unmapped=fixture::bases[g.image]+g.va+n;if(g.image==3)require(!dart(r));else reject(r);}r.reset_imports();});
 run("foreign or absent Weather property import rejected",[&]{for(uintptr_t value:{uintptr_t(0),fixture::bases[1]+0x129bc,fixture::bases[2]+0x129b8,UINTPTR_MAX}){r.pointer(fixture::bases[0],0x6badc8,value);reject(r);}r.reset_imports();});
 run("foreign or absent core private import rejected",[&]{for(uintptr_t value:{uintptr_t(0),fixture::bases[2]+0x34b18,fixture::bases[1]+0x34b14,UINTPTR_MAX}){r.pointer(fixture::bases[1],0x18a48,value);reject(r);}r.reset_imports();});
 for(size_t i=0;i<fixture::vtables.size();++i)run("actual relocated callback pointer "+std::to_string(i),[&,i]{auto s=fixture::vtables[i];r.pointer(fixture::bases[0],s.slot,fixture::bases[0]+s.target+4);reject(r);require(!wb::verify_weather(fixture::bases[0],r));r.reset_imports();});
 run("each pointer read can fail closed",[&]{for(auto s:fixture::weather_imports){r.unmapped=fixture::bases[0]+s.slot;reject(r);}for(auto s:fixture::private_imports){r.unmapped=fixture::bases[1]+s.slot;reject(r);}for(auto s:fixture::vtables){r.unmapped=fixture::bases[0]+s.slot;reject(r);}r.reset_imports();});
 for(uintptr_t base:{uintptr_t(0),uintptr_t(1),fixture::bases[0]+1,UINTPTR_MAX,UINTPTR_MAX&~uintptr_t(4095)})run("invalid Weather base "+std::to_string(base),[&,base]{auto c=r.calls;reject(r,base);require(c==r.calls);});
 for(uintptr_t base:{uintptr_t(0),uintptr_t(1),UINTPTR_MAX&~uintptr_t(4095)})run("invalid Dart base "+std::to_string(base),[&,base]{auto c=r.calls;require(!dart(r,base));require(c==r.calls);});
 run("public subtraction underflow",[&]{r.pointer(fixture::bases[0],0x6badc8,0x129b7);reject(r);r.reset_imports();});
 run("private subtraction underflow",[&]{r.pointer(fixture::bases[1],0x18a48,0x34b13);reject(r);r.reset_imports();});
 run("checked address helper rejects wrap without output change",[]{uintptr_t out=77;require(!wb::detail::address(UINTPTR_MAX-7,8,0,out)&&out==77);require(!wb::detail::address(UINTPTR_MAX-7,0,8,out)&&out==77);require(!wb::detail::address(0,1,1,out)&&out==77);});
 run("Weather import changed during validation",[&]{r.change_slot=fixture::bases[0]+0x6badc8;r.change_after=2;reject(r);r.reset_imports();});
 run("private import changed during validation",[&]{r.change_slot=fixture::bases[1]+0x18a48;r.change_after=2;reject(r);r.reset_imports();});
 for(size_t i=0;i<fixture::vtables.size();++i)run("vtable changed during validation "+std::to_string(i),[&,i]{r.change_slot=fixture::bases[0]+fixture::vtables[i].slot;r.change_after=2;reject(r);r.reset_imports();});
 run("only final caller output publication",[&]{Binding out=sentinel();auto reader=[&](uintptr_t addr,std::span<std::byte>bytes){require(same(out,sentinel()),"partial publication");return r(addr,bytes);};require(wb::resolve(fixture::bases[0],reader,out));});
 run("cold state exact3 and zero heap",[&]{Binding b;require(resolve(r,b));forbid_alloc=true;bool cold=wb::cold_state(b,r);forbid_alloc=false;require(cold);});
 run("warm running poisoned and waiter states refused",[&]{Binding b;require(resolve(r,b));for(uintptr_t state:{0U,1U,2U,4U,5U,6U,7U,UINT32_MAX}){r.pointer(fixture::bases[0],0x6c1548,state);require(!wb::cold_state(b,r));}r.pointer(fixture::bases[0],0x6c1548,3);});
 run("cold state read failure",[&]{Binding b;require(resolve(r,b));r.unmapped=fixture::bases[0]+0x6c1548;require(!wb::cold_state(b,r));r.reset_imports();});
 run("cold state rejects invalid base before reader",[&]{for(uintptr_t base:{uintptr_t(0),uintptr_t(1),UINTPTR_MAX&~uintptr_t(4095)}){Binding b;b.weather_base=base;auto c=r.calls;require(!wb::cold_state(b,r));require(c==r.calls);}});
 run("resolve is identity check not cold-state reservation",[&]{r.pointer(fixture::bases[0],0x6c1548,0);Binding b;require(resolve(r,b)&&!wb::cold_state(b,r));r.pointer(fixture::bases[0],0x6c1548,3);});
 std::cout<<"RESULT passes="<<passed<<" failures="<<failed<<" guard_bit_mutations="<<bit_rejections<<"; actual ROM data, simulated relocations/reader; not Android loader or phone\n";return failed?1:0;
}
