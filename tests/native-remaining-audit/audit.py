#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""Reproducible, read-only OS4 Home/Gallery/Weather migration inventory.

No address from this report is used to patch an image. A native export or a
matching preference name is evidence to investigate, never proof of support.
"""
import argparse
from collections import Counter
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import struct
import subprocess
import sys
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "fixture_symbols", ROOT / "tests/home-layout-native/fixture_symbols.py")
ELF = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(ELF)
JAVA = ROOT / "library/libhook/src/main/java"
APP = JAVA / "com/sevtinge/hyperceiler/libhook/app"


def sha(data):
    return hashlib.sha256(data).hexdigest()


def source(path):
    data = path.read_bytes()
    return {"path": str(test_paths.report_path(path, ROOT)), "sha256": sha(data)}


def uncomment(text):
    # Preserve quoted strings, offsets and line numbers, including // in URLs.
    pattern = r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*.*?\*/'
    return re.sub(pattern, lambda m: re.sub(r"[^\n]", " ", m[0])
                  if m[0].startswith(("//", "/*")) else m[0], text, flags=re.S)


def calls(text, name):
    clean = uncomment(text)
    for match in re.finditer(r"\b" + re.escape(name) + r"\s*\(", clean):
        depth, pos, quote, escaped = 1, match.end(), None, False
        while pos < len(clean) and depth:
            char = clean[pos]
            if quote:
                if escaped:
                    escaped = False
                elif char == "\\":
                    escaped = True
                elif char == quote:
                    quote = None
            elif char in "\"'":
                quote = char
            elif char == "(":
                depth += 1
            elif char == ")":
                depth -= 1
            pos += 1
        if depth:
            raise ValueError(f"Unbalanced {name} call")
        yield clean[match.end():pos - 1], clean.count("\n", 0, match.start()) + 1


def pref_keys(text):
    return sorted(set(re.findall(r'PrefsBridge\.\w+\(\s*"([^"]+)"', uncomment(text))))


def hooks(path):
    raw = path.read_text()
    clean = uncomment(raw)
    imports = {x.rsplit(".", 1)[-1]: x for x in
               re.findall(r"import\s+([\w.]+);", clean)}
    variables = {name: expression for name, expression in re.findall(
        r"\b(?:boolean|int|String)\s+(\w+)\s*=\s*([^;]+);", clean)}
    result = []
    for call, line in calls(raw, "initHook"):
        name_match = re.match(r"\s*(?:new\s+)?(\w+)", call)
        if not name_match or name_match[1] not in imports:
            raise ValueError(f"Unresolved hook import: {call}")
        name = name_match[1]
        base = JAVA / imports[name].replace(".", "/")
        paths = [p for suffix in (".java", ".kt") if (p := base.with_suffix(suffix)).exists()]
        if len(paths) != 1:
            raise ValueError(f"Expected one source for {name}, found {paths}")
        impl = paths[0]
        gate_keys = set(pref_keys(call))
        resolved_variables, pending = {}, [call]
        while pending:
            expression = pending.pop()
            for variable, value in variables.items():
                if variable not in resolved_variables and re.search(r"\b" + variable + r"\b", expression):
                    resolved_variables[variable] = " ".join(value.split())
                    gate_keys.update(pref_keys(value))
                    pending.append(value)
        impl_raw = impl.read_text()
        lookups = []
        for lookup in ("findAndHookMethod", "findAndChainMethod", "findAndHookConstructor", "findClass",
                       "findClassIfExists", "hookAllConstructors"):
            for arguments, lookup_line in calls(impl_raw, lookup):
                # The literal class/method arguments are enough for the legacy
                # lookup identity; don't embed the whole anonymous hook body.
                prefix = arguments.split("new IMethodHook", 1)[0]
                lookups.append({"api": lookup, "line": lookup_line,
                                "argument_prefix": " ".join(prefix.split())[:300]})
        result.append({"hook": name, "entry_source": source(path), "entry_line": line,
                       "gate": " ".join(call.split()), "gate_preference_keys": sorted(gate_keys),
                       "gate_resolved_variables": resolved_variables,
                       "implementation": source(impl),
                       "implementation_preference_keys": pref_keys(impl_raw),
                       "legacy_lookup_evidence": sorted(lookups, key=lambda x: x["line"]),
                       "device_verified": False})
    return result


def image_info(path):
    raw = path.read_bytes()
    table, named = ELF.sections(raw)
    if struct.unpack_from("<H", raw, 18)[0] != 183:
        raise ValueError(f"Not ARM64: {path}")
    symtab = named[".dynsym"]
    if symtab[9] != 24 or symtab[6] >= len(table):
        raise ValueError("Invalid dynamic symbol table")
    strings = ELF.section_bytes(raw, table[symtab[6]])
    entries = []
    for name, info, _other, index, value, size in struct.iter_unpack(
            "<IBBHQQ", ELF.section_bytes(raw, symtab)):
        if name:
            entries.append({"name": ELF.cstring(strings, name), "defined": index != 0,
                            "va": value, "size": size, "symbol_type": info & 15})
    result = {"path": str(path), "sha256": sha(raw), "size": len(raw), "machine": "AArch64",
              "has_symtab": ".symtab" in named, "has_gnu_debugdata": ".gnu_debugdata" in named,
              "gnu_debuglink": ELF.cstring(ELF.section_bytes(raw, named[".gnu_debuglink"]), 0)
              if ".gnu_debuglink" in named else None,
              "named_dynamic_symbol_count": len(entries),
              "defined_dynamic_symbol_count": sum(x["defined"] for x in entries)}
    return result, entries


def function_body(path, address, size):
    raw = path.read_bytes()
    _table, named = ELF.sections(raw)
    for sec in named.values():
        if sec[2] & 4 and sec[3] <= address and address + size <= sec[3] + sec[5]:
            start = sec[4] + address - sec[3]
            return raw[start:start + size]
    raise ValueError(f"Function is outside executable sections: {address:x}")


HOME_REPLACEMENTS = {
    "DoubleTap": ("os4_java_path_present", "HomeGestureOS4 replaces touch handling; no device input test yet."),
    "ScreenSwipe": ("os4_java_path_present", "HomeGestureOS4 replaces touch handling; no device input test yet."),
    "ShakeDevice": ("os4_java_path_present", "OS4 entry explicitly initializes the shared shake hook."),
    "SeekPoints": ("native_path_present", "Indicator policy uses home_other_seek_points in the authenticated endpoint."),
    "LayoutRules": ("native_path_present", "Native phone-grid/geometry guards pass the 7719 fixture; visual effects unverified."),
    "WorkspacePadding": ("native_path_present", "Workspace geometry target guards pass the 7719 fixture."),
    "IndicatorMarginBottom": ("native_path_present", "Native indicator/capsule geometry guard passes; no device layout test."),
    "HotSeatsMarginBottom": ("native_path_present", "Native Dock geometry guard passes; no device layout test."),
    "FolderVerticalSpacing": ("native_path_present", "Native folder geometry guard passes; no device layout test."),
    "FolderColumns": ("partial_native_path", "Column count has a native planner; title position and folder-width legacy switches are not mapped by this path."),
    "UnlockHotseatIcon": ("native_path_present", "Native Dock capacity targets resolve; effect and lifecycle still need device validation."),
    "DockCustomNew": ("os4_framework_path_present", "OS4 entry delegates Dock background to SystemFrameworkB/HomeDockWindow; no real device validation."),
    "AlwaysShowCleanUp": ("different_native_preferences", "Native hide-clear/no-clear use home_layout_recents_*; legacy always-show/hide-clean-up keys are not equivalent."),
    "AnimDurationRatio": ("different_native_preferences", "OS4 open/recents animation rate has separate home_animation_*_enable/rate configuration."),
    "IconSize": ("replaced_settings_control", "OS4 settings move icon scaling to home_layout_icon_scale; legacy home_title_icon_size control is retired."),
    "SearchBarMarginWidth": ("intentionally_unused_native_key", "Endpoint retains the legacy key in its snapshot but ignores old searchbar geometry; knob 7 now carries indicator policy."),
    "SearchBarMarginBottom": ("intentionally_unused_native_key", "Endpoint retains the legacy key in its snapshot but ignores old searchbar geometry; knob 6 now carries indicator policy."),
}

GALLERY_CANDIDATES = {
    "LongerTrashbinTime": ["get_trash_retention_days"],
    "EnableHdrEnhance": ["device_feature_api_is_support_hdr_enhance"],
    "EnablePhotoMovie": ["device_feature_api_is_device_support_photo_movie"],
    "EnableMagicMatting": ["device_feature_api_is_media_editor_support_segment"],
    "EnableVideoEditor": ["device_feature_api_is_device_super_low_vlog_entrance",
                          "device_feature_api_is_device_support_vlog_entrance"],
    "EnableOcr": ["device_feature_api_is_support_local_ocr", "device_feature_api_is_aicr_ocr_support"],
    "ChangeBackupServer": ["base_build_util__BaseBuildUtil_is_global",
                           "base_build_util__BaseBuildUtil_is_international",
                           "google_sync__google_get_backup_status",
                           "google_sync__google_launch_backup_settings",
                           "google_sync__google_is_initialized"],
}


def render(report):
    lines = ["OS4 原生剩余适配审计（静态证据，不能作为实机验收）", "",
             "目标：小米 17 Pro Max / popsicle / OS4.0.0.44.XPBCNXM / Android 17 API 37。",
             "本轮没有新增生产 hook，也没有修改认证或版本拒绝策略。",
             "手机尚未连接：以下路径存在、符号命中、守卫通过均不表示 LSPosed 实际注入或长期稳定。", ""]
    for app in report["applications"]:
        lines += [f'{app["package"]}: {app["versionName"]} ({app["versionCode"]})',
                  f'  APK SHA256 {app["sha256"]}', f'  DEX 条目 {len(app["dex_files"])}；ARM64 原生库 {len(app["native_entries"])}']
    lines += ["", "范围与限制：", *["- " + x for x in report["limitations"]], "",
              "桌面 OS3 启用入口与 OS4 路径逐项对应（不是设置项总数）："]
    for row in report["home_legacy_features"]:
        keys = sorted(set(row["gate_preference_keys"] + row["implementation_preference_keys"]))
        lines += [f'- {row["hook"]}: {row["status"]}', "  " + row["assessment"],
                  "  设置键: " + (", ".join(keys) or "无直接 PrefsBridge 键；参考源文件"),
                  f'  {row["implementation"]["path"]}']
    for package, key in [("相册", "gallery_features"), ("天气", "weather_features")]:
        lines += ["", package + "："]
        for row in report[key]:
            lines += [f'- {row["hook"]}: {row["status"]}; {", ".join(row["gate_preference_keys"])}',
                      "  " + row["assessment"], "  源码: " + row["implementation"]["path"]]
            for target in row["native_candidates"]:
                lines += [f'  候选: {target["name"]} @ 0x{target["va"]:x} size=0x{target["size"]:x}']
    lines += ["", "原生 ABI 证据与本轮不实施原因："]
    for candidate in report["home_candidates"]:
        lines += [f'- {candidate["symbol"]}: ' + candidate["decision_reason"]]
        for body in candidate["bodies"]:
            lines += [f'  VA 0x{body["va"]:x}, size 0x{body["size"]:x}, body SHA256 {body["sha256"]}']
    lines += ["", "后续验收条件：", *["- " + x for x in report["next_requirements"]], "",
              "详细 JSON 含 SHA256、每项入口条件、源码引用、导出符号、协议边界与证据文件。"]
    return "\n".join(lines) + "\n"


def audit(project, objdump):
    out = project / 'acceptance'
    rom_root = test_paths.configured_path('HYPERCEILER_ROM_ROOT', project / 'firmware')
    evidence = out / "native-evidence"
    evidence.mkdir(parents=True, exist_ok=True)
    inventory = json.loads((rom_root / 'component-inventory.json').read_text())
    by_package = {x["package"]: x for x in inventory if "package" in x}
    applications, libraries, exports = [], [], {}
    app_specs = [("com.miui.home", "home", ["libapp.so", "libapp_launcher.so"]),
                 ("com.miui.gallery", "gallery", ["libapp.so", "libapp_gallery.so"]),
                 ("com.miui.weather2", "weather", ["libapp.so", "libweather_app.so"])]
    for package, tag, libs in app_specs:
        entry = by_package[package]
        apk = rom_root / entry["path"]
        actual_hash = sha(apk.read_bytes())
        if actual_hash != entry["sha256"]:
            raise ValueError(f"APK differs from verified partition inventory: {apk}")
        with zipfile.ZipFile(apk) as archive:
            names = archive.namelist()
            dex = sorted(x for x in names if re.fullmatch(r"classes\d*\.dex", x))
            if dex != entry["dex_files"]:
                raise ValueError(f"DEX inventory mismatch: {apk}")
            applications.append({**entry, "path": str(apk), "dex_files": dex,
                                 "native_entries": sorted(x for x in names if x.startswith("lib/") and x.endswith(".so"))})
            for lib in libs:
                archived = archive.read("lib/arm64-v8a/" + lib)
                target = (rom_root / 'launcher-native/lib/arm64-v8a' / lib
                          if tag == "home" else evidence / f"{tag}-{lib}")
                if tag != "home" and not target.exists():
                    target.write_bytes(archived)
                if sha(target.read_bytes()) != sha(archived):
                    raise ValueError(f"Extracted native file mismatch: {target}")
                meta, entries = image_info(target)
                meta["apk_entry"] = "lib/arm64-v8a/" + lib
                meta["package"] = package
                libraries.append(meta)
                exports[tag + ":" + lib] = entries
                (evidence / f"{tag}-{lib}.dynsym.txt").write_text("".join(
                    f'{x["va"]:x} {x["size"]:x} {"DEF" if x["defined"] else "UND"} {x["name"]}\n'
                    for x in entries))

    home = hooks(APP / "Home/os3/HomePhone.java")
    os4 = hooks(APP / "Home/os4/HomePhone.java")
    for row in home:
        status, note = HOME_REPLACEMENTS.get(row["hook"], (
            "not_wired_in_reviewed_os4_home_entry",
            "OS3 hook is not initialized by the OS4 HomePhone entry and has no mapped replacement in the reviewed Home native/provider path. Other system processes are outside this conclusion."))
        row.update(status=status, assessment=note)
    gallery = hooks(APP / "Gallery.java")
    gal_exports = [x for x in exports["gallery:libapp_gallery.so"] if x["defined"]]
    for row in gallery:
        fragments = GALLERY_CANDIDATES.get(row["hook"], [])
        matches = [x for x in gal_exports if any(part in x["name"] for part in fragments)]
        row.update(status="legacy_java_without_native_port", native_candidates=matches,
                   assessment="The supplied Gallery APK has no DEX, while this hook requires Java classes/methods. Current native injection is Home-only. Named FRB exports are investigation candidates, not proven equivalent hook points; bridge result layout and lifecycle must be decoded." if matches else
                   "The supplied Gallery APK has no DEX, while this hook requires Java classes/methods. No matching feature export was established in this audit; current native injection is Home-only.")
    weather = hooks(APP / "Weather.java")
    weather_exports = [x for x in exports["weather:libweather_app.so"] if x["defined"]]
    for row in weather:
        row.update(status="legacy_java_without_native_port", native_candidates=[],
                   assessment="The supplied Weather APK has no DEX. The legacy hook uses Java/DexKit targets, and current native injection is Home-only. Exported renderer APIs do not establish a corresponding preference/feature hook.")

    home_image = rom_root / 'launcher-native/lib/arm64-v8a/libapp.so'
    symbols = ELF.symbols(home_image)
    candidates = []
    candidate_specs = [
        ("ShortcutInfoModel.isNewInstalled", "Reads model flags, bit 2 at compressed field 0x6b; forcing false would alter model state queries beyond hiding the visual marker. No standard Dart frame prologue."),
        ("ShortCutCommonMixin.isHideTitle", "Two same-name leaf functions read different fields (0x5b and 0x97). Neither has the standard Dart frame. A name-only lookup is ambiguous; receiver layouts and desired surfaces are unresolved."),
        ("ShortCutCommonMixin.setIsShowNewInstallLight", "Writes bool field 0x8b and triggers reactive update. This is the install-light animation, not evidence that all shortcut and folder install indicators disappear."),
        ("ShortcutIconWidget._addNewInstallLight", "Builds/writes the widget child and conditionally wraps it after reading controller field 0x8b. Suppressing this light does not establish equivalence to the legacy title/folder indicator switch."),
        ("GridConfig.getTitleTextSize", "A named font-size getter is present, but caller units, scope (Dock/folder/drawer), return-object ownership and preference semantics are not proven."),
    ]
    for name, reason in candidate_specs:
        found = sorted({(va, size) for va, size, symbol in symbols if symbol == name})
        if not found:
            raise ValueError(f"Missing reviewed candidate: {name}")
        bodies = []
        for address, size in found:
            body = function_body(home_image, address, size)
            stem = name.replace(".", "_") + f"-{address:x}"
            body_path = evidence / (stem + ".words.txt")
            body_path.write_text("\n".join(f"{address + i:x}: {struct.unpack_from('<I', body, i)[0]:08x}"
                                          for i in range(0, len(body), 4)) + "\n")
            disasm_path = evidence / (stem + ".asm.txt")
            subprocess.run([str(objdump), "-D", "--no-show-raw-insn", f"--start-address={address}",
                            f"--stop-address={address + size}", str(home_image)], check=True,
                           stdout=disasm_path.open("w"))
            bodies.append({"va": address, "size": size, "sha256": sha(body),
                           "words": str(body_path), "disassembly": str(disasm_path)})
        candidates.append({"symbol": name, "decision": "defer_until_semantic_and_abi_evidence",
                           "decision_reason": reason, "bodies": bodies})

    # The non-leaf FRB bridge is deliberately disassembled, never replaced with
    # a guessed bool-return stub.
    hdr = [x for x in gal_exports if x["name"].endswith("device_feature_api_is_support_hdr_enhance")]
    if len(hdr) != 1:
        raise ValueError("Expected one Gallery HDR bridge export")
    hdr = hdr[0]
    gal_image = evidence / "gallery-libapp_gallery.so"
    hdr_body = function_body(gal_image, hdr["va"], hdr["size"])
    hdr_asm = evidence / "gallery-hdr-frb-bridge.asm.txt"
    subprocess.run([str(objdump), "-D", "--no-show-raw-insn", f'--start-address={hdr["va"]}',
                    f'--stop-address={hdr["va"] + hdr["size"]}', str(gal_image)],
                   check=True, stdout=hdr_asm.open("w"))
    snapshot = JAVA / "com/sevtinge/hyperceiler/libhook/provider/HomeLayoutPrefsSnapshot.java"
    endpoint = JAVA / "com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeLayoutNativeEndpointOS4.java"
    entry = ROOT / "app/src/main/cpp/targets/home/hyperceiler_home.cpp"
    protocol_keys = re.findall(r'\{"(boolean|integer|string)", "([^"]+)"\}', snapshot.read_text())
    report = {
        "schema": 1, "target": {"device": "Xiaomi 17 Pro Max", "codename": "popsicle",
                                "rom": "OS4.0.0.44.XPBCNXM", "android": 17, "api": 37},
        "evidence_level": "offline_source_and_actual_rom_binary_only", "device_verified": False,
        "production_change_this_audit": False,
        "applications": applications, "libraries": libraries,
        "counts": {"home_os3_initialized_hooks": len(home), "home_os4_initialized_hooks": len(os4),
                   "home_status_counts": dict(sorted(Counter(x["status"] for x in home).items())),
                   "gallery_initialized_hooks": len(gallery), "weather_initialized_hooks": len(weather)},
        "home_legacy_features": home, "home_os4_entry_hooks": os4,
        "gallery_features": gallery, "weather_features": weather,
        "weather_defined_exports": weather_exports,
        "home_candidates": candidates,
        "gallery_hdr_abi": {**hdr, "body_sha256": sha(hdr_body), "disassembly": str(hdr_asm),
                            "finding": "0x38c-byte FRB wire wrapper with stack allocation, state checks and bridge machinery; not a native leaf bool getter. Do not return x0=1 or reuse Dart x22 bool ABI."},
        "protocol_boundary": {"snapshot": source(snapshot), "endpoint": source(endpoint),
                              "native_entry": source(entry), "snapshot_schema": 1,
                              "snapshot_keys": [{"type": k, "key": v} for k, v in protocol_keys],
                              "authentication": "Endpoint requires application UID, positive PID, verified com.miui.home process and Binder interface token; retain all checks for future additions.",
                              "migration_rule": "Any new preference needs explicit snapshot/endpoint/native wire/cache handling, old-schema compatibility or rejection, missing-key safe default, bounds validation and unauthorized/partial/corrupt-packet rejection tests.",
                              "process_scope": "Current hyperceiler_home.cpp native specialization checks com.miui.home; no Gallery/Weather native initializer is established."},
        "os4_mapping_sources": [source(ROOT / path) for path in [
            "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/gesture/HomeGestureOS4.kt",
            "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/other/NativeHomeHooksOS4.kt",
            "library/core/src/main/java/com/sevtinge/hyperceiler/hooker/home/HomeTitleSettings.java",
            "app/src/main/cpp/targets/home/home_layout_config.cpp",
            "app/src/main/cpp/targets/home/home_layout_hooks.cpp",
        ]],
        "existing_validation": {
            "native_audit": str(project / "validation/native-os4-audit.txt"),
            "qualification": "Previously validated 7719 full-body guards and planner are static binding evidence only. The legacy diagnostic Dart aggregator/items resolver still fails this ROM and must not be hidden in a blanket 'all tests pass' claim."},
        "limitations": [
            "Zero classes*.dex entries establish that Java implementations are not inside these APKs; they do not prove that no external runtime class can ever be loaded. A matching external class and correct runtime ClassLoader would require separate evidence.",
            "71 Home rows are initialized OS3 phone hook families, not a count of all UI preferences. OS4-only hooks, shared framework hooks, disabled source comments and preference sub-options need separate accounting.",
            "A native path present means code is wired and some target binding was statically checked. It does not mean a preference takes effect, survives restart, or is stable under LSPosed.",
            "Gallery/Weather lack .symtab and .gnu_debugdata in the four reviewed libraries. Gallery .gnu_debuglink names companion DWARF files, which were not provided here. Dynamic export names alone do not establish FRB input/output ABI.",
            "Searchbar preferences remain in the snapshot for compatibility but are deliberately ignored by the OS4 endpoint; whitelist membership is not support evidence.",
        ],
        "next_requirements": [
            "Before another native feature patch: prove precise receiver layout, calling convention, return encoding/ownership, callers/inlined alternatives and the setting's intended scope; require exact known-image instruction guards.",
            "Gallery/Weather ports additionally require process initialization and authenticated preference transport; FRB dispatch/result structures must be decoded before changing any result.",
            "On the real phone: verify LSPosed scope and injected process, test each setting at default/changed/reset values, restart app/reboot, capture hook success/rejection/crash logs and repeat interaction/stability cases.",
        ],
    }
    (out / "native-remaining-audit.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    (out / "native-remaining-audit.txt").write_text(render(report))
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, default=test_paths.TEST_DATA_ROOT)
    parser.add_argument("--objdump", type=Path, required=True)
    args = parser.parse_args()
    report = audit(args.project.resolve(), args.objdump.resolve())
    print(json.dumps(report["counts"], ensure_ascii=False, indent=2))
    print("Wrote acceptance/native-remaining-audit.json and .txt; device_verified=false")


if __name__ == "__main__":
    main()
