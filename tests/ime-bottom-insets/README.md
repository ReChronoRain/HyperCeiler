# MIUI keyboard duplicate bottom space

Run `python3 tests/ime-bottom-insets/run.py` to exercise the production filter
against lightweight Android fixtures. It checks that only the input-frame child
receives a copy with zero navigation-bar bottom insets; side insets, other inset
types, the original packet and the MIUI bottom area are preserved. Hidden/empty
bottom areas and input methods targeting SDK 34 or lower retain their insets.
It also rebinds to a new keyboard tree after service recreation and checks that
window-show inset requests target the current frame and stop after cleanup.

The integration hooks both `View` and `ViewGroup.dispatchApplyWindowInsets`,
including optimized group dispatch. It requests insets again
after the MIUI plugin's `onWindowShown`, since the initial bottom-view binding
can run before the input view is attached or the bottom buttons are visible.
The first visible global layout also requests insets; this covers a bottom area
that is still GONE at the initial bind/show. Repeated unchanged layouts do not
request another traversal, and rebinding/cleanup removes the old listener.

On the Android 17 test phone, WeType 4.0.0 targets SDK 35. Its input frame
contained a 941 px keyboard view with 876 px of keyboard content: the remaining
65 px matched the navigation-bar bottom inset. MIUI already reserved a separate
179 px bottom area for its buttons. Changing that area's margin moves the whole
keyboard but does not remove the duplicated gap, so this fix filters the keyboard
subtree's incoming insets instead. The hook is limited to selected third-party
input methods using the unlocked MIUI style on Android 15 or newer.

Release build and signature verification passed. The APK was installed with
`adb install --no-incremental -r` and the WeType `:hld` process restarted. The
installed SHA-256 matched `30c5846c63fe925f0f32c27321d9df0a96fb97e03726ccc0bf65a3ef4a7d8fbc`.
On-device logs reported `Removed duplicate keyboard navigation inset=65`.
The input frame changed from `[0,1410][1200,2351]` to
`[0,1475][1200,2351]`, matching its 876 px content. The MIUI bottom area and
both button bounds remained unchanged. A screenshot and a hide/show cycle
confirmed the gap reduction persisted. No phone reboot was needed.
