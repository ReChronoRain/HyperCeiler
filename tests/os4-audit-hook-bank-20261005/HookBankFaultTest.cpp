#include "hook_bank.h"
#include <cstdio>
#include <cstring>
#include <vector>
#include <string>
using Slot=nhk::InlineSlot<4>;using Words=nhk::SlotWords<4>;
struct Process {
 Words original{0xa9bf79fd,0xaa0f03fd,0xd10081ef,0xf81f83a1};
 Words patch{0x58000051,0xd61f0220,0x12345678,0x87654321};
 Words foreign{0x14000004,0xd503201f,0xd503201f,0xd503201f};
 Words live=original;void* continuation=(void*)0xc0ffee;
 int reads=0,writes=0,installs=0,unhooks=0,failRead=0,rc=0,unhookRc=0;
 bool noContinuation=false,noPatch=false,failWrite=false,copyOnFailure=false,noWrite=false,foreignProtect=false,foreignBackendFailure=false,changedFailurePointer=false,clearFailurePointer=false;
 bool freedWhilePatch=false;std::string event;
} g;
int checks=0,failed=0,cases=0;const char*name="";
void ok(bool x,const char*why){checks++;if(!x){failed++;std::printf("FAIL %s: %s\n",name,why);}}
bool read(const Slot&,Words&w){g.reads++;if(g.failRead==g.reads)return false;w=g.live;return true;}
bool write(uintptr_t,const Words&w){g.writes++;if(!g.noWrite&&(!g.failWrite||g.copyOnFailure))g.live=w;return !g.failWrite;}
int install(void*,void*,void**p){g.installs++;if(g.rc){if(g.foreignBackendFailure)g.live=g.foreign;if(g.changedFailurePointer)*p=(void*)0xBADF00D;if(g.clearFailurePointer)*p=nullptr;return g.rc;}if(!g.noPatch)g.live=g.patch;*p=g.noContinuation?nullptr:g.continuation;return 0;}
int unhook(void*){g.unhooks++;if(g.live!=g.original)g.freedWhilePatch=true;return g.unhookRc;}
bool protect(uintptr_t,size_t){if(g.foreignProtect)g.live=g.foreign;return true;}
nhk::InlineHookHost<4> host(){nhk::InlineHookHost<4> h;h.read_slot=read;h.write_words=write;h.hook_install=install;h.hook_uninstall=unhook;h.protect_range=protect;h.on_guard=[](const nhk::HookEvent&e){g.event=e.reason;};return h;}
Slot fresh(){Slot s;s.address=0x1000;s.replacement=(void*)0xbeef;s.original=&g.continuation;s.original_words=g.original;return s;}
Slot armed(){auto s=fresh();s.registered=true;s.patch_known=true;s.patch_words=g.patch;g.live=g.patch;
#ifdef HC_NEW_BANK
 s.backend_owned=true;
#endif
 return s;}
