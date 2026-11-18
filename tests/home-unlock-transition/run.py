#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
from pathlib import Path
import os,subprocess,sys,zipfile
if len(sys.argv)!=3:raise SystemExit('Usage: python3 run.py <Android SDK> <output directory>')
here=Path(__file__).resolve().parent
sdk=Path(sys.argv[1]);work=Path(sys.argv[2]);work.mkdir(parents=True,exist_ok=True)
jdk=Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home'))/'bin'
platform=sdk/'platforms/android-37.0/android.jar'
source=here.parents[1]/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeUnlockTransitionEndpointOS4.java'
subprocess.run([str(jdk/'javac'),'--release','17','-cp',str(platform),'-d',str(work/'classes'),str(source),str(source.parent/'HomeDockBackendOS4.java'),str(here/'BackendCoordinatorTest.java'),str(here/'EndpointDeviceTest.java'),str(source.parent/'HomeWindowVisibilityContract.java'),str(here/'PlatformVisibilityDeviceTest.java')],check=True)
subprocess.run([str(jdk/'jar'),'cf',str(work/'tests.jar'),'-C',str(work/'classes'),'.'],check=True)
subprocess.run([str(sdk/'build-tools/37.0.0/d8'),'--min-api','35','--lib',str(platform),'--output',str(work),str(work/'tests.jar')],check=True)
with zipfile.ZipFile(work/'android-tests.jar','w') as archive:archive.write(work/'classes.dex','classes.dex')
print('Built actual endpoint Android Binder test, device entry EndpointDeviceTest, run under launcher UID; no system registration or screen changes')
