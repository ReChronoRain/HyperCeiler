#include <algorithm>
#include <array>
#include <atomic>
#include <cerrno>
#include <cstddef>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <limits>
#include <map>
#include <span>
#include <string>
#include <vector>

#define LOGI(...) ((void)0)
#define LOGW(...) ((void)0)
constexpr int S_IFREG=0100000,S_IFDIR=0040000,S_IFIFO=0010000;
#define S_ISREG(x) (((x)&0170000)==S_IFREG)
#define S_ISDIR(x) (((x)&0170000)==S_IFDIR)
constexpr int O_RDONLY=0,O_WRONLY=1,O_CREAT=2,O_TRUNC=4,O_CLOEXEC=8,O_NOFOLLOW=16,O_NONBLOCK=32,O_DIRECTORY=64,O_EXCL=128;
using fixture_off_t=int64_t;
struct fixture_time { int64_t tv_sec=1,tv_nsec=2; };
struct fixture_stat {uint64_t st_dev=55,st_ino=111;int64_t st_size=0;uint32_t st_mode=0,st_uid=10001,st_gid=10001;fixture_time st_mtim{},st_ctim{};};
#define ELFMAG "\177ELF"
constexpr int SELFMAG=4,EI_CLASS=4,ELFCLASS64=2,EI_DATA=5,ELFDATA2LSB=1,EI_VERSION=6,EV_CURRENT=1,ET_DYN=3,EM_AARCH64=183,PT_LOAD=1,PF_R=4,PF_W=2,PF_X=1;
struct Elf64_Ehdr{unsigned char e_ident[16];uint16_t e_type,e_machine;uint32_t e_version;uint64_t e_entry,e_phoff,e_shoff;uint32_t e_flags;uint16_t e_ehsize,e_phentsize,e_phnum,e_shentsize,e_shnum,e_shstrndx;};
struct Elf64_Phdr{uint32_t p_type,p_flags;uint64_t p_offset,p_vaddr,p_paddr,p_filesz,p_memsz,p_align;};
static_assert(sizeof(Elf64_Ehdr)==64 && sizeof(Elf64_Phdr)==56);
namespace nhk { constexpr size_t kMaxExecutableCodeBytes=256u*1024u*1024u; bool safe_read(uintptr_t,std::span<std::byte>); }

