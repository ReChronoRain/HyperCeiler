package com.sevtinge.hyperceiler.search.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

@Dao
public interface ModDao {
    @Insert
    void insertAll(List<ModEntity> mods);

    @Query("DELETE FROM mods")
    void deleteAll();

    @RawQuery
    List<ModEntity> checkpoint(SupportSQLiteQuery query);

    @Query("SELECT mods.* FROM mods " +
        "JOIN mods_fts ON (mods.id = mods_fts.rowid) " +
        "WHERE mods_fts MATCH :query " +
        "ORDER BY mods.item_order ASC")
    List<ModEntity> search(String query);

    // 临时测试用：
    @Query("SELECT * FROM mods WHERE title LIKE '%' || :query || '%'")
    List<ModEntity> testSearch(String query);


    @Query("SELECT COUNT(*) FROM mods")
    int getCount();

}
