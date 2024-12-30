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
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.data;

import android.content.pm.ApplicationInfo;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class ProcessInfo implements Parcelable {

    Object pkgList;
    int pkgListSize;
    int pid;
    int userId;
    ApplicationInfo appInfo;

    String processName;
    String packageName;
    boolean isLauncher;
    boolean isMainProcess;
    boolean isBackgroundProcess;
    boolean isRepeatingCrash;

    public ProcessInfo() {}

    protected ProcessInfo(Parcel in) {
        pkgListSize = in.readInt();
        pid = in.readInt();
        userId = in.readInt();
        appInfo = in.readParcelable(ApplicationInfo.class.getClassLoader());
        processName = in.readString();
        packageName = in.readString();
        isLauncher = in.readByte() != 0;
        isMainProcess = in.readByte() != 0;
        isBackgroundProcess = in.readByte() != 0;
        isRepeatingCrash = in.readByte() != 0;
    }

    public static final Creator<ProcessInfo> CREATOR = new Creator<ProcessInfo>() {
        @Override
        public ProcessInfo createFromParcel(Parcel in) {
            return new ProcessInfo(in);
        }

        @Override
        public ProcessInfo[] newArray(int size) {
            return new ProcessInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(pkgListSize);
        dest.writeInt(pid);
        dest.writeInt(userId);
        dest.writeParcelable(appInfo, flags);
        dest.writeString(processName);
        dest.writeString(packageName);
        dest.writeByte((byte) (isLauncher ? 1 : 0));
        dest.writeByte((byte) (isMainProcess ? 1 : 0));
        dest.writeByte((byte) (isBackgroundProcess ? 1 : 0));
        dest.writeByte((byte) (isRepeatingCrash ? 1 : 0));
    }

    public Object getPkgList() {
        return pkgList;
    }

    public void setPkgList(Object pkgList) {
        this.pkgList = pkgList;
    }

    public int getPkgListSize() {
        return pkgListSize;
    }

    public void setPkgListSize(int pkgListSize) {
        this.pkgListSize = pkgListSize;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public ApplicationInfo getAppInfo() {
        return appInfo;
    }

    public void setAppInfo(ApplicationInfo appInfo) {
        this.appInfo = appInfo;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public boolean isLauncher() {
        return isLauncher;
    }

    public void setLauncher(boolean launcher) {
        isLauncher = launcher;
    }

    public boolean isMainProcess() {
        return isMainProcess;
    }

    public void setMainProcess(boolean mainProcess) {
        isMainProcess = mainProcess;
    }

    public boolean isBackgroundProcess() {
        return isBackgroundProcess;
    }

    public void setBackgroundProcess(boolean backgroundProcess) {
        isBackgroundProcess = backgroundProcess;
    }

    public boolean isRepeatingCrash() {
        return isRepeatingCrash;
    }

    public void setRepeatingCrash(boolean repeatingCrash) {
        isRepeatingCrash = repeatingCrash;
    }
}
