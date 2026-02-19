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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.common.model.data;

import android.os.Parcel;
import android.os.Parcelable;

public class ModData implements Parcelable {
    public String title;
    public String breadcrumbs;
    public int xml;
    public String key;
    public int order;
    public String fragment;
    public int catTitleResId;

    public ModData() {}

    protected ModData(Parcel in) {
        title = in.readString();
        breadcrumbs = in.readString();
        xml = in.readInt();
        key = in.readString();
        order = in.readInt();
        fragment = in.readString();
        catTitleResId = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(breadcrumbs);
        dest.writeInt(xml);
        dest.writeString(key);
        dest.writeInt(order);
        dest.writeString(fragment);
        dest.writeInt(catTitleResId);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<ModData> CREATOR = new Creator<>() {
        @Override public ModData createFromParcel(Parcel in) { return new ModData(in); }
        @Override public ModData[] newArray(int size) { return new ModData[size]; }
    };

    /**
     * 从 breadcrumbs 中提取第一级分组名（用于搜索结果分组显示）
     */
    public String getGroup() {
        if (breadcrumbs == null || breadcrumbs.isEmpty()) return "";
        int idx = breadcrumbs.indexOf('/');
        return idx > 0 ? breadcrumbs.substring(0, idx) : breadcrumbs;
    }
}
