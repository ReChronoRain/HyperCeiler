#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""Run the back-feedback checks; ROM fixtures and Android device checks are optional."""
import argparse
import os
from pathlib import Path
import shlex
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[2]
TESTS = ROOT / "tests/home-back-feedback"
JAVA = ROOT / "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/gesture"
MODULE_LOG = ROOT / "library/common/src/main/java/com/sevtinge/hyperceiler/common/log/ModuleLog.java"


def run(command):
    subprocess.run([str(value) for value in command], check=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--build-dir", type=Path, default=Path.home() / ".cache/hyperceiler-build/home-back-feedback")
    parser.add_argument("--launcher-library", action="append", type=Path, default=[])
    parser.add_argument("--device", action="store_true", help="Run Android Parcel checks with root app_process; does not inject touch")
    parser.add_argument("--android-jar", type=Path)
    parser.add_argument("--r8-jar", type=Path)
    args = parser.parse_args()
    if args.device and (args.android_jar is None or args.r8_jar is None):
        parser.error("--device requires --android-jar and --r8-jar")
    output = args.build_dir.expanduser().resolve()
    output.mkdir(parents=True, exist_ok=True)
    compiler = shlex.split(os.environ.get("CXX", shutil.which("clang++") or "c++"))
    print(f"Build artifacts: {output}", flush=True)
    common = [*compiler, "-std=c++20", "-O1", "-I" + str(ROOT / "app/src/main/cpp")]
    run([*common, TESTS / "NativeHoldTest.cpp", "-o", output / "hold-test"])
    run([output / "hold-test"])
    java_output = output / "java"
    java_output.mkdir(exist_ok=True)
    run(["javac", "-d", java_output, JAVA / "BackHoldState.java", TESTS / "StateTest.java"])
    run(["java", "-cp", java_output, "com.sevtinge.hyperceiler.libhook.rules.home.gesture.StateTest"])
    if args.launcher_library:
        run([*common, TESTS / "ArrowResolverTest.cpp", "-o", output / "resolver-test"])
        for fixture in args.launcher_library:
            print(f"Launcher fixture: {fixture}", flush=True)
            run([output / "resolver-test", fixture.expanduser().resolve()])
    if args.device:
        classes, dex = output / "android-classes", output / "android-dex"
        classes.mkdir(exist_ok=True)
        dex.mkdir(exist_ok=True)
        run(["javac", "-cp", args.android_jar, "-d", classes, JAVA / "BackHoldState.java",
             JAVA / "Os4BackFeedback.java", MODULE_LOG, TESTS / "BinderProtocolTest.java"])
        dex_file = dex / "classes.dex"
        dex_file.unlink(missing_ok=True)
        run(["java", "-cp", args.r8_jar, "com.android.tools.r8.D8", "--min-api", "34",
             "--output", dex, *sorted(classes.rglob("*.class"))])
        dex_file.chmod(0o444)
        command = "CLASSPATH=" + shlex.quote(str(dex_file)) + " app_process /system/bin BinderProtocolTest"
        run(["su", "-c", command])


if __name__ == "__main__":
    main()
