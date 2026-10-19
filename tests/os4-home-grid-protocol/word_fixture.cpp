#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wreturn-type"
#define main geometry_v4_historical_main
#include "geometry_v4_fixture.cpp"
#undef main
#pragma GCC diagnostic pop
extern "C" {void geometry_pref(),geometry_copy(),secondary_dispatch(),secondary_return(),geometry_foreign();uint64_t geometry_copy_args[3]={};uint64_t geometry_foreign_calls=0;}
int main(int argc,char**argv){assert(argc==2||argc==3);std::string mutation=argc==3?argv[2]:"";Image i(argv[1]);
 i.jump(0x1b56b78,uintptr_t(geometry_context));i.word(0x18ac658,0xd65f03c0);i.jump(0x18ac364,uintptr_t(geometry_async_end));i.jump(0x1b56f3c,uintptr_t(geometry_null));i.jump(0x8a8810,uintptr_t(geometry_null));i.jump(0x1892b4c,uintptr_t(geometry_pref));i.jump(0x151dd28,uintptr_t(geometry_copy));
 uint32_t saved_col[3]; const size_t colsites[]={0x1abeab0,0x1abead0,0x1abe9ec};
 for(int z=0;z<3;z++)memcpy(&saved_col[z],i.base+colsites[z],4);
 check(saved_col[0]==0xd2800102&&saved_col[1]==0xd2800142&&saved_col[2]==0xf8407004,"three actual column producers guarded");
 for(int enabled=0;enabled<2;enabled++)for(int cols=3;cols<=9;cols++)for(int kind=0;kind<5;kind++){
  ++cases;Heap h;std::array<uint8_t,0x6000>table{};h.put<uintptr_t>(uintptr_t(h.thread.data()),0x78,uintptr_t(table.data()));h.put<uintptr_t>(uintptr_t(table.data()),0x2490,h.other);const auto store=h.obj(0xa00),mint=h.obj(0xb00),phone=h.obj(0xc00),jeejen=h.obj(0xd00);h.put<uintptr_t>(uintptr_t(table.data()),0x4f98,store);h.ptr(store,0xf,h.n);h.put<int64_t>(mint,7,6);h.put<uintptr_t>(uintptr_t(h.thread.data()),0x80,mint);
  h.pp(0x14088,phone);h.pp(0x14070,jeejen);h.ptr(h.config,0x17,kind==4?h.other:phone);h.ptr(h.config,0x2f,kind==3?jeejen:h.other);h.ptr(h.config,0x37,h.layout+16*(kind<3?kind:0));
  for(int z=0;z<3;z++){i.word(colsites[z],enabled&&!(mutation=="jeejen-missed"&&z==2)?0xd2800000u|(uint32_t(cols*(z==2?1:2))<<5)|uint32_t(z==2?4:2):saved_col[z]);}
  i.sync();
  std::fill(std::begin(geometry_copy_args),std::end(geometry_copy_args),0xdead);auto top=uintptr_t(&h.stack[1800]);Call c{uintptr_t(i.base+0x1abe8f8),h.handler,h.context,top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,uintptr_t(h.thread.data()),0,0};call_geometry(&c);
  const int oldcols=kind==0||kind==1?4:kind==2?5:kind==3?6:0;const uint64_t want=uint64_t((enabled&&kind!=4?cols:oldcols)*2);
  check(geometry_copy_args[1]==want,"typed column matches selected phone or exact untouched other-device branch");check(geometry_copy_args[0]==(kind==4?0:UINT64_MAX-1),"original row minus-one policy marker retained");check(geometry_copy_args[2]==h.context,"original copyWith receives same context identity");check(c.returned==top,"complete original PhoneRowCol frame restored");
 }

 for(int z=0;z<3;z++)i.word(colsites[z],saved_col[z]);
 // Same four original, Phone-only caller blocks. The outer asynchronous
 // function frame and caller inputs are explicit adapters; these blocks,
 // class guard, BL, full fixedRows body, and one original post-call Rx write execute.
 i.word(0xa4418c,0xd65f03c0);i.jump(0x17d0f8c,uintptr_t(geometry_rx));i.jump(0xb19564,uintptr_t(geometry_rx_set));i.jump(0xa4ac00,uintptr_t(geometry_zero));i.jump(0x1781f28,uintptr_t(geometry_true));i.jump(0x18a363c,uintptr_t(geometry_sub));i.jump(0x18a3aa4,uintptr_t(geometry_add));i.jump(0xa49e60,uintptr_t(geometry_null));i.jump(0x1928030,uintptr_t(secondary_return));i.jump(0x8fb4a4,uintptr_t(geometry_foreign));i.jump(0xa4b458,uintptr_t(geometry_foreign));
 const size_t callsites[]={0x19274f4,0x1927574,0x1927d3c,0x1927dd8};const size_t blocks[]={0x19274c4,0x1927564,0x1927d0c,0x1927da8};const int layouts[]={3,4,9,10};
 const size_t fixedsites[]={0xa49aec,0xa49bd4,0xa49c28,0xa49c80};const unsigned fixedregs[]={2,0,0,4};uint32_t saved_fixed[4],saved_calls[4];
 for(int k=0;k<4;k++){memcpy(&saved_fixed[k],i.base+fixedsites[k],4);memcpy(&saved_calls[k],i.base+callsites[k],4);}
 check(saved_fixed[0]==0xaa0203e0&&saved_fixed[1]==0xf85e83a0&&saved_fixed[2]==0xf85e83a0&&saved_fixed[3]==0xf85e83a4,"secondary dead MOVE and all three row load words guarded");
 auto bl=[](size_t from,size_t to){const int64_t d=int64_t(to)-int64_t(from);assert(d%4==0&&d>=-0x8000000&&d<0x8000000);return 0x94000000u|(uint32_t(d/4)&0x3ffffff);};
 for(int path=0;path<4;path++)for(int cols=3;cols<=9;cols++)for(int rows=4;rows<=13;rows++){
  ++cases;Heap h;auto rx=h.obj(0x900),grid=h.obj(0xe00),payload=h.obj(0xf00);h.put<uint64_t>(h.handler-1,0,uint64_t(0x785)<<12);h.ptr(grid,0x1f,h.handler);h.ptr(h.handler,0x43,rx);h.put<uint32_t>(rx,0x13,12);h.put<double>(h.handler,7,120.0);h.put<double>(h.handler,0xf,260.0);h.put<uint32_t>(h.handler,0xdb,16);h.put<uint32_t>(h.handler,0xdf,16);h.put<uintptr_t>(uintptr_t(h.table.data()),0x26f0,h.other);h.ptr(h.other,7,h.other);
  for(int k=0;k<4;k++)i.word(fixedsites[k],mutation=="secondary-row-old"&&k==2?saved_fixed[k]:0xd2800000u|(uint32_t(k==0?cols:rows)<<5)|fixedregs[k]);
  for(int k=0;k<4;k++)i.word(callsites[k],mutation=="secondary-redirect-missed"&&k==3?saved_calls[k]:bl(callsites[k],0xa49adc));
  i.word(0xa49ca0,mutation=="secondary-no-cache"?0xd503201f:0x94033e31);i.sync();
  h.put<uintptr_t>(uintptr_t(h.thread.data()),0x88,grid);h.put<uintptr_t>(uintptr_t(h.thread.data()),0x90,payload);h.put<uintptr_t>(uintptr_t(h.thread.data()),0x98,uintptr_t(i.base+blocks[path]));h.put<uintptr_t>(uintptr_t(h.thread.data()),0xa0,path==1?1:0);
  auto top=uintptr_t(&h.stack[1800]);Call cache{uintptr_t(i.base+0xa4bbd0),h.handler,0,top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,uintptr_t(h.thread.data()),0,0,300.0,0};call_geometry(&cache);check(cache.integer_result==6,"pre-producer original zero-cell read remains old cache");
  Call c{uintptr_t(secondary_dispatch),h.handler,uintptr_t(path==1?4:8),top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,uintptr_t(h.thread.data()),0,0,0,0,uintptr_t(layouts[path]*(path==1?1:2)),UINT64_MAX};geometry_foreign_calls=0;call_geometry(&c);
  const double cell=std::min(120.0/cols,260.0/(rows+1));check(geometry_foreign_calls==0,"four secondary original caller BLs reach fixed body");check(std::abs(h.get<double>(h.handler,0x47)-cell)<1e-9,"secondary word-only shrink geometry uses both selected dimensions");check(h.get<uint32_t>(rx,0x13)==uint32_t(rows*2),"secondary original Rx setter publishes selected row");check(c.result>=-1e-9&&std::abs(c.result-(260.0-cell*(rows+1)))<1e-9,"secondary word-only original remainder is bounded");check(c.returned==top,"secondary original caller-slice frame restored");if(path==1)check(h.get<int64_t>(payload,0xf)==rows,"original post-call branch writes selected cache row back to payload");
  h.put<double>(h.handler,0x47,0.0);call_geometry(&cache);check(cache.integer_result==uintptr_t(rows),"post-producer original zero-cell read uses same-owner cache");
 }
 // Keep patched callees but take each original non-phone guard/fallback. The
 // test fallback only records dispatch; it is not OEM dynamic invocation.
 for(int path=0;path<4;path++){
  ++cases;Heap h;auto rx=h.obj(0x900),grid=h.obj(0xe00),payload=h.obj(0xf00);h.put<uint64_t>(h.handler-1,0,uint64_t(0x784)<<12);h.ptr(grid,0x1f,h.handler);h.ptr(h.handler,0x43,rx);h.put<uint32_t>(rx,0x13,12);h.put<uintptr_t>(uintptr_t(h.thread.data()),0x88,grid);h.put<uintptr_t>(uintptr_t(h.thread.data()),0x90,payload);h.put<uintptr_t>(uintptr_t(h.thread.data()),0x98,uintptr_t(i.base+blocks[path]));h.put<uintptr_t>(uintptr_t(h.thread.data()),0xa0,path==1?1:0);
  auto top=uintptr_t(&h.stack[1800]);Call c{uintptr_t(secondary_dispatch),h.handler,uintptr_t(path==1?4:8),top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,uintptr_t(h.thread.data()),0,0,0,0,uintptr_t(layouts[path]*(path==1?1:2)),UINT64_MAX};geometry_foreign_calls=0;call_geometry(&c);check(geometry_foreign_calls>0,"original foreign-class branch retains fallback");check(h.get<uint32_t>(rx,0x13)==12,"foreign class not published by selected fixed body");check(c.returned==top,"foreign caller-slice frame restored");
 }
 for(int k=0;k<4;k++){i.word(fixedsites[k],saved_fixed[k]);i.word(callsites[k],saved_calls[k]);}i.word(0xa49ca0,0x94033e31);i.sync();
 for(int oldrows:{6,8}){
  ++cases;Heap h;auto rx=h.obj(0x900);h.ptr(h.handler,0x43,rx);h.put<uint32_t>(rx,0x13,12);h.put<double>(h.handler,7,120.0);h.put<double>(h.handler,0xf,260.0);h.put<uint32_t>(h.handler,0xdb,16);h.put<uint32_t>(h.handler,0xdf,16);h.put<uintptr_t>(uintptr_t(h.table.data()),0x26f0,h.other);h.ptr(h.other,7,h.other);auto top=uintptr_t(&h.stack[1800]);Call c{uintptr_t(i.base+0xa49adc),h.handler,4,top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,uintptr_t(h.thread.data()),0,0,0,0,uintptr_t(oldrows),0};call_geometry(&c);check(h.get<uint32_t>(rx,0x13)==uint32_t(oldrows*2),"disabled word restoration returns to original typed rows");check(std::abs(h.get<double>(h.handler,0x47)-std::min(30.0,260.0/(oldrows+1)))<1e-9,"disabled word restoration returns original cell policy");
 }
 std::cout<<"{\"passed\":true,\"checks\":"<<checks<<",\"cases\":"<<cases<<",\"device_status\":\"NOT_TESTED\"}\n";
}
