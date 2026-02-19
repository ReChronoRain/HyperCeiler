package com.sevtinge.hyperceiler.about;

import android.content.Context;
import android.provider.MiuiSettings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.LargeFontUtils;

public class DeviceNameCard extends FrameLayout implements View.OnClickListener {

    private TextView mDeviceNameText;

    public DeviceNameCard(@NonNull Context context) {
        super(context);
        initView();
    }

    public DeviceNameCard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.app_device_info_item, this, true);
        ((TextView) findViewById(R.id.title)).setText(getResources().getString(R.string.device_name));
        mDeviceNameText = findViewById(R.id.summary);
        if (mDeviceNameText != null && LargeFontUtils.isLargeFontLevel(getContext())) {
            mDeviceNameText.setMaxLines(2);
            if (ViewCompat.getLayoutDirection(mDeviceNameText) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                mDeviceNameText.setGravity(8388613);
            } else {
                mDeviceNameText.setGravity(0);
            }
            mDeviceNameText.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        }
        refreshDeviceName();
        setOnClickListener(this);
    }

    public void refreshDeviceName() {
        String deviceName = MiuiSettings.System.getDeviceName(getContext());
        if (!TextUtils.isEmpty(deviceName) && mDeviceNameText != null) {
            mDeviceNameText.setText(deviceName);
        }
    }

    @Override
    public void onClick(View v) {

    }
}
