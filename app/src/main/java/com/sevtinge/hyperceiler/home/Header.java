package com.sevtinge.hyperceiler.home;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

public class Header implements Parcelable {


    public long id = -1;
    public int groupId;

    public int iconRes;

    public int titleRes;
    public CharSequence title;

    public int summaryRes;
    public CharSequence summary;

    public int breadCrumbTitleRes;
    public CharSequence breadCrumbTitle;

    public int breadCrumbShortTitleRes;
    public CharSequence breadCrumbShortTitle;

    public CharSequence key;

    public Intent intent;

    public Bundle extras;
    public String fragment;

    public Bundle fragmentArguments;

    public int inflatedXml;
    public boolean displayStatus = true;


    public static final Parcelable.Creator<Header> CREATOR = new Parcelable.Creator<>() {

        @Override
        public Header createFromParcel(Parcel source) {
            return new Header(source);
        }

        @Override
        public Header[] newArray(int size) {
            return new Header[size];
        }
    };

    public Header() {}

    Header(Parcel source) {
        readFromParcel(source);
    }

    public CharSequence getTitle(Resources res) {
        if (titleRes != 0) {
            return res.getText(titleRes);
        }
        return title;
    }

    public CharSequence getSummary(Resources res) {
        if (summaryRes != 0) {
            return res.getText(summaryRes);
        }
        return summary;
    }

    public CharSequence getBreadCrumbTitle(Resources res) {
        if (breadCrumbTitleRes != 0) {
            return res.getText(breadCrumbTitleRes);
        }
        return breadCrumbTitle;
    }

    public CharSequence getBreadCrumbShortTitle(Resources res) {
        if (breadCrumbShortTitleRes != 0) {
            return res.getText(breadCrumbShortTitleRes);
        }
        return breadCrumbShortTitle;
    }

    public int getInflatedXml() {
        return inflatedXml;
    }

    public void readFromParcel(Parcel source) {
        id = source.readLong();
        titleRes = source.readInt();
        Parcelable.Creator<CharSequence> creator = TextUtils.CHAR_SEQUENCE_CREATOR;
        title = creator.createFromParcel(source);
        summaryRes = source.readInt();
        summary = creator.createFromParcel(source);
        key = creator.createFromParcel(source);
        breadCrumbTitleRes = source.readInt();
        breadCrumbTitle = creator.createFromParcel(source);
        breadCrumbShortTitleRes = source.readInt();
        breadCrumbShortTitle = creator.createFromParcel(source);
        iconRes = source.readInt();
        fragment = source.readString();
        fragmentArguments = source.readBundle();
        if (source.readInt() != 0) {
            intent = Intent.CREATOR.createFromParcel(source);
        }
        extras = source.readBundle();

        inflatedXml = source.readInt();
        displayStatus = source.readBoolean();
    }

    @Override
    public int describeContents() {
        return 0;
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
        dest.writeInt(inflatedXml);
        dest.writeBoolean(displayStatus);
    }
}
