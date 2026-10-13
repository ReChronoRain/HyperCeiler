from pathlib import Path
import sys,struct,subprocess,json,re
D=Path(__file__).resolve().parent;W=D.parents[1];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
sys.path.insert(0,str(W/'tests/home-layout-native'));from dart_dump import Elf
A=Path('C:/Users/XiaoYe/.codex/visualizations/2026/09/29/01a0eb2b-4de7-7442-83d8-db3197b7610f');outer=Elf(A/'launcher-7722-libapp.so');mini=Elf(A/'launcher-7722-mini.elf')
S=(R/'app/src/main/cpp/targets/home/tweaks/symtab.cpp').read_text(encoding='utf8');I=(R/'app/src/main/cpp/targets/home/tweaks/image.cpp').read_text(encoding='utf8')
def fun(s,n):
 a=s.index(n);a=s.rfind('\n',0,a)+1;j=s.index('{',a)+1;depth=1
 while depth:depth+=(s[j]=='{')-(s[j]=='}');j+=1
 return s[a:j]
run=D/('run-'+R.name);run.mkdir(exist_ok=True)
# Only XZ decompression is substituted: the ELF metadata parser, registry, code guard,
# selection loop and public index methods below are the actual production implementation.
code='\n'.join(line for line in S.splitlines() if not line.startswith('#include'))
code=code.replace('extern "C" {\n}','')
dec=fun(code,'enum xz_ret RunDecoder(')
code=code.replace(dec,'enum xz_ret RunDecoder(const std::vector<uint8_t>&,uint32_t,std::vector<uint8_t>*out,uint64_t=0){if(slowDecode){auto until=std::chrono::steady_clock::now()+std::chrono::milliseconds(5);while(std::chrono::steady_clock::now()<until){}}*out=decoded;return XZ_STREAM_END;}')
header='\n'.join(line for line in (R/'app/src/main/cpp/targets/home/tweaks/symtab.h').read_text().splitlines() if not line.startswith('#include') and not line.startswith('#pragma'))
pre=r'''
#define LOGI(...) ((void)0)
#include <chrono>
#include <array>
#include <cstdint>
#include <cstddef>
#include <vector>
#include <string>
#include <cstring>
#include <cstdlib>
#include <cstdio>
#include <cstdarg>
#include <iostream>
#include <fstream>
#include <algorithm>
#include <limits>
#include <cerrno>
#include <stdexcept>
static bool blockLargeString=false;static int blocked=0;
void* operator new(std::size_t n){if(blockLargeString&&n==2097153){blocked++;throw std::bad_alloc();}if(auto p=std::malloc(n))return p;throw std::bad_alloc();}
void operator delete(void*p) noexcept{std::free(p);}
void operator delete(void*p,std::size_t) noexcept{std::free(p);}
struct Elf64_Ehdr{unsigned char e_ident[16];uint16_t e_type,e_machine;uint32_t e_version;uint64_t e_entry,e_phoff,e_shoff;uint32_t e_flags;uint16_t e_ehsize,e_phentsize,e_phnum,e_shentsize,e_shnum,e_shstrndx;};
struct Elf64_Shdr{uint32_t sh_name,sh_type;uint64_t sh_flags,sh_addr,sh_offset,sh_size;uint32_t sh_link,sh_info;uint64_t sh_addralign,sh_entsize;};
struct Elf64_Sym{uint32_t st_name;uint8_t st_info,st_other;uint16_t st_shndx;uint64_t st_value,st_size;};
struct Elf64_Phdr{uint32_t p_type,p_flags;uint64_t p_offset,p_vaddr,p_paddr,p_filesz,p_memsz,p_align;};
static_assert(sizeof(Elf64_Ehdr)==64&&sizeof(Elf64_Shdr)==64&&sizeof(Elf64_Sym)==24);
#define ELFMAG "\177ELF"
constexpr int SELFMAG=4,EI_CLASS=4,ELFCLASS64=2,EI_DATA=5,ELFDATA2LSB=1,EI_VERSION=6,EV_CURRENT=1,EM_AARCH64=183;
constexpr int SHT_SYMTAB=2,SHT_STRTAB=3,SHN_UNDEF=0,STT_FUNC=2,SHF_EXECINSTR=4;
#define ELF64_ST_TYPE(info) ((info)&15)
#define LOGW(...) ((void)0)
enum xz_ret{XZ_OK,XZ_STREAM_END,XZ_MEM_ERROR,XZ_UNSUPPORTED_CHECK};
uint32_t xz_crc32(const unsigned char*,size_t,uint32_t){return 0;}void xz_crc32_init(){}
bool slowDecode=false;std::vector<uint8_t> outerFile,decoded,mapped;std::vector<std::pair<uintptr_t,size_t>> readable;unsigned fileReads=0;bool faultAll=false;uintptr_t faultAddress=0;
namespace hometweaks{
struct Segment{uintptr_t begin,end;uint32_t flags;};
struct Image{uintptr_t base=0;char path[512]{};Segment segments[16]{};size_t segmentCount=0;uint64_t apkEntryOffset=0;bool fromApkEntry=false;uint64_t fileViewBytes=0,sourceDevice=0,sourceInode=0;};
uint64_t ImageIdentity(const Image&){return 123;}
int saves=0;bool saveFault=false;std::vector<uint8_t> cached;bool cacheOn=false;
bool LoadSymbolCache(uint64_t,std::vector<uint8_t>*out){if(!cacheOn)return false;*out=cached;return true;}
bool SaveSymbolCache(uint64_t,const std::vector<uint8_t>&p){++saves;cached=p;return !saveFault;}
bool ReadImageFile(const Image&,uint64_t offset,void*out,size_t n){fileReads++;if(offset>outerFile.size()||n>outerFile.size()-offset)return false;std::memcpy(out,outerFile.data()+offset,n);return true;}
using off_t=int64_t;using ssize_t=int64_t;int opened=0;
constexpr int O_RDONLY=0,O_CLOEXEC=0;
struct stat{int st_mode=1;int64_t st_size=0;uint64_t st_dev=0,st_ino=0;};
#define S_ISREG(m) ((m)==1)
int fstat(int,struct stat*s){s->st_size=outerFile.size();s->st_dev=123;s->st_ino=456;return 0;}
bool StatMatches(const Image& image,const struct stat& st){return S_ISREG(st.st_mode)&&st.st_size>=0&&st.st_dev==image.sourceDevice&&st.st_ino==image.sourceInode&&(image.fromApkEntry?image.apkEntryOffset:0)<=uint64_t(st.st_size)&&image.fileViewBytes<=uint64_t(st.st_size)-(image.fromApkEntry?image.apkEntryOffset:0);}
int open(const char*,int){opened++;return 3;}int close(int){return 0;}
ssize_t pread(int,void*out,size_t n,off_t at){if(at<0||uint64_t(at)>outerFile.size()||n>outerFile.size()-at)return -1;memcpy(out,outerFile.data()+at,n);return n;}
}
'''
if 'CodeView(image)' in S:
 sys.path.insert(0,str(W/'tests/os4-audit-symbol-prologue-20261005'));from fixture_support import codeview_fixture
 pre+=codeview_fixture(R,'if(faultAll || (faultAddress && address==faultAddress))return false;for(const auto& [begin,bytes]:readable)if(address>=begin&&address-begin<=bytes&&out.size()<=bytes-(address-begin)){std::memcpy(out.data(),reinterpret_cast<void*>(address),out.size());return true;}return false;')
