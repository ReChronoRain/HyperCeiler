// Reuse only the established maps/live-read host boundaries and production includes.
#define main synthetic_fixture_main
#include "test.cpp"
#undef main

int main(int argc,char**argv){
    if(argc!=2)return 2;
    const std::string path=argv[1];
    const uint64_t page=nhk::host_page_size(),bias=0x7100000000ULL;
    struct stat metadata{};if(stat(path.c_str(),&metadata))return 2;
    for(const char* name:{"libapp.so","libapp_launcher.so"}){
        const auto stored=nhk::zip_stored_entry(path,std::string("lib/arm64-v8a/")+name);
        if(!stored)return 2;
        std::ifstream file(path,std::ios::binary);file.seekg(stored->first);
        std::vector<std::byte> header(8192);file.read(reinterpret_cast<char*>(header.data()),header.size());
        const auto segments=nhk::elf::parse_program_segments(header,true);if(!segments)return 2;
        headers.clear();headers[bias]=header;
        mapsText.clear();std::ostringstream rows;
        size_t count=0;
        // Split each file-backed load into individual pages, as mprotect hooks can do.
        // Follow p_vaddr/p_offset rather than assuming the Rust image's bias is linear.
        for(const auto& segment:*segments){
            if(segment.type!=1||!segment.filesz)continue;
            const uint64_t first=nhk::page_down(segment.offset),last=nhk::page_up(segment.offset+segment.filesz);
            for(uint64_t offset=first;offset<last;offset+=page){
                const uint64_t address=bias+nhk::page_down(segment.vaddr)+offset-first;
                const char* perms=(segment.flags&1)?"r-xp":(segment.flags&2)?"rw-p":"r--p";
                rows<<std::hex<<address<<'-'<<address+page<<' '<<perms<<' '<<stored->first+offset
                    <<' '<<major(metadata.st_dev)<<':'<<minor(metadata.st_dev)<<' '
                    <<std::dec<<metadata.st_ino<<' '<<path<<'\n';
                ++count;
            }
        }
        mapsText=rows.str();hometweaks::Image image{};hometweaks::ImageDiscoveryDiagnostic diagnostic;
        if(count<=64||!hometweaks::FindImageFromMappedApks(name,&image,&diagnostic)||image.base!=bias){
            std::cerr<<"FAIL actual "<<name<<" page="<<page<<" stage="<<diagnostic.reason<<" maps="<<count<<'\n';return 1;
        }
        std::cout<<"REAL_HYOS_IMAGE library="<<name<<" page="<<page<<" maps="<<count<<" coalesced="<<image.segmentCount<<" PASS\n";
    }
}
