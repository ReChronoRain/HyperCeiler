from pathlib import Path
import sys,subprocess,re
D=Path(__file__).resolve().parent;W=D.parents[1];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
s=(R/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text(encoding='utf8')
p=R/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/navigation/BackGestureWidthState.java'
if not p.exists():
 print('SYSTEMUI_WIDTH=original97 < requested136/273; width consumer absent');print('SETUP_LEASE=absent; initializer/worker bank serialization missing');sys.exit(1)
worker=s[s.index('void *worker('):s.index('bool adopt_layout_state') if 'bool adopt_layout_state' in s else s.index('void prime_home_layout_knobs')]
assert worker.index('while (g_loader_priming.test_and_set')<worker.index('for (Slot &slot : g_slots)')
assert worker.index('g_back_setup_complete.store(true')<worker.index('setup_lease.release()')<worker.index('for (;;)')
prime=s[s.index('void prime_back_regions('):s.index('void start_home_layout_hooks(')]
assert prime.index('g_loader_priming.test_and_set')<prime.index('g_slots[kBackWindowSlot].backend_owned')
assert 'g_back_setup_complete.store(false' in s
lease=re.search(r'struct SetupLease \{(.*?)\} setup_lease;',worker,re.S).group(0).replace('} setup_lease;','};')
cpp='#include <atomic>\n#include <thread>\n#include <cstdio>\nstd::atomic_flag g_loader_priming=ATOMIC_FLAG_INIT;\n'+lease+r"""
std::atomic<int> active{0},errors{0};
void run(){for(int i=0;i<10000;i++){while(g_loader_priming.test_and_set(std::memory_order_acquire))std::this_thread::yield();SetupLease lease;if(active.fetch_add(1)!=0)errors++;std::this_thread::yield();if(active.fetch_sub(1)!=1)errors++;if(i%2)lease.release();}}
int main(){std::thread a(run),b(run),c(run);a.join();b.join();c.join();printf("SETUP_LEASE=30000 critical sections; overlaps=%d; RAII/explicit release\n",errors.load());return errors.load()!=0;}
"""
(D/'lease.cpp').write_text(cpp);zig=W/'tests/dynamic-address-audit/runtime/zig-windows-x86_64-0.13.0/zig.exe'
subprocess.run([str(zig),'c++','-std=c++20',str(D/'lease.cpp'),'-o',str(D/'lease.exe')],check=True);subprocess.run([str(D/'lease.exe')],check=True)
jdk=Path('D:/build-tools/jdk-25.0.4.1+1/bin');out=D/'classes';out.mkdir(exist_ok=True)
subprocess.run([str(jdk/'javac.exe'),'-encoding','UTF-8','-d',str(out),str(p),str(D/'WidthTest.java')],check=True)
subprocess.run([str(jdk/'java.exe'),'-ea','-cp',str(out),'WidthTest'],check=True)
print('SOURCE_BINDING=validated typed fields; constructor + original updateCurrentUserResources; remote change listener; cleanup restoration')
