package com.sevtinge.hyperceiler.search.data;

import androidx.room.Entity;
import androidx.room.Fts4;

@Fts4(contentEntity = ModEntity.class)
@Entity(tableName = "mods_fts")
public class ModFtsEntity {
    // 标题：搜索的核心
    public String title;

    // 路径：让用户可以通过分类名搜到下面的功能
    //public String breadcrumbs;

    // Key：有时候开发者记得 key 却不记得标题，保留 key 的搜索很有用
    //public String key;
}
