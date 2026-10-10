#!/usr/bin/env python3
"""Focused host lifetime regression. Needs Python 3 and a C++20 compiler only."""

import os
from pathlib import Path
import shlex
import subprocess  # nosec B404
import tempfile


def require(condition, message):
    """Reject a failed invariant, including when Python optimization is enabled."""
    if not condition:
        raise AssertionError(message)


root = Path(__file__).resolve().parents[2]
cpp = root / "app/src/main/cpp"
production = cpp / "targets/home/home_startup.cpp"
fixture = Path(__file__).with_name("lifetime_test.cpp")
scenarios = [
    "before-activation",
    "after-activation",
    "failed-install",
    "normal-revoke",
    "declined-grids",
]
with tempfile.TemporaryDirectory(prefix="hc-startup-lifetime-") as folder:
    work = Path(folder)

    def build(source, binary):
        # Developer-selected tools/fixtures only; argv is separate and no shell is used.
        subprocess.run(  # nosec B603
            [
                *shlex.split(os.environ.get("CXX", "c++")),
                "-std=c++20",
                "-O2",
                "-Wall",
                "-Wextra",
                "-Werror",
                "-DHOME_STARTUP_TEST",
                "-I" + str(cpp),
                "-I" + str(production.parent),
                str(fixture),
                str(source),
                "-o",
                str(binary),
            ],
            check=True,
        )

    binary = work / "lifetime"
    build(production, binary)
    for scenario in scenarios:
        # Developer-selected tools/fixtures only; argv is separate and no shell is used.
        subprocess.run(  # nosec B603
            [str(binary), scenario], check=True, timeout=20
        )

    # A source-copy negative control must compile and fail the lifetime
    # assertion specifically. Neither a compile error nor a crash counts.
    body = production.read_text()
    guard = "unhook = unhook && !g_state.ever_activated;"
    require(body.count(guard) == 1, "Required invariant failed")
    mutant = work / "without_lifetime_guard.cpp"
    mutant.write_text(body.replace(guard, "/* lifetime guard deliberately removed */"))
    binary = work / "without-lifetime-guard"
    build(mutant, binary)
    # Developer-selected tools/fixtures only; argv is separate and no shell is used.
    result = subprocess.run(  # nosec B603
        [str(binary), "after-activation"], capture_output=True, text=True, timeout=20
    )
    require(
        result.returncode == 1
        and "FAIL: post-activation must retain continuation" in result.stderr,
        result,
    )
    print(
        "PASS negative control: removing the lifetime guard triggers the expected assertion"
    )
