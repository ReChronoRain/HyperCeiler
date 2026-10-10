#!/system/bin/sh
# SPDX-License-Identifier: AGPL-3.0-or-later
# Sourced by the root manager's standard module installer.
[ "$BOOTMODE" = true ] || abort "Install through the root manager app; recovery installation is unsupported."
[ "$ARCH" = arm64 ] || abort "This experimental companion requires arm64."
[ "$API" = 37 ] || abort "This build targets Android 17 / API 37 only."
case "$(getprop ro.product.build.fingerprint)" in
  Xiaomi/popsicle/*) ;;
  *) abort "This companion is restricted to the supplied popsicle ROM." ;;
esac
[ "$(getprop ro.product.build.version.incremental)" = OS4.0.0.44.XPBCNXM ] || abort "ROM mismatch: OS4.0.0.44.XPBCNXM is required."
[ -f /system_ext/bin/hyos_spawner ] || abort "HyperOS Rust spawner is unavailable."
set_perm_recursive "$MODPATH" 0 0 0755 0644
ui_print "Experimental Home and Weather native companion installed."
ui_print "Requires matching HyperCeiler APK, LSPosed system scope, and Zygisk Next API 4 / Hyos API 1."
ui_print "No framework is bundled or enabled by this installer. Reboot using your normal root-manager workflow."
ui_print "Startup-only layouts and real-device stability have not passed device acceptance."
