#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""Independent ELF symbol evidence for native launcher fixture tests."""

# Read .gnu_debugdata using XZ, independently of the production resolver.
# No fixed launcher addresses participate. Audit mode lists all function
# symbols, including Rust mangled names, for binary inspection.
import argparse
import lzma
import struct
from pathlib import Path


def sections(data):
    if data[:6] != b"\x7fELF\x02\x01":
        raise ValueError("Expected little-endian ELF64")
    shoff = struct.unpack_from("<Q", data, 40)[0]
    entry_size, count, names_index = struct.unpack_from("<HHH", data, 58)
    if entry_size != 64 or count == 0 or names_index >= count:
        raise ValueError("Unsupported or malformed section table")
    if shoff + entry_size * count > len(data):
        raise ValueError("Truncated section table")
    table = [struct.unpack_from("<IIQQQQIIQQ", data, shoff + i * entry_size)
             for i in range(count)]
    names = section_bytes(data, table[names_index])
    return table, {cstring(names, s[0]): s for s in table}


def section_bytes(data, section):
    offset, size = section[4:6]
    if offset > len(data) or size > len(data) - offset:
        raise ValueError("Truncated ELF section")
    return data[offset:offset + size]


def cstring(data, offset):
    if offset >= len(data):
        raise ValueError("String offset outside table")
    end = data.find(b"\0", offset)
    if end < 0:
        raise ValueError("Unterminated ELF string")
    return data[offset:end].decode("utf-8", errors="replace")


def symbols(path):
    data = Path(path).read_bytes()
    _, named = sections(data)
    debug = lzma.decompress(section_bytes(data, named[".gnu_debugdata"]), memlimit=1 << 30)
    table, named = sections(debug)
    symtab = named[".symtab"]
    if symtab[9] != 24 or symtab[6] >= len(table):
        raise ValueError("Malformed symbol table")
    strings = section_bytes(debug, table[symtab[6]])
    result = []
    for name, info, _other, section, value, size in struct.iter_unpack(
            "<IBBHQQ", section_bytes(debug, symtab)):
        if (info & 15) == 2 and section != 0 and value and name:
            result.append((value, size, cstring(strings, name)))
    return result


def unique(entries, *parts, exact=False):
    found = {va for va, _size, name in entries
             if (name == parts[0] if exact else all(part in name for part in parts))}
    if len(found) != 1:
        raise ValueError(f"Need one symbol matching {parts}, got {len(found)}")
    return found.pop()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("image")
    parser.add_argument("--kind", choices=("rust", "dart", "all"), default="all")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    entries = symbols(args.image)
    if args.kind == "all":
        output = "".join(f"{va:x} {size:x} {name}\n" for va, size, name in sorted(entries))
    else:
        if args.kind == "rust":
            wanted = {
                "grid_config_initializer": ("_RNC", "call_once_force", "GridConfig", "instance"),
                "grid_height": ("PhoneDeviceRules", "get_grid_height_with_search_and_indicator"),
                "cell_count_x": ("DeviceConfigs", "get_cell_count_x"),
                "cell_count_y": ("DeviceConfigs", "get_cell_count_y"),
                "resolve_dimen_px": ("device_utils", "resolve_dimen_px"),
            }
        else:
            wanted = {
                "margin": ("GridController.hotSeatsMarginBottom",),
                "margin_view": ("HotSeatsConstants2.hotSeatsMarginBottom",),
                "height": ("HotSeatsConstants2.hotSeatsHeight",),
            }
        output = "".join(f"{label} {unique(entries, *parts, exact=args.kind == 'dart'):x}\n"
                         for label, parts in wanted.items())
        if args.kind == "dart":
            aliases = {va for va, _size, name in entries
                       if name == "GridController.hotSeatsHeight"}
            if len(aliases) == 1:
                output += f"height_alt {aliases.pop():x}\n"
    if args.output:
        args.output.write_text(output, encoding="utf-8")
    else:
        print(output, end="")


if __name__ == "__main__":
    main()
