/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_recent_cards_sdk.h"
#include <cerrno>
#include <elf.h>
#include <link.h>
#include <sys/syscall.h>
#include <sys/uio.h>
#include <unistd.h>

namespace home_layout::cards::sdk {
// No procfs/file compatibility fallback: this same reader is used by the
// synchronous query callback. SDK native heap pointers may carry Android's
// AArch64 top-byte malloc tag; retain it in owned values, strip it only for the
// kernel's remote-iovec address. This is unrelated to Dart's +1 object tag.
inline bool read_live(uintptr_t address,std::span<std::byte> output) {
    if(output.empty())return true;
    if(!address||address>UINTPTR_MAX-output.size())return false;
#if defined(__aarch64__)
    address&=UINT64_C(0x00ffffffffffffff);
#endif
    iovec local{output.data(),output.size()},remote{reinterpret_cast<void*>(address),output.size()};
    ssize_t count;
    do{count=syscall(__NR_process_vm_readv,getpid(),&local,1,&remote,1,0);}while(count<0&&errno==EINTR);
    return count==static_cast<ssize_t>(output.size());
}
struct LiveImage {
    uintptr_t base=0,program_headers=0;
    uint16_t program_count=0;
    std::string path;
    std::vector<std::byte> file;
};
inline std::optional<LiveImage> snapshot_live(const dl_phdr_info& info,std::string_view* reason=nullptr) {
    // The linker holds its inventory lock for this callback. The parser runs
    // later on owned snapshots, never while holding that lock or on a frame.
    if(reason)*reason="linker headers";
    if(!info.dlpi_addr||!info.dlpi_name||!info.dlpi_phnum||info.dlpi_phnum>nhk::elf::kMaxProgramHeaders)return {};
    uint64_t file_end=0,memory_end=0;
    bool header=false;
    for(size_t i=0;i<info.dlpi_phnum;++i) {
        const auto& p=info.dlpi_phdr[i];if(p.p_type!=PT_LOAD)continue;
        if(p.p_filesz>p.p_memsz||p.p_offset>UINT64_MAX-p.p_filesz||p.p_vaddr>UINT64_MAX-p.p_memsz)return {};
        file_end=std::max<uint64_t>(file_end,p.p_offset+p.p_filesz);memory_end=std::max<uint64_t>(memory_end,p.p_vaddr+p.p_memsz);
        if(!p.p_vaddr&&!p.p_offset&&p.p_filesz>=64)header=true;
    }
    if(reason)*reason="load segment bounds";
    if(!header||file_end<64||file_end>64u*1024*1024||memory_end>64u*1024*1024
        ||info.dlpi_addr>UINTPTR_MAX-memory_end)return {};
    LiveImage out{info.dlpi_addr,reinterpret_cast<uintptr_t>(info.dlpi_phdr),info.dlpi_phnum,info.dlpi_name,
        std::vector<std::byte>(static_cast<size_t>(file_end))};
    if(reason)*reason="whole process_vm_readv";
    for(size_t i=0;i<info.dlpi_phnum;++i) {
        const auto& p=info.dlpi_phdr[i];if(p.p_type!=PT_LOAD||!p.p_filesz)continue;
        if(!read_live(info.dlpi_addr+p.p_vaddr,std::span(out.file).subspan(p.p_offset,p.p_filesz)))return {};
    }
    if(reason)*reason="ELF/linker header agreement";
    const auto segments=nhk::elf::parse_program_segments(out.file);if(!segments)return {};
    if(nhk::load_le16(out.file.data()+56)!=info.dlpi_phnum
        ||nhk::load_le64(out.file.data()+32)>UINTPTR_MAX-info.dlpi_addr
        ||info.dlpi_addr+nhk::load_le64(out.file.data()+32)!=out.program_headers)return {};
    // Section tables are not loaded ELF metadata. Do not pretend zero-filled
    // unmapped file tails are a usable section view.
    std::fill(out.file.begin()+40,out.file.begin()+48,std::byte{});
    std::fill(out.file.begin()+58,out.file.begin()+64,std::byte{});
    return out;
}
inline std::optional<std::vector<LiveImage>> capture_live(std::span<const std::string_view> names,std::string* reason=nullptr) {
    // Bounded analysis inventory, not a vendor ABI limit. Surface Parcelable
    // ownership additionally spans schema/UI public and private images.
    if(names.empty()||names.size()>16){if(reason)*reason="invalid capture inventory";return {};}
    struct Inventory {std::span<const std::string_view> names;std::vector<unsigned> count;} inventory{names,std::vector<unsigned>(names.size())};
    dl_iterate_phdr([](dl_phdr_info* info,size_t,void* opaque){
        auto& state=*static_cast<Inventory*>(opaque);const std::string_view path=info->dlpi_name?info->dlpi_name:"";
        const auto slash=path.find_last_of('/');const auto name=path.substr(slash==std::string_view::npos?0:slash+1);
        for(size_t i=0;i<state.names.size();++i)if(name==state.names[i])++state.count[i];return 0;
    },&inventory);
    for(size_t i=0;i<names.size();++i)if(inventory.count[i]!=1){
        if(reason)*reason=std::string(names[i])+(inventory.count[i]?": duplicate mapping":": not loaded");return {};
    }
    struct Capture {
        std::span<const std::string_view> names;
        std::vector<std::optional<LiveImage>> images;
        bool valid=true;
        std::string* reason=nullptr;
    } capture{names,std::vector<std::optional<LiveImage>>(names.size()),true,reason};
    dl_iterate_phdr([](dl_phdr_info* info,size_t,void* opaque) {
        auto& state=*static_cast<Capture*>(opaque);
        const std::string_view path=info->dlpi_name?info->dlpi_name:"";
        const auto slash=path.find_last_of('/');const auto name=path.substr(slash==std::string_view::npos?0:slash+1);
        for(size_t i=0;i<state.names.size();++i)if(name==state.names[i]) {
            if(state.images[i]){if(state.reason)*state.reason=std::string(name)+": duplicate mapping";state.valid=false;return 1;}
            std::string_view stage;auto image=snapshot_live(*info,&stage);
            if(!image){if(state.reason)*state.reason=std::string(name)+": "+std::string(stage);state.valid=false;return 1;}state.images[i]=std::move(image);
        }
        return 0;
    },&capture);
    if(!capture.valid)return {};
    std::vector<LiveImage> result;result.reserve(names.size());
    for(size_t i=0;i<capture.images.size();++i){auto& image=capture.images[i];
        if(!image){if(reason)*reason=std::string(names[i])+": not loaded";return {};}result.push_back(std::move(*image));}
    return result;
}
inline bool images_current(std::span<const LiveImage> images) {
    struct Check {std::span<const LiveImage> images;std::vector<unsigned> count;bool valid=true;};
    Check check{images,std::vector<unsigned>(images.size()),true};
    dl_iterate_phdr([](dl_phdr_info* info,size_t,void* opaque) {
        auto& state=*static_cast<Check*>(opaque);
        for(size_t i=0;i<state.images.size();++i)if(info->dlpi_name&&state.images[i].path==info->dlpi_name) {
            ++state.count[i];state.valid&=state.images[i].base==info->dlpi_addr
                &&state.images[i].program_headers==reinterpret_cast<uintptr_t>(info->dlpi_phdr)
                &&state.images[i].program_count==info->dlpi_phnum;
        }
        return 0;
    },&check);
    return check.valid&&std::all_of(check.count.begin(),check.count.end(),[](unsigned count){return count==1;});
}
}
