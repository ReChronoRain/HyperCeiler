#!/usr/bin/env python3
"""Offline reader for the launcher's Dart AOT snapshot.

`libapp.so` is a stripped shared object plus a `.gnu_debugdata` mini-ELF that still carries every
Dart function's name, address and size. The released module resolves the same names through
`HomeTweaksFindSymbol` and reads the same instruction words through `dart_words`; this script does
those two steps on the host so a hook target can be chosen from evidence instead of from a device
log. Nothing here is used at run time - it is the "read the image first" half of the protocol.

Subcommands:
    sym <name>              name -> VA, size, and the enclosing symbol for context
    at  <hex-va>            which symbol owns this address
    dump <name|--va hex>    disassemble a function (Dart's register roles annotated)
    callers <name|--va hex> every `bl` in .text that targets the address, with its own owner
    consts                  field offsets read/written by a function, plus its pool loads
"""

import re
import struct
import sys

from capstone import Cs, CS_ARCH_ARM64, CS_MODE_LITTLE_ENDIAN, CS_OP_IMM, CS_OP_MEM, CS_OP_REG

# Dart AOT's register roles. x26 is the isolate thread, x27 the object pool, x28 the compressed
# heap base, x15 the Dart stack pointer; x16/x17 are the assembler's scratch registers.
DART_REGS = {"x15": "dsp", "x26": "thread", "x27": "pp", "x28": "heap", "x16": "ip0", "x17": "ip1"}


class Elf:
    def __init__(self, path):
        self.path = path
        with open(path, "rb") as handle:
            self.data = handle.read()
        if self.data[:4] != b"\x7fELF" or self.data[4] != 2 or self.data[5] != 1:
            raise SystemExit(f"{path}: not a little-endian ELF64")
        (self.machine,) = struct.unpack_from("<H", self.data, 0x12)
        (self.phoff,) = struct.unpack_from("<Q", self.data, 0x20)
        (self.shoff,) = struct.unpack_from("<Q", self.data, 0x28)
        (self.phentsize,) = struct.unpack_from("<H", self.data, 0x36)
        (self.phnum,) = struct.unpack_from("<H", self.data, 0x38)
        (self.shentsize,) = struct.unpack_from("<H", self.data, 0x3A)
        (self.shnum,) = struct.unpack_from("<H", self.data, 0x3C)
        (self.shstrndx,) = struct.unpack_from("<H", self.data, 0x3E)
        self.loads = []
        for index in range(self.phnum):
            base = self.phoff + index * self.phentsize
            fields = struct.unpack_from("<IIQQQQQQ", self.data, base)
            p_type, _flags, p_offset, p_vaddr = fields[0], fields[1], fields[2], fields[3]
            p_filesz = fields[5]
            if p_type == 1:  # PT_LOAD
                self.loads.append((p_vaddr, p_offset, p_filesz))
        self.sections = {}
        for index in range(self.shnum):
            base = self.shoff + index * self.shentsize
            self.sections[index] = struct.unpack_from("<IIQQQQIIQQ", self.data, base)
        shstr = self.sections[self.shstrndx]
        table = self.data[shstr[4]:shstr[4] + shstr[5]]
        self.section_names = {}
        for index in range(self.shnum):
            (name,) = struct.unpack_from("<I", self.data, self.shoff + index * self.shentsize)
            self.section_names[table[name:table.find(b"\0", name)].decode()] = index

    def file_offset(self, va):
        """VA -> file offset, the same PT_LOAD walk `dart_file_offset` performs on device."""
        for vaddr, offset, filesz in self.loads:
            if va >= vaddr and va - vaddr < filesz:
                return offset + (va - vaddr)
        return None

    def read(self, va, size):
        offset = self.file_offset(va)
        if offset is None:
            raise SystemExit(f"VA {va:#x} is not inside a file-backed PT_LOAD segment")
        return self.data[offset:offset + size]

    def executable_range(self):
        section = self.section_names.get(".text")
        _name, _type, _flags, addr, offset, size = self.sections[section][:6]
        return addr, offset, size


