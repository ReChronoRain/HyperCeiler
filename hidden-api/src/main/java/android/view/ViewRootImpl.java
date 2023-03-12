package android.view;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.android.internal.graphics.drawable.BackgroundBlurDrawable;

public class ViewRootImpl {
    @RequiresApi(Build.VERSION_CODES.S)
    public BackgroundBlurDrawable createBackgroundBlurDrawable() {
        throw new RuntimeException("Stub!");
    }
}