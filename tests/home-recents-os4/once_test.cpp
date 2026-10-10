#include <algorithm>
#include <atomic>
#include <cerrno>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <elf.h>
#include <fstream>
#include <iostream>
#include <string>
#include <sys/mman.h>
#include <sys/syscall.h>
#include <thread>
#include <unistd.h>
#include <vector>
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

static Image*image=nullptr;
static std::atomic<bool> entered{false},release_initializer{false};
static std::atomic<int> calls{0};
static uintptr_t owner_payload[3]{1,1,0}, observer_payload[4]{1,1,0,0};
static uint32_t state(){return __atomic_load_n(reinterpret_cast<uint32_t*>(image->base+0xf19dc0),__ATOMIC_ACQUIRE);}
static void closure(void *argument,void*){
    ++calls;
    auto *capture=*reinterpret_cast<uintptr_t**>(argument);
    auto *destination=reinterpret_cast<uintptr_t*>(*capture);
    *capture=0;
    entered.store(true,std::memory_order_release);
    while(!release_initializer.load(std::memory_order_acquire))std::this_thread::yield();
    destination[0]=reinterpret_cast<uintptr_t>(owner_payload);
    destination[1]=reinterpret_cast<uintptr_t>(observer_payload);
}
static int *errno_pointer(){return &errno;}
static void initialize(){reinterpret_cast<void(*)()>(image->base+0x4f0890)();}
int main(int argc,char**argv){
    if(argc<2||argc>3)return 2;
    Image loaded(argv[1]);image=&loaded;
    loaded.stub(0x625708,reinterpret_cast<void*>(closure));
    loaded.stub(0xdfb590,reinterpret_cast<void*>(::syscall));
    loaded.stub(0xdfbe00,reinterpret_cast<void*>(errno_pointer));
    const std::string mutant=argc==3?argv[2]:"none";
    if(mutant=="still-cold")loaded.word(0xd8e148,0x32000129,0x32000529);
    if(mutant=="publish-cold")loaded.word(0xd8e22c,0xb9400be9,0x52800069);
    int passed=0,failed=0;
    auto check=[&](bool okay,const char *name){if(okay)++passed;else{++failed;std::cerr<<"FAIL "<<name<<'\n';}};
    check(state()==3,"actual Once starts cold3");
    std::thread first(initialize);
    for(int i=0;i<100000 && !entered.load(std::memory_order_acquire);++i)usleep(10);
    check(entered.load(),"actual Once dispatches initializer through real vtable");
    check(state()==1,"actual owner initializer begins only after Once becomes running1");
    std::thread second(initialize);
    for(int i=0;i<100000 && state()!=5 && calls.load()==1;++i)usleep(10);
    check(state()==5,"actual concurrent reader records waiter bit5");
    check(calls.load()==1,"actual original Once excludes duplicate initializer");
    release_initializer.store(true,std::memory_order_release);first.join();second.join();
    check(state()==0,"original completion releases initialized0");
    check(calls.load()==1,"waiter resumes without repeating initializer");
    uintptr_t tuple[2];std::memcpy(tuple,loaded.base+0xf19db0,sizeof(tuple));
    check(tuple[0]==reinterpret_cast<uintptr_t>(owner_payload) && tuple[1]==reinterpret_cast<uintptr_t>(observer_payload),"original tuple publication visible after Once");
    initialize();check(calls.load()==1,"subsequent original get_or_init cached fastpath");
    std::cout<<"PASSED="<<passed<<" FAILED="<<failed<<std::endl;return failed?1:0;
}
