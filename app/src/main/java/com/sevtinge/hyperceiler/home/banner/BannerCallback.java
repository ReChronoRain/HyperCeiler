package com.sevtinge.hyperceiler.home.banner;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.dashboard.SubSettings;
import com.sevtinge.hyperceiler.home.helper.FrameworkWarningHelpFragment;
import com.sevtinge.hyperceiler.home.safemode.SafeModeFragment;
import com.sevtinge.hyperceiler.utils.SettingLauncherHelper;

public class BannerCallback implements View.OnClickListener {

    private static final String TAG = "BannerCallback";
    public static final String ACTION_OPEN_SAFE_MODE_SETTINGS =
        "com.sevtinge.hyperceiler.action.OPEN_SAFE_MODE_SETTINGS";
    public static final String ACTION_OPEN_FRAMEWORK_WARNING_HELP =
        "com.sevtinge.hyperceiler.action.OPEN_FRAMEWORK_WARNING_HELP";

    public BannerCallback() {}

    @Override
    public void onClick(View view) {
        Object tag = view.getTag();
        if (!(tag instanceof BannerBean)) return;

        BannerBean bean = (BannerBean) tag;
        Context context = view.getContext();
        if (handleUrl(context, bean)) {
            return;
        }
        handleAction(context, bean);
    }

    private boolean handleUrl(Context context, BannerBean bean) {
        String url = bean.getUrl();
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            return true;
        } catch (Exception e) {
            AndroidLog.e(TAG, "Failed to open URL: " + url, e);
            return false;
        }
    }

    private void handleAction(Context context, BannerBean bean) {
        String action = bean.getAction();
        if (TextUtils.isEmpty(action) || handleCustomAction(context, action)) {
            return;
        }

        try {
            context.startActivity(buildActionIntent(bean, action));
        } catch (Exception e) {
            AndroidLog.e(TAG, "Failed to handle action: " + action, e);
        }
    }

    private Intent buildActionIntent(BannerBean bean, String action) {
        Intent intent = new Intent(action);
        if (!TextUtils.isEmpty(bean.getPkg())) {
            intent.setPackage(bean.getPkg());
        }
        if (!TextUtils.isEmpty(bean.getExtras())) {
            intent.putExtra("banner_extras", bean.getExtras());
        }
        return intent;
    }

    private boolean handleCustomAction(Context context, String action) {
        if (ACTION_OPEN_SAFE_MODE_SETTINGS.equals(action)) {
            SettingLauncherHelper.onStartSettingsForArguments(
                context,
                SubSettings.class,
                SafeModeFragment.class.getName(),
                com.sevtinge.hyperceiler.core.R.string.settings_safe_mode
            );
            return true;
        }
        if (ACTION_OPEN_FRAMEWORK_WARNING_HELP.equals(action)) {
            SettingLauncherHelper.onStartSettingsForArguments(
                context,
                SubSettings.class,
                FrameworkWarningHelpFragment.class.getName(),
                R.string.help_framework_warning_title
            );
            return true;
        }
        return false;
    }

}
