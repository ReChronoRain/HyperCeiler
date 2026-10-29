#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
import os,re,shutil,subprocess,tempfile,json
from pathlib import Path
root=Path(__file__).resolve().parents[2];here=Path(__file__).parent
cpp=root/'app/src/main/cpp';lib=root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook'
jdk=Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home'))/'bin'
def run(command):subprocess.run([str(c) for c in command],check=True)
with tempfile.TemporaryDirectory(prefix='hc-dock-horizontal-') as folder:
    out=Path(folder)
    fixture=json.loads((here/'position-8584.json').read_text())
    header='static Functions position_fixture(){return {\n'
    for f in fixture['functions']:header+='{'+str(f['va'])+',{'+','.join(map(str,f['words']))+'}},\n'
    header+='};}\nstatic constexpr uint32_t position_config='+str(fixture['config'])+',position_owner='+str(fixture['owner'])+';\n'
    (out/'position_fixture.h').write_text(header)
    run([shutil.which('c++'),'-std=c++20','-O1','-g','-Wall','-Wextra','-Werror','-fsanitize=address,undefined','-I'+str(cpp),'-I'+str(root/'tests/runtime-feature-addressing'),'-I'+str(out),here/'position_test.cpp','-o',out/'position'])
    run([out/'position'])
    source=(cpp/'targets/home/home_layout_hooks.cpp').read_text();first=source[source.index('void prime_home_layout_first_render_locked()'):source.index('void prime_home_layout_first_render(HookFunction')];assert 'bind_dock_position()' in first and 'kDockPositionSlotBase+2' in first
    source=(cpp/'targets/home/home_layout_hooks.cpp').read_text();start=source.index('extern "C" void hc_layout_workspace_layout(');end=source.index('\nextern "C"',start+1)
    (out/'actual_callback.h').write_text(source[start:end])
    begin=source.index('        if (index == 5 && knob.symbol ==',source.index('size_t arm_hooks('))
    finish=source.index('        const size_t slot = kKnobHookSlotBase + index;',begin)
    selection=source[begin:finish]
    entries=['hc_layout_capsule_entry','hc_layout_workspace_entry','hc_layout_workspace_occupied_entry','hc_layout_hotseat_horizontal_entry','hc_layout_container_probe_entry','hc_layout_indicator_policy_entry','hc_layout_indicator_slide_only_entry']
    binding='#include <string>\n#include <cassert>\n#include <cstdio>\n'+''.join('void '+f+'(){}\n' for f in entries)
    binding+='struct Knob{std::string symbol;void* hook_entry=nullptr;};Knob g_knobs[8];int main(){\n'
    for index,name,entry in [(2,'GridCellDelegate.performLayout','hc_layout_workspace_entry'),(3,'GridOccupiedCellDelegate.performLayout','hc_layout_workspace_occupied_entry'),(4,'HotSeatLayoutDelegate.cellLayout','hc_layout_hotseat_horizontal_entry')]:
        binding+='{size_t index='+str(index)+';auto& knob=g_knobs[index];knob.symbol="'+name+'";knob.hook_entry=(void*)hc_layout_capsule_entry;\n'+selection+'assert(knob.hook_entry==(void*)'+entry+');}\n'
    binding+='puts("actual final knob entry selection PASS, including hotseat after generic initialization");}\n'
    (out/'binding.cpp').write_text(binding)
    run([shutil.which('c++'),'-std=c++20',out/'binding.cpp','-o',out/'binding'])
    run([out/'binding'])
    source=(cpp/'targets/home/home_layout_config.cpp').read_text();start=source.index('void serialize_config(');end=source.index('bool write_config_cache(',start)
    version=re.search(r'constexpr uint32_t kCacheVersion = (\d+);',source)[1]
    (out/'actual_cache.h').write_text('namespace home_layout {\nconstexpr char kCacheMagic[]={\'H\',\'C\',\'L\',\'C\'};constexpr uint32_t kCacheVersion='+version+';\n'+source[start:end]+'\n}\n')
    run([os.environ.get('CXX',shutil.which('c++')),'-std=c++20','-O1','-g','-fsanitize=address,undefined','-I'+str(cpp),'-I'+str(root/'tests/home-layout-native'),'-I'+str(out),here/'native_test.cpp','-o',out/'native'])
    run([out/'native'])
    stubs=[p for p in (root/'tests/home-layout-native/stubs').rglob('*.java') if p.name!='ContentResolver.java']
    stubs += list((root/'tests/hyos-control/java').rglob('*.java'))
    stubs += [root/'tests/home-dock-window/stubs/android/os'/f for f in ['Binder.java','IBinder.java','Parcel.java']]
    prod=[lib/f for f in ['base/FrameworkHookGate.java','provider/HomeLayoutPrefsSnapshot.java','provider/HomeGesturePrefsSnapshot.java','rules/home/dock/HomeLayoutNativeEndpointOS4.java']]
    prod += [root/'library/common/src/main/java/com/sevtinge/hyperceiler/common/log/ModuleLog.java']
    run([jdk/'javac','--release','21','-encoding','UTF-8','-d',out,*stubs,*prod,here/'EndpointTest.java'])
    run([jdk/'java','-ea','-cp',out,'com.sevtinge.hyperceiler.libhook.rules.home.dock.EndpointTest'])
