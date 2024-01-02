package com.sevtinge.hyperceiler.utils.blur.zhenxiang

import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewRootImpl
import androidx.annotation.RequiresApi
import com.android.internal.graphics.drawable.BackgroundBlurDrawable
import org.lsposed.hiddenapibypass.HiddenApiBypass

@RequiresApi(Build.VERSION_CODES.S)
fun View.createBackgroundBlurDrawable(): BackgroundBlurDrawable? {

    return try {
        val getViewRootImpl =
            HiddenApiBypass.invoke(View::class.java, this, "getViewRootImpl") as ViewRootImpl
        HiddenApiBypass.invoke(
            ViewRootImpl::class.java,
            getViewRootImpl,
            "createBackgroundBlurDrawable"
        ) as BackgroundBlurDrawable
    } catch (e: Exception) {
        Log.w(null, e)
        null
    }
}