bool pending(const Slot&s){
#ifdef HC_NEW_BANK
 return nhk::slot_has_pending(s);
#else
 (void)s;return false;
#endif
}
void reset(const char*n){g=Process{};name=n;cases++;}
auto read_all=[](auto,auto,auto out){g.reads++;for(auto&w:out)w=g.live;return true;};
bool healthy(Slot s){std::array<Slot,1>a{s};return nhk::slots_healthy(a,[](auto,auto,auto &out){g.reads++;out[0]=g.live;return true;});}
int main(int argc,char**){
 if(argc>1){g=Process{};std::array<Slot,1>a{armed()};std::vector<size_t>order(4096,0);bool good=nhk::ordered_slots_healthy(a,order,read_all);std::printf("OVERLONG_ORDER=%s; reads=%d\n",good?"accepted":"rejected",g.reads);return good||g.reads?1:0;}
 reset("normal install");{auto s=fresh();auto h=host();ok(nhk::install_slot(s,h),"success");ok(s.registered&&s.patch_known&&healthy(s),"verified patch health");}
 reset("nonbranch foreign first preimage");{auto s=fresh();g.live=g.foreign;g.live[0]=0xd503201f;auto before=g.live;ok(!nhk::install_slot(s,host()),"must reject changed scalar/frame word");ok(g.installs==0&&g.live==before&&s.original_words==g.original,"do not adopt or write foreign preimage");}
 reset("branch foreign first preimage");{auto s=fresh();g.live=g.foreign;ok(!nhk::install_slot(s,host()),"B entry is foreign");ok(g.installs==0&&g.live==g.foreign,"foreign B not overwritten");}
 reset("protect callback changes preimage");{auto s=fresh();g.foreignProtect=true;ok(!nhk::install_slot(s,host()),"second preimage check");ok(g.installs==0&&g.live==g.foreign,"protect callback mutation not overwritten");}
 reset("installed patch read fails");{auto s=fresh();g.failRead=
#ifdef HC_NEW_BANK
 3;
#else
 2;
#endif
 bool result=nhk::install_slot(s,host());ok(!result&&!s.registered&&!s.patch_known,"unverified result not registered");ok(pending(s)&&g.continuation!=nullptr,"keep backend/continuation obligation");g.failRead=0;int installs=g.installs;ok(!healthy(s),"no late health adoption");std::array<Slot,1>a{s};ok(!nhk::ensure_slots_live(a,host(),std::vector<size_t>{0})&&g.installs==installs,"do not reinstall unknown patch");}
 reset("backend success no patch");{auto s=fresh();g.noPatch=true;ok(!nhk::install_slot(s,host()),"unchanged prologue is not installed patch");ok(!s.registered&&!healthy(s)&&g.live==g.original,"no readiness");ok(g.unhooks==1&&!g.freedWhilePatch,"clean unmodified backend record");}
 reset("failed backend mutates code");{auto s=fresh();g.rc=-1;g.foreignBackendFailure=true;ok(!nhk::install_slot(s,host())&&pending(s),"failed mutation retained uncertain");g.rc=0;g.foreignBackendFailure=false;int calls=g.installs;ok(!nhk::install_slot(s,host())&&g.installs==calls&&g.live==g.foreign,"no overwrite/reinstall of uncertain failure");}
 reset("missing continuation cleanup fails");{auto s=fresh();g.noContinuation=true;g.failWrite=true;ok(!nhk::install_slot(s,host()),"refuses null");ok(pending(s)&&s.patch_known&&!s.registered,"cleanup ownership retained");ok(g.unhooks==0&&g.live==g.patch,"do not release before restoration");ok(g.event.find("incomplete")!=std::string::npos,"no false restored log");}
 reset("missing continuation cleanup recovers");{auto s=fresh();g.noContinuation=true;g.failWrite=true;nhk::install_slot(s,host());g.failWrite=false;std::array<Slot,1>a{s};nhk::ensure_slots_live(a,host(),std::vector<size_t>{0});ok(g.live==g.original&&g.unhooks==1&&!g.freedWhilePatch,"restore before release");ok(!pending(a[0])&&!a[0].registered,"retire after verified cleanup");}
 reset("lost continuation foreign edit");{auto s=armed();g.continuation=nullptr;g.live=g.foreign;std::array<Slot,1>a{s};ok(!nhk::ensure_slots_live(a,host(),std::vector<size_t>{0}),"lost pointer refuses");ok(g.writes==0&&g.unhooks==0&&g.live==g.foreign&&a[0].patch_known,"foreign words not restored over; record retained");}
 reset("restore foreign words");{auto s=armed();s.registered=false;g.live=g.foreign;ok(!nhk::restore_patch_words(s,host()),"reject foreign");ok(g.writes==0&&g.live==g.foreign,"do not overwrite");}
 reset("restore protect race");{auto s=armed();s.registered=false;g.live=g.original;g.foreignProtect=true;ok(!nhk::restore_patch_words(s,host()),"reject changed pre-write words");ok(g.writes==0&&g.live==g.foreign,"do not overwrite callback edit");}
 reset("writer reports success without copy");{auto s=armed();s.registered=false;g.live=g.original;g.noWrite=true;ok(!nhk::restore_patch_words(s,host())&&!s.registered,"readback required");ok(pending(s)&&!healthy(s),"do not lose write obligation");}
 reset("writer false after bytes changed");{auto s=armed();s.registered=false;g.live=g.original;g.failWrite=true;g.copyOnFailure=true;ok(!nhk::restore_patch_words(s,host()),"false result");ok(g.live==g.patch&&pending(s)&&!healthy(s),"bytes do not prove permission restoration");g.failWrite=false;int writes=g.writes;ok(nhk::restore_patch_words(s,host())&&g.writes==writes+1&&healthy(s),"retry writer before discharging obligation");}
 reset("rearm readback fails");{auto s=armed();s.registered=false;g.live=g.original;g.failRead=3;ok(!nhk::restore_patch_words(s,host())&&pending(s)&&!s.registered,"keep unverified write pending");}
 reset("registered install is not unconditional success");{auto s=armed();g.live=g.foreign;ok(!nhk::install_slot(s,host())&&g.installs==0,"registered still checks live proof");}
 reset("full health unknown patch");{auto s=armed();s.patch_known=false;std::array<Slot,1>a{s};ok(!nhk::slots_healthy(a,[](auto,auto,auto &o){o[0]=g.live;return true;}),"unknown patch not healthy");ok(!a[0].patch_known,"health does not mutate ownership");}
 reset("ordered health unknown patch");{auto s=armed();s.patch_known=false;std::array<Slot,1>a{s};ok(!nhk::ordered_slots_healthy(a,std::vector<size_t>{0},read_all),"unknown patch not healthy");ok(!a[0].patch_known,"ordered health no adoption");}
 reset("repair unknown patch");{auto s=armed();s.patch_known=false;std::array<Slot,1>a{s};ok(!nhk::ensure_slots_live(a,host(),std::vector<size_t>{0}),"unknown words not adopted");ok(!a[0].patch_known&&g.writes==0,"no repair write/adoption");}
 reset("duplicate order health");{std::array<Slot,2>a{armed(),armed()};a[1].address+=16;ok(!nhk::ordered_slots_healthy(a,std::vector<size_t>{0,0},read_all)&&g.reads==0,"duplicates rejected before read_all");}
 reset("invalid late index preflight");{std::array<Slot,1>a{fresh()};ok(!nhk::ensure_slots_live(a,host(),std::vector<size_t>{0,1}),"invalid index");ok(g.reads==0&&g.installs==0&&g.live==g.original,"no partial installation before invalid order");}
 reset("overlapping patch ranges");{std::array<Slot,2>a{armed(),armed()};a[1].address+=4;ok(!nhk::ordered_slots_healthy(a,std::vector<size_t>{0,1},read_all)&&g.reads==0,"overlap rejected");}
 reset("full bank overlapping ranges");{std::array<Slot,2>a{armed(),armed()};a[1].address+=4;ok(!nhk::slots_healthy(a,[](auto,auto,auto&o){g.reads++;for(auto&w:o)w=g.live;return true;})&&g.reads==0,"full bank overlap rejected before I/O");}
 reset("unaligned patch range");{std::array<Slot,1>a{armed()};a[0].address++;ok(!nhk::ordered_slots_healthy(a,std::vector<size_t>{0},read_all)&&g.reads==0,"four-byte instruction alignment");}
 reset("patch endpoint overflow");{std::array<Slot,1>a{armed()};a[0].address=UINTPTR_MAX-7;ok(!nhk::ordered_slots_healthy(a,std::vector<size_t>{0},read_all)&&g.reads==0,"full range must not overflow");}
 reset("uninstall restore fails");{auto s=armed();g.failWrite=true;ok(!nhk::uninstall_slot(s,host()),"failure");ok(g.unhooks==0&&!g.freedWhilePatch&&g.continuation!=nullptr&&pending(s),"continuation not released while live patch reachable");}
 reset("uninstall false after original copied");{auto s=armed();g.failWrite=true;g.copyOnFailure=true;ok(!nhk::uninstall_slot(s,host())&&g.unhooks==0&&pending(s),"retain permission obligation");g.failWrite=false;int writes=g.writes;ok(nhk::uninstall_slot(s,host())&&g.writes==writes+1&&g.unhooks==1&&!pending(s),"retry restoration then unhook once");}
 reset("unhook failure retries after restore");{auto s=armed();g.unhookRc=-1;ok(!nhk::uninstall_slot(s,host()),"failed backend");ok(g.live==g.original&&pending(s)&&g.continuation!=nullptr&&!g.freedWhilePatch,"orig before backend; record kept");g.unhookRc=0;ok(nhk::uninstall_slot(s,host())&&g.unhooks==2&&!pending(s),"release retry");}
 reset("post-unhook read failure no double release");{auto s=armed();g.failRead=4;ok(!nhk::uninstall_slot(s,host())&&pending(s),"keep pending final verification");g.failRead=0;ok(nhk::uninstall_slot(s,host())&&g.unhooks==1&&!pending(s),"do not unhook already freed record twice");}
 reset("foreign uninstall preserved");{auto s=armed();g.live=g.foreign;ok(!nhk::uninstall_slot(s,host())&&g.writes==0&&g.unhooks==0&&g.live==g.foreign,"ownership guard");}
 reset("live patch loss repairs known bank");{auto s=armed();g.live=g.original;g.rc=-1;std::array<Slot,1>a{s};ok(nhk::ensure_slots_live(a,host(),std::vector<size_t>{0})&&healthy(a[0]),"fallback known patch survives already-owned rc");}

 reset("failed backend replaces continuation");{auto s=armed();s.registered=false;g.live=g.original;g.rc=-1;g.changedFailurePointer=true;ok(!nhk::install_slot(s,host())&&pending(s),"uncertain continuation retained");ok(!nhk::restore_patch_words(s,host())&&g.writes==0&&g.live==g.original,"do not arm a known patch against an unverified new continuation");}
 reset("individual install overflow");{auto s=fresh();s.address=UINTPTR_MAX-7;ok(!nhk::install_slot(s,host())&&g.reads==0&&g.installs==0,"direct API also bounds range");}

 reset("unverified success retires only after original returns");{auto s=fresh();
#ifdef HC_NEW_BANK
 g.failRead=3;
#else
 g.failRead=2;
#endif
 nhk::install_slot(s,host());g.failRead=0;
#ifdef HC_NEW_BANK
 ok(!nhk::settle_slot_pending(s,host())&&g.unhooks==0&&g.live==g.patch,"unknown patch quarantined");g.live=g.original;ok(nhk::settle_slot_pending(s,host())&&!pending(s)&&g.unhooks==1,"original proof retires backend record");
#else
 ok(false,"original-proof retirement missing");ok(false,"unknown backend state not tracked");
#endif
 }
 reset("1000 failed permission cleanups retain record");{auto s=armed();g.failWrite=true;g.copyOnFailure=true;nhk::uninstall_slot(s,host());int freed=g.unhooks;
#ifdef HC_NEW_BANK
 for(int i=0;i<1000;i++)ok(!nhk::settle_slot_pending(s,host())&&pending(s)&&g.unhooks==freed,"permission failure stays pending");g.failWrite=false;ok(nhk::settle_slot_pending(s,host())&&!pending(s)&&g.unhooks==freed+1,"recover without new backend install");
#else
 ok(false,"permission pending model missing");(void)freed;
#endif
 }

 reset("successful backend changes previously known patch");{auto s=armed();s.registered=false;g.live=g.original;Words prior=s.patch_words;g.patch[2]^=0x100;ok(!nhk::install_slot(s,host())&&pending(s),"changed backend patch remains unverified");g.live=g.original;ok(!nhk::restore_patch_words(s,host())&&g.writes==0&&s.patch_words==prior,"do not resurrect an old trampoline after backend replacement");}

 reset("failed backend clears prior continuation");{auto s=armed();s.registered=false;g.live=g.original;g.rc=-1;g.clearFailurePointer=true;ok(!nhk::install_slot(s,host())&&pending(s),"null failure output is not erased as evidence");ok(!nhk::restore_patch_words(s,host())&&g.writes==0&&g.live==g.original,"do not revive possibly released old continuation");}
 reset("empty order no I/O");{std::array<Slot,0>a{};ok(nhk::ensure_slots_live(a,host(),{})&&nhk::ordered_slots_healthy(a,{},read_all)&&g.reads==0,"empty optional bank");}
 std::printf("HOOK_BANK_FAULTS=%d cases; %d checks; failed=%d\n",cases,checks,failed);return failed?1:0;
}
