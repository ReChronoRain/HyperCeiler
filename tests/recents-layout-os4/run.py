#!/usr/bin/env python3
"""Run shipping recents executable discovery, renderer, wire/cache and Java tests."""
import argparse,hashlib,json,re,struct,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).resolve().parent
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--image7751',type=Path);parser.add_argument('--image8583',type=Path);args=parser.parse_args()
for version in ('7751','8583'):
 data=json.loads((HERE/f'launcher-{version}.json').read_text());source=getattr(args,'image'+version)
 if source:
  raw=source.read_bytes();assert hashlib.sha256(raw).hexdigest()==data['source_sha256']
  ph=struct.unpack_from('<Q',raw,32)[0];count=struct.unpack_from('<H',raw,56)[0]
  loads=[struct.unpack_from('<IIQQQQQQ',raw,ph+i*56)for i in range(count)]
  for f in data['functions']:
   va=f['va'];size=len(f['words'])*4;p=next(p for p in loads if p[0]==1 and p[3]<=va and va+size<=p[3]+p[5])
   assert list(struct.unpack_from('<'+'I'*len(f['words']),raw,p[2]+va-p[3]))==f['words']
 with tempfile.TemporaryDirectory(prefix='hc-recents-')as work:
  out=Path(work);header='static Functions recent_fixture(){return {\n'
  for f in data['functions']:header+='{'+str(f['va'])+',{'+','.join(map(str,f['words']))+'}},\n'
  header+='};}\nstatic std::vector<uint32_t> expected_sites={'+','.join(map(str,data['sites']))+'};\n'
  (out/'recent_fixture.h').write_text(header)
  subprocess.run(['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-I'+str(ROOT/'app/src/main/cpp'),'-I'+str(out),str(HERE/'native_test.cpp'),'-o',str(out/'native')],check=True)
  subprocess.run([str(out/'native')]+([str(source)]if source else[]),check=True)
with tempfile.TemporaryDirectory(prefix='hc-recents-cache-')as work:
 out=Path(work);config=(ROOT/'app/src/main/cpp/targets/home/home_layout_config.cpp').read_text()
 version=re.search(r'constexpr uint32_t kCacheVersion = (\d+);',config)[1]
 functions=config[config.index('void serialize_config('):config.index('bool write_config_cache(')]
 (out/'cache.cpp').write_text('#include "targets/home/home_layout_config.h"\n#include <iostream>\n#include <cassert>\n#include <cstring>\nusing namespace home_layout;\nconstexpr char kCacheMagic[]={\'H\',\'C\',\'L\',\'C\'};constexpr uint32_t kCacheVersion='+version+';\n'+functions+(HERE/'cache_test.inc').read_text())
 subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-I'+str(ROOT/'app/src/main/cpp'),str(out/'cache.cpp'),'-o',str(out/'cache')],check=True)
 subprocess.run([str(out/'cache')],check=True)
with tempfile.TemporaryDirectory(prefix='hc-recents-jvm-')as work:
 lib=ROOT/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook'
 sources=list((ROOT/'tests/home-layout-native/stubs').rglob('*.java'))+list((ROOT/'tests/home-dock-window/stubs').rglob('*.java'))
 sources += [lib/'provider/HomeLayoutPrefsSnapshot.java',lib/'provider/HomeGesturePrefsSnapshot.java',lib/'base/FrameworkHookGate.java',lib/'rules/home/dock/HomeLayoutNativeEndpointOS4.java',ROOT/'library/common/src/main/java/com/sevtinge/hyperceiler/common/log/ModuleLog.java',HERE/'EndpointTest.java']
 subprocess.run(['javac','-encoding','UTF-8','-d',work]+list(map(str,sources)),check=True)
 subprocess.run(['java','-ea','-cp',work,'com.sevtinge.hyperceiler.libhook.rules.home.dock.EndpointTest'],check=True)
# Actual production binding must consume the runtime plan, never a symbol/name resolver.
hooks=(ROOT/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text();binding=hooks[hooks.index('bool bind_recent()'):hooks.index('size_t arm_hooks(')]
assert 'g_runtime_features.recent' in binding and 'HomeTweaksFindSymbol' not in binding and 'dartscan::' not in binding
writer=(lib/'rules/home/dock/HomeDockWindow.kt').read_text();assert 'HomeLayoutNativeEndpointOS4.writeRecent(reply,' in writer
print('recents production integration PASS: executable plan, checked all-bank readiness, shared tested wire writer')
