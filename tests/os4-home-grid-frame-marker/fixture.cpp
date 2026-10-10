// Original callers + original callee capture, and bounded immutable router experiment.
#include <tuple>
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wreturn-type"
#define main frozen_singlepoint_main
#include "../os4-home-grid-singlepoint/fixture_v2.cpp"
#undef main
#pragma GCC diagnostic pop
extern "C" void routing_dispatch(),routing_return(),routing_variable_record(),routing_natural_dispatch();
extern "C" const unsigned char grid_router_begin[],grid_router_end[],grid_router_gate[],grid_router_targets[],grid_router_callers[],grid_router_captured[],grid_router_payload_store[];
static uint32_t branch(size_t from,size_t to,bool link=false){const auto d=int64_t(to)-int64_t(from);assert(d%4==0&&d>=-0x8000000&&d<0x8000000);return(link?0x94000000u:0x14000000u)|(uint32_t(d/4)&0x3ffffff);}
static void setup(Heap&h,double dpr=1){
 const auto thread=uintptr_t(h.thread.data()),table=uintptr_t(h.table.data());
 h.put<double>(h.handler,7,120);h.put<double>(h.handler,0xf,800);
 h.put<uint64_t>(h.handler,size_t(-1),uint64_t(0x785)<<12);
 h.ptr(h.handler,0xdb,h.n+0x30);h.ptr(h.handler,0xdf,h.n+0x30);h.ptr(h.handler,0xe3,h.n+0x30);
 h.put<uintptr_t>(table,0x26f0,h.other);h.ptr(h.other,7,h.other);h.put<uintptr_t>(table,0x1d30,h.n);
 for(unsigned off:std::array<unsigned,4>{0x3f,0x43,0x7f,0x83}){auto rx=h.obj(0x900+off*16),box=h.obj(0x2000+off*8);h.ptr(h.handler,off,rx);h.put<double>(box,7,0);h.ptr(rx,0x13,box);}
 auto rowrx=h.get<uint32_t>(h.handler,0x43)|(uintptr_t(h.mem)&~uintptr_t(0xffffffff));h.put<uint32_t>(rowrx,0x13,12);
 auto enumobj=h.obj(0x2900);h.put<int64_t>(enumobj,7,0);h.put<uintptr_t>(table,0x4ee0,enumobj);
 auto descriptor=h.obj(0x2a00),provider=h.obj(0x2b00),view=h.obj(0x2c00);
 h.pp(0x418,descriptor);h.put<uint32_t>(descriptor,0x13,2);h.ptr(descriptor,0x1f,h.n);h.pp(0x276b8,h.other+0x10);
 h.put<uintptr_t>(table,0xa08,provider);h.ptr(provider,0x17,h.n);h.put<uint64_t>(provider,size_t(-1),uint64_t(0x2390)<<12);h.ptr(provider,0x13,view);h.put<double>(view,0x13,dpr);
 h.put<uintptr_t>(thread,0x88,provider);h.put<uintptr_t>(thread,0x750,uintptr_t(secondary_trunc));h.put<uintptr_t>(thread,0x758,uintptr_t(secondary_round));
 for(auto [off,bits]:std::array<std::pair<size_t,uint64_t>,3>{{{0x27708,4607569728367971271ULL},{0x27710,4596373779694328216ULL},{0x27718,4590861373750426736ULL}}})h.pp(off,bits);
}
extern "C" void marked_dispatch();
#define REGION(x) extern "C" const unsigned char x[]
REGION(marked_router_key);REGION(marked_router_captured_fixed);REGION(marked_router_begin);REGION(marked_router_end);REGION(marked_router_gate);REGION(marked_router_targets);REGION(marked_router_callers);REGION(marked_router_captured);REGION(marked_router_payload_store);
REGION(marked_store_begin);REGION(marked_store_end);REGION(marked_store_branch);REGION(marked_store_write);
REGION(marked_math_begin);REGION(marked_math_end);REGION(marked_math_expected);REGION(marked_math_callers);REGION(marked_math_branch);REGION(marked_math_restore);REGION(marked_math_quantize);
REGION(observer_getter);REGION(observer_getter_end);REGION(observer_getter_branch);REGION(observer_tail);REGION(observer_tail_end);REGION(observer_tail_branch);REGION(observer_math);REGION(observer_math_end);REGION(observer_math_branch);
int main(int argc,char**argv){
 assert(argc==2||argc==3);Image i(argv[1]);const std::string mutant=argc==3?argv[2]:"";
 for(auto[va,target]:std::array<std::pair<size_t,uintptr_t>,12>{{{0x1b57c08,uintptr_t(secondary_array)},{0x18aa0e0,uintptr_t(geometry_context)},{0xe1d028,uintptr_t(secondary_view)},{0xb19564,uintptr_t(geometry_rx_set)},{0x1781f28,uintptr_t(geometry_true)},{0x18a363c,uintptr_t(geometry_sub)},{0x18a3aa4,uintptr_t(geometry_add)},{0x868b8c,uintptr_t(geometry_null)},{0x86af0c,uintptr_t(geometry_null)},{0xa4ac00,uintptr_t(geometry_zero)},{0x1928030,uintptr_t(routing_return)},{0xa4b47c,uintptr_t(routing_variable_record)}}})i.jump(va,target);
 i.jump(0x1927f30,uintptr_t(routing_return));
 const size_t calls[]={0x19274f4,0x1927574,0x1927d3c,0x1927dd8,0x1927f1c,0x192801c};uintptr_t callers[6];
 for(int z=0;z<6;z++){callers[z]=uintptr_t(i.base+calls[z]+4);if(z<4)i.word(calls[z],branch(calls[z],0xa49adc,true));}
 // Original fixed first16B trampoline; all three original integer row loads stay.
 constexpr size_t RO=0,ST=640,MA=896,TR=1408,OG=1536,OT=1664,OM=1792;
 auto copy=[&](size_t offset,const unsigned char*begin,const unsigned char*end){const auto n=size_t(end-begin);check(n<640,"candidate/observer fits its private-image region");memcpy(i.base+i.veneer+offset,begin,n);};
 copy(RO,marked_router_begin,marked_router_end);copy(ST,marked_store_begin,marked_store_end);copy(MA,marked_math_begin,marked_math_end);copy(OG,observer_getter,observer_getter_end);copy(OT,observer_tail,observer_tail_end);copy(OM,observer_math,observer_math_end);
 i.word(i.veneer+TR,0xd53b4211);i.word(i.veneer+TR+4,0xf900bb51); // test MRSx17,NZCV / STRx17,[thread,0x170]
 memcpy(i.base+i.veneer+TR+8,i.base+0xa49adc,16);i.word(i.veneer+TR+24,branch(i.veneer+TR+24,0xa49aec));
 auto patchbranch=[&](size_t offset,const unsigned char*begin,const unsigned char*site,size_t target){const auto at=i.veneer+offset+size_t(site-begin);i.word(at,branch(at,target));};
 patchbranch(ST,marked_store_begin,marked_store_branch,0xa49b0c);patchbranch(MA,marked_math_begin,marked_math_branch,0xa4ab88);patchbranch(OG,observer_getter,observer_getter_branch,0xa4ace8);patchbranch(OT,observer_tail,observer_tail_branch,0xa49e70);patchbranch(OM,observer_math,observer_math_branch,i.veneer+MA);
 const uintptr_t targets[]={uintptr_t(i.base+i.veneer+TR),uintptr_t(i.base+0xa4b458)};
 memcpy(i.base+i.veneer+RO+size_t(marked_router_targets-marked_router_begin),targets,sizeof targets);
 memcpy(i.base+i.veneer+RO+size_t(marked_router_callers-marked_router_begin),callers,sizeof callers);
 memcpy(i.base+i.veneer+MA+size_t(marked_math_callers-marked_math_begin),callers,sizeof callers);
 const uintptr_t expected=uintptr_t(i.base+0xa49c5c);memcpy(i.base+i.veneer+MA+size_t(marked_math_expected-marked_math_begin),&expected,8);
 i.jump(0xa49adc,uintptr_t(i.base+i.veneer+RO));i.jump(0xa49afc,uintptr_t(i.base+i.veneer+ST));i.jump(0xa4ab78,uintptr_t(i.base+i.veneer+OM));
 i.jump(0xa4acd8,uintptr_t(i.base+i.veneer+OG));i.jump(0xa49e60,uintptr_t(i.base+i.veneer+OT));
 if(mutant=="lost-restore")i.word(i.veneer+MA+size_t(marked_math_restore-marked_math_begin),0xd503201f);
 if(mutant=="lost-marker")i.word(i.veneer+ST+size_t(marked_store_write-marked_store_begin),0xd503201f);
 if(mutant=="round-instead-of-trunc")i.word(i.veneer+MA+size_t(marked_math_quantize-marked_math_begin)+12,0x1e644063);
 if(mutant=="leak-encoded-arg")i.word(i.veneer+ST+size_t(marked_store_write-marked_store_begin)+4,0xaa1003e5); // replaces LDURd0; shape fails too
 i.sync();const auto gateptr=i.base+i.veneer+RO+size_t(marked_router_gate-marked_router_begin);const auto cut=i.veneer+RO+size_t(marked_router_captured-marked_router_begin);const auto cutfixed=i.veneer+RO+size_t(marked_router_captured_fixed-marked_router_begin);const auto keyptr=i.base+i.veneer+RO+size_t(marked_router_key-marked_router_begin);
 unsigned admitted=0,closed=0,collision=0;
 for(int route=0;route<2;route++)for(int state=0;state<4;state++)for(auto [cx,cy]:std::array<std::pair<int,int>,2>{{{3,13},{9,4}}})for(int64_t layout:std::array<int64_t,4>{-1,0,1,2})for(double dpr:std::array<double,5>{1,2.75,0,-2,NAN}){
  if(route==1&&layout!=0)continue;
  ++cases;const bool selected=state!=0;const uint64_t gate=selected?1:0,key=(cx<<8)|cy;memcpy(gateptr,&gate,8);memcpy(keyptr,&key,8);i.word(cut,state==2?0xf900023f:0xd503201f);i.word(cutfixed,state==2?0xf900023f:0xd503201f);if(mutant=="ignore-epoch")i.word(cutfixed+4,0xd503201f);i.sync();
  Heap h;setup(h,dpr);auto thread=uintptr_t(h.thread.data());h.put<double>(h.handler,0xf,260);h.put<uintptr_t>(thread,0x128,uint64_t(layout));h.put<uintptr_t>(thread,0x150,state==3);h.put<uintptr_t>(thread,0x158,uintptr_t(gateptr));h.put<uintptr_t>(thread,0x160,mutant=="late-global-gate");
  auto top=uintptr_t(&h.stack[1800]);
  if(route==0){h.put<uintptr_t>(thread,0xe0,uintptr_t(i.base+0x1927f10));Call c{uintptr_t(marked_dispatch),h.handler,6,top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,thread,0,0,0,0,0,4,0};call_geometry(&c);check(c.returned==top,"original natural caller/fixed/centering/tail restores stack");}
  else{auto grid=h.obj(0x2d00),payload=h.obj(0x2e00);h.ptr(grid,0x1f,h.handler);h.put<int64_t>(payload,7,4);h.put<int64_t>(payload,0xf,-1);h.put<int64_t>(payload,0x1f,0);h.put<uintptr_t>(thread,0xd0,grid);h.put<uintptr_t>(thread,0xd8,payload);h.put<uintptr_t>(thread,0xe0,uintptr_t(i.base+0x1927564));h.put<uintptr_t>(thread,0xe8,1);Call c{uintptr_t(routing_dispatch),h.handler,4,top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,thread,0,0,0,0,8,UINT64_MAX};call_geometry(&c);check(c.returned==top,"original reason4 caller restores stack");check(h.get<int64_t>(payload,7)==(selected?cx:4),"preview payload is admitted value only when enabled");if(!selected){check(h.get<uintptr_t>(thread,0x108)==1,"future closed preview returns to original variable domain");++closed;continue;}layout=0;}
  if(route==0)check(h.get<uint64_t>(thread,0x170)==0xa0000000ULL,"feature-key validation preserves original caller NZCV");
  const uint64_t saved=h.get<uint64_t>(thread,0x140);const uint64_t wantmarker=0x4000000000000000ULL|uint64_t(layout+1);
  check(saved==(selected?wantmarker:uint64_t(layout)),"first original getter observes exact admitted marker in parent slot only");
  check(h.get<int64_t>(thread,0x130)==layout&&h.get<int64_t>(thread,0x138)==layout,"first original getter x2 and x5 keep original layout");
  check(h.get<int64_t>(thread,0x148)==layout,"full original tail receives restored original layout");
  const auto rx=h.get<uint32_t>(h.handler,0x43)|(uintptr_t(h.mem)&~uintptr_t(0xffffffff));check(h.get<uint32_t>(rx,0x13)==uint32_t((selected?cy:6)*2),"row integer capture remains selected or original");
  const double pd=std::isfinite(dpr)&&dpr>0?dpr:1;const double raw=std::min(120.0/(selected?cx:4),260.0/((selected?cy:6)+1));const double want=selected?std::trunc(raw*pd)/pd:raw;
  close(h.get<double>(h.handler,0x47),want,"admitted math completes after global gate closes");
  if(selected){++admitted;check(h.get<double>(h.handler,0x37)>=-1e-8,"admitted frame final margin remains bounded");}else ++closed;
  if(state==3||state==2){uint64_t v;memcpy(&v,gateptr,8);check(v==0,"precise instruction cut actually cleared global admission gate");}
 }
 // Explicit invalid domains retain original arguments, except reserved marker
 // collision: this is a demonstrated limitation, NOT a silently accepted pass.
 for(int64_t layout:std::array<int64_t,7>{-2,3,4,INT64_MAX,INT64_MIN,int64_t(0x4000000000000000ULL),int64_t(0x4000000000000003ULL)}){
  ++cases;const uint64_t gate=1,key=(3<<8)|13;memcpy(gateptr,&gate,8);memcpy(keyptr,&key,8);i.word(cut,0xd503201f);i.word(cutfixed,0xd503201f);i.sync();Heap h;setup(h);auto thread=uintptr_t(h.thread.data());h.put<uintptr_t>(thread,0x128,uint64_t(layout));h.put<uintptr_t>(thread,0xe0,uintptr_t(i.base+0x1927f10));const auto top=uintptr_t(&h.stack[1800]);Call c{uintptr_t(marked_dispatch),h.handler,6,top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,thread,0,0,0,0,0,4,0};call_geometry(&c);
  check(h.get<int64_t>(thread,0x130)==layout&&h.get<int64_t>(thread,0x138)==layout,"rejected-domain entry leaves getter arguments unchanged");
  const auto got=h.get<int64_t>(thread,0x148);if(layout==int64_t(0x4000000000000000ULL)||layout==int64_t(0x4000000000000003ULL)){++collision;check(got==int64_t((uint64_t(layout)^0x4000000000000000ULL)-1),"reserved-domain collision is reproduced rather than called safe");std::cerr<<"RESERVED_COLLISION original="<<layout<<" tail="<<got<<"\n";}else check(got==layout,"nonreserved invalid layout passes through unchanged");
 }
 // Odd epoch is necessary but not sufficient: exact single feature-key range.
 for(uint64_t key:std::array<uint64_t,8>{0,0x0204,0x0a04,0x0303,0x030e,0x10304,UINT64_MAX,0x030d})for(uint64_t epoch:std::array<uint64_t,2>{0,1}){
  ++cases;memcpy(gateptr,&epoch,8);memcpy(keyptr,&key,8);Heap h;setup(h);auto thread=uintptr_t(h.thread.data());h.put<uintptr_t>(thread,0x128,0);h.put<uintptr_t>(thread,0xe0,uintptr_t(i.base+0x1927f10));auto top=uintptr_t(&h.stack[1800]);Call c{uintptr_t(marked_dispatch),h.handler,6,top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,thread,0,0,0,0,0,4,0};call_geometry(&c);const auto rx=h.get<uint32_t>(h.handler,0x43)|(uintptr_t(h.mem)&~uintptr_t(0xffffffff));check(h.get<uint32_t>(rx,0x13)==uint32_t((epoch==1&&key==0x030d?13:6)*2),"one epoch and canonical shared key determine admission including late nonzero key");
 }
 check(admitted>0&&closed>0&&collision==2,"explicit admission/off/collision cases execute");
 std::cout<<"{\"passed\":true,\"cases\":"<<cases<<",\"checks\":"<<checks<<",\"admitted_frames\":"<<admitted<<",\"closed_frames\":"<<closed<<",\"reserved_domain_collisions\":"<<collision<<",\"unrestricted_raw_domain_safe\":false,\"production_ready\":false,\"device_status\":\"NOT_TESTED\"}\n";
}
