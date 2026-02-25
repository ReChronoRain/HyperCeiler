package com.sevtinge.hyperceiler.about;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.AboutPhoneUtils;

public class VersionNameCard extends FrameLayout implements View.OnClickListener {

    private TextView mValue;

    public VersionNameCard(@NonNull Context context) {
        super(context);
        initView();
    }

    public VersionNameCard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.app_device_info_item, this, true);
        TextView textView = findViewById(R.id.title);
        mValue = findViewById(R.id.summary);
        ImageView imageView = findViewById(R.id.arrow_right);
        if (textView != null) {
            textView.setText(getContext().getResources().getString(R.string.app_device_version_parameters));
        }
        setVersionCode();
        if (imageView != null) {
            imageView.setVisibility(View.GONE);
        }
        setOnClickListener(this);
    }

    public void setVersionCode() {
        if (mValue != null) {
            mValue.setText(AboutPhoneUtils.addVersionSuffix(getContext()));
            mValue.setGravity(0);
        }
    }


    @Override
    public void onClick(View v) {

    }
}