std::filesystem::path root;
std::vector<uint8_t> live;
uint32_t uid=10001;
struct Node {uint32_t mode=S_IFREG|0600,owner=10001,gid=10001;uint64_t ino=111,epoch=1;};
struct Handle {FILE* file=nullptr;std::string name;bool dir=false;};
std::map<std::string,Node> nodes;
std::map<int,Handle> handles;
int nextFd=5;unsigned preCalls=0,sourceReads=0,linksFollowed=0;size_t maxRead=0;
std::string lastDir;
bool dirMissing=false,dirLink=false,dirBad=false,publicOnly=false,swapSource=false,lateMutation=false,nested=false,nesting=false,chooseB=false;
bool readEintr=false,writeEintr=false,syncEintr=false,shortIO=false,zeroWrite=false,readFault=false,writeFault=false,chmodFault=false,closeFault=false,syncFileFault=false,syncDirFault=false,renameFault=false;
bool fileLink=false,fileBadUid=false,filePublic=false,fileFifo=false,statFault=false,statMutation=false,liveFault=false,partialLiveFault=false;
void nested_read();
uint32_t mock_geteuid(){return uid;}
uint32_t mock_getegid(){return uid;}
int mock_getpid(){return 1234;}
std::string key(const char* path){
 std::string p=path;
 if(p.find("fixture.so")!=std::string::npos)return "source";
 if(p.find("real.so")!=std::string::npos)return "real";
 if(p.ends_with(".tmp"))return p.ends_with("hometweaks-sites.bin.tmp")?"victim":"temp-"+std::filesystem::path(p).filename().string();
 if(p.find("/data/local/tmp")!=std::string::npos||p.find("/storage/")!=std::string::npos)return "public";
 if(p.find("hometweaks-symbols.bin")!=std::string::npos)return "cacheSymbols";
 return chooseB?"cacheB":"cacheA";
}
int mock_open(const char* path,int flags,unsigned mode=0){
 if(flags&O_DIRECTORY){
  lastDir=path;
  if(publicOnly || (dirMissing&&std::string(path).ends_with("/files"))){errno=ENOENT;return -1;}
  if(dirLink&&(flags&O_NOFOLLOW)){errno=ELOOP;return -1;}
  int fd=nextFd++;handles[fd]={nullptr,"directory",true};return fd;
 }
 std::string k=key(path);bool exists=std::filesystem::exists(root/k);
 if(publicOnly&&k!="public"){errno=ENOENT;return -1;}
 if(fileLink&&k.starts_with("cache")&&(flags&O_NOFOLLOW)){errno=ELOOP;return -1;}
 if(k=="victim"&&!(flags&O_NOFOLLOW))++linksFollowed;
 if((flags&O_EXCL)&&exists){errno=EEXIST;return -1;}
 if(!exists&&!(flags&O_CREAT)){errno=ENOENT;return -1;}
 if(k=="source"&&swapSource)nodes[k].ino=222;
 FILE* f=std::fopen((root/k).string().c_str(),flags&O_WRONLY?"wb":"rb");
 if(!f){errno=EIO;return -1;}
 if(flags&O_CREAT){nodes[k].mode=S_IFREG|mode;nodes[k].owner=uid;nodes[k].gid=uid;}
 int fd=nextFd++;handles[fd]={f,k,false};return fd;
}
int mock_openat(int fd,const char* path,int flags,unsigned mode=0){
 if(!handles.contains(fd)||!handles[fd].dir){errno=EBADF;return -1;}
 return mock_open(path,flags,mode);
}
int mock_mkdir(const char*,unsigned){return 0;}
int mock_mkdirat(int fd,const char*,unsigned){if(!handles.contains(fd)){errno=EBADF;return -1;}dirMissing=false;return 0;}
int mock_fstat(int fd,fixture_stat* out){
 if(statFault||!handles.contains(fd)){errno=EIO;return -1;}
 const auto& h=handles[fd];*out={};
 if(h.dir){out->st_mode=S_IFDIR|0700;out->st_uid=dirBad?uid+1:uid;out->st_gid=uid;return 0;}
 const auto& n=nodes[h.name];out->st_mode=n.mode;out->st_uid=n.owner;out->st_gid=n.gid;out->st_ino=n.ino;
 out->st_size=int64_t(std::filesystem::file_size(root/h.name));out->st_ctim.tv_nsec=int64_t(n.epoch);
 if(h.name.starts_with("cache")){
  if(fileBadUid)out->st_uid=uid+1;if(filePublic)out->st_mode=S_IFREG|0644;if(fileFifo)out->st_mode=S_IFIFO|0600;
  if(statMutation&&preCalls)++out->st_ctim.tv_nsec;
 }
 return 0;
}
int64_t perform_read(int fd,void* out,size_t want,bool at,fixture_off_t offset){
 if(!handles.contains(fd)||handles[fd].dir){errno=EBADF;return -1;}
 if(readEintr){readEintr=false;errno=EINTR;return -1;}
 if(readFault){errno=EIO;return -1;}
 auto& h=handles[fd];if(at){if(offset<0 || _fseeki64(h.file,offset,SEEK_SET)){errno=EIO;return -1;}++preCalls;}
 maxRead=std::max(maxRead,want);if(h.name=="source"||h.name=="real")++sourceReads;
 if(shortIO)want=std::min(want,size_t(7));
 size_t n=std::fread(out,1,want,h.file);
 if(lateMutation&&h.name=="source"&&at&&offset>128)++nodes[h.name].epoch;
 if(nested&&!nesting&&h.name=="cacheA"){nested=false;nesting=true;nested_read();nesting=false;}
 return int64_t(n);
}
int64_t mock_read(int fd,void* out,size_t n){return perform_read(fd,out,n,false,0);}
int64_t mock_pread(int fd,void* out,size_t n,fixture_off_t at){return perform_read(fd,out,n,true,at);}
int64_t mock_write(int fd,const void* data,size_t n){
 if(writeEintr){writeEintr=false;errno=EINTR;return -1;}
 if(writeFault){errno=EIO;return -1;}if(zeroWrite)return 0;
 if(shortIO)n=std::min(n,size_t(7));
 auto& h=handles.at(fd);auto size=std::fwrite(data,1,n,h.file);std::fflush(h.file);return int64_t(size);
}
int mock_fchmod(int fd,unsigned mode){if(chmodFault){errno=EPERM;return -1;}nodes[handles.at(fd).name].mode=S_IFREG|mode;return 0;}
int mock_fsync(int fd){
 if(syncEintr){syncEintr=false;errno=EINTR;return -1;}
 if(handles.at(fd).dir?syncDirFault:syncFileFault){errno=EIO;return -1;}return 0;
}
int mock_close(int fd){
 if(!handles.contains(fd)){errno=EBADF;return -1;}
 auto h=handles[fd];handles.erase(fd);if(h.file)std::fclose(h.file);
 if(closeFault){errno=EINTR;return -1;}return 0;
}
int mock_unlink(const char* path){auto k=key(path);bool r=std::filesystem::remove(root/k);nodes.erase(k);if(!r){errno=ENOENT;return -1;}return 0;}
int mock_unlinkat(int fd,const char* path,int){if(!handles.contains(fd)){errno=EBADF;return -1;}return mock_unlink(path);}
int mock_rename(const char* from,const char* to){
 if(renameFault){errno=EIO;return -1;}
 auto a=key(from),b=key(to);std::filesystem::copy_file(root/a,root/b,std::filesystem::copy_options::overwrite_existing);std::filesystem::remove(root/a);nodes[b]=nodes[a];nodes.erase(a);return 0;
}
int mock_renameat(int a,const char* from,int b,const char* to){if(!handles.contains(a)||!handles.contains(b)){errno=EBADF;return -1;}return mock_rename(from,to);}
bool nhk::safe_read(uintptr_t at,std::span<std::byte> out){
 if(liveFault)return false;
 uintptr_t base=reinterpret_cast<uintptr_t>(live.data());
 if(at<base || at-base>live.size() || out.size()>live.size()-(at-base))return false;
 if(partialLiveFault){if(!out.empty())out[0]=std::byte{live[at-base]};return false;}
 std::memcpy(out.data(),reinterpret_cast<void*>(at),out.size());return true;
}