pre+=header+'\n'+code+'\nnamespace hometweaks {\n'+fun(I,'bool ReadImageFile(').replace('bool ReadImageFile(','bool audit_read_image_file(')+'\n}\n'
pre+=r'''
std::vector<uint8_t> read(const char*p){std::ifstream f(p,std::ios::binary|std::ios::ate);if(!f)return {};const auto size=f.tellg();std::vector<uint8_t>b(size);f.seekg(0);f.read(reinterpret_cast<char*>(b.data()),b.size());return b;}
int main(int argc,char**argv){if(argc!=5)return 2;std::string test=argv[1];outerFile=read(argv[2]);decoded=read(argv[3]);auto full=read(argv[4]);
using namespace hometweaks;Elf64_Ehdr h{};memcpy(&h,full.data(),64);uint64_t end=0;
for(unsigned i=0;i<h.e_phnum;i++){Elf64_Phdr p{};memcpy(&p,full.data()+h.e_phoff+i*sizeof p,sizeof p);if(p.p_type==1)end=std::max(end,p.p_vaddr+p.p_memsz);}
mapped.resize(end+4096);readable.push_back({uintptr_t(mapped.data()),mapped.size()});Image image{};image.base=uintptr_t(mapped.data());strcpy(image.path,"fixture.so");
for(unsigned i=0;i<h.e_phnum;i++){Elf64_Phdr p{};memcpy(&p,full.data()+h.e_phoff+i*sizeof p,sizeof p);if(p.p_type!=1)continue;
 memcpy(mapped.data()+p.p_vaddr,full.data()+p.p_offset,p.p_filesz);image.segments[image.segmentCount++]={image.base+p.p_vaddr,image.base+p.p_vaddr+p.p_memsz,p.p_flags};}
if(test=="fault_all")faultAll=true;
int checks=0,failed=0;auto ok=[&](bool v,const char*m){checks++;if(!v){failed++;std::cout<<"FAIL "<<m<<"\n";}};
if(test=="exec_bounds"){
 alignas(4) uint32_t words[64]{};words[0]=0xa9bf79fd;words[1]=0xaa0f03fd;readable.push_back({uintptr_t(words),sizeof(words)});
 Image small{};small.base=uintptr_t(words)-0x100;small.segmentCount=1;small.segments[0]={uintptr_t(words),uintptr_t(words)+4,5};
 ok(!LooksLikeDartFunction(small,0x100,"Fixture",8),"second prologue word must be inside executable segment");
 small.segments[0].end+=4;
 ok(LooksLikeDartFunction(small,0x100,"Fixture",8),"exact 8-byte prologue remains supported");
 ok(!LooksLikeDartFunction(small,0x100,"Fixture",12),"entire advertised function extent must fit exec segment");
 ok(!LooksLikeDartFunction(small,0x100,"Fixture",4),"four-byte frame claim rejected");
 ok(!LooksLikeDartFunction(small,0x100,"Fixture",9),"unaligned byte extent rejected");
 ok(!LooksLikeDartFunction(small,0x100,nullptr,8),"null name rejected before opcode reads");
 words[0]=0xf94001e2;words[1]=0x93407c42;
 ok(!LooksLikeDartFunction(small,0x100,"allocateTwoByteString",0xec),"verified stub reads require full declared exec extent too");
}else if(test=="io_bounds"){
 unsigned char dst[16]{};image.fileViewBytes=outerFile.size();image.sourceDevice=123;image.sourceInode=456;image.fromApkEntry=true;image.apkEntryOffset=UINT64_MAX-15;opened=0;
 ok(!audit_read_image_file(image,32,dst,16)&&opened==0,"APK origin+offset overflow rejected before IO");
 image.apkEntryOffset=0;opened=0;
 ok(!audit_read_image_file(image,UINT64_MAX,dst,16)&&opened==0,"signed off_t conversion overflow rejected before IO");
 opened=0;ok(!audit_read_image_file(image,INT64_MAX-3,dst,16)&&opened==0,"end offset overflow rejected before IO");
 opened=0;ok(audit_read_image_file(image,16,dst,16)&&opened==1&&!memcmp(dst,outerFile.data()+16,16),"valid original pread behavior remains exact");
}else{
 if(test=="fault_target"){
 Elf64_Ehdr mini{};memcpy(&mini,decoded.data(),64);std::vector<Elf64_Shdr> sh(mini.e_shnum);memcpy(sh.data(),decoded.data()+mini.e_shoff,sh.size()*64);
 for(auto &sy:sh)if(sy.sh_type==SHT_SYMTAB){auto &st=sh[sy.sh_link];for(size_t at=0;at<sy.sh_size;at+=24){Elf64_Sym row{};memcpy(&row,decoded.data()+sy.sh_offset+at,24);if(!strcmp(reinterpret_cast<char*>(decoded.data()+st.sh_offset+row.st_name),"BigFolderCommonStrategy.calItemSize"))faultAddress=image.base+row.st_value;}}}
 blockLargeString=test=="oversize_shstr";auto &index=SymbolIndex::Instance();bool loaded=false;bool threw=false;
 try{loaded=index.EnsureLoaded(image);}catch(const std::exception&e){threw=true;std::cout<<"EXCEPTION "<<e.what()<<"\n";}
 blockLargeString=false;uint32_t va=0,size=0,span=0;const char* target="BigFolderCommonStrategy.calItemSize";
 if(test=="fault_all"||test=="fault_target"){
  ok(!threw&&loaded==(test=="fault_target"),"read fault drops affected roots without crashing unrelated roots");
  va=0xabcdef;size=0xabcdef;ok(!index.Find(target,&va,&size)&&va==0xabcdef&&size==0xabcdef,"failed prologue never publishes target/out fields");
 }else if(test=="real"){
  ok(loaded&&!threw,"real 7722 ELF loads");ok(index.Find(target,&va,&size)&&va==0x15e0088,"actual original root address preserved");
  ok(index.SpanFrom(va,&span)&&span==0x3b8,"full STT_FUNC boundary retained for large-folder body");
  ok(!index.Find("HotseatLayerGetxController.calculatePositionX",&va,&size),"real duplicate function name is ambiguous, not largest-body wins");
  ok(!index.Find("HotseatLayerGetxController.calHotseatCenterPosition",&va,&size),"second real same-name alias is not guessed");
  const auto reads=fileReads;for(int i=0;i<1000;i++)index.EnsureLoaded(image);
  ok(fileReads==reads,"ready symbol index never rescans file on repeated lookup");

    auto good=cached;cacheOn=true;index.ResetForTest();auto before=fileReads;
    ok(index.TryLoadCached(image,123)&&fileReads==before,"cache loads real roots without ELF/XZ reread");
    ok(index.Find(target,&va,&size)&&va==0x15e0088,"cache restores original target");
    for(size_t off: {size_t(0),size_t(4),size_t(8),size_t(12)}){cached=good;cached[off]^=0xff;index.ResetForTest();ok(!index.TryLoadCached(image,123)&&!index.loaded(),"corrupt registry/count/flag rejects atomic publication");}
    cached=good;cached.pop_back();index.ResetForTest();ok(!index.TryLoadCached(image,123),"truncated cache rejects");
    cached=good;faultAll=true;index.ResetForTest();ok(!index.TryLoadCached(image,123),"cached address still requires original live code guard");faultAll=false;
    cached=good;index.ResetForTest();ok(index.TryLoadCached(image,123),"valid cache recovers after rejected attempts");
cacheOn=false;slowDecode=true;index.ResetForTest();
    ok(!index.EnsureLoaded(image,1)&&!index.loaded()&&!index.attempted(),"deadline discards partial index and permits worker retry");
    slowDecode=false;ok(index.EnsureLoaded(image)&&index.Find(target,&va,&size),"unbounded worker recovers timed out startup");
    saveFault=true;index.ResetForTest();ok(index.EnsureLoaded(image),"persist failure does not discard ready index");
    const auto failedSaveCount=saves;for(int n=0;n<1000;++n)index.EnsureLoaded(image);
    ok(saves==failedSaveCount,"failed persistence attempted once, not per lookup");saveFault=false;
    std::cout<<"FOUND="<<index.foundCount()<<"/"<<TargetFunctionCount()<<"\n";
  for(size_t i=0;i<TargetFunctionCount();i++){uint32_t v=0,z=0,s=0;const auto& target=TargetFunctionAt(i);
   if(index.Find(target.needle,&v,&z)){index.SpanFrom(v,&s);std::cout<<"ROOT\t"<<target.needle<<"\t"<<v<<"\t"<<z<<"\t"<<s<<"\n";}}
 }else if(test=="foreign_boundary"){
  ok(loaded&&!threw&&index.Find(target,&va,&size)&&index.SpanFrom(va,&span)&&span==0x3b8,"non-executable fake function record cannot truncate genuine owning body");
 }else if(test=="valid_unaligned_tables"){
  ok(loaded&&!threw&&index.Find(target,&va,&size)&&va==0x15e0088,"unaligned packed records read via memcpy without dereference UB");
 }else if(test=="bad_type"||test=="high_va"||test=="high_size"||test=="undefined"||test=="wrong_section"){
  ok(loaded&&!threw,"unrelated valid original roots remain loaded");
  ok(!index.Find(target,&va,&size),"invalid typed/ranged named function never survives narrowing");
 }else{
  ok(!loaded&&!threw,"malformed metadata cleanly rejected");
  if(test=="oversize_shstr")ok(blocked==0,"oversized shstr checked BEFORE allocation");
  if(test=="wrap_section_headers")ok(std::string(index.status()).find("调试数据不是预期的 ELF")!=std::string::npos,"wrapped section table rejected before dereference");
 }
 const auto reads=fileReads;for(int i=0;i<1000;i++)index.EnsureLoaded(image);ok(fileReads==reads,"immutable parsed or rejected image never rescans on repeat lookup");
 std::cout<<"STATUS="<<index.status()<<"\n";
}
std::cout<<"SYMBOL_LOADER "<<test<<": "<<checks<<" checks; failed="<<failed<<"\n";return failed?1:0;
}
'''
(run/'test.cpp').write_text(pre,encoding='utf8')
zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe'
p=subprocess.run([str(zig),'c++','-std=c++20','-O0',str(run/'test.cpp'),'-o',str(run/'test.exe')],capture_output=True,text=True,encoding='utf8');print(p.stdout+p.stderr,end='')
if p.returncode:sys.exit(p.returncode)
cases={}
def add(name,out=None,debug=None):cases[name]=(bytes(outer.data if out is None else out),bytes(mini.data if debug is None else debug))
for n in ['real','exec_bounds','io_bounds']:add(n)
if 'CodeView(image)' in S:
 for n in ['fault_all','fault_target']:add(n)
