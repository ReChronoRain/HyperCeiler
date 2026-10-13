// SPDX-License-Identifier: Apache-2.0
#include "image.h"
#include "common.h"
#include "nativehook/native_image.h"
#include "nativehook/memory_io.h"
#include <elf.h>
#include <fcntl.h>
#include <link.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/sysmacros.h>
#include <cerrno>
#include <limits>
#include <array>
#include <sstream>
#include <set>

namespace hometweaks {
namespace {
struct MapRow { nhk::FileMapping map; bool readable; };
// One bounded, complete snapshot per discovery. No static mutable inventory or timer.
bool ReadMaps(std::string& text) {
    const int fd=open("/proc/self/maps",O_RDONLY|O_CLOEXEC);
    if(fd<0)return false;
    text.clear();std::array<char,4096> chunk{};bool good=true;
    for(;;){const ssize_t n=read(fd,chunk.data(),chunk.size());
        if(n<0&&errno==EINTR)continue;
        if(n<0){good=false;break;}
        if(n==0)break;
        if(size_t(n)>chunk.size()||size_t(n)>2*1024*1024-text.size()){good=false;break;}
        text.append(chunk.data(),size_t(n));
    }
    close(fd);
    return good&&!text.empty()&&text.back()=='\n';
}
bool MapSnapshot(std::vector<MapRow>& rows) {
    std::string text;if(!ReadMaps(text))return false;
    std::istringstream stream(text);std::string line;size_t count=0;
    while(std::getline(stream,line)){
        if(++count>16384||line.find('\0')!=std::string::npos)return false;
        unsigned long long b=0,e=0,off=0,ino=0;unsigned maj=0,min=0;char perms[5]{};int at=0;
        if(sscanf(line.c_str(),"%llx-%llx %4s %llx %x:%x %llu %n",&b,&e,perms,&off,&maj,&min,&ino,&at)!=7
            ||at<=0||size_t(at)>line.size())return false;
        if(b>=e||e>UINTPTR_MAX||strlen(perms)!=4
            ||(perms[0]!='r'&&perms[0]!='-')||(perms[1]!='w'&&perms[1]!='-')
            ||(perms[2]!='x'&&perms[2]!='-')||(perms[3]!='p'&&perms[3]!='s'))return false;
        std::string path=line.substr(size_t(at));
        if(path.empty()||path[0]!='/')continue;
        rows.push_back({{uintptr_t(b),uintptr_t(e),off,maj,min,ino,perms[2]=='x',perms[1]=='w',path},perms[0]=='r'});
    }
    return !rows.empty();
}
uint64_t Device(const nhk::FileMapping& m){return uint64_t(makedev(m.device_major,m.device_minor));}
bool StatMatches(const Image& image,const struct stat& st) {
    if(!S_ISREG(st.st_mode)||st.st_size<0||uint64_t(st.st_dev)!=image.sourceDevice
        ||uint64_t(st.st_ino)!=image.sourceInode)return false;
    const uint64_t origin=image.fromApkEntry?image.apkEntryOffset:0;
    return origin<=uint64_t(st.st_size)&&image.fileViewBytes<=uint64_t(st.st_size)-origin;
}
// Both linker and fallback use the same verified source and program-header path.
bool MappedImage(const std::vector<MapRow>& inventory,const std::string& path,
    uint64_t origin,uint64_t bytes,bool apk,Image& image) {
    if(path.empty()||path.size()>=sizeof(image.path)||origin>UINT64_MAX-bytes||!bytes)return false;
    Image result{};memcpy(result.path,path.c_str(),path.size()+1);
    result.fromApkEntry=apk;result.apkEntryOffset=origin;result.fileViewBytes=bytes;
    std::vector<nhk::FileMapping> maps;
    for(const auto& row:inventory){const auto& m=row.map;
        if(m.path!=path||m.file_offset<origin||m.file_offset>=origin+bytes)continue;
        if(!row.readable||!m.inode||(m.executable&&m.writable))return false;
        if(!maps.empty()&&(Device(m)!=result.sourceDevice||m.inode!=result.sourceInode))return false;
        result.sourceDevice=Device(m);result.sourceInode=m.inode;maps.push_back(m);
    }
    if(maps.empty()||maps.size()>64)return false;
    Elf64_Ehdr header{};
    if(!ReadImageFile(result,0,&header,sizeof header)
        ||memcmp(header.e_ident,ELFMAG,SELFMAG)||header.e_ident[EI_CLASS]!=ELFCLASS64
        ||header.e_ident[EI_DATA]!=ELFDATA2LSB||header.e_ident[EI_VERSION]!=EV_CURRENT
        ||header.e_version!=EV_CURRENT||header.e_type!=ET_DYN||header.e_machine!=EM_AARCH64
        ||header.e_ehsize!=sizeof header||header.e_phentsize!=sizeof(Elf64_Phdr)
        ||header.e_phnum==0||header.e_phnum>64||header.e_phoff<sizeof header||header.e_phoff>4096)return false;
    const size_t tableEnd=size_t(header.e_phoff)+size_t(header.e_phnum)*sizeof(Elf64_Phdr);
    std::array<std::byte,8192> disk{},live{};
    if(tableEnd>disk.size()||!ReadImageFile(result,0,disk.data(),tableEnd))return false;
    const auto decoded=nhk::elf::parse_program_segments(std::span(disk.data(),tableEnd),true);
    if(!decoded)return false;
    std::vector<nhk::elf::ProgramSegment> loads;bool exec=false;
    const uint64_t page=nhk::host_page_size();
    for(const auto& p:*decoded){
        if(p.type!=PT_LOAD||!p.memsz)continue;
        if(loads.size()>=16||!(p.flags&PF_R)||(p.flags&(PF_W|PF_X))==(PF_W|PF_X)
            ||p.offset>bytes||p.filesz>bytes-p.offset||p.vaddr>UINT32_MAX
            ||p.memsz>UINT32_MAX-p.vaddr||p.offset%page!=p.vaddr%page)return false;
        // Virtual intervals may touch, but may not claim each other's bytes.
        for(const auto& old:loads)if(p.vaddr<old.vaddr+old.memsz&&old.vaddr<p.vaddr+p.memsz)return false;
        loads.push_back(p);exec|=(p.flags&PF_X)!=0&&p.filesz!=0;
    }
    if(loads.empty()||!exec)return false;
    const auto view=nhk::image_view_from_segments_in_window(maps,path,loads,origin,origin+bytes);
    if(!view||view->load_base>UINTPTR_MAX-view->needed_vaddr)return false;
    result.base=view->load_base;
    // Confirm full map extents and effective permissions, not only the first page.
    std::vector<Segment> actual;actual.reserve(maps.size());
    for(const auto& m:maps){bool owned=false;
        for(const auto& p:loads){
            if(!p.filesz)continue;
            const uint64_t relative=m.file_offset-origin;
            const uint64_t fileBegin=nhk::page_down(p.offset),fileEnd=nhk::page_up(p.offset+p.filesz);
            if(relative<fileBegin||relative>=fileEnd)continue;
            const uint64_t va=nhk::page_down(p.vaddr)+relative-fileBegin;
            if(result.base+va!=m.begin||m.end-result.base>nhk::page_up(p.vaddr+p.memsz))continue;
            if(m.executable!=bool(p.flags&PF_X)|| (m.writable&&!(p.flags&PF_W)))continue;
            const uintptr_t begin=std::max(m.begin,result.base+p.vaddr);
            const uintptr_t end=std::min(m.end,result.base+p.vaddr+p.memsz);
            if(begin>=end)continue;
            const uint32_t flags=PF_R|(m.writable?PF_W:0)|(m.executable?PF_X:0);
            actual.push_back({begin,end,flags});owned=true;
        }
        if(!owned)return false;
    }
    std::sort(actual.begin(),actual.end(),
        [](const Segment& a,const Segment& b){return a.begin<b.begin;});
    size_t merged=0;
    for(const Segment& s:actual){
        if(merged&&s.begin<result.segments[merged-1].end)return false;
        if(merged&&s.begin==result.segments[merged-1].end&&s.flags==result.segments[merged-1].flags)
            result.segments[merged-1].end=s.end;
        else {if(merged>=16)return false;result.segments[merged++]=s;}
    }
    result.segmentCount=merged;
    // Every file-backed load byte must be mapped; page padding is not trusted code.
    for(const auto& p:loads){uintptr_t cursor=result.base+p.vaddr,target=cursor+p.filesz;
        for(size_t i=0;i<merged&&cursor<target;++i){const auto& s=result.segments[i];
            if(s.begin>cursor)break;if(s.end>cursor)cursor=std::min(s.end,target);}
        if(cursor!=target)return false;
    }
    bool sameHeader=false;
    for(const auto& m:maps)if(m.file_offset==origin&&m.end-m.begin>=tableEnd
        &&nhk::safe_read(m.begin,std::span(live.data(),tableEnd))
        &&memcmp(disk.data(),live.data(),tableEnd)==0)sameHeader=true;
    if(!sameHeader)return false;
    image=result;return true;
}
struct SearchRequest { const char* basename;std::string path;uintptr_t bias=0;unsigned matches=0; };
int CollectCallback(struct dl_phdr_info* info,size_t,void* opaque) {
    auto& req=*static_cast<SearchRequest*>(opaque);
    if(!info||!info->dlpi_name)return 0;
    const size_t size=strnlen(info->dlpi_name,4096);if(size==4096)return 0;
    const char* slash=strrchr(info->dlpi_name,'/');const char* base=slash?slash+1:info->dlpi_name;
    if(strcmp(base,req.basename))return 0;
    if(++req.matches==1){req.path.assign(info->dlpi_name,size);req.bias=info->dlpi_addr;}
    return 0; // Do not dereference linker phdrs; verified file headers drive resolution.
}
}

bool FindImageByName(const char* basename,Image* out) {
    if(!basename||!*basename||!out)return false;
    SearchRequest req{basename};dl_iterate_phdr(CollectCallback,&req);
    if(req.matches!=1||req.path.size()>=sizeof(out->path))return false;
    std::vector<MapRow> rows;if(!MapSnapshot(rows))return false;
    Image image{};const auto bang=req.path.find('!');
    if(bang!=std::string::npos){
        if(req.path.find('!',bang+1)!=std::string::npos||req.path.compare(bang,2,"!/")!=0)return false;
        const std::string path=req.path.substr(0,bang),entry=req.path.substr(bang+2);
        const auto stored=nhk::zip_stored_entry(path,entry);
        if(!stored||!MappedImage(rows,path,stored->first,stored->second,true,image))return false;
    }else{
        struct stat st{};const int fd=open(req.path.c_str(),O_RDONLY|O_CLOEXEC);
        if(fd<0)return false;const bool good=fstat(fd,&st)==0&&S_ISREG(st.st_mode)&&st.st_size>0;close(fd);
        if(!good||!MappedImage(rows,req.path,0,uint64_t(st.st_size),false,image))return false;
    }
    if(image.base!=req.bias)return false;
    *out=image;return true;
}
bool FindImageFromApkMaps(const char* apkMarker,const char* libName,Image* out) {
    if(!apkMarker||!*apkMarker||!libName||!*libName||!out)return false;
    std::vector<MapRow> rows;if(!MapSnapshot(rows))return false;
    std::set<std::string> paths;
    for(const auto& row:rows)if(row.map.path.find(apkMarker)!=std::string::npos){
        paths.insert(row.map.path);if(paths.size()>64)return false;}
    Image chosen{};unsigned found=0;
    for(const auto& path:paths){const auto stored=nhk::zip_stored_entry(path,libName);if(!stored)continue;
        Image image{};
        if(MappedImage(rows,path,stored->first,stored->second,true,image)){
            if(++found>1)return false;chosen=image;
        }
    }
    if(found!=1)return false;
    *out=chosen;return true;
}
bool RangeInImage(const Image& image,uintptr_t address,size_t length) {
    if(!length||image.segmentCount==0||image.segmentCount>16)return false;
    for(size_t i=0;i<image.segmentCount;++i){const Segment& s=image.segments[i];
        if(!(s.flags&PF_R)||s.begin>=s.end)continue;
        if(address>=s.begin&&address<s.end&&length<=s.end-address)return true;
    }
    return false;
}
bool ReadImageFile(const Image& image,uint64_t offset,void* out,size_t length) {
    if(!out||!length||!image.path[0]||!memchr(image.path,'\0',sizeof(image.path))
        ||!image.sourceInode||!image.fileViewBytes||offset>image.fileViewBytes
        ||uint64_t(length)>image.fileViewBytes-offset)return false;
    const uint64_t origin=image.fromApkEntry?image.apkEntryOffset:0;
    const uint64_t maxOffset=uint64_t(std::numeric_limits<off_t>::max());
    if(origin>maxOffset||offset>maxOffset-origin)return false;
    const uint64_t fileOffset=origin+offset;
    if(uint64_t(length)>maxOffset-fileOffset)return false;
    const int fd=open(image.path,O_RDONLY|O_CLOEXEC);if(fd<0)return false;
    struct stat st{};bool ok=fstat(fd,&st)==0&&StatMatches(image,st);
    auto* dst=static_cast<uint8_t*>(out);size_t done=0;
    while(ok&&done<length){const ssize_t n=pread(fd,dst+done,length-done,off_t(fileOffset+done));
        if(n<0&&errno==EINTR)continue;
        if(n<=0||size_t(n)>length-done){ok=false;break;}
        done+=size_t(n);
    }
    close(fd);return ok;
}

namespace {

struct DumpRequest {
    int maxEntries;
    int seen;
};

int DumpCallback(struct dl_phdr_info* info, size_t, void* opaque) {
    auto* req = static_cast<DumpRequest*>(opaque);
    if (info == nullptr) return 0;
    if (req->seen >= req->maxEntries) return 1;
    ++req->seen;
    const char* name = info->dlpi_name != nullptr ? info->dlpi_name : "(空)";
    int loads = 0;
    for (size_t i = 0; info->dlpi_phdr != nullptr && i < std::min<size_t>(info->dlpi_phnum,64); ++i) {
        Elf64_Phdr ph{};
        const uintptr_t base=reinterpret_cast<uintptr_t>(info->dlpi_phdr);
        const size_t off=i*sizeof(ph);
        if(base>UINTPTR_MAX-off||!nhk::safe_read(base+off,
            std::span(reinterpret_cast<std::byte*>(&ph),sizeof(ph))))break;
        if (ph.p_type == PT_LOAD) ++loads;
    }
    LOGI("  [phdr] base=%#lx PT_LOAD=%d name=%s",
         static_cast<unsigned long>(info->dlpi_addr), loads, name);
    return 0;
}

}

void DumpLoadedLibraries(int maxEntries) {
    DumpRequest req{maxEntries, 0};
    LOGI("dl_iterate_phdr 可见的库（最多 %d 条）：", maxEntries);
    dl_iterate_phdr(DumpCallback, &req);
    LOGI("dl_iterate_phdr 遍历结束，共 %d 条", req.seen);
}

void DumpApkMaps(int maxEntries) {
    std::vector<MapRow> rows;if(!MapSnapshot(rows))return;
    int shown=0;for(const auto& row:rows){if(shown>=maxEntries)break;
        if(row.map.path.find(".apk")==std::string::npos)continue;
        LOGI("  [maps] %#lx-%#lx off=%#llx %s",(unsigned long)row.map.begin,
            (unsigned long)row.map.end,(unsigned long long)row.map.file_offset,row.map.path.c_str());++shown;}
}

void DumpImage(const Image& image) {
    LOGI("镜像 base=%#lx 段数=%zu path=%s", static_cast<unsigned long>(image.base),
         image.segmentCount, image.path);
    for (size_t i = 0; i < std::min<size_t>(image.segmentCount,16); ++i) {
        const Segment& s = image.segments[i];
        LOGI("  段%zu %#lx - %#lx flags=%#x 大小=%#lx", i,
             static_cast<unsigned long>(s.begin), static_cast<unsigned long>(s.end), s.flags,
             static_cast<unsigned long>(s.end - s.begin));
    }
}

}
