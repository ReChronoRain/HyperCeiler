/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "ambient_sha256.h"
#include "ambient_bindings.generated.h"
#include <android/dlext.h>
#include <android/log.h>
#include <dlfcn.h>
#include <elf.h>
#include <jni.h>
#include <link.h>
#include <cerrno>
#include <cstdlib>
#include <cstring>
#include <fcntl.h>
#include <memory>
#include <mutex>
#include <sys/stat.h>
#include <unistd.h>

namespace ambient_os4 {
namespace {
constexpr size_t kFileSize=362752;
constexpr uintptr_t kProcess=0x5c0b0, kProcessGot=0x5bfe8;
constexpr uintptr_t kInit=0x1eea4, kHct=0x209f8, kWrapper=0x25ce8;
constexpr std::array<uint8_t,32> kDigest={0x65,0x2e,0xb3,0xfb,0x88,0x50,0xb4,0x6a,0x4d,0xf0,0x86,0x5c,0x1a,0xbf,0x4f,0x3c,0xa5,0xb0,0x08,0x4d,0x82,0x8d,0xce,0xb5,0xce,0x54,0x7b,0xe8,0x6e,0x1d,0x18,0x52};
constexpr char kInitName[]="_ZN19MainColorSingleRect4initEv";
constexpr char kHctName[]="_ZN19MainColorSingleRect15getMainColorHCTEPiiiS0_";
constexpr char kWrapperName[]="_Z15getMainColorHCTP7_JNIEnvP7_jclassP10_jintArrayii";
#ifndef HYPERCEILER_AMBIENT_TEST_PATH
constexpr char kPath[]="/system_ext/lib64/libMiMainColor.so";
#else
// Research builds only: substitute the same hash-pinned OTA file inside a probe app.
constexpr char kPath[]=HYPERCEILER_AMBIENT_TEST_PATH;
#endif
using Init=void(*)(void*);
using Hct=int(*)(void*,int*,int,int,int*);
std::mutex mutex;
bool attempted=false;
bool available=false;
void* held_private=nullptr;
void* process=nullptr;
Hct hct=nullptr;
uintptr_t private_base=0;
enum class Stage { NotStarted, GlobalSymbolVisible, FileOpen, FileStat, FileRead,
    FileIdentity, ElfIdentity, MappingInventory, MappingIdentity, PrivateLoad,
    PrivateIdentity, SelfBindings, CodeIdentity, ConstructorDefaults, Ready, Exception,
    AlgorithmException };
Stage stage=Stage::NotStarted;
bool init_logged=false;
size_t prior_mapping_count=0, prior_vendor_name_count=0;
bool fail(Stage value) noexcept {stage=value;return false;}
void log_initialization() noexcept {
    if(init_logged||!attempted)return;
    init_logged=true;
    __android_log_print(available?ANDROID_LOG_INFO:ANDROID_LOG_WARN,"HyperCeilerAmbientOS4",
        "isolated color initialization %s stage=%d vendor_sha=652eb3fb8850b46a prior_objects=%zu prior_vendor_names=%zu",
        available?"ready":"unavailable",static_cast<int>(stage),prior_mapping_count,prior_vendor_name_count);
}
struct CloseFd { int fd; ~CloseFd(){if(fd>=0)close(fd);} };
using Bytes=std::unique_ptr<uint8_t,decltype(&std::free)>;
struct MappingInventory {
    std::array<uintptr_t,2048> bases{};
    size_t count=0, vendor_names=0;
    bool valid=true;
    bool contains(uintptr_t base) const noexcept {
        for(size_t i=0;i<count;++i)if(bases[i]==base)return true;
        return false;
    }
};
int collect_mapping(dl_phdr_info* info,size_t size,void* opaque) noexcept {
    auto& inventory=*static_cast<MappingInventory*>(opaque);
    if(!info||size<offsetof(dl_phdr_info,dlpi_phnum)+sizeof(info->dlpi_phnum)||
       !info->dlpi_phdr||!info->dlpi_phnum||!info->dlpi_addr||
       inventory.count==inventory.bases.size()||inventory.contains(info->dlpi_addr)) {
        inventory.valid=false;
        return 1;
    }
    inventory.bases[inventory.count++]=info->dlpi_addr;
    // Diagnostic only: freshness is checked against EVERY previously loaded base,
    // not a filename or the caller's namespace visibility of an OEM handle.
    if(info->dlpi_name) {
        const char* name=std::strrchr(info->dlpi_name,'/');
        name=name?name+1:info->dlpi_name;
        if(std::strcmp(name,"libMiMainColor.so")==0)++inventory.vendor_names;
    }
    return 0;
}

bool symbol_at(void* handle,const char* name,uintptr_t base,uintptr_t offset) noexcept {
    void* symbol=dlsym(handle,name);Dl_info info{};
    return symbol && reinterpret_cast<uintptr_t>(symbol)==base+offset &&
        dladdr(symbol,&info)!=0 && reinterpret_cast<uintptr_t>(info.dli_fbase)==base;
}
bool loaded_identity() noexcept {
    if(!held_private||!process||!hct||!private_base)return false;
    void* actual=nullptr;
    std::memcpy(&actual,reinterpret_cast<void*>(private_base+kProcessGot),sizeof(actual));
    if(!(actual==process && reinterpret_cast<uintptr_t>(process)==private_base+kProcess &&
        symbol_at(held_private,"process",private_base,kProcess) &&
        symbol_at(held_private,kInitName,private_base,kInit) &&
        symbol_at(held_private,kHctName,private_base,kHct) &&
        symbol_at(held_private,kWrapperName,private_base,kWrapper)))return false;
    for(const auto& binding:kSelfBindings) {
        uintptr_t value=0;
        std::memcpy(&value,reinterpret_cast<void*>(private_base+binding.slot),sizeof(value));
        if(value!=private_base+binding.target)return false;
    }
    return true;
}
bool initialize() {
    // Init_array executes before a post-load GOT check. Reject an already-visible
    // self-domain symbol before FORCE_LOAD to avoid constructor interposition.
    // Generic libc/libc++ APIs remain normal dependencies; this includes all
    // dynamic MainColor/material/ThreadPool references of the exact vendor DSO.
    for(const auto& binding:kSelfBindings)
        if(dlsym(RTLD_DEFAULT,binding.name)!=nullptr)return fail(Stage::GlobalSymbolVisible);
    // No path-based check/use pair: inspect and hash this descriptor, then give
    // this same held descriptor to ANDROID_DLEXT_USE_LIBRARY_FD below. Reject
    // symlinks, non-regular files and changed identity before the loader runs.
    CloseFd file{open(kPath,O_RDONLY|O_CLOEXEC|O_NOFOLLOW)}; // nosemgrep: c_misc_rule-fopen-open
    if(file.fd<0)return fail(Stage::FileOpen);
    struct stat before{},after{};
    if(fstat(file.fd,&before)!=0||!S_ISREG(before.st_mode)||before.st_size!=kFileSize)return fail(Stage::FileStat);
    Bytes bytes(static_cast<uint8_t*>(std::malloc(kFileSize)),&std::free);
    if(!bytes)return fail(Stage::FileRead);
    size_t done=0;
    while(done<kFileSize) {
        const ssize_t n=pread(file.fd,bytes.get()+done,kFileSize-done,static_cast<off_t>(done));
        if(n<0&&errno==EINTR)continue;
        if(n<=0)return fail(Stage::FileRead);
        done+=static_cast<size_t>(n);
    }
    if(fstat(file.fd,&after)!=0||before.st_dev!=after.st_dev||before.st_ino!=after.st_ino||
       before.st_size!=after.st_size||before.st_mtim.tv_sec!=after.st_mtim.tv_sec||
       before.st_mtim.tv_nsec!=after.st_mtim.tv_nsec||sha256(bytes.get(),kFileSize)!=kDigest)return fail(Stage::FileIdentity);
    Elf64_Ehdr header{};std::memcpy(&header,bytes.get(),sizeof(header));
    if(std::memcmp(header.e_ident,ELFMAG,SELFMAG)!=0||header.e_ident[EI_CLASS]!=ELFCLASS64||
       header.e_ident[EI_DATA]!=ELFDATA2LSB||header.e_type!=ET_DYN||header.e_machine!=EM_AARCH64||
       header.e_phnum!=8||header.e_entry!=0x1ba90)return fail(Stage::ElfIdentity);
    // Bionic's public iterator traverses the process-wide loaded-object list,
    // including objects in other classloader namespaces. No OEM handle is needed.
    // Keep a bounded pre-load snapshot, and reject any reused mapped object base.
    MappingInventory inventory;
    if(dl_iterate_phdr(collect_mapping,&inventory)!=0||!inventory.valid||!inventory.count)
        return fail(Stage::MappingInventory);
    prior_mapping_count=inventory.count;prior_vendor_name_count=inventory.vendor_names;
    Dl_info bridge_info{};
    if(!dladdr(reinterpret_cast<void*>(&collect_mapping),&bridge_info)||
       !inventory.contains(reinterpret_cast<uintptr_t>(bridge_info.dli_fbase)))
        return fail(Stage::MappingIdentity);
    android_dlextinfo ext{};ext.flags=ANDROID_DLEXT_FORCE_LOAD|ANDROID_DLEXT_USE_LIBRARY_FD;ext.library_fd=file.fd;
    held_private=android_dlopen_ext(kPath,RTLD_NOW|RTLD_LOCAL,&ext);
    if(!held_private)return fail(Stage::PrivateLoad);
    process=dlsym(held_private,"process");Dl_info info{};
    if(!process||!dladdr(process,&info)||!info.dli_fbase||
       inventory.contains(reinterpret_cast<uintptr_t>(info.dli_fbase)))return fail(Stage::PrivateIdentity);
    private_base=reinterpret_cast<uintptr_t>(info.dli_fbase);
    hct=reinterpret_cast<Hct>(dlsym(held_private,kHctName));
    if(!loaded_identity())return fail(Stage::SelfBindings);
    // RX text and literal data have no dynamic relocations in this exact ELF.
    // Check all instructions, including private constructor/slow paths, against FD.
    if(std::memcmp(reinterpret_cast<void*>(private_base+0x1a930),bytes.get()+0x1a930,0x49e19-0x1a930)!=0)return fail(Stage::CodeIdentity);
    int tone=0,chroma=0,mode=0;
    std::memcpy(&tone,static_cast<uint8_t*>(process)+0x80030,4);
    std::memcpy(&chroma,static_cast<uint8_t*>(process)+0x80034,4);
    std::memcpy(&mode,static_cast<uint8_t*>(process)+0x8003c,4);
    if(tone!=20||chroma!=20||mode!=1)return fail(Stage::ConstructorDefaults);
    // Initialize only the fully constructed private SingleRect member. The OEM
    // object and its settings remain untouched; HCT's null-rect path uses no pool.
    reinterpret_cast<Init>(dlsym(held_private,kInitName))(process);
    if(!loaded_identity())return fail(Stage::SelfBindings);
    stage=Stage::Ready;
    return true;
}
} // namespace
} // namespace ambient_os4

