package com.sevtinge.cemiuiler.data.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.data.AppData;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AppDataAdapter extends RecyclerView.Adapter<AppDataAdapter.ViewHolder> {

    private static List<AppData> appInfoList;
    private Set<String> selectedApps;
    private onItemClickListener onItemClickListener;
    private Context mContext;
    private String mKey;
    private int mType;


    public void setData (List<AppData> appInfoList) {
        this.appInfoList = appInfoList;
        notifyDataSetChanged();
    }

    public AppDataAdapter(Context context, String key, int type) {
        mContext = context;
        mKey = key;
        mType = type;
    }

    /**
     * 在Adapter中设置一个过滤方法，目的是为了将过滤后的数据传入Adapter中并刷新数据
     * @param locationListModels
     */
    public void setFilter(List<AppData> locationListModels ) {
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
        selectedApps = PrefsUtils.mSharedPreferences.getStringSet(mKey, new LinkedHashSet<String>());

        holder.mAppListIcon.setImageDrawable(appInfo.icon);
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
    };

    @Override
    public int getItemCount() {
        return appInfoList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView mAppListIcon;
        private TextView mAppName;
        private TextView mAppPackageName;
        private CheckBox mSelecte;

        public ViewHolder(View itemView) {
            super(itemView);
            mAppListIcon = itemView.findViewById(android.R.id.icon);
            mAppName = itemView.findViewById(android.R.id.title);
            mAppPackageName = itemView.findViewById(android.R.id.summary);
            mSelecte = itemView.findViewById(android.R.id.checkbox);
        }
    }

    public interface onItemClickListener{
        void onItemClick(View view, int position, AppData appData , boolean isCheck);
    }

}
