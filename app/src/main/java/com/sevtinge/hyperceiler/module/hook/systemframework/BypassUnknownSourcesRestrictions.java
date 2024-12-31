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

package com.sevtinge.hyperceiler.module.hook.systemframework;

import static de.robv.android.xposed.XposedHelpers.setIntField;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class BypassUnknownSourcesRestrictions extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        String packageName = lpparam.packageName;
        ClassLoader classLoader = lpparam.classLoader;
        //注意：com.miui.packageinstaller是com.android.packageinstaller的改包名安装器，不是MIUI原版安装器
        //缘由：MIUI原版安装器上传应用信息并协同小米账号到反诈中心等，并且用其他安装器不认，故将原生的安装器直接改包名并使用核破解覆盖com.miui.packageinstaller
        /*if (packageName.equals("com.miui.packageinstaller") || packageName.equals("com.android.packageinstaller")){
            XposedHelpers.findAndHookMethod("com.android.packageinstaller.PackageInstallerActivity", classLoader, "onCreate", android.os.Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    //很简单，只需要在onCreate方法执行前把mAllowUnknownSources字段设置为true即可
                    XposedHelpers.setBooleanField(param.thisObject,"mAllowUnknownSources",true);
                    Context context;
                }
            });
        }*/
        if (packageName.equals("android")) {
            //Hook掉未知来源权限检查，一律返回有
            findAndHookMethod("com.android.server.appop.AppOpsService", classLoader, "noteOperation", int.class, int.class, java.lang.String.class, java.lang.String.class, boolean.class, java.lang.String.class, boolean.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    super.before(param);
                }
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    super.after(param);
                    if ((Integer) param.args[0] == 66){//AppOpsManager.permissionToOpCode("android.permission.REQUEST_INSTALL_PACKAGES") == 66
                        //XposedBridge.log("com.android.server.appop.AppOpsService.noteOperation("+Arrays.toString(param.args)+") return:"+param.getResult());
                        setIntField(param.getResult(),"mOpMode",0);
                    }
                }
            });
            //以免智障程序自己检查是否有这个权限，然后跑来申请
            findAndHookMethod("com.android.server.pm.PackageManagerService", classLoader, "canRequestPackageInstalls", java.lang.String.class, int.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    super.before(param);
                }

                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    super.after(param);
                    param.setResult(true);
                }
            });
        }
    }
}
