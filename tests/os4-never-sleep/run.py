#!/usr/bin/env python3
"""Run complete production hook classes against a reviewed ROM-method host model."""
import argparse, os, subprocess, tempfile
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths

ROOT=Path(__file__).resolve().parents[2]
p=argparse.ArgumentParser();p.add_argument('--baseline',action='store_true');args=p.parse_args()
java=test_paths.JAVA_HOME/'bin'
with tempfile.TemporaryDirectory(prefix='hc-never-sleep-') as tmp:
    work=Path(tmp)
    for source in (Path(__file__).parent/'fixtures').rglob('*.java'):
        dest=work/source.relative_to(Path(__file__).parent/'fixtures');dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(source.read_text())
    for name in ['UnlockNeverSleepScreen','UnlockNeverSleepScreenOS4']:
        rel=Path('com/sevtinge/hyperceiler/libhook/rules/systemsettings')/(name+'.java')
        dest=work/rel;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text((ROOT/'library/libhook/src/main/java'/rel).read_text())
    subprocess.run([str(java/'javac'),'-encoding','UTF-8','-d',str(work/'classes'),*map(str,work.rglob('*.java'))],check=True)
    raise SystemExit(subprocess.run([str(java/'java'),'-cp',str(work/'classes'),'TestMain',*(['baseline'] if args.baseline else [])]).returncode)
