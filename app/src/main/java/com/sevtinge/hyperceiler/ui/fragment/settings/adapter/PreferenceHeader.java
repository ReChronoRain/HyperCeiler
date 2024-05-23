package com.sevtinge.hyperceiler.ui.fragment.settings.adapter;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

public class PreferenceHeader implements Parcelable {

    public static final Creator<PreferenceHeader> CREATOR = new Creator<PreferenceHeader>() {
        @Override
        public PreferenceHeader createFromParcel(Parcel in) {
            return new PreferenceHeader(in);
        }

        @Override
        public PreferenceHeader[] newArray(int size) {
            return new PreferenceHeader[size];
        }
    };

    public CharSequence breadCrumbShortTitle;
    public int breadCrumbShortTitleRes;
    public CharSequence breadCrumbTitle;
    public int breadCrumbTitleRes;
    public Bundle extras;
    public String fragment;
    public Bundle fragmentArguments;
    public int iconRes;
    public long id = -1;
    public Intent intent;
    public CharSequence key;
    public CharSequence summary;
    public int summaryRes;
    public CharSequence title;
    public int titleRes;

    public PreferenceHeader() {}

    public PreferenceHeader(Parcel in) {
        readFromParcel(in);
    }


    private CharSequence getText(Resources res, int resId) {
        return res.getText(resId);
    }

    public CharSequence getTitle(Resources res) {
        return titleRes != 0 ? getText(res, titleRes) : title;
    }

    public CharSequence getSummary(Resources res) {
        return summaryRes != 0 ? getText(res, summaryRes) : summary;
    }

    public CharSequence getBreadCrumbTitle(Resources res) {
        return breadCrumbTitleRes != 0 ? getText(res, breadCrumbTitleRes) : breadCrumbTitle;
    }

    public CharSequence getBreadCrumbShortTitle(Resources res) {
        return breadCrumbShortTitleRes != 0 ? getText(res, breadCrumbShortTitleRes) : breadCrumbShortTitle;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void readFromParcel(Parcel in) {
        id = in.readLong();
        titleRes = in.readInt();
        title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        summaryRes = in.readInt();
        summary = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        key = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        breadCrumbTitleRes = in.readInt();
        breadCrumbTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        breadCrumbShortTitleRes = in.readInt();
        breadCrumbShortTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        iconRes = in.readInt();
        fragment = in.readString();
        fragmentArguments = in.readBundle();
        if (in.readInt() != 0) {
            intent = Intent.CREATOR.createFromParcel(in);
        }
        extras = in.readBundle();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeInt(titleRes);
        TextUtils.writeToParcel(title, dest, flags);
        dest.writeInt(summaryRes);
        TextUtils.writeToParcel(summary, dest, flags);
        TextUtils.writeToParcel(key, dest, flags);
        dest.writeInt(breadCrumbTitleRes);
        TextUtils.writeToParcel(breadCrumbTitle, dest, flags);
        dest.writeInt(breadCrumbShortTitleRes);
        TextUtils.writeToParcel(breadCrumbShortTitle, dest, flags);
        dest.writeInt(iconRes);
        dest.writeString(fragment);
        dest.writeBundle(fragmentArguments);
        if (intent != null) {
            dest.writeInt(1);
            intent.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        dest.writeBundle(extras);
    }

    @NonNull
    @Override
    public String toString() {
        return id + "|" + title + "|" + summary;
    }
}
