package com.sevtinge.hyperceiler.safemode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class CrashHandlerReceiver extends BroadcastReceiver {

    private static CrashHandlerReceiver receiver = null;
    public static final String CRASH_HANDLER = "com.sevtinge.hyperceiler.CrashHandler";

    public static void register(Context context) {
        if (receiver == null) {
            receiver = new CrashHandlerReceiver();
            IntentFilter intentFilter = new IntentFilter(CRASH_HANDLER);
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED);
        }
    }

    public static void unregister(Context context) {
        if (receiver != null) {
            context.unregisterReceiver(receiver);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (CRASH_HANDLER.equals(intent.getAction())) {
            new CrashHandlerDialog(context, intent);
        }
    }
}
