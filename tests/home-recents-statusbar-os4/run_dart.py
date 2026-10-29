#!/usr/bin/env python3
# SPDX-License-Identifier: AGPL-3.0-or-later
"""Pure resolver/state checks and optional standalone device ABI probe, no launcher hook install."""
from pathlib import Path
import argparse,os,subprocess,tempfile
R=Path(__file__).resolve().parents[2];H=Path(__file__).parent
p=argparse.ArgumentParser();p.add_argument('--adb',action='store_true');p.add_argument('images',nargs='*');a=p.parse_args()
def run(args):subprocess.run(list(map(str,args)),check=True)
with tempfile.TemporaryDirectory(prefix='hc-recent-dart-')as tmp:
 out=Path(tmp);binary=out/'scene'
 run(['c++','-std=c++20','-O1','-g','-fsanitize=address,undefined','-I'+str(R/'app/src/main/cpp'),H/'dart_scene_test.cpp','-o',binary]);run([binary,*a.images])
 if a.adb:
  source=(R/'app/src/main/cpp/targets/home/home_layout_dart_arm64.S').read_text()
  def macro(name):
   start=source.index('.macro '+name+'\n');end=source.index('.endm',start)+len('.endm');return source[start:end]+'\n'
  start=source.index('.globl hc_recent_dart_state_entry');end=source.index('\n/* Workspace original double-tap',start)
  assembly='.text\n'+''.join(macro(n)for n in ['dart_save','dart_restore','workspace_native_save','workspace_native_restore'])+source[start:end]+'\n'
  # Native-ABI observer deliberately destroys caller-save state. The original
  # continuation records the values restored by the production assembly.
  assembly+='''.globl hc_recent_dart_state_observe
hc_recent_dart_state_observe:
 adrp x9, observed_arguments
 add x9, x9, :lo12:observed_arguments
 stp x0, x1, [x9]
 str x2, [x9, #16]
'''
  for i in range(19):assembly+=f' mov x{i}, #0xb00\n'
  for i in range(32):assembly+=f' movi v{i}.16b, #0xaa\n'
  assembly+=' mov x9, #0xf0000000\n msr nzcv, x9\n ret\n.globl scene_original\nscene_original:\n'
  for i in range(0,16,2):assembly+=f' stp x{i}, x{i+1}, [x19, #{i*8}]\n'
  for i in range(0,32,2):assembly+=f' stp q{i}, q{i+1}, [x19, #{128+i*16}]\n'
  assembly+=''' mrs x9, nzcv
 str x9, [x19, #640]
 mov x9, sp
 str x9, [x19, #648]
 ret
.globl scene_abi_call
scene_abi_call:
 sub sp, sp, #224
'''
  for i in range(19,31,2):assembly+=f' stp x{i}, x{i+1}, [sp, #{(i-19)*8}]\n'
  for i in range(8,16,2):assembly+=f' stp q{i}, q{i+1}, [sp, #{96+(i-8)*16}]\n'
  assembly+=''' mov x19, x4
 mov x29, x0
 mov x26, x1
 mov x27, x2
 mov x15, x3
 mov x9, sp
 str x9, [x19, #656]
'''
  for i in range(15):assembly+=f' mov x{i}, #{0x100+i}\n'
  for i in range(32):assembly+=f' movi v{i}.16b, #{0x40+i}\n'
  # Load NZCV through ABI scratch X16, leaving all tested X0-X15 intact.
  assembly+=' mov x16, #0x60000000\n msr nzcv, x16\n bl hc_recent_dart_state_entry\n'
  for i in range(8,16,2):assembly+=f' ldp q{i}, q{i+1}, [sp, #{96+(i-8)*16}]\n'
  for i in range(19,31,2):assembly+=f' ldp x{i}, x{i+1}, [sp, #{(i-19)*8}]\n'
  assembly+=' add sp, sp, #224\n ret\n'
  (out/'abi.S').write_text(assembly)
  sdk=Path(os.environ.get('ANDROID_HOME','/opt/homebrew/share/android-commandlinetools'))
  clang=sdk/'ndk/29.0.14206865/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android35-clang'
  target=out/'scene-abi';run([clang,'-O2',H/'dart_scene_abi.c',out/'abi.S','-o',target])
  remote='/data/local/tmp/hc-recents-scene-abi'
  try:
   run(['adb','push',target,remote]);run(['adb','shell','chmod','700',remote]);run(['adb','shell',remote])
  finally:run(['adb','shell','rm','-f',remote])
