package com.sevtinge.hyperceiler.ui.settings.tips;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.navigator.page.HomePageFragment;

import java.lang.ref.WeakReference;

public class TipsListener extends Handler implements View.OnClickListener {

    private final WeakReference<HomePageFragment> mRef;

    public TipsListener(HomePageFragment fragment) {
        mRef = new WeakReference<>(fragment);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        if (mRef != null) {
            HomePageFragment fragment = mRef.get();
            TipsLocalModel model = fragment.getTipsLocalModel();
            if (fragment != null && model != null) {
                if (!model.isEmpty()) {
                    model.update(fragment.getContext(), "show");
                }
                fragment.updateTips(
                        !model.isEmpty(),
                        model.getTitle(),
                        model.getSummary(),
                        model.getIcon(fragment.getContext()),
                        model.getArrowIcon(),
                        model.getTextColor(),
                        model.getBackground(),
                        this
                );
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (mRef != null && v != null) {
            HomePageFragment fragment = mRef.get();
            TipsLocalModel model = fragment.getTipsLocalModel();
            if (fragment != null && model != null) {
                if (v.getId() == R.id.arrow_right && model.getArrowIcon() == TipsUtils.getArrowIcon("cancel")) {
                    model.update(fragment.getContext(), "cancel");
                } else {
                    Intent intent = model.getIntent();
                    if (fragment.getActivity() != null && !fragment.getPackageManager().queryIntentActivities(intent, 65536).isEmpty()) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            fragment.startActivity(intent);
                        } catch (Exception e) {
                            Log.e("SettingsFragment", "startActivity error: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    if (model.isEmpty()) {
                        return;
                    } else {
                        model.update(fragment.getContext(), "click");
                    }
                }
                fragment.updateTips(
                        false,
                        model.getTitle(),
                        model.getSummary(),
                        model.getIcon(fragment.getContext()),
                        model.getArrowIcon(),
                        model.getTextColor(),
                        model.getBackground(),
                        null
                );
            }
        }
    }
}
