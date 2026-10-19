/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include <cassert>
#include <cstdio>
#include <cstring>
#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#include <string>
#include "../../app/src/main/cpp/targets/hyos/hyos_report_runtime.cpp"
extern "C" void hc_home_report_entry() {}
struct ZnSymbolResolver {};
namespace test {
bool enabled=true,loaded=true,expire=false,wrong_size=false,duplicate=false,outside=false;
int new_count=0,free_count=0,hook_count=0;
uintptr_t image=0;
ZnSymbolResolver resolver;
constexpr uintptr_t target_va=0x14b3838;
ZnSymbolResolver *create(const char *name,void *base){assert(std::strcmp(name,"libapp.so")==0&&base==nullptr);++new_count;return loaded?&resolver:nullptr;}
void release(ZnSymbolResolver *r){assert(r==&resolver);++free_count;}
void *base(ZnSymbolResolver *r){assert(r==&resolver);if(expire)enabled=false;return reinterpret_cast<void *>(image);}
void symbols(ZnSymbolResolver *r,bool(*visitor)(const char*,void*,size_t,void*),void *data){
 assert(r==&resolver);
 assert(visitor("irrelevant",reinterpret_cast<void*>(image),4,data));
 auto target=reinterpret_cast<void *>(image+(outside?0:target_va));
 bool more=visitor(home_report::kSymbol.data(),target,wrong_size?16:home_report::kBodyBytes,data);
 if(duplicate&&more)visitor(home_report::kSymbol.data(),reinterpret_cast<void *>(image+target_va+4),home_report::kBodyBytes,data);
}
int hook(void *target,void*,void **original){assert(target==reinterpret_cast<void *>(image+target_va));++hook_count;*original=target;return 0;}
int unhook(void*){return 0;}
}
namespace hyos_control { bool feature_enabled(uint32_t flag){assert(flag==kHideReport);return test::enabled;} }
int main(int argc,char **argv){
 assert(argc==3);using namespace test;
 int fd=open(argv[2],O_RDONLY);assert(fd>=0);struct stat st{};assert(fstat(fd,&st)==0);
 void *bytes=mmap(nullptr,st.st_size,PROT_READ|PROT_WRITE,MAP_PRIVATE,fd,0);close(fd);assert(bytes!=MAP_FAILED);image=reinterpret_cast<uintptr_t>(bytes);
 ZygiskNextAPI api{};api.newSymbolResolver=create;api.freeSymbolResolver=release;api.getBaseAddress=base;api.forEachSymbols=symbols;api.inlineHook=hook;api.inlineUnhook=unhook;
 const std::string scenario=argv[1];
 if(scenario=="disabled"){enabled=false;hyperceiler_hyos_retry_home_report(&api);assert(new_count==0&&hook_count==0);}
 else if(scenario=="null-api"){hyperceiler_hyos_retry_home_report(nullptr);api.getBaseAddress=nullptr;hyperceiler_hyos_retry_home_report(&api);assert(new_count==0);}
 else if(scenario=="not-loaded-then-retry"){loaded=false;hyperceiler_hyos_retry_home_report(&api);assert(new_count==1&&free_count==0&&hook_count==0);loaded=true;hyperceiler_hyos_retry_home_report(&api);assert(hook_count==1&&free_count==1);}
 else if(scenario=="wrong-note"){reinterpret_cast<unsigned char *>(bytes)[hyos_guard::kDartNoteVa+16]^=1;hyperceiler_hyos_retry_home_report(&api);assert(hook_count==0&&free_count==1);}
 else if(scenario=="wrong-elf"){reinterpret_cast<unsigned char *>(bytes)[18]^=1;hyperceiler_hyos_retry_home_report(&api);assert(hook_count==0&&free_count==1);}
 else if(scenario=="outside-executable"){outside=true;hyperceiler_hyos_retry_home_report(&api);assert(hook_count==0&&free_count==1);}
 else if(scenario=="wrong-size"){wrong_size=true;hyperceiler_hyos_retry_home_report(&api);assert(hook_count==0&&free_count==1);}
 else if(scenario=="duplicate"){duplicate=true;hyperceiler_hyos_retry_home_report(&api);assert(hook_count==0&&free_count==1);}
 else if(scenario=="expires-during-discovery"){expire=true;hyperceiler_hyos_retry_home_report(&api);assert(hook_count==0&&free_count==1);}
 else if(scenario=="exact-rom-installs-once"){hyperceiler_hyos_retry_home_report(&api);assert(hook_count==1&&free_count==1);hyperceiler_hyos_retry_home_report(&api);assert(hook_count==1&&new_count==1&&free_count==1);}
 else if(scenario=="body-mutated"){reinterpret_cast<unsigned char *>(bytes)[target_va+4]^=1;hyperceiler_hyos_retry_home_report(&api);assert(hook_count==0&&free_count==1);}
 else assert(false);
 munmap(bytes,st.st_size);std::printf("PASS %s\n",argv[1]);
}
