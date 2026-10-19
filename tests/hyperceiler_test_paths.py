#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""Portable locations for the optional OS4 regression tools and ROM fixtures.

Importing this module only resolves paths; it does not download dependencies or
create directories. See PORTABILITY.md for the environment variables and inputs.
"""

import os
from pathlib import Path


def configured_path(name: str, default: Path) -> Path:
    """Use a nonempty environment override, expanding a user's home directory."""
    value = os.environ.get(name)
    return Path(value).expanduser().resolve() if value else default.resolve()


SOURCE_ROOT = Path(__file__).resolve().parents[1]
BUILD_CACHE = configured_path(
    "HYPERCEILER_BUILD_CACHE", Path.home() / ".cache/hyperceiler-build"
)

def java_home(cache: Path = BUILD_CACHE) -> Path:
    return configured_path("JAVA_HOME", cache / "jdk-25.0.4.1")


def android_home(cache: Path = BUILD_CACHE) -> Path:
    return configured_path(
        "ANDROID_HOME", configured_path("ANDROID_SDK_ROOT", cache / "android-sdk")
    )


def snapshot_root(cache: Path = BUILD_CACHE) -> Path:
    return configured_path("HYPERCEILER_SNAPSHOT_ROOT", cache)


JAVA_HOME = java_home()
ANDROID_HOME = android_home()
ANDROID_NDK_HOME = configured_path(
    "ANDROID_NDK_HOME", ANDROID_HOME / "ndk/28.2.13676358"
)
TEST_DATA_ROOT = configured_path("HYPERCEILER_TEST_DATA", SOURCE_ROOT.parent)
KOTLIN_HOME = configured_path(
    "KOTLIN_HOME", TEST_DATA_ROOT / "tools-cache/kotlin-2.4.10/kotlinc"
)
ROM_ROOT = configured_path("HYPERCEILER_ROM_ROOT", TEST_DATA_ROOT / "firmware")
EXTRACTED_ROM_ROOT = configured_path(
    "HYPERCEILER_EXTRACTED_ROM_ROOT", ROM_ROOT / "extracted"
)
SNAPSHOT_ROOT = snapshot_root()


def report_path(path: Path, root: Path) -> Path:
    """Keep report paths relative even when external inputs live outside root."""
    return Path(os.path.relpath(path, root))


def java_tool(name: str) -> Path:
    """Resolve a JDK executable for the current host platform."""
    suffix = ".exe" if os.name == "nt" else ""
    return JAVA_HOME / "bin" / (name + suffix)
