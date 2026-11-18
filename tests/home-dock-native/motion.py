#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
from pathlib import Path
import os,subprocess,tempfile
here=Path(__file__).resolve().parent
root=here.parents[1]
jdk=Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home'))/'bin'
source=root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock'
with tempfile.TemporaryDirectory(prefix='hc-dock-motion-') as temporary:
    work=Path(temporary)
    subprocess.run([str(jdk/'javac'),'-d',str(work),str(source/'DockNativeMotion.java'),str(source/'DockRecentsMotion.java'),str(here/'MotionGolden.java')],check=True)
    subprocess.run([str(jdk/'java'),'-cp',str(work),'MotionGolden',str(work/'motion.txt')],check=True)
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-ffp-contract=off','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/'motion_test.cpp'),'-o',str(work/'motion')],check=True)
    subprocess.run([str(work/'motion'),str(work/'motion.txt')],check=True)
