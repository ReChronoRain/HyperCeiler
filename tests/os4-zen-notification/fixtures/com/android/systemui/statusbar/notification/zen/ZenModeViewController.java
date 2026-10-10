package com.android.systemui.statusbar.notification.zen;
public final class ZenModeViewController { public boolean manuallyDismissed,visible;public int calls;public void updateVisibility(){calls++;visible=!manuallyDismissed;} }
