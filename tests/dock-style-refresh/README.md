# Dock unlock style refresh regression

Run `python tests/dock-style-refresh/run.py [source-root]` with JDK 25 at
`D:/build-tools/jdk-25.0.4.1+1`. An optional root lets the same tests inspect a
pre-fix or rollback fixture. The harness compiles the real PrefsBridge with
minimal Android stubs; it does not replace its write/notification logic.

Coverage: physical writes/removals when remote prefs are absent, writable,
commit-failing or read-only; no notification on a failed physical commit;
hook processes remain read-only; full provider key/URI; first boot at uptime 0;
30-second fallback; forced launcher/style reads; burst coalescing; 1,000
concurrent events during a read; one follow-up read; failure latch release.
Source guards cover context bootstrap, independent provider observer and cleanup,
new WindowState forced refresh, and the disabled-dock no-per-frame-query case.

The existing IPC HandlerThread performs all style queries. No new timer,
HandlerThread, service, wake lock or native/Dart patch is introduced. A fresh
provider value is applied on the next WMS traversal. The 30-second interval is
only a fallback for ordinary traversals; notifications and new launcher windows
bypass it. Removing a Dock layer does not reset window identity, since disabled
Dock traversals routinely execute that path.

Device acceptance after loading the new system_server hook: change style, restart
launcher immediately (within 30 seconds), check `reveal style queried=...` and
`prefs applied ... reveal=...`, then lock/unlock and visually check the selected
curve. Host tests and compilation do not establish that device result.