# All mutation offsets come from the actual ELF metadata, never production patch offsets.
shoff=mini.shoff;syidx=mini.section_names['.symtab'];sysec=mini.sections[syidx];stsec=mini.sections[sysec[6]];strs=mini.data[stsec[4]:stsec[4]+stsec[5]]
target_entry=None
for at in range(sysec[4],sysec[4]+sysec[5],24):
 n,info,other,ndx,v,z=struct.unpack_from('<IBBHQQ',mini.data,at)
 if strs[n:strs.find(b'\0',n)]==b'BigFolderCommonStrategy.calItemSize':target_entry=at;break
assert target_entry is not None
for n,o,b in [('inner_class',4,1),('inner_endian',5,2),('inner_ident_version',6,0)]:
 d=bytearray(mini.data);d[o]=b;add(n,debug=d)
for n,o,fmt,v in [('inner_machine',18,'H',62),('inner_version',20,'I',0),('wrap_section_headers',40,'Q',2**64-8),('large_section_count',60,'H',300)]:
 d=bytearray(mini.data);struct.pack_into('<'+fmt,d,o,v);add(n,debug=d)
for n,o,b in [('outer_endian',5,2),('outer_ident_version',6,0)]:
 d=bytearray(outer.data);d[o]=b;add(n,out=d)
