/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.diagnostics;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import com.sevtinge.hyperceiler.R;
import fan.preference.PreferenceStyle;
import java.text.NumberFormat;
import java.util.function.IntConsumer;

/** MIUIX HyperCardView, diagnostic status counts. */
public final class DiagnosticsOverviewPreference extends Preference implements PreferenceStyle {
    private long success,abnormal,pending,unknown;
    private boolean loaded,loading,updateFailed;
    private IntConsumer metricClick;
    public DiagnosticsOverviewPreference(Context context,AttributeSet attrs){
        super(context,attrs);setLayoutResource(R.layout.preference_diagnostics_overview);
        setPersistent(false);setSelectable(false);setSingleLineTitle(false);
    }
    public void beginRefresh(){loading=true;updateFailed=false;notifyChanged();}
    public void refreshFailed(){loading=false;updateFailed=true;notifyChanged();}
    public void setOnMetricClickListener(IntConsumer listener){metricClick=listener;notifyChanged();}
    public void setCounts(long success,long abnormal,long pending,long unknown){
        this.success=success;this.abnormal=abnormal;this.pending=pending;this.unknown=unknown;
        loaded=true;loading=false;updateFailed=false;notifyChanged();
    }
    @Override public void onBindViewHolder(PreferenceViewHolder holder){
        super.onBindViewHolder(holder);
        // The child owns its MIUIX card and padding, the list must not flatten
        // this content into a standard preference-row background or padding.
        holder.itemView.setPadding(0,0,0,0);
        int[] ids={R.id.diagnostic_verified,R.id.diagnostic_abnormal,R.id.diagnostic_pending,R.id.diagnostic_unknown};
        int[] groups={R.id.diagnostic_verified_group,R.id.diagnostic_abnormal_group,R.id.diagnostic_pending_group,R.id.diagnostic_unknown_group};
        int[] labels={R.string.hook_diagnostics_success,R.string.hook_diagnostics_abnormal,R.string.hook_diagnostics_pending_label,R.string.hook_diagnostics_unknown};
        long[] counts={success,abnormal,pending,unknown};
        NumberFormat numbers=NumberFormat.getIntegerInstance();numbers.setGroupingUsed(false);
        for(int i=0;i<ids.length;i++){
            String value=loaded?numbers.format(counts[i]):getContext().getString(R.string.hook_diagnostics_waiting_count);
            ((TextView)holder.findViewById(ids[i])).setText(value);
            View group=holder.findViewById(groups[i]);int index=i;
            group.setContentDescription(getContext().getString(labels[i])+"，"+value);
            boolean actionable=loaded&&!loading&&counts[i]>0&&metricClick!=null;
            group.setOnClickListener(actionable?v->{if(metricClick!=null)metricClick.accept(index);}:null);
            group.setClickable(actionable);group.setFocusable(actionable);
        }
        ((TextView)holder.findViewById(R.id.diagnostic_overview_badge)).setText(loading
            ?R.string.hook_diagnostics_analysing_badge:updateFailed?R.string.hook_diagnostics_update_failed_badge:R.string.hook_diagnostics_local_evidence);
        ((TextView)holder.findViewById(android.R.id.summary)).setText(getSummary());
    }
    @Override public boolean isCardStyleEnabled(){return false;}
    @Override public boolean isTouchAnimationEnable(){return false;}
}
