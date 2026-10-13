OS4 Home Recents animation multiplier: component and joint reproduction

Run from the repository root, with a NEW output directory each time.
See tests/PORTABILITY.md for toolchain and external input configuration.

python3 -B tests/home-recents-os4/generate.py --check --output NEW_GUARD_DIR
python3 -B tests/home-recents-os4/run.py --output NEW_ARM_DIR
python3 -B tests/home-recents-os4/run_once.py --output NEW_ONCE_DIR
python3 -B tests/home-recents-os4/run_installer.py --output NEW_INSTALLER_DIR
python3 -B tests/home-recents-os4/run_joint.py --output NEW_JOINT_DIR

The generator reads the exact supplied ROM ELF, not guessed symbol offsets. It
binds 85 complete immutable spans and 58 relocated pointers, including all 53
Home init_array entries and the concrete Once implementation. Mutable Once3
and zero owner/observer are separate observations, never a reservation.

run.py executes the actual production policy/gateway and original ARM owner,
parser, five observer callbacks and Folme getter using QEMU. It preserves all
171 choices (30..200), including the OEM simple-mode path. Provider/String
drop/simple-device/FRB/Dart-heap services are named fixtures. The 8208 ABI
checks cover all GP registers, SP, NZCV, FPCR, FPSR and all 128 bits of each
SIMD register. This does not establish asynchronous unwind or an actual Dart
VM/Android process. Do not describe QEMU SVC113 as target seccomp admission.

run_once.py executes the actual original Once/get_or_init/argument-shim ARM
bytes on two threads. Only the initializer body and libc imports are adapted.
It witnesses cold3 -> running1 -> waiter5 -> complete0 and no duplicate init.
It is controlled scheduling evidence, not a proof about all vendor threads.

run_installer.py includes unchanged production recents_runtime.cpp. It maps
the real ROM as data, performs real process_vm_readv, mmap, mprotect, mlock,
smaps reads and aligned CAS. The template is first fully linked as ET_DYN,
has no remaining relocations, and equals the actually executed ARM template.
The host does not execute its ARM instructions. HCS/PinImage/HWCAP and fault
scheduling are explicit adapters. 40 fork-isolated cases include writer
contention before the phase CAS, rollback, foreign code, protection journal,
last-moment HCS revision, lifetime lock loss and permanent lease cancellation.

run_joint.py includes unchanged hyos_control.cpp and recents_runtime.cpp in
the same process, with the complete real HCS Cache/parser/config. Eleven
scenarios exercise initial HCS -> installation, same-value renewal, changes,
disable, gate denial, watchdog expiry while Binder is stuck, reentry and
publication during install. Binder/worker scheduling/clock, original image
pinning, ISA admission and other Home backends remain explicit adapters.
The old controller suite's Recents adapter is NOT this real joint suite.

Production scope:
  prefs_key_home_animation_recents_enable
  prefs_key_home_animation_recents_rate
The existing Home UI already requests a quick Home restart. The chosen value
is absolute percentage/100, not a multiplier on OEM's 0..1 value, and remains
separate from app-open/close animation rate. It is applied after the original
Settings read/normalization but before original shared owner publication.
No Settings.Global value is written. Original drops, observer registration,
owner stores and subsequent FRB/Dart cache flow remain in their original code.

Startup is one shot. The shared entry must authenticate Home and the actual
runtime call and hold its original entry until preparation returns. A second
entry cancels even while phase is still Cold but a writer has claimed it.
Original loader NOLOAD pinning holds the exact image; successful mlock plus
the actual VMA lo flag protects the one Rust patch page from proven Flutter
MADV_DONTNEED advice4. These mechanisms do not prove protection from arbitrary
munmap/mremap/munlock/DONTNEED_LOCKED/foreign native modifications.

Only the initial epoch and selected percentage may renew the <=5-second
BOOTTIME lease. Changed value, disable, denial, expiry or missed initialization
cannot reactivate in the same Home process; cold restart uses the new choice.
The raw owner/Dart caches are not instantaneously restored on cancellation.
Cancellation leaves the safe gateway installed, whose disabled path replays
the OEM instruction. Failed installation rolls back only its exact own word;
foreign changes or unrecovered page protection are Residue, never reported as
clean success. Arena, locked page and pinned image reference live until exit
because a restored code word does not prove absence of in-flight execution.

All actual-device, Android/Hyos injection, visible animation and long-term
stability statuses remain NOT_TESTED. The installer must succeed on the phone
under its actual mlock/SELinux/linker policy; the ROM's 64 KiB default limit
and static seccomp decisions do not establish those runtime observations.

Historical non-final runs are retained:
  components-v1: fixture compilation error; no passing report.
  installer-v1: a mutant removed the last use of a fixture function, -Werror.
  installer-v2: an incorrect chosen negative witness; top-level passed=false.
  installer-exploration-v1: early host exploration with ET_REL template data;
    it is not evidence of final linked ARM template correctness.
Final exact input hashes and results are recorded in their fresh report.json.
