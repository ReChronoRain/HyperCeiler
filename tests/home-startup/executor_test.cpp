/* SPDX-License-Identifier: AGPL-3.0-or-later */
// Runs the actual production executor with isolated memory/backend fixtures.
// The ROM is mapped as data, never executed on the host.
#include "targets/home/home_startup.h"
#include "targets/home/home_startup_plan.generated.h"
#include "nativehook/elf_image.h"
#include <atomic>
#include <cassert>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <functional>
#include <iterator>
#include <new>
#include <string>
#include <thread>
#include <vector>
#include <sys/wait.h>
#include <unistd.h>

using namespace home_startup;
extern "C" uint64_t hc_layout_startup_epoch_gate;
static std::vector<std::byte> mapped;
static uintptr_t base;
static std::atomic<bool> authorized{true}, in_window{false};
static std::atomic<unsigned> forbidden_allocations{0}, cleared{0}, published{0};
static void *originals[15]{};
static unsigned hook_calls, unhook_calls, write_calls, read_calls;
static unsigned fail_hook, null_original, fail_write, fail_read, revoke_at_hook;
static bool fail_all_writes, fail_unhook, foreign_writeback, foreign_hook_tail;
static bool revoke_at_publish, change_auth_at_publish;
static unsigned protect_calls, fail_protect;
static bool fail_rx;
static uintptr_t pending_pages[64]{};
static std::atomic<bool> pause_hook{false}, entered_hook{false}, resume_hook{false};

void *operator new(std::size_t n) {
    if(in_window.load()) ++forbidden_allocations;
    if(void *p=std::malloc(n))return p;
    throw std::bad_alloc();
}
void operator delete(void *p) noexcept { std::free(p); }
void operator delete(void *p,std::size_t) noexcept { std::free(p); }

