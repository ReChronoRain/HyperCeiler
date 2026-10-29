#!/usr/bin/env python3
"""Execute the production closed-folder landing banks (NDK path as argv[1]).

The native callbacks are ABI-clobbering scalar mocks; run_state.py separately
tests the production bounded state. This does not simulate Dart GC or a phone UI.
"""
import itertools
import struct
import subprocess
import sys
import tempfile
from pathlib import Path

from capstone import Cs, CS_ARCH_ARM64, CS_MODE_LITTLE_ENDIAN
from unicorn import Uc, UC_ARCH_ARM64, UC_MODE_ARM, UC_HOOK_CODE
from unicorn import arm64_const as A

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT/'tests/home-layout-native'))
from dart_dump import Elf

NDK = Path(sys.argv[1])
CLANG = next((NDK/'toolchains/llvm/prebuilt').glob('*/bin/clang'))
TEXT, DATA, REPLAY, HEAP, DSP, FRAME, SP = (
    0x100000, 0x300000, 0x400000, 0x500000, 0x607000, 0x607800, 0x708000)
CAPTURE, TAKE, WIDTH_RESUME, HEIGHT_RESUME = 0x401000, 0x402000, 0x403000, 0x404000
FLAGS = 0xa0000000
NAMES = [
    'hc_folder_preview_caller', 'hc_folder_preview_width_field',
    'hc_folder_preview_height_field', 'hc_folder_preview_originals',
    'hc_folder_preview_boxed_field', 'hc_folder_preview_x_frame',
    'hc_folder_preview_y_frame', 'hc_folder_preview_right_frame',
    'hc_folder_preview_bottom_frame', 'hc_folder_preview_width_resume',
    'hc_folder_preview_height_resume',
]
TARGETS = {name: DATA+index*128 for index, name in enumerate(NAMES)}
TARGETS.update(hc_folder_preview_capture=CAPTURE, hc_folder_preview_take=TAKE)
ENTRIES = ['hc_folder_preview_capture_entry', 'hc_folder_preview_width_entry',
           'hc_folder_preview_height_entry']

def word(value):
    return struct.pack('<Q', value)

def double(value):
    return struct.pack('<d', value)

def bits(value):
    return struct.unpack('<Q', double(value))[0]

def xreg(index):
    return getattr(A, 'UC_ARM64_REG_X'+str(index))

def qreg(index):
    return getattr(A, 'UC_ARM64_REG_Q'+str(index))

def fvalue(uc, index):
    return struct.unpack('<d', word(uc.reg_read(qreg(index)) & ((1 << 64)-1)))[0]

