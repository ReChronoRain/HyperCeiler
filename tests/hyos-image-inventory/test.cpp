#include <atomic>
#include <cstdio>
#include <cstring>
#include <fcntl.h>
#include <fstream>
#include <iostream>
#include <map>
#include <sstream>
#include <sys/stat.h>
#include <unistd.h>
#include "nativehook/native_image.h"
#include "nativehook/memory_read.h"

static std::string mapsText;
static std::map<uintptr_t,std::vector<std::byte>> headers;
static bool fault=false;
static int fixture_open(const char*path,int flags) {
    if(std::strcmp(path,"/proc/self/maps"))return ::open(path,flags);
    FILE*file=tmpfile();if(!file)return -1;
    if(fwrite(mapsText.data(),1,mapsText.size(),file)!=mapsText.size()){fclose(file);return -1;}
    fflush(file);rewind(file);int fd=dup(fileno(file));fclose(file);return fd;
}
namespace nhk {
bool safe_read(uintptr_t at,std::span<std::byte> out){
    if(fault)return false;
    auto i=headers.find(at);if(i==headers.end()||out.size()>i->second.size())return false;
    std::memcpy(out.data(),i->second.data(),out.size());return true;
}
}
#define open fixture_open
#include "targets/home/tweaks/image.cpp"
#undef open

