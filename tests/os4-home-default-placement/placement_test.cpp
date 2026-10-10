// Executes complete OTA bodies, with explicit Dart allocation/service/legality
// adapters. No VM, SQLite, XML parser or asynchronous scheduling is emulated.
#include "image.h"
#include <map>
static unsigned checks=0,cases=0;
void expect(bool v,const char*s){++checks;if(!v){std::cerr<<"FAILED "<<s<<" case="<<cases<<"\n";exit(3);}}
struct DartCall{uintptr_t entry,x1,x2,x3,x4,x5,x6,stack,pool,heap,null,thread,dispatch,result,returned_stack,x7;};
extern "C" void call_dart(DartCall*);extern "C" void adapter();
struct Heap{
 uint8_t*mem;size_t cursor=0x1000;uintptr_t n,controller,config,grid,descriptor,range,rule;
 std::vector<uint8_t>pool=std::vector<uint8_t>(0x60000),thread=std::vector<uint8_t>(0x100),statics=std::vector<uint8_t>(0x5000);
 std::array<uint64_t,2048>stack{};std::array<uintptr_t,32>dispatch{};bool rtl=false,full_width=false;
 template<class T>void put(uintptr_t p,size_t off,T v){memcpy((void*)(p+off),&v,sizeof v);}template<class T>T get(uintptr_t p,size_t off){T v;memcpy(&v,(void*)(p+off),sizeof v);return v;}
 uintptr_t obj(size_t size=256){size=(size+15)&~size_t(15);assert(cursor+size<0x1000000);auto p=uintptr_t(mem+cursor)+1;cursor+=size;return p;}
 void ref(uintptr_t p,size_t off,uintptr_t q){put<uint32_t>(p,off,uint32_t(q));}
 uintptr_t list(const std::vector<uintptr_t>&v){auto a=obj(32+v.size()*4),l=obj();put<uint32_t>(a,0xb,v.size()*2);for(size_t j=0;j<v.size();j++)ref(a,0xf+j*4,v[j]);put<uint32_t>(l,0xb,v.size()*2);ref(l,0xf,a);return l;}
 Heap(){mem=(uint8_t*)mmap((void*)0x5000000000ULL,0x1000000,3,MAP_PRIVATE|MAP_ANONYMOUS|MAP_FIXED_NOREPLACE,-1,0);assert(mem!=MAP_FAILED);assert(uintptr_t(mem)>>32==(uintptr_t(mem)+0xffffff)>>32);n=uintptr_t(mem)+1;controller=obj(512);config=obj();grid=obj();descriptor=obj();range=list({0,0});rule=obj();ref(controller,0x19f,range);ref(controller,0x4f,rule);put<uint64_t>(rule,size_t(-1),0x1000ULL<<12);dispatch[0]=uintptr_t(adapter);auto st=uintptr_t(statics.data());memcpy(thread.data()+0x78,&st,8);put<uintptr_t>(uintptr_t(statics.data()),0x2710,grid);put<uintptr_t>(uintptr_t(statics.data()),0x1cd0,n);put<uintptr_t>(uintptr_t(pool.data()),0x40,n+0x80);put<uintptr_t>(uintptr_t(pool.data()),0xedf8,n+0x100);ref(descriptor,0x1f,n+0x100);put<uint32_t>(descriptor,0x13,2);put<uint32_t>(descriptor,0x23,0);}
 ~Heap(){munmap(mem,0x1000000);}
 void dimensions(int x,int y){put<int64_t>(config,0x1b,x);put<int64_t>(config,0x23,y);}
 void occupied(const std::vector<std::array<int,4>>&rects){std::vector<uintptr_t>v;for(auto a:rects){auto model=obj(),cell=obj();put<int64_t>(model,0x1f,a[2]);put<int64_t>(model,0x27,a[3]);put<int64_t>(model,0x37,a[0]);put<int64_t>(model,0x3f,a[1]);ref(cell,7,model);put<int64_t>(cell,0xb,a[0]);put<int64_t>(cell,0x13,a[1]);v.push_back(cell);}ref(controller,0xdb,list(v));}
 uintptr_t call(Image&i,size_t va,uintptr_t a,uintptr_t b,uintptr_t c,uintptr_t d,uintptr_t e=0,uintptr_t f=0,bool force=false,uintptr_t g=0){auto top=uintptr_t(&stack[1800]);stack[1800]=n+(force?0x20:0x30);DartCall call{uintptr_t(i.base+va),a,b,c,d,e,f,top,uintptr_t(pool.data()),uintptr_t(mem)>>32,n,uintptr_t(thread.data()),uintptr_t(dispatch.data()),0,0,g};call_dart(&call);expect(call.returned_stack==top,"original Dart stack restored");return call.result;}
 std::array<int64_t,2> coord(uintptr_t l){if(l==n)return {-999,-999};auto a=(uintptr_t(mem)&~uintptr_t(0xffffffff))|get<uint32_t>(l,0xf);return {int32_t(get<uint32_t>(a,0xf))>>1,int32_t(get<uint32_t>(a,0x13))>>1};}
};
static Heap*h;static uintptr_t image_base;static std::map<unsigned,unsigned> adapter_calls;
extern "C" void handle_adapter(uint64_t*r){unsigned id=r[17];if(r[30]-image_base==0xaa9474){r[0]=h->n+0x30;return;}if(r[30]-image_base==0x898ce8||r[30]-image_base==0x8d196c)id=99;++adapter_calls[id];switch(id){
 case 1:{auto a=h->obj(32+(r[2]>>1)*4);h->put<uint32_t>(a,0xb,r[2]);r[0]=a;break;} // AllocateArray tagged length
 case 2:r[0]=h->obj();break; // AllocateGrowableArray
 case 3:{auto a=h->obj(32+r[1]*4);h->put<uint32_t>(a,7,r[1]);r[0]=a;break;} // AllocateContext, preserve registers
 case 4:{auto a=h->obj();memcpy((void*)(a-1),(void*)(r[5]-1),64);r[0]=a;break;} // CloneContext
 case 5:r[0]=h->grid;break; // service locator
 case 6:r[0]=h->n+(h->rtl?0x20:0x30);break; // AppService RTL
 case 7:case 8:r[0]=h->config;break; // currentConfig / Rx.value
 case 10:r[0]=h->n+0x200;break; // string interpolation result, content unused by exercised geometry
 case 11:r[0]=h->n;break; // logger / logger callback allocation; logger never invokes callback
 case 12:r[0]=h->n+(h->full_width?0x20:0x30);break; // resource full-screen allowlist
 case 99:r[0]=h->n+0x20;break; // external drop-rule accepts coordinates; actual bounds/occupancy still execute
 default:std::cerr<<"unadapted id="<<id<<" lr="<<std::hex<<r[30]-image_base<<"\n";exit(4);
 }}
