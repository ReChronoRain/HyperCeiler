package androidx.core.content;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.List;

public final class ContextCompat {
    public static final int RECEIVER_EXPORTED = 0x2;
    public static final List<BroadcastReceiver> registered = new ArrayList<>();

    private ContextCompat() {
    }

    public static void registerReceiver(Context context, BroadcastReceiver receiver,
                                        IntentFilter filter, int flags) {
        registered.add(receiver);
    }
}
