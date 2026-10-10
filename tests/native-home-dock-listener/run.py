#!/usr/bin/env python3
"""Compile the real production Kotlin object and exercise its preference callback."""
import argparse
import os
from pathlib import Path
import subprocess
import tempfile

parser = argparse.ArgumentParser()
parser.add_argument("--kotlin-home", required=True, type=Path, help="Unpacked Kotlin compiler (kotlinc directory)")
parser.add_argument("--java", default="java", help="JDK executable; Windows java.exe works from WSL")
parser.add_argument("--production", type=Path, help="Optional production Kotlin file for baseline comparison")
args = parser.parse_args()
root = Path(__file__).resolve().parents[2]
tests = Path(__file__).resolve().parent
windows_java = args.java.lower().endswith(".exe")

def runtime_path(path):
    path = str(Path(path).resolve())
    if windows_java and os.name != "nt":
        return subprocess.check_output(["wslpath", "-w", path], text=True).strip()
    return path

compiler = args.kotlin_home / "lib" / "kotlin-compiler.jar"
stdlib = args.kotlin_home / "lib" / "kotlin-stdlib.jar"
production = args.production or root / "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/other/NativeHomeHooksOS4.kt"
with tempfile.TemporaryDirectory(prefix="hc-dock-listener-", dir=os.environ.get("HC_TEST_TMPDIR")) as out:
    jar = Path(out) / "listener-tests.jar"
    subprocess.run([
        args.java, "-cp", runtime_path(compiler), "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
        "-kotlin-home", runtime_path(args.kotlin_home), "-jvm-target", "17", "-d", runtime_path(jar),
        runtime_path(production), *[runtime_path(p) for p in sorted(tests.glob("*.kt"))],
    ], check=True)
    separator = ";" if windows_java or os.name == "nt" else ":"
    subprocess.run([
        args.java, "-cp", separator.join([runtime_path(jar), runtime_path(stdlib)]),
        "com.sevtinge.hyperceiler.tests.nativehome.NativeHomeDockListenerTestKt",
    ], check=True)
