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
#include "targets/weather/weather_binding.h"
#include "fixture.generated.h"
static bool forbid_alloc=false;
void* operator new(std::size_t n){if(forbid_alloc)throw std::bad_alloc();if(void*p=std::malloc(n?n:1))return p;throw std::bad_alloc();}
void operator delete(void*p)noexcept{std::free(p);}void operator delete(void*p,std::size_t)noexcept{std::free(p);}
void* operator new[](std::size_t n){return ::operator new(n);}void operator delete[](void*p)noexcept{::operator delete(p);}void operator delete[](void*p,std::size_t)noexcept{::operator delete(p);}
using weather_binding::Binding;
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
 void reset_imports(){mutation=unmapped=change_slot=0;xor_mask=0;slot_reads=change_after=0;for(auto&s:fixture::weather_imports)pointer(images[0].base,s.slot,images[1].base+s.target);for(auto&s:fixture::standard_imports)pointer(images[1].base,s.slot,images[2].base+s.target);for(auto&s:fixture::vtables)pointer(images[0].base,s.slot,images[0].base+s.target);}
};
Binding sentinel(){Binding b;b.weather_base=11;b.public_base=22;b.standard_base=33;b.slot=44;b.api.get=reinterpret_cast<decltype(b.api.get)>(uintptr_t(0x1110));b.api.allocate=reinterpret_cast<decltype(b.api.allocate)>(uintptr_t(0x2220));b.api.allocation_shim=reinterpret_cast<decltype(b.api.allocation_shim)>(uintptr_t(0x3330));b.api.drop=reinterpret_cast<decltype(b.api.drop)>(uintptr_t(0x4440));return b;}
bool same(const Binding&a,const Binding&b){return a.weather_base==b.weather_base&&a.public_base==b.public_base&&a.standard_base==b.standard_base&&a.slot==b.slot&&a.api.get==b.api.get&&a.api.allocate==b.api.allocate&&a.api.allocation_shim==b.api.allocation_shim&&a.api.drop==b.api.drop;}
bool resolve(Reader&r,Binding&out,uintptr_t base=fixture::bases[0]){forbid_alloc=true;bool ok;try{ok=weather_binding::resolve(base,r,out);}catch(...){forbid_alloc=false;throw;}forbid_alloc=false;return ok;}
void reject(Reader&r,uintptr_t base=fixture::bases[0]){Binding out=sentinel();require(!resolve(r,out,base),"invalid binding accepted");require(same(out,sentinel()),"failed resolve changed caller output");}
bool dart(Reader&r,uintptr_t base=fixture::bases[3]){forbid_alloc=true;bool ok;try{ok=weather_binding::verify_dart(base,r);}catch(...){forbid_alloc=false;throw;}forbid_alloc=false;return ok;}
int main(int argc,char**argv){require(argc==5,"Weather/public/std/Dart ELF paths required");Reader r(const_cast<const char*const*>(argv+1));
 run("actual ELF PT_LOAD bytes and simulated resolved imports bind without heap",[&]{Binding out;require(resolve(r,out));require(out.weather_base==fixture::bases[0]&&out.public_base==fixture::bases[1]&&out.standard_base==fixture::bases[2]);require(out.slot==fixture::bases[0]+fixture::weather_imports[0].slot);require(reinterpret_cast<uintptr_t>(out.api.get)==fixture::bases[1]+0x10ec8);require(reinterpret_cast<uintptr_t>(out.api.drop)==fixture::bases[1]+0x10e8c);require(reinterpret_cast<uintptr_t>(out.api.allocate)==fixture::bases[2]+0x630ac);require(reinterpret_cast<uintptr_t>(out.api.allocation_shim)==fixture::bases[2]+0x63284);require(out.api.complete());require(r.max_read<=256);});
 run("Dart six full functions plus exact prefix and headers accept",[&]{require(dart(r));});
 run("export and entry offsets match independent source reports",[]{require(weather_guard::kWeatherEntryVa==0x2a56a8);require(weather_guard::kDartVmDataVa==0x340);});
 run("all 72040 guard bit flips rejected without output changes",[&]{for(const auto&g:fixture::regions){for(size_t n=0;n<g.size;n++){r.mutation=fixture::bases[g.image]+g.va+n;for(uint8_t bit=1;bit!=0;bit<<=1){r.xor_mask=bit;if(g.image==3)require(!dart(r),"Dart guard bit mutation accepted");else reject(r);bit_rejections++;}}}r.reset_imports();require(bit_rejections==72040);});
 run("every guard first middle last read unmapped rejected",[&]{for(const auto&g:fixture::regions){for(size_t n:{size_t(0),g.size/2,g.size-1}){r.unmapped=fixture::bases[g.image]+g.va+n;if(g.image==3)require(!dart(r));else reject(r);}}r.reset_imports();});
 for(size_t i=0;i<fixture::weather_imports.size();i++)run("mixed or missing Weather import "+std::to_string(i),[&,i]{auto row=fixture::weather_imports[i];for(uintptr_t value:{uintptr_t(0),fixture::bases[1]+row.target+4,fixture::bases[1]+0x100000+row.target,fixture::bases[2]+row.target,UINTPTR_MAX}){r.pointer(fixture::bases[0],row.slot,value);reject(r);}r.reset_imports();});
 run("swapped RAM/CPU getters refused despite valid same public image",[&]{auto a=fixture::weather_imports[1],b=fixture::weather_imports[2];r.pointer(fixture::bases[0],a.slot,fixture::bases[1]+b.target);r.pointer(fixture::bases[0],b.slot,fixture::bases[1]+a.target);reject(r);r.reset_imports();});
 for(size_t i=0;i<fixture::standard_imports.size();i++)run("foreign allocator or mismatched shim/drop import "+std::to_string(i),[&,i]{auto row=fixture::standard_imports[i];for(uintptr_t value:{uintptr_t(0),fixture::bases[2]+row.target+4,fixture::bases[2]+0x100000+row.target,fixture::bases[1]+row.target,UINTPTR_MAX}){r.pointer(fixture::bases[1],row.slot,value);reject(r);}r.reset_imports();});
 for(size_t i=0;i<fixture::vtables.size();i++)run("Weather channel dispatch points to wrong full function "+std::to_string(i),[&,i]{auto row=fixture::vtables[i];r.pointer(fixture::bases[0],row.slot,fixture::bases[0]+row.target+4);reject(r);require(!weather_binding::verify_weather(fixture::bases[0],r));r.reset_imports();});
 run("each imported pointer read can fail closed",[&]{for(const auto&s:fixture::weather_imports){r.unmapped=fixture::bases[0]+s.slot;reject(r);}for(const auto&s:fixture::standard_imports){r.unmapped=fixture::bases[1]+s.slot;reject(r);}for(const auto&s:fixture::vtables){r.unmapped=fixture::bases[0]+s.slot;reject(r);}r.reset_imports();});
 for(uintptr_t base:{uintptr_t(0),uintptr_t(1),fixture::bases[0]+1,UINTPTR_MAX,UINTPTR_MAX&~uintptr_t(4095)})run("zero misaligned or overflowing Weather base "+std::to_string(base),[&,base]{size_t before=r.calls;reject(r,base);require(r.calls==before,"invalid image base reached reader");});
 for(uintptr_t base:{uintptr_t(0),uintptr_t(1),UINTPTR_MAX&~uintptr_t(4095)})run("zero misaligned or overflowing Dart base "+std::to_string(base),[&,base]{size_t before=r.calls;require(!dart(r,base));require(r.calls==before);});
 run("public pointer subtraction underflow and extent overflow",[&]{for(uintptr_t p:{uintptr_t(0x10ec7),(UINTPTR_MAX&~uintptr_t(4095))+uintptr_t(0xec8)}){r.pointer(fixture::bases[0],fixture::weather_imports[0].slot,p);reject(r);}r.reset_imports();});
 run("standard pointer subtraction underflow",[&]{r.pointer(fixture::bases[1],0x18698,0x630ab);reject(r);r.reset_imports();});
 run("checked address overflow helper never changes output",[]{uintptr_t out=777;require(!weather_binding::detail::address(UINTPTR_MAX-7,8,0,out));require(out==777);require(!weather_binding::detail::address(UINTPTR_MAX-7,0,8,out));require(out==777);require(!weather_binding::detail::address(0,10,1,out));require(out==777);});
 run("Weather import changes during verification rejected",[&]{r.change_slot=fixture::bases[0]+fixture::weather_imports[0].slot;r.change_after=2;reject(r);r.reset_imports();});
 run("allocator changes between import snapshot and allocator-base read rejected",[&]{r.change_slot=fixture::bases[1]+0x18698;r.change_after=2;reject(r);r.reset_imports();});
 run("allocator changes at final read rejected",[&]{r.change_slot=fixture::bases[1]+0x18698;r.change_after=3;reject(r);r.reset_imports();});
 run("dispatch changes at final read rejected",[&]{r.change_slot=fixture::bases[0]+fixture::vtables[0].slot;r.change_after=2;reject(r);r.reset_imports();});
 run("read callback observes untouched caller output until return",[&]{Binding out=sentinel();auto checking=[&](uintptr_t addr,std::span<std::byte>bytes){require(same(out,sentinel()),"partial output publication");return r(addr,bytes);};require(weather_binding::resolve(fixture::bases[0],checking,out));require(out.api.complete());});
 std::cout<<"RESULT passes="<<passed<<" failures="<<failed<<" guard_bit_mutations="<<bit_rejections<<"; actual ELF data, simulated relocation/reader, no Android loader or phone execution\n";return failed?1:0;
}
