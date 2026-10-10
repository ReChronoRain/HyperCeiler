// Private ROM loader reused in this new fixture; no production or frozen test edits.
#pragma once
#include <algorithm>
#include <array>
#include <cassert>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <elf.h>
#include <fstream>
#include <iostream>
#include <string>
#include <sys/mman.h>
#include <vector>
extern void expect(bool,const char*);
struct Image{
 uint8_t* base;size_t size,veneer;std::vector<uint8_t> file;std::vector<std::pair<size_t,std::vector<uint8_t>>> original_text;std::vector<std::pair<size_t,size_t>> patches;
 explicit Image(const char* path){std::ifstream f(path,std::ios::binary);file.assign(std::istreambuf_iterator<char>(f),{});assert(!file.empty());
 const auto& h=*reinterpret_cast<const Elf64_Ehdr*>(file.data());assert(h.e_machine==EM_AARCH64&&h.e_type==ET_DYN);const auto* p=reinterpret_cast<const Elf64_Phdr*>(file.data()+h.e_phoff);size=0;
 for(unsigned j=0;j<h.e_phnum;j++)if(p[j].p_type==PT_LOAD)size=std::max(size,size_t(p[j].p_vaddr+p[j].p_memsz));
 size=(size+4095)&~size_t(4095);veneer=size;size+=65536;
 base=static_cast<uint8_t*>(mmap(nullptr,size,7,MAP_PRIVATE|MAP_ANONYMOUS,-1,0));assert(base!=MAP_FAILED);
 for(unsigned j=0;j<h.e_phnum;j++)if(p[j].p_type==PT_LOAD){assert(p[j].p_offset+p[j].p_filesz<=file.size());memcpy(base+p[j].p_vaddr,file.data()+p[j].p_offset,p[j].p_filesz);}
 for(unsigned j=0;j<h.e_phnum;j++)if(p[j].p_type==PT_LOAD&&(p[j].p_flags&PF_X)){original_text.push_back({p[j].p_vaddr,std::vector<uint8_t>(base+p[j].p_vaddr,base+p[j].p_vaddr+p[j].p_filesz)});}
 const auto* s=reinterpret_cast<const Elf64_Shdr*>(file.data()+h.e_shoff);auto* names=reinterpret_cast<const char*>(file.data()+s[h.e_shstrndx].sh_offset);
 for(unsigned j=0;j<h.e_shnum;j++)if(!strcmp(names+s[j].sh_name,".rela.dyn")){auto*r=reinterpret_cast<const Elf64_Rela*>(file.data()+s[j].sh_offset);for(size_t k=0;k<s[j].sh_size/sizeof(*r);k++)if(ELF64_R_TYPE(r[k].r_info)==R_AARCH64_RELATIVE)at<uintptr_t>(r[k].r_offset)=uintptr_t(base)+r[k].r_addend;}}
 ~Image(){munmap(base,size);} template<class T>T&at(size_t va){assert(va+sizeof(T)<=size);return *reinterpret_cast<T*>(base+va);}template<class F>F fn(size_t va){return reinterpret_cast<F>(base+va);}
 void word(size_t va,uint32_t value){memcpy(base+va,&value,4);patches.push_back({va,4});}void jump(size_t va,uintptr_t to){word(va,0x58000050);word(va+4,0xd61f0200);memcpy(base+va+8,&to,8);patches.push_back({va+8,8});}
 void small_jump(size_t va,uintptr_t to){auto v=veneer;veneer+=16;jump(v,to);const int64_t delta=int64_t(v)-int64_t(va);assert(delta%4==0&&delta>=-0x8000000&&delta<0x8000000);word(va,0x14000000|(uint32_t(delta/4)&0x3ffffff));}
 void intact(){for(const auto&[va,bytes]:original_text){for(size_t j=0;j<bytes.size();j++){bool edited=false;for(auto [p,n]:patches)if(va+j>=p&&va+j<p+n){edited=true;break;}if(!edited&&base[va+j]!=bytes[j])expect(false,"non-adapter executable byte changed");}}expect(true,"all original executable bytes outside declared fixture patches retained");}
 void sync(){__builtin___clear_cache(reinterpret_cast<char*>(base),reinterpret_cast<char*>(base+size));}
};