int main(int argc,char**argv){
    if(argc!=3)return 2;
    const std::string dir=argv[1],apk=dir+"/home-"+argv[2]+".apk",second=dir+"/second-"+argv[2]+".apk";
    const uint64_t page=std::stoull(argv[2]),base=0x7100000000ULL;
    int count=0,failed=0;
    auto check=[&](bool value,const char*name){++count;if(!value){++failed;std::cerr<<"FAIL "<<name<<'\n';}};
    auto rows=[&](const std::string&path,uint64_t bias,bool split){
        const auto stored=nhk::zip_stored_entry(path,"lib/arm64-v8a/libapp.so");
        if(!stored)throw std::runtime_error("missing fixture entry");
        struct stat metadata{};if(stat(path.c_str(),&metadata))throw std::runtime_error("missing fixture file");
        headers[bias].resize(page);std::ifstream file(path,std::ios::binary);file.seekg(stored->first);
        file.read(reinterpret_cast<char*>(headers[bias].data()),page);
        std::ostringstream output;
        auto row=[&](uint64_t offset,uint64_t bytes,const char*perms){
            output<<std::hex<<bias+offset<<'-'<<bias+offset+bytes<<' '<<perms<<' '<<stored->first+offset
                <<' '<<major(metadata.st_dev)<<':'<<minor(metadata.st_dev)<<' '<<std::dec<<metadata.st_ino<<' '<<path<<'\n';
        };
        row(0,page,"r--p");
        if(split)for(unsigned i=1;i<=128;++i)row(i*page,page,"r-xp");
        else row(page,128*page,"r-xp");
        row(129*page,page,"rw-p");return output.str();
    };
    auto anonymous=[&](size_t n){std::ostringstream out;
        for(size_t i=0;i<n;++i)out<<std::hex<<0x100000+i*page<<'-'<<0x100000+(i+1)*page
            <<" r--p 0 00:00 0 [anon:hyos-preloader-"<<std::string(100,'x')<<"]\n";
        return out.str();
    };
    auto candidates=[&](size_t n){std::ostringstream out;
        for(size_t i=0;i<n;++i)out<<std::hex<<0x7300000000ULL+i*page<<'-'<<0x7300000000ULL+(i+1)*page
            <<" r-xp 0 01:01 123 "<<dir<<"/other-"<<std::dec<<i<<".apk\n";
        return out.str();
    };
    hometweaks::Image image{};hometweaks::ImageDiscoveryDiagnostic diagnostic;
    auto discover=[&]{return hometweaks::FindImageFromMappedApks("libapp.so",&image,&diagnostic);};
    auto reset=[&]{fault=false;mapsText=rows(apk,base,false);image={};image.base=0x55;};
    reset();check(discover()&&image.base==base,"minimal verified image");
    reset();mapsText=rows(apk,base,true);
    check(discover()&&image.base==base&&image.segmentCount==3,"130 split VMAs coalesce into three verified segments");
    check(diagnostic.imageMappings==130,"raw mapping count retained for diagnosis");
    reset();mapsText+=candidates(80);
    check(discover()&&diagnostic.apkCandidates==81,"81 APK candidates do not reject the only live ELF");
    reset();mapsText=anonymous(20000)+rows(apk,base,true)+candidates(80);
    check(mapsText.size()>2*1024*1024,"inherited inventory exceeds old byte and line budgets");
    check(discover()&&image.base==base,"combined inherited inventory and fragmented image accepted");
    std::istringstream all(mapsText);const auto parsed=nhk::parse_file_mappings(all);
    check(!parsed.empty()&&parsed.front().path==apk,"layout parser reaches the launcher after 20000 inherited rows");
    std::vector<nhk::ReadRegion> readable;
    check(nhk::ParseReadableRanges(mapsText,base,64,readable),"safe-read fallback accepts the same complete inventory");
    reset();mapsText+=rows(second,base+0x100000000ULL,false);
    check(!discover()&&std::string(diagnostic.reason)=="ambiguous-images"&&image.base==0x55,"two full live images remain ambiguous");
    reset();mapsText=rows(apk,base,true);auto pos=mapsText.find("r-xp");mapsText.replace(pos,4,"rwxp");
    check(!discover()&&image.base==0x55,"writable executable page refused");
    reset();fault=true;
    check(!discover()&&std::string(diagnostic.reason)=="image-live-header","faulting live header refuses attribution");
    // The second APK contains libapp.so but its only mapped offset is outside it.
    mapsText+="7400000000-7400001000 r-xp 0 01:01 123 "+second+"\n";
    check(!discover()&&std::string(diagnostic.reason)=="image-live-header","unmapped preloaded entry cannot mask actual live-header failure");
    reset();
    struct stat metadata{};stat(apk.c_str(),&metadata);
    auto identity=std::to_string(metadata.st_ino)+" "+apk;
    pos=mapsText.find(identity);mapsText.replace(pos,identity.size(),std::to_string(metadata.st_ino+1)+" "+apk);
    check(!discover()&&std::string(diagnostic.reason)=="image-source-metadata","mismatched source inode refuses attribution");
    reset();
    auto shifted=rows(apk,base+0x100000000ULL,false);
    mapsText.replace(0,mapsText.find('\n')+1,shifted.substr(0,shifted.find('\n')+1));
    check(!discover()&&std::string(diagnostic.reason)=="image-load-bias","inconsistent load addresses remain rejected");
    reset();
    for(size_t at=0;(at=mapsText.find(apk+"\n",at))!=std::string::npos;at+=apk.size()+11)
        mapsText.insert(at+apk.size()," (deleted)");
    check(!discover()&&image.base==0x55,"deleted APK never reopens a replacement pathname");
    reset();mapsText=rows(apk,base,true);pos=mapsText.find('\n',mapsText.find('\n')+1);auto end=mapsText.find('\n',pos+1);mapsText.erase(pos+1,end-pos);
    check(!discover()&&std::string(diagnostic.reason)=="image-incomplete-load","missing executable page refuses incomplete image");
    reset();mapsText.pop_back();
    check(!discover()&&std::string(diagnostic.reason)=="maps-incomplete","truncated maps rejected");
    reset();mapsText+=candidates(1024);
    check(!discover()&&std::string(diagnostic.reason)=="apk-candidate-budget","APK discovery still has finite budget");
    reset();
    // Short rows exercise the line cap without hitting the byte cap first.
    mapsText.clear();for(size_t i=0;i<=nhk::kMaxFileInventoryLines;++i)mapsText+="1000-2000 r--p 0 00:00 0\n";
    check(!discover()&&std::string(diagnostic.reason)=="maps-line-budget","over-budget line inventory rejected");
    std::istringstream tooMany(mapsText);
    check(nhk::parse_file_mappings(tooMany).empty(),"shared parser never publishes a truncated inventory");
    reset();mapsText.assign(nhk::kMaxFileInventoryBytes+1,'x');
    check(!discover()&&std::string(diagnostic.reason)=="maps-byte-budget","oversized map bytes rejected");
    std::istringstream tooLong(mapsText+"\n");
    check(nhk::parse_file_mappings(tooLong).empty(),"shared parser rejects oversized input");
    reset();
    std::cout<<"HYOS_IMAGE_INVENTORY page="<<page<<" checks="<<count<<" failed="<<failed<<'\n';
    return failed?1:0;
}
