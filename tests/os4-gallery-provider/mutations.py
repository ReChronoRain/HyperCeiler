#!/usr/bin/env python3
"""Negative controls: perturb temporary copies, never the production files."""

from pathlib import Path
import argparse
import json
import shutil
import subprocess  # nosec B404
import tempfile
import sys


def require(condition, message):
    """Reject a failed invariant, including when Python optimization is enabled."""
    if not condition:
        raise AssertionError(message)


ROOT = Path(__file__).resolve().parents[2]
p = argparse.ArgumentParser()
p.add_argument("--output", required=True, type=Path)
a = p.parse_args()
a.output.mkdir(parents=True, exist_ok=True)
rel = Path("library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook")
original = (
    ROOT / rel / "rules/mediaeditor/GalleryCreationAvailabilityOS4.java"
).read_text()
mutations = {
    "no_override": ("param.setResult(result);", "/* intentionally missing override */"),
    "wrong_caller_allowed": (
        '|| !"com.miui.gallery".equals(provider.getCallingPackage())',
        "|| false",
    ),
    "original_exception_ignored": (
        "if (param.getHasThrowable() || param.isSkipped()) return;",
        "if (param.isSkipped()) return;",
    ),
    "original_bundle_mutated": (
        "Bundle result = new Bundle(original);",
        "Bundle result = original;",
    ),
}
results = []
for name, (old, new) in mutations.items():
    require(original.count(old) == 1, (name, old))
    with tempfile.TemporaryDirectory(prefix="hc-gallery-negative-") as temp:
        t = Path(temp)
        for f in [
            "app/MediaEditor.java",
            "rules/mediaeditor/GalleryCreationAvailabilityOS4.java",
        ]:
            out = t / rel / f
            out.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(ROOT / rel / f, out)
        (t / rel / "rules/mediaeditor/GalleryCreationAvailabilityOS4.java").write_text(
            original.replace(old, new)
        )
        # Developer-selected tools/fixtures only; argv is separate and no shell is used.
        r = subprocess.run(  # nosec B603
            [
                sys.executable,
                str(Path(__file__).with_name("run.py")),
                "--source-root",
                str(t),
            ],
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
        )
        (a.output / (name + ".txt")).write_text(r.stdout)
        failures = [line for line in r.stdout.splitlines() if line.startswith("FAIL ")]
        result = {
            "mutation": name,
            "exit_code": r.returncode,
            "detected": r.returncode != 0 and bool(failures),
            "failures": failures,
        }
        results.append(result)
        print(name, "DETECTED" if result["detected"] else "MISSED", len(failures))
(a.output / "report.json").write_text(json.dumps(results, indent=2) + "\n")
raise SystemExit(0 if all(x["detected"] for x in results) else 1)
