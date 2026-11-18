/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_layout_elf_targets.h"
#include <array>
#include <memory>
#include <map>

namespace home_layout::cards::sdk {
struct Address {
    size_t image=0;uint64_t va=0;
    bool operator==(const Address&)const=default;
};
struct Body {
    Address address;
    std::vector<uint32_t> words;
};

// File snapshots, mapped by PT_LOAD rather than section headers. Moving an
// Image preserves the backing vector allocation used by the metadata view.
class Image {
    std::vector<std::byte> mapped_;
    nhk::elf::ElfImage metadata_;
    std::span<const std::byte> unwind_;
    uint64_t unwind_va_=0;
    Image(std::vector<std::byte> mapped,nhk::elf::ElfImage metadata,uint64_t unwind_va,size_t unwind_size)
        :mapped_(std::move(mapped)),metadata_(std::move(metadata)),
        unwind_(mapped_.data()+unwind_va,unwind_size),unwind_va_(unwind_va){}
public:
    Image(const Image&)=delete;
    Image& operator=(const Image&)=delete;
    Image(Image&&)=default;
    Image& operator=(Image&&)=default;
    static std::optional<Image> parse(std::span<const std::byte> bytes,uintptr_t load_base=0) {
        const auto file=elf_targets::parse(bytes);if(!file)return {};
        uint64_t end=0;
        for(const auto& p:file->segments)if(p.type==nhk::elf::kProgramTypeLoad)end=std::max(end,p.vaddr+p.memsz);
        if(!end||end>64u*1024*1024)return {};
        std::vector<std::byte> mapped(static_cast<size_t>(end));
        for(const auto& p:file->segments)if(p.type==nhk::elf::kProgramTypeLoad) {
            if(!nhk::in_image(bytes.size(),p.offset,p.filesz))return {};
            std::memcpy(mapped.data()+p.vaddr,bytes.data()+p.offset,static_cast<size_t>(p.filesz));
        }
        const auto metadata=nhk::elf::ElfImage::make(load_base,mapped);if(!metadata)return {};
        if(!nhk::in_image(mapped.size(),file->unwind_vaddr,file->unwind_header.size()))return {};
        return Image(std::move(mapped),*metadata,file->unwind_vaddr,file->unwind_header.size());
    }
    const nhk::elf::ElfImage& metadata()const{return metadata_;}
    std::optional<uint32_t> word(uint64_t va)const {
        if(!metadata_.contains(va,4,nhk::elf::kFlagExecute))return {};
        return nhk::load_le32(mapped_.data()+va);
    }
    std::optional<Body> body(Address at)const {
        const auto bounds=unwind::lookup(unwind_,unwind_va_,at.va);
        if(!bounds||bounds->begin!=at.va||bounds->end<=at.va||bounds->end-at.va>0x10000)return {};
        Body out{at,{}};
        for(uint64_t va=at.va;va<bounds->end;va+=4) {
            const auto w=word(va);if(!w)return {};out.words.push_back(*w);
        }
        return out;
    }
};

class Graph {
    std::vector<Image> images_;
    // One setup worker owns a Graph. Callbacks receive only the resolved value
    // contract, never this parser or its memoized linkage table.
    mutable std::map<std::pair<size_t,uint64_t>,std::optional<Address>> linkage_;
    mutable std::map<std::pair<size_t,uint64_t>,std::optional<std::string_view>> imports_;
public:
    explicit Graph(std::vector<Image> images):images_(std::move(images)){}
    const nhk::elf::ElfImage* metadata(size_t index)const {
        return index<images_.size()?&images_[index].metadata():nullptr;
    }
    bool readable(Address at,size_t size)const {
        return at.image<images_.size()&&images_[at.image].metadata().contains(at.va,size,nhk::elf::kFlagRead);
    }
    std::optional<Address> data_reference(Address slot)const {
        if(slot.image>=images_.size())return {};
        const auto reference=images_[slot.image].metadata().data_reference(slot.va);if(!reference)return {};
        return reference->rva?std::optional<Address>{{slot.image,reference->rva}}:exported(reference->external);
    }
    std::optional<Address> exported(std::string_view name)const {
        std::optional<Address> found;
        for(size_t i=0;i<images_.size();++i) {
            const auto symbol=images_[i].metadata().find_symbol(name);
            if(!symbol||!symbol->rva||!images_[i].word(symbol->rva))continue;
            if(found)return {};found=Address{i,symbol->rva};
        }
        return found;
    }
    std::optional<Body> body(Address at)const {
        return at.image<images_.size()?images_[at.image].body(at):std::nullopt;
    }
    // Some non-throwing drop adapters have no unwind record. Accept only a
    // bounded straight-line leaf reached by a verified SDK call, ending in a
    // tail branch or return. Other functions still require their unwind span.
    std::optional<Body> leaf(Address at)const {
        if(at.image>=images_.size())return {};
        Body out{at,{}};
        for(size_t i=0;i<32;++i) {
            const auto w=images_[at.image].word(at.va+i*4);if(!w)return {};
            out.words.push_back(*w);
            if(*w==0xd65f03c0u||(*w&0xfc000000u)==0x14000000u)return out;
            if((*w&0xfc000000u)==0x94000000u||(*w&0xff000010u)==0x54000000u
                ||(*w&0x7e000000u)==0x34000000u||(*w&0x7e000000u)==0x36000000u
                ||(*w&0xfffffc1fu)==0xd61f0000u)return {};
        }
        return {};
    }
    // Follow the ELF's linkage. Only the standard AArch64 PLT sequence is
    // accepted; a new linker thunk requires a new semantic decoder, not a name
    // or address fallback. X16/X17 are the platform ABI PLT scratch registers.
    std::optional<std::string_view> resolve_import_name(Address at)const {
        if(at.image>=images_.size())return {};
        const auto& image=images_[at.image];
        std::array<uint32_t,4> code{};
        for(size_t i=0;i<code.size();++i){auto w=image.word(at.va+i*4);if(!w)return {};code[i]=*w;}
        if((code[0]&0x9f00001fu)!=0x90000010u)return {};
        if((code[1]&0xffc003ffu)!=0xf9400211u||(code[2]&0xffc003ffu)!=0x91000210u
            ||code[3]!=0xd61f0220u)return {};
        const uint64_t load=((code[1]>>10)&4095)*8,add=(code[2]>>10)&4095;
        if(load!=add)return {};
        const std::array<uint32_t,2> pair{code[0],code[2]};
        const auto slot=nhk::arm64::decode_address_pair(pair,0,at.va);
        if(!slot)return {};
        return image.metadata().import_name(*slot);
    }
    std::optional<std::string_view> import_name(Address at)const {
        const auto key=std::pair{at.image,at.va};
        if(auto cached=imports_.find(key);cached!=imports_.end())return cached->second;
        const auto resolved=resolve_import_name(at);imports_.emplace(key,resolved);return resolved;
    }
    std::optional<Address> linked(Address at)const {
        if(at.image>=images_.size())return {};
        const auto word=images_[at.image].word(at.va);if(!word)return {};
        if((*word&0x9f00001fu)!=0x90000010u)return at;
        const auto name=import_name(at);return name?exported(*name):std::nullopt;
    }
    std::optional<Address> raw_callee(const Body& body,size_t index,bool tail=false)const {
        if(index>=body.words.size())return {};
        const auto w=body.words[index];
        if((w&0xfc000000u)!=(tail?0x14000000u:0x94000000u))return {};
        const int64_t delta=int64_t(int32_t(w<<6)>>6)*4;
        const int64_t va=int64_t(body.address.va+index*4)+delta;
        if(va<0)return {};
        return Address{body.address.image,static_cast<uint64_t>(va)};
    }
    std::optional<Address> callee(const Body& body,size_t index,bool tail=false)const {
        const auto target=raw_callee(body,index,tail);if(!target)return {};
        const auto key=std::pair{target->image,target->va};
        if(auto cached=linkage_.find(key);cached!=linkage_.end())return cached->second;
        const auto resolved=linked({key.first,key.second});linkage_.emplace(key,resolved);return resolved;
    }
    std::vector<Address> calls(const Body& body,bool tails=false)const {
        std::vector<Address> result;
        for(size_t i=0;i<body.words.size();++i) {
            const auto w=body.words[i];
            if((w&0xfc000000u)==0x94000000u||(tails&&(w&0xfc000000u)==0x14000000u)) {
                const auto call=callee(body,i,(w&0xfc000000u)==0x14000000u);
                if(call)result.push_back(*call);
            }
        }
        return result;
    }
};
}
