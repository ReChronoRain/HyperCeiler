package com.sevtinge.hyperceiler.search.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "mods")
public class ModEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String key;
    public String fragment;
    public String breadcrumbs;
    public int xmlResId;

    @ColumnInfo(name = "item_order")
    public int order;

    // 新增：保存分类的资源 ID
    public int catTitleResId;
}

