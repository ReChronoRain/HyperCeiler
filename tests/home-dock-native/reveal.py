#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
from pathlib import Path
import os,subprocess,tempfile
here=Path(__file__).resolve().parent
root=here.parents[1]
jdk=Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home'))/'bin'
source=root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock'
with tempfile.TemporaryDirectory(prefix='hc-dock-reveal-') as directory:
    work=Path(directory)
    subprocess.run([str(jdk/'javac'),'-d',str(work),str(source/'DockUnlockReveal.java'),str(here/'RevealGolden.java')],check=True)
    subprocess.run([str(jdk/'java'),'-cp',str(work),'RevealGolden',str(work/'reveal.txt')],check=True)
    subprocess.run(['c++','-std=c++20','-Wall','-Wextra','-Werror','-ffp-contract=off','-fsanitize=address,undefined','-I'+str(root/'app/src/main/cpp'),str(here/'reveal_test.cpp'),'-o',str(work/'reveal')],check=True)
    subprocess.run([str(work/'reveal'),str(work/'reveal.txt')],check=True)
