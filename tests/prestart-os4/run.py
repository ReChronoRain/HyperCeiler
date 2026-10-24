#!/usr/bin/env python3
"""Offline production discovery, policy and OS4 preference admission checks."""
import argparse,hashlib,json,re,struct,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).resolve().parent
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--image7751',type=Path);p.add_argument('--image8583',type=Path);args=p.parse_args()
for version in ('7751','8583'):
 data=json.loads((HERE/f'launcher-{version}.json').read_text());source=getattr(args,'image'+version)
 if source:
  raw=source.read_bytes();assert hashlib.sha256(raw).hexdigest()==data['source_sha256']
  ph=struct.unpack_from('<Q',raw,32)[0];loads=[struct.unpack_from('<IIQQQQQQ',raw,ph+i*56)for i in range(struct.unpack_from('<H',raw,56)[0])]
  for f in data['functions']:
   va=f['va'];size=len(f['words'])*4;s=next(s for s in loads if s[0]==1 and s[1]&1 and s[3]<=va and va+size<=s[3]+s[5])
   assert list(struct.unpack_from('<'+'I'*len(f['words']),raw,s[2]+va-s[3]))==f['words']
 with tempfile.TemporaryDirectory(prefix='hc-prestart-')as work:
  out=Path(work);header='static Functions prestart_fixture(){return {\n'
  for f in data['functions']:header+='{'+str(f['va'])+',{'+','.join(map(str,f['words']))+'}},\n'
  header+='};}\nstatic constexpr uint32_t expected_entry='+str(data['entry'])+',expected_initializer='+str(data['initializer'])+';\n'
  (out/'prestart_fixture.h').write_text(header)
  subprocess.run(['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-I'+str(ROOT/'app/src/main/cpp'),'-I'+work,str(HERE/'native_test.cpp'),'-o',str(out/'native')],check=True)
  subprocess.run([str(out/'native')]+([str(source)]if source else[]),check=True)
# Execute the actual request getter in both compiled backends. An old JNI true
# must never override a disabled/expired Hyos lease. Actual lease scheduling and
# stalled Binder races are covered by tests/hyos-control/run_native.py.
native=(ROOT/'app/src/main/cpp/targets/home/hyperceiler_home.cpp').read_text()
getter=native[native.index('bool hyperceiler_home_prestart_disabled() {'):native.index('\nvoid hyperceiler_home_runtime_options(')]
with tempfile.TemporaryDirectory(prefix='hc-prestart-auth-')as work:
 out=Path(work)
 source='#include <atomic>\n#include <cassert>\n#include <iostream>\nstd::atomic_bool g_disable_prestart{false};\nnamespace hyos_control {constexpr unsigned kDisablePrestart=4;bool lease=false;bool feature_enabled(unsigned flag){assert(flag==kDisablePrestart);return lease;}}\n'+getter+'''
int main(){for(bool jni:{false,true})for(bool lease:{false,true}){g_disable_prestart=jni;hyos_control::lease=lease;
#ifdef HYPERCEILER_HYOS_BACKEND
assert(hyperceiler_home_prestart_disabled()==lease);
#else
assert(hyperceiler_home_prestart_disabled()==jni);
#endif
}std::cout<<"prestart request getter PASS\\n";}
'''
 (out/'getter.cpp').write_text(source)
 for mode in ([],['-DHYPERCEILER_HYOS_BACKEND']):
  subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror']+mode+[str(out/'getter.cpp'),'-o',str(out/'getter')],check=True)
  subprocess.run([str(out/'getter')],check=True)
gate=ROOT/'library/core/src/main/java/com/sevtinge/hyperceiler/hooker/home/HomeOS4AdaptationGate.java'
resources=sorted(set(re.findall(r'R\.xml\.(\w+)',gate.read_text())))
stubs={
 'androidx/preference/Preference.java':'package androidx.preference;public class Preference{public String key;public boolean enabled=true;public String getKey(){return key;}public void setEnabled(boolean b){enabled=b;}}',
 'androidx/preference/PreferenceGroup.java':'package androidx.preference;public class PreferenceGroup extends Preference{public java.util.List<Preference> children=new java.util.ArrayList<>();public int getPreferenceCount(){return children.size();}public Preference getPreference(int i){return children.get(i);}}',
 'com/sevtinge/hyperceiler/libhook/utils/api/DeviceHelper.java':'package com.sevtinge.hyperceiler.libhook.utils.api;public class DeviceHelper{public static class System{public static float version;public static boolean isHyperOSVersion(float f){return version==f;}}}',
 'com/sevtinge/hyperceiler/core/R.java':'package com.sevtinge.hyperceiler.core;public class R{public static class xml{'+''.join('public static final int '+n+'='+str(i)+';'for i,n in enumerate(resources,1))+'}}',
 'GateTest.java':'''import androidx.preference.*;import com.sevtinge.hyperceiler.hooker.home.HomeOS4AdaptationGate;import com.sevtinge.hyperceiler.core.R;import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper;
public class GateTest{public static void main(String[] args){int checks=0;for(float version:new float[]{3,4})for(boolean prior:new boolean[]{false,true}){DeviceHelper.System.version=version;PreferenceGroup g=new PreferenceGroup();Preference p=new Preference();p.key="prefs_key_home_other_disable_prestart";p.enabled=prior;g.children.add(p);Preference other=new Preference();other.key="prefs_key_home_other_allow_share_apk";g.children.add(other);HomeOS4AdaptationGate.apply(g,R.xml.home_other_new);if(p.enabled!=prior||other.enabled!=(version!=4))throw new AssertionError("OS4 admission/dependency changed");checks+=2;}System.out.println("prestart UI PASS: "+checks+" OS3/OS4/dependency/unsupported-control checks");}}'''
}
with tempfile.TemporaryDirectory(prefix='hc-prestart-ui-')as work:
 files=[]
 for name,text in stubs.items():
  f=Path(work)/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(text);files.append(str(f))
 subprocess.run(['javac','-encoding','UTF-8','-d',work,str(gate)]+files,check=True)
 subprocess.run(['java','-ea','-cp',work,'GateTest'],check=True)
print('prestart offline checks complete; no device access')
