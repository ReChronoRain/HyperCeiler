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
package com.sevtinge.hyperceiler.model.data;

import android.app.ApplicationErrorReport.CrashInfo;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class AppCrashInfo implements Parcelable {

    /**
     * The name of the exception handler that is installed.
     * @hide
     */
    public String exceptionHandlerClassName;

    /**
     * Class name of the exception that caused the crash.
     */
    public String exceptionClassName;

    /**
     * Message stored in the exception.
     */
    public String exceptionMessage;

    /**
     * File which the exception was thrown from.
     */
    public String throwFileName;

    /**
     * Class which the exception was thrown from.
     */
    public String throwClassName;

    /**
     * Method which the exception was thrown from.
     */
    public String throwMethodName;

    /**
     * Line number the exception was thrown from.
     */
    public int throwLineNumber;

    /**
     * Stack trace.
     */
    public String stackTrace;

    /**
     * Crash tag for some context.
     * @hide
     */
    public String crashTag;

    public static final Creator<AppCrashInfo> CREATOR = new Creator<>() {
        @Override
        public AppCrashInfo createFromParcel(Parcel in) {
            return new AppCrashInfo(in);
        }

        @Override
        public AppCrashInfo[] newArray(int size) {
            return new AppCrashInfo[size];
        }
    };

    public AppCrashInfo() {}

    public AppCrashInfo(CrashInfo info) {
        exceptionMessage = info.exceptionMessage;
        throwMethodName = info.throwMethodName;
        throwFileName = info.throwFileName;
        exceptionClassName = info.exceptionClassName;
        throwClassName = info.throwClassName;
        throwLineNumber = info.throwLineNumber;
        stackTrace = info.stackTrace;
    }

    protected AppCrashInfo(Parcel in) {
        exceptionHandlerClassName = in.readString();
        exceptionClassName = in.readString();
        exceptionMessage = in.readString();
        throwFileName = in.readString();
        throwClassName = in.readString();
        throwMethodName = in.readString();
        throwLineNumber = in.readInt();
        stackTrace = in.readString();
        crashTag = in.readString();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(exceptionHandlerClassName);
        dest.writeString(exceptionClassName);
        dest.writeString(exceptionMessage);
        dest.writeString(throwFileName);
        dest.writeString(throwClassName);
        dest.writeString(throwMethodName);
        dest.writeInt(throwLineNumber);
        dest.writeString(stackTrace);
        dest.writeString(crashTag);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getExceptionHandlerClassName() {
        return exceptionHandlerClassName;
    }

    public void setExceptionHandlerClassName(String exceptionHandlerClassName) {
        this.exceptionHandlerClassName = exceptionHandlerClassName;
    }

    public String getExceptionClassName() {
        return exceptionClassName;
    }

    public void setExceptionClassName(String exceptionClassName) {
        this.exceptionClassName = exceptionClassName;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public String getThrowFileName() {
        return throwFileName;
    }

    public void setThrowFileName(String throwFileName) {
        this.throwFileName = throwFileName;
    }

    public String getThrowClassName() {
        return throwClassName;
    }

    public void setThrowClassName(String throwClassName) {
        this.throwClassName = throwClassName;
    }

    public String getThrowMethodName() {
        return throwMethodName;
    }

    public void setThrowMethodName(String throwMethodName) {
        this.throwMethodName = throwMethodName;
    }

    public int getThrowLineNumber() {
        return throwLineNumber;
    }

    public void setThrowLineNumber(int throwLineNumber) {
        this.throwLineNumber = throwLineNumber;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getCrashTag() {
        return crashTag;
    }

    public void setCrashTag(String crashTag) {
        this.crashTag = crashTag;
    }
}
