#!/usr/bin/env python3
"""Run the real NetworkSpeedSpacing Java callbacks against a deterministic message queue."""

import argparse
import os
from pathlib import Path
import subprocess  # nosec B404
import tempfile
import shutil

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument(
    "--source",
    type=Path,
    default=ROOT
    / "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/statusbar/network/NetworkSpeedSpacing.java",
)
args = parser.parse_args()


java_home = (
    Path(os.environ["JAVA_HOME"])
    if os.environ.get("JAVA_HOME")
    else Path(shutil.which("javac") or "javac").resolve().parents[1]
)
with tempfile.TemporaryDirectory(prefix="hc-network-spacing-") as tmp:
    work = Path(tmp)
    for fixture in (Path(__file__).parent / "fixtures").rglob("*.java"):
        file = work / fixture.relative_to(Path(__file__).parent / "fixtures")
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(fixture.read_text())
    target = (
        work
        / "com/sevtinge/hyperceiler/libhook/rules/systemui/statusbar/network/NetworkSpeedSpacing.java"
    )
    target.parent.mkdir(parents=True)
    target.write_text(args.source.read_text())
    # Developer-selected tools/fixtures only; argv is separate and no shell is used.
    # nosemgrep: python_exec_rule-subprocess-call, python_exec_rule-subprocess-call-array
    subprocess.run(  # nosec B603
        [
            str(java_home / "bin/javac"),
            "-encoding",
            "UTF-8",
            "-d",
            str(work / "classes"),
            *map(str, work.rglob("*.java")),
        ],
        check=True,
    )
    # Developer-selected tools/fixtures only; argv is separate and no shell is used.
    raise SystemExit(
        # nosemgrep: python_exec_rule-subprocess-call, python_exec_rule-subprocess-call-array
        subprocess.run(  # nosec B603
            [
                str(java_home / "bin/java"),
                "-cp",
                str(work / "classes"),
                "fixture.TestMain",
            ]
        ).returncode
    )