d=bytearray(outer.data);struct.pack_into('<H',d,18,62);add('outer_machine',out=d)
d=bytearray(outer.data);outersh=outer.shoff+outer.shstrndx*64;struct.pack_into('<Q',d,outersh+32,2**21);add('oversize_shstr',out=d)
for n,o,fmt,v in [('wrap_symtab',shoff+syidx*64+24,'Q',2**64-8),('trailing_symbol_bytes',shoff+syidx*64+32,'Q',sysec[5]+1),('bad_strtab_type',shoff+sysec[6]*64+4,'I',0),('wrap_strtab',shoff+sysec[6]*64+24,'Q',2**64-8)]:
 d=bytearray(mini.data);struct.pack_into('<'+fmt,d,o,v);add(n,debug=d)
for n,o,fmt,v in [('bad_type',target_entry+4,'B',17),('high_va',target_entry+8,'Q',0x100000000+0x15e0088),('high_size',target_entry+16,'Q',0x100000000+0x3b8),('undefined',target_entry+6,'H',0),('wrong_section',target_entry+6,'H',sysec[6])]:
 d=bytearray(mini.data);struct.pack_into('<'+fmt,d,o,v);add(n,debug=d)
d=bytearray(mini.data)
for at in range(sysec[4],sysec[4]+sysec[5],24):
 n,info,other,ndx,v,z=struct.unpack_from('<IBBHQQ',mini.data,at)
 if info&15==2 and v and v<0x1400000:
  struct.pack_into('<IBBHQQ',d,at,0,info,other,sysec[6],0x15e0088+4,0);break
