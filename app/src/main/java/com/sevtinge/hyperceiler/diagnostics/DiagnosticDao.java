/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.diagnostics;
import androidx.room.*;
import java.util.List;
import com.sevtinge.hyperceiler.log.db.LogEntry;
@Dao
public interface DiagnosticDao {
    @Query("SELECT id,module,level,tag,substr(message,1,8192) AS message,timestamp,source_group,process_ids FROM logs WHERE module IN ('Xposed','App') ORDER BY timestamp DESC,id DESC LIMIT 3000")
    List<LogEntry> inputs();
    @Insert(onConflict=OnConflictStrategy.REPLACE) void save(DiagnosticSnapshotEntity snapshot);
    @Query("DELETE FROM hook_diagnostics WHERE fingerprint NOT IN (SELECT fingerprint FROM hook_diagnostics ORDER BY timestamp DESC LIMIT 16)")
    void trim();
    @Query("SELECT * FROM hook_diagnostics ORDER BY timestamp DESC LIMIT 1") DiagnosticSnapshotEntity latest();
    @Query("SELECT * FROM logs WHERE id = :id LIMIT 1") LogEntry raw(long id);
    @Query("DELETE FROM hook_diagnostics") void clear();
}
