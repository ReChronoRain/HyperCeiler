# Notification expansion regression checks

These checks compile the **actual** `AutoDismissExpandedPopupsHook.kt` production
file and invoke the callbacks registered by `init()` on reflection-selected
methods. The original callback logic is never copied into a test implementation.

The small stand-ins provide only Android's delayed runnable queue, preference
lookup, method/field reflection, and hook registration. Fixture presenter methods
represent both the older `(NotificationEntry, boolean)` signature and the ROM's
OS4 `(ExpandableNotificationRow, EntryAdapter, boolean)` signature; the adapter
offers `getEntry()` and `getKey()` and is never a Boolean.

Checks cover both signatures, adapter-specific keys, lockscreen, pin state,
active reply input, missing entries/runnables, unexpected arguments, legacy
field names, and actual queued runnable execution/delays.

Run on Linux with a JDK and Kotlin 2.4.10:

```sh
python3 tests/os4-notification-expansion/run.py \
  --kotlin-home /path/to/kotlinc --java /path/to/jdk/bin/java
```

For a baseline comparison, pass `--production /path/to/original-hook.kt` to the
same runner. These are host-side callback checks; they do not simulate LSPosed,
SystemUI lifecycle, or touchscreen behavior and do not replace device testing.