namespace home_startup::detail {
HookBinding binding(unsigned i) noexcept {
    return i<15 ? HookBinding{reinterpret_cast<void *>(uintptr_t(0x1000+16*i)),&originals[i]}:HookBinding{};
}
void initialize_consumers(uintptr_t b,uintptr_t a,uintptr_t c,uintptr_t d,uintptr_t e,const uint32_t *f) noexcept {
    assert(b==base && a&&c&&d&&e&&f[0]);
}
void publish_consumers(const home_layout::Config&,uint64_t) noexcept {
    ++published;if(revoke_at_publish)request_revoke();
    if(change_auth_at_publish)authorized.store(false);
}
void clear_consumers() noexcept { ++cleared; }
}
bool home_startup_test_read(uintptr_t address,void *out,size_t n) noexcept {
    ++read_calls;
    if(fail_read && read_calls==fail_read)return false;
    if(address<base||address-base>mapped.size()||n>mapped.size()-(address-base))return false;
    std::memcpy(out,reinterpret_cast<void *>(address),n);return true;
}
bool home_startup_test_write(uintptr_t address,const void *data,size_t n) noexcept {
    ++write_calls;
    if(address<base||address-base>mapped.size()||n>mapped.size()-(address-base))return false;
    if(fail_all_writes)return false;
    std::memcpy(reinterpret_cast<void *>(address),data,n);
    if(foreign_writeback && write_calls==1) {
        const uint32_t foreign=0xdeadbeef;std::memcpy(reinterpret_cast<void *>(address),&foreign,4);
    }
    return write_calls!=fail_write;
}
int home_startup_test_protect(uintptr_t page,size_t,int protection) noexcept {
    ++protect_calls;
    if(protect_calls==fail_protect || (fail_rx && !(protection&2)))return -1;
    for(auto &entry:pending_pages) {
        if(entry==page) { if(!(protection&2))entry=0;return 0; }
    }
    if(protection&2)for(auto &entry:pending_pages)if(!entry){entry=page;return 0;}
    return 0;
}
static int fake_hook(void *address,void *,void **original) {
    ++hook_calls;
    if(pause_hook.load()) { entered_hook.store(true); while(!resume_hook.load()) std::this_thread::yield(); }
    if(revoke_at_hook==hook_calls)request_revoke();
    if(fail_hook==hook_calls)return -1;
    *original=null_original==hook_calls?nullptr:reinterpret_cast<void *>(uintptr_t(0xabcdef00+hook_calls*16));
    const uint32_t patch[]={0x58000050,0xd61f0200,uint32_t(hook_calls),0xdecafbad};
    std::memcpy(address,patch,sizeof(patch));
    if(foreign_hook_tail){const uint32_t foreign=0xdeadbeef;std::memcpy(static_cast<char*>(address)+16,&foreign,4);}
    return 0;
}
static int fake_unhook(void *address) {
    ++unhook_calls;if(fail_unhook)return -1;
    const uint32_t va=uintptr_t(address)-base;
    for(const auto &guard:plan::kGuards)if(guard.va<=va&&va+16<=guard.va+guard.size) {
        std::memcpy(address,plan::kGuardBytes+guard.offset+va-guard.va,16);return 0;
    }
    return -1;
}
static bool auth(void *) { return authorized.load(); }
static home_layout::Config all_config() {
    home_layout::Config c;c.grid_enabled=true;c.cell_x=6;c.cell_y=7;
    for(auto &k:c.knobs){k.enabled=true;k.delta_dp=12;}
    c.knobs[6].delta_dp=2;c.knobs[7].delta_dp=1;
    auto &t=c.tweaks;t.folder_enabled=t.pad_enabled=t.fold_enabled=t.icon_scale_enabled=true;
    t.folder_cols=5;t.pad_major=9;t.pad_minor=6;t.fold_major=9;t.fold_minor=6;
    t.recents_no_clear=true;t.hotseat_unlimited=true;t.animation_open_enabled=t.animation_recents_enabled=true;
    t.animation_open_rate_percent=140;t.animation_recents_rate_percent=80;
    return c;
}
static Result execute_config(const home_layout::Config &c) {
    Request request{base,fake_hook,fake_unhook,&c,auth,nullptr};
    in_window.store(true);auto r=execute(request);in_window.store(false);
    assert(forbidden_allocations.load()==0);return r;
}
static bool original_constants() {
    for(const auto &w:plan::kWords) {
        uint32_t value;std::memcpy(&value,reinterpret_cast<void *>(base+w.va),4);
        if(value!=w.original)return false;
    }
    for(const auto &w:plan::kCapacity) {
        uint32_t value;std::memcpy(&value,reinterpret_cast<void *>(base+w.va),4);
        if(value!=w.original)return false;
    }
    return true;
}
static bool original_hooks() {
    for(const auto &h:plan::kHooks)for(const auto &g:plan::kGuards)
        if(g.va<=h.va&&h.va+16<=g.va+g.size && std::memcmp(reinterpret_cast<void *>(base+h.va),
            plan::kGuardBytes+g.offset+h.va-g.va,16))return false;
    return true;
}
static bool gate() { return (__atomic_load_n(&hc_layout_startup_epoch_gate,__ATOMIC_ACQUIRE)&1)!=0; }
static unsigned tests=0,failed=0;
static void test(const char*name,const std::function<void()>& fn) {
    const char *filter=std::getenv("HOME_STARTUP_TEST_FILTER");
    if(filter&&!std::strstr(name,filter))return;
    const char *limit=std::getenv("HOME_STARTUP_TEST_LIMIT");
    if(limit&&tests>=std::strtoul(limit,nullptr,10))return;
    ++tests;std::fflush(nullptr);const pid_t child=fork();assert(child>=0);
    if(child==0) { fn();_exit(0); }
    int status=0;assert(waitpid(child,&status,0)==child);
    const bool ok=WIFEXITED(status)&&WEXITSTATUS(status)==0;
    if(!ok)++failed;
    std::printf("%s %s\n",ok?"PASS":"FAIL",name);
}
int main(int argc,char** argv) {
    assert(argc==2);std::ifstream file(argv[1],std::ios::binary);assert(file);
    std::string raw(std::istreambuf_iterator<char>{file},{});
    std::span<const std::byte> bytes(reinterpret_cast<const std::byte*>(raw.data()),raw.size());
    auto segments=nhk::elf::parse_program_segments(bytes);assert(segments);
    size_t extent=0;for(const auto&s:*segments)if(s.type==1)extent=std::max(extent,size_t(s.vaddr+s.memsz));
    mapped.resize(extent);base=reinterpret_cast<uintptr_t>(mapped.data());
    for(const auto&s:*segments)if(s.type==1)std::memcpy(mapped.data()+s.vaddr,bytes.data()+s.offset,s.filesz);
    test("not prepared",[]{assert(execute_config(all_config()).error==Error::NotPrepared);assert(!hook_calls);});
    test("all supported groups plus honest Rust gaps",[]{
        assert(prepare_for_child());auto r=execute_config(all_config());assert(r.outcome==Outcome::Applied);
        assert(r.declined==(PhoneGridDart|PhoneGridRust|PadGrid|FoldGrid|RecentsAnimationRust));assert(r.applied&&gate()&&hook_calls==15);
        request_revoke();assert(!gate());r=service_revoke();assert(r.error==Error::Revoked&&original_constants());
        assert(!unhook_calls); // Valid originals retained for pass-through.
        auto c=all_config();r=maintenance(&c,true,auth,nullptr);
        assert(r.error==Error::LateConfiguration&&!gate()); // Cached constants need a new Home launch.
    });
    test("all disabled remains neutral",[]{assert(prepare_for_child());home_layout::Config c;
        auto r=execute_config(c);assert(r.applied==0&&r.requested==0&&hook_calls==0&&write_calls==0&&original_constants());});
    test("initial permission refused",[]{assert(prepare_for_child());authorized=false;
        auto r=execute_config(all_config());assert(r.error==Error::NotAuthorized&&!hook_calls&&!write_calls&&!gate());});
    test("saturated revoke epoch never activates",[]{assert(prepare_for_child());
        __atomic_store_n(&hc_layout_startup_epoch_gate,UINT64_MAX-1,__ATOMIC_RELEASE);
        auto r=execute_config(all_config());assert(r.error==Error::NotAuthorized&&!hook_calls&&!gate());});
    test("second observer never retries",[]{assert(prepare_for_child());execute_config(all_config());
        auto n=hook_calls;assert(execute_config(all_config()).outcome==Outcome::Missed&&hook_calls==n);});
    test("disabled nonzero delta rejected",[]{assert(prepare_for_child());auto c=all_config();
        c.knobs[0].enabled=false;assert(execute_config(c).error==Error::UnsupportedConfiguration&&!hook_calls);});
    test("invalid value rejected",[]{assert(prepare_for_child());auto c=all_config();c.tweaks.folder_cols=99;
        assert(execute_config(c).error==Error::UnsupportedConfiguration&&!hook_calls);});
    test("missing config or API refused",[]{assert(prepare_for_child());Request r{base};
        assert(execute(r).error==Error::InvalidRequest);});
    for(unsigned position=1;position<=15;++position) {
        auto label=std::string("hook failure ")+std::to_string(position);
        test(label.c_str(),[position]{assert(prepare_for_child());fail_hook=position;
            auto r=execute_config(all_config());assert(r.outcome==Outcome::Declined&&!gate());
            assert(original_hooks()&&original_constants());});
        label=std::string("missing continuation ")+std::to_string(position);
        test(label.c_str(),[position]{assert(prepare_for_child());null_original=position;
            auto r=execute_config(all_config());assert(r.error==Error::MissingContinuation&&!gate());
            assert(original_hooks()&&original_constants());});
    }
    test("failed unhook is terminal retained",[]{assert(prepare_for_child());null_original=2;fail_unhook=true;
        assert(execute_config(all_config()).outcome==Outcome::TerminalFailure&&!gate());});
    test("unhook success cannot hide foreign tail outside 16-byte journal",[]{assert(prepare_for_child());
        foreign_hook_tail=true;null_original=1;
        auto r=execute_config(all_config());assert(r.outcome==Outcome::TerminalFailure&&r.error==Error::RollbackFailed&&!gate());
        assert(*reinterpret_cast<uint32_t*>(base+plan::kHooks[0].va+16)==0xdeadbeef);});
    test("backend exceeding 16-byte contract is rejected immediately",[]{assert(prepare_for_child());
        foreign_hook_tail=true;auto r=execute_config(all_config());
        assert(r.outcome==Outcome::TerminalFailure&&r.error==Error::RollbackFailed&&hook_calls==1&&!gate());});
    for(unsigned position=1;position<=30;++position) {
        auto label=std::string("write-after-mutation failure ")+std::to_string(position);
        test(label.c_str(),[position]{assert(prepare_for_child());fail_write=position;
            auto r=execute_config(all_config());if(write_calls>=position) {
                assert(r.outcome==Outcome::Declined&&!gate()&&original_constants()&&original_hooks());
            } else assert(r.outcome==Outcome::Applied);});
    }
    test("foreign readback is preserved",[]{assert(prepare_for_child());foreign_writeback=true;
        auto r=execute_config(all_config());assert(r.outcome==Outcome::TerminalFailure&&!gate());
        unsigned count=0;for(const auto&w:plan::kWords){uint32_t v;std::memcpy(&v,reinterpret_cast<void*>(base+w.va),4);if(v==0xdeadbeef)++count;}
        assert(count>0);});
    test("revoke during backend prevents all publication",[]{assert(prepare_for_child());revoke_at_hook=8;
        auto r=execute_config(all_config());assert(r.error==Error::Revoked&&!gate()&&original_hooks()&&original_constants());});
    test("watchdog refuses busy writer and later restores",[]{assert(prepare_for_child());auto c=all_config();
        pause_hook=true;Result result;
        std::thread worker([&]{Request r{base,fake_hook,fake_unhook,&c,auth,nullptr};result=execute(r);});
        while(!entered_hook.load())std::this_thread::yield();
        request_revoke();assert(service_revoke().error==Error::Busy&&!gate());
        assert(execute_config(c).error==Error::Busy);resume_hook=true;worker.join();
        assert(result.error==Error::Revoked&&!gate()&&original_constants()&&original_hooks());});
    test("late newly enabled group refused",[]{assert(prepare_for_child());home_layout::Config c;
        execute_config(c);c.knobs[0].enabled=true;c.knobs[0].delta_dp=20;
        auto r=maintenance(&c,true,auth,nullptr);assert(r.applied==0&&(r.declined&HotseatMargin)&&r.error==Error::LateConfiguration&&!hook_calls);});
    test("expired maintenance restores code",[]{assert(prepare_for_child());auto c=all_config();execute_config(c);
        authorized=false;auto r=maintenance(&c,true,auth,nullptr);assert(r.error==Error::Revoked&&!gate()&&original_constants());});
    test("lost detour never installed after first-use",[]{assert(prepare_for_child());auto c=all_config();execute_config(c);
        fake_unhook(reinterpret_cast<void*>(base+plan::kHooks[0].va));auto n=hook_calls;
        auto r=maintenance(&c,true,auth,nullptr);assert(r.error==Error::ForeignCode&&hook_calls==n&&!gate()&&original_constants());});
    test("failed restore retains journal for watchdog retry",[]{assert(prepare_for_child());execute_config(all_config());
        request_revoke();fail_all_writes=true;assert(service_revoke().outcome==Outcome::TerminalFailure&&!gate());
        fail_all_writes=false;service_revoke();assert(original_constants());});
    test("RX restoration failure survives original-byte rollback",[]{assert(prepare_for_child());fail_rx=true;
        auto r=execute_config(all_config());assert(r.outcome==Outcome::TerminalFailure&&!gate()&&original_constants());
        bool pending=false;for(auto p:pending_pages)pending|=p!=0;assert(pending);
        fail_rx=false;service_revoke();for(auto p:pending_pages)assert(!p);assert(original_constants());});
    test("single RX failure rolls back and clears page journal",[]{assert(prepare_for_child());fail_protect=2;
        auto r=execute_config(all_config());assert(r.outcome==Outcome::Declined&&!gate()&&original_constants());
        for(auto p:pending_pages)assert(!p);});
    test("revoke after last publish cannot reopen epoch",[]{assert(prepare_for_child());revoke_at_publish=true;
        auto r=execute_config(all_config());assert(r.error==Error::Revoked&&!gate()&&original_constants()&&original_hooks());});
    test("changed authorization after publish rolls back before return",[]{assert(prepare_for_child());change_auth_at_publish=true;
        auto r=execute_config(all_config());assert(r.error==Error::Revoked&&!gate()&&original_constants()&&original_hooks());});
    test("changed authorization in maintenance publication stays closed",[]{assert(prepare_for_child());home_layout::Config c;
        c.knobs[0]={true,12};execute_config(c);change_auth_at_publish=true;c.knobs[0].delta_dp=20;
        auto r=maintenance(&c,true,auth,nullptr);assert(r.error==Error::Revoked&&!gate()&&original_constants());});
    test("dynamic gate-only setting update and recovery",[]{assert(prepare_for_child());home_layout::Config c;
        c.knobs[0]={true,12};auto r=execute_config(c);assert(r.applied==HotseatMargin&&gate());
        c.knobs[0].delta_dp=27;r=maintenance(&c,true,auth,nullptr);assert(r.applied==HotseatMargin&&gate());
        request_revoke();service_revoke();assert(!gate());r=maintenance(&c,true,auth,nullptr);
        assert(r.applied==HotseatMargin&&gate());});
    for(unsigned family: {2u,3u})test((std::string("pad/fold model composite refusal ")+std::to_string(family)).c_str(),[family]{
        assert(prepare_for_child());home_layout::Config c;
        if(family==2){c.tweaks.pad_enabled=true;c.tweaks.pad_major=10;c.tweaks.pad_minor=7;}
        else{c.tweaks.fold_enabled=true;c.tweaks.fold_major=10;c.tweaks.fold_minor=7;}
        const auto r=execute_config(c);assert(r.applied==0&&r.declined==(1ull<<family)&&!hook_calls&&!write_calls);
    });
    for(const auto &v:plan::kVariants) {
        const auto label=std::string("production planner variant ")+std::to_string(v.family)+":"+std::to_string(v.key);
        test(label.c_str(),[v]{assert(prepare_for_child());home_layout::Config c;auto&t=c.tweaks;
            switch(v.family){
                case 0:c.grid_enabled=true;c.cell_x=v.key>>8;c.cell_y=v.key&255;break;
                case 1:t.folder_enabled=true;t.folder_cols=v.key;break;
                case 2:t.pad_enabled=true;t.pad_major=v.key>>8;t.pad_minor=v.key&255;break;
                case 3:t.fold_enabled=true;t.fold_major=v.key>>8;t.fold_minor=v.key&255;break;
                case 4:t.icon_scale_enabled=true;t.icon_scale_code=v.key;break;
                case 5:t.recents_no_clear=true;break;case 6:t.recents_hide_clear=true;break;
            }
            auto r=execute_config(c);assert(r.outcome==Outcome::Applied);
            if(v.family==0)assert(r.applied==0&&r.declined==(PhoneGridDart|PhoneGridRust)&&!write_calls);
            else if(v.family==2||v.family==3)assert(r.applied==0&&r.declined==(1ull<<v.family)&&!write_calls);
            else {
                assert(r.applied==(1ull<<v.family)&&r.declined==0);
                for(const auto &w:plan::kWords){uint32_t expected=w.original,actual=0;
                    for(unsigned i=0;i<v.count;++i)if(plan::kWords[v.begin+i].va==w.va)expected=plan::kWords[v.begin+i].replacement;
                    std::memcpy(&actual,reinterpret_cast<void*>(base+w.va),4);assert(actual==expected);
                }
            }
            request_revoke();service_revoke();assert(original_constants());
        });
    }
    for(const auto &g:plan::kGuards) {
        const auto label=std::string("guard mutation ")+std::to_string(g.va);
        test(label.c_str(),[g]{assert(prepare_for_child());mapped[g.va]^=std::byte{1};
            auto r=execute_config(all_config());assert(r.error==Error::GuardMismatch&&!hook_calls&&!write_calls);});
    }
    test("identity/build note mutation",[]{assert(prepare_for_child());mapped[0x1d8]^=std::byte{1};
        assert(execute_config(all_config()).error==Error::GuardMismatch&&!hook_calls);});
    for(unsigned position: {1u,3u,20u,60u,100u}) {
        auto label=std::string("guard memory read fault ")+std::to_string(position);
        test(label.c_str(),[position]{assert(prepare_for_child());fail_read=position;
            auto r=execute_config(all_config());assert(r.error==Error::GuardMismatch&&!hook_calls&&!write_calls);});
    }
    std::printf("{\"tests\":%u,\"passed\":%u,\"failed\":%u}\n",tests,tests-failed,failed);
    return failed?1:0;
}
