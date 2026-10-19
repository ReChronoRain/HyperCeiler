#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wreturn-type"
#define main geometry_v4_historical_main
#include "geometry_v4_fixture.cpp"
#undef main
#pragma GCC diagnostic pop
extern "C" {void geometry_pref(),geometry_copy();uint64_t geometry_copy_args[3]={};}
int main(int argc,char**argv){assert(argc==2||argc==3);std::string mutation=argc==3?argv[2]:"";Image i(argv[1]);
 i.jump(0x1b56b78,uintptr_t(geometry_context));i.word(0x18ac658,0xd65f03c0);i.jump(0x18ac364,uintptr_t(geometry_async_end));i.jump(0x1b56f3c,uintptr_t(geometry_null));i.jump(0x8a8810,uintptr_t(geometry_null));i.jump(0x1892b4c,uintptr_t(geometry_pref));i.jump(0x151dd28,uintptr_t(geometry_copy));
 uint32_t word;memcpy(&word,i.base+0x1abeaf8,4);check(word==0xb8413002,"exact common typed column-load instruction");const size_t vg=i.veneer;
 auto branch=[](size_t from,size_t to){const int64_t d=int64_t(to)-int64_t(from);assert(d%4==0&&d>=-0x8000000&&d<0x8000000);return 0x14000000u|(uint32_t(d/4)&0x3ffffff);};
 for(int enabled=0;enabled<2;enabled++)for(int cols=3;cols<=9;cols++)for(int kind=0;kind<5;kind++){
  ++cases;Heap h;std::array<uint8_t,0x6000>table{};h.put<uintptr_t>(uintptr_t(h.thread.data()),0x78,uintptr_t(table.data()));h.put<uintptr_t>(uintptr_t(table.data()),0x2490,h.other);const auto store=h.obj(0xa00),mint=h.obj(0xb00),phone=h.obj(0xc00),jeejen=h.obj(0xd00);h.put<uintptr_t>(uintptr_t(table.data()),0x4f98,store);h.ptr(store,0xf,h.n);h.put<int64_t>(mint,7,6);h.put<uintptr_t>(uintptr_t(h.thread.data()),0x80,mint);
  h.pp(0x14088,phone);h.pp(0x14070,jeejen);h.ptr(h.config,0x17,kind==4?h.other:phone);h.ptr(h.config,0x2f,kind==3?jeejen:h.other);h.ptr(h.config,0x37,h.layout+16*(kind<3?kind:0));
  i.word(vg,word);i.word(vg+4,mutation=="all-devices"?0xd503201f:0xb6f80041); // tbz x1,#63,+8: original phone marker is -1, other is0
  i.word(vg+8,0xd2800002u|(uint32_t(cols*2)<<5));i.word(vg+12,branch(vg+12,0x1abeafc));i.word(0x1abeaf8,enabled&&mutation!="skip-typed"?branch(0x1abeaf8,vg):word);i.sync();
  std::fill(std::begin(geometry_copy_args),std::end(geometry_copy_args),0xdead);auto top=uintptr_t(&h.stack[1800]);Call c{uintptr_t(i.base+0x1abe8f8),h.handler,h.context,top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,uintptr_t(h.thread.data()),0,0};call_geometry(&c);
  const int oldcols=kind==0||kind==1?4:kind==2?5:kind==3?6:0;const uint64_t want=uint64_t((enabled&&kind!=4?cols:oldcols)*2);
  check(geometry_copy_args[1]==want,"typed column matches selected phone or exact untouched other-device branch");check(geometry_copy_args[0]==(kind==4?0:UINT64_MAX-1),"original row minus-one policy marker retained");check(geometry_copy_args[2]==h.context,"original copyWith receives same context identity");check(c.returned==top,"complete original PhoneRowCol frame restored");
 }
 std::cout<<"{\"passed\":true,\"checks\":"<<checks<<",\"cases\":"<<cases<<",\"device_status\":\"NOT_TESTED\"}\n";
}