class Symbols:
    """Sorted (va, size, name) from a `.gnu_debugdata` mini-ELF or `llvm-nm` output."""

    def __init__(self, path):
        with open(path, "rb") as handle:
            head = handle.read(4)
        entries = []
        if head == b"\x7fELF":
            elf = Elf(path)
            index = elf.section_names.get(".symtab")
            if index is None:
                raise SystemExit(f"{path}: no .symtab")
            _, _, _, _, offset, size, link, _info, _align, _entsize = elf.sections[index]
            strtab = elf.sections[link]
            strings = elf.data[strtab[4]:strtab[4] + strtab[5]]
            for at in range(offset, offset + size, 24):
                name, _info, _other, _shndx, value, length = struct.unpack_from(
                    "<IBBHQQ", elf.data, at)
                if name == 0 or value == 0 or length == 0:
                    continue
                end = strings.find(b"\0", name)
                entries.append((value, length, strings[name:end].decode()))
        else:
            with open(path, "r", encoding="utf-8", errors="replace") as handle:
                for line in handle:
                    parts = line.split()
                    if len(parts) != 4:
                        continue
                    address, length, _kind, name = parts
                    try:
                        entries.append((int(address, 16), int(length, 16), name))
                    except ValueError:
                        continue
        entries.sort()
        self.entries = entries
        self.by_name = {name: (va, size) for va, size, name in entries}

    def enclosing(self, va):
        """The symbol whose [va, va+size) range covers the address, or None for a gap."""
        low, high = 0, len(self.entries)
        while low < high:
            middle = (low + high) // 2
            if self.entries[middle][0] <= va:
                low = middle + 1
            else:
                high = middle
        candidate = self.entries[low - 1] if low else None
        if candidate and candidate[0] <= va < candidate[0] + candidate[1]:
            return candidate[2], candidate[0]
        return None, None

    def near(self, va):
        """The two symbols bracketing an address, for sites that sit in an unsymbolised gap."""
        low, high = 0, len(self.entries)
        while low < high:
            middle = (low + high) // 2
            if self.entries[middle][0] <= va:
                low = middle + 1
            else:
                high = middle
        before = self.entries[low - 1] if low else None
        after = self.entries[low] if low < len(self.entries) else None
        return before, after

    def resolve(self, token):
        """`NAME`, `0xADDR` or a unique substring of a name -> (va, size, name)."""
        if re.fullmatch(r"0x[0-9a-fA-F]+", token):
            va = int(token, 16)
            name, owner = self.enclosing(va)
            size = dict((entry[0], entry[1]) for entry in self.entries).get(owner, 0)
            return va, size, name or f"<{va:#x}>"
        if token in self.by_name:
            return self.by_name[token][0], self.by_name[token][1], token
        matches = [name for name in self.by_name if token.lower() in name.lower()]
        if len(matches) == 1:
            return self.by_name[matches[0]][0], self.by_name[matches[0]][1], matches[0]
        if not matches:
            raise SystemExit(f"{token}: no symbol matches")
        raise SystemExit(f"{token}: {len(matches)} matches, e.g. " + ", ".join(sorted(matches)[:8]))


def annotate(instruction):
    notes = []
    for operand in instruction.operands:
        if operand.type == CS_OP_REG:
            role = DART_REGS.get(instruction.reg_name(operand.reg))
            if role:
                notes.append(f"{instruction.reg_name(operand.reg)}={role}")
        elif operand.type == CS_OP_MEM:
            role = DART_REGS.get(instruction.reg_name(operand.mem.base))
            if role:
                notes.append(f"[{instruction.reg_name(operand.mem.base)}={role}]")
    return ("  ; " + ", ".join(dict.fromkeys(notes))) if notes else ""


def engine():
    disassembler = Cs(CS_ARCH_ARM64, CS_MODE_LITTLE_ENDIAN)
    disassembler.detail = True
    return disassembler


def instructions(elf, va, size):
    return list(engine().disasm(elf.read(va, size), va))