with tempfile.TemporaryDirectory(prefix='hc-folder-preview-arm-') as directory:
    obj = Path(directory)/'banks.o'
    subprocess.run([str(CLANG), '--target=aarch64-linux-android35',
                    '-I'+str(ROOT/'app/src/main/cpp'), '-c',
                    str(ROOT/'app/src/main/cpp/targets/home/home_layout_dart_arm64.S'),
                    '-o', str(obj)], check=True)
    elf = Elf(obj)
    sym = elf.sections[elf.section_names['.symtab']]
    strings = elf.sections[sym[6]]
    strings = elf.data[strings[4]:strings[4]+strings[5]]
    symbols = []
    for at in range(sym[4], sym[4]+sym[5], 24):
        name, info, other, section, value, size = struct.unpack_from('<IBBHQQ', elf.data, at)
        symbols.append((strings[name:strings.find(b'\0', name)].decode(), section, value))
    text_index = elf.section_names['.text']
    section = elf.sections[text_index]
    code = bytearray(elf.data[section[4]:section[4]+section[5]])
    starts = {name: next(value for symbol, _, value in symbols if symbol == name)
              for name in ENTRIES}
    disassembler = Cs(CS_ARCH_ARM64, CS_MODE_LITTLE_ENDIAN)
    ranges = [(start, next(ins.address+4 for ins in disassembler.disasm(code[start:], start)
                           if ins.mnemonic == 'br')) for start in starts.values()]
    for section in elf.sections.values():
        if section[1] != 4 or section[7] != text_index:
            continue
        for at in range(section[4], section[4]+section[5], 24):
            offset, info, addend = struct.unpack_from('<QQq', elf.data, at)
            if not any(start <= offset < end for start, end in ranges):
                continue
            name, kind = symbols[info >> 32][0], info & 0xffffffff
            address = TARGETS[name]+addend
            instruction = struct.unpack_from('<I', code, offset)[0]
            if kind == 275:  # ADR_PREL_PG_HI21
                pages = (address >> 12)-((TEXT+offset) >> 12)
                instruction = ((instruction & ~((3 << 29) | (0x7ffff << 5)))
                               | ((pages & 3) << 29) | (((pages >> 2) & 0x7ffff) << 5))
            elif kind == 277:  # ADD_ABS_LO12_NC
                instruction = (instruction & ~(4095 << 10)) | ((address & 4095) << 10)
            elif kind == 286:  # LDST64_ABS_LO12_NC
                instruction = (instruction & ~(4095 << 10)) | (((address & 4095)//8) << 10)
            elif kind == 283:  # CALL26; only calls the native scalar mock.
                instruction = ((instruction & 0xfc000000)
                               | (((address-(TEXT+offset))//4) & 0x3ffffff))
            else:
                raise AssertionError((name, kind))
            struct.pack_into('<I', code, offset, instruction)
    entry = {name: TEXT+start for name, start in starts.items()}

    def put(uc, name, value, fmt='<Q'):
        uc.mem_write(TARGETS[name], struct.pack(fmt, value))

    def registers(uc):
        return ([uc.reg_read(xreg(i)) for i in range(31)],
                [uc.reg_read(qreg(i)) for i in range(32)])

    def initial(uc, lr, source):
        for i in range(31):
            uc.reg_write(xreg(i), 0x11000000+i*256)
        for i in range(32):
            uc.reg_write(qreg(i), ((i+1) << 96) | (0x12345678 << 32) | i)
        for index, value in {0: HEAP+257, 1: HEAP+513, 2: source,
                             15: DSP, 29: FRAME, 30: lr}.items():
            uc.reg_write(xreg(index), value)
        uc.reg_write(A.UC_ARM64_REG_SP, SP)
        uc.reg_write(A.UC_ARM64_REG_NZCV, FLAGS)

    def clobber(uc):
        # More severe than AAPCS64 requires: all SIMD including callee-saved
        # lanes are overwritten. The production gateway saves all 32 registers.
        for i in range(19):
            uc.reg_write(xreg(i), 0xabc00000+i)
        for i in range(32):
            uc.reg_write(qreg(i), (0xdef00000+i) << 64 | i)
        uc.reg_write(A.UC_ARM64_REG_NZCV, 0xf0000000)

    def machine(moved, matching, dimensions):
        uc = Uc(UC_ARCH_ARM64, UC_MODE_ARM)
        for address, size in [(TEXT, (len(code)+4095) & ~4095), (DATA, 4096),
                              (REPLAY, 65536), (HEAP, 4096),
                              (0x600000, 65536), (0x700000, 65536)]:
            uc.mem_map(address, size)
        uc.mem_write(TEXT, bytes(code))
        uc.mem_write(HEAP, b'\x5a'*4096)
        uc.mem_write(CAPTURE, struct.pack('<I', 0xd65f03c0))
        uc.mem_write(TAKE, struct.pack('<I', 0xd65f03c0))
        # Original calOriginPreviewIconLoc prologue from all three launchers.
        uc.mem_write(REPLAY, struct.pack('<4I', 0xa9bf79fd, 0xaa0f03fd,
                                        0xd100e1ef, 0xf81f83a1))
        metadata = {'hc_folder_preview_width_field': 7+moved,
                    'hc_folder_preview_height_field': 15+moved,
                    'hc_folder_preview_boxed_field': 7+moved,
                    'hc_folder_preview_x_frame': -40-moved,
                    'hc_folder_preview_y_frame': -32-moved,
                    'hc_folder_preview_right_frame': -56-moved,
                    'hc_folder_preview_bottom_frame': -48-moved}
        for name, value in metadata.items():
            put(uc, name, value, '<q')
        put(uc, 'hc_folder_preview_caller', 0x48a120)
        put(uc, 'hc_folder_preview_originals', REPLAY)
        put(uc, 'hc_folder_preview_width_resume', WIDTH_RESUME)
        put(uc, 'hc_folder_preview_height_resume', HEIGHT_RESUME)
        source = HEAP+1 if matching else 0xdead0001  # Unrelated LR must not dereference X2.
        if matching:
            uc.mem_write(source+7+moved, double(dimensions[0]))
            uc.mem_write(source+15+moved, double(dimensions[1]))
        initial(uc, 0x48a120 if matching else 0x48a124, source)
        calls, pending = [], {}

        def callback(uc, address, size, user):
            if address == CAPTURE:
                args = [uc.reg_read(xreg(i)) for i in range(3)]
                assert args == [FRAME, bits(dimensions[0]), bits(dimensions[1])], args
                assert not any(HEAP <= arg < HEAP+4096 for arg in args)
                calls.append(('capture', tuple(args)))
                pending[args[0]] = [args[1], args[2], False]
                clobber(uc)
            elif address == TAKE:
                frame, height = uc.reg_read(xreg(0)), uc.reg_read(xreg(1))
                assert frame == FRAME and height in [0, 1]
                calls.append(('take', (frame, height)))
                pair = pending.get(frame)
                result, applied = 0xfedcba9876543210, 0
                if pair:
                    if height:
                        if pair[2]:
                            result, applied = pair[1], 1
                        del pending[frame]
                    elif not pair[2]:
                        result, applied, pair[2] = pair[0], 1, True
                clobber(uc)
                uc.reg_write(xreg(0), result)
                uc.reg_write(xreg(1), applied)
        uc.hook_add(UC_HOOK_CODE, callback)
        return uc, metadata, source, calls, pending

    def unchanged(uc, old, changed_x=(), changed_q=()):
        old_x, old_q = old
        for i, value in enumerate(old_x):
            if i not in [16, 17] and i not in changed_x:
                assert uc.reg_read(xreg(i)) == value, ('X', i, hex(value), hex(uc.reg_read(xreg(i))))
        for i, value in enumerate(old_q):
            if i not in changed_q:
                assert uc.reg_read(qreg(i)) == value, ('Q', i, hex(value), hex(uc.reg_read(qreg(i))))
        assert uc.reg_read(A.UC_ARM64_REG_SP) == SP
        assert uc.reg_read(A.UC_ARM64_REG_NZCV) == FLAGS

    def scalar(uc, metadata, height, origin, expected_length, stock_length):
        source = HEAP+(513 if height else 257)
        uc.mem_write(source+metadata['hc_folder_preview_boxed_field'], double(stock_length))
        uc.reg_write(xreg(1 if height else 0), source)
        origin_name = 'hc_folder_preview_y_frame' if height else 'hc_folder_preview_x_frame'
        endpoint_name = 'hc_folder_preview_bottom_frame' if height else 'hc_folder_preview_right_frame'
        uc.mem_write(FRAME+metadata[origin_name], double(origin))
        before = registers(uc)
        name = ENTRIES[2 if height else 1]
        resume = HEIGHT_RESUME if height else WIDTH_RESUME
        uc.emu_start(entry[name], resume, count=2000)
        pos, result = (3, 4) if height else (1, 2)
        unchanged(uc, before, changed_q=[0, pos, result])
        assert uc.reg_read(qreg(0)) == bits(expected_length)
        assert uc.reg_read(qreg(pos)) == bits(origin)
        assert uc.reg_read(qreg(result)) == bits(origin+expected_length)
        endpoint = struct.unpack('<d', uc.mem_read(FRAME+metadata[endpoint_name], 8))[0]
        assert endpoint == origin+expected_length
        assert uc.reg_read(xreg(15)) == DSP and uc.reg_read(xreg(29)) == FRAME
        return endpoint

    geometries = list(itertools.product([48., 64., 96.], [48., 64., 80.],
                                       [.6, .7, 1., 1.25, 1.4], [0, 1]))
    assert len(geometries) == 90
    cases = reproduced = 0
    for base_width, base_height, factor, origin_kind in geometries:
        dimensions = base_width*factor, base_height*factor
        origins = ((-71.75+base_width/8, 14.125-base_height/16) if origin_kind
                   else (103.25+base_width/4, 231.5+base_height/8))
        for moved, matching in itertools.product([0, 16], [False, True]):
            uc, metadata, source, calls, pending = machine(moved, matching, dimensions)
            before = registers(uc)
            heap_before = bytes(uc.mem_read(HEAP, 4096))
            uc.emu_start(entry[ENTRIES[0]], REPLAY, count=2000)
            unchanged(uc, before)
            assert len(calls) == int(matching)
            assert bytes(uc.mem_read(HEAP, 4096)) == heap_before
            # Execute the real displaced four words, proving the capture did not
            # perturb its original Dart FP/DSP/receiver spill or native SP.
            old_x = before[0]
            uc.ctl_remove_cache(REPLAY, REPLAY+16)
            uc.emu_start(REPLAY, REPLAY+16, count=100)
            assert uc.reg_read(xreg(29)) == DSP-16, (hex(uc.reg_read(xreg(29))),hex(DSP-16),hex(uc.reg_read(A.UC_ARM64_REG_PC)))
            assert uc.reg_read(xreg(15)) == DSP-16-0x38, (hex(uc.reg_read(xreg(15))),hex(DSP-16-0x38))
            assert struct.unpack('<QQ', uc.mem_read(DSP-16, 16)) == (FRAME, old_x[30])
            assert struct.unpack('<Q', uc.mem_read(DSP-24, 8))[0] == old_x[1]
            unchanged(uc, before, changed_x=[15, 29])
            # The unmodified origin function returns to the original caller.
            # Recreate that live caller state without inventing/moving a Dart BL.
            initial(uc, old_x[30], source)
            # Execute the original fixture's two arithmetic banks first. Its
            # Rx square remains unscaled even though origin/painted preview
            # already use the selected strategy's new dimensions.
            old_words=[0xfc407000,0xfc5d83a1,0x1e602822,0xfc1c83a2,
                       0xfc407020,0xfc5e03a3,0x1e602864,0xfc1d03a4]
            for i in [0,1,3,4,5,7]:
                delta=moved if i in [0,4] else -moved
                off=((old_words[i]>>12)&511)
                old_words[i]=(old_words[i]&~(511<<12))|(((off+delta)&511)<<12)
            old_code=REPLAY+0x100
            uc.mem_write(old_code,struct.pack('<8I',*old_words))
            for index in [0,1]:uc.mem_write(HEAP+(257 if index==0 else 513)+7+moved,double(base_width))
            uc.mem_write(FRAME+metadata['hc_folder_preview_x_frame'],double(origins[0]))
            uc.mem_write(FRAME+metadata['hc_folder_preview_y_frame'],double(origins[1]))
            uc.emu_start(old_code,old_code+32,count=100)
            old_right=struct.unpack('<d',uc.mem_read(FRAME+metadata['hc_folder_preview_right_frame'],8))[0]
            old_bottom=struct.unpack('<d',uc.mem_read(FRAME+metadata['hc_folder_preview_bottom_frame'],8))[0]
            assert old_right==origins[0]+base_width and old_bottom==origins[1]+base_width
            if matching and factor!=1:
                assert abs(old_right-(origins[0]+dimensions[0]))>1
                reproduced+=1
            right = scalar(uc, metadata, False, origins[0], dimensions[0] if matching else base_width,base_width)
            bottom = scalar(uc, metadata, True, origins[1], dimensions[1] if matching else base_width,base_width)
            expected = dimensions if matching else (base_width, base_width)
            assert (right-origins[0], bottom-origins[1]) == (expected[0], expected[1]) or (
                abs(right-origins[0]-expected[0]) < 1e-12
                and abs(bottom-origins[1]-expected[1]) < 1e-12)
            assert not pending
            assert calls[-2:] == [('take', (FRAME, 0)), ('take', (FRAME, 1))]
            cases += 1

    # Missing capture/partial install also preserves stock when height is called
    # first. Applied=0 is accompanied by deliberately invalid bits in the mock.
    uc, metadata, source, calls, pending = machine(32, False, (80., 42.))
    scalar(uc, metadata, True, -3.5, 24.125,24.125)
    scalar(uc, metadata, False, 9.75, 24.125,24.125)
    assert not pending
    print(f'FOLDER_PREVIEW_RETURN_ARM PASS: {cases} complete lifecycles over {len(geometries)} geometries; {reproduced} original scaled-size failures reproduced; '
          'matching/unrelated LR, original prologue, relocated fields/frame slots, rectangular '
          'endpoints, stock fallback, full GPR/SIMD/flags/DSP/native-SP preservation; no Dart GC simulation')
