// Executes complete OTA bodies, with explicit Dart allocation/service/legality
// adapters. Ready-result async stubs are explicit; no VM or durable SQLite commit.
#include "image.h"
#include <map>
static unsigned checks=0,cases=0;
void expect(bool v,const char*s){++checks;if(!v){std::cerr<<"FAILED "<<s<<" case="<<cases<<"\n";exit(3);}}
struct DartCall{uintptr_t entry,x1,x2,x3,x4,x5,x6,stack,pool,heap,null,thread,dispatch,result,returned_stack,x7;};
extern "C" void call_dart(DartCall*);extern "C" void adapter();
struct Heap{
 uint8_t*mem;size_t cursor=0x1000;uintptr_t n,controller,config,grid,descriptor,range,rule;
 std::vector<uint8_t>pool=std::vector<uint8_t>(0x60000),thread=std::vector<uint8_t>(0x100),statics=std::vector<uint8_t>(0x5000);
 std::array<uint64_t,2048>stack{};std::vector<uintptr_t>dispatch=std::vector<uintptr_t>(190000,uintptr_t(adapter));bool rtl=false,full_width=false;
 template<class T>void put(uintptr_t p,size_t off,T v){memcpy((void*)(p+off),&v,sizeof v);}template<class T>T get(uintptr_t p,size_t off){T v;memcpy(&v,(void*)(p+off),sizeof v);return v;}
 uintptr_t obj(size_t size=256){size=(size+15)&~size_t(15);assert(cursor+size<0x1000000);auto p=uintptr_t(mem+cursor)+1;cursor+=size;return p;}
 void ref(uintptr_t p,size_t off,uintptr_t q){put<uint32_t>(p,off,uint32_t(q));}
 uintptr_t list(const std::vector<uintptr_t>&v){auto a=obj(32+256*4),l=obj();put<uint64_t>(l,size_t(-1),92ULL<<12);ref(l,7,n);put<uint32_t>(a,0xb,512);for(size_t j=0;j<v.size();j++)ref(a,0xf+j*4,v[j]);put<uint32_t>(l,0xb,v.size()*2);ref(l,0xf,a);return l;}
 Heap(){mem=(uint8_t*)mmap((void*)0x5000000000ULL,0x1000000,3,MAP_PRIVATE|MAP_ANONYMOUS|MAP_FIXED_NOREPLACE,-1,0);assert(mem!=MAP_FAILED);assert(uintptr_t(mem)>>32==(uintptr_t(mem)+0xffffff)>>32);n=uintptr_t(mem)+1;controller=obj(512);config=obj();grid=obj();descriptor=obj();range=list({0,0});rule=obj();ref(controller,0x19f,range);ref(controller,0x4f,rule);put<uint64_t>(rule,size_t(-1),0x1000ULL<<12);dispatch[4096]=uintptr_t(adapter);auto st=uintptr_t(statics.data());memcpy(thread.data()+0x78,&st,8);put<uintptr_t>(uintptr_t(statics.data()),0x2710,grid);put<uintptr_t>(uintptr_t(statics.data()),0x1cd0,n);put<uintptr_t>(uintptr_t(pool.data()),0x40,n+0x80);put<uintptr_t>(uintptr_t(pool.data()),0xedf8,n+0x100);ref(descriptor,0x1f,n+0x100);put<uint32_t>(descriptor,0x13,2);put<uint32_t>(descriptor,0x23,0);}
 ~Heap(){munmap(mem,0x1000000);}
 void dimensions(int x,int y){put<int64_t>(config,0x1b,x);put<int64_t>(config,0x23,y);}
 void occupied(const std::vector<std::array<int,4>>&rects){std::vector<uintptr_t>v;for(auto a:rects){auto model=obj(),cell=obj();put<int64_t>(model,0x1f,a[2]);put<int64_t>(model,0x27,a[3]);put<int64_t>(model,0x37,a[0]);put<int64_t>(model,0x3f,a[1]);ref(cell,7,model);put<int64_t>(cell,0xb,a[0]);put<int64_t>(cell,0x13,a[1]);v.push_back(cell);}ref(controller,0xdb,list(v));}
 uintptr_t call(Image&i,size_t va,uintptr_t a=0,uintptr_t b=0,uintptr_t c=0,uintptr_t d=0,uintptr_t e=0,uintptr_t f=0,bool force=false,uintptr_t g=0,const std::vector<uintptr_t>&args={}){
  std::array<uintptr_t,4096> local{};auto top=uintptr_t(&local[3800]);local[3800]=n+(force?0x20:0x30);for(size_t j=0;j<args.size();++j)local[3800+j]=args[j];
  DartCall call{uintptr_t(i.base+va),a,b,c,d,e,f,top,uintptr_t(pool.data()),uintptr_t(mem)>>32,n,uintptr_t(thread.data()),uintptr_t(dispatch.data()+4096),0,0,g};call_dart(&call);expect(call.returned_stack==top,"original Dart stack restored");return call.result;
 }
 uintptr_t ptr(uintptr_t p,size_t off){return (uintptr_t(mem)&~uintptr_t(0xffffffff))|get<uint32_t>(p,off);}
 std::vector<uintptr_t> values(uintptr_t p){std::vector<uintptr_t>v;auto a=ptr(p,0xf);auto len=get<uint32_t>(p,0xb)/2;assert(len<256);for(unsigned j=0;j<len;++j)v.push_back(ptr(a,0xf+j*4));return v;}
 void append(uintptr_t p,uintptr_t v){auto len=get<uint32_t>(p,0xb)/2;assert(len<256);ref(ptr(p,0xf),0xf+len*4,v);put<uint32_t>(p,0xb,(len+1)*2);}
 std::array<int64_t,2> coord(uintptr_t l){if(l==n)return {-999,-999};auto a=(uintptr_t(mem)&~uintptr_t(0xffffffff))|get<uint32_t>(l,0xf);return {int32_t(get<uint32_t>(a,0xf))>>1,int32_t(get<uint32_t>(a,0x13))>>1};}
};
static Heap*h;static Image*img;static uintptr_t image_base;static std::map<unsigned,unsigned> adapter_calls;
static std::map<uintptr_t,std::pair<size_t,uintptr_t>> closures;
static std::map<uintptr_t,std::pair<uintptr_t,uintptr_t>> mapped;
static std::vector<uintptr_t> submitted;
static std::map<uintptr_t,std::map<uintptr_t,uintptr_t>> dictionaries;
static uintptr_t screen_list,workspace,classifier;static unsigned batches=0;static bool large_id=false;
static size_t closure_entry(uintptr_t f){return h->get<uintptr_t>(f,7);}
static uintptr_t invoke(uintptr_t cl,uintptr_t arg){auto v=closures.at(cl);return h->call(*img,v.first,0,0,0,0,0,0,false,0,{arg,cl});}
extern "C" void handle_adapter(uint64_t*r){unsigned id=r[17];auto lr=r[30]-image_base;
 if(lr==0xaa9474){r[0]=h->n+0x30;return;}
 if(lr==0x898ce8||lr==0x8d196c)id=99;
 if(lr==0x1955b70||lr==0x1955ba0||lr==0x1955bd4){r[0]=h->get<uint32_t>(h->get<uintptr_t>(r[15],0),0xb);return;}
 if(lr==0x1955c20){auto l=h->get<uintptr_t>(r[15],8);auto j=h->get<uintptr_t>(r[15],0)>>1;r[0]=h->values(l).at(j);return;}
 if(lr==0x1955fbc){auto l=h->get<uintptr_t>(r[15],8),v=h->get<uintptr_t>(r[15],0);h->append(l,v);r[0]=h->n;return;}
 if(lr==0x1956038){auto l=r[1],cl=r[2];std::vector<uintptr_t>v;for(auto p:h->values(l))if(invoke(cl,p)==h->n+0x20)v.push_back(p);r[0]=h->list(v);return;}
 if(lr==0x1956064){r[0]=h->list(h->values(r[1]));return;}
 ++adapter_calls[id];
 if(getenv("TRACE_ADAPTER"))std::cerr<<"adapter "<<id<<" lr="<<std::hex<<lr<<std::dec<<"\n";
 switch(id){
 case 97:r[0]=h->obj(32);break; // Mint allocation; actual caller writes payload.
 case 1:{auto a=h->obj(32+(r[2]>>1)*4);h->put<uint32_t>(a,0xb,r[2]);r[0]=a;break;}
 case 2:r[0]=h->obj(512);break;
 case 3:{auto a=h->obj(64+r[1]*4);h->put<uint32_t>(a,7,r[1]);r[0]=a;break;}
 case 4:{auto a=h->obj();memcpy((void*)(a-1),(void*)(r[5]-1),128);r[0]=a;break;}
 case 5:r[0]=h->grid;break;
 case 6:r[0]=h->n+(h->rtl?0x20:0x30);break;
 case 7:case 8:r[0]=h->config;break;
 case 10:r[0]=h->n+0x200;break;
 case 11:r[0]=h->n;break;
 case 12:r[0]=h->n+(h->full_width?0x20:0x30);break;
 case 13:{auto p=h->obj();h->ref(p,0x17,r[2]);closures[p]={closure_entry(r[1]),r[2]};r[0]=p;break;}
 case 14:break; // AsyncStart: already-ready fixture, frame remains owned by original body.
 case 15:r[0]=r[1];break; // Await: immediate completed-result only, no scheduler or continuation interleave.
 case 16:{auto fp=r[29];r[29]=h->get<uintptr_t>(fp,0);r[30]=h->get<uintptr_t>(fp,8);r[15]=fp+16;break;} // AsyncReturn frame unwind.
 case 17:r[0]=h->n+0x20;break; // Workspace service exists.
 case 18:r[0]=h->list({});break; // growable list constructor
 case 19:h->put<uint32_t>(r[1],0xb,0);r[0]=h->n;break;
 case 20:r[0]=h->list(h->values(r[1]));break;
 case 21:{auto l=r[1],cl=r[2];auto vals=h->values(l);h->put<uint32_t>(l,0xb,0);for(auto v:vals)if(invoke(cl,v)==h->n+0x30)h->append(l,v);r[0]=h->n;break;} // removeWhere with real predicate
 case 22:r[0]=screen_list;break; // RxList workspace value; only expected caller.
 case 23:{auto&d=dictionaries[r[1]];auto k=r[2];if(!d.count(k))d[k]=invoke(r[3],h->n);r[0]=d[k];break;}
 case 24:{auto&d=dictionaries[r[1]];r[0]=d.count(r[2])?d[r[2]]:h->ptr(r[1],0xf);break;}
 case 25:r[0]=0;break; // hash irrelevant to deterministic test collection adapter
 case 26:r[0]=h->n+0x20;break; // linked set insertion, UI screen dispatch below deliberately excluded
 case 27:{auto p=h->obj();h->ref(p,7,h->n);r[0]=p;break;}
 case 28:r[0]=h->n+0x30;break; // post-batch UI iterator has no subscribed screens in fixture
 case 29:{auto p=h->obj();mapped[p]={h->get<uintptr_t>(r[15],8),h->get<uintptr_t>(r[15],0)};r[0]=p;break;}
 case 30:{auto pair=mapped.at(r[1]);std::vector<uintptr_t>v;for(auto x:h->values(pair.first))v.push_back(invoke(pair.second,x));r[0]=h->list(v);break;}
 case 31:{submitted=h->values(r[2]);++batches;r[0]=h->n;break;} // exact FRB producer boundary capture, no SQLite or transport execution
 case 32:{auto p=h->obj();dictionaries[p]={};r[0]=p;break;} // Bundle constructor
 case 33:dictionaries[r[1]][r[2]]=r[3];r[0]=h->n;break; // Bundle.put* retains actual original key/value
 case 99:r[0]=h->n+0x20;break;
 default:std::cerr<<"unadapted id="<<id<<" lr="<<std::hex<<lr<<"\n";exit(4);
 }}
