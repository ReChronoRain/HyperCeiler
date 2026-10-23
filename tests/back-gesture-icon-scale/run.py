#!/usr/bin/env python3
"""Exercise the shipping worker refresh pass after Rust-only startup."""
from pathlib import Path
import os
import shlex
import subprocess
import tempfile

root = Path(__file__).resolve().parents[2]
here = Path(__file__).resolve().parent
source = (root / 'app/src/main/cpp/targets/home/home_layout_hooks.cpp').read_text()
worker = source.index('void *worker(void *) {')
start = source.index('        if (iterations % 10 == 0) {', worker)
end = source.index('        if ((any_knob || top_probe)', start)
constants = source[source.index('constexpr size_t kConfigCaptureSlot'):source.index('using Slot =')]
located = source[source.index('struct Located {'):source.index('\n};', source.index('struct Located {')) + 3]
dart = source[source.index('struct DartLibrary {'):source.index('\n};', source.index('struct DartLibrary {')) + 3]

with tempfile.TemporaryDirectory(prefix='hc-gesture-scale-') as directory:
    out = Path(directory)
    (out / 'worker-refresh.inc').write_text(source[start:end])
    (out / 'worker-types.inc').write_text(constants + '\n' + located + '\n' + dart)
    subprocess.run(shlex.split(os.environ.get('CXX', 'c++')) + [
        '-std=c++20', '-Wall', '-Wextra', '-Werror', '-pthread',
        '-I' + str(root / 'app/src/main/cpp'), '-I' + str(out),
        str(here / 'worker_test.cpp'), '-o', str(out / 'worker-test')], check=True)
    subprocess.run([str(out / 'worker-test')], check=True)

# The worker must retire the one-shot loader while it still owns the setup lease.
# Otherwise the newly supported late image can be published by two threads.
handoff = source.index('    g_loader_prime_finished.store(true', worker)
release = source.index('    setup_lease.release();', worker)
assert handoff < release < start
prime = source[source.index('void prime_home_layout_knobs('):source.index('void prime_home_layout_probe(')]
lease = prime.index('struct ReleasePrime')
assert prime.index('if (g_loader_prime_finished.load(', lease) < prime.index('ensure_dart_library()')
print('worker/loader handoff stays inside the setup lease; late loader rechecks ownership: PASS')