extern "C" JNIEXPORT jlong JNICALL
Java_com_sevtinge_hyperceiler_libhook_rules_systemui_controlcenter_media3_AmbientNativeColorOS4_nativeResolve(
    JNIEnv* env,jclass,jintArray pixels,jint width,jint height) {
    if(env->ExceptionCheck()||!pixels||width<=0||height<=0)return 0;
    const int64_t count=static_cast<int64_t>(width)*height;
    if(count>25088||count>env->GetArrayLength(pixels))return 0;
    try {
        std::lock_guard<std::mutex> lock(ambient_os4::mutex);
        if(!ambient_os4::attempted) {
            ambient_os4::attempted=true;
            try {
                ambient_os4::available=ambient_os4::initialize();
            } catch(...) {
                ambient_os4::available=false;
                ambient_os4::stage=ambient_os4::Stage::Exception;
            }
            ambient_os4::log_initialization();
        }
        if(!ambient_os4::available||!ambient_os4::loaded_identity())return 0;
        std::unique_ptr<int,decltype(&std::free)> copy(static_cast<int*>(std::malloc(static_cast<size_t>(count)*sizeof(int))),&std::free);
        if(!copy)return 0;
        env->GetIntArrayRegion(pixels,0,static_cast<jsize>(count),copy.get());
        // Leave a JNI exception pending. The Kotlin boundary can handle its own
        // call failure; native code must not erase OOM or any caller exception.
        if(env->ExceptionCheck())return 0;
        try {
            const uint32_t color=static_cast<uint32_t>(ambient_os4::hct(ambient_os4::process,copy.get(),width,height,nullptr));
            return static_cast<jlong>(UINT64_C(0x100000000)|color);
        } catch(...) {
            // A vendor exception may leave histogram buckets partially updated.
            // Retire this private object under the same lock; never reuse it.
            ambient_os4::available=false;
            ambient_os4::stage=ambient_os4::Stage::AlgorithmException;
            __android_log_print(ANDROID_LOG_WARN,"HyperCeilerAmbientOS4",
                "isolated color unavailable stage=%d algorithm exception; instance retired",
                static_cast<int>(ambient_os4::stage));
            return 0;
        }
    } catch(...) {
        return 0;
    }
}
