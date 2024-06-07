package com.sevtinge.hyperceiler.ui.settings.notify;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getBaseOs;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getRomAuthor;

import android.content.Context;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.MainActivityContextHelper;
import com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt;

import java.util.Calendar;
import java.util.Objects;

public class SettingsNotifyHelper {

    public static boolean isBirthday() {
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        return currentMonth == Calendar.MAY && currentDay == 1;
    }

    public static boolean isOfficialRom() {
        return (!getBaseOs().startsWith("V") &&
                !getBaseOs().startsWith("Xiaomi") &&
                !getBaseOs().startsWith("Redmi") &&
                !getBaseOs().startsWith("POCO") &&
                !getBaseOs().isEmpty()) ||
                !getRomAuthor().isEmpty() ||
                Objects.equals(SystemSDKKt.getHost(), "xiaomi.eu") || (
                        !SystemSDKKt.getHost().startsWith("pangu-build-component-system") &&
                                !SystemSDKKt.getHost().startsWith("non-pangu-pod") &&
                                !Objects.equals(SystemSDKKt.getHost(), "xiaomi.com")
                );
    }

    public static boolean isSignPass(Context context) {
        MainActivityContextHelper helper = new MainActivityContextHelper(context);
        return !helper.isSignCheckPass();
    }
}
