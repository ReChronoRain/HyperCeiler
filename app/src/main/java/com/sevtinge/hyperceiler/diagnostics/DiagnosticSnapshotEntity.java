/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.diagnostics;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName="hook_diagnostics")
public class DiagnosticSnapshotEntity {
    @PrimaryKey @NonNull public String fingerprint="";
    public long timestamp;
    @NonNull public String report="";
    public DiagnosticSnapshotEntity(@NonNull String fingerprint,long timestamp,@NonNull String report){this.fingerprint=fingerprint;this.timestamp=timestamp;this.report=report;}
}
