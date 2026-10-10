// Runs original full ARM64 functions from private ELF mappings. Explicit adapters
// replace allocation/FFI transport/task queue/panic only; no Android/Dart VM lives here.
#include <algorithm>
#include <array>
#include <cassert>
#include <csetjmp>
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
using Params=std::array<int32_t,4>;
static unsigned checks=0,cases=0;
void expect(bool v,const char* s){++checks;if(!v){std::cerr<<"FAILED: "<<s<<" case="<<cases<<"\n";std::exit(3);}}
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
static std::jmp_buf failure;
extern "C" [[noreturn]] void panic_adapter(){std::longjmp(failure,1);}
extern "C" {uintptr_t adapter_writer,adapter_serializer,sent_registers[6];void dart_factory_adapter();void dart_allocator_adapter();void dart_send_adapter();}
struct DartCall{uintptr_t entry,x1,x2,x3,x4,stack,pool,heap,null,result,returned_stack;};
extern "C" void call_dart(DartCall*);
struct DartHeap{
 uint8_t*mem;uintptr_t n,p,s,w,t,z,u,b,o,d,c,ctx,api,mint;std::array<uint8_t,4096>pool{};std::array<uint64_t,2048>stack{};std::array<uint8_t,96>output{};std::array<uint8_t,16>temp{};
 template<class T>void put(uintptr_t tag,size_t offset,T value){memcpy(reinterpret_cast<void*>(tag+offset),&value,sizeof value);}template<class T>T get(uintptr_t tag,size_t off){T v;memcpy(&v,reinterpret_cast<void*>(tag+off),sizeof v);return v;}
 uintptr_t obj(size_t off){return uintptr_t(mem+off)+1;}void ref(uintptr_t obj,size_t off,uintptr_t to){assert((to>>32)==(uintptr_t(mem)>>32));put<uint32_t>(obj,off,uint32_t(to));}
 DartHeap(){mem=static_cast<uint8_t*>(mmap(nullptr,65536,3,MAP_PRIVATE|MAP_ANONYMOUS,-1,0));assert(mem!=MAP_FAILED);assert((uintptr_t(mem)>>32)==((uintptr_t(mem)+65535)>>32));n=obj(0);p=obj(256);s=obj(512);w=obj(768);t=obj(1024);z=obj(1280);u=obj(1536);b=obj(1792);o=obj(2048);d=obj(2304);c=obj(2560);ctx=obj(2816);api=obj(3072);mint=obj(3328);
 ref(s,7,w);ref(w,7,b);ref(w,0x17,t);ref(w,0x1b,u);put<int64_t>(w,0xb,0);put<uint32_t>(t,0x13,8);ref(t,0x17,z);put<uint32_t>(t,0x1b,0);put<uintptr_t>(z,7,uintptr_t(temp.data()));put<uintptr_t>(u,7,uintptr_t(temp.data()));put<uint32_t>(u,0x13,8);put<int64_t>(b,0xb,64);ref(b,0x17,o);put<uintptr_t>(o,7,uintptr_t(output.data()+16));put<uint32_t>(o,0x13,128);put<uint32_t>(d,0x13,6);ref(c,0x17,ctx);ref(ctx,0xf,api);ref(ctx,0x13,p);ref(api,0x13,n);memcpy(pool.data()+0x120,&d,8); // Sender's descriptor is read at PP+0x1f58, so separate larger PP below.
 output.fill(0xa5);adapter_writer=w;adapter_serializer=s;std::fill(std::begin(sent_registers),std::end(sent_registers),0);}
 ~DartHeap(){munmap(mem,65536);}uintptr_t top(){return uintptr_t(&stack[1800]);}
 void reset(){put<int64_t>(w,0xb,0);output.fill(0xa5);temp.fill(0);}
 void fields(std::array<int64_t,4>v){for(unsigned j=0;j<4;j++)put<int64_t>(p,7+j*8,v[j]);}
 Params wire(){Params q;memcpy(q.data(),output.data()+16,16);return q;}
 void call(Image&i,size_t va,uintptr_t a,uintptr_t b1,uintptr_t c1,uintptr_t d1,uintptr_t optional=0){std::array<uint8_t,8192>pp{};memcpy(pp.data(),pool.data(),pool.size());auto desc=d;memcpy(pp.data()+0x1f58,&desc,8);stack[1800]=optional;stack[1801]=c;DartCall x{uintptr_t(i.base+va),a,b1,c1,d1,top(),uintptr_t(pp.data()),uintptr_t(mem)>>32,n,0,0};call_dart(&x);expect(x.result==n,"Dart canonical-null return");expect(x.returned_stack==top(),"Dart x15 restored");}
 bool guards(){return std::all_of(output.begin(),output.begin()+16,[](auto v){return v==0xa5;})&&std::all_of(output.begin()+32,output.end(),[](auto v){return v==0xa5;});}
};
static void* enqueued;static uint64_t task_header;static unsigned enqueue_count,free_count;static uint64_t freed_cap;static void*freed_ptr;static std::array<uint64_t,3>task_metadata;static uintptr_t expected_vtable;
extern "C" void release_buffer_adapter(uint64_t cap,void*ptr){++free_count;freed_cap=cap;freed_ptr=ptr;}
extern "C" void metadata_adapter(void*out,const void*){memcpy(out,task_metadata.data(),24);}
extern "C" uintptr_t find_rule_adapter(const char*,size_t){return 0;}
extern "C" void* alloc_adapter(size_t n){expect(n==264,"FRB allocates exact 264-byte async closure");return calloc(1,n);}
extern "C" uint64_t* enqueue_adapter(uintptr_t,uintptr_t,void*closure,void*vtable,uint64_t){enqueued=closure;++enqueue_count;task_header=0xcc;expect(uintptr_t(vtable)==expected_vtable,"FRB supplies exact launcher_model_start_load async vtable");return &task_header;}
static Params read_current(Image&r){Params out;r.fn<void(*)(void*)>(0x6c567c)(&out);return out;}
static void setup_rust(Image&r){expected_vtable=uintptr_t(r.base)+0xe25568;r.at<uint8_t>(0xf23ec0)=2;r.at<uint8_t>(0xf2228c)=0;r.at<uint64_t>(0xf21968)=1;r.jump(0x9d5f38,uintptr_t(release_buffer_adapter));r.jump(0x92c5d4,uintptr_t(metadata_adapter));r.jump(0x8e914c,uintptr_t(find_rule_adapter));r.jump(0xdf9f40,uintptr_t(alloc_adapter));r.jump(0xae64bc,uintptr_t(enqueue_adapter));r.jump(0xa25548,uintptr_t(panic_adapter));r.jump(0xa21218,uintptr_t(panic_adapter));r.sync();}
static void setup_dart(Image&d){d.jump(0x86ed9c,uintptr_t(dart_factory_adapter));d.small_jump(0x18a92c4,uintptr_t(dart_allocator_adapter));d.jump(0x86da14,uintptr_t(dart_send_adapter));for(auto at:{0x1b58238,0x86e5e4,0x869384,0x86d738,0x1b58398})d.small_jump(at,uintptr_t(panic_adapter));d.sync();}
static bool try_dispatch(Image* r,uint32_t id,void* bytes){if(setjmp(failure))return false;r->fn<void(*)(uint32_t,uint64_t,void*,int32_t,int32_t)>(0x7ed3b8)(id,0x9988776655443322ULL,bytes,16,16);return true;}
static bool try_decode(Image* r,Params* p,uint64_t* reader){if(setjmp(failure))return false;r->fn<void(*)(void*,void*)>(0x965f98)(p,reader);return true;}
static bool try_frb(Image* r,void* bytes,int cap,int data_length){if(setjmp(failure))return false;r->fn<void(*)(uint64_t,void*,int32_t,int32_t)>(0x84152c)(123,bytes,cap,data_length);return true;}
static bool try_dart(Image* d,DartHeap* h){if(setjmp(failure))return false;h->call(*d,0x8bb864,0,h->p,h->s,0);return true;}
int main(int argc,char**argv){assert(argc==3||argc==4);const std::string mutant=argc==4?argv[3]:"positive";Image dart(argv[1]),rust(argv[2]);setup_dart(dart);setup_rust(rust);
 struct Mutation{const char*name;bool rust;size_t va;uint32_t oldword,newword;};
 const Mutation mutations[]={{"constant-two",false,0x8bb8a4,0xd2800082,0xd2800042},{"wrong-y-field",false,0x8bb894,0xf840f002,0xf8407002},{"raw-payload-shift",false,0x86ea90,0x93407c42,0x9341fc42},{"skip-byte-copy",false,0x86ecec,0xb8004450,0xd503201f},{"decoder-no-advance",true,0x966020,0xf9000d09,0xd503201f},{"save-skip-copy",true,0x6c47a0,0x3d800120,0xd503201f},{"decoder-skip-truncation",true,0x966014,0x540000c9,0xd503201f},{"end-ignore-length",true,0xaa4114,0x540000c1,0xd503201f}};
 bool recognized=mutant=="positive";for(const auto&m:mutations){auto&i=m.rust?rust:dart;expect(i.at<uint32_t>(m.va)==m.oldword,"mutation site is exact original ROM instruction");if(mutant==m.name){i.word(m.va,m.newword);recognized=true;}}assert(recognized);
 dart.sync();rust.sync();
 expect(read_current(rust)==Params({0,0,0,0}),"original cold OnceCell zero initialized");
 const std::array<std::array<int64_t,4>,10>vectors{{{4,6,4,6},{5,7,4,6},{3,4,999,6},{9,13,999,6},{6,8,99,6},{5,-1,777,-1},{0,0,222,0},{INT32_MIN,INT32_MAX,INT32_MIN,-2147483647},{1,-2,3,4},{0x100000001LL,-0x100000002LL,333,0x180000000LL}}};
 for(const auto&v:vectors){++cases;DartHeap h;h.fields(v);h.call(dart,0x8bb7bc,0,0,0,0,0x1234567812345678ULL);const Params want{int32_t(v[0]),int32_t(v[1]),4,int32_t(v[3])};expect(h.wire()==want,"original sender+serializer writes X Y literal4 Ymin in order");expect(h.get<int64_t>(h.w,0xb)==16&&h.guards(),"writer exact 16-byte advance and no overrun");expect(sent_registers[2]==851&&sent_registers[1]==h.s&&sent_registers[4]==0x1234567812345678ULL&&sent_registers[5]==1,"original sender passes func851 serializer port exactly once");
 unsigned before=enqueue_count;auto*bytes=h.output.data()+16;expect(try_dispatch(&rust,uint32_t(sent_registers[2]),bytes),"valid full dispatcher/FRB must not panic");expect(enqueue_count==before+1&&task_header==0x84,"original full FRB enqueues one task and returns normally");expect(freed_ptr==bytes&&freed_cap==16,"original end reaches exact buffer release adapter");Params got;memcpy(got.data(),static_cast<uint8_t*>(enqueued)+0x48,16);expect(got==want,"actual caller-qualified decoder moves all four words into task");uint64_t port;memcpy(&port,static_cast<uint8_t*>(enqueued)+0x40,8);expect(port==0x9988776655443322ULL,"actual FRB port remains external to four-word DeviceParam");
 rust.fn<void(*)(const void*)>(0x6c46e0)(&got);expect(read_current(rust)==want,"original save and getter expose full decoded DeviceParam");expect(rust.fn<int32_t(*)()>(0x6c44d8)()==want[0]&&rust.fn<int32_t(*)()>(0x6c4584)()==want[1],"original X Y getters preserve signed raw words");expect(rust.at<uint8_t>(0xf1996c)==0,"original save releases shared mutex");std::cout<<"TRACE {\"input\":["<<v[0]<<","<<v[1]<<","<<v[2]<<","<<v[3]<<"],\"wire_and_saved\":["<<got[0]<<","<<got[1]<<","<<got[2]<<","<<got[3]<<"],\"funcid\":"<<sent_registers[2]<<"}\n";free(enqueued);enqueued=nullptr;}
 // Smi/Mint variation belongs to _addAll's count, never to DeviceParam payload.
 for(int repr=0;repr<3;repr++){++cases;DartHeap h;h.temp={0x12,0x34,0x56,0x78};h.put<int64_t>(h.mint,7,4);const auto value=repr==0?uintptr_t(8):repr==1?h.mint:h.n;h.call(dart,0x86ead4,h.w,h.u,0,h.d,value);expect(h.get<int64_t>(h.w,0xb)==4&&!memcmp(h.output.data()+16,h.temp.data(),4),"original addAll accepts Smi4 Mint4 or null default4");}
 // The decoder does not commit partially read output. Panic is a fixture trap,
 // not a claim about Rust unwinding or Dart's exception transport.
 for(unsigned n=0;n<16;n++){++cases;std::array<uint8_t,32>wire{};Params result{111,222,333,444};std::array<uint64_t,5>reader{n,uintptr_t(wire.data()),n,0,n};expect(!try_decode(&rust,&result,reader.data()),"truncated decoder reaches panic adapter");expect(result==Params({111,222,333,444}),"truncated full decoder does not partially commit");expect(reader[3]==n,"truncated scalar clamps cursor to available length");}
 for(unsigned extra=0;extra<2;extra++){++cases;std::array<uint8_t,20>wire{};Params wanted{1,-1,4,6};memcpy(wire.data(),wanted.data(),16);unsigned before=enqueue_count;const int cap=17;const int data_length=16+extra;if(try_frb(&rust,wire.data(),cap,data_length)){expect(extra==0,"full FRB rejects trailing declared data");expect(enqueue_count==before+1&&freed_cap==17,"capacity17 dataLength16 accepted distinctly");free(enqueued);enqueued=nullptr;}else{expect(extra==1&&enqueue_count==before,"end mismatch rejects before enqueue");}}
 // Exact-capacity growth and invalid metadata reach real error boundaries.
 for(int bad=0;bad<3;bad++){++cases;DartHeap h;h.fields({1,2,99,6});if(bad==0)h.put<int64_t>(h.b,0xb,16);if(bad==1)h.put<uint32_t>(h.t,0x13,6);if(bad==2)h.put<uint32_t>(h.u,0x13,6);expect(!try_dart(&dart,&h),"invalid buffer metadata reaches explicit external edge trap");expect(h.get<int64_t>(h.w,0xb)==(bad==0?12:0),"no logical advance after failed writer edge");}
 dart.intact();rust.intact();
 for(auto [name,image]:std::array<std::pair<const char*,Image*>,2>{{{"dart",&dart},{"rust",&rust}}}){std::cout<<"PATCHES {\"image\":\""<<name<<"\",\"ranges\":[";bool first=true;for(auto [va,n]:image->patches){if(!first)std::cout<<",";first=false;std::cout<<"["<<va<<","<<n<<"]";}std::cout<<"]}\n";}

 std::cout<<"{\"passed\":true,\"checks\":"<<checks<<",\"cases\":"<<cases<<",\"device_status\":\"NOT_TESTED\"}\n";
}
