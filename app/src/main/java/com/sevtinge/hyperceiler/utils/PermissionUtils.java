/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {

    //权限项数组
    public static final String[] PERMISSIONS = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.SYSTEM_ALERT_WINDOW
    };


    private static int mRequestCode = -1;

    private static OnPermissionListener mOnPermissionListener;

    /**
     * 权限请求回调
     */
    public interface OnPermissionListener {

        //权限通过
        void onPermissionGranted(Context context);

        //权限拒绝
        void onPermissionDenied();

    }

    /**
     * 请求响应的权限
     * @param context     Activity
     * @param requestCode 请求码
     * @param permissions 请求权限
     * @param listener    权限请求监听
     */
    public static void requestPermissions(Activity context, String[] permissions, int requestCode, OnPermissionListener listener) {
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }
        mOnPermissionListener = listener;
        List<String> deniedPermissions = new ArrayList<String>();
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(context, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permissions[i]);//添加未授予的权限
            }
        }

        if (deniedPermissions.size() > 0) {
            mRequestCode = requestCode;
            //其中请求码范围在[0,65535]
            ActivityCompat.requestPermissions(context, deniedPermissions.toArray(new String[deniedPermissions.size()]), requestCode);
        } else {
            mOnPermissionListener.onPermissionGranted(context);
        }
    }

    /**
     * 验证所有权限是否都已经授权
     */
    private static boolean verifyPermissions(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {//拒绝权限
                System.out.println("grantResult: "+grantResult);
                return false;
            }
        }
        return true;
    }

    /**
     * 请求权限结果，对应Activity中onRequestPermissionsResult()方法。
     */
    public static void onRequestPermissionsResult(Context context, int requestCode,
                                                  String[] permissions, int[] grantResults) {
        if (mRequestCode != -1 && requestCode == mRequestCode) {
            if (mOnPermissionListener != null) {
                if (verifyPermissions(grantResults)) {
                    mOnPermissionListener.onPermissionGranted(context);
                } else {
                    mOnPermissionListener.onPermissionDenied();
                }
            }
        }
    }
}