static void patch_adapter(Image&i,size_t va,unsigned id){size_t v=i.veneer;i.veneer+=32;i.word(v,0xd2800011|(id<<5));i.jump(v+4,uintptr_t(adapter));i.word(va,0x14000000|(uint32_t((v-va)/4)&0x3ffffff));}
int main(int argc,char**argv){assert(argc==2||argc==3);Image i(argv[1]);image_base=uintptr_t(i.base);for(auto [va,id]:std::vector<std::pair<size_t,unsigned>>{{0x1b57c08,1},{0x1b56b3c,2},{0x1b56b78,3},{0x1b56558,4},{0xb1f9a0,5},{0x891610,6},{0xb1a270,7},{0x17d0f8c,8},{0x1b58238,98},{0x1b57e90,97},{0x1b55d50,96},{0x1b55cac,95},{0x868b8c,10},{0x1b56f3c,11},{0x89c194,11},{0xf63834,12}})patch_adapter(i,va,id);
 if(argc==3){std::string m=argv[2];if(m=="ignore-x-bound"){expect(i.at<uint32_t>(0x898dd4)==0x54000d6a,"exact bound mutation word");i.word(0x898dd4,0xd503201f);}else if(m=="force-no-fallback"){expect(i.at<uint32_t>(0x8ddec8)==0x37200063,"exact fallback mutation word");i.word(0x8ddec8,0x14000003);}else if(m=="skip-maml-clamp"){expect(i.at<uint32_t>(0xaa90c4)==0x94000015,"exact constructor clamp call");i.word(0xaa90c4,0xd503201f);}else if(m=="lose-original-span"){expect(i.at<uint32_t>(0xaa91c8)==0xb8316840,"exact original-span store");i.word(0xaa91c8,0xb831685f);}else assert(false);}i.sync();
 for(int columns:{3,4,5,9})for(int rows:{4,6,13})for(bool rtl:{false,true})for(int populated:{0,1})for(auto span:std::vector<std::array<int,2>>{{1,1},{4,2},{2,2},{9,13}})for(bool force:{false,true}){++cases;Heap heap;h=&heap;h->rtl=rtl;h->dimensions(columns,rows);h->occupied(populated?std::vector<std::array<int,4>>{{0,0,1,1}}:std::vector<std::array<int,4>>{});auto p=h->call(i,0x8dd2dc,h->controller,span[0],span[1],h->descriptor,0,0,force);auto xy=h->coord(p);bool occupied=false;if(p!=h->n){auto q=h->call(i,0x898c2c,h->controller,uintptr_t(xy[0]*2),uintptr_t(xy[1]*2),0,span[0]*2,span[1]*2);occupied=q==h->n+0x20;}
 if(!populated){if(force){expect(p!=h->n,"actual force flag returns fallback even empty region list");expect(xy==std::array<int64_t,2>{rtl?columns-span[0]:0,0},"actual forced fallback coordinate");expect(occupied==(span[0]>columns||span[1]>rows),"actual occupancy rejects overwide or overtall fallback");}else expect(p==h->n,"actual default flag returns null for empty region list");}
 if(populated&&p!=h->n)expect(!occupied,"occupied-record result must pass original bounds and occupancy");
 if(populated&&(span[0]>columns||span[1]>rows))expect(p==h->n,"nonempty screen rejects span larger than current grid");
 std::cout<<"TRACE {\"grid\":["<<columns<<","<<rows<<"],\"span\":["<<span[0]<<","<<span[1]<<"],\"rtl\":"<<rtl<<",\"records\":"<<populated<<",\"force\":"<<force<<",\"coordinate\":["<<xy[0]<<","<<xy[1]<<"],\"occupied_or_outside\":"<<occupied<<"}\n";
 }

 // Actual default-item MAML constructor and both base constructors execute;
 // XML/SQLite decoding is represented by the already-decoded ItemInfo fields.
 for(int columns:{3,4,5,9})for(int rows:{4,13})for(int threshold:{3,4,5})for(bool full:{false,true})for(bool boxed:{false,true}){
  ++cases;Heap heap;h=&heap;h->dimensions(columns,rows);h->full_width=full;h->occupied({});
  for(size_t at=0;at+8<h->pool.size();at+=8)h->put<uintptr_t>(uintptr_t(h->pool.data()),at,h->n);
  h->put<uintptr_t>(uintptr_t(h->pool.data()),0x40,h->n+0x80);
  auto device=h->obj(512),dc=h->obj(),dto=h->obj(),model=h->obj(512);
  h->ref(device,0x1f,dc);h->ref(device,0xeb,h->n+0x30);h->put<int64_t>(dc,0x6b,threshold);
  h->put<uintptr_t>(uintptr_t(h->statics.data()),0x26d8,device);
  h->put<uintptr_t>(uintptr_t(h->statics.data()),0x42e8,h->n+0x200);
  for(size_t at=7;at<0x98;at+=4)h->ref(dto,at,h->n);
  auto number=[&](int v){if(!boxed)return uintptr_t(uint32_t(v*2));auto m=h->obj();h->put<int64_t>(m,7,v);return m;};
  h->ref(dto,7,number(101));h->ref(dto,0xf,number(19));h->ref(dto,0x17,number(4));h->ref(dto,0x1b,number(2));h->ref(dto,0x1f,number(1));h->ref(dto,0x23,number(0));h->ref(dto,0x27,number(0));h->ref(dto,0x2b,number(-100));
  const std::vector<uint8_t>before((uint8_t*)(dto-1),(uint8_t*)(dto-1)+256);
  auto r=h->call(i,0xaa8cf4,model,dto,0,0);
  expect(r==h->n,"original MAML ctor normal return");
  int wantx=threshold>4?4:full?columns:std::min(4,columns);
  expect(h->get<int64_t>(model,0x1f)==wantx&&h->get<int64_t>(model,0x27)==2,"original MAML ctor uses phone span policy");
  expect(h->get<uint32_t>(model,0x14b)==8&&h->get<uint32_t>(model,0x14f)==4,"original MAML original spans retained independent of display span");
  expect(h->get<int64_t>(model,7)==101&&h->get<int64_t>(model,0xf)==19&&h->get<int64_t>(model,0x2f)==1&&h->get<int64_t>(model,0x47)==-100,"original MAML identity screen and container retained");
  expect(!memcmp(before.data(),(void*)(dto-1),before.size()),"original decoded ItemInfo bytes not rewritten by constructor");
  std::cout<<"MAML {\"grid\":["<<columns<<","<<rows<<"],\"threshold\":"<<threshold<<",\"full_width\":"<<full<<",\"mint\":"<<boxed<<",\"display_span\":["<<wantx<<",2],\"original_span\":[4,2]}\n";
 }
 // Default XML favorite x=3 is illegal in three columns but has a valid
 // alternative returned by the actual original placement core.
 for(bool rtl:{false,true}){++cases;Heap heap;h=&heap;h->rtl=rtl;h->dimensions(3,6);h->occupied({{0,0,3,2}});
  auto occupied=h->call(i,0x898c2c,h->controller,6,0,0,2,2);expect(occupied==h->n+0x20,"original x3 favorite rejects in three-column occupancy");
  auto p=h->call(i,0x8dd2dc,h->controller,1,1,h->descriptor,0,0,true);auto xy=h->coord(p);expect(p!=h->n&&xy==std::array<int64_t,2>{rtl?2:0,2},"original favorite moves below clamped MAML at first free row");
  auto model=h->obj(),cell=h->obj();h->ref(cell,7,model);h->put<int64_t>(model,7,201);h->put<int64_t>(model,0x1f,1);h->put<int64_t>(model,0x27,1);h->put<int64_t>(model,0x37,3);
  h->call(i,0x8f8990,0,cell,1,0,uintptr_t(-100),xy[0],false,xy[1]);
  expect(h->get<int64_t>(model,7)==201&&h->get<int64_t>(model,0x1f)==1&&h->get<int64_t>(model,0x27)==1,"original position updater preserves favorite identity and span");
  expect(h->get<int64_t>(model,0x2f)==1&&h->get<int64_t>(model,0x47)==-100&&h->get<int64_t>(model,0x37)==xy[0]&&h->get<int64_t>(model,0x3f)==xy[1],"original position updater commits screen container and new coordinates");
  std::cout<<"FAVORITE {\"old_xy\":[3,0],\"span\":[1,1],\"new_xy\":["<<xy[0]<<","<<xy[1]<<"],\"rtl\":"<<rtl<<"}\n";
 }
 i.intact();std::cout<<"PATCHES [";bool first=true;for(auto [va,n]:i.patches){if(!first)std::cout<<",";first=false;std::cout<<"["<<va<<","<<n<<"]";}std::cout<<"]\n";
 std::cout<<"{\"passed\":true,\"checks\":"<<checks<<",\"cases\":"<<cases<<",\"device_status\":\"NOT_TESTED\"}\n";
}
