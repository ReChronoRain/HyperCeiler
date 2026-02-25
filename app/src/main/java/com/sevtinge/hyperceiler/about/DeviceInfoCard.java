package com.sevtinge.hyperceiler.about;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getSystemVersionIncremental;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.PropUtils.getProp;
import static com.sevtinge.hyperceiler.libhook.utils.api.PropUtils.getPropSu;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.AboutPhoneUtils;

import java.util.Objects;

public class DeviceInfoCard extends FrameLayout {

    private TextView mDeviceNameTitle;
    private TextView mDeviceInfoDeviceTitle, mDeviceInfoDeviceSummary;
    private TextView mDeviceInfoAndroidTitle, mDeviceInfoAndroidSummary;
    private TextView mDeviceInfoOSTitle, mDeviceInfoOSSummary;

    public DeviceInfoCard(@NonNull Context context) {
        this(context, null);
    }

    public DeviceInfoCard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.app_device_info_item2, this, true);
        mDeviceNameTitle = findViewById(R.id.device_name);
        mDeviceInfoDeviceTitle = findViewById(R.id.device_info_device_title);
        mDeviceInfoDeviceSummary = findViewById(R.id.device_info_device_summary);
        mDeviceInfoAndroidTitle = findViewById(R.id.device_info_android_title);
        mDeviceInfoAndroidSummary = findViewById(R.id.device_info_android_summary);
        mDeviceInfoOSTitle = findViewById(R.id.device_info_os_title);
        mDeviceInfoOSSummary = findViewById(R.id.device_info_os_summary);

       refreshDeviceInfo();
    }


    public void refreshDeviceInfo() {
        String deviceName;
        if (isMoreAndroidVersion(36)) {
            // 我就说我设备名字怎么就对不上了，这玩意还要 Root 获取，破烂
            deviceName = getPropSu("persist.private.device_name");
        } else {
            deviceName = getProp("persist.sys.device_name");
        }
        String marketName = getProp("ro.product.marketname");
        String androidVersion = getProp("ro.build.version.release");
        String osVersion = AboutPhoneUtils.addVersionSuffix(getContext());

        if (Objects.equals(marketName, "")) marketName = android.os.Build.MODEL;
        if (Objects.equals(deviceName, "")) deviceName = marketName;
        if (Objects.equals(osVersion, "")) osVersion = getSystemVersionIncremental();
        if (Objects.equals(osVersion, "")) osVersion = androidVersion;

        mDeviceNameTitle.setText(deviceName);
        mDeviceInfoDeviceTitle.setText(marketName);
        mDeviceInfoDeviceSummary.setText(com.sevtinge.hyperceiler.core.R.string.about_device_info_device);
        mDeviceInfoAndroidTitle.setText(androidVersion);
        mDeviceInfoAndroidSummary.setText(com.sevtinge.hyperceiler.core.R.string.about_device_info_android);
        mDeviceInfoOSTitle.setText(osVersion);
        mDeviceInfoOSSummary.setText(com.sevtinge.hyperceiler.core.R.string.about_device_info_os);
    }
}
