# Startup hook lifetime regression

Run `python3 tests/home-startup-lifetime/run.py` from the repository root on a
POSIX host with Python 3 and a C++20 compiler (`CXX` is optional).

The test compiles the production startup owner. It constructs a synthetic data
image from the checked-in guard/plan tables; no private firmware file, Android
SDK, external hook library or local tool cache is required. Fixture callbacks
model installation, rollback and authorization. No OEM instructions execute.

Five cases cover revocation before and immediately after activation, failed
installation, normal later revocation, and continued refusal of phone/Pad/Fold
grid features. A source-copy negative control removes the lifetime guard and
must compile, then fail the specific post-activation assertion.

After the epoch gate first opens, a caller may already be in flight. Revocation
closes the gate and restores owned constant words, but retains the detours and
their backend-owned continuations until process exit. Before activation, the
existing rollback behavior is preserved. The fixture checks that ownership
policy; it does not establish actual Android/Hyos backend safety, installation
ordering, Dart execution or device stability.
