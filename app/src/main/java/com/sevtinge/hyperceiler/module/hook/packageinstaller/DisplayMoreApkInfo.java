/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.packageinstaller;

import static com.sevtinge.hyperceiler.module.base.tool.OtherTool.getModuleRes;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

public class DisplayMoreApkInfo extends BaseHook {

    Class<?> mApkInfo;
    Class<?> mAppInfoViewObject;

    @Override
    public void init() {

        mApkInfo = findClassIfExists("com.miui.packageInstaller.model.ApkInfo");
        mAppInfoViewObject = findClassIfExists("com.miui.packageInstaller.ui.listcomponets.AppInfoViewObject");

        if (mAppInfoViewObject != null) {
            Class<?> ViewHolderClass = findClassIfExists("com.miui.packageInstaller.ui.listcomponets.AppInfoViewObject$ViewHolder");
            Method[] methods = XposedHelpers.findMethodsByExactParameters(mAppInfoViewObject, void.class, ViewHolderClass);
            if (methods.length == 0) {
                logI(TAG, this.lpparam.packageName, "Cannot find appropriate method");
                return;
            }

            Field[] fields = mAppInfoViewObject.getDeclaredFields();
            String apkInfoFieldName = null;
            for (Field field : fields)
                if (mApkInfo.isAssignableFrom(field.getType())) {
                    apkInfoFieldName = field.getName();
                    break;
                }
            if (apkInfoFieldName == null) return;
            String finalApkInfoFieldName = apkInfoFieldName;
            hookMethod(methods[0], new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    Object viewHolder = param.args[0];
                    if (viewHolder == null) return;
                    TextView tvAppSize = (TextView) XposedHelpers.callMethod(viewHolder, "getAppSize");
                    if (tvAppSize == null) return;

                    Object apkInfo = XposedHelpers.getObjectField(param.thisObject, finalApkInfoFieldName);
                    ApplicationInfo mAppInfo = (ApplicationInfo) XposedHelpers.callMethod(apkInfo, "getInstalledPackageInfo");
                    PackageInfo mPkgInfo = (PackageInfo) XposedHelpers.callMethod(apkInfo, "getPackageInfo");
                    Resources modRes = getModuleRes(tvAppSize.getContext());

                    LinearLayout layout = (LinearLayout) tvAppSize.getParent();
                    layout.removeAllViews();

                    ViewGroup mRootView = (ViewGroup) layout.getParent();
                    ImageView mRoundImageView = (ImageView) mRootView.getChildAt(0);
                    TextView mAppNameView = (TextView) mRootView.getChildAt(1);

                    mRootView.removeAllViews();

                    LinearLayout linearLayout = new LinearLayout(mRootView.getContext());
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    linearLayout.setGravity(Gravity.CENTER);
                    linearLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));

                    LinearLayout.LayoutParams AppNameViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    AppNameViewParams.setMargins(0, DisplayUtils.dp2px(10), 0, 0);
                    mAppNameView.setLayoutParams(AppNameViewParams);
                    mAppNameView.setGravity(Gravity.CENTER);

                    LinearLayout linearLayout2 = new LinearLayout(mRootView.getContext());
                    linearLayout2.setOrientation(LinearLayout.VERTICAL);
                    linearLayout2.setGravity(Gravity.CENTER);
                    linearLayout2.setPadding(0, DisplayUtils.dp2px(10), 0, 0);
                    linearLayout2.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));

                    TextView mAppVersionNameView = new TextView(mRootView.getContext());
                    TextView mAppVersionCodeView = new TextView(mRootView.getContext());
                    TextView mAppSdkView = new TextView(mRootView.getContext());
                    TextView mAppSizeView = new TextView(mRootView.getContext());

                    setTextAppearance(mAppVersionNameView, tvAppSize);
                    setTextAppearance(mAppVersionCodeView, tvAppSize);
                    setTextAppearance(mAppSdkView, tvAppSize);
                    setTextAppearance(mAppSizeView, tvAppSize);

                    mAppVersionNameView.setGravity(Gravity.CENTER);
                    mAppVersionCodeView.setGravity(Gravity.CENTER);
                    mAppSdkView.setGravity(Gravity.CENTER);
                    mAppSizeView.setGravity(Gravity.CENTER);


                    String mAppVersionName;
                    String mAppVersionCode;
                    String mAppSdk;
                    if (mAppInfo != null) {
                        mAppVersionName = XposedHelpers.callMethod(apkInfo, "getInstalledVersionName") + " ➟ " + mPkgInfo.versionName;
                        mAppVersionCode = XposedHelpers.callMethod(apkInfo, "getInstalledVersionCode") + " ➟ " + mPkgInfo.getLongVersionCode();
                        mAppSdk = mAppInfo.minSdkVersion + "-" + mAppInfo.targetSdkVersion + " ➟ " + mPkgInfo.applicationInfo.minSdkVersion + "-" + mPkgInfo.applicationInfo.targetSdkVersion;
                    } else {
                        mAppVersionName = mPkgInfo.versionName;
                        mAppVersionCode = String.valueOf(mPkgInfo.getLongVersionCode());
                        mAppSdk = mPkgInfo.applicationInfo.minSdkVersion + "-" + mPkgInfo.applicationInfo.targetSdkVersion;
                    }

                    mAppVersionNameView.setText(modRes.getString(R.string.various_install_app_info_version_name) + ": " + mAppVersionName);
                    mAppVersionCodeView.setText(modRes.getString(R.string.various_install_app_info_version_code) + ": " + mAppVersionCode);
                    mAppSdkView.setText(modRes.getString(R.string.various_install_app_info_sdk) + ": " + mAppSdk);
                    mAppSizeView.setText(tvAppSize.getText());

                    linearLayout2.addView(mAppVersionNameView, 0);
                    linearLayout2.addView(mAppVersionCodeView, 1);
                    linearLayout2.addView(mAppSdkView, 2);
                    linearLayout2.addView(mAppSizeView, 3);

                    linearLayout.addView(mRoundImageView, 0);
                    linearLayout.addView(mAppNameView, 1);
                    linearLayout.addView(linearLayout2, 2);
                    mRootView.addView(linearLayout);
                }
            });
        }
    }

    private void setTextAppearance(TextView textView, TextView textView2) {
        textView.setTextSize(15f);
        textView.setTextColor(textView2.getTextColors());
        textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        textView.setHorizontalFadingEdgeEnabled(true);
        textView.setSingleLine();
        textView.setMarqueeRepeatLimit(-1);
        textView.setSelected(true);
        textView.setHorizontallyScrolling(true);
    }
}
