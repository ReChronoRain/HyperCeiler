package com.sevtinge.cemiuiler.module.screenshot

import android.os.Handler
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.Helpers
import de.robv.android.xposed.XposedHelpers


object ScreenshotFloatTime : BaseHook() {
    override fun init() {
        Helpers.findAndHookMethod(
            "com.miui.screenshot.GlobalScreenshot", lpparam.classLoader,
            "startGotoThumbnailAnimation",
            Runnable::class.java,
            object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    var mIsShowLongScreenShotGuide = false
                    try {
                        mIsShowLongScreenShotGuide =
                            XposedHelpers.getBooleanField(param.thisObject, "mIsShowLongScreenShotGuide")
                    } catch (ignore: Throwable) {
                    }
                    if (mIsShowLongScreenShotGuide) return
                    val opt: Int = mPrefsMap.getInt("screenshot_float_time", 0)
                    if (opt <= 0) return
                    val mHandler: Handler =
                        XposedHelpers.getObjectField(param.thisObject, "mHandler") as Handler
                    val mQuitThumbnailRunnable = XposedHelpers.getObjectField(
                        param.thisObject,
                        "mQuitThumbnailRunnable"
                    ) as Runnable
                    mHandler.removeCallbacks(mQuitThumbnailRunnable)
                    mHandler.postDelayed(mQuitThumbnailRunnable, opt * 1000L)
                }
            })
    }

}