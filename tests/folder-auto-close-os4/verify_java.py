from pathlib import Path
import subprocess,json,re,os
W=Path(__file__).resolve().parents[2];D=Path(__file__).resolve().parent;out=D/'java';out.mkdir(exist_ok=True)
jdk=Path(os.environ.get('JAVA_HOME','D:/build-tools/jdk-25.0.4.1+1'))/'bin'
sources=list((W/'tests/home-layout-native/stubs').rglob('*.java'))+list((W/'tests/home-dock-window/stubs').rglob('*.java'))+[W/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/provider/HomeLayoutPrefsSnapshot.java',W/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeLayoutNativeEndpointOS4.java',D/'FolderAutoCloseEndpointTest.java']
subprocess.run([str(jdk/'javac.exe'),'-encoding','UTF-8','-d',str(out)]+list(map(str,sources)),check=True)
subprocess.run([str(jdk/'java.exe'),'-ea','-cp',str(out),'com.sevtinge.hyperceiler.libhook.rules.home.dock.FolderAutoCloseEndpointTest'],check=True)
stubs=json.loads((W/'tests/home-os4-gate-controls/stubs.json').read_text(encoding='utf8'))
gate=W/'library/core/src/main/java/com/sevtinge/hyperceiler/hooker/home/HomeOS4AdaptationGate.java'
res={}
for kind,name in re.findall(r'R\.(\w+)\.(\w+)',gate.read_text(encoding='utf8')):res.setdefault(kind,set()).add(name)
stubs['com/sevtinge/hyperceiler/core/R.java']='package com.sevtinge.hyperceiler.core;public class R {'+''.join('public static class '+k+'{'+''.join('public static final int '+n+'='+str(i)+';' for i,n in enumerate(sorted(v),1))+'}' for k,v in res.items())+'}'
stubs['FolderAutoGateTest.java']='''import androidx.preference.*;import com.sevtinge.hyperceiler.hooker.home.HomeOS4AdaptationGate;import com.sevtinge.hyperceiler.core.R;import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper;
public class FolderAutoGateTest {public static void main(String[]a){int checks=0;for(float version:new float[]{3,4})for(boolean prior:new boolean[]{false,true}){DeviceHelper.System.version=version;PreferenceGroup g=new PreferenceGroup();Preference auto=new Preference();auto.key="prefs_key_home_folder_auto_close";auto.enabled=prior;g.addPreference(auto);Preference other=new Preference();other.key="prefs_key_home_other_disable_prestart";g.addPreference(other);HomeOS4AdaptationGate.apply(g,R.xml.home_other_new);if(auto.enabled!=prior)throw new AssertionError("dependency overwritten");checks++;if(other.enabled!=(version!=4))throw new AssertionError("other gate changed");checks++;}System.out.println("FOLDER_AUTO_CLOSE_UI="+checks+" checks; failed=0");}}'''
keep=['androidx/preference/Preference.java','androidx/preference/PreferenceGroup.java','com/sevtinge/hyperceiler/libhook/utils/api/DeviceHelper.java','com/sevtinge/hyperceiler/core/R.java','FolderAutoGateTest.java']
files=[]
for n in keep:
 p=out/'stubs'/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(stubs[n],encoding='utf8');files.append(str(p))
subprocess.run([str(jdk/'javac.exe'),'-encoding','UTF-8','-d',str(out/'gate'),str(gate)]+files,check=True)
subprocess.run([str(jdk/'java.exe'),'-ea','-cp',str(out/'gate'),'FolderAutoGateTest'],check=True)
