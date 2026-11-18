package com.sevtinge.hyperceiler.libhook.provider;

import android.content.Context;
import android.os.Bundle;

// The Weather bridge tests use the real Provider but never call these unrelated routes.
final class XiaoheiziLayerHost {
    Bundle call(Context context, String method, String id, Bundle extras) { return null; }
}

final class HomeExtraGestureRegistry {
    Bundle call(Context context, String method, Bundle extras) { return null; }
}

final class HomePlatformContract {
    static Bundle query(Context context) { return null; }
}
