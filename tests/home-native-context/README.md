# Launcher native SDK Context ownership

Run `python3 run.py /path/to/read-only-sdk-samples` from this directory.
Inputs are `libhyper_os_app_public.so` and `libhyper_os_app_private.dylib.so`.
The test compiles the production decoder under ASan/UBSan, then repeats it with
private Rust linkage renamed and GNU hashes rebuilt. It verifies an actual
borrowed Runtime Context return and scalar UID/PID consumers, including relocated
fields and rejection of changed returns, stores, control flow and widths.

The experimental launcher integration observes public Runtime borrow/retain/release
calls through the caller's own GOT. It retains one SDK reference using the SDK's
public retain API, not a guessed Arc header or a Dart pointer. Only a maintenance
worker checks the SDK Context's UID/PID; callbacks do not make Binder/Provider
requests or read properties. Default WMS mode installs none of these observers.
A loader availability notification bypasses an earlier missing-library delay.

Installation alone is not evidence of Context availability. The first device
build installed the GOT observer but did not capture a Context; subsequent
retain/release observers and loader timing changes need independent device
verification. This is bootstrap infrastructure, not a completed feature migration.

A later device run captured the Runtime and matched UID/PID, but exposed an
incorrect bootstrap constructor input level. Capture alone is insufficient.
The new observer additionally checks a stock `content_resolver_new` call's
handle reference against the retained Runtime, then permits bootstrap. The
constructor observer forwards the original call, does not allocate or do IPC,
and keeps only a boolean confirmation. The updated four-library integration
has not yet been accepted on device.
