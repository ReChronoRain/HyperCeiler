/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include <array>
#include <atomic>
#include <bit>
#include <cstdint>
#include <mutex>
#include <chrono>
#include <cstring>
#include <fstream>
#include <iostream>
#include <limits>
#include <stdexcept>
#include <string>
#include <thread>
#include <vector>
// Only expose the actual cache's atomic sequence/mutex for deterministic stalled-writer tests.
#define private public
#include "targets/weather/weather_snapshot.h"
#undef private
using namespace weather_snapshot;
static int passed=0,failed=0;
void require(bool b,const char *why="assertion"){if(!b)throw std::runtime_error(why);}
void long_put(Words &w,size_t i,int64_t v){auto bits=std::bit_cast<uint64_t>(v);w[i]=std::bit_cast<int32_t>(static_cast<uint32_t>(bits));w[i+1]=std::bit_cast<int32_t>(static_cast<uint32_t>(bits>>32));}
Words active(int64_t rev=1,int32_t level=2,int64_t epoch=11){Words w{};w[0]=kAck;w[1]=1;w[2]=1;long_put(w,3,epoch);long_put(w,5,rev);long_put(w,7,15'000'000'000LL);w[9]=level;w[10]=3;w[11]=1;return w;}
Words disabled(int64_t rev=1){auto w=active(rev);w[2]=0;long_put(w,7,0);w[9]=3;w[10]=0;w[11]=0;return w;}
void run(const std::string&name,auto test){try{test();++passed;std::cout<<"PASS "<<name<<'\n';}catch(const std::exception&e){++failed;std::cout<<"FAIL "<<name<<" "<<e.what()<<'\n';}}
void rejected(const Words&w,int64_t now=10'000'000'000LL){Snapshot s;s.epoch=777;s.device_level=1;require(!decode(w,now,s));require(s.epoch==777&&s.device_level==1,"failed decode changed output");}
std::vector<uint8_t> bytes(const std::string&p){std::ifstream f(p,std::ios::binary);require(bool(f),"missing corpus");return {std::istreambuf_iterator<char>(f),{}};}
bool fixed_words(const std::vector<uint8_t>&b,Words&w){if(b.size()!=48)return false;std::memcpy(w.data(),b.data(),48);return true;}
int main(int argc,char **argv){
 require(argc==2,"expected generated corpus index");
 run("wire constants independent HWS1",[]{require(kTransaction==0x00485753&&kAck==0x48575331&&kWordCount==12&&kByteCount==48&&sizeof(Words)==48);});
 std::ifstream index(argv[1]);std::string name,path;int expected,available,allowed,level,card,blur;int64_t now,epoch,revision,deadline;
 while(index>>name>>path>>now>>expected>>available>>allowed>>epoch>>revision>>deadline>>level>>card>>blur){
  run("production Java writer corpus "+name,[&]{auto b=bytes(path);Words w{};require(fixed_words(b,w));Snapshot s;bool ok=decode(w,now,s);require(ok==bool(expected),"corpus decode decision");if(ok){require(s.available==bool(available)&&s.allowed==bool(allowed));require(s.epoch==epoch&&s.revision==revision&&s.valid_until==deadline);require(s.device_level==level&&s.card_type==card&&s.unlock_blur==bool(blur));}});
 }
 run("host byte adapter refuses short and extra buffers (transport obligation)",[]{Words w{};for(size_t n=0;n<100;n++){std::vector<uint8_t>b(n);require(fixed_words(b,w)==(n==48));}});
 for(int32_t level=0;level<=3;level++)run("raw device no remap "+std::to_string(level),[=]{Snapshot s;require(decode(active(1,level),10'000'000'000LL,s));require(s.device_level==level);});
 for(int32_t v:{std::numeric_limits<int32_t>::min(),-1,4,std::numeric_limits<int32_t>::max()})run("invalid raw device "+std::to_string(v),[=]{auto w=active();w[9]=v;rejected(w);});
 for(int32_t v:{-1,1,4,INT32_MAX})run("invalid raw card "+std::to_string(v),[=]{auto w=active();w[10]=v;rejected(w);});
 for(int32_t v:{-1,2,INT32_MAX})run("invalid raw blur "+std::to_string(v),[=]{auto w=active();w[11]=v;rejected(w);});
 for(size_t idx:{size_t(1),size_t(2)})for(int32_t v:{-1,2,INT32_MAX})run("invalid available/flags "+std::to_string(idx)+":"+std::to_string(v),[=]{auto w=active();w[idx]=v;rejected(w);});
 run("wrong ACK cannot decode",[]{auto w=active();w[0]=0x48435331;rejected(w);});
 for(size_t idx:{size_t(3),size_t(5)})for(int64_t v:{INT64_MIN,int64_t(-1),int64_t(0)})run("negative/zero active token "+std::to_string(idx)+":"+std::to_string(v),[=]{auto w=active();long_put(w,idx,v);rejected(w);});
 run("long words preserve low signed/high bits exactly",[]{auto w=active();long_put(w,3,0x11223344aabbccddLL);long_put(w,5,0x22334455ddeeff88LL);Snapshot s;require(decode(w,10'000'000'000LL,s));require(s.epoch==0x11223344aabbccddLL&&s.revision==0x22334455ddeeff88LL);});
 for(int64_t t:{int64_t(-1),int64_t(0),INT64_MIN})run("nonpositive BOOTTIME "+std::to_string(t),[=]{rejected(active(),t);});
 run("active exact expiry refuses and one nanosecond before accepts",[]{Snapshot s;require(decode(active(),14'999'999'999LL,s));rejected(active(),15'000'000'000LL);});
 run("TTL may not exceed five seconds",[]{auto w=active();long_put(w,7,15'000'000'001LL);rejected(w);});
 run("negative deadline and wrapped deadline rejected",[]{auto w=active();long_put(w,7,INT64_MIN);rejected(w);long_put(w,7,-1);rejected(w);});
 run("signed maximum deadline safe subtraction no overflow",[]{auto w=active();long_put(w,7,INT64_MAX);rejected(w,1);Snapshot s;require(decode(w,INT64_MAX-5'000'000'000LL,s));rejected(w,INT64_MAX);});
 run("disabled canonical level3 accepted",[]{Snapshot s;require(decode(disabled(),10'000'000'000LL,s));require(s.available&&!s.allowed&&s.device_level==3&&s.card_type==0&&!s.unlock_blur&&s.valid_until==0);});
 run("unavailable cold and positive invalidation both accepted",[]{Snapshot s;auto w=unavailable_words();require(decode(w,1,s));require(!s.available&&!s.allowed&&s.device_level==3);long_put(w,3,11);long_put(w,5,12);require(decode(w,1,s));require(s.epoch==11&&s.revision==12);});
 for(size_t i:{size_t(9),size_t(10),size_t(11)})run("inactive noncanonical payload refused "+std::to_string(i),[=]{for(auto w:{disabled(),unavailable_words()}){w[i]=i==9?0:1;rejected(w);}});
 run("inactive deadline cannot leak lease",[]{auto w=disabled();long_put(w,7,1);rejected(w);w=unavailable_words();long_put(w,7,1);rejected(w);});
 run("inactive mixed or negative tokens rejected",[]{for(auto w:{disabled(),unavailable_words()}){long_put(w,3,1);long_put(w,5,0);rejected(w);long_put(w,5,1);long_put(w,3,-1);rejected(w);}auto w=disabled();long_put(w,3,0);long_put(w,5,0);rejected(w);});
 run("unavailable cannot have allowed bit",[]{auto w=active();w[1]=0;rejected(w);});
 run("cold cache cannot authorize",[]{Cache c;Snapshot s;require(!c.read(10'000'000'000LL,s));});
 run("cache expiry never renews original deadline",[]{Cache c;Snapshot s;require(c.publish(active(),10'000'000'000LL));for(int i=0;i<100;i++){require(c.read(12'000'000'000LL,s));require(s.valid_until==15'000'000'000LL);}require(!c.read(15'000'000'000LL,s));});
 run("same-epoch revision rollback revokes",[]{Cache c;Snapshot s;require(c.publish(active(20),10'000'000'000LL));require(!c.publish(active(19),10'000'000'000LL));require(c.read(10'000'000'000LL,s)&&!s.allowed&&!s.available);});
 run("revoke retains revision watermark",[]{Cache c;Snapshot s;require(c.publish(active(20),10'000'000'000LL));c.revoke();require(!c.publish(active(19),10'000'000'000LL));require(c.read(10'000'000'000LL,s)&&!s.allowed);require(c.publish(active(20),10'000'000'000LL));});
 run("cold unavailable publication cannot reset revision watermark",[]{Cache c;require(c.publish(active(20),10'000'000'000LL));require(c.publish(unavailable_words(),10'000'000'000LL));require(!c.publish(active(19),10'000'000'000LL));});
 run("malformed publication revokes without clearing watermark",[]{Cache c;Snapshot s;require(c.publish(active(20),10'000'000'000LL));auto bad=active(21);bad[0]=0;require(!c.publish(bad,10'000'000'000LL));require(!c.publish(active(19),10'000'000'000LL));require(c.read(10'000'000'000LL,s)&&!s.allowed);});
 run("positive unavailable advances same-epoch highwater",[]{Cache c;require(c.publish(active(20),10'000'000'000LL));auto w=unavailable_words();long_put(w,3,11);long_put(w,5,30);require(c.publish(w,10'000'000'000LL));require(!c.publish(active(29),10'000'000'000LL));require(c.publish(active(31),10'000'000'000LL));});
 run("opaque epoch change accepts numerically smaller generation",[]{Cache c;Snapshot s;require(c.publish(active(999,1,20),10'000'000'000LL));require(c.publish(active(1,2,10),10'000'000'000LL));require(c.read(10'000'000'000LL,s)&&s.epoch==10&&s.revision==1);});
 run("duplicate identical publication allowed without renewing deadline",[]{Cache c;Snapshot s;require(c.publish(active(10),10'000'000'000LL));require(c.publish(active(10),12'000'000'000LL));require(c.read(12'000'000'000LL,s)&&s.valid_until==15'000'000'000LL);});
 run("odd sequence read never waits on held writer mutex",[]{Cache c;Snapshot s;s.epoch=777;require(c.publish(active(),10'000'000'000LL));std::lock_guard hold(c.writer_mutex_);c.sequence_.store(1);auto start=std::chrono::steady_clock::now();for(int n=0;n<100'000;n++)require(!c.read(10'000'000'000LL,s));require(s.epoch==777);require(std::chrono::steady_clock::now()-start<std::chrono::seconds(5),"bounded iteration took unexpectedly long");});
 run("four readers see complete same-publication settings only",[]{Cache c;std::atomic<bool> stop{false},bad{false};std::atomic<uint64_t> samples{0};std::vector<std::thread> readers;for(int i=0;i<4;i++)readers.emplace_back([&]{Snapshot s;while(!stop.load()){if(c.read(10'000'000'000LL,s)&&s.allowed){auto n=s.revision;int expected_level=n%3;int expected_card=expected_level==0?0:expected_level+1;if(s.epoch!=11||s.device_level!=expected_level||s.card_type!=expected_card||s.unlock_blur!=bool(n&1)||s.valid_until!=15'000'000'000LL)bad=true;samples++;}}});for(int64_t n=1;n<=40'000;n++){auto w=active(n,n%3);w[10]=n%3==0?0:n%3+1;w[11]=n&1;require(c.publish(w,10'000'000'000LL));}stop=true;for(auto&t:readers)t.join();require(!bad.load()&&samples.load()>0);});
 run("two racing same-epoch writers preserve greatest revision highwater",[]{Cache c;std::thread a([&]{for(int64_t n=1;n<=10'000;n++)c.publish(active(n),10'000'000'000LL);});std::thread b([&]{for(int64_t n=10'001;n<=20'000;n++)c.publish(active(n),10'000'000'000LL);});a.join();b.join();require(!c.publish(active(19'999),10'000'000'000LL));require(c.publish(active(20'000),10'000'000'000LL));Snapshot s;require(c.read(10'000'000'000LL,s)&&s.revision==20'000);});
 std::cout<<"RESULT passes="<<passed<<" failures="<<failed<<"; production header and Java writer corpus; no Android/phone execution\n";return failed?1:0;
}
