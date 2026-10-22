#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""Copy pure production predicates into the host fixture harness, without rewriting them."""
import re
import sys
from pathlib import Path

source = Path(sys.argv[1]).read_text()
names = ("bl_target", "capsule_wrapper_layout_compatible",
         "workspace_geometry_code_compatible", "hotseat_geometry_code_compatible",
         "indicator_policy_compatible")
output = []
for name in names:
    match = re.search(r"^bool " + name + r"\([^;{}]+\)\s*\{", source, re.M)
    if not match:
        raise SystemExit(f"Missing production function: {name}")
    at = match.end()
    depth = 1
    while depth and at < len(source):
        depth += (source[at] == "{") - (source[at] == "}")
        at += 1
    if depth:
        raise SystemExit(f"Unbalanced production function: {name}")
    output.append(source[match.start():at])
Path(sys.argv[2]).write_text("\n\n".join(output) + "\n")
