"""Run the production synchronous transport predicate with only the Binder constant stubbed."""
from pathlib import Path
import sys, subprocess, re

W = Path(__file__).resolve().parents[2]
R = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else W
p = R / 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/gesture/HomeQuickSwitchOS4.java'
s = p.read_text(encoding='utf8') if p.exists() else ''
m = re.search(r'static boolean synchronous\(int flags\)\s*\{[^}]*\}', s)
guard = re.search(r'if \(([^\n]*?) \|\| data\.dataAvail\(\) != 0', s)
if not guard:
    print('TRANSPORT_GUARD=missing endpoint; failed=1')
    sys.exit(1)
assert guard.group(1) in ('flags != 0', '!synchronous(flags)'), guard.group(1)
assert 'now - time > 750' in s and '!isLauncher(context,uid,pid)' in re.sub(r'\s+', '', s)
out = W / 'handover/gestures-os4-20261014' / ('transport-' + R.name)
(out / 'android/os').mkdir(parents=True, exist_ok=True)
(out / 'android/os/IBinder.java').write_text('package android.os;public interface IBinder {int FLAG_ONEWAY=1;}')
java = 'public class Transport {\n' + (m.group() if m else '')
java += '\nstatic boolean rejected(int flags) { return ' + guard.group(1) + '; }\n' + r'''
 public static void main(String[]a) {int checks=0,failed=0;
 for(int f:new int[]{0,2,16,18,32,48}) {checks++;if(rejected(f))failed++;}
 for(int f:new int[]{1,3,17,19,33,49}) {checks++;if(!rejected(f))failed++;}
 System.out.println("BINDER_TRANSPORT_CHECKS="+checks+" FAILED="+failed+"; production predicate, framework constant stubbed");
 if(failed>0)System.exit(1);}
}'''
(out / 'Transport.java').write_text(java)
x = subprocess.run(['javac', '-d', str(out), str(out / 'Transport.java'), str(out / 'android/os/IBinder.java')], capture_output=True, text=True)
print(x.stdout + x.stderr, end='')
if x.returncode:
    sys.exit(x.returncode)
x = subprocess.run(['java', '-cp', str(out), 'Transport'], capture_output=True, text=True)
print(x.stdout + x.stderr, end='')
sys.exit(x.returncode)
