#!/usr/bin/env python3
"""Compile actual production source and execute registered callbacks in two isolated JVMs."""
import argparse
from pathlib import Path
import subprocess
import tempfile

parser=argparse.ArgumentParser()
parser.add_argument('--kotlin-home',required=True,type=Path)
parser.add_argument('--java',default='java')
parser.add_argument('--production',type=Path)
args=parser.parse_args()
tests=Path(__file__).resolve().parent
root=tests.parents[1]
production=args.production or root/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/controlcenter/RedirectToNotificationChannelSetting.kt'
with tempfile.TemporaryDirectory(prefix='hc-notification-channel-') as tmp:
    jar=Path(tmp)/'tests.jar'
    subprocess.run([args.java,'-cp',str(args.kotlin_home/'lib/kotlin-compiler.jar'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-kotlin-home',str(args.kotlin_home),'-jvm-target','17','-nowarn','-d',str(jar),str(production),*map(str,sorted(tests.glob('*.kt')))],check=True)
    failed=0
    for scenario in ('os4','legacy'):
        result=subprocess.run([args.java,'-cp',f"{jar}:{args.kotlin_home/'lib/kotlin-stdlib.jar'}",'fixture.NotificationChannelTestKt',scenario])
        failed+=result.returncode!=0
    raise SystemExit(1 if failed else 0)
