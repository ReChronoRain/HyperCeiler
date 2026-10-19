/* Reuse transport gates, but link the real entry start/record calls to real control. */
#define main control_regression_main
#include "control_test.cpp"
#undef main
void joint_setup(ZygiskNextAPI *,bool);
bool joint_load();bool joint_lookup();bool joint_prepare(bool);void joint_mutate_runtime();bool joint_concurrent_binding(bool);
void joint_snapshot_bind();bool joint_snapshot_lookup(int index=0,int mode=0);
void joint_snapshot_corrupt(int);void joint_reenter_during_guard(void (*)());
static void prepare_joint(){using namespace fixture;joint_setup(&api,true);CHECK(joint_lookup()&&joint_prepare(true));join_workers();}
int main(){using namespace fixture;
 run("real loader and dlsym entry paths never start control",[]{joint_setup(&api,true);CHECK(joint_load()&&joint_lookup());CHECK(workers==0&&queries==0&&inits==0&&!hyos_control::active()&&g_pending_libraries==3);});
 run("non Home specialization leaves actual control untouched",[]{joint_setup(&api,false);CHECK(joint_load()&&!joint_lookup());CHECK(workers==0&&queries==0&&inits==0&&g_pending_libraries==0);});
 run("wrong entry caller returns original target without control",[]{joint_setup(&api,true);CHECK(joint_lookup()&&joint_prepare(false));CHECK(workers==0&&queries==0&&inits==0);});
 run("changed runtime guard returns original target without control",[]{joint_setup(&api,true);CHECK(joint_lookup());joint_mutate_runtime();CHECK(joint_prepare(true));CHECK(workers==0&&queries==0&&inits==0);});
 run("authenticated app entry starts actual worker and drains actual loader events",[]{joint_setup(&api,true);CHECK(joint_load()&&joint_lookup()&&joint_prepare(true));join_workers();CHECK(workers==2&&queries==1&&inits==1&&events.size()==2&&hyos_control::allowed());});
 run("blocked real control Binder still returns original entry target within bounded wait",[]{joint_setup(&api,true);CHECK(joint_lookup());query_gate.arm();auto start=std::chrono::steady_clock::now();bool original=joint_prepare(true);auto elapsed=std::chrono::steady_clock::now()-start;query_gate.wait();bool defaults=inits==0&&!hyos_control::allowed();query_gate.release();join_workers();CHECK(original&&defaults&&elapsed>=std::chrono::milliseconds(120)&&elapsed<std::chrono::seconds(1));std::printf("  first entry wait %lld ms\n",static_cast<long long>(std::chrono::duration_cast<std::chrono::milliseconds>(elapsed).count()));});
 run("reentered application entry never creates replacement for hung Binder",[]{joint_setup(&api,true);CHECK(joint_lookup());query_gate.arm();CHECK(joint_prepare(true));query_gate.wait();bool original=joint_prepare(true);int count=workers;query_gate.release();join_workers();CHECK(original&&count==2&&queries==1&&events.size()==1);});
 run("entry with unavailable snapshot cannot initialize features",[]{joint_setup(&api,true);available=0;CHECK(joint_lookup()&&joint_prepare(true));join_workers();CHECK(workers==2&&inits==0&&!hyos_control::allowed()&&g_pending_libraries==1);joint_snapshot_bind();CHECK(joint_snapshot_lookup()&&g_startup_phase==StartupPhase::Missed);available=1;flags=33;next_poll();CHECK(inits==1&&report_retries==1&&events.size()==2&&events[0]=="libapp_launcher.so"&&events[1]=="libapp.so");});
 run("entry with explicit disabled snapshot retains original entry and defaults",[]{joint_setup(&api,true);flags=0;CHECK(joint_lookup()&&joint_prepare(true));join_workers();CHECK(inits==0&&last_options()==0&&!hyos_control::allowed());});
 run("64 competing target bindings preserve one immutable actual pair",[]{joint_setup(&api,true);CHECK(joint_concurrent_binding(false));CHECK(workers==0&&queries==0);});
 run("64 competing runtime and target bindings never mix pairs",[]{joint_setup(&api,true);CHECK(joint_concurrent_binding(true));CHECK(workers==0&&queries==0);});
 run("exact snapshot observer enters real HCS controller once",[]{
  flags=63;prepare_joint();CHECK(g_startup_prepared&&g_startup_phase==StartupPhase::Pending&&executions==0&&post_startups==0&&report_retries==0);
  home_layout::Config config;CHECK(hyos_control::layout_config(config)&&config.grid_enabled&&config.cell_x==7&&config.cell_y==10);
  joint_snapshot_bind();CHECK(joint_snapshot_lookup());CHECK(executions==1&&execute_authorized&&g_startup_phase==StartupPhase::Applied);
  CHECK(joint_snapshot_lookup(1)&&joint_snapshot_lookup());next_poll();
  CHECK(executions==1&&maintenances==1&&post_startups==1&&report_retries==1);
 });
 for(int kind=0;kind<3;++kind)run(("real observer guard failure closes real phase "+std::to_string(kind)).c_str(),[kind]{
  flags=33;prepare_joint();joint_snapshot_bind();joint_snapshot_corrupt(kind);CHECK(joint_snapshot_lookup());
  uint32_t expected=kind==0?6:kind==1?3:2;
  CHECK(g_startup_phase==StartupPhase::Missed&&g_startup_reason==expected&&executions==0);
  next_poll();CHECK(joint_snapshot_lookup()&&executions==0&&maintenances==0);
 });
 run("first other successful snapshot cannot be recovered by later VMData",[]{
  prepare_joint();joint_snapshot_bind();CHECK(joint_snapshot_lookup(2));
  CHECK(g_startup_phase==StartupPhase::Missed&&g_startup_reason==5&&executions==0);
  CHECK(joint_snapshot_lookup()&&executions==0);
 });
 run("wrong namespace handle conservatively closes window",[]{
  prepare_joint();joint_snapshot_bind();CHECK(joint_snapshot_lookup(0,2));
  CHECK(g_startup_phase==StartupPhase::Missed&&g_startup_reason==4&&executions==0);
 });
 run("wrong snapshot caller propagates exact guard refusal to controller",[]{
  prepare_joint();joint_snapshot_bind();CHECK(joint_snapshot_lookup(0,1));
  CHECK(g_startup_phase==StartupPhase::Missed&&g_startup_reason==3&&executions==0);
 });
 run("explicit global off snapshot cannot become late startup authorization",[]{
  flags=0;prepare_joint();joint_snapshot_bind();CHECK(joint_snapshot_lookup());
  CHECK(g_startup_phase==StartupPhase::Missed&&executions==0);flags=63;next_poll();
  CHECK(joint_snapshot_lookup()&&hyos_control::allowed()&&executions==0);
 });
 run("snapshot during blocked initialization never waits for prepared",[]{
  joint_setup(&api,true);CHECK(joint_lookup());init_gate.arm();CHECK(joint_prepare(true));init_gate.wait();joint_snapshot_bind();
  auto start=std::chrono::steady_clock::now();bool same=joint_snapshot_lookup();auto elapsed=std::chrono::steady_clock::now()-start;
  bool missed=g_startup_phase==StartupPhase::Missed&&executions==0&&g_startup_error==home_startup::Error::NotPrepared;
  init_gate.release();join_workers();CHECK(same&&missed&&elapsed<std::chrono::seconds(1));
  next_poll();CHECK(g_startup_prepared&&joint_snapshot_lookup()&&executions==0);
 });
 run("late preparation retry remains eligible only before first snapshot",[]{
  fail_prepare_startup=true;prepare_joint();CHECK(prepares==1&&!g_startup_prepared);fail_prepare_startup=false;next_poll();
  CHECK(g_startup_prepared&&prepares==2);joint_snapshot_bind();CHECK(joint_snapshot_lookup()&&executions==1&&g_startup_phase==StartupPhase::Applied);
 });
 run("unavailable HCS reply then first snapshot cannot late install",[]{
  bytes=16;prepare_joint();joint_snapshot_bind();CHECK(joint_snapshot_lookup());
  CHECK(g_startup_phase==StartupPhase::Missed&&executions==0&&!hyos_control::allowed());
  bytes=188;next_poll();CHECK(hyos_control::allowed()&&g_startup_prepared&&joint_snapshot_lookup()&&executions==0);
 });
 run("real snapshot cannot extend expired absolute HCS deadline",[]{
  server_age=4'000'000'000LL;prepare_joint();joint_snapshot_bind();now+=1'000'000'000LL;
  CHECK(joint_snapshot_lookup()&&g_startup_phase==StartupPhase::Missed&&executions==0);
 });
 run("guard-time nested observer miss cancels actual controller before execute",[]{
  prepare_joint();joint_snapshot_bind();joint_reenter_during_guard([]{CHECK(joint_snapshot_lookup());});
  CHECK(joint_snapshot_lookup());CHECK(g_startup_phase==StartupPhase::Missed&&g_startup_reason==7&&executions==0);
 });
 run("executing observer reentry keeps Dock Report gated through cancellation",[]{
  flags=63;prepare_joint();execute_gate.arm();std::atomic_bool same{false};
  std::thread first([&]{joint_snapshot_bind();same=joint_snapshot_lookup();});execute_gate.wait();
  bool second=joint_snapshot_lookup();bool cancelling=g_startup_phase==StartupPhase::Cancelling&&!startup_finished();
  next_poll();bool gated=post_startups==0&&report_retries==0&&maintenances==0;
  execute_gate.release();first.join();CHECK(second&&same&&cancelling&&gated);
  CHECK(executions==1&&!execute_authorized&&g_startup_cancelled&&g_startup_phase==StartupPhase::TerminalFailure);
  next_poll();CHECK(joint_snapshot_lookup()&&executions==1&&maintenances==0);
 });
 run("publication changes during actual observer execution revoke captured HCS identity",[]{
  prepare_joint();execute_gate.arm();std::atomic_bool same{false};
  std::thread first([&]{joint_snapshot_bind();same=joint_snapshot_lookup();});execute_gate.wait();next_poll();execute_gate.release();first.join();
  CHECK(same&&!execute_authorized&&executions==1&&g_startup_phase==StartupPhase::Declined);
 });
 std::printf("RESULT passes=%d failures=%d; actual entry + actual HCS control linked in separate TUs; startup patch backend is a fixture; ARM tail call remains separate executable test\n",pass,fail);return fail?1:0;
}
