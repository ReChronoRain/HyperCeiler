# HYOS inherited mapping inventory

```sh
python3 tests/hyos-image-inventory/run.py
python3 tests/hyos-image-inventory/run.py --launcher-apk /path/to/MiuiHome.apk
```

Runs the production `image.cpp` discovery and shared image/safe-read inventory parsers.
Only `/proc/self/maps`, linker enumeration and live header reads are host boundaries;
ZIP parsing, file identity checks, ELF decoding, ownership, coalescing and load-bias
validation use production code and real files. No Android SDK or Python packages are needed.

The 22 checks at each of 4 KB and 16 KB cover 130 split VMAs, 81 candidate APKs,
20,000 inherited rows exceeding 2 MB, consistent fallback-read budgets, complete
inventory refusal on overflow/truncation, ambiguous live images, RWX and missing
pages, source identity/load-address changes, deleted containers, live-header read
faults and preservation of the relevant failure among preloaded entries.
Optional launcher tests split every actual
Dart/Rust load into pages and verify its original source identity and bias.

The old raw 64-VMA and 64-APK limits reject valid HYOS inventories before any
layout hook can bind. Raw inventories now have separate finite budgets (8 MB,
65,536 rows, 1,024 candidate APKs). The existing final segment and immutable code
run budgets, generation checks and full live-header proof remain in force.

Discovery diagnostics identify the rejected stage and inventory/candidate/mapping
counts. Layout logs also distinguish ZIP/read/load-bias/executable-ownership
failures. They are emitted on stage transitions to avoid steady-state log spam.

Third-party device logs confirmed a live config endpoint and `delta=-15`, with
all layout knobs unbound despite the AOT mapping being present. Those logs do not
contain the full mapping inventory, so the exact guard that rejected that device
still requires validation with the corrected build.
