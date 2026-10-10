// Dedicated Android experiment adapters. Not part of the production target.
#include <android/dlext.h>
#include <link.h>
#include <jni.h>
#include <dlfcn.h>
#include <cstring>
#include <cstdlib>
#include <cstdint>
#include <cerrno>
#include <sys/mman.h>
#include <unistd.h>
static int scenario=0, force_calls=0, pread_calls=0, init_attempts=0;static void* original=nullptr;static void* private_handle=nullptr;
extern "C" void* __real_dlsym(void*,const char*);
extern "C" void* __real_dlopen(const char*,int);
extern "C" void* __real_android_dlopen_ext(const char*,int,const android_dlextinfo*);
extern "C" void* __real_malloc(size_t);
extern "C" ssize_t __real_pread(int,void*,size_t,off_t);
extern "C" JNIEXPORT void JNICALL Java_local_hyperceiler_colorprobe_MainActivity_fault(JNIEnv*,jclass,jint s){scenario=s;}
extern "C" JNIEXPORT jint JNICALL Java_local_hyperceiler_colorprobe_MainActivity_forceCalls(JNIEnv*,jclass){return force_calls;}
extern "C" JNIEXPORT jint JNICALL Java_local_hyperceiler_colorprobe_MainActivity_initAttempts(JNIEnv*,jclass){return init_attempts;}
extern "C" void* __wrap_dlsym(void* h,const char* n){
 if(h==RTLD_DEFAULT&&strcmp(n,"_Z15getMainColorHCTP7_JNIEnvP7_jclassP10_jintArrayii")==0)++init_attempts;
 if(scenario==1&&h==RTLD_DEFAULT&&strcmp(n,"_ZN24material_color_utilities3HctC1Ej")==0)return reinterpret_cast<void*>(1);
 void* p=__real_dlsym(h,n);
 if(scenario==4&&private_handle&&h==private_handle&&strcmp(n,"_ZN19MainColorSingleRect15getMainColorHCTEPiiiS0_")==0)return static_cast<char*>(p)+4;
 return p;
}
extern "C" void* __wrap_dlopen(const char* p,int flags){if(flags&RTLD_NOLOAD){if(scenario==2)return nullptr;original=__real_dlopen(p,flags);return original;}return __real_dlopen(p,flags);}
static void alter(uintptr_t address,const void* bytes,size_t size,int final_perm){const uintptr_t page=address&~uintptr_t(getpagesize()-1);if(mprotect(reinterpret_cast<void*>(page),getpagesize(),PROT_READ|PROT_WRITE)!=0)abort();memcpy(reinterpret_cast<void*>(address),bytes,size);if(mprotect(reinterpret_cast<void*>(page),getpagesize(),final_perm)!=0)abort();}
extern "C" void* __wrap_android_dlopen_ext(const char* p,int f,const android_dlextinfo* e){
 ++force_calls;if(scenario==3)return __real_dlopen(p,RTLD_NOW|RTLD_NOLOAD|RTLD_LOCAL);
 private_handle=__real_android_dlopen_ext(p,f,e);if(!private_handle)return nullptr;
 void* process=__real_dlsym(private_handle,"process");Dl_info i{};if(!dladdr(process,&i))abort();uintptr_t b=reinterpret_cast<uintptr_t>(i.dli_fbase);
 if(scenario==5){uintptr_t foreign=b+0x209fc;alter(b+0x5bb38,&foreign,8,PROT_READ);}
 if(scenario==6){uint32_t nop=0xd503201f;alter(b+0x1eea4,&nop,4,PROT_READ|PROT_EXEC);}
 if(scenario==7)*reinterpret_cast<int*>(static_cast<char*>(process)+0x80030)=99;
 return private_handle;
}
extern "C" void* __wrap_malloc(size_t size){if(scenario==8&&size==362752)return nullptr;return __real_malloc(size);}
extern "C" ssize_t __wrap_pread(int fd,void* p,size_t n,off_t o){++pread_calls;if(scenario==9)return 0;if(scenario==10&&pread_calls==1){errno=EINTR;return -1;}return __real_pread(fd,p,n,o);}
#include <stdexcept>
extern "C" JNIEXPORT jlong JNICALL Java_com_sevtinge_hyperceiler_libhook_rules_systemui_controlcenter_media3_AmbientNativeColorOS4_nativeResolve(JNIEnv*,jclass,jintArray,jint,jint);
static const JNINativeInterface* saved_jni=nullptr;static int special_mode=0,throws=0;
static int throw_algorithm(void*,int*,int,int,int*){++throws;throw std::runtime_error("injected vendor failure after histogram mutation");}
static void JNICALL region(JNIEnv* env,jintArray a,jsize start,jsize len,jint* out){
 if(special_mode==13){env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"),"injected GetIntArrayRegion OOM");return;}
 saved_jni->GetIntArrayRegion(env,a,start,len,out);if(env->ExceptionCheck())return;
 Dl_info i{};if(!dladdr(__real_dlsym(private_handle,"process"),&i))abort();uintptr_t pc=reinterpret_cast<uintptr_t>(i.dli_fbase)+0x209f8;
 if(special_mode==14){uint64_t words[2]={UINT64_C(0xd61f020058000050),reinterpret_cast<uintptr_t>(&throw_algorithm)};alter(pc,words,sizeof(words),PROT_READ|PROT_EXEC);}
 if(special_mode==15){uint32_t words[2]={0x2a1f03e0,0xd65f03c0};alter(pc,words,sizeof(words),PROT_READ|PROT_EXEC);}
 __builtin___clear_cache(reinterpret_cast<char*>(pc),reinterpret_cast<char*>(pc+16));
}
extern "C" JNIEXPORT jlong JNICALL Java_local_hyperceiler_colorprobe_MainActivity_special(JNIEnv* env,jclass,jint mode,jintArray a){
 saved_jni=env->functions;JNINativeInterface table=*saved_jni;table.GetIntArrayRegion=region;special_mode=mode;env->functions=&table;
 const jlong result=Java_com_sevtinge_hyperceiler_libhook_rules_systemui_controlcenter_media3_AmbientNativeColorOS4_nativeResolve(env,nullptr,a,112,112);
 const bool pending=env->ExceptionCheck();env->functions=saved_jni;
 // Only this test observer consumes the injected exception, after proving it survived.
 if(pending)env->ExceptionClear();
 if(mode==13)return result==0&&pending?1:0;
 if(mode==14){const jlong second=Java_com_sevtinge_hyperceiler_libhook_rules_systemui_controlcenter_media3_AmbientNativeColorOS4_nativeResolve(env,nullptr,a,112,112);return result==0&&second==0&&!pending&&throws==1?1:0;}
 return result;
}

using IterateCallback=int(*)(dl_phdr_info*,size_t,void*);
extern "C" int __real_dl_iterate_phdr(IterateCallback,void*);
struct IterateContext {IterateCallback callback;void* data;uintptr_t bridge;};
static int tracked_callback(dl_phdr_info* info,size_t size,void* data){
 auto& c=*static_cast<IterateContext*>(data);
 if(scenario==19&&info->dlpi_addr==c.bridge)return 0;
 if(scenario==21)return c.callback(info,0,c.data);
 if(scenario==22){auto copy=*info;copy.dlpi_addr=0;return c.callback(&copy,size,c.data);}
 int r=c.callback(info,size,c.data);if(scenario==23&&!r)r=c.callback(info,size,c.data);return r;
}
extern "C" int __wrap_dl_iterate_phdr(IterateCallback callback,void* data){
 if(scenario==2)return 1;
 if(scenario==18)return 0;
 if(scenario==20){Elf64_Phdr ph{};dl_phdr_info i{};i.dlpi_phdr=&ph;i.dlpi_phnum=1;for(unsigned n=0;n<2049;++n){i.dlpi_addr=UINT64_C(0x100000000)+n*4096;int r=callback(&i,sizeof(i),data);if(r)return r;}return 0;}
 Dl_info self{};if(!dladdr(reinterpret_cast<void*>(&tracked_callback),&self))abort();IterateContext c{callback,data,reinterpret_cast<uintptr_t>(self.dli_fbase)};return __real_dl_iterate_phdr(tracked_callback,&c);
}
