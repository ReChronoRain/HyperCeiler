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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.home.folder;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class BigFolderItemMaxCount extends BaseHook {

    Class<?> mFolderIcon2x2;
    Class<?> mFolderIcon2x2_9;
    Class<?> mFolderIcon2x2_4;
    Class<?> mBaseFolderIconPreviewContainer2X2;

    int mRealPvChildCount;

    @Override
    public void init() {
        mFolderIcon2x2 = findClassIfExists("com.miui.home.launcher.folder.FolderIcon2x2");
        mFolderIcon2x2_9 = findClassIfExists("com.miui.home.launcher.folder.FolderIconPreviewContainer2X2_9");
        mFolderIcon2x2_4 = findClassIfExists("com.miui.home.launcher.folder.FolderIconPreviewContainer2X2_4");
        mBaseFolderIconPreviewContainer2X2 = findClassIfExists("com.miui.home.launcher.folder.BaseFolderIconPreviewContainer2X2");

        findAndHookMethod(mFolderIcon2x2_9, "preSetup2x2", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {

                mRealPvChildCount = (int) XposedHelpers.callMethod(param.thisObject, "getMRealPvChildCount");
                logI(TAG, BigFolderItemMaxCount.this.lpparam.packageName, "getMRealPvChildCount1：" + mRealPvChildCount);

                if (mRealPvChildCount < 10) {
                    XposedHelpers.callMethod(param.thisObject, "setMItemsMaxCount", 9);
                    XposedHelpers.callMethod(param.thisObject, "setMLargeIconNum", 9);
                } else {
                    XposedHelpers.callMethod(param.thisObject, "setMItemsMaxCount", 12);
                    XposedHelpers.callMethod(param.thisObject, "setMLargeIconNum", 8);
                }
            }
        });

        findAndHookMethod(mFolderIcon2x2_4, "preSetup2x2", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {

                mRealPvChildCount = (int) XposedHelpers.callMethod(param.thisObject, "getMRealPvChildCount");
                logI(TAG, BigFolderItemMaxCount.this.lpparam.packageName, "getMRealPvChildCount1：" + mRealPvChildCount);

                if (mRealPvChildCount < 5) {
                    XposedHelpers.callMethod(param.thisObject, "setMItemsMaxCount", 4);
                    XposedHelpers.callMethod(param.thisObject, "setMLargeIconNum", 4);
                } else {
                    XposedHelpers.callMethod(param.thisObject, "setMItemsMaxCount", 7);
                    XposedHelpers.callMethod(param.thisObject, "setMLargeIconNum", 3);
                }
            }
        });
    }
}
