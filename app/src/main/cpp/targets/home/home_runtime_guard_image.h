/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "nativehook/native_image.h"
#include "nativehook/elf_image.h"
#include "nativehook/memory_read.h"
#include <array>

namespace home_runtime {
struct ImportBinding {
    uint64_t rva;
    uintptr_t address;
    nhk::CodeSource source; // May be anonymous ONLY with an owned live PLT proof.
    uintptr_t plt_address;
    nhk::CodeSource plt_source;
    std::array<uint32_t,4> plt_words;
    std::vector<nhk::ReadRegion> regions;
};
struct ImportImage {
    const nhk::elf::ElfImage* image;
    std::vector<ImportBinding> bindings;
    auto collect_slots(std::string_view name) const {
        return name == "madvise" ? image->collect_slots(name)
                                : std::vector<nhk::elf::GotSlot>{};
    }
    uintptr_t runtime(uint64_t rva) const {
        for (const auto& slot : bindings) if (slot.rva == rva) return slot.address;
        return 0;
    }
};
inline std::optional<uintptr_t> plt_target(const std::array<uint32_t,4>& words, uintptr_t pc) {
    if ((words[0]&0x9f00001fU)!=0x90000010U || (words[1]&0xffc003ffU)!=0xf9400211U
        || (words[2]&0xffc003ffU)!=0x91000210U || words[3]!=0xd61f0220U) return {};
    const uint64_t low=((words[1]>>10)&0xfffU)*8U;
    if (((words[2]>>10)&0xfffU)!=low) return {};
    int64_t pages=((words[0]>>5)&0x7ffffU)*4U+((words[0]>>29)&3U);
    if (pages&(1<<20)) pages-=1<<21;
    const uintptr_t page=pc&~uintptr_t(4095);
    if (pages<0 && uint64_t(-pages)*4096>page) return {};
    if (pages>=0 && nhk::add_overflows(page,uint64_t(pages)*4096)) return {};
    const uintptr_t base=pages<0?page-uint64_t(-pages)*4096:page+uint64_t(pages)*4096;
    if (nhk::add_overflows(base,low)) return {};
    return base+low;
}
// Metadata alone is insufficient: a shared RELRO template and private relocated
// data can coexist. Follow the exact imported symbol's LIVE PLT reference. An
// anonymous candidate is accepted only at the original data-segment coordinate
// referenced by that validated executable stub, with before/after VMA proof.
template<class Reader>
std::optional<ImportImage> bind_imports(const nhk::elf::ElfImage& image,
    std::span<const std::byte> snapshot, const std::vector<nhk::FileMapping>& maps,
    const std::string& maps_text, std::span<const nhk::elf::ProgramSegment> segments,
    std::string_view path, uint64_t view_begin, uint64_t view_end,
    const nhk::FileMapping& header, Reader read) {
    if (header.file_offset!=view_begin || header.writable || header.inode==0
        || nhk::strip_deleted(header.path)!=path) return {};
    std::vector<nhk::ExecutableMapping> code;
    for (const auto& map:maps) {
        if (nhk::strip_deleted(map.path)!=path || !map.executable
            || map.file_offset<view_begin || map.file_offset>=view_end) continue;
        if (map.inode!=header.inode || map.device_major!=header.device_major
            || map.device_minor!=header.device_minor) return {};
        const uint64_t offset=map.file_offset-view_begin;bool owned=false;
        for (const auto& segment:segments) {
            if (segment.type!=1 || !(segment.flags&1)) continue;
            const auto first=nhk::page_down(segment.offset),last=nhk::page_up(segment.offset+segment.filesz);
            if (offset<first || offset>=last) continue;
            const uint64_t va=nhk::page_down(segment.vaddr)+offset-first;
            if (!nhk::add_overflows(header.begin,va) && header.begin+va==map.begin
                && map.end>map.begin && map.end-map.begin<=last-offset) owned=true;
        }
        if (!owned) return {};
        code.push_back({map.begin,map.end,map.file_offset,map.device_major,map.device_minor,map.inode});
    }
    if (code.empty()) return {};
    const auto imports=image.collect_slots("madvise");
    if (imports.empty() || imports.size()>8) return {};
    ImportImage result{&image,{}};
    for (const auto& slot:imports) {
        bool data=false;
        for (const auto& segment:segments)
            if (segment.type==1 && !(segment.flags&1) && slot.rva>=segment.vaddr
                && slot.rva-segment.vaddr<=segment.filesz
                && sizeof(void*)<=segment.filesz-(slot.rva-segment.vaddr)) data=true;
        if (!data) return {};
        std::optional<ImportBinding> chosen;
        for (const auto& segment:segments) {
            if (segment.type!=1 || !(segment.flags&1) || segment.vaddr>snapshot.size()
                || segment.filesz>snapshot.size()-segment.vaddr) continue;
            for (uint64_t delta=0;delta+16<=segment.filesz;delta+=4) {
                const uint64_t va=segment.vaddr+delta;
                std::array<uint32_t,4> original{};
                std::memcpy(original.data(),snapshot.data()+va,16);
                if (plt_target(original,va)!=std::optional<uintptr_t>{slot.rva}) continue;
                if (chosen || nhk::add_overflows(header.begin,va)) return {};
                const uintptr_t pc=header.begin+va;
                const auto source=nhk::source_at(code,pc,16);
                std::array<uint32_t,4> live{};
                if (!source || !read(pc,live) || live[1]!=original[1]
                    || live[2]!=original[2] || live[3]!=original[3]) return {};
                const auto address=plt_target(live,pc);
                if (!address || *address%alignof(void*)) return {};
                std::vector<nhk::ReadRegion> regions;
                if (!nhk::ParseReadableRanges(maps_text,*address,sizeof(void*),regions)
                    || regions.size()!=1 || (regions[0].permissions!="rw-p" && regions[0].permissions!="r--p")) return {};
                const auto& region=regions[0];
                if (region.inode==0) {
                    if (region.major || region.minor || region.offset
                        || nhk::add_overflows(header.begin,slot.rva)
                        || *address!=header.begin+slot.rva) return {};
                } else if (region.inode!=header.inode || region.major!=header.device_major
                    || region.minor!=header.device_minor) return {};
                if (nhk::add_overflows(region.offset,*address-region.begin)) return {};
                chosen=ImportBinding{slot.rva,*address,
                    {region.major,region.minor,region.inode,region.offset+*address-region.begin},
                    pc,*source,live,std::move(regions)};
            }
        }
        if (!chosen) return {};
        for (const auto& old:result.bindings) if (old.address==chosen->address) return {};
        result.bindings.push_back(std::move(*chosen));
    }
    return result;
}
} // namespace home_runtime
