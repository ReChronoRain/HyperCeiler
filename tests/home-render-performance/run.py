#!/usr/bin/env python3
"""Host regressions for render-thread bootstrap and process identity overhead."""
from pathlib import Path
import subprocess,tempfile
ROOT=Path(__file__).resolve().parents[2];HERE=Path(__file__).resolve().parent
with tempfile.TemporaryDirectory(prefix='hc-render-perf-')as work:
 binary=Path(work)/'identity'
 subprocess.run(['c++','-std=c++20','-O2','-Wall','-Wextra','-Werror','-I'+str(ROOT/'app/src/main/cpp'),str(HERE/'process_test.cpp'),'-o',str(binary)],check=True)
 subprocess.run([str(binary)],check=True)
# This compiles the actual shipping bootstrap body against a counted slot host,
# including optional-missing, backend-failure, busy-config and late-image cases.
subprocess.run(['python3',str(ROOT/'tests/layout-refresh-os4/run.py')],check=True)

source=(ROOT/'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text()
def body(name):
 at=source.index(name+' {') if name+' {' in source else source.index(name+'{')
 start=source.index('{',at);end=start+1;depth=1
 while depth:depth+=(source[end]=='{')-(source[end]=='}');end+=1
 return source[at:end].replace('std::ifstream maps("/proc/self/maps")','std::istringstream maps("")').replace('getpid()','fixture_pid()').replace('gettid()','fixture_tid()')
with tempfile.TemporaryDirectory(prefix='hc-native-primer-perf-')as work:
 src=Path(work)/'primers.cpp';binary=Path(work)/'primers'
 src.write_text((HERE/'native_primers.inc').read_text()+body('void prime_native_columns(HookFunction hook, UnhookFunction unhook)')+'\n'+body('void prime_back_regions(HookFunction hook,UnhookFunction unhook)')+(HERE/'native_primers_test.inc').read_text())
 subprocess.run(['c++','-std=c++20','-O2','-I'+str(ROOT/'app/src/main/cpp'),str(src),'-o',str(binary)],check=True)
 subprocess.run([str(binary)],check=True)
