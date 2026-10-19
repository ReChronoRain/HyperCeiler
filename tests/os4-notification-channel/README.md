# Notification channel redirect callback checks

This compiles the actual production `RedirectToNotificationChannelSetting.kt`
and invokes the callbacks registered by its `init()`. No production callback
logic is duplicated in the test fixture. Android UI/Intent classes, field/method
reflection, and hook registration are lightweight host stand-ins.

Separate JVMs cover the target OS4 menu shape (`createMenuViews(boolean)` with
no `onClickInfoItem`) and the legacy shape with the optional capture callback.
The supplied SystemUI DEX inventory confirms the OS4 method shape and an existing
`NotificationSettingsHelper.startAppNotificationSettings(Context,String,String,int,String)`.

Checks verify that unscoped helper calls retain their result and launch no
activity, the direct icon listener still redirects the exact notification's
package/channel/conversation/UID, menu dismissal still runs, and legacy capture
and clearing continue to work. No new notification scope is intercepted.

```sh
python3 tests/os4-notification-channel/run.py \
  --kotlin-home /path/to/kotlinc --java /path/to/jdk/bin/java
```

Pass `--production /path/to/original-hook.kt` for the same checks on the baseline.
These are host callback checks; they do not replace LSPosed or device UI testing.