add('foreign_boundary',debug=d)
# Shift the actual serialized sections/symbols by one byte, updating all offsets.
d=bytearray(mini.data[:64])+bytearray(b'\0')+bytearray(mini.data[64:]);struct.pack_into('<Q',d,40,shoff+1)
for i,section in mini.sections.items():
 if section[4]>=64:struct.pack_into('<Q',d,shoff+1+i*64+24,section[4]+1)
add('valid_unaligned_tables',debug=d)
failed=0;results=[]
for name,(o,d) in cases.items():
 op=run/(name+'-outer.bin');dp=run/(name+'-mini.bin');op.write_bytes(o);dp.write_bytes(d)
 command=[str(run/'test.exe'),name,str(op),str(dp),str(A/'launcher-7722-libapp.so')]
 p=subprocess.run(command,capture_output=True,text=True,encoding='utf8',errors='replace');print(p.stdout+p.stderr,end='',flush=True)
 if p.returncode:failed+=1;print(f'CASE_EXIT {name}={p.returncode}')
 results.append(dict(case=name,command=subprocess.list2cmdline(command),output=p.stdout+p.stderr,exit_status=p.returncode))
(run/'results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf8')
print(f'SYMBOL_LOADER_AUDIT={len(cases)} cases; failed={failed}; production parser/code guards; XZ decoder and file/live-read boundaries substituted')
sys.exit(1 if failed else 0)
