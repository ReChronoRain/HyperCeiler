#!/usr/bin/env python3
"""Compile the production hook and invoke the callbacks it actually registers."""
import argparse
from pathlib import Path
import subprocess
import tempfile

parser = argparse.ArgumentParser()
parser.add_argument("--kotlin-home", required=True, type=Path)
parser.add_argument("--java", default="java")
parser.add_argument("--production", type=Path)
args = parser.parse_args()
tests = Path(__file__).resolve().parent
root = tests.parents[1]
production = args.production or root / "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/controlcenter/AutoDismissExpandedPopupsHook.kt"
with tempfile.TemporaryDirectory(prefix="hc-notification-expansion-") as tmp:
    jar = Path(tmp) / "tests.jar"
    subprocess.run([
        args.java, "-cp", str(args.kotlin_home / "lib/kotlin-compiler.jar"),
        "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler", "-kotlin-home", str(args.kotlin_home),
        "-jvm-target", "17", "-nowarn", "-d", str(jar), str(production),
        *map(str, sorted(tests.glob("*.kt"))),
    ], check=True)
    subprocess.run([
        args.java, "-cp", f"{jar}:{args.kotlin_home / 'lib/kotlin-stdlib.jar'}",
        "com.sevtinge.hyperceiler.tests.notification.NotificationExpansionTestKt",
    ], check=True)
