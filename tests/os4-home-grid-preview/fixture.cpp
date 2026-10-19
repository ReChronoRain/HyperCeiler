// Reuse only frozen image/heap test utilities and original dependency adapters.
#include <tuple>
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wreturn-type"
#define main frozen_quantized_main
#include "../os4-home-grid-protocol/quantized_fixture.cpp"
#undef main
#pragma GCC diagnostic pop
extern "C" void preview_allocate(),preview_cal_bridge(),preview_cal_return(),preview_load_record(),preview_variable_record();
extern "C" const unsigned char preview_column_begin[],preview_column_end[];
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
 assert(argc==2||argc==3);Image image(argv[1]);std::string mutation=argc==3?argv[2]:"";
 const std::array<std::pair<size_t,uintptr_t>,16> adapters{{
 {0x1b57c08,uintptr_t(secondary_array)},{0x18aa0e0,uintptr_t(geometry_context)},{0xe1d028,uintptr_t(secondary_view)},
 {0xb19564,uintptr_t(geometry_rx_set)},{0x1781f28,uintptr_t(geometry_true)},{0x18a363c,uintptr_t(geometry_sub)},
 {0x18a3aa4,uintptr_t(geometry_add)},{0x868b8c,uintptr_t(geometry_null)},{0x86af0c,uintptr_t(geometry_null)},
 {0xa4ac00,uintptr_t(geometry_zero)},{0x1b56f3c,uintptr_t(geometry_null)},{0x8a8810,uintptr_t(geometry_null)},
 {0x19e4840,uintptr_t(preview_allocate)},{0x192718c,uintptr_t(preview_cal_bridge)},
 {0x1928030,uintptr_t(preview_cal_return)},{0x18bab44,uintptr_t(preview_load_record)}}};
 for(auto[va,target]:adapters)image.jump(va,target);
 std::array<std::pair<size_t,size_t>,5> guarded{{{0xeac820,268},{0xeac9d4,420},{0xb1a270,100},{0xeacb78,28},{0x1927524,220}}};
 std::vector<std::vector<uint8_t>> before;for(auto[va,size]:guarded)before.emplace_back(image.base+va,image.base+va+size);
 if(mutation=="clamp-main-copy")image.word(0xeaca9c,0xd28000a1);
 image.sync();
 unsigned prepare_cases=0,creation_cases=0,option_mismatches=0,normalized_mismatches=0,disabled_cases=0;
 for(int cols=3;cols<=9;cols++)for(int rows=4;rows<=13;rows++)for(int layout=0;layout<3;layout++){
  ++cases;++prepare_cases;Heap h;setup(h);auto table=uintptr_t(h.table.data());
  auto owner=h.obj(0x2d00),controller=h.obj(0x2e00),rx=h.obj(0x2f00),cell=h.obj(0x2f80);
  h.put<uintptr_t>(table,0x2710,controller);h.ptr(controller,0x2b,rx);h.ptr(rx,0x13,h.config);h.ptr(owner,0x27,cell);
  h.put<int64_t>(h.config,0x1b,cols);h.put<int64_t>(h.config,0x23,rows);h.put<int64_t>(h.obj(0x2900),7,layout);
  std::vector<uint8_t>pool(0xa9000);memcpy(pool.data(),h.pool.data(),h.pool.size());
  const auto top=uintptr_t(&h.stack[1800]);Call c{uintptr_t(image.base+0xeac9d4),owner,0,top,uintptr_t(pool.data()),uintptr_t(h.mem)>>32,h.n,uintptr_t(h.thread.data()),0,0};call_geometry(&c);
  check(c.returned==top,"original prepare restores Dart stack");
  check(h.get<int64_t>(cell,7)==cols,"actual currentConfig to preview retains all selected columns");
  check(h.get<int64_t>(cell,0xf)==rows,"actual currentConfig to preview retains all selected rows");
  check(h.get<int64_t>(cell,0x17)==-1&&h.get<int64_t>(cell,0x1f)==layout,"actual reset and layout assignment preserve independent index and mode");
  check(h.get<uint32_t>(owner,0x2b)==uint32_t(cell),"original preview active field points to same copied object");
 }
 // Install the already-frozen local geometry candidate in the private image.
 memcpy(image.base+image.veneer,grid_quantize_begin,24);image.word(image.veneer+20,branch(image.veneer+20,0xa49c48));
 for(unsigned z=0;z<3;z++){uint32_t w;memcpy(&w,grid_quantize_end+4*z,4);image.word(0xa49b74+4*z,w);}image.word(0xa49c44,branch(0xa49c44,image.veneer));
 check(preview_column_end-preview_column_begin==12,"phone payload normalization uses three linked instructions");
 memcpy(image.base+image.veneer+64,preview_column_begin,12);
 image.word(image.veneer+72,branch(image.veneer+72,0xa49adc));
 if(mutation=="lost-row-write")image.word(0x19275f8,0xd503201f);
 if(mutation=="overwrite-option-x")image.word(0xeac84c,0xd2800120); // MOV x0,9; a real executed false-provenance counterexample.
 image.sync();
 for(bool normalized:std::array<bool,2>{false,true})for(int cols=3;cols<=9;cols++)for(int rows=4;rows<=13;rows++)for(int layout=0;layout<3;layout++){
  ++cases;++creation_cases;Heap h;setup(h);auto table=uintptr_t(h.table.data()),thread=uintptr_t(h.thread.data());
  image.word(0x1927574,branch(0x1927574,normalized?image.veneer+64:0xa49adc,true));
  image.word(image.veneer+64,0xd2800002u|(uint32_t(cols)<<5));
  if(mutation=="lost-payload-sync")image.word(image.veneer+68,0xd503201f);
  __builtin___clear_cache(reinterpret_cast<char*>(image.base+0x1927574),reinterpret_cast<char*>(image.base+0x1927578));
  __builtin___clear_cache(reinterpret_cast<char*>(image.base+image.veneer+64),reinterpret_cast<char*>(image.base+image.veneer+76));
  auto grid=h.obj(0x2d00),controller=h.obj(0x2e00),closure=h.obj(0x2f00);
  h.put<uintptr_t>(table,0x42a8,grid);h.ptr(grid,0x1f,h.handler);h.put<uintptr_t>(table,0x2710,controller);
  h.put<uintptr_t>(thread,0x98,closure);h.put<uintptr_t>(thread,0xa0,uintptr_t(image.base+0x1927524));
  for(auto[va,reg,value]:std::array<std::tuple<size_t,unsigned,int>,4>{{{0xa49aec,2,cols},{0xa49bd4,0,rows},{0xa49c28,0,rows},{0xa49c80,4,rows}}})image.word(va,0xd2800000u|(uint32_t(value)<<5)|reg);
  __builtin___clear_cache(reinterpret_cast<char*>(image.base+0xa49adc),reinterpret_cast<char*>(image.base+0xa49e60));
  const int option_x=layout==2?5:4;const auto top=uintptr_t(&h.stack[1800]);Call c{uintptr_t(image.base+0xeac820),h.n,uintptr_t(layout),top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,thread,0,0,0,0,uintptr_t(option_x),uintptr_t(layout),0};call_geometry(&c);auto cell=c.integer_result;
  check(c.returned==top,"original option constructor and guarded calculation slice restore Dart stack");
  check(h.get<int64_t>(cell,7)==(normalized?cols:option_x),normalized?"phone-class normalization synchronizes option column before original calculation":"original option payload retains constructor column argument");
  check(h.get<int64_t>(cell,0xf)==rows,"original reason4 caller writes computed selected rows to payload");
  check(h.get<int64_t>(cell,0x17)==layout&&h.get<int64_t>(cell,0x1f)==layout,"original option retains index and layout metadata");
  check(h.get<uintptr_t>(thread,0xb0)==4&&h.get<uintptr_t>(thread,0xc0)==8,"original create uses calculation reason4 then load reason8");
  check(h.get<uintptr_t>(thread,0xb8)==cell,"original load receives the same option object");
  close(h.get<double>(h.handler,0x47),std::trunc(std::min(120.0/cols,800.0/(rows+1))),"candidate geometry uses selected columns and rows");
  if(h.get<int64_t>(cell,7)!=cols){if(normalized)++normalized_mismatches;else ++option_mismatches;}
  if(cols==9&&rows==6&&layout==2)std::cerr<<"WITNESS normalized="<<normalized<<" selected="<<cols<<"x"<<rows<<" option="<<h.get<int64_t>(cell,7)<<"x"<<h.get<int64_t>(cell,0xf)<<" cell="<<h.get<double>(h.handler,0x47)<<"\n";
 }
 // Restore the original variable BL; the callee is explicitly a numerical
 // adapter, while the original constructor/class guard/cache/write/load runs.
 image.word(0x1927574,0x97c48fb9);image.jump(0xa4b458,uintptr_t(preview_variable_record));image.sync();
 for(int layout=0;layout<3;layout++){
  ++cases;++disabled_cases;Heap h;setup(h);auto table=uintptr_t(h.table.data()),thread=uintptr_t(h.thread.data());
  auto grid=h.obj(0x2d00),controller=h.obj(0x2e00),closure=h.obj(0x2f00);h.put<uintptr_t>(table,0x42a8,grid);h.ptr(grid,0x1f,h.handler);h.put<uintptr_t>(table,0x2710,controller);
  h.put<uintptr_t>(thread,0x98,closure);h.put<uintptr_t>(thread,0xa0,uintptr_t(image.base+0x1927524));
  const int option_x=layout==2?5:4;const auto top=uintptr_t(&h.stack[1800]);Call c{uintptr_t(image.base+0xeac820),h.n,uintptr_t(layout),top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,thread,0,0,0,0,uintptr_t(option_x),uintptr_t(layout),0};call_geometry(&c);auto cell=c.integer_result;
  check(c.returned==top,"disabled original option sequence restores stack");
  check(h.get<uintptr_t>(thread,0xc8)==uintptr_t(option_x),"disabled original BL reaches variable callee with OEM 4/4/5");
  check(h.get<int64_t>(cell,7)==option_x&&h.get<int64_t>(cell,0xf)==7,"disabled payload retains OEM column and original returned row path");
  check(h.get<uintptr_t>(thread,0xb8)==cell,"disabled original load still receives original option object");
 }
 for(size_t k=0;k<4;k++)check(memcmp(image.base+guarded[k].first,before[k].data(),guarded[k].second)==0,"complete preview and getter bodies remain original");
 check(normalized_mismatches==0,"narrow phone candidate has no column mismatch in observed option creation");
 check(option_mismatches==180,"all observed mismatches are exactly columns different from OEM option 4/4/5");
 std::cout<<"{\"passed\":true,\"cases\":"<<cases<<",\"checks\":"<<checks<<",\"prepare_cases\":"<<prepare_cases<<",\"creation_cases\":"<<creation_cases<<",\"option_mismatches\":"<<option_mismatches<<",\"normalized_mismatches\":"<<normalized_mismatches<<",\"disabled_cases\":"<<disabled_cases<<",\"candidate_consistency_passed\":false,\"device_status\":\"NOT_TESTED\"}\n";
}
