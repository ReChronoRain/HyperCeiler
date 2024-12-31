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

package com.sevtinge.hyperceiler.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class AppErrorsData implements Parcelable {

    AppCrashInfo crashInfo;
    ProcessInfo processInfo;
    boolean handleAppCrashInActivity;

    public static final Creator<AppErrorsData> CREATOR = new Creator<>() {
        @Override
        public AppErrorsData createFromParcel(Parcel in) {
            return new AppErrorsData(in);
        }

        @Override
        public AppErrorsData[] newArray(int size) {
            return new AppErrorsData[size];
        }
    };

    public AppErrorsData() {}

    public AppErrorsData(AppCrashInfo crashInfo, ProcessInfo processInfo, boolean handleAppCrashInActivity) {
        this.crashInfo = crashInfo;
        this.processInfo = processInfo;
        this.handleAppCrashInActivity = handleAppCrashInActivity;
    }

    protected AppErrorsData(Parcel in) {
        crashInfo = in.readParcelable(AppCrashInfo.class.getClassLoader());
        processInfo = in.readParcelable(ProcessInfo.class.getClassLoader());
        handleAppCrashInActivity = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(crashInfo, flags);
        dest.writeParcelable(processInfo, flags);
        dest.writeByte((byte) (handleAppCrashInActivity ? 1 : 0));
    }

    public AppCrashInfo getCrashInfo() {
        return crashInfo;
    }

    public void setCrashInfo(AppCrashInfo crashInfo) {
        this.crashInfo = crashInfo;
    }

    public ProcessInfo getProcessInfo() {
        return processInfo;
    }

    public void setProcessInfo(ProcessInfo appInfo) {
        this.processInfo = appInfo;
    }

    public boolean isHandleAppCrashInActivity() {
        return handleAppCrashInActivity;
    }

    public void setHandleAppCrashInActivity(boolean handleAppCrashInActivity) {
        this.handleAppCrashInActivity = handleAppCrashInActivity;
    }

    public void clean() {
        setCrashInfo(null);
        setProcessInfo(null);
    }
}
