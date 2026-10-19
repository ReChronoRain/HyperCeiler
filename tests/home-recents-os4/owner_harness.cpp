#include "targets/home/recents_policy.h"
#include <time.h>
#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <elf.h>
#include <fstream>
#include <iostream>
#include <string>
#include <sys/mman.h>
#include <vector>

// Controlled image execution, not a loader or production hook. All replacements
// below are named in the report. In particular no Android provider is emulated.
struct Image {
 uint8_t *base; size_t size;
 explicit Image(const char *path) {
  std::ifstream f(path,std::ios::binary);std::vector<char>b((std::istreambuf_iterator<char>(f)),{});
  const auto *e=reinterpret_cast<const Elf64_Ehdr*>(b.data());
  const auto *p=reinterpret_cast<const Elf64_Phdr*>(b.data()+e->e_phoff);size=0;
  for(int i=0;i<e->e_phnum;++i)if(p[i].p_type==PT_LOAD)size=std::max(size,size_t(p[i].p_vaddr+p[i].p_memsz));
  size=((size+4095)&~size_t(4095))+4096;base=static_cast<uint8_t*>(mmap(nullptr,size,7,MAP_PRIVATE|MAP_ANONYMOUS,-1,0));
  if(base==MAP_FAILED)std::abort();
  for(int i=0;i<e->e_phnum;++i)if(p[i].p_type==PT_LOAD)std::memcpy(base+p[i].p_vaddr,b.data()+p[i].p_offset,p[i].p_filesz);
  const auto *s=reinterpret_cast<const Elf64_Shdr*>(b.data()+e->e_shoff);
  for(int i=0;i<e->e_shnum;++i)if(s[i].sh_type==SHT_RELA){
   const auto *r=reinterpret_cast<const Elf64_Rela*>(b.data()+s[i].sh_offset);
   for(size_t j=0;j<s[i].sh_size/sizeof(*r);++j)if(ELF64_R_TYPE(r[j].r_info)==R_AARCH64_RELATIVE){
    uintptr_t value=reinterpret_cast<uintptr_t>(base)+r[j].r_addend;std::memcpy(base+r[j].r_offset,&value,8);
   }
  }
 }
 ~Image(){munmap(base,size);}
 template<class T>void set(size_t off,T v){std::memcpy(base+off,&v,sizeof(v));}
 void stub(size_t off,void *fn){uint32_t w[]{0x58000050,0xd61f0200};std::memcpy(base+off,w,8);set(off+8,fn);__builtin___clear_cache(reinterpret_cast<char*>(base+off),reinterpret_cast<char*>(base+off+16));}
 void word(size_t off,uint32_t expected,uint32_t replacement){uint32_t old;std::memcpy(&old,base+off,4);if(old!=expected)std::abort();set(off,replacement);__builtin___clear_cache(reinterpret_cast<char*>(base+off),reinterpret_cast<char*>(base+off+4));}
};
static int passed=0,failed=0;
static void check(bool c,const std::string &s){if(c)++passed;else{++failed;std::cerr<<"FAIL "<<s<<'\n';}}
static bool missing=false,simple=false;
static std::string setting;
static int created=0,deleted=0,gets=0,drops=0,device_calls=0;
static std::vector<std::string> queried;
static void *resolver_new(void*){++created;return reinterpret_cast<void*>(0x12345678);}
static void resolver_delete(void *p){check(p==reinterpret_cast<void*>(0x12345678),"resolver identity retained");++deleted;}
static void get_string(uint64_t *out,void *r,const char *key,size_t len){
 ++gets;check(r==reinterpret_cast<void*>(0x12345678),"original resolver passed to settings getter");queried.emplace_back(key,len);
 out[0]=missing?UINT64_MAX:setting.size();out[1]=reinterpret_cast<uintptr_t>(setting.data());out[2]=setting.size();
}
static void raw_drop(uint64_t cap,void *ptr){++drops;check(cap==setting.size()&&ptr==setting.data(),"original string ownership tuple retained");}
static uint32_t device_simple(){++device_calls;return simple?1:0;}
static std::array<uint8_t,24> owner{};
static Image *rust=nullptr;
static int64_t now_ns(){ timespec ts{};if(clock_gettime(CLOCK_BOOTTIME,&ts)!=0)std::abort();return int64_t(ts.tv_sec)*1000000000+ts.tv_nsec; }
static hyos_snapshot::Snapshot snapshot(int percent){hyos_snapshot::Snapshot s;s.available=true;s.flags=1;s.epoch=1;s.revision=1;s.valid_until=now_ns()+4000000000;s.config.tweaks.animation_recents_enabled=true;s.config.tweaks.animation_recents_rate_percent=percent;return s;}
extern "C" const char hc_home_recents_template[],hc_home_recents_template_end[],hc_home_recents_lease_literal[],hc_home_recents_return_branch[];
static int callback_drops=0,bridge_reads=0;
static void callback_drop(void*){++callback_drops;}
static float value(){float f;std::memcpy(&f,owner.data()+16,4);return f;}
static double bridge_fixture(){++bridge_reads;return double(value());}
extern "C" double run_dart(void*,void*,void*,void*,void*);
static void update(){reinterpret_cast<void(*)(void*)>(rust->base+0x7a57d8)(owner.data());}
static void reset(){created=deleted=gets=drops=device_calls=0;queried.clear();owner.fill(0xcc);}
int main(int argc,char **argv){
 if(argc<3||argc>4)return 2;
 const std::string mutation=argc==4?argv[3]:"none";
 Image image(argv[1]);rust=&image;
 image.stub(0xdfa3f0,reinterpret_cast<void*>(&resolver_new));image.stub(0xdfa400,reinterpret_cast<void*>(&resolver_delete));
 image.stub(0xb9af1c,reinterpret_cast<void*>(&get_string));image.stub(0x9d5f38,reinterpret_cast<void*>(&raw_drop));
 image.stub(0x6c3260,reinterpret_cast<void*>(&device_simple));
 image.set<uint32_t>(0xf1f460,0);image.set<uint64_t>(0xf27370,0);
 if(mutation=="skip-clamp")image.word(0x7a5878,0x1e217800,0xd503201f);
 if(mutation=="skip-publication")image.word(0x7a58ac,0xbd001260,0xd503201f);
 struct Case{const char *text;bool absent;float normalized;};
 const Case cases[]={{"",true,1},{"",false,1},{"0",false,0},{"0.3",false,.3f},{"0.5",false,.5f},{"1",false,1},{"2",false,1},{"-1",false,0},{"bad",false,1},{"NaN",false,1},{"inf",false,1},{"-inf",false,0},{" 0.5",false,1},{"1e-1",false,.1f},{"1e99",false,1}};
 for(const auto &c:cases)for(bool mode:{false,true}){
  setting=c.text;missing=c.absent;simple=mode;reset();update();
  const float want=c.normalized*(mode?.8f:1.f);
  check(value()==want,std::string("actual Rust owner normalize '")+c.text+"' missing="+std::to_string(c.absent)+" simple="+std::to_string(mode));
  check(created==1&&deleted==1&&gets==1&&drops==(missing?0:1)&&device_calls==1,"single original query/drop/device decision");
  check(queried==std::vector<std::string>{"transition_animation_duration_ratio"},"exact 35-byte system key");
  check(std::all_of(owner.begin(),owner.begin()+16,[](auto x){return x==0xcc;})&&std::all_of(owner.begin()+20,owner.end(),[](auto x){return x==0xcc;}),"only owner float payload updated");
 }
 // Execute each original initialized ContentObserver callback, including the
 // tail-call forms. The two `etc` forms also drop the x2 callback argument
 // through its retained vtable. The other three forms ignore this argument.
 image.set<uint32_t>(0xf19dc0,0);image.set<uintptr_t>(0xf19db0,reinterpret_cast<uintptr_t>(owner.data()));
 std::array<uintptr_t,4> table{},argument{};table[3]=reinterpret_cast<uintptr_t>(&callback_drop);argument[3]=reinterpret_cast<uintptr_t>(table.data());
 for(size_t callback:{0x735898u,0x7358e4u,0x735914u,0x735944u,0x735990u}){
  setting="0.75";missing=false;simple=false;reset();callback_drops=0;
  reinterpret_cast<void(*)(void*,void*,void*,void*)>(image.base+callback)(nullptr,nullptr,argument.data(),nullptr);
  check(value()==.75f&&gets==1&&device_calls==1,"actual initialized observer callback updates shared owner "+std::to_string(callback));
  check(callback_drops==((callback==0x735898u||callback==0x735944u)?1:0),"observer retains exact argument drop behavior");
 }
 // Execute the full original Folme getter with a bounded fake Dart heap. Only
 // getAnimDurationRatio is replaced with a bridge that reads the original Rust
 // owner's f32 value. This is NOT execution of FRB, a GC or a running isolate.
 Image dart(argv[2]);dart.stub(0xcd6aa8,reinterpret_cast<void*>(&bridge_fixture));
 if(mutation=="skip-cache")dart.word(0x1657064,0x540002a1,0xd503201f);
 auto *heap=static_cast<uint8_t*>(mmap(reinterpret_cast<void*>(0x30000000),0x40000,3,MAP_PRIVATE|MAP_ANONYMOUS,-1,0));
 if(heap==MAP_FAILED||reinterpret_cast<uintptr_t>(heap)+0x40000>UINT32_MAX)std::abort();
 std::array<uintptr_t,128> thread{},pool{};auto *receiver=heap+0x1001;
 thread[0x60/8]=reinterpret_cast<uintptr_t>(heap+0x10000);thread[0x68/8]=reinterpret_cast<uintptr_t>(heap+0x18000);
 uint32_t null=0x10001;std::memcpy(receiver+0x1f,&null,4);
 auto cached=[&](){return run_dart(dart.base+0x1657048,thread.data(),pool.data(),heap+0x30000,receiver);};
 setting="0.25";simple=false;missing=false;reset();update();bridge_reads=0;
 check(cached()==.25&&bridge_reads==1,"original Folme getter reads owner on first use");
 setting="0.75";update();
 check(cached()==.25&&bridge_reads==1,"original Folme getter retains cached ratio after real owner update");
 std::memcpy(receiver+0x1f,&null,4);
 check(cached()==.75&&bridge_reads==2,"fresh fixture receiver reads current owner through original cache path");
 // Execute the ACTUAL production gateway and policy, with the exact vendor
 // updater/parser/callback bodies. The image mapping remains a fixture and is
 // deliberately not a production installer or Android page-lifetime proof.
 const auto off=image.size-4096;const auto len=size_t(hc_home_recents_template_end-hc_home_recents_template);
 if(len>4096)std::abort();
 std::memcpy(image.base+off,hc_home_recents_template,len);
 alignas(8) uint64_t lease_word=home_recents_policy::kPending;
 image.set(off+size_t(hc_home_recents_lease_literal-hc_home_recents_template),&lease_word);
 image.set(off+size_t(hc_home_recents_return_branch-hc_home_recents_template),uint32_t(0x14000000u|((int64_t(0x7a58ac)-int64_t(off+size_t(hc_home_recents_return_branch-hc_home_recents_template)))>>2&0x3ffffff)));
 __builtin___clear_cache(reinterpret_cast<char*>(image.base+off),reinterpret_cast<char*>(image.base+off+len));
 image.word(0x7a58a8,0xbd001fa0,0x14000000u|((off-0x7a58a8)>>2));
 for(bool mode:{false,true})for(int rate=30;rate<=200;++rate){
  // A new lease instance models a new cold process selection. Production never
  // resets an already denied process to Pending, even when the same value returns.
  __atomic_store_n(&lease_word,uint64_t{0},__ATOMIC_RELEASE);home_recents_policy::Lease lease(lease_word);
  auto selected=snapshot(rate);check(lease.arm(selected,now_ns()),"actual production policy arms every UI rate");
  setting="0.5";missing=false;simple=mode;reset();update();
  check(value()==float(rate)/100.f,"actual production gateway full 30..200 without OEM clamp/simple-factor");
  check(gets==1&&created==1&&deleted==1&&drops==1&&device_calls==1,"production gateway keeps original effects exactly once");
 }
 // A real gateway invocation before activation atomically makes later arm fail.
 __atomic_store_n(&lease_word,uint64_t{0},__ATOMIC_RELEASE);home_recents_policy::Lease early(lease_word);
 setting="0.5";simple=true;reset();update();check(value()==.4f,"pending gateway retains OEM");
 check(lease_word==1&&!early.arm(snapshot(200),now_ns()),"actual early gateway prevents late activation");
 for(int state:{0,1,2,3,4}){
  __atomic_store_n(&lease_word,uint64_t{0},__ATOMIC_RELEASE);home_recents_policy::Lease lease(lease_word);auto selected=snapshot(200);
  if(state==0)selected.config.tweaks.animation_recents_enabled=false;
  if(state==1)selected.valid_until=now_ns()-1000000;
  if(state==2)selected.config.tweaks.animation_recents_rate_percent=29;
  if(state==3)selected.config.tweaks.animation_recents_rate_percent=201;
  if(state==4)selected.flags=0;
  check(!lease.arm(selected,now_ns()),"disabled stale invalid unauthorized selection refuses arm");
  setting="0.5";simple=true;reset();update();check(value()==.4f,"denied production gateway retains exact OEM result");
 }
 // Direct expired-word fixture exercises actual SVC113 and terminal CASAL.
 lease_word=((uint64_t(now_ns()/1000000)-1)<<8)|200;setting="0.5";simple=true;reset();update();
 check(value()==.4f&&lease_word==1,"gateway expiry permanently denies without a worker");
 // Observer paths remain the real native callback bodies when armed.
 __atomic_store_n(&lease_word,uint64_t{0},__ATOMIC_RELEASE);home_recents_policy::Lease callbacks(lease_word);
 check(callbacks.arm(snapshot(170),now_ns()),"observer fixture selected rate armed");
 for(size_t callback:{0x735898u,0x7358e4u,0x735914u,0x735944u,0x735990u}){
  setting="0.5";missing=false;simple=true;reset();callback_drops=0;
  reinterpret_cast<void(*)(void*,void*,void*,void*)>(image.base+callback)(nullptr,nullptr,argument.data(),nullptr);
  check(value()==1.7f&&gets==1,"production gateway also covers each original ContentObserver");
  check(callback_drops==((callback==0x735898u||callback==0x735944u)?1:0),"production gateway preserves observer ownership drop");
 }
 munmap(heap,0x40000);
 std::cout<<"RESULT "<<passed<<" passed, "<<failed<<" failed\n";return failed?1:0;
}
