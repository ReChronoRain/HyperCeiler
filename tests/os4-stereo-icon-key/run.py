#!/usr/bin/env python3
"""Compile the actual production hook against narrow host dependencies."""

import argparse
import hashlib
import json
import subprocess  # nosec B404
import tempfile
import defusedxml.ElementTree as element_tree
from pathlib import Path


def require(condition, message):
    """Reject a failed invariant, including when Python optimization is enabled."""
    if not condition:
        raise AssertionError(message)


HERE = Path(__file__).resolve().parent
SOURCE = HERE.parents[1]
PRODUCTION = (
    SOURCE
    / "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/statusbar/icon/all/StatusBarIcon.java"
)
XML = SOURCE / "library/core/src/main/res/xml/system_ui_status_bar_icon_manage_new.xml"
KEY = "prefs_key_system_ui_status_bar_icon_soundbox_group_stereo"


def sha(path):
    """Hash an input or produced artifact."""
    return hashlib.sha256(path.read_bytes()).hexdigest()


parser = argparse.ArgumentParser()
parser.add_argument("--java", default="java")
parser.add_argument("--javac", default="javac")
parser.add_argument("--output", required=True, type=Path)
args = parser.parse_args()
args.output.mkdir(parents=True, exist_ok=False)
nodes = [
    n
    for n in element_tree.parse(XML).getroot().iter()
    if n.attrib.get("{http://schemas.android.com/apk/res/android}key") == KEY
]
require(len(nodes) == 1, "Required invariant failed")
node = nodes[0]
require(
    node.attrib["{http://schemas.android.com/apk/res-auto}entryValues"]
    == "@array/status_bar_icon_mode_new_value",
    "Required invariant failed",
)
arrays = element_tree.parse(
    SOURCE / "library/core/src/main/res/values/arrays_app.xml"
).getroot()
values = next(
    a for a in arrays if a.attrib.get("name") == "status_bar_icon_mode_new_value"
)
require(
    [n.text for n in values] == ["0", "1", "11", "12", "2"], "Required invariant failed"
)
inputs = {
    str(p.relative_to(SOURCE)): sha(p)
    for p in [PRODUCTION, XML, *sorted(HERE.glob("*.java")), Path(__file__)]
}
with tempfile.TemporaryDirectory(prefix="hc-stereo-key-") as folder:
    work = Path(folder)

    def run(name, production):
        dest = work / name
        dest.mkdir()
        # Developer-selected tools/fixtures only; argv is separate and no shell is used.
        build = subprocess.run(  # nosec B603
            [
                args.javac,
                "--release",
                "17",
                "-d",
                str(dest),
                str(production),
                *map(str, sorted(HERE.glob("*.java"))),
            ],
            capture_output=True,
            text=True,
        )
        (args.output / (name + "-build.txt")).write_text(build.stdout + build.stderr)
        require(build.returncode == 0, "Required invariant failed")
        # Developer-selected tools/fixtures only; argv is separate and no shell is used.
        result = subprocess.run(  # nosec B603
            [args.java, "-cp", str(dest), "fixture.StereoKeyTest", KEY],
            capture_output=True,
            text=True,
        )
        (args.output / (name + "-run.txt")).write_text(result.stdout + result.stderr)
        return result

    positive = run("production", PRODUCTION)
    require(
        positive.returncode == 0 and "PASS 59 " in positive.stdout,
        "Required invariant failed",
    )
    mutant_dir = work / "old-key-source"
    mutant_dir.mkdir()
    mutant = mutant_dir / PRODUCTION.name
    current = PRODUCTION.read_text()
    require(
        current.count('"system_ui_status_bar_icon_soundbox_group_stereo"') == 1,
        "Required invariant failed",
    )
    mutant.write_text(
        current.replace(
            '"system_ui_status_bar_icon_soundbox_group_stereo"',
            '"system_ui_status_bar_icon_soundbox_stereo"',
        )
    )
    negative = run("original-wrong-key", mutant)
    require(
        negative.returncode != 0 and "AssertionError" in negative.stderr,
        "Required invariant failed",
    )
    require(
        all(sha(SOURCE / name) == digest for name, digest in inputs.items()),
        "Required invariant failed",
    )
report = dict(
    passed=True,
    positive_scenarios=59,
    original_key_regression_rejected=True,
    inputs=inputs,
    device_status="NOT_TESTED",
    scope="Actual production StatusBarIcon.init against host list/reflection/preference dependencies; "
    "setting key and modes from current XML. "
    "Not original ROM DEX execution or on-device injection.",
    limitation="Mini-status-bar behavior, connection state icon production and actual visual rendering are not changed or approved by this key-binding fix.",
)
(args.output / "report.json").write_text(json.dumps(report, indent=2) + "\n")
print(json.dumps(report, indent=2))