static void patch_adapter(Image&i,size_t va,unsigned id){size_t v=i.veneer;i.veneer+=32;i.word(v,0xd2800011|(id<<5));i.jump(v+4,uintptr_t(adapter));i.word(va,0x14000000|(uint32_t((v-va)/4)&0x3ffffff));}
static void init_pool(){for(size_t at=0;at+8<h->pool.size();at+=8)h->put<uintptr_t>(uintptr_t(h->pool.data()),at,h->n);h->put<uintptr_t>(uintptr_t(h->pool.data()),0x40,h->n+0x80);
 for(size_t off:{0x169c8,0x169d0,0x169d8,0x169e0,0x169e8,0x169f0,0x169f8,0x16a00,0x16a08,0x16a10,0x16a18,0x97b8,0x97c0})h->put<uintptr_t>(uintptr_t(h->pool.data()),off,h->obj());
 for(auto[off,va]:std::vector<std::pair<size_t,size_t>>{{0x2e210,0x99a79c},{0x2e298,0x8e0b24},{0x2e2a0,0x8f6f24},{0x2e3a8,0xaef838}}){auto f=h->obj();h->put<uintptr_t>(f,7,va);h->put<uintptr_t>(uintptr_t(h->pool.data()),off,f);}
 auto descriptor=h->obj();h->put<uint32_t>(descriptor,0x13,2);h->put<uint32_t>(descriptor,0x23,0);auto token=h->n+0x100;h->ref(descriptor,0x1f,token);h->put<uintptr_t>(uintptr_t(h->pool.data()),0xedf8,token);h->put<uintptr_t>(uintptr_t(h->pool.data()),0x11ca8,descriptor);
 // Non-business logger closure tokens point to harmless sentinel; these closures are never invoked.
 h->dispatch[4096+2030-0xfbb]=image_base+0x18525f8;h->dispatch[4096+2021-0xfbb]=image_base+0x1852c34;
}
static uintptr_t make_maml(){auto device=h->obj(512),dc=h->obj(),dto=h->obj(),model=h->obj(512);h->put<uint64_t>(model,size_t(-1),2030ULL<<12);h->ref(device,0x1f,dc);h->ref(device,0xeb,h->n+0x30);h->put<int64_t>(dc,0x6b,4);h->put<uintptr_t>(uintptr_t(h->statics.data()),0x26d8,device);h->put<uintptr_t>(uintptr_t(h->statics.data()),0x42e8,h->n+0x200);
 for(size_t at=7;at<0x98;at+=4)h->ref(dto,at,h->n);
 for(auto[at,v]:std::vector<std::pair<size_t,int>>{{7,101},{0xf,19},{0x17,4},{0x1b,2},{0x1f,1},{0x23,0},{0x27,0},{0x2b,-100}})h->ref(dto,at,uint32_t(v*2));
 if(large_id){auto m=h->obj();h->put<int64_t>(m,7,0x100000000LL+101);h->ref(dto,7,m);}
 const std::vector<uint8_t> original((uint8_t*)(dto-1),(uint8_t*)(dto-1)+256);
 h->call(*img,0xaa8cf4,model,dto);expect(!memcmp(original.data(),(void*)(dto-1),256),"original ItemInfo unchanged by constructor");return model;
}
int main(int argc,char**argv){assert(argc>=2);Image i(argv[1]);img=&i;image_base=uintptr_t(i.base);
 for(auto [va,id]:std::vector<std::pair<size_t,unsigned>>{{0x1b57c08,1},{0x1b56b3c,2},{0x1b56b78,3},{0x1b56558,4},{0xb1f9a0,5},{0x891610,6},{0xb1a270,7},{0x17d0f8c,8},{0x1b58238,98},{0x1b57e90,97},{0x1b55d50,96},{0x1b55cac,95},{0x868b8c,10},{0x1b56f3c,13},{0x89c194,11},{0xf63834,12},{0x18ac658,14},{0x18ac41c,15},{0x18ac364,16},{0x89a718,17},{0x867e54,18},{0xb40914,19},{0xf620a4,20},{0xd15ce4,21},{0x1772958,22},{0xd1ab08,23},{0x1892b4c,24},{0x18a20c0,25},{0x8beb48,26},{0x18b01f4,2},{0x18b2524,2},{0xf69028,27},{0x1724d5c,28},{0xf67c10,29},{0xe1d83c,30},{0x8d4790,31},{0x8cd634,11},{0x8a8810,11},{0x8a379c,11},{0x86af0c,11},{0xa588ac,11},{0x8fdc64,11},{0x18b7f84,2},{0x18bfccc,2},{0x9a2c50,32},{0x9a2a0c,33},{0x9cfdc4,33},{0x9e5630,33}}){patch_adapter(i,va,id);}
 if(argc==3){std::string m=argv[2];std::map<std::string,std::array<uint32_t,3>> mutations={{"skip-batch",{0x1955788,0x97bdfbb4,0xd503201f}},{"skip-position-x",{0x8f89a0,0xf8037026,0xd503201f}},{"lose-id",{0x1852cfc,0xb8007062,0xb800707f}},{"lose-maml-original-span",{0x1852a04,0xb808b061,0xb808b07f}},{"lose-maml-display-span",{0x1852934,0xb8017062,0xb801707f}},{"drop-nonstack",{0xaef874,0x9100c2c0,0x910082c0}}};auto [va,original,word]=mutations.at(m);expect(i.at<uint32_t>(va)==original,"mutation precondition original opcode");i.word(va,word);}
 i.sync();
 for(int rows:{4,13})for(bool rtl:{false,true})for(int scenario:{0,1,2})for(bool big:{false,true}){bool pending_maml=scenario!=0;large_id=big;++cases;Heap heap;h=&heap;closures.clear();mapped.clear();dictionaries.clear();submitted.clear();batches=0;h->rtl=rtl;h->dimensions(3,rows);init_pool();
 auto maml=make_maml();auto favorite=h->obj(512);h->put<uint64_t>(favorite,size_t(-1),2021ULL<<12);for(size_t o:{0x5f,0x63,0x67})h->ref(favorite,o,h->n);h->put<int64_t>(favorite,7,(big?0x100000000LL:0)+201);h->put<int64_t>(favorite,0xf,0);h->put<int64_t>(favorite,0x1f,1);h->put<int64_t>(favorite,0x27,1);h->put<int64_t>(favorite,0x2f,1);h->put<int64_t>(favorite,0x37,3);h->put<int64_t>(favorite,0x47,-100);
 auto cell=h->obj();h->ref(cell,7,pending_maml?maml:favorite);classifier=h->obj();auto pending=h->list({cell});h->ref(classifier,0x1f,pending);h->ref(classifier,0x23,h->n+0x30);auto map=h->obj();h->ref(map,0xf,h->obj());h->ref(classifier,0x17,map);
 h->occupied(scenario==2?std::vector<std::array<int,4>>{{0,0,1,1}}:pending_maml?std::vector<std::array<int,4>>{}:std::vector<std::array<int,4>>{{0,0,3,2}});h->ref(h->controller,0x16b,h->n+0x20);h->put<int64_t>(h->controller,0x2f,0); // inactive cell view: original records invalidation, no rendered-cell rebuild
 auto screen=h->obj();h->put<int64_t>(screen,7,1);h->ref(screen,0x1b,h->controller);screen_list=h->list({screen});workspace=h->obj();h->ref(workspace,0x37,h->obj());h->put<uintptr_t>(uintptr_t(h->statics.data()),0x2900,workspace);
 auto manager=h->obj(),model=h->obj(),rustlib=h->obj(),rustholder=h->obj();h->ref(manager,0x13,model);h->ref(rustlib,0xb,rustholder);h->ref(rustholder,0x13,h->obj());h->put<uintptr_t>(uintptr_t(h->statics.data()),0x2538,manager);h->put<uintptr_t>(uintptr_t(h->statics.data()),0x23c0,rustlib);
 if(scenario==0||scenario==2){auto result=h->call(i,0x898c2c,h->controller,scenario==0?6:0,0,0,scenario==0?2:6,scenario==0?2:4);expect(result==h->n+0x20,"original bounds or collision marks selected initial position invalid");}
 h->call(i,0x19553c8,classifier);
 expect(batches==1&&submitted.size()==1,"original flush reaches one real batch producer with one item");auto dto=submitted.at(0);auto rawid=h->get<uint32_t>(dto,7);int64_t id=(rawid&1)?h->get<int64_t>(h->ptr(dto,7),7):(int32_t(rawid)>>1);expect(id==(big?0x100000000LL:0)+(pending_maml?101:201),"batch preserves original identity");
 auto x=int32_t(h->get<uint32_t>(dto,0x23))>>1,y=int32_t(h->get<uint32_t>(dto,0x27))>>1;expect(x==(pending_maml?0:rtl?2:0)&&y==(scenario==2?1:pending_maml?0:2),"batch coordinates equal original placement result");expect(h->get<uint32_t>(pending,0xb)==0,"original flush clears pending list");expect(h->ptr(classifier,0x23)==h->n+0x30,"original flush resets running flag");
 auto selected=pending_maml?maml:favorite;expect(h->get<int64_t>(selected,7)==id,"original model id survives flush");expect(h->get<int64_t>(selected,0x37)==x&&h->get<int64_t>(selected,0x3f)==y,"submitted coordinate agrees with original mutable model");expect(h->get<int64_t>(selected,0x2f)==1&&h->get<int64_t>(selected,0x47)==-100,"original model stays on selected desktop screen");
 expect(h->get<uint32_t>(dto,0x1f)==2&&int32_t(h->get<uint32_t>(dto,0x2b))==-200,"submitted screen/container agree with original update");
 if(pending_maml){expect(h->get<uint32_t>(dto,0x17)==6&&h->get<uint32_t>(dto,0x1b)==4,"submitted MAML display span3x2");expect(h->get<uint32_t>(dto,0x8b)==8&&h->get<uint32_t>(dto,0x8f)==4,"submitted MAML original span4x2 retained");auto bundle=h->ptr(h->ptr(dto,0xb),0x1b);expect(uint32_t(dictionaries[bundle][h->get<uintptr_t>(uintptr_t(h->pool.data()),0x16a00)])==8&&uint32_t(dictionaries[bundle][h->get<uintptr_t>(uintptr_t(h->pool.data()),0x16a08)])==4,"original MAML copy also preserves resource span in Bundle");}
 std::cout<<"BATCH {\"rows\":"<<rows<<",\"rtl\":"<<rtl<<",\"scenario\":"<<scenario<<",\"maml\":"<<pending_maml<<",\"id\":"<<id<<",\"x\":"<<x<<",\"y\":"<<y<<",\"span_x\":"<<(int32_t(h->get<uint32_t>(dto,0x17))>>1)<<",\"span_y\":"<<(int32_t(h->get<uint32_t>(dto,0x1b))>>1)<<",\"original_span\":"<<(pending_maml?"[4,2]":"null")<<",\"screen\":1,\"container\":-100}\n";
 }
 i.intact();std::cout<<"ADAPTER_COUNTS {";bool count_first=true;for(auto[id,count]:adapter_calls){if(!count_first)std::cout<<",";count_first=false;std::cout<<"\""<<id<<"\":"<<count;}std::cout<<"}\n";std::cout<<"PATCHES [";bool first=true;for(auto[va,n]:i.patches){if(!first)std::cout<<",";first=false;std::cout<<"["<<va<<","<<n<<"]";}std::cout<<"]\n";std::cout<<"{\"passed\":true,\"checks\":"<<checks<<",\"cases\":"<<cases<<",\"device_status\":\"NOT_TESTED\"}\n";
}
