/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "diagnostics.h"
#ifdef __ANDROID__
#include <android/log.h>
#include <link.h>
#include <unistd.h>
#include <array>
#include <cstdio>
#include <cstring>
#include <ctime>
#include <string>
namespace nhk::android_diagnostics {
inline std::string b64(const std::string& s){
    constexpr char chars[]="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    std::string out;unsigned value=0,bits=0;
    for(unsigned char c:s){value=(value<<8)|c;bits+=8;while(bits>=6){bits-=6;out+=chars[(value>>bits)&63];}}
    if(bits)out+=chars[(value<<(6-bits))&63];return out;
}
inline std::string file(const char* path){char out[512]{};FILE* f=std::fopen(path,"r");if(!f)return {};size_t n=fread(out,1,sizeof(out)-1,f);fclose(f);return std::string(out,n);}
struct Identity {pid_t pid=0;std::string boot,start,process,module;};
inline Identity identity(){
    Identity id;id.pid=getpid();id.boot=file("/proc/sys/kernel/random/boot_id");
    while(!id.boot.empty()&&(id.boot.back()=='\n'||id.boot.back()=='\r'))id.boot.pop_back();
    id.process=file("/proc/self/cmdline");id.process.resize(std::strlen(id.process.c_str()));
    auto stat=file("/proc/self/stat");auto close=stat.rfind(')');
    if(close!=std::string::npos){size_t pos=close+2;for(unsigned i=0;i<19&&pos<stat.size();++i){auto end=stat.find(' ',pos);pos=end==std::string::npos?stat.size():end+1;}auto end=stat.find(' ',pos);if(pos<stat.size())id.start=stat.substr(pos,end-pos);}
    struct Search {uintptr_t self;std::string build;};Search search{reinterpret_cast<uintptr_t>(&identity),{}};
    dl_iterate_phdr([](dl_phdr_info* info,size_t,void* raw){auto& s=*static_cast<Search*>(raw);bool own=false;
        for(unsigned i=0;i<info->dlpi_phnum;++i){auto& p=info->dlpi_phdr[i];if(p.p_type==PT_LOAD&&s.self>=info->dlpi_addr+p.p_vaddr&&s.self<info->dlpi_addr+p.p_vaddr+p.p_memsz)own=true;}
        if(!own)return 0;
        for(unsigned i=0;i<info->dlpi_phnum;++i){auto& p=info->dlpi_phdr[i];if(p.p_type!=PT_NOTE||p.p_memsz>65536)continue;
            const auto* bytes=reinterpret_cast<const unsigned char*>(info->dlpi_addr+p.p_vaddr);size_t at=0;
            while(at+12<=p.p_memsz){uint32_t names=0,size=0,type=0;memcpy(&names,bytes+at,4);memcpy(&size,bytes+at+4,4);memcpy(&type,bytes+at+8,4);at+=12;
                if(names>p.p_memsz-at)break;size_t desc=at+((size_t(names)+3)&~size_t(3));if(desc>p.p_memsz||size>p.p_memsz-desc)break;
                if(type==3&&names==4&&!memcmp(bytes+at,"GNU",4)&&size>0&&size<=64){constexpr char hex[]="0123456789abcdef";s.build="elf:";for(size_t j=0;j<size;++j){s.build+=hex[bytes[desc+j]>>4];s.build+=hex[bytes[desc+j]&15];}return 1;}
                at=desc+((size_t(size)+3)&~size_t(3));
            }
        }return 1;
    },&search);
    id.module=search.build;return id;
}
// Called only from initialization/maintenance transitions, never from trampolines.
// Cache is PID scoped so a fork does not inherit the parent's identity.
inline void emit(const char* hook,const char* feature,const char* type,const char* phase,const char* stage,
    const char* code,const std::string& generation,const char* detail) noexcept {
    try{
        static thread_local Identity cached;
        if(cached.pid!=getpid())cached=identity();
        if(cached.process!="com.miui.home")return;
        timespec now{};clock_gettime(CLOCK_REALTIME,&now);
        const std::array<std::string,16> fields{hook,feature,type,"com.miui.home",cached.process,
            std::to_string(cached.pid),cached.boot,cached.start,generation,cached.module,"native",
            std::to_string(int64_t(now.tv_sec)*1000+now.tv_nsec/1000000),phase,stage,code,std::string(detail?detail:"").substr(0,1200)};
        std::string out="HCD1|";for(size_t i=0;i<fields.size();++i){if(i)out+='|';out+=b64(fields[i]);}
        __android_log_write(ANDROID_LOG_INFO,"HyperCeiler.HookDiagnostics",out.c_str());
    }catch(...){}
}
inline void counter(const char* id,const char* feature,uint64_t hits,uint64_t applied=0) noexcept {
    try {
        struct Seen {const char* id=nullptr;uint8_t flags=0;};
        static thread_local std::array<Seen,32> seen{};
        auto* record=seen.data();
        for(;record!=seen.data()+seen.size();++record)if(!record->id||!std::strcmp(record->id,id))break;
        if(record==seen.data()+seen.size())return;record->id=id;
        if(hits&&!(record->flags&1)){record->flags|=1;emit(id,feature,"NATIVE_INLINE","EXECUTED","COUNTER","EXECUTED","counter","Existing runtime hit counter is nonzero; final effect unverified");}
        if(applied&&!(record->flags&2)){record->flags|=2;emit(id,feature,"NATIVE_INLINE","APPLIED","COUNTER","INTERNAL_APPLIED","counter","Existing applied counter is nonzero; user-visible effect still requires separate verification");}
    }catch(...){}
}
inline void bank(const char* bank,size_t slot,const DiagnosticEvent& event,const char* feature=nullptr){
    std::string id=event.hook_id?event.hook_id:std::string(bank)+".slot."+std::to_string(slot);
    const char* phase=(event.code==DiagnosticCode::installed||event.code==DiagnosticCode::rearmed)?"INSTALLED":event.code==DiagnosticCode::hook_lost||event.code==DiagnosticCode::foreign_modification?"DEGRADED":"FAILED";
    const char* stage=event.code==DiagnosticCode::validation_failed?"VALIDATION":"INSTALL_HEALTH";
    emit(id.c_str(),feature?feature:bank,"NATIVE_INLINE",phase,stage,diagnostic_code(event.code),
        std::to_string(event.inode)+"/"+std::to_string(event.address)+"/"+std::to_string(event.file_offset),event.detail);
}
}
#endif
