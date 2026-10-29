/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_recent_dart_scene.h"
#include <cstring>
#include <fstream>
#include <iostream>
#include <map>
using namespace home_recent_bar::dart_scene;
namespace {
unsigned checks=0;
void check(bool b){if(!b){std::cerr<<"failed Flutter scene check "<<checks<<"\n";std::abort();}++checks;}
void put(std::vector<std::byte>& b,size_t at,uint32_t v){std::memcpy(b.data()+at,&v,4);}
void put64(std::vector<std::byte>& b,size_t at,uint64_t v){std::memcpy(b.data()+at,&v,8);}
uint32_t load(unsigned reg,unsigned base,uint32_t offset){return 0xf9400000u|(offset/8<<10)|(base<<5)|reg;}
uint32_t mem9(uint32_t op,unsigned reg,unsigned base,int32_t offset){return op|((uint32_t(offset)&511)<<12)|(base<<5)|reg;}
uint32_t call(uint32_t pc,uint32_t to){return 0x94000000u|((uint32_t(int64_t(to)-pc)/4)&0x3ffffffu);}
struct Fixture {
 std::vector<std::byte> bytes;
 uint32_t base,overview,table,sentinel;int32_t field,below,receiver,arg;
 uint32_t G=0x100,B=0x300,C=0x500,P=0x600,W=0x800,T=0xa00,I=0xc00,H=0xc20;
 Fixture(unsigned seed):bytes(0x6000),base(0x500000+seed*0x10000),overview(0x1000+seed*0x40),table(0x78+seed*8),sentinel(0x40+seed*8),field(0x53+seed*8),below(0x57+seed*8),receiver(seed%2?-40:-8),arg(seed%2?-32:-16){
  put(bytes,0,0x464c457f);bytes[4]=std::byte{2};bytes[5]=std::byte{1};bytes[6]=std::byte{1};
  uint16_t type=3;std::memcpy(bytes.data()+16,&type,2);uint16_t machine=183;std::memcpy(bytes.data()+18,&machine,2);put64(bytes,32,64);uint16_t size=56,count=1;std::memcpy(bytes.data()+54,&size,2);std::memcpy(bytes.data()+56,&count,2);
  put(bytes,64,1);put(bytes,68,5);put64(bytes,72,0x1000);put64(bytes,80,base);put64(bytes,96,0x5000);put64(bytes,104,0x5000);
  for(size_t at=0x1000;at<bytes.size();at+=4)put(bytes,at,0xd503201f);
  build(0);
 }
 void body(uint32_t at,Body b,std::initializer_list<std::pair<unsigned,uint32_t>> calls={}){
  for(auto [i,to]:calls)b[i]=call(base+at+i*4,base+to);
  for(size_t i=0;i<b.size();++i)put(bytes,0x1000+at+i*4,b[i]);
 }
 void build(uint32_t shift){
  const uint32_t g=G+shift,b=B+shift,c=C+shift,p=P+shift,w=W+shift,t=T+shift,init=I+shift,barrier=H+shift;
  Body getter={0xa9bf79fd,0xaa0f03fd,0xd10021ef,mem9(0xb8400000,0,1,field),0x8b1c8000,0xf81f83a0,
   load(0,26,table),load(0,0,overview),load(16,27,sentinel),0x6b10001f,0x54000081,0x91400762,0xf9400442,0,
   0xf85f83a1,0x6b00003f,0x910082d0,0x9100c2d1,0x9a910202,0xaa0203e0,0xaa1d03ef,0xa8c179fd,0xd65f03c0};
  body(g,getter,{{13,init}});
  body(b,{0xa9bf79fd,0xaa0f03fd,0xd10021ef,0xaa0103e0,0xf81f83a1,0,0x37200080,0xf85f83a1,0,0x14000002,0x9100c2c0,0xaa1d03ef,0xa8c179fd,0xd65f03c0},{{5,g},{8,c}});
  body(c,{0xa9bf79fd,0xaa0f03fd,mem9(0xb8400000,2,1,below),0x8b1c8042,0,0xaa1d03ef,0xa8c179fd,0xd65f03c0},{{4,p}});
  Body pred(59,0xd503201f);pred[0]=0xa9bf79fd;pred[1]=0xaa0f03fd;pred[2]=0xd10021ef;pred[3]=0xf81f83a2;
  for(unsigned n=0;n<4;++n){unsigned start=n<3?4+n*12:42;pred[start]=load(0,26,table);pred[start+1]=load(0,0,overview+8+n*8);pred[start+2]=load(16,27,sentinel);pred[start+3]=0x6b10001f;pred[start+4]=0x54000081;pred[start+5]=0x91400762;pred[start+6]=0xf9400442;pred[start+7]=call(base+p+(start+7)*4,base+init);}
  const uint32_t tail[]={0x910082d0,0x9100c2d1,0x9a910202,0xaa0203e0,0xaa1d03ef,0xa8c179fd,0xd65f03c0};std::copy(std::begin(tail),std::end(tail),pred.begin()+52);body(p,pred);
  body(w,{0xa9bf79fd,0xaa0f03fd,0xd10021ef,0xf81f83a2,load(0,26,table),load(0,0,overview-8),load(16,27,sentinel),0x6b10001f,0x54000081,0x91400762,0xf9400442,0,
   0xaa0003e1,0xf85f83a0,0x6b01001f,0x54000120,load(0,26,table),load(0,0,overview),load(16,27,sentinel),0x6b10001f,0x54000081,0x91400762,0xf9400442,0,0xaa1603e0,0xaa1d03ef,0xa8c179fd,0xd65f03c0},{{11,init},{23,init}});
  body(t,{0xa9bf79fd,0xaa0f03fd,0xd100a1ef,0xaa0103e3,0xaa0203e0,mem9(0xf8000000,1,29,receiver),mem9(0xf8000000,2,29,arg),
   mem9(0xb8400000,2,3,field),0x8b1c8042,0x6b00005f,0x54000320,0xaa0203e6,0xf81e83a6,
   load(0,26,table),load(0,0,overview-8),load(0,26,table),load(0,0,overview),0,
   0xf85e83a0,mem9(0xf8400000,1,29,receiver),mem9(0xf8400000,2,29,arg),mem9(0xb8000000,0,1,below),
   mem9(0xf8400000,0,29,arg),mem9(0xf8400000,1,29,receiver),mem9(0xb8000000,0,1,field),
   0x385ff030,0x385ff011,0x8a500a30,0xea5c821f,0x54000040,0,load(0,26,table),load(0,0,overview+8),load(16,27,sentinel),0x6b10001f,
   0xaa1603e0,0xaa1d03ef,0xa8c179fd,0xd65f03c0},{{17,p},{30,barrier}});
  body(init,{0xd65f03c0});body(barrier,{0xd65f03c0});
 }
 std::optional<Plan> resolve()const{auto c=Code::parse(bytes);return c?home_recent_bar::dart_scene::resolve(*c):std::nullopt;}
 void word(uint32_t va,uint32_t value){put(bytes,0x1000+va-base,value);}
};
void sample_checks(const Plan& plan){
 for(uint64_t shift:{0ULL,8ULL}){
  std::map<uint64_t,uint64_t> values;const uint64_t frame=0x5000+shift,thread=0x8000,pool=0xa000,table=0xc000,receiver=0x10001,state=0x12001;
  values[frame+plan.receiver_frame]=receiver;values[thread+plan.static_table]=table;values[table+plan.overview]=state;values[pool+plan.sentinel_pool]=0x14001;values[receiver+plan.state_field]=uint32_t(state);
  // Generic reader keeps the destination's width, including compressed refs.
  auto reader=[&](uint64_t at,auto& out){auto i=values.find(at);if(i==values.end())return false;using T=std::remove_reference_t<decltype(out)>;out=T(i->second);return true;};
  auto got=sample(plan,frame,thread,pool,reader);check(got.result==ReadResult::Ready&&got.active);
  values[receiver+plan.state_field]=0x16001;got=sample(plan,frame,thread,pool,reader);check(got.result==ReadResult::Ready&&!got.active);
  values[receiver+plan.state_field]=uint32_t(state);values[table+plan.overview]=values[pool+plan.sentinel_pool];check(sample(plan,frame,thread,pool,reader).result==ReadResult::Uninitialized);
  values.erase(table+plan.overview);check(sample(plan,frame,thread,pool,reader).result==ReadResult::Memory);
  check(sample(plan,frame+1,thread,pool,reader).result==ReadResult::Frame);
 }
}
}
int main(int argc,char**argv){
 check(compose(0,3)==9); // Flutter overview while all Rust setters still say false.
 check(compose(3,2)==8); // Committed normal state wins over stale native portrait/landscape bits.
 check(compose(4,2)==12);check(compose(2,0)==10);check(compose(0,unsigned(ReadResult::Memory)*4)==0);

 for(unsigned seed=0;seed<12;++seed){Fixture f(seed);auto p=f.resolve();check(bool(p));
  auto code=Code::parse(f.bytes);auto words=replay_words(*code,*p);check(bool(words));
  check((*words)[0]==load(0,26,f.table)&&(*words)[1]==load(0,0,f.overview+8)
      &&(*words)[2]==load(16,27,f.sentinel)&&(*words)[3]==0x6b10001f);
  check(p->observe==f.base+f.T+31*4&&p->state_field==f.field&&p->below_field==f.below&&p->receiver_frame==f.receiver);
  check(p->static_table==f.table&&p->overview==f.overview&&p->sentinel_pool==f.sentinel);
  sample_checks(*p);
  auto reject=[&](auto mutate){auto bad=f;mutate(bad,*p);check(!bad.resolve());};
  reject([](auto& f,const auto& p){f.word(p.getter+3*4,0xd503201f);});
  reject([](auto& f,const auto& p){f.word(p.getter+4*4,0xd503201f);});
  reject([](auto& f,const auto& p){f.word(p.getter+7*4,load(0,0,p.overview+0x100));});
  reject([](auto& f,const auto& p){f.word(p.observe-7*4,0xd503201f);});
  reject([](auto& f,const auto& p){f.word(p.observe-6*4,0xd503201f);});
  reject([](auto& f,const auto& p){f.word(p.transition+7*4,0xd503201f);});
  reject([](auto& f,const auto& p){f.word(p.transition+21*4,0xd503201f);});
  reject([](auto& f,const auto& p){f.word(p.transition+17*4,call(p.transition+17*4,p.transition));});
  reject([](auto& f,const auto& p){f.word(p.transition+10*4,0x14000000u|((p.observe-(p.transition+40))/4));});
  reject([](auto& f,const auto& p){f.word(p.wrapper+6*4,0x37200020);});
  reject([](auto& f,const auto&){f.build(0x2000);});
 }
 for(int i=1;i<argc;++i){std::ifstream file(argv[i],std::ios::binary);check(bool(file));std::vector<char>b((std::istreambuf_iterator<char>(file)),{});auto c=Code::parse(std::as_bytes(std::span(b)));check(bool(c));auto plan=home_recent_bar::dart_scene::resolve(*c);check(bool(plan));check(bool(replay_words(*c,*plan)));std::cout<<"Flutter scene actual image verified "<<argv[i]<<"\n";}
 std::cout<<"Flutter committed scene "<<checks<<" checks passed, independent code/field/static/frame relocation, CFG rejection, word-aligned frames and no Dart allocation\n";
}
