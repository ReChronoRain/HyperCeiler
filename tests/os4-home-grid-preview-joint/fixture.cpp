// Joint private-image experiment: exact preview payload normalization plus
// root's singlepoint-v2 mathematical leaf. No FP-18 type reinterpretation.
#include <tuple>
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wreturn-type"
#define main frozen_singlepoint_main
#include "../os4-home-grid-singlepoint/fixture_v2.cpp"
#undef main
#pragma GCC diagnostic pop
extern "C" void preview_allocate(),preview_cal_bridge(),preview_cal_return(),preview_load_record();
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
 const std::array<std::pair<size_t,uintptr_t>,14> adapters{{
 {0x1b57c08,uintptr_t(secondary_array)},{0x18aa0e0,uintptr_t(geometry_context)},{0xe1d028,uintptr_t(secondary_view)},
 {0xb19564,uintptr_t(geometry_rx_set)},{0x1781f28,uintptr_t(geometry_true)},{0x18a363c,uintptr_t(geometry_sub)},
 {0x18a3aa4,uintptr_t(geometry_add)},{0x868b8c,uintptr_t(geometry_null)},{0x86af0c,uintptr_t(geometry_null)},
 {0xa4ac00,uintptr_t(geometry_zero)},{0x19e4840,uintptr_t(preview_allocate)},{0x192718c,uintptr_t(preview_cal_bridge)},
 {0x1928030,uintptr_t(preview_cal_return)},{0x18bab44,uintptr_t(preview_load_record)}}};
 for(auto[va,target]:adapters)image.jump(va,target);
 const std::array<std::pair<size_t,size_t>,5> guards{{{0xeac820,268},{0xa49adc,900},{0xa4ab58,168},{0xa4418c,560},{0x1927524,220}}};
 std::vector<std::vector<uint8_t>> before;for(auto[va,size]:guards)before.emplace_back(image.base+va,image.base+va+size);
 auto expect=[&](size_t va,uint32_t expected){uint32_t got;memcpy(&got,image.base+va,4);check(got==expected,"exact original joint instruction site");};
 expect(0xa49aec,0xaa0203e0);expect(0xa49af4,0xaa0503e2);expect(0x1927574,0x97c48fb9);
 expect(0xa49bd4,0xf85e83a0);expect(0xa49c28,0xf85e83a0);expect(0xa49c80,0xf85e83a4);
 constexpr size_t mathsite=0xa4ab78;const uint32_t replay[]={0xf85f03a0,0x9e620001,0x1e610802,0xfc1e83a2};check(memcmp(image.base+mathsite,replay,16)==0,"root leaf exact original16 replay");
 const auto leafsize=size_t(grid_quantize_end-grid_quantize_begin),gateoff=size_t(grid_leaf_gate-grid_quantize_begin),expectedoff=size_t(grid_leaf_expected-grid_quantize_begin),branchoff=size_t(grid_leaf_branch-grid_quantize_begin);
 check(leafsize==96&&gateoff==88&&expectedoff==80&&branchoff==72,"exact frozen root leaf96 boundaries");
 memcpy(image.base+image.veneer,grid_quantize_begin,leafsize);image.word(image.veneer+branchoff,branch(image.veneer+branchoff,mathsite+16));
 const uintptr_t expected_caller=uintptr_t(image.base+0xa49c5c);memcpy(image.base+image.veneer+expectedoff,&expected_caller,8);
 image.jump(mathsite,uintptr_t(image.base+image.veneer));
 check(preview_column_end-preview_column_begin==12,"exact frozen payload leaf12 boundaries");
 constexpr size_t payload_off=128;memcpy(image.base+image.veneer+payload_off,preview_column_begin,12);image.word(image.veneer+payload_off+8,branch(image.veneer+payload_off+8,0xa49adc));
 image.word(0x1927574,branch(0x1927574,image.veneer+payload_off,true));
 if(mutation=="lost-payload-write")image.word(image.veneer+payload_off+4,0xd503201f);
 if(mutation=="lost-cell-write")image.word(image.veneer+44,0xd503201f);
 if(mutation=="lost-original-multiply")image.word(image.veneer+64,0xd503201f);
 image.sync();
 unsigned enabled_cases=0,closed_cases=0,negative_margins=0,short_strides=0;
 auto one=[&](int cols,int rows,int layout,double dpr,double width,double height,bool math_enabled){
  ++cases;if(math_enabled)++enabled_cases;else ++closed_cases;Heap h;setup(h,dpr);auto table=uintptr_t(h.table.data()),thread=uintptr_t(h.thread.data());
  h.put<double>(h.handler,7,width);h.put<double>(h.handler,0xf,height);
  auto grid=h.obj(0x2d00),controller=h.obj(0x2e00),closure=h.obj(0x2f00);h.put<uintptr_t>(table,0x42a8,grid);h.ptr(grid,0x1f,h.handler);h.put<uintptr_t>(table,0x2710,controller);
  h.put<uintptr_t>(thread,0x98,closure);h.put<uintptr_t>(thread,0xa0,uintptr_t(image.base+0x1927524));
  const uint64_t gate=math_enabled?1:0;memcpy(image.base+image.veneer+gateoff,&gate,8);
  image.word(image.veneer+payload_off,0xd2800002u|(uint32_t(cols)<<5));
  image.word(0xa49aec,0xd2800002u|(uint32_t(cols)<<5));image.word(0xa49af4,0xd2800003u|(uint32_t(mutation=="old-row-value"?6:rows)<<5));
  __builtin___clear_cache(reinterpret_cast<char*>(image.base+image.veneer+payload_off),reinterpret_cast<char*>(image.base+image.veneer+payload_off+12));
  __builtin___clear_cache(reinterpret_cast<char*>(image.base+0xa49aec),reinterpret_cast<char*>(image.base+0xa49af8));
  const int option_x=layout==2?5:4;const auto top=uintptr_t(&h.stack[1800]);Call c{uintptr_t(image.base+0xeac820),h.n,uintptr_t(layout),top,uintptr_t(h.pool.data()),uintptr_t(h.mem)>>32,h.n,thread,0,0,0,0,uintptr_t(option_x),uintptr_t(layout),0};call_geometry(&c);const auto cellobj=c.integer_result;
  check(c.returned==top,"joint original constructor and caller restore stack");
  check(h.get<int64_t>(cellobj,7)==cols,"joint payload X agrees with selected input");
  check(h.get<int64_t>(cellobj,0xf)==rows,"joint payload Y agrees with captured selected row");
  check(h.get<int64_t>(cellobj,0x17)==layout&&h.get<int64_t>(cellobj,0x1f)==layout,"joint original index and layout metadata retained");
  check(h.get<uintptr_t>(thread,0xb8)==cellobj,"joint original load receives same normalized payload");
  check(h.get<uintptr_t>(thread,0xb0)==4&&h.get<uintptr_t>(thread,0xc0)==8,"joint original reason4 and load8 retained");
  check(h.get<uintptr_t>(thread,0x90)==4,"joint original fixed and tail publish four Rx calls");
  const auto rowrx=h.get<uint32_t>(h.handler,0x43)|(uintptr_t(h.mem)&~uintptr_t(0xffffffff));check(h.get<uint32_t>(rowrx,0x13)==uint32_t(rows*2),"joint original row Rx agrees with payload");
  const double effective=std::isfinite(dpr)&&dpr>0?dpr:1;auto pixel=[&](double x){return std::round(x*effective)/effective;};
  const double raw=std::min(width/cols,height/(rows+1)),cell=h.get<double>(h.handler,0x47),stride=h.get<double>(h.handler,0x4f);
  close(cell,math_enabled?std::trunc(raw*effective)/effective:raw,"joint exact leaf quantization follows its local gate");
  close(h.get<double>(h.handler,0x1f),(width-pixel(cell)*cols)/2,"joint original horizontal centering consumes normalized selected cell");
  close(h.get<double>(h.handler,0x8f),pixel(cell),"joint original rounded cell field is current");
  const double remaining=height-pixel(cell)*(rows+1),margin=(layout<2?remaining-(pixel(stride)-pixel(cell))*(rows+1):remaining)/2;
  close(h.get<double>(h.handler,0x37),margin,"joint original top margin matches updated cell and stride");close(h.get<double>(h.handler,0x67),margin,"joint original bottom margin matches updated cell and stride");
  if(math_enabled){if(margin < -1e-8)++negative_margins;if(stride+1e-8<cell)++short_strides;}
  if(cols==9&&rows==6&&layout==2&&dpr==1&&width==120)std::cerr<<"WITNESS math_gate="<<math_enabled<<" selected="<<cols<<"x"<<rows<<" payload="<<h.get<int64_t>(cellobj,7)<<"x"<<h.get<int64_t>(cellobj,0xf)<<" cell="<<cell<<" margin="<<margin<<"\n";
 };
 for(double dpr:std::array<double,8>{1,2,2.75,3,0,-2,INFINITY,NAN})for(auto[width,height]:std::array<std::pair<double,double>,2>{{{120,260},{360,800}}})for(int cols=3;cols<=9;cols++)for(int rows=4;rows<=13;rows++)for(int layout=0;layout<3;layout++)one(cols,rows,layout,dpr,width,height,true);
 // Only mathematical quantizer is closed. Grid values and typed normalizer are
 // still enabled; this is deliberately not called whole-module disabled.
 for(int layout=0;layout<3;layout++)one(9,6,layout,1,120,260,false);
 check(negative_margins==0,"joint enabled scenarios have nonnegative final margin within1e-8");check(short_strides==0,"joint enabled scenarios have no stride below cell within1e-8");
 expect(0xa49bd4,0xf85e83a0);expect(0xa49c28,0xf85e83a0);expect(0xa49c80,0xf85e83a4);
 image.word(0xa49aec,0xaa0203e0);image.word(0xa49af4,0xaa0503e2);image.word(0x1927574,0x97c48fb9);memcpy(image.base+mathsite,replay,16);
 for(size_t k=0;k<guards.size();k++)check(memcmp(image.base+guards[k].first,before[k].data(),guards[k].second)==0,"after local experiment exact selected complete bodies restored");
 std::cout<<"{\"passed\":true,\"cases\":"<<cases<<",\"checks\":"<<checks<<",\"enabled_cases\":"<<enabled_cases<<",\"quantizer_closed_cases\":"<<closed_cases<<",\"negative_margin_cases\":"<<negative_margins<<",\"short_stride_cases\":"<<short_strides<<",\"device_status\":\"NOT_TESTED\"}\n";
}
