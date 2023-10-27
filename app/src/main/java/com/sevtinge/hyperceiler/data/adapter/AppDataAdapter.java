package com.sevtinge.hyperceiler.data.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.data.AppData;
import com.sevtinge.hyperceiler.utils.PrefsUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AppDataAdapter extends RecyclerView.Adapter<AppDataAdapter.ViewHolder> {

    private static List<AppData> appInfoList;
    private Set<String> selectedApps;
    private onItemClickListener onItemClickListener;
    private final Context mContext;
    private final String mKey;
    private final int mType;


    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<AppData> appInfoList) {
        AppDataAdapter.appInfoList = appInfoList;
        notifyDataSetChanged();
    }

    public AppDataAdapter(Context context, String key, int type) {
        mContext = context;
        mKey = key;
        mType = type;
    }

    /**
     * 在Adapter中设置一个过滤方法，目的是为了将过滤后的数据传入Adapter中并刷新数据
     *
     * @param locationListModels
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setFilter(List<AppData> locationListModels) {
        appInfoList = new ArrayList<>();
        appInfoList.addAll(locationListModels);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rowItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_list, parent, false);
        return new ViewHolder(rowItem);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        AppData appInfo = appInfoList.get(position);
        selectedApps = PrefsUtils.mSharedPreferences.getStringSet(mKey, new LinkedHashSet<>());

        holder.mAppListIcon.setImageBitmap(appInfo.icon);
        holder.mAppName.setText(appInfo.label);
        holder.mAppPackageName.setText(appInfo.packageName);
        holder.mSelecte.setChecked(shouldSelect(appInfo.packageName));

        holder.mSelecte.setVisibility(mType != 0 ? View.GONE : View.VISIBLE);
        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(v, position, appInfo, holder.mSelecte.isChecked()));
    }


    public boolean shouldSelect(String pkgName) {
        return (selectedApps.contains(pkgName));
    }

    public void setOnItemClickListener(onItemClickListener onItemClick) {
        this.onItemClickListener = onItemClick;
    }

    @Override
    public int getItemCount() {
        return appInfoList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView mAppListIcon;
        private final TextView mAppName;
        private final TextView mAppPackageName;
        private final CheckBox mSelecte;

        public ViewHolder(View itemView) {
            super(itemView);
            mAppListIcon = itemView.findViewById(android.R.id.icon);
            mAppName = itemView.findViewById(android.R.id.title);
            mAppPackageName = itemView.findViewById(android.R.id.summary);
            mSelecte = itemView.findViewById(android.R.id.checkbox);
        }
    }

    public interface onItemClickListener {
        void onItemClick(View view, int position, AppData appData, boolean isCheck);
    }

}
