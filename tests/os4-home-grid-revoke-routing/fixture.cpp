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
int main(int argc,char**argv){
 assert(argc==2||argc==3);Image i(argv[1]);std::string mutation=argc==3?argv[2]:"";
 for(auto[va,target]:std::array<std::pair<size_t,uintptr_t>,12>{{{0x1b57c08,uintptr_t(secondary_array)},{0x18aa0e0,uintptr_t(geometry_context)},{0xe1d028,uintptr_t(secondary_view)},{0xb19564,uintptr_t(geometry_rx_set)},{0x1781f28,uintptr_t(geometry_true)},{0x18a363c,uintptr_t(geometry_sub)},{0x18a3aa4,uintptr_t(geometry_add)},{0x868b8c,uintptr_t(geometry_null)},{0x86af0c,uintptr_t(geometry_null)},{0xa4ac00,uintptr_t(geometry_zero)},{0x1928030,uintptr_t(routing_return)},{0xa4b47c,uintptr_t(routing_variable_record)}}})i.jump(va,target);
 const size_t callsites[]={0x19274f4,0x1927574,0x1927d3c,0x1927dd8};const size_t blocks[]={0x19274c4,0x1927564,0x1927d0c,0x1927da8};const int reasons[]={3,4,9,10};
 std::array<uint32_t,4> oldcalls;for(int z=0;z<4;z++){memcpy(&oldcalls[z],i.base+callsites[z],4);check(oldcalls[z]==branch(callsites[z],0xa4b458,true),"four original caller BL targets variable function");i.word(callsites[z],branch(callsites[z],0xa49adc,true));}
 const uint32_t origcol=0xaa0203e0,origrow=0xaa0503e2;constexpr int selected_x=9,selected_y=13;
 std::array<uint8_t,16> originalhead;memcpy(originalhead.data(),i.base+0xa49adc,16);
 auto run=[&](int path,int wantkind,int wantcols,int wantrows,bool normalized){
  ++cases;Heap h;setup(h);auto thread=uintptr_t(h.thread.data());auto grid=h.obj(0x2d00),payload=h.obj(0x2e00);h.ptr(grid,0x1f,h.handler);h.put<int64_t>(payload,7,4);h.put<int64_t>(payload,0xf,-1);h.put<int64_t>(payload,0x1f,0);
  h.put<uintptr_t>(thread,0xd0,grid);h.put<uintptr_t>(thread,0xd8,payload);h.put<uintptr_t>(thread,0xe0,uintptr_t(i.base+blocks[path]));h.put<uintptr_t>(thread,0xe8,path==1?1:0);
  auto top=uintptr_t(&h.stack[1800]);Call c{uintptr_t(routing_dispatch),h.handler,uintptr_t(path==1?4:8),top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,thread,0,0,0,0,uintptr_t(reasons[path]*2),UINT64_MAX};call_geometry(&c);
  check(c.returned==top,"actual caller original frame and callee return restore stack");
  check(h.get<uintptr_t>(thread,0x108)==uintptr_t(wantkind==0),"route chooses original variable versus fixed according to captured policy");
  if(wantkind==0){check(h.get<uintptr_t>(thread,0xf0)==uint64_t(wantcols),"disabled router preserves original variable column input");check(h.get<uintptr_t>(thread,0xf8)==uint64_t(reasons[path]),"disabled router preserves variable reason instead of treating it as row");check(h.get<uintptr_t>(thread,0x100)==uint64_t(path==1?0:UINT64_MAX),"disabled router preserves layout argument");}
  else{const auto rx=h.get<uint32_t>(h.handler,0x43)|(uintptr_t(h.mem)&~uintptr_t(0xffffffff));check(h.get<uint32_t>(rx,0x13)==uint32_t(wantrows*2),"original fixed publishes the row actually captured by its prologue");}
  if(path==1)check(h.get<int64_t>(payload,7)==(normalized?selected_x:4),"preview payload mutates only after enabled router admission");
 };
 // Deterministic reachable restore states, not a simulated concurrency PASS:
 // callers already BL fixed; restore has changed zero, one, then both prologs.
 for(int path=0;path<4;path++){
  i.word(0xa49aec,0xd2800002u|(selected_x<<5));i.word(0xa49af4,0xd2800003u|(selected_y<<5));i.sync();run(path,1,selected_x,selected_y,false);
  i.word(0xa49af4,origrow);i.sync();run(path,1,selected_x,reasons[path],false);
  i.word(0xa49aec,origcol);i.sync();run(path,1,4,reasons[path],false);
  std::cerr<<"COUNTEREXAMPLE caller="<<std::hex<<callsites[path]<<std::dec<<" original_variable_reason="<<reasons[path]<<" restored_fixed_published_rows="<<reasons[path]<<" selected_rows="<<selected_y<<"\n";
 }
 // Permanently retain structure. Original fixed prologue is now immutable;
 // full original16B trampoline, no prolog words or callsite restoration in flight.
 constexpr size_t fixedtramp=512;
 memcpy(i.base+i.veneer+fixedtramp,originalhead.data(),16);i.word(i.veneer+fixedtramp+16,branch(i.veneer+fixedtramp+16,0xa49aec));
 const auto routersize=size_t(grid_router_end-grid_router_begin),gateoff=size_t(grid_router_gate-grid_router_begin),targetoff=size_t(grid_router_targets-grid_router_begin),calleroff=size_t(grid_router_callers-grid_router_begin),captureoff=size_t(grid_router_captured-grid_router_begin);check(routersize<fixedtramp,"router and original trampoline do not overlap");
 memcpy(i.base+i.veneer,grid_router_begin,routersize);const uintptr_t targets[]={uintptr_t(i.base+i.veneer+fixedtramp),uintptr_t(i.base+0xa4b458)};memcpy(i.base+i.veneer+targetoff,targets,sizeof targets);
 const uintptr_t callers[]={uintptr_t(i.base+callsites[0]+4),uintptr_t(i.base+callsites[1]+4),uintptr_t(i.base+callsites[2]+4),uintptr_t(i.base+callsites[3]+4),uintptr_t(i.base+0x1927f20),uintptr_t(i.base+0x1928020)};memcpy(i.base+i.veneer+calleroff,callers,sizeof callers);
 if(mutation=="wrong-variable-target"){const uintptr_t wrong=targets[0];memcpy(i.base+i.veneer+targetoff+8,&wrong,8);}
 // One intentionally injected revoke immediately after LDAR is a precise ARM
 // instruction cut. The already-read gate/config stays in x16 for this call.
 const uint32_t normal_capture=0xd503201f,clear_after_capture=0xf900023f;
 if(mutation=="lost-preview-sync")i.word(i.veneer+size_t(grid_router_payload_store-grid_router_begin),0xd503201f);
 i.jump(0xa49adc,uintptr_t(i.base+i.veneer));
 for(int state=0;state<3;state++)for(int path=0;path<4;path++){
  const uint64_t gate=state==0?0:1|(selected_x<<8)|(selected_y<<16);memcpy(i.base+i.veneer+gateoff,&gate,8);
  i.word(i.veneer+captureoff,state==2?clear_after_capture:normal_capture);
  if(mutation=="late-recheck"&&state==2)i.word(i.veneer+captureoff+4,0xf9400230); // LDRx16,[x17], erasing captured admission (replaces TBZ)
  i.sync();run(path,state==0?0:1,state==0?4:selected_x,state==0?6:selected_y,state!=0);
  if(state==2){uint64_t after;memcpy(&after,i.base+i.veneer+gateoff,8);check(after==0,"test cut really revoked shared gate after capture");}
 }
 // The two actual natural fixed caller suffixes execute their original
 // register moves and BLs, with decoded input/frame supplied by this adapter.
 i.jump(0x1927f30,uintptr_t(routing_return));
 for(bool enabled:std::array<bool,2>{false,true})for(int path=0;path<2;path++){
  ++cases;const uint64_t gate=enabled?1|(selected_x<<8)|(selected_y<<16):0;memcpy(i.base+i.veneer+gateoff,&gate,8);i.sync();Heap h;setup(h);auto thread=uintptr_t(h.thread.data());h.put<uintptr_t>(thread,0xe8,path);h.put<uintptr_t>(thread,0xe0,uintptr_t(i.base+(path?0x1928010:0x1927f10)));
  auto top=uintptr_t(&h.stack[1800]);Call c{uintptr_t(routing_natural_dispatch),h.handler,6,top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,thread,0,0,0,0,0,4,0};call_geometry(&c);auto rx=h.get<uint32_t>(h.handler,0x43)|(uintptr_t(h.mem)&~uintptr_t(0xffffffff));
  check(c.returned==top,"actual natural fixed suffix and original fixed restore stack");
  check(h.get<uintptr_t>(thread,0x108)==0,"natural fixed caller never routes through variable numerical adapter");
  check(h.get<uint32_t>(rx,0x13)==uint32_t((enabled?selected_y:6)*2),"natural fixed caller preserves OEM rows off and captured rows on");
 }
 // Unknown direct host caller is treated as original fixed, even gate enabled.
 for(bool enabled:std::array<bool,2>{false,true}){++cases;const uint64_t gate=enabled?1|(selected_x<<8)|(selected_y<<16):0;memcpy(i.base+i.veneer+gateoff,&gate,8);i.word(i.veneer+captureoff,normal_capture);i.sync();Heap h;setup(h);auto top=uintptr_t(&h.stack[1800]);Call c{uintptr_t(i.base+0xa49adc),h.handler,4,top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,uintptr_t(h.thread.data()),0,0,0,0,6,0,0};call_geometry(&c);const auto rx=h.get<uint32_t>(h.handler,0x43)|(uintptr_t(h.mem)&~uintptr_t(0xffffffff));check(h.get<uint32_t>(rx,0x13)==12,"unknown fixed caller stays original even when module gate enabled");}
 std::cout<<"{\"passed\":true,\"cases\":"<<cases<<",\"checks\":"<<checks<<",\"unsafe_restore_counterexamples\":4,\"router_installer_approved\":false,\"device_status\":\"NOT_TESTED\"}\n";
}