def disassemble(elf, va, size, symbols=None):
    for instruction in instructions(elf, va, size):
        comment = annotate(instruction)
        target = ""
        if instruction.mnemonic in ("bl", "b") and instruction.operands \
                and instruction.operands[0].type == CS_OP_IMM:
            destination = instruction.operands[0].imm
            if symbols:
                name, _owner = symbols.enclosing(destination)
                target = f"  -> {name}" if name else ""
        print(f"  {instruction.address:08x}: {instruction.bytes.hex(' '):<18} "
              f"{instruction.mnemonic:<10} {instruction.op_str}{comment}{target}")


def callers(elf, symbols, va):
    """Every `bl` whose immediate target is `va`, named by the function it sits in."""
    found = list(branch_sites_to(elf, va))
    for site in found:
        owner, start = symbols.enclosing(site)
        inside = f" (+{site - start:#x})" if start is not None else ""
        print(f"  {site:#010x}  in {owner or '<unknown>'}{inside}")
    print(f"  {len(found)} call site(s)")
    return found


def call_targets(elf, symbols, va, size):
    """Every direct `bl`/`b` target inside a function, named."""
    targets = []
    for instruction in instructions(elf, va, size):
        if instruction.mnemonic not in ("bl", "b") or not instruction.operands:
            continue
        if instruction.operands[0].type != CS_OP_IMM:
            continue
        destination = instruction.operands[0].imm
        name, _owner = symbols.enclosing(destination)
        targets.append((destination, name))
    return targets


def evidence(elf, symbols, expect):
    """Assert the OS4 indicator dataflow the IndicatorMargin knob is wired to.

    Every claim in `kKnobHookSymbols` about who calls what is checked against the image itself, so a
    launcher build that reshapes the chain makes this exit non-zero instead of silently moving a
    different widget. `expect` carries the names; the addresses come from the symbol table.
    """
    failures = []

    def check(label, condition, detail):
        if condition:
            print(f"  ok   {label}: {detail}")
        else:
            print(f"  FAIL {label}: {detail}")
            failures.append(label)

    # The knob's own target: it must exist, start with the Dart prologue the inline hook needs, and
    # be reached only from the two indicator update paths.
    knob_va, knob_size, knob_name = symbols.resolve(expect["knob"])
    prologue = struct.unpack_from("<I", elf.read(knob_va, 4))[0]
    check("knob prologue", prologue == 0xA9BF79FD,
          f"{knob_name} first word {prologue:#x} (stp x29,x30,[x15,#-0x10]!)")
    callers = {owner or f"<{site:#x}>" for site, owner
               in ((site, symbols.enclosing(site)[0]) for site in branch_sites_to(elf, knob_va))}
    check("knob callers", callers == set(expect["knob_callers"]),
          ", ".join(sorted(callers)) or "<none>")

    # The knob must delegate to the GridController margin, which must in turn be edit-only.
    callees = {name or f"<{target:#x}>" for target, name
               in call_targets(elf, symbols, knob_va, knob_size)}
    check("knob callee", set(expect["knob_callees"]) <= callees,
          ", ".join(sorted(callees)) or "<none>")
    upstream_va, _size, upstream_name = symbols.resolve(expect["upstream"])
    upstream_callers = {owner or f"<{site:#x}>" for site, owner
               in ((site, symbols.enclosing(site)[0]) for site in branch_sites_to(elf, upstream_va))}
    check("upstream callers", upstream_callers == set(expect["upstream_callers"]),
          ", ".join(sorted(upstream_callers)) or "<none>")

    # The rejected targets must stay rejected: creation-time only. Their callers may sit in
    # unsymbolised gaps (closures), which the "<unsymbolised>" entry stands for.
    for label, symbol_key, allowed_key in (
        ("rejected", "rejected", "rejected_callers"),
        ("laptop twin", "laptop_twin", "laptop_twin_callers"),
    ):
        va, _size, resolved = symbols.resolve(expect[symbol_key])
        sites = {owner or "<unsymbolised>" for site, owner
                 in ((site, symbols.enclosing(site)[0]) for site in branch_sites_to(elf, va))}
        check(f"{label} callers", sites <= set(expect[allowed_key]),
              f"{resolved}: " + (", ".join(sorted(sites)) or "<none>"))
    return failures


