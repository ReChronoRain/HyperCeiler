#!/usr/bin/env python3
"""Focused host lifetime regression. Needs Python 3 and a C++20 compiler only."""
import os
from pathlib import Path
import shlex
import subprocess
import tempfile

root = Path(__file__).resolve().parents[2]
cpp = root / "app/src/main/cpp"
production = cpp / "targets/home/home_startup.cpp"
fixture = Path(__file__).with_name("lifetime_test.cpp")
scenarios = ["before-activation", "after-activation", "failed-install", "normal-revoke", "declined-grids"]
with tempfile.TemporaryDirectory(prefix="hc-startup-lifetime-") as folder:
    work = Path(folder)
    def build(source, binary):
        subprocess.run([*shlex.split(os.environ.get("CXX", "c++")), "-std=c++20", "-O2",
                        "-Wall", "-Wextra", "-Werror", "-DHOME_STARTUP_TEST",
                        "-I" + str(cpp), "-I" + str(production.parent),
                        str(fixture), str(source), "-o", str(binary)], check=True)
    binary = work / "lifetime"
    build(production, binary)
    for scenario in scenarios:
        subprocess.run([str(binary), scenario], check=True, timeout=20)

    # A source-copy negative control must compile and fail the lifetime
    # assertion specifically. Neither a compile error nor a crash counts.
    body = production.read_text()
    guard = "unhook = unhook && !g_state.ever_activated;"
    assert body.count(guard) == 1
    mutant = work / "without_lifetime_guard.cpp"
    mutant.write_text(body.replace(guard, "/* lifetime guard deliberately removed */"))
    binary = work / "without-lifetime-guard"
    build(mutant, binary)
    result = subprocess.run([str(binary), "after-activation"], capture_output=True, text=True, timeout=20)
    assert result.returncode == 1 and "FAIL: post-activation must retain continuation" in result.stderr, result
    print("PASS negative control: removing the lifetime guard triggers the expected assertion")
