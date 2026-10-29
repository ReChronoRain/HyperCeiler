#include "nativehook/hook_bank.h"
#include <cassert>
#include <cstdio>
using Slot=nhk::InlineSlot<4>;using Words=nhk::SlotWords<4>;
Words live{},original{1,2,3,4},patch{5,6,7,8};
void* continuation=nullptr;int installs=0;bool fail_backend=false,fail_protection=false,null_continuation=false,throw_sink=false;
std::vector<nhk::DiagnosticCode> events;
bool read(const Slot&,Words& w){w=live;return true;}
bool write(uintptr_t,const Words& w){live=w;return true;}
int install(void*,void*,void** out){++installs;if(fail_backend)return 1;live=patch;*out=null_continuation?nullptr:reinterpret_cast<void*>(0x1234);return 0;}
int uninstall(void*){return 0;}
bool protect(uintptr_t,size_t){return !fail_protection;}
void sink(const nhk::DiagnosticEvent& e){if(throw_sink)throw 5;events.push_back(e.code);}
Slot slot(){Slot s{};s.address=0x1000;s.replacement=reinterpret_cast<void*>(0x2000);s.original=&continuation;s.original_words=original;return s;}
int main(){
 nhk::InlineHookHost<4> host{};host.read_slot=read;host.write_words=write;host.hook_install=install;host.hook_uninstall=uninstall;host.protect_range=protect;host.on_diagnostic=sink;
 live=original;auto s=slot();assert(nhk::install_slot(s,host));assert(events.back()==nhk::DiagnosticCode::installed);
 auto count=events.size();for(int i=0;i<10000;i++)assert(nhk::install_slot(s,host));assert(events.size()==count&&installs==1);
 std::array<Slot,1> bank{s};std::array<size_t,1> order{0};
 live=original;assert(nhk::ensure_slots_live(bank,host,order));assert(events[events.size()-2]==nhk::DiagnosticCode::hook_lost&&events.back()==nhk::DiagnosticCode::rearmed);assert(installs==1);
 live={99,99,99,99};assert(!nhk::ensure_slots_live(bank,host,order));assert(live[0]==99&&events.back()==nhk::DiagnosticCode::foreign_modification);
 live=original;s=slot();fail_backend=true;assert(!nhk::install_slot(s,host));assert(events.back()==nhk::DiagnosticCode::install_failed);fail_backend=false;
 live=original;s=slot();null_continuation=true;assert(!nhk::install_slot(s,host));assert(events.back()==nhk::DiagnosticCode::continuation_invalid);null_continuation=false;
 live=patch;s=slot();assert(!nhk::install_slot(s,host));assert(events.back()==nhk::DiagnosticCode::validation_failed);
 live=original;s=slot();fail_protection=true;assert(!nhk::install_slot(s,host));assert(events.back()==nhk::DiagnosticCode::protection_failed);fail_protection=false;
 live=original;s=slot();throw_sink=true;assert(nhk::install_slot(s,host));assert(nhk::slot_ready(s));
 nhk::ResolverEvidence proof{};assert(!nhk::evidence_acceptable(proof));proof.candidate_count=2;proof.disambiguators={"diagnostics cannot relax uniqueness"};assert(!nhk::evidence_acceptable(proof));proof.candidate_count=1;assert(nhk::evidence_acceptable(proof));
 puts("Native diagnostics PASS: real install/health/re-arm paths, 10000 warm checks, foreign bytes retained, null continuation, protection/backend refusal, throwing observer isolated, resolver uniqueness unchanged");
}