def branch_sites_to(elf, va):
    """Every `bl` in .text whose target is `va` — the same scan `callers` prints."""
    base, offset, size = elf.executable_range()
    words = struct.unpack_from(f"<{size // 4}I", elf.data, offset)
    for index, word in enumerate(words):
        if word & 0xFC000000 != 0x94000000:
            continue
        immediate = word & 0x03FFFFFF
        if immediate & 0x02000000:
            immediate -= 0x04000000
        if base + index * 4 + (immediate << 2) == va:
            yield base + index * 4


def fields(elf, va, size):
    """Field offsets touched by the function: `ldur/stur` at Dart's odd (tag=1) field offsets."""
    code = elf.read(va, size)
    for instruction in engine().disasm(code, va):
        if instruction.mnemonic not in ("ldur", "stur", "ldrb", "strb"):
            continue
        for operand in instruction.operands:
            if operand.type != CS_OP_MEM or operand.mem.disp > 0x1000:
                continue
            base = instruction.reg_name(operand.mem.base)
            disp = operand.mem.disp
            tag = f"obj+{disp - 1:#x}" if disp & 1 else "aligned (not a heap field)"
            print(f"  {instruction.address:08x}: {instruction.mnemonic:<6} "
                  f"{instruction.op_str:<30} [{base}+{disp:#x}] {tag}")


# The OS4 indicator chain as read from launcher RELEASE-8.01.02.7722. The `evidence` subcommand
# re-derives it from whatever image it is handed, so an OTA that reshapes the dataflow fails loudly
# instead of leaving the knob pointed at a symbol that no longer means the same thing.
EVIDENCE = {
    "knob": "WorkspaceGetxController.indicatorOffsetBottomPortrait",
    "knob_callers": [
        "WorkspaceGetxController.startIndictorAnimation",
        "WorkspaceGetxController.onOrientationChanged",
    ],
    "knob_callees": ["GridController.workspaceEditIndicatorMarginBottom"],
    "upstream": "GridController.workspaceIndicatorMarginBottom",
    "upstream_callers": ["GridController.workspaceEditIndicatorMarginBottom"],
    "rejected": "WidgetLocationCalculator.calIndicatorCenterPosition",
    "rejected_callers": [
        "Workspace._createIndicator",
        "_UnlockWidgetState._resolveCellLocationInfo",
    ],
    "laptop_twin": "LaptopWorkspaceGetxController.calIndicatorCenterPosition",
    "laptop_twin_callers": ["<unsymbolised>"],
}


def main(argv):
    if len(argv) < 4:
        raise SystemExit(__doc__)
    elf = Elf(argv[1])
    symbols = Symbols(argv[2])
    command, argument = argv[3], argv[4] if len(argv) > 4 else None
    if command == "sym":
        va, size, name = symbols.resolve(argument)
        print(f"{name}  va={va:#x}  size={size:#x} ({size})")
    elif command == "at":
        name, start = symbols.enclosing(int(argument, 16))
        print(f"{name or '<unknown>'}  start={start if start is None else hex(start)}")
    elif command == "near":
        before, after = symbols.near(int(argument, 16))
        for label, entry in (("before", before), ("after", after)):
            print(f"  {label}: {entry[2]} {entry[0]:#x} +{entry[1]:#x}" if entry else f"  {label}: -")
    elif command == "dump":
        va, size, name = symbols.resolve(argument)
        print(f"# {name} va={va:#x} size={size:#x}")
        disassemble(elf, va, size, symbols)
    elif command == "callers":
        va, _size, name = symbols.resolve(argument)
        print(f"# call sites of {name} ({va:#x})")
        callers(elf, symbols, va)
    elif command == "fields":
        va, size, name = symbols.resolve(argument)
        print(f"# {name} va={va:#x} size={size:#x}")
        fields(elf, va, size)
    elif command == "evidence":
        print("# OS4 indicator dataflow, re-derived from the image:")
        failures = evidence(elf, symbols, EVIDENCE)
        if failures:
            raise SystemExit(f"{len(failures)} check(s) failed: " + ", ".join(failures))
        print("# all checks passed")
    else:
        raise SystemExit(__doc__)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
