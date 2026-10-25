"""Carrier-name hook regressions plus source-level mutation controls.

The production hook sources are compiled against host fixtures and executed by
the generated harness. Mutations are applied to an in-memory copy only, so the
working tree is never modified.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import hyperceiler_test_paths as test_paths  # noqa: E402

BASE = Path(__file__).resolve().parent
SOURCE = BASE.parents[1]
ROOT = test_paths.TEST_DATA_ROOT
CACHE = test_paths.BUILD_CACHE
JAVA = test_paths.JAVA_HOME / "bin"
KOTLIN_HOME = test_paths.KOTLIN_HOME

HOOK_RULES = "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules"
HOOK_UTILS = "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/utils/hookapi"
RULE = SOURCE / HOOK_RULES / "systemui/controlcenter/CustomCarrierText.kt"
NETWORK_NAME = SOURCE / HOOK_UTILS / "systemui/CarrierNetworkName.kt"
SOURCES = [RULE, NETWORK_NAME]

JARS = ["hook-xposed-102-1.3.0.jar", "core-1.3.0.jar", "api-102.0.0.jar"]
DEPS = [CACHE / "changed-hooks-compile" / name for name in JARS] + [
    KOTLIN_HOME / "lib/kotlin-stdlib.jar",
    test_paths.ANDROID_HOME / "platforms/android-37.0/android.jar",
]

TIMEOUT_SECONDS = 60
RESULT_PATTERN = re.compile(r"RESULT passes=(\d+) failures=(\d+)")

SCOPE = (
    "Complete production Kotlin object; real EzHook1.3.0/API102 callback chain. "
    "Android views, version/prefs/property source, target classes and hook "
    "installation backend are host fixtures; no ROM DEX/ART/LSPosed/device "
    "execution."
)


def require(condition: bool, message: str) -> None:
    """Fail loudly instead of a bare check, so the guard survives ``python -O``."""
    if not condition:
        raise RuntimeError(message)


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def inputs() -> dict:
    tracked = [*SOURCES, Path(__file__), *sorted((BASE / "fixtures").rglob("*"))]
    return {
        str(test_paths.report_path(path, ROOT)): digest(path)
        for path in tracked
        if path.is_file()
    }


def execute(command: list) -> subprocess.CompletedProcess:
    """Run one build step; every argument is an internal path or a literal."""
    return subprocess.run(  # noqa: S603  # nosec B603
        [str(part) for part in command],
        capture_output=True,
        text=True,
        timeout=TIMEOUT_SECONDS,
        check=False,
    )


def compile_fixtures(classes: Path, classpath: str) -> None:
    fixtures = [
        path
        for path in sorted((BASE / "fixtures").rglob("*.java"))
        if path.name != "TestMain.java"
    ]
    command = [
        JAVA / "javac",
        "--release",
        "21",
        "-proc:none",
        "-cp",
        classpath,
        "-d",
        classes,
        *fixtures,
    ]
    result = execute(command)
    require(result.returncode == 0, "fixture compile failed:\n" + result.stdout + result.stderr)


def compile_kotlin(work: Path, classes: Path, body: str) -> None:
    prod = work / RULE.name
    prod.write_text(body, encoding="utf-8")
    helper = work / NETWORK_NAME.name
    helper.write_text(NETWORK_NAME.read_text(encoding="utf-8"), encoding="utf-8")
    command = [
        JAVA / "java",
        "-cp",
        KOTLIN_HOME / "lib/kotlin-compiler.jar",
        "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
        "-kotlin-home",
        KOTLIN_HOME,
        "-jvm-target",
        "21",
        "-classpath",
        str(classes) + os.pathsep + os.pathsep.join(map(str, DEPS)),
        "-d",
        classes,
        prod,
        helper,
        BASE / "fixtures/Visible.kt",
    ]
    result = execute(command)
    require(result.returncode == 0, "kotlin compile failed:\n" + result.stdout + result.stderr)


def compile_harness(classes: Path, classpath: str) -> None:
    command = [
        JAVA / "javac",
        "--release",
        "21",
        "-proc:none",
        "-cp",
        classpath,
        "-d",
        classes,
        BASE / "fixtures/TestMain.java",
    ]
    result = execute(command)
    require(result.returncode == 0, "harness compile failed:\n" + result.stdout + result.stderr)


def run_harness(classpath: str) -> dict:
    result = execute([JAVA / "java", "-ea", "-cp", classpath, "TestMain"])
    match = RESULT_PATTERN.search(result.stdout or "")
    require(match is not None, "harness produced no result:\n" + result.stdout + result.stderr)
    return {
        "passed": int(match.group(1)),
        "failed": int(match.group(2)),
        "exit_code": result.returncode,
        "stdout_tail": (result.stdout or "")[-4000:],
    }


def run(out: Path, mutation: str | None = None) -> dict:
    out.mkdir(parents=True, exist_ok=False)
    before = inputs()
    body = RULE.read_text(encoding="utf-8")
    if mutation:
        old, new = MUTANTS[mutation]
        require(
            body.count(old) == 1,
            f"mutation {mutation} matched {body.count(old)} times, expected 1",
        )
        body = body.replace(old, new)

    with tempfile.TemporaryDirectory(prefix="hc-carrier-") as temp:
        work = Path(temp)
        classes = work / "classes"
        classes.mkdir()
        classpath = os.pathsep.join(map(str, DEPS))
        allpath = str(classes) + os.pathsep + classpath

        compile_fixtures(classes, classpath)
        compile_kotlin(work, classes, body)
        compile_harness(classes, allpath)
        outcome = run_harness(allpath)

    after = inputs()
    report = {
        "passed": outcome["failed"] == 0 and outcome["exit_code"] == 0,
        "tests": {"passed": outcome["passed"], "failed": outcome["failed"]},
        "exit_code": outcome["exit_code"],
        "stdout_tail": outcome["stdout_tail"],
        "inputs_before": before,
        "inputs_after": after,
        "inputs_unchanged": before == after,
        "dependencies": [str(path) for path in DEPS],
        "mutation": mutation,
        "device_status": "NOT_TESTED",
        "scope": SCOPE,
    }
    (out / "report.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    return report


MUTANTS = {
    "legacy_argument_roles": (
        "val text = param.args[1] as? String",
        "val text = param.args[2] as? String",
    ),
    "overwrite_slot": (
        "param.args[1] = when (mode)",
        "param.args[2] = when (mode)",
    ),
    "remove_unknown_status_guard": (
        "carrierCount !in 0..2 || slot !in 0..1",
        "slot !in 0..1",
    ),
    "wrong_primary_slot": (
        "else if (slot == 0) name else \"\"",
        "else if (slot == 1) name else \"\"",
    ),
    "discard_empty_name_guard": (
        "if (name.isNullOrBlank()) text else if",
        "if (false) text else if",
    ),
    "hide_measure_by_skip": (
        "override fun after(param: HookParam) {\n"
        "                        if (!active.get() || param.hasThrowable) return",
        "override fun before(param: HookParam) {\n"
        "                        if (!active.get() || param.isSkipped) return\n"
        "                        param.result = null",
    ),
    "drop_text_separator": (
        "views = listOf(viewField(layout, \"carrierSeparatorView\"), "
        "viewField(layout, \"carrierSeparatorText\"))",
        "views = listOf(viewField(layout, \"carrierSeparatorView\"))",
    ),
    "drop_rollback": (
        "try { handle.unhook() } catch (rollback: RuntimeException) "
        "{ error.addSuppressed(rollback) }",
        "try { /* retain registration */ } catch (rollback: RuntimeException) "
        "{ error.addSuppressed(rollback) }",
    ),
    "count_dependent_display_slot": (
        "else if (slot == 0) name else \"\"",
        "else if (carrierCount == 1 || slot == 0) name else \"\"",
    ),
    "empty_primary_slot_skipped": (
        "if (mode != 3 && mode != MODE_REAL_NETWORK_NAME && text.isNullOrEmpty()) return",
        "if (text.isNullOrEmpty()) return",
    ),
    "drop_os4_gate": (
        "if (isMoreHyperOSVersion(4f)) {",
        "if (true) {",
    ),
    "network_name_slot_unchecked": (
        "MODE_REAL_NETWORK_NAME -> resolveRealNetworkName(slot) ?: text",
        "MODE_REAL_NETWORK_NAME -> resolveRealNetworkName(0) ?: text",
    ),
    "network_name_overwrites_empty": (
        "if (mode != 3 && mode != MODE_REAL_NETWORK_NAME && text.isNullOrEmpty()) return",
        "if (mode != 3 && text.isNullOrEmpty()) return",
    ),
    "network_name_without_fallback": (
        "MODE_REAL_NETWORK_NAME -> resolveRealNetworkName(slot) ?: text",
        "MODE_REAL_NETWORK_NAME -> resolveRealNetworkName(slot) ?: \"\"",
    ),
    "custom_name_ignored": (
        "PrefsBridge.getString(key, \"\")",
        "\"\"",
    ),
    "custom_name_not_per_slot": (
        "1 -> SECOND_SLOT_PREF_KEY",
        "1 -> FIRST_SLOT_PREF_KEY",
    ),
    "custom_name_blank_accepted": (
        ".trim().takeIf { it.isNotEmpty() }",
        ".trim()",
    ),
    "network_name_resolves_wrong_slot": (
        "return CarrierNetworkName.resolve(context, slot)",
        "return CarrierNetworkName.resolve(context, 0)",
    ),
    "custom_name_before_network": (
        "readCustomNetworkName(slot)?.let { return it }",
        "",
    ),
}


def negative_controls(output: Path) -> dict:
    checks = []
    for name in MUTANTS:
        outcome = run(output / name, name)
        checks.append(
            {
                "mutation": name,
                "rejected": not outcome["passed"]
                and outcome["tests"]["failed"] > 0
                and outcome["inputs_unchanged"],
                "failed_tests": outcome["tests"]["failed"],
            }
        )
    verdict = {
        "passed": all(check["rejected"] for check in checks),
        "checks": checks,
        "scope": "In-memory source-copy regressions. Production file unchanged.",
    }
    (output / "negative-controls.json").write_text(
        json.dumps(verdict, indent=2), encoding="utf-8"
    )
    return verdict


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--negative-controls", action="store_true")
    args = parser.parse_args()

    report = run(args.output)
    require(report["passed"], "regression suite failed")

    if args.negative_controls:
        verdict = negative_controls(args.output)
        require(verdict["passed"], "at least one mutation survived")

    print(f"passes={report['tests']['passed']} failures={report['tests']['failed']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
