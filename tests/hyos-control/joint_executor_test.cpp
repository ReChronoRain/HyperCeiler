/* Actual entry + HCS controller + journal executor. No phone or Dart execution. */
#define HC_REAL_STARTUP 1
#define main unused_control_regression_main
#include "control_test.cpp"
#undef main
#include "../home-startup/startup_fixture.h"
#include <sys/wait.h>
#include <unistd.h>
void joint_setup(ZygiskNextAPI *,bool);
bool joint_lookup();bool joint_prepare(bool);
void joint_snapshot_bind();bool joint_snapshot_lookup(int index=0,int mode=0);
bool joint_snapshot_use_dart(uintptr_t);
void joint_snapshot_corrupt(int);
void joint_reenter_during_guard(void (*)());

static int combined_hook(void *target,void *replacement,void **original) {
    return startup_fixture::contains(reinterpret_cast<uintptr_t>(target))
        ? startup_fixture::hook(target,replacement,original)
        : fixture::hook(target,replacement,original);
}
static int combined_unhook(void *target) {
    return startup_fixture::contains(reinterpret_cast<uintptr_t>(target))
        ? startup_fixture::unhook(target) : fixture::unhook(target);
}
static void all_layout(hyos_snapshot::Words &words) {
    if(words[1]!=1 || (words[2]&1)==0)return;
    words[9]=0; // Both halves of phone grid remain explicitly unsupported.
    for(size_t i=0;i<8;++i){words[13+i*2]=1;words[14+i*2]=12;}
    words[26]=2;words[28]=1;
    const int32_t tweaks[]{5,1,9,6,1,9,6,1,102,1,0,1,1,140,0,100,1};
    for(size_t i=0;i<17;++i)words[30+i]=tweaks[i];
}
static void boot_combined() {
    using namespace fixture;
    flags=63;frame_edit=all_layout;api.inlineHook=combined_hook;api.inlineUnhook=combined_unhook;
    joint_setup(&api,true);CHECK(joint_snapshot_use_dart(startup_fixture::base()));
    CHECK(joint_lookup()&&joint_prepare(true));join_workers();
    CHECK(g_startup_prepared&&hyos_control::allowed());joint_snapshot_bind();
}
static void cancelled_at_hook(unsigned count) {
    if(count==1) { CHECK(joint_snapshot_lookup()); CHECK(g_startup_phase==StartupPhase::Cancelling); }
}
static void expired_at_hook(unsigned count) {
    if(count==1) { fixture::now+=5'000'000'000LL;expire_lease();service_startup_revoke(); }
}
static void cancel_at_publish() { CHECK(joint_snapshot_lookup()); }
static void change_at_publish() { fixture::next_poll(); }
static void verify_cancelled(bool terminal) {
    CHECK(!startup_fixture::gate_enabled());
    CHECK(startup_fixture::all_words_original());
    CHECK(g_startup_phase==(terminal?StartupPhase::TerminalFailure:StartupPhase::Declined));
    auto count=startup_fixture::hook_count();fixture::next_poll();
    CHECK(joint_snapshot_lookup()&&startup_fixture::hook_count()==count);
}
int main(int argc,char **argv) {
    CHECK(argc==2&&startup_fixture::load(argv[1]));
    unsigned total=0,failed=0;
    auto scenario=[&](const char *name,const std::function<void()> &fn) {
        ++total;std::fflush(nullptr);const pid_t child=fork();CHECK(child>=0);
        if(child==0) { fixture::run(name,fn);std::fflush(nullptr);_exit(fixture::fail?1:0); }
        int status=0;CHECK(waitpid(child,&status,0)==child);
        if(!WIFEXITED(status)||WEXITSTATUS(status)!=0)++failed;
    };
    scenario("three real layers install one complete eligible plan",[]{
        boot_combined();CHECK(joint_snapshot_lookup());
        CHECK(g_startup_phase==StartupPhase::Applied&&startup_fixture::gate_enabled());
        CHECK(startup_fixture::hook_count()==15&&startup_fixture::published_mask()!=0);
        CHECK(!startup_fixture::all_words_original());auto hooks=startup_fixture::hook_count();
        CHECK(joint_snapshot_lookup(1)&&joint_snapshot_lookup());fixture::next_poll();
        CHECK(startup_fixture::hook_count()==hooks&&fixture::post_startups==1&&fixture::report_retries==1);
    });
    scenario("real entry reentry at first hook cancels and restores actual journal",[]{
        boot_combined();startup_fixture::set_hook_event(cancelled_at_hook);
        CHECK(joint_snapshot_lookup());CHECK(startup_fixture::hook_count()==1);verify_cancelled(true);
    });
    scenario("real HCS expiration at first hook closes gate and rolls back",[]{
        boot_combined();startup_fixture::set_hook_event(expired_at_hook);
        CHECK(joint_snapshot_lookup());CHECK(!hyos_control::allowed());verify_cancelled(false);
    });
    scenario("real entry reentry immediately before commit restores hooks and constants",[]{
        boot_combined();startup_fixture::set_publish_event(cancel_at_publish);
        CHECK(joint_snapshot_lookup());CHECK(startup_fixture::hook_count()==15);verify_cancelled(true);
    });
    scenario("new actual HCS publication before commit cannot authorize old revision",[]{
        boot_combined();startup_fixture::set_publish_event(change_at_publish);
        CHECK(joint_snapshot_lookup());CHECK(startup_fixture::hook_count()==15);verify_cancelled(false);
    });
    scenario("real guard rejection never reaches actual executor",[]{
        boot_combined();joint_snapshot_corrupt(1);CHECK(joint_snapshot_lookup());
        CHECK(g_startup_phase==StartupPhase::Missed&&startup_fixture::hook_count()==0);
        CHECK(startup_fixture::all_words_original()&&!startup_fixture::gate_enabled());
    });
    scenario("guard-time reentry cancels before actual executor starts",[]{
        boot_combined();joint_reenter_during_guard([]{CHECK(joint_snapshot_lookup());});
        CHECK(joint_snapshot_lookup());CHECK(g_startup_phase==StartupPhase::Missed);
        CHECK(startup_fixture::hook_count()==0&&startup_fixture::all_words_original());
    });
    scenario("unavailable HCS then first window remains permanently missed",[]{
        using namespace fixture;available=0;flags=63;frame_edit=all_layout;
        api.inlineHook=combined_hook;api.inlineUnhook=combined_unhook;
        joint_setup(&api,true);CHECK(joint_snapshot_use_dart(startup_fixture::base()));
        CHECK(joint_lookup()&&joint_prepare(true));join_workers();joint_snapshot_bind();
        CHECK(joint_snapshot_lookup()&&g_startup_phase==StartupPhase::Missed);
        available=1;next_poll();CHECK(joint_snapshot_lookup());
        CHECK(startup_fixture::hook_count()==0&&startup_fixture::all_words_original());
    });
    std::printf("RESULT passes=%u failures=%u; actual entry/controller/executor; ROM bytes mapped as host data; fixture loader/Binder/hook backend/consumer ABI, no Android or Dart execution\n",total-failed,failed);
    return failed?1:0;
}
